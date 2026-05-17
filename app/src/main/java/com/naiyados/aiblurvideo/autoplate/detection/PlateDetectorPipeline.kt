package com.naiyados.aiblurvideo.autoplate.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.naiyados.aiblurvideo.autoplate.AutoPlateBox
import com.naiyados.aiblurvideo.autoplate.MlKitPlateOcrDetector

/**
 * Primary: TFLite plate detector + temporal tracker.
 * Fallback: ML Kit OCR (legacy) if the model fails to load.
 */
class PlateDetectorPipeline(
    context: Context
) : AutoCloseable {

    private val tfliteDetector: TflitePlateDetector?
    private val ocrFallback = MlKitPlateOcrDetector()
    private val tracker = PlateTracker()
    private val usingTflite: Boolean

    init {
        tfliteDetector = if (hasAsset(context, MODEL_ASSET)) {
            try {
                TflitePlateDetector(context)
            } catch (e: Exception) {
                android.util.Log.w(
                    "PlateDetector",
                    "TFLite load failed, using OCR. Run scripts/export_plate_model.sh — ${e.message}"
                )
                null
            }
        } else {
            android.util.Log.w(
                "PlateDetector",
                "Missing assets/$MODEL_ASSET — using OCR. Run: ./scripts/export_plate_model.sh"
            )
            null
        }
        usingTflite = tfliteDetector != null
    }

    val isUsingMlDetector: Boolean
        get() = usingTflite

    fun resetTrack() {
        tracker.reset()
    }

    suspend fun detectFrame(
        bitmap: Bitmap,
        timeMs: Long
    ): List<AutoPlateBox> {
        if (tfliteDetector != null) {
            val detections = tfliteDetector.detect(bitmap)
            val tracked = tracker.update(
                detections = detections,
                frameWidth = bitmap.width,
                frameHeight = bitmap.height
            )

            if (tracked != null) {
                val bestConf = detections.maxOfOrNull { it.confidence } ?: 0f
                return listOf(
                    AutoPlateBox(
                        timeMs = timeMs,
                        rect = RectF(tracked),
                        text = "PLATE",
                        frameWidth = bitmap.width,
                        frameHeight = bitmap.height,
                        confidence = bestConf
                    )
                )
            }
            return emptyList()
        }

        return ocrFallback.detect(bitmap, timeMs)
    }

    override fun close() {
        tfliteDetector?.close()
    }

    companion object {
        private const val MODEL_ASSET = "plate_detector.tflite"

        private fun hasAsset(context: Context, name: String): Boolean {
            return try {
                context.assets.openFd(name).use { }
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
