package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoPlateScanner(
    private val context: Context
) {

    private val detector = MlKitPlateOcrDetector()

    suspend fun scan(
        videoUri: Uri,
        onFoundCountChanged: (Int) -> Unit
    ): List<AutoPlateBox> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val allBoxes = mutableListOf<AutoPlateBox>()

        try {
            retriever.setDataSource(context, videoUri)

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            var timeMs = 0L
            var frameCount = 0

            // Smoother detection timeline.
            // 250ms = 4 scans per second.
            val stepMs = 100L

            // 160 frames * 250ms = about 40 seconds max scan.
            val maxFrames = 250

            while (timeMs <= durationMs && frameCount < maxFrames) {
                val bitmap = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    val boxes = detector.detect(
                        bitmap = bitmap,
                        timeMs = timeMs
                    )

                    allBoxes += boxes

                    boxes.forEach { box ->
                        Log.d(
                            "AutoPlate",
                            "time=${box.timeMs} text=${box.text} rect=${box.rect} frame=${box.frameWidth}x${box.frameHeight}"
                        )
                    }

                    onFoundCountChanged(allBoxes.size)
                    bitmap.recycle()
                }

                frameCount++
                timeMs += stepMs
            }
        } catch (e: Exception) {
            Log.e("AutoPlate", "Scan failed", e)
        } finally {
            retriever.release()
        }

        Log.d("AutoPlate", "Raw scan finished. Boxes=${allBoxes.size}")

        allBoxes
    }
}