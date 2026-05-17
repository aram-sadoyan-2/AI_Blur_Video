package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.detection.PlateDetectorPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoPlateScanner(
    private val context: Context
) {

    suspend fun scan(
        videoUri: Uri,
        onFoundCountChanged: (Int) -> Unit
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

            val stepMs = 100L
            val maxFrames = 240

            Log.d(
                "AutoPlate",
                "Scan start detector=${if (pipeline.isUsingMlDetector) "TFLite+track" else "OCR-fallback"}"
            )

            while (timeMs <= durationMs && frameCount < maxFrames) {
                val bitmap = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    val boxes = pipeline.detectFrame(
                        bitmap = bitmap,
                        timeMs = timeMs
                    )

                    allBoxes += boxes

                    boxes.forEach { box ->
                        Log.d(
                            "AutoPlate",
                            "time=${box.timeMs} conf=${box.confidence} rect=${box.rect}"
                        )
                    }

                    onFoundCountChanged(allBoxes.size)

                    if (timeMs == 0L && boxes.isNotEmpty()) {
                        hasFrameZeroDetection = true
                    }

                    detectStreak = if (boxes.isNotEmpty()) detectStreak + 1 else 0
                    bitmap.recycle()

                    if (
                        !pipeline.isUsingMlDetector &&
                        detectStreak >= 10 &&
                        hasFrameZeroDetection
                    ) {
                        Log.d("AutoPlate", "Early exit: stable detections")
                        break
                    }
                }

                frameCount++
                timeMs += stepMs
            }

            Log.d("AutoPlate", "Scan finished. Boxes=${allBoxes.size}")

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
        } catch (e: Exception) {
            Log.e("AutoPlate", "Scan failed", e)
            AutoPlateScanResult(
                boxes = emptyList(),
                durationMs = 0L,
                confidence = PlateTrackConfidence.Low,
                dominantText = null
            )
        } finally {
            retriever.release()
            pipeline.close()
        }
    }
}
