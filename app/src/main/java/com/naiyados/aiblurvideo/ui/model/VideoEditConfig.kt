package com.naiyados.aiblurvideo.ui.model

import android.graphics.RectF
import com.naiyados.aiblurvideo.autoplate.AutoPlateTimeline
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings

/**
 * Complete consolidated configuration for all editing tools applied to a video.
 */
data class VideoEditConfig(
    val blurMode: BlurMode = BlurMode.AutoPlate,
    val blurStrength: Float = 0.65f,
    val isPlateBlurActive: Boolean = true,
    val isFaceBlurActive: Boolean = true,
    val filter: VideoFilter = VideoFilter.NONE,
    val filterIntensity: Float = 1.0f,
    val pixelateBlockSize: Int = 24,
    val playbackSpeed: Float = 1.0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L, // 0L means full video
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.ORIGINAL,
    val customCropRect: RectF? = null, // Normalized 0..1 crop window
    val customCropRotation: Float = 0f,
    val customObjectNormalizedRect: RectF? = null, // Normalized 0..1 coordinates for custom object box
    val customObjectRotationDegrees: Float = 0f,
    val customObjectShape: CustomBlurShape = CustomBlurShape.ROUNDED_RECT,
    val customKeyframes: List<KeyframeBlurBox> = emptyList(), // Frame-by-frame keyframe positions
    val keyframePropagationOffset: Int = 10, // Default +-10 frames
    val isFullVideoBlur: Boolean = true, // If true, blur entire video in FullBlur mode
    val blurFrameRangeStartMs: Long = 0L,
    val blurFrameRangeEndMs: Long = 0L,
    val selectedBlurFrameTimestampsMs: Set<Long> = emptySet(), // Individual selected frame timestamps
    val isMuted: Boolean = false,
    val exportSettings: ExportSettings = ExportSettings()
) {
    /**
     * Helper to determine if a specific frame timestamp (in ms) should be blurred in FullBlur mode.
     */
    fun shouldBlurFrame(timeMs: Long): Boolean {
        if (blurMode != BlurMode.FullBlur) return false
        if (isFullVideoBlur) return true

        // Check if falls within range
        if (blurFrameRangeEndMs > blurFrameRangeStartMs && timeMs in blurFrameRangeStartMs..blurFrameRangeEndMs) {
            return true
        }

        // Check individual selected frames (allow +/- 1000ms frame cluster window)
        if (selectedBlurFrameTimestampsMs.isNotEmpty()) {
            val hasNearMatch = selectedBlurFrameTimestampsMs.any { selTime ->
                kotlin.math.abs(selTime - timeMs) <= 500L
            }
            if (hasNearMatch) return true
        }

        return false
    }

    fun hasActiveEdits(): Boolean {
        return blurMode != BlurMode.AutoPlate ||
                filter != VideoFilter.NONE ||
                playbackSpeed != 1.0f ||
                trimStartMs > 0L ||
                trimEndMs > 0L ||
                aspectRatio != VideoAspectRatio.ORIGINAL ||
                (customCropRect != null && (customCropRect.left > 0.001f || customCropRect.top > 0.001f || customCropRect.right < 0.999f || customCropRect.bottom < 0.999f)) ||
                customCropRotation != 0f ||
                customObjectNormalizedRect != null ||
                customObjectRotationDegrees != 0f ||
                isMuted
    }
}
