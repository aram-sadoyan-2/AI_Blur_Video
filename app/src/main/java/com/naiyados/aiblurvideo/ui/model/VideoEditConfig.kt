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
    val customObjectNormalizedRect: RectF? = null, // Normalized 0..1 coordinates for custom object box
    val isMuted: Boolean = false,
    val exportSettings: ExportSettings = ExportSettings()
) {
    fun hasActiveEdits(): Boolean {
        return blurMode != BlurMode.AutoPlate ||
                filter != VideoFilter.NONE ||
                playbackSpeed != 1.0f ||
                trimStartMs > 0L ||
                trimEndMs > 0L ||
                aspectRatio != VideoAspectRatio.ORIGINAL ||
                customObjectNormalizedRect != null ||
                isMuted
    }
}
