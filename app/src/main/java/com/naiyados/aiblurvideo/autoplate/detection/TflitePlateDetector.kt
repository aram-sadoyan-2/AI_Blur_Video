package com.naiyados.aiblurvideo.autoplate.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * License plate detector using a standard Ultralytics YOLOv8 TFLite export.
 *
 * Generate `plate_detector.tflite` with: `./scripts/export_plate_model.sh`
 */
class TflitePlateDetector(
    context: Context
) : AutoCloseable {

    private val interpreter: Interpreter
    private val inputWidth: Int
    private val inputHeight: Int
    private val inputDataType: DataType
    private val outputMode: OutputMode
    private val yoloOutputShape: IntArray?
    private val maxDetections: Int
    private val boxesOutputIndex: Int
    private val classesOutputIndex: Int
    private val confidenceOutputIndex: Int

    init {
        val model = loadModelFile(context, MODEL_ASSET)
        interpreter = createInterpreter(model)

        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        inputDataType = inputTensor.dataType()

        inputHeight = when {
            inputShape.size >= 4 && inputShape.getOrNull(3) == 3 -> inputShape[1]
            inputShape.size >= 4 && inputShape.getOrNull(1) == 3 -> inputShape[2]
            else -> INPUT_SIZE
        }
        inputWidth = when {
            inputShape.size >= 4 && inputShape.getOrNull(3) == 3 -> inputShape[2]
            inputShape.size >= 4 && inputShape.getOrNull(1) == 3 -> inputShape[3]
            else -> INPUT_SIZE
        }

        when (interpreter.outputTensorCount) {
            1 -> {
                outputMode = OutputMode.YoloV8
                yoloOutputShape = interpreter.getOutputTensor(0).shape()
                maxDetections = 0
                boxesOutputIndex = 0
                classesOutputIndex = 0
                confidenceOutputIndex = 0
            }
            else -> {
                outputMode = OutputMode.KerasBoxes
                yoloOutputShape = null
                boxesOutputIndex = findOutputIndex { name, shape ->
                    name.contains("box") || (shape.size >= 3 && shape.last() == 4)
                }
                classesOutputIndex = findOutputIndex { name, shape ->
                    name.contains("class") || (shape.size == 2 || (shape.size == 3 && shape.last() != 4))
                }
                confidenceOutputIndex = findOutputIndex { name, _ ->
                    name.contains("conf")
                }
                maxDetections = interpreter.getOutputTensor(boxesOutputIndex).shape()
                    .getOrElse(1) { 50 }
            }
        }

        Log.d(
            TAG,
            "Ready mode=$outputMode input=${inputWidth}x$inputHeight yoloShape=${yoloOutputShape?.contentToString()}"
        )
    }

    fun detect(bitmap: Bitmap): List<PlateDetection> {
        val inputBuffer = preprocess(bitmap)

        return when (outputMode) {
            OutputMode.YoloV8 -> {
                val shape = yoloOutputShape ?: return emptyList()
                val channels = shape[1]
                val anchors = shape[2]
                val output = Array(1) { Array(channels) { FloatArray(anchors) } }
                interpreter.run(inputBuffer, output)
                val flat = FloatArray(channels * anchors)
                var index = 0
                for (channel in 0 until channels) {
                    for (anchor in 0 until anchors) {
                        flat[index++] = output[0][channel][anchor]
                    }
                }
                YoloV8OutputParser.parse(
                    output = flat,
                    shape = shape,
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    inputWidth = inputWidth,
                    inputHeight = inputHeight
                )
            }
            OutputMode.KerasBoxes -> detectKerasOutputs(bitmap, inputBuffer)
        }
    }

    private fun detectKerasOutputs(bitmap: Bitmap, inputBuffer: ByteBuffer): List<PlateDetection> {
        val boxesBuffer = Array(1) { Array(maxDetections) { FloatArray(4) } }
        val classesBuffer = Array(1) { FloatArray(maxDetections) }
        val confidenceBuffer = Array(1) { FloatArray(maxDetections) }

        val outputs = hashMapOf<Int, Any>(
            boxesOutputIndex to boxesBuffer,
            classesOutputIndex to classesBuffer,
            confidenceOutputIndex to confidenceBuffer
        )

        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        return parseKerasDetections(
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
        val buffer = ByteBuffer.allocateDirect(bytesPerChannel * inputWidth * inputHeight * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputWidth * inputHeight)
        scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            if (inputDataType == DataType.UINT8) {
                buffer.put((r * 255f).toInt().toByte())
                buffer.put((g * 255f).toInt().toByte())
                buffer.put((b * 255f).toInt().toByte())
            } else {
                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun parseKerasDetections(
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

            val rect = if (maxOf(box[0], box[1], box[2], box[3]) <= 2.5f) {
                // xyxy normalized
                RectF(
                    box[0] * sourceWidth,
                    box[1] * sourceHeight,
                    box[2] * sourceWidth,
                    box[3] * sourceHeight
                )
            } else {
                // xyxy in model-input pixels
                RectF(
                    box[0] * scaleX,
                    box[1] * scaleY,
                    box[2] * scaleX,
                    box[3] * scaleY
                )
            }
            if (rect.width() <= 0f || rect.height() <= 0f) continue

            results += PlateDetection(
                rect = rect,
                confidence = confidence
            )
        }

        return results.sortedByDescending { it.confidence }.take(3)
    }

    private fun findOutputIndex(
        matcher: (name: String, shape: IntArray) -> Boolean
    ): Int {
        for (i in 0 until interpreter.outputTensorCount) {
            val tensor = interpreter.getOutputTensor(i)
            if (matcher(tensor.name().lowercase(), tensor.shape())) {
                return i
            }
        }
        return 0
    }

    override fun close() {
        interpreter.close()
    }

    private enum class OutputMode {
        YoloV8,
        KerasBoxes
    }

    companion object {
        private const val TAG = "PlateDetector"
        private const val MODEL_ASSET = "plate_detector.tflite"
        private const val INPUT_SIZE = 640
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

        private fun createInterpreter(model: MappedByteBuffer): Interpreter {
            val optionSets = listOf(
                Interpreter.Options().apply { setNumThreads(4) },
                Interpreter.Options().apply {
                    setNumThreads(4)
                    setUseXNNPACK(false)
                }
            )

            var lastError: Exception? = null
            for ((attempt, options) in optionSets.withIndex()) {
                try {
                    val interpreter = Interpreter(model, options)
                    interpreter.allocateTensors()
                    Log.d(TAG, "TFLite interpreter ready (attempt=${attempt + 1})")
                    return interpreter
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Interpreter init failed: ${e.message}")
                }
            }

            throw lastError ?: IllegalStateException("Could not create TFLite interpreter")
        }
    }
}
