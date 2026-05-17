package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF

/**
 * Limits how fast the on-screen blur box can move during playback (px per update).
 */
class PlaybackRectSmoother(
    private val maxDeltaPx: Float = 8f
) {
    private var rect: RectF? = null

    fun reset() {
        rect = null
    }

    fun smooth(target: RectF): RectF {
        val current = rect
        if (current == null) {
            rect = RectF(target)
            return RectF(target)
        }

        val next = RectF(
            step(current.left, target.left),
            step(current.top, target.top),
            step(current.right, target.right),
            step(current.bottom, target.bottom)
        )
        rect = next
        return RectF(next)
    }

    private fun step(from: Float, to: Float): Float {
        val delta = (to - from).coerceIn(-maxDeltaPx, maxDeltaPx)
        return from + delta
    }
}
