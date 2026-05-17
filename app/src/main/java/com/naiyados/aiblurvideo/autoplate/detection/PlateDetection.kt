package com.naiyados.aiblurvideo.autoplate.detection

import android.graphics.RectF

data class PlateDetection(
    val rect: RectF,
    val confidence: Float
)
