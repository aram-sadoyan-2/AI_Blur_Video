package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF

enum class DetectionTarget(val label: String) {
    PLATE("License Plate"),
    FACE("Human Face")
}

data class AutoPlateBox(
    val timeMs: Long,
    val rect: RectF,
    val text: String,
    val frameWidth: Int,
    val frameHeight: Int,
    val confidence: Float = 0f,
    val targetType: DetectionTarget = DetectionTarget.PLATE
)