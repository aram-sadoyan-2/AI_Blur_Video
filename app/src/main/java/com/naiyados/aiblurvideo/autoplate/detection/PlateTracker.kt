package com.naiyados.aiblurvideo.autoplate.detection

import android.graphics.RectF
/**
 * Smooths plate bounding boxes between sparse video frames (Magritte-style tracking).
 */
class PlateTracker(
    private val smoothAlpha: Float = 0.38f
) {
    private var smoothedRect: RectF? = null
    private var missedFrames = 0

    fun reset() {
        smoothedRect = null
        missedFrames = 0
    }

    fun update(
        detections: List<PlateDetection>,
        frameWidth: Int,
        frameHeight: Int
    ): RectF? {
        val best = detections.maxByOrNull { it.confidence }

        if (best == null) {
            missedFrames++
            return if (missedFrames <= 3) smoothedRect else {
                smoothedRect = null
                null
            }
        }

        missedFrames = 0
        val clamped = clampToFrame(best.rect, frameWidth, frameHeight)

        smoothedRect = if (smoothedRect == null) {
            RectF(clamped)
        } else {
            smoothRect(smoothedRect!!, clamped, smoothAlpha)
        }

        return RectF(smoothedRect)
    }

    private fun smoothRect(old: RectF, new: RectF, alpha: Float): RectF {
        return RectF(
            lerp(old.left, new.left, alpha),
            lerp(old.top, new.top, alpha),
            lerp(old.right, new.right, alpha),
            lerp(old.bottom, new.bottom, alpha)
        )
    }

    private fun lerp(start: Float, end: Float, alpha: Float): Float {
        return start + (end - start) * alpha
    }

    private fun clampToFrame(rect: RectF, frameWidth: Int, frameHeight: Int): RectF {
        return RectF(
            rect.left.coerceIn(0f, frameWidth.toFloat()),
            rect.top.coerceIn(0f, frameHeight.toFloat()),
            rect.right.coerceIn(0f, frameWidth.toFloat()),
            rect.bottom.coerceIn(0f, frameHeight.toFloat())
        )
    }
}
