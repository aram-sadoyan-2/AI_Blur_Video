package com.naiyados.aiblurvideo.autoplate.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.naiyados.aiblurvideo.autoplate.AutoPlateBox
import com.naiyados.aiblurvideo.autoplate.MlKitPlateOcrDetector
import com.naiyados.aiblurvideo.autoplate.PlateMaskInsets

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
    private var anchorRect: RectF? = null
    private var anchorWidth = 0f
    private var anchorHeight = 0f

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
        anchorRect = null
        anchorWidth = 0f
        anchorHeight = 0f
    }

    suspend fun detectFrame(
        bitmap: Bitmap,
        timeMs: Long
    ): List<AutoPlateBox> {
        if (tfliteDetector != null) {
            val detections = tfliteDetector.detect(bitmap)
            tracker.update(
                detections = detections,
                frameWidth = bitmap.width,
                frameHeight = bitmap.height
            )

            val best = PlateDetectionSelector.pickBest(
                detections = detections,
                anchor = anchorRect,
                frameWidth = bitmap.width,
                frameHeight = bitmap.height,
                minConfidence = RECORD_CONFIDENCE
            ) ?: return emptyList()

            val tightened = PlateMaskInsets.tighten(best.rect)
            val (stableRect, nextSize) = if (anchorWidth <= 0f || anchorHeight <= 0f) {
                anchorWidth = tightened.width()
                anchorHeight = tightened.height()
                tightened to (anchorWidth to anchorHeight)
            } else {
                PlateDetectionSelector.rectWithStableSize(
                    centerRect = tightened,
                    anchorWidth = anchorWidth,
                    anchorHeight = anchorHeight,
                    sizeAlpha = SIZE_UPDATE_ALPHA
                )
            }

            anchorWidth = nextSize.first
            anchorHeight = nextSize.second
            anchorRect = RectF(stableRect)

            return listOf(
                AutoPlateBox(
                    timeMs = timeMs,
                    rect = stableRect,
                    text = PLATE_PLACEHOLDER,
                    frameWidth = bitmap.width,
                    frameHeight = bitmap.height,
                    confidence = best.confidence
                )
            )
        }

        return ocrFallback.detect(bitmap, timeMs)
    }

    override fun close() {
        tfliteDetector?.close()
    }

    companion object {
        private const val MODEL_ASSET = "plate_detector.tflite"
        private const val RECORD_CONFIDENCE = 0.12f
        private const val SIZE_UPDATE_ALPHA = 0.08f
        const val PLATE_PLACEHOLDER = "PLATE"

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
