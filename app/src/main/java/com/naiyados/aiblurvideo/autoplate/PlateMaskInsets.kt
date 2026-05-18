package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF

/** Sizes the on-screen plate cover so text stays hidden. */
object PlateMaskInsets {

    /** Light trim of oversized YOLO boxes. */
    const val FRAME_WIDTH_SCALE = 0.96f
    const val FRAME_HEIGHT_SCALE = 0.92f

    fun tighten(rect: RectF): RectF {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val halfW = rect.width() * FRAME_WIDTH_SCALE / 2f
        val halfH = rect.height() * FRAME_HEIGHT_SCALE / 2f
        return RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    }

    /** Extra margin so plate characters are not visible at the edges. */
    fun paddingForCover(rect: RectF): RectF {
        val padX = rect.width() * 0.08f
        val padY = rect.height() * 0.14f
        return RectF(
            rect.left - padX,
            rect.top - padY,
            rect.right + padX,
            rect.bottom + padY
        )
    }
}
