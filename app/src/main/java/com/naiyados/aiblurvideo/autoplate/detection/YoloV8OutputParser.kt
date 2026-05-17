package com.naiyados.aiblurvideo.autoplate.detection

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Parses Ultralytics YOLOv8 TFLite output: [1, 4+classes, anchors] or transposed.
 */
object YoloV8OutputParser {

    private const val CONFIDENCE_THRESHOLD = 0.25f
    private const val IOU_THRESHOLD = 0.45f

    fun parse(
        output: FloatArray,
        shape: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        inputWidth: Int,
        inputHeight: Int
    ): List<PlateDetection> {
        if (shape.size != 3) return emptyList()

        val channels = shape[1]
        val anchors = shape[2]
        val numClasses = channels - 4
        if (numClasses < 1) return emptyList()

        val channelMajor = channels < anchors
        val candidates = mutableListOf<PlateDetection>()

        for (anchor in 0 until anchors) {
            val cx: Float
            val cy: Float
            val w: Float
            val h: Float
            var bestClass = 0
            var bestConf = 0f

            if (channelMajor) {
                cx = value(output, shape, 0, anchor)
                cy = value(output, shape, 1, anchor)
                w = value(output, shape, 2, anchor)
                h = value(output, shape, 3, anchor)
                for (cls in 0 until numClasses) {
                    val score = value(output, shape, 4 + cls, anchor)
                    if (score > bestConf) {
                        bestConf = score
                        bestClass = cls
                    }
                }
            } else {
                val base = anchor * channels
                cx = output[base]
                cy = output[base + 1]
                w = output[base + 2]
                h = output[base + 3]
                for (cls in 0 until numClasses) {
                    val score = output[base + 4 + cls]
                    if (score > bestConf) {
                        bestConf = score
                        bestClass = cls
                    }
                }
            }

            if (bestConf < CONFIDENCE_THRESHOLD) continue

            val rect = boxToFrameRect(
                cx = cx,
                cy = cy,
                w = w,
                h = h,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                inputWidth = inputWidth,
                inputHeight = inputHeight
            ) ?: continue

            candidates += PlateDetection(
                rect = rect,
                confidence = bestConf
            )
        }

        return applyNms(candidates).take(3)
    }

    private fun value(
        output: FloatArray,
        shape: IntArray,
        channel: Int,
        anchor: Int
    ): Float {
        return output[channel * shape[2] + anchor]
    }

    /**
     * Ultralytics TFLite exports use normalized center boxes (0–1).
     * Older / Keras exports use pixel coords on the model input size (e.g. 640).
     */
    internal fun boxToFrameRect(
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        inputWidth: Int,
        inputHeight: Int
    ): RectF? {
        val normalized = max(max(cx, cy), max(w, h)) <= 2.5f

        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float

        if (normalized) {
            x1 = (cx - w / 2f) * sourceWidth
            y1 = (cy - h / 2f) * sourceHeight
            x2 = (cx + w / 2f) * sourceWidth
            y2 = (cy + h / 2f) * sourceHeight
        } else {
            val scaleX = sourceWidth.toFloat() / inputWidth.toFloat()
            val scaleY = sourceHeight.toFloat() / inputHeight.toFloat()
            x1 = (cx - w / 2f) * scaleX
            y1 = (cy - h / 2f) * scaleY
            x2 = (cx + w / 2f) * scaleX
            y2 = (cy + h / 2f) * scaleY
        }

        if (x2 <= x1 || y2 <= y1) return null

        return RectF(
            x1.coerceIn(0f, sourceWidth.toFloat()),
            y1.coerceIn(0f, sourceHeight.toFloat()),
            x2.coerceIn(0f, sourceWidth.toFloat()),
            y2.coerceIn(0f, sourceHeight.toFloat())
        )
    }

    private fun applyNms(boxes: List<PlateDetection>): List<PlateDetection> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<PlateDetection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept += best
            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (iou(best.rect, other.rect) >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return kept
    }

    private fun iou(first: RectF, second: RectF): Float {
        val x1 = max(first.left, second.left)
        val y1 = max(first.top, second.top)
        val x2 = min(first.right, second.right)
        val y2 = min(first.bottom, second.bottom)
        val intersection = max(0f, x2 - x1) * max(0f, y2 - y1)
        val union = first.width() * first.height() +
            second.width() * second.height() - intersection
        return if (union <= 0f) 0f else intersection / union
    }
}
