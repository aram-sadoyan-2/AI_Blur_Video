package com.naiyados.aiblurvideo.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized Firebase Analytics tracking utility for AI Blur Video.
 * Handles tracking of screen views, video editing operations, AI plate/face detection,
 * batch queue runs, export metrics, and user interactions.
 */
object AppAnalytics {

    private const val TAG = "AppAnalytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val appContext = context.applicationContext
            firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
            isInitialized = true
            Log.d(TAG, "Firebase Analytics successfully initialized")
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Analytics initialization skipped or fallback used: ${e.message}")
        }
    }

    /**
     * Log a generic event with bundle parameters.
     */
    fun logEvent(eventName: String, params: Bundle.() -> Unit = {}) {
        val bundle = Bundle().apply(params)
        try {
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Logged event: $eventName -> $bundle")
        } catch (e: Throwable) {
            Log.w(TAG, "Error logging event $eventName: ${e.message}")
        }
    }

    /**
     * Track Screen Views
     */
    fun trackScreenView(screenName: String, screenClass: String = screenName) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    /**
     * Track App Launch & Onboarding / First Launch
     */
    fun trackFirstAppOpen() {
        logEvent("app_first_open") {
            putLong("timestamp", System.currentTimeMillis())
        }
    }

    /**
     * Video Import & Picker Events
     */
    fun trackVideoImported(source: String, durationMs: Long = 0L, width: Int = 0, height: Int = 0) {
        logEvent("video_imported") {
            putString("import_source", source)
            putLong("video_duration_ms", durationMs)
            putInt("video_width", width)
            putInt("video_height", height)
        }
    }

    /**
     * Video Editor Tool & Mode Changes
     */
    fun trackToolSelected(toolName: String) {
        logEvent("editor_tool_selected") {
            putString("tool_name", toolName)
        }
    }

    fun trackBlurModeChanged(mode: String) {
        logEvent("blur_mode_changed") {
            putString("mode_type", mode)
        }
    }

    fun trackBlurTypeChanged(type: String) {
        logEvent("blur_type_changed") {
            putString("blur_type", type)
        }
    }

    fun trackBlurStrengthChanged(strength: Int) {
        logEvent("blur_strength_changed") {
            putInt("strength_value", strength)
        }
    }

    fun trackAspectRatioChanged(ratioLabel: String) {
        logEvent("aspect_ratio_changed") {
            putString("ratio_label", ratioLabel)
        }
    }

    fun trackFilterApplied(filterName: String) {
        logEvent("filter_applied") {
            putString("filter_name", filterName)
        }
    }

    fun trackSpeedChanged(speedMultiplier: Float) {
        logEvent("speed_changed") {
            putDouble("speed_multiplier", speedMultiplier.toDouble())
        }
    }

    fun trackCropAdjusted(ratio: String, rotationDeg: Float) {
        logEvent("crop_adjusted") {
            putString("crop_ratio", ratio)
            putDouble("rotation_degrees", rotationDeg.toDouble())
        }
    }

    /**
     * AI Detection Scanning Events
     */
    fun trackDetectionScanStarted(detectorType: String, videoDurationMs: Long) {
        logEvent("ai_scan_started") {
            putString("detector_type", detectorType)
            putLong("video_duration_ms", videoDurationMs)
        }
    }

    fun trackDetectionScanCompleted(
        detectorType: String,
        detectionsCount: Int,
        timeTakenMs: Long
    ) {
        logEvent("ai_scan_completed") {
            putString("detector_type", detectorType)
            putInt("detected_regions_count", detectionsCount)
            putLong("scan_duration_ms", timeTakenMs)
        }
    }

    /**
     * Video Export Lifecycle Tracking
     */
    fun trackExportStarted(
        resolution: String,
        bitrate: String,
        durationMs: Long,
        hasAudio: Boolean,
        fps: Int = 30
    ) {
        logEvent("export_started") {
            putString("export_resolution", resolution)
            putString("export_bitrate", bitrate)
            putInt("export_fps", fps)
            putLong("video_duration_ms", durationMs)
            putBoolean("include_audio", hasAudio)
        }
    }

    fun trackExportSuccess(
        resolution: String,
        bitrate: String,
        frameCount: Int,
        blurredFrames: Int,
        exportTimeMs: Long
    ) {
        logEvent("export_completed") {
            putString("export_resolution", resolution)
            putString("export_bitrate", bitrate)
            putInt("total_frame_count", frameCount)
            putInt("blurred_frame_count", blurredFrames)
            putLong("export_time_taken_ms", exportTimeMs)
        }
    }

    fun trackExportCancelled(progressPercent: Int) {
        logEvent("export_cancelled") {
            putInt("progress_at_cancellation", progressPercent)
        }
    }

    fun trackExportFailed(errorMessage: String) {
        logEvent("export_failed") {
            putString("error_message", errorMessage.take(100))
        }
    }

    /**
     * Video Sharing & History Interactions
     */
    fun trackShareVideo(source: String) {
        logEvent(FirebaseAnalytics.Event.SHARE) {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "video/mp4")
            putString("share_source", source)
        }
    }

    fun trackHistoryVideoPlayed(resolution: String) {
        logEvent("history_video_played") {
            putString("resolution", resolution)
        }
    }

    fun trackHistoryVideoDeleted() {
        logEvent("history_video_deleted")
    }

    /**
     * Frame-by-Frame Precision & Keyframe Propagation Tracking
     */
    fun trackFrameStepped(stepDelta: Int, currentFrame: Int) {
        logEvent("frame_stepped") {
            putInt("step_delta", stepDelta)
            putInt("target_frame", currentFrame)
        }
    }

    fun trackKeyframePropagated(frameOffset: Int, totalKeyframes: Int, mode: String) {
        logEvent("keyframe_propagated") {
            putInt("frame_offset", frameOffset)
            putInt("total_keyframes", totalKeyframes)
            putString("blur_mode", mode)
        }
    }

    /**
     * Batch Processing Queue Tracking
     */
    fun trackBatchItemAdded(count: Int) {
        logEvent("batch_items_added") {
            putInt("added_count", count)
        }
    }

    fun trackBatchProcessingStarted(pendingCount: Int) {
        logEvent("batch_processing_started") {
            putInt("pending_count", pendingCount)
        }
    }

    fun trackBatchQueueCreated(videoCount: Int) {
        logEvent("batch_queue_created") {
            putInt("video_count", videoCount)
        }
    }

    fun trackBatchExportStarted(videoCount: Int, resolution: String, bitrate: String) {
        logEvent("batch_export_started") {
            putInt("total_videos", videoCount)
            putString("export_resolution", resolution)
            putString("export_bitrate", bitrate)
        }
    }

    fun trackBatchItemCompleted(index: Int, total: Int, blurredFrames: Int) {
        logEvent("batch_item_completed") {
            putInt("item_index", index)
            putInt("total_items", total)
            putInt("blurred_frames", blurredFrames)
        }
    }

    /**
     * App Settings & Theme
     */
    fun trackThemeChanged(themeName: String) {
        logEvent("theme_changed") {
            putString("selected_theme", themeName)
        }
    }
}
