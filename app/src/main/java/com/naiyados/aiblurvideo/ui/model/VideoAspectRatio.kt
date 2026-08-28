package com.naiyados.aiblurvideo.ui.model

enum class VideoAspectRatio(
    val id: String,
    val title: String,
    val ratioValue: Float?, // null = original
    val widthRatio: Int,
    val heightRatio: Int
) {
    FREEFORM("freeform", "Freeform", null, 0, 0),
    ORIGINAL("original", "Original", null, 0, 0),
    PORTRAIT_9_16("9:16", "9:16 Reels/TikTok", 9f / 16f, 9, 16),
    LANDSCAPE_16_9("16:9", "16:9 YouTube", 16f / 9f, 16, 9),
    SQUARE_1_1("1:1", "1:1 Square", 1f, 1, 1),
    FEED_4_5("4:5", "4:5 Instagram", 4f / 5f, 4, 5),
    PORTRAIT_3_4("3:4", "3:4 Portrait", 3f / 4f, 3, 4),
    LANDSCAPE_4_3("4:3", "4:3 Standard", 4f / 3f, 4, 3),
    CINEMATIC_21_9("21:9", "21:9 Cinema", 21f / 9f, 21, 9);

    fun calculateCropRect(sourceWidth: Int, sourceHeight: Int): android.graphics.RectF {
        if (ratioValue == null || sourceWidth <= 0 || sourceHeight <= 0) {
            return android.graphics.RectF(0f, 0f, sourceWidth.toFloat(), sourceHeight.toFloat())
        }

        val targetRatio = ratioValue
        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()

        return if (sourceRatio > targetRatio) {
            // Source is wider than target -> Crop horizontal sides
            val targetW = sourceHeight * targetRatio
            val left = (sourceWidth - targetW) / 2f
            android.graphics.RectF(left, 0f, left + targetW, sourceHeight.toFloat())
        } else {
            // Source is taller than target -> Crop top and bottom
            val targetH = sourceWidth / targetRatio
            val top = (sourceHeight - targetH) / 2f
            android.graphics.RectF(0f, top, sourceWidth.toFloat(), top + targetH)
        }
    }

    fun calculateNormalizedCropRect(sourceWidth: Int, sourceHeight: Int): android.graphics.RectF {
        if (ratioValue == null || sourceWidth <= 0 || sourceHeight <= 0) {
            return android.graphics.RectF(0f, 0f, 1f, 1f)
        }
        val pixelRect = calculateCropRect(sourceWidth, sourceHeight)
        return android.graphics.RectF(
            pixelRect.left / sourceWidth.toFloat(),
            pixelRect.top / sourceHeight.toFloat(),
            pixelRect.right / sourceWidth.toFloat(),
            pixelRect.bottom / sourceHeight.toFloat()
        )
    }

    fun getOutputDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        if (sourceWidth <= 0 || sourceHeight <= 0) return Pair(0, 0)
        val rect = calculateCropRect(sourceWidth, sourceHeight)
        val outW = (rect.width().toInt() / 2) * 2
        val outH = (rect.height().toInt() / 2) * 2
        return Pair(outW, outH)
    }
}
