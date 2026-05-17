package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF

/**
 * Limits how fast the on-screen blur box can move during playback (px per update).
 */
class PlaybackRectSmoother(
    private val maxDeltaPx: Float = 28f,
    private val maxDeltaWidthFraction: Float = 0.10f
) {
    private var rect: RectF? = null

    fun reset() {
        rect = null
    }

    fun smooth(target: RectF): RectF {
        val limit = maxDeltaPx.coerceAtLeast(target.width() * maxDeltaWidthFraction)
        val current = rect
        if (current == null) {
            rect = RectF(target)
            return RectF(target)
        }

        val next = RectF(
            step(current.left, target.left, limit),
            step(current.top, target.top, limit),
            step(current.right, target.right, limit),
            step(current.bottom, target.bottom, limit)
        )
        rect = next
        return RectF(next)
    }

    private fun step(from: Float, to: Float, limit: Float): Float {
        val delta = (to - from).coerceIn(-limit, limit)
        return from + delta
    }
}
