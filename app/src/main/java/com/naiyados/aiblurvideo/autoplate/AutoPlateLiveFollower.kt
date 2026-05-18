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
        onBox: suspend (AutoPlateBox?) -> Unit
    ) = withContext(Dispatchers.IO) {
        grabber.open(videoUri)
        pipeline.resetTrack()

        try {
            while (coroutineContext.isActive && shouldContinue()) {
                val timeMs = readPositionMs()
                val bitmap = grabber.frameAt(timeMs)
                if (bitmap != null) {
                    try {
                        val boxes = pipeline.detectFrame(bitmap, timeMs)
                        onBox(boxes.firstOrNull())
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
