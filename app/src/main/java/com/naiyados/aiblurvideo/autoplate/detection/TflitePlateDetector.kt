package com.naiyados.aiblurvideo.autoplate.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * On-device license plate detector (YOLOv8-style, KerasCV export).
 * Model: [mithilai/YOLOv8-License-Plate](https://github.com/mithilai/YOLOv8-License-Plate) (MIT)
 */
class TflitePlateDetector(
    context: Context
) : AutoCloseable {

    private val interpreter: Interpreter
    private val inputWidth: Int
    private val inputHeight: Int
    private val inputChannels: Int
    private val inputDataType: DataType

    private val boxesOutputIndex: Int
    private val classesOutputIndex: Int
    private val confidenceOutputIndex: Int
    private val maxDetections: Int

    init {
        val model = loadModelFile(context, MODEL_ASSET)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            addDelegate(FlexDelegate())
        }
        interpreter = Interpreter(model, options)

        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        inputDataType = inputTensor.dataType()

        inputHeight = when {
            inputShape.size >= 4 && inputShape[3] == 3 -> inputShape[1]
            inputShape.size >= 4 && inputShape[1] == 3 -> inputShape[2]
            else -> INPUT_SIZE
        }
        inputWidth = when {
            inputShape.size >= 4 && inputShape[3] == 3 -> inputShape[2]
            inputShape.size >= 4 && inputShape[1] == 3 -> inputShape[3]
            else -> INPUT_SIZE
        }
        inputChannels = 3

        val outputCount = interpreter.outputTensorCount
        var boxesIdx = -1
        var classesIdx = -1
        var confidenceIdx = -1
        val vectorOutputs = mutableListOf<Int>()

        for (i in 0 until outputCount) {
            val tensor = interpreter.getOutputTensor(i)
            val name = tensor.name().lowercase()
            val shape = tensor.shape()

            when {
                name.contains("box") -> boxesIdx = i
                name.contains("class") -> classesIdx = i
                name.contains("conf") -> confidenceIdx = i
                shape.size >= 3 && shape.last() == 4 -> boxesIdx = i
                shape.size == 2 || (shape.size == 3 && shape.last() != 4) -> vectorOutputs += i
            }
        }

        if (vectorOutputs.size >= 2) {
            val scored = vectorOutputs.map { index ->
                index to interpreter.getOutputTensor(index).shape().getOrElse(1) { 0 }
            }
            val sorted = scored.sortedByDescending { it.second }
            if (classesIdx == -1) classesIdx = sorted[0].first
            if (confidenceIdx == -1) confidenceIdx = sorted.getOrNull(1)?.first ?: sorted[0].first
        }

        if (boxesIdx == -1) boxesIdx = 0
        if (classesIdx == -1) classesIdx = 1.coerceAtMost(outputCount - 1)
        if (confidenceIdx == -1) confidenceIdx = 2.coerceAtMost(outputCount - 1)

        boxesOutputIndex = boxesIdx
        classesOutputIndex = classesIdx
        confidenceOutputIndex = confidenceIdx

        val boxesShape = interpreter.getOutputTensor(boxesOutputIndex).shape()
        maxDetections = when {
            boxesShape.size >= 2 -> boxesShape[1]
            else -> 50
        }

        Log.d(
            TAG,
            "Ready input=${inputWidth}x$inputHeight outputs=[$boxesOutputIndex,$classesOutputIndex,$confidenceOutputIndex] maxDet=$maxDetections"
        )
    }

    fun detect(bitmap: Bitmap): List<PlateDetection> {
        val inputBuffer = preprocess(bitmap)
        val boxesBuffer = Array(1) { Array(maxDetections) { FloatArray(4) } }
        val classesBuffer = Array(1) { FloatArray(maxDetections) }
        val confidenceBuffer = Array(1) { FloatArray(maxDetections) }

        val outputs = hashMapOf<Int, Any>(
            boxesOutputIndex to boxesBuffer,
            classesOutputIndex to classesBuffer,
            confidenceOutputIndex to confidenceBuffer
        )

        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        return parseDetections(
            boxes = boxesBuffer[0],
            classes = classesBuffer[0],
            confidences = confidenceBuffer[0],
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height
        )
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val bytesPerChannel = if (inputDataType == DataType.UINT8) 1 else 4
        val buffer = ByteBuffer.allocateDirect(bytesPerChannel * inputWidth * inputHeight * inputChannels)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputWidth * inputHeight)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            when (inputDataType) {
                DataType.UINT8 -> {
                    buffer.put(r.toByte())
                    buffer.put(g.toByte())
                    buffer.put(b.toByte())
                }
                else -> {
                    buffer.putFloat(r.toFloat())
                    buffer.putFloat(g.toFloat())
                    buffer.putFloat(b.toFloat())
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun parseDetections(
        boxes: Array<FloatArray>,
        classes: FloatArray,
        confidences: FloatArray,
        sourceWidth: Int,
        sourceHeight: Int
    ): List<PlateDetection> {
        val scaleX = sourceWidth.toFloat() / inputWidth.toFloat()
        val scaleY = sourceHeight.toFloat() / inputHeight.toFloat()
        val results = mutableListOf<PlateDetection>()

        for (i in 0 until maxDetections) {
            val confidence = confidences.getOrNull(i) ?: continue
            val classId = classes.getOrNull(i)?.toInt() ?: -1

            if (confidence < CONFIDENCE_THRESHOLD) continue
            if (classId == -1) continue

            val box = boxes.getOrNull(i) ?: continue
            if (box.size < 4) continue

            val xmin = box[0] * scaleX
            val ymin = box[1] * scaleY
            val xmax = box[2] * scaleX
            val ymax = box[3] * scaleY

            if (xmax <= xmin || ymax <= ymin) continue

            val width = xmax - xmin
            val height = ymax - ymin
            val aspect = width / height.coerceAtLeast(1f)
            if (aspect < 1.8f || aspect > 10f) continue

            results += PlateDetection(
                rect = RectF(xmin, ymin, xmax, ymax),
                confidence = confidence
            )
        }

        return results.sortedByDescending { it.confidence }.take(3)
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "PlateDetector"
        private const val MODEL_ASSET = "plate_detector.tflite"
        private const val INPUT_SIZE = 416
        private const val CONFIDENCE_THRESHOLD = 0.20f

        fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
            context.assets.openFd(assetName).use { fd ->
                FileInputStream(fd.fileDescriptor).use { stream ->
                    return stream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength
                    )
                }
            }
        }
    }
}
