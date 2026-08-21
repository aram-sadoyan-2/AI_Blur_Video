package com.naiyados.aiblurvideo.ui.model

enum class VideoAspectRatio(
    val id: String,
    val title: String,
    val ratioValue: Float?, // null = original
    val widthRatio: Int,
    val heightRatio: Int
) {
    ORIGINAL("original", "Original", null, 0, 0),
    PORTRAIT_9_16("9:16", "9:16 Reels/TikTok", 9f / 16f, 9, 16),
    LANDSCAPE_16_9("16:9", "16:9 YouTube/Cinema", 16f / 9f, 16, 9),
    SQUARE_1_1("1:1", "1:1 Square", 1f, 1, 1),
    FEED_4_5("4:5", "4:5 Instagram Feed", 4f / 5f, 4, 5);

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
}
