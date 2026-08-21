package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.detection.PlateDetectorPipeline
import com.naiyados.aiblurvideo.autoplate.face.FaceBlurDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class AutoPlateScanner(
    private val context: Context
) {

    suspend fun scan(
        videoUri: Uri,
        detectPlates: Boolean = true,
        detectFaces: Boolean = true,
        onProgress: suspend (framesScanned: Int, totalFrames: Int, detectionsFound: Int, progress: Float) -> Unit
    ): AutoPlateScanResult = withContext(Dispatchers.IO) {
        val pipeline = PlateDetectorPipeline(context)
        val retriever = MediaMetadataRetriever()
        val allBoxes = mutableListOf<AutoPlateBox>()

        try {
            pipeline.resetTrack()
            retriever.setDataSource(context, videoUri)

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            var timeMs = 0L
            var frameCount = 0
            var detectStreak = 0
            var hasFrameZeroDetection = false

            val stepMs = if (pipeline.isUsingMlDetector) 50L else 100L
            val maxFrames = if (pipeline.isUsingMlDetector) 480 else 240
            val estimatedTotalFrames = if (durationMs > 0L) {
                ((durationMs / stepMs) + 1).toInt().coerceIn(1, maxFrames)
            } else {
                maxFrames
            }

            Log.d(
                "AutoPlate",
                "Scan start detector=${if (pipeline.isUsingMlDetector) "TFLite+track" else "OCR-fallback"} durationMs=$durationMs estFrames=$estimatedTotalFrames detectPlates=$detectPlates detectFaces=$detectFaces"
            )

            withContext(Dispatchers.Main) {
                onProgress(0, estimatedTotalFrames, 0, 0f)
            }

            while (timeMs <= durationMs && frameCount < maxFrames) {
                coroutineContext.ensureActive()

                val bitmap = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    var frameDetectionsCount = 0

                    if (detectPlates) {
                        val plateBoxes = pipeline.detectFrame(
                            bitmap = bitmap,
                            timeMs = timeMs
                        )
                        allBoxes += plateBoxes
                        frameDetectionsCount += plateBoxes.size

                        plateBoxes.forEach { box ->
                            Log.d(
                                "AutoPlate",
                                "PLATE time=${box.timeMs} conf=${box.confidence} rect=${box.rect}"
                            )
                        }
                    }

                    if (detectFaces) {
                        val faceBoxes = FaceBlurDetector.detectFaceBoxes(
                            bitmap = bitmap,
                            timeMs = timeMs
                        )
                        allBoxes += faceBoxes
                        frameDetectionsCount += faceBoxes.size

                        faceBoxes.forEach { box ->
                            Log.d(
                                "AutoPlate",
                                "FACE time=${box.timeMs} conf=${box.confidence} rect=${box.rect}"
                            )
                        }
                    }

                    if (timeMs == 0L && frameDetectionsCount > 0) {
                        hasFrameZeroDetection = true
                    }

                    detectStreak = if (frameDetectionsCount > 0) detectStreak + 1 else 0
                    bitmap.recycle()

                    if (
                        !pipeline.isUsingMlDetector &&
                        !detectFaces &&
                        detectStreak >= 10 &&
                        hasFrameZeroDetection
                    ) {
                        Log.d("AutoPlate", "Early exit: stable detections")
                        break
                    }
                }

                frameCount++
                val completionProgress = if (durationMs > 0L) {
                    (timeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    (frameCount.toFloat() / estimatedTotalFrames.toFloat()).coerceIn(0f, 1f)
                }

                withContext(Dispatchers.Main) {
                    onProgress(frameCount, estimatedTotalFrames, allBoxes.size, completionProgress)
                }

                timeMs += stepMs
            }

            Log.d("AutoPlate", "Scan finished. Boxes=${allBoxes.size} frames=$frameCount")

            val timeline = AutoPlateTimeline(
                boxes = allBoxes,
                videoDurationMs = durationMs
            )

            AutoPlateScanResult(
                boxes = allBoxes,
                durationMs = durationMs,
                confidence = timeline.confidence,
                dominantText = timeline.dominantText
            )
        } catch (ce: CancellationException) {
            Log.d("AutoPlate", "Scan cancelled, freeing resources")
            throw ce
        } catch (e: Exception) {
            Log.e("AutoPlate", "Scan failed", e)
            AutoPlateScanResult(
                boxes = emptyList(),
                durationMs = 0L,
                confidence = PlateTrackConfidence.Low,
                dominantText = null
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
            try {
                pipeline.close()
            } catch (_: Throwable) {}
        }
    }
}
