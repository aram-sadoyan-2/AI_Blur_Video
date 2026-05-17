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
                    candidates += AutoPlateBox(
                        timeMs = timeMs,
                        rect = RectF(rect),
                        text = text,
                        frameWidth = bitmap.width,
                        frameHeight = bitmap.height
                    )
                }
            }
        }

        // For MVP: hide only the biggest plate-like text per frame.
        // This avoids hiding car model names, stickers, small random text, etc.
        return candidates
            .sortedWith(
                compareByDescending<AutoPlateBox> {
                    it.rect.width() * it.rect.height()
                }.thenByDescending {
                    plateScore(it)
                }
            )
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
    }

    private fun isPossiblePlate(
        value: String,
        rect: Rect,
        frameWidth: Int,
        frameHeight: Int
    ): Boolean {
        if (value.length !in 5..9) return false

        val digitCount = value.count { it.isDigit() }
        val letterCount = value.count { it.isLetter() }

        if (digitCount < 2) return false
        if (letterCount < 1) return false
        if (!value.all { it.isLetterOrDigit() }) return false

        val rectWidth = rect.width().toFloat()
        val rectHeight = rect.height().toFloat()

        if (rectWidth <= 0f || rectHeight <= 0f) return false

        val aspectRatio = rectWidth / rectHeight
        val area = rectWidth * rectHeight
        val frameArea = frameWidth.toFloat() * frameHeight.toFloat()
        val areaRatio = area / frameArea

        // License plates are usually wide rectangles.
        // This removes vertical/square text like logos or model names.
        if (aspectRatio < 2.4f) return false
        if (aspectRatio > 8.8f) return false

        // Remove very tiny OCR noise.
        if (areaRatio < 0.00035f) return false

        // Usually plates are not in the very top area of the frame.
        // This removes many random upper-screen texts.
        val centerY = rect.centerY().toFloat()
        if (centerY < frameHeight * 0.32f) return false

        return true
    }

    private fun plateScore(box: AutoPlateBox): Float {
        val rect = box.rect
        val width = rect.width().coerceAtLeast(1f)
        val height = rect.height().coerceAtLeast(1f)
        val aspectRatio = width / height

        val text = box.text
        val digits = text.count { it.isDigit() }
        val letters = text.count { it.isLetter() }

        var score = 0f

        // Good plate text usually has both letters and digits.
        score += digits * 1.2f
        score += letters * 0.8f

        // Prefer plate-like wide shape.
        score += when {
            aspectRatio in 3.0f..6.5f -> 4f
            aspectRatio in 2.4f..8.8f -> 2f
            else -> -3f
        }

        // Prefer larger candidates.
        score += (width * height) / 1000f

        return score
    }
}