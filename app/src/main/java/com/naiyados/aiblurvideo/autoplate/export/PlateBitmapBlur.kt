package com.naiyados.aiblurvideo.autoplate.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.naiyados.aiblurvideo.autoplate.AutoPlateBox
import com.naiyados.aiblurvideo.autoplate.AutoPlateTimeline
import com.naiyados.aiblurvideo.autoplate.PlateMaskInsets
import kotlin.math.max
import kotlin.math.roundToInt

object PlateBitmapBlur {

    fun plateBoxAt(timeline: AutoPlateTimeline, timeMs: Long): AutoPlateBox? {
        return timeline.boxesAt(timeMs).firstOrNull()
    }

    fun blurPlateRegion(
        source: Bitmap,
        plateRect: RectF,
        strength: Float = 0.65f
    ): Bitmap {
        val cover = PlateMaskInsets.paddingForCover(plateRect)
        return FastStackBlur.blurRegion(source, cover, strength)
    }

    /** Opaque black on top of blur so the plate is definitely hidden in export. */
    fun drawCoverOn(bitmap: Bitmap, plateRect: RectF) {
        val cover = PlateMaskInsets.paddingForCover(plateRect)
        Canvas(bitmap).drawRoundRect(
            cover,
            8f,
            8f,
            Paint().apply { color = Color.BLACK }
        )
    }

    private fun pixelate(source: Bitmap, scale: Int): Bitmap {
        val smallW = max(1, source.width / scale)
        val smallH = max(1, source.height / scale)
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        return Bitmap.createScaledBitmap(small, source.width, source.height, false).also {
            if (small != source) small.recycle()
        }
    }

    private fun boxBlur(source: Bitmap, passes: Int): Bitmap {
        var current = source
        repeat(passes) {
            val next = current.copy(Bitmap.Config.ARGB_8888, true)
            boxBlurPass(current, next, radius = 3)
            if (current != source) current.recycle()
            current = next
        }
        return current
    }

    private fun boxBlurPass(input: Bitmap, output: Bitmap, radius: Int) {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val horizontal = IntArray(pixels.size)
        val window = radius * 2 + 1

        for (y in 0 until height) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            val row = y * width

            for (i in -radius..radius) {
                val pixel = pixels[row + i.coerceIn(0, width - 1)]
                sumA += pixel ushr 24 and 0xFF
                sumR += pixel shr 16 and 0xFF
                sumG += pixel shr 8 and 0xFF
                sumB += pixel and 0xFF
            }

            for (x in 0 until width) {
                val left = (x - radius - 1).coerceAtLeast(0)
                val right = (x + radius).coerceAtMost(width - 1)
                val outLeft = pixels[row + left]
                val inRight = pixels[row + right]

                sumA += (inRight ushr 24 and 0xFF) - (outLeft ushr 24 and 0xFF)
                sumR += (inRight shr 16 and 0xFF) - (outLeft shr 16 and 0xFF)
                sumG += (inRight shr 8 and 0xFF) - (outLeft shr 8 and 0xFF)
                sumB += (inRight and 0xFF) - (outLeft and 0xFF)

                horizontal[row + x] =
                    (sumA / window shl 24) or
                    (sumR / window shl 16) or
                    (sumG / window shl 8) or
                    (sumB / window)
            }
        }

        for (x in 0 until width) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -radius..radius) {
                val y = i.coerceIn(0, height - 1)
                val pixel = horizontal[y * width + x]
                sumA += pixel ushr 24 and 0xFF
                sumR += pixel shr 16 and 0xFF
                sumG += pixel shr 8 and 0xFF
                sumB += pixel and 0xFF
            }

            for (y in 0 until height) {
                val top = (y - radius - 1).coerceAtLeast(0)
                val bottom = (y + radius).coerceAtMost(height - 1)
                val outTop = horizontal[top * width + x]
                val inBottom = horizontal[bottom * width + x]

                sumA += (inBottom ushr 24 and 0xFF) - (outTop ushr 24 and 0xFF)
                sumR += (inBottom shr 16 and 0xFF) - (outTop shr 16 and 0xFF)
                sumG += (inBottom shr 8 and 0xFF) - (outTop shr 8 and 0xFF)
                sumB += (inBottom and 0xFF) - (outTop and 0xFF)

                pixels[y * width + x] =
                    (sumA / window shl 24) or
                    (sumR / window shl 16) or
                    (sumG / window shl 8) or
                    (sumB / window)
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
