package com.naiyados.aiblurvideo.autoplate

enum class PlateTrackConfidence {
    /** One fixed rect for the whole clip — no per-keyframe jumps. */
    High,
    /** Interpolated keyframes; plate moves slightly in frame. */
    Medium,
    /** Scan did not find a reliable plate track. */
    Low
}

data class AutoPlateScanResult(
    val boxes: List<AutoPlateBox>,
    val durationMs: Long,
    val confidence: PlateTrackConfidence,
    val dominantText: String?
)
