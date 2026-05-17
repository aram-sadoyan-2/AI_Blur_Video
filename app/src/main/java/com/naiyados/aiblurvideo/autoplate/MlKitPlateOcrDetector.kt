package com.naiyados.aiblurvideo.autoplate

import android.graphics.Bitmap
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

        val boxes = mutableListOf<AutoPlateBox>()

        result.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val rect = line.boundingBox ?: return@forEach
                val text = cleanPlateText(line.text)

                if (isPossiblePlate(text)) {
                    boxes += AutoPlateBox(
                        timeMs = timeMs,
                        rect = RectF(rect),
                        text = text
                    )
                }
            }
        }

        return boxes
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
    }

    private fun isPossiblePlate(value: String): Boolean {
        if (value.length !in 5..9) return false

        val digitCount = value.count { it.isDigit() }
        val letterCount = value.count { it.isLetter() }

        if (digitCount < 2) return false
        if (letterCount < 1) return false

        return value.all { it.isLetterOrDigit() }
    }
}