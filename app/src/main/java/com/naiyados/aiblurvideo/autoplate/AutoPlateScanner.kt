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

            val stepMs = 500L
            val maxFrames = 80

            while (timeMs <= durationMs && frameCount < maxFrames) {
                val bitmap = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
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
                            "time=${box.timeMs} text=${box.text} rect=${box.rect}"
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

        allBoxes
    }
}