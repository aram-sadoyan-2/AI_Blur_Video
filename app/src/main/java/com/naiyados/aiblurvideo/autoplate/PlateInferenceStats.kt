package com.naiyados.aiblurvideo.autoplate

/**
 * Real-time telemetry metrics for license plate inference and detection.
 */
data class PlateInferenceStats(
    val detectedCount: Int = 0,
    val latencyMs: Long = 0L,
    val isLiveTracking: Boolean = false,
    val confidence: Float = 0f,
    val dominantText: String? = null,
    val detectorEngine: String = "TFLite SSD Tracker"
)
