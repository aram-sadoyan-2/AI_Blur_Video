package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF
import kotlin.math.abs

object PlateScoring {

    fun score(box: AutoPlateBox): Float {
        val width = box.rect.width().coerceAtLeast(1f)
        val height = box.rect.height().coerceAtLeast(1f)
        val frameArea = box.frameWidth.toFloat() * box.frameHeight.coerceAtLeast(1)
        val areaRatio = (width * height) / frameArea
        val centerYRatio = box.rect.centerY() / box.frameHeight.coerceAtLeast(1).toFloat()

        return scoreRaw(
            text = box.text,
            rectWidth = width,
            rectHeight = height,
            centerYRatio = centerYRatio,
            areaRatio = areaRatio
        )
    }

    fun scoreRaw(
        text: String,
        rectWidth: Float,
        rectHeight: Float,
        centerYRatio: Float,
        areaRatio: Float
    ): Float {
        if (text.length !in 5..9) return -100f
        if (!text.all { it.isLetterOrDigit() }) return -100f

        val digits = text.count { it.isDigit() }
        val letters = text.count { it.isLetter() }
        val length = text.length.coerceAtLeast(1)
        val digitRatio = digits.toFloat() / length
        val aspectRatio = rectWidth / rectHeight.coerceAtLeast(1f)

        var score = 0f

        // Strong priority for UK style: SN66XHZ
        if (isUkPlate(text)) {
            score += 120f
        }

        // General plate-like patterns.
        if (isGeneralPlateLike(text)) {
            score += 45f
        }

        // Penalize model/year-like text: RCH2023, ABC2024
        if (Regex("^[A-Z]{3,5}\\d{4}$").matches(text)) {
            score -= 90f
        }

        // Penalize all letters or letter-heavy codes.
        if (digits < 2) score -= 60f
        if (letters < 2) score -= 40f

        score += digits * 2.4f
        score += letters * 0.9f

        score += when {
            digitRatio in 0.28f..0.55f -> 18f
            digitRatio in 0.20f..0.70f -> 6f
            else -> -15f
        }

        score += when {
            aspectRatio in 3.0f..7.2f -> 25f
            aspectRatio in 2.3f..9.5f -> 8f
            else -> -40f
        }

        score += when {
            centerYRatio < 0.40f -> -40f
            centerYRatio in 0.48f..0.92f -> 18f
            centerYRatio <= 0.96f -> 4f
            else -> -8f
        }

        score += when {
            areaRatio < 0.00030f -> -50f
            areaRatio in 0.00030f..0.03000f -> 8f
            else -> -12f
        }

        return score
    }

    fun textConsistencyRatio(track: List<AutoPlateBox>): Float {
        if (track.isEmpty()) return 0f
        val dominant = dominantText(track) ?: return 0f
        val matches = track.count {
            normalizeSimilarPlateText(it.text) == dominant
        }
        return matches.toFloat() / track.size.toFloat()
    }

    fun textConsistencyBonus(track: List<AutoPlateBox>): Float {
        if (track.isEmpty()) return 0f

        val normalizedTexts = track.map { normalizeSimilarPlateText(it.text) }

        val dominantCount = normalizedTexts
            .groupingBy { it }
            .eachCount()
            .maxOfOrNull { it.value }
            ?: 0

        return dominantCount.toFloat() / track.size.toFloat() * 100f
    }

    fun dominantText(track: List<AutoPlateBox>): String? {
        if (track.isEmpty()) return null

        return track
            .groupingBy { normalizeSimilarPlateText(it.text) }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    fun normalizeSimilarPlateText(text: String): String {
        return text
            .uppercase()
            .replace("0", "O")
            .replace("1", "I")
            .replace("5", "S")
            .replace("8", "B")
    }

    fun isOutlierJump(
        previous: RectF,
        next: RectF
    ): Boolean {
        val dx = abs(previous.centerX() - next.centerX())
        val dy = abs(previous.centerY() - next.centerY())

        val averageWidth = (previous.width() + next.width()) / 2f
        val averageHeight = (previous.height() + next.height()) / 2f

        return dx > averageWidth * 2.2f || dy > averageHeight * 2.6f
    }

    private fun isUkPlate(value: String): Boolean {
        // Example: SN66XHZ
        return Regex("^[A-Z]{2}\\d{2}[A-Z]{3}$").matches(value)
    }

    private fun isGeneralPlateLike(value: String): Boolean {
        return listOf(
            Regex("^[A-Z]{1,3}\\d{2,4}[A-Z]{1,3}$"),
            Regex("^\\d{2,4}[A-Z]{1,3}\\d{1,3}$"),
            Regex("^[A-Z]{1,2}\\d{3,4}[A-Z]{1,2}$")
        ).any { it.matches(value) }
    }
}