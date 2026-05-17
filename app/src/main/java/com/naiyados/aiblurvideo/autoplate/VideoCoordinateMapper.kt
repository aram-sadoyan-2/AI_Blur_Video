package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF

object VideoCoordinateMapper {

    data class FitTransform(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
        val frameWidth: Float,
        val frameHeight: Float
    )

    fun fitTransform(
        frameWidth: Float,
        frameHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ): FitTransform {
        val frameRatio = frameWidth / frameHeight
        val viewRatio = viewWidth / viewHeight

        val scale: Float
        val offsetX: Float
        val offsetY: Float

        if (frameRatio > viewRatio) {
            scale = viewWidth / frameWidth
            offsetX = 0f
            offsetY = (viewHeight - frameHeight * scale) / 2f
        } else {
            scale = viewHeight / frameHeight
            offsetX = (viewWidth - frameWidth * scale) / 2f
            offsetY = 0f
        }

        return FitTransform(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }

    fun contentBounds(transform: FitTransform, viewWidth: Float, viewHeight: Float): RectF {
        return RectF(
            transform.offsetX,
            transform.offsetY,
            transform.offsetX + transform.frameWidth * transform.scale,
            transform.offsetY + transform.frameHeight * transform.scale
        )
    }

    fun frameToView(
        frameRect: RectF,
        transform: FitTransform
    ): RectF {
        return RectF(
            transform.offsetX + frameRect.left * transform.scale,
            transform.offsetY + frameRect.top * transform.scale,
            transform.offsetX + frameRect.right * transform.scale,
            transform.offsetY + frameRect.bottom * transform.scale
        )
    }

    fun viewToFrame(
        viewRect: RectF,
        transform: FitTransform
    ): RectF {
        return RectF(
            (viewRect.left - transform.offsetX) / transform.scale,
            (viewRect.top - transform.offsetY) / transform.scale,
            (viewRect.right - transform.offsetX) / transform.scale,
            (viewRect.bottom - transform.offsetY) / transform.scale
        )
    }

    fun normalizedToFrame(
        normalized: RectF,
        frameWidth: Int,
        frameHeight: Int
    ): RectF {
        return RectF(
            normalized.left * frameWidth,
            normalized.top * frameHeight,
            normalized.right * frameWidth,
            normalized.bottom * frameHeight
        )
    }

    fun frameToNormalized(
        frameRect: RectF,
        frameWidth: Int,
        frameHeight: Int
    ): RectF {
        return RectF(
            frameRect.left / frameWidth,
            frameRect.top / frameHeight,
            frameRect.right / frameWidth,
            frameRect.bottom / frameHeight
        )
    }

    /** Typical plate band for a first guess before the user drags. */
    fun defaultPlateNormalizedRect(): RectF {
        return RectF(0.18f, 0.72f, 0.82f, 0.82f)
    }

    fun clampViewRectToContent(
        viewRect: RectF,
        content: RectF
    ): RectF {
        val width = viewRect.width().coerceAtLeast(48f)
        val height = viewRect.height().coerceAtLeast(20f)

        var left = viewRect.left
        var top = viewRect.top

        if (left < content.left) left = content.left
        if (top < content.top) top = content.top
        if (left + width > content.right) left = content.right - width
        if (top + height > content.bottom) top = content.bottom - height

        return RectF(left, top, left + width, top + height)
    }
}
