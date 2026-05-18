package com.naiyados.aiblurvideo.autoplate.detection

import android.graphics.RectF
import com.naiyados.aiblurvideo.autoplate.PlateScoring

object PlateDetectionSelector {

    fun pickBest(
        detections: List<PlateDetection>,
        anchor: RectF?,
        frameWidth: Int,
        frameHeight: Int,
        minConfidence: Float
    ): PlateDetection? {
        val shaped = detections.filter {
            it.confidence >= minConfidence &&
                PlateScoring.isPlateShaped(it.rect, frameWidth, frameHeight)
        }
        if (shaped.isEmpty()) return null

        if (anchor == null) {
            return shaped.maxByOrNull { it.confidence }
        }

        val consistent = shaped.filter {
            !PlateScoring.isOutlierJump(anchor, it.rect) &&
                !PlateScoring.isSizeOutlier(anchor, it.rect)
        }

        return consistent.maxWithOrNull(
            compareBy<PlateDetection> { PlateScoring.iou(anchor, it.rect) }
                .thenBy { it.confidence }
        )
    }

    fun rectWithStableSize(
        centerRect: RectF,
        anchorWidth: Float,
        anchorHeight: Float,
        sizeAlpha: Float = 0.10f
    ): Pair<RectF, Pair<Float, Float>> {
        val nextWidth = anchorWidth + (centerRect.width() - anchorWidth) * sizeAlpha
        val nextHeight = anchorHeight + (centerRect.height() - anchorHeight) * sizeAlpha
        val cx = centerRect.centerX()
        val cy = centerRect.centerY()

        val stable = RectF(
            cx - nextWidth / 2f,
            cy - nextHeight / 2f,
            cx + nextWidth / 2f,
            cy + nextHeight / 2f
        )
        return stable to (nextWidth to nextHeight)
    }
}
