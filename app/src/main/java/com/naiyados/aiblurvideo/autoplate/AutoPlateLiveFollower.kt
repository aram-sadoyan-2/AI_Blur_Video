package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.net.Uri
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.detection.PlateDetectorPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Runs TFLite on the current video frame while playing so the mask follows the plate
 * instead of relying only on pre-scanned keyframes.
 */
class AutoPlateLiveFollower(
    context: Context
) {
    private val appContext = context.applicationContext
    private val grabber = VideoFrameGrabber(appContext)
    private val pipeline = PlateDetectorPipeline(appContext)

    suspend fun followWhilePlaying(
        videoUri: Uri,
        readPositionMs: () -> Long,
        shouldContinue: () -> Boolean,
        onBox: suspend (AutoPlateBox?) -> Unit,
        onStats: (suspend (PlateInferenceStats) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        grabber.open(videoUri)
        pipeline.resetTrack()

        try {
            while (coroutineContext.isActive && shouldContinue()) {
                val timeMs = readPositionMs()
                val bitmap = grabber.frameAt(timeMs)
                if (bitmap != null) {
                    try {
                        val startNs = System.nanoTime()
                        val boxes = pipeline.detectFrame(bitmap, timeMs)
                        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                        val firstBox = boxes.firstOrNull()

                        onBox(firstBox)
                        onStats?.invoke(
                            PlateInferenceStats(
                                detectedCount = boxes.size,
                                latencyMs = elapsedMs,
                                isLiveTracking = true,
                                confidence = firstBox?.confidence ?: 0f,
                                dominantText = firstBox?.text,
                                detectorEngine = if (pipeline.isUsingMlDetector) "TFLite SSD" else "ML Kit CV"
                            )
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
                delay(LIVE_STEP_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Live follow failed", e)
        } finally {
            onBox(null)
            onStats?.invoke(
                PlateInferenceStats(
                    detectedCount = 0,
                    latencyMs = 0L,
                    isLiveTracking = false
                )
            )
        }
    }

    fun close() {
        grabber.close()
        pipeline.close()
    }

    companion object {
        private const val TAG = "AutoPlateLive"
        private const val LIVE_STEP_MS = 66L
    }
}
