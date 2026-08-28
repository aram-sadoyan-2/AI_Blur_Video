package com.naiyados.aiblurvideo.ui.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class VideoFilter(
    val id: String,
    val title: String,
    val description: String
) {
    NONE(
        id = "none",
        title = "Normal",
        description = "Original natural video colors"
    ),
    CINEMATIC(
        id = "cinematic",
        title = "Cinema",
        description = "Teal & Orange Hollywood film grade"
    ),
    NOIR(
        id = "noir",
        title = "Noir B&W",
        description = "High contrast classic black & white"
    ),
    VIBRANT(
        id = "vibrant",
        title = "Vibrant",
        description = "Rich, vivid colors and enhanced clarity"
    ),
    VINTAGE(
        id = "vintage",
        title = "Vintage",
        description = "Warm golden hour nostalgic tones"
    ),
    COOL_ICE(
        id = "cool_ice",
        title = "Cool Ice",
        description = "Crisp Nordic icy blue tint"
    ),
    NIGHT_VISION(
        id = "night_vision",
        title = "Night Vision",
        description = "Phosphor green surveillance look"
    ),
    SEPIA(
        id = "sepia",
        title = "Sepia",
        description = "Antique timeless photographic warm sepia"
    );

    fun createColorMatrix(intensity: Float = 1.0f): ColorMatrix {
        val clamped = intensity.coerceIn(0f, 1f)
        val matrix = ColorMatrix()

        when (this) {
            NONE -> {
                // Identity
            }
            CINEMATIC -> {
                // Boost blues/cyans in shadows and warm reds/oranges in highlights
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0.05f, 0.00f, 0f, 10f * clamped,
                        0.00f, 1.05f, 0.05f, 0f, 0f,
                        -0.05f, 0.10f, 1.25f, 0f, 18f * clamped,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                matrix.set(cm)
            }
            NOIR -> {
                // High-contrast Grayscale
                val cm = ColorMatrix()
                cm.setSaturation(1f - clamped)
                // Boost contrast
                val contrast = 1f + 0.35f * clamped
                val translate = (-0.5f * contrast + 0.5f) * 255f * clamped
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                matrix.set(cm)
            }
            VIBRANT -> {
                val cm = ColorMatrix()
                cm.setSaturation(1f + 0.40f * clamped)
                val contrast = 1f + 0.15f * clamped
                val translate = (-0.5f * contrast + 0.5f) * 255f * clamped
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                matrix.set(cm)
            }
            VINTAGE -> {
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.20f, 0.05f, 0.00f, 0f, 22f * clamped,
                        0.05f, 1.10f, 0.00f, 0f, 12f * clamped,
                        0.00f, 0.00f, 0.85f, 0f, -15f * clamped,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                matrix.set(cm)
            }
            COOL_ICE -> {
                val cm = ColorMatrix(
                    floatArrayOf(
                        0.85f, 0.00f, 0.00f, 0f, -15f * clamped,
                        0.00f, 1.05f, 0.05f, 0f, 5f * clamped,
                        0.05f, 0.10f, 1.35f, 0f, 30f * clamped,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                matrix.set(cm)
            }
            NIGHT_VISION -> {
                val cm = ColorMatrix(
                    floatArrayOf(
                        0.10f, 0.20f, 0.05f, 0f, 0f,
                        0.30f, 1.60f, 0.30f, 0f, 40f * clamped,
                        0.05f, 0.20f, 0.10f, 0f, 0f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                matrix.set(cm)
            }
            SEPIA -> {
                val cm = ColorMatrix(
                    floatArrayOf(
                        0.393f + 0.607f * (1f - clamped), 0.769f * clamped, 0.189f * clamped, 0f, 0f,
                        0.349f * clamped, 0.686f + 0.314f * (1f - clamped), 0.168f * clamped, 0f, 0f,
                        0.272f * clamped, 0.534f * clamped, 0.131f + 0.869f * (1f - clamped), 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.set(cm)
            }
        }

        return matrix
    }

    fun applyToBitmap(bitmap: Bitmap, intensity: Float = 1.0f): Bitmap {
        if (this == NONE || intensity <= 0.01f) return bitmap
        val colorMatrix = createColorMatrix(intensity)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }
}
