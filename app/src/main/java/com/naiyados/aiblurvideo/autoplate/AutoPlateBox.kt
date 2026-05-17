package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF

data class AutoPlateBox(
    val timeMs: Long,
    val rect: RectF,
    val text: String,
    val frameWidth: Int,
    val frameHeight: Int
)