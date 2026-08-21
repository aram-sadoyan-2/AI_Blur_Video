package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.net.Uri
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.detection.PlateDetectorPipeline
import com.naiyados.aiblurvideo.autoplate.face.FaceBlurDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Runs TFLite & ML Kit on the current video frame while playing so the masks follow plates and faces
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
        detectPlates: Boolean = true,
        detectFaces: Boolean = true,
        readPositionMs: () -> Long,
        shouldContinue: () -> Boolean,
        onBoxes: suspend (List<AutoPlateBox>) -> Unit,
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
                        val detectedBoxes = mutableListOf<AutoPlateBox>()

                        if (detectPlates) {
                            val plateBoxes = pipeline.detectFrame(bitmap, timeMs)
                            detectedBoxes += plateBoxes
                        }

                        if (detectFaces) {
                            val faceBoxes = FaceBlurDetector.detectFaceBoxes(bitmap, timeMs)
                            detectedBoxes += faceBoxes
                        }

                        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                        val platesCount = detectedBoxes.count { it.targetType == DetectionTarget.PLATE }
                        val facesCount = detectedBoxes.count { it.targetType == DetectionTarget.FACE }
                        val firstBox = detectedBoxes.firstOrNull()

                        onBoxes(detectedBoxes)
                        onStats?.invoke(
                            PlateInferenceStats(
                                detectedCount = detectedBoxes.size,
                                platesDetectedCount = platesCount,
                                facesDetectedCount = facesCount,
                                latencyMs = elapsedMs,
                                isLiveTracking = true,
                                confidence = firstBox?.confidence ?: 0f,
                                dominantText = firstBox?.text,
                                detectorEngine = when {
                                    detectPlates && detectFaces -> "ML Kit Face + TFLite Plate"
                                    detectFaces -> "ML Kit Face Detector"
                                    pipeline.isUsingMlDetector -> "TFLite SSD Tracker"
                                    else -> "ML Kit Vision OCR"
                                }
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
            onBoxes(emptyList())
            onStats?.invoke(
                PlateInferenceStats(
                    detectedCount = 0,
                    latencyMs = 0L,
                    isLiveTracking = false
                )
            )
        }
    }

    suspend fun followWhilePlaying(
        videoUri: Uri,
        readPositionMs: () -> Long,
        shouldContinue: () -> Boolean,
        onBox: suspend (AutoPlateBox?) -> Unit,
        onStats: (suspend (PlateInferenceStats) -> Unit)? = null
    ) {
        followWhilePlaying(
            videoUri = videoUri,
            detectPlates = true,
            detectFaces = true,
            readPositionMs = readPositionMs,
            shouldContinue = shouldContinue,
            onBoxes = { boxes -> onBox(boxes.firstOrNull()) },
            onStats = onStats
        )
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
