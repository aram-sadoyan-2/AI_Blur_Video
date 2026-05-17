package com.naiyados.aiblurvideo.autoplate

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MlKitPlateOcrDetector {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    suspend fun detect(
        bitmap: Bitmap,
        timeMs: Long
    ): List<AutoPlateBox> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()

        val candidates = mutableListOf<AutoPlateBox>()

        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val rect = line.boundingBox ?: return@forEach
                val text = cleanPlateText(line.text)

                if (
                    isPossiblePlate(
                        value = text,
                        rect = rect,
                        frameWidth = bitmap.width,
                        frameHeight = bitmap.height
                    )
                ) {
                    val score = PlateScoring.scoreRaw(
                        text = text,
                        rectWidth = rect.width().toFloat(),
                        rectHeight = rect.height().toFloat(),
                        centerYRatio = rect.centerY().toFloat() / bitmap.height,
                        areaRatio = (rect.width() * rect.height()).toFloat() /
                            (bitmap.width * bitmap.height).toFloat()
                    )
                    candidates += AutoPlateBox(
                        timeMs = timeMs,
                        rect = RectF(rect),
                        text = text,
                        frameWidth = bitmap.width,
                        frameHeight = bitmap.height,
                        confidence = (score / 40f).coerceIn(0.05f, 1f)
                    )
                }
            }
        }

        return candidates
            .sortedByDescending { PlateScoring.score(it) }
            .take(1)
    }

    private fun cleanPlateText(value: String): String {
        return value
            .uppercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace(".", "")
            .replace(":", "")
            .replace("|", "")
            .replace("/", "")
            .replace("\\", "")
            .replace("[", "")
            .replace("]", "")
            .replace("(", "")
            .replace(")", "")
            .replace("'", "")
            .replace("\"", "")
    }

    private fun isPossiblePlate(
        value: String,
        rect: Rect,
        frameWidth: Int,
        frameHeight: Int
    ): Boolean {
        if (value.length !in 5..9) return false
        if (!value.all { it.isLetterOrDigit() }) return false

        val digitCount = value.count { it.isDigit() }
        val letterCount = value.count { it.isLetter() }

        if (digitCount < 2) return false
        if (letterCount < 2) return false

        val rectWidth = rect.width().toFloat()
        val rectHeight = rect.height().toFloat()

        if (rectWidth <= 0f || rectHeight <= 0f) return false

        val aspectRatio = rectWidth / rectHeight
        val area = rectWidth * rectHeight
        val frameArea = frameWidth.toFloat() * frameHeight.toFloat()
        val areaRatio = area / frameArea
        val centerYRatio = rect.centerY().toFloat() / frameHeight.toFloat()

        // Plate text is usually wide.
        if (aspectRatio < 2.3f) return false
        if (aspectRatio > 9.5f) return false

        // Remove tiny OCR fragments.
        if (areaRatio < 0.00030f) return false

        // Usually car plate is not in the top part.
        if (centerYRatio < 0.40f) return false

        return PlateScoring.scoreRaw(
            text = value,
            rectWidth = rectWidth,
            rectHeight = rectHeight,
            centerYRatio = centerYRatio,
            areaRatio = areaRatio
        ) > 0f
    }
}