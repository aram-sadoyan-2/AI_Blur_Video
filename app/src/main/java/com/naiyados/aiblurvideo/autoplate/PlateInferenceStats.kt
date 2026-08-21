package com.naiyados.aiblurvideo.autoplate

/**
 * Real-time telemetry metrics for AI inference and detection (license plates & human faces).
 */
data class PlateInferenceStats(
    val detectedCount: Int = 0,
    val platesDetectedCount: Int = 0,
    val facesDetectedCount: Int = 0,
    val latencyMs: Long = 0L,
    val isLiveTracking: Boolean = false,
    val confidence: Float = 0f,
    val dominantText: String? = null,
    val detectorEngine: String = "ML Kit Face + TFLite Plate"
)

