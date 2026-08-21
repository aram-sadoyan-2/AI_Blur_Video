package com.naiyados.aiblurvideo.autoplate.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object FastStackBlur {

    /**
     * Applies a high-quality, creamy Gaussian StackBlur to the given Bitmap.
     * [strength] is in the range 0.0f .. 1.0f.
     */
    fun blur(source: Bitmap, strength: Float): Bitmap {
        if (strength <= 0.01f) {
            return if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        }

        // Map strength (0.0 .. 1.0) to an effective blur radius (e.g. 4 .. 80 px)
        val targetRadius = (strength * 60f).roundToInt().coerceIn(3, 90)

        // Downscale slightly for performance while maintaining crisp full-frame geometry
        val scaleFactor = when {
            targetRadius >= 35 -> 4
            targetRadius >= 15 -> 2
            else -> 1
        }

        val effectiveRadius = (targetRadius / scaleFactor).coerceAtLeast(2)

        val workingBitmap: Bitmap
        val shouldRecycleWorking: Boolean
        if (scaleFactor > 1) {
            val smallW = max(4, source.width / scaleFactor)
            val smallH = max(4, source.height / scaleFactor)
            workingBitmap = Bitmap.createScaledBitmap(source, smallW, smallH, true)
            shouldRecycleWorking = true
        } else {
            workingBitmap = if (source.isMutable) source.copy(Bitmap.Config.ARGB_8888, true) else source.copy(Bitmap.Config.ARGB_8888, true)
            shouldRecycleWorking = false
        }

        val blurredSmall = stackBlurInPlace(workingBitmap, effectiveRadius)

        val output: Bitmap
        if (scaleFactor > 1) {
            output = Bitmap.createScaledBitmap(blurredSmall, source.width, source.height, true)
            if (shouldRecycleWorking) {
                blurredSmall.recycle()
            }
        } else {
            output = blurredSmall
        }

        return output
    }

    /**
     * Blurs a specific sub-region defined by [rect] with high-quality Gaussian StackBlur.
     */
    fun blurRegion(
        source: Bitmap,
        rect: RectF,
        strength: Float
    ): Bitmap {
        val output = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val left = rect.left.roundToInt().coerceIn(0, output.width - 1)
        val top = rect.top.roundToInt().coerceIn(0, output.height - 1)
        val right = rect.right.roundToInt().coerceIn(left + 1, output.width)
        val bottom = rect.bottom.roundToInt().coerceIn(top + 1, output.height)

        val regionWidth = right - left
        val regionHeight = bottom - top
        if (regionWidth < 4 || regionHeight < 4) {
            return output
        }

        val crop = Bitmap.createBitmap(output, left, top, regionWidth, regionHeight)
        val blurredCrop = blur(crop, strength)

        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(blurredCrop, left.toFloat(), top.toFloat(), paint)

        if (blurredCrop != crop) {
            blurredCrop.recycle()
        }
        crop.recycle()

        return output
    }

    /**
     * In-place StackBlur on a mutable Bitmap.
     * Implementation of Mario Klingemann's StackBlur algorithm in pure Kotlin.
     */
    fun stackBlurInPlace(bitmap: Bitmap, radius: Int): Bitmap {
        val rad = radius.coerceIn(1, 120)
        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = rad + rad + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val a = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = rad + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var aoutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        var ainsum: Int

        for (yIdx in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            ainsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            aoutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0
            for (iIdx in -rad..rad) {
                p = pix[yi + min(wm, max(iIdx, 0))]
                sir = stack[iIdx + rad]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                sir[3] = (p ushr 24)

                rbs = r1 - kotlin.math.abs(iIdx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                asum += sir[3] * rbs
                if (iIdx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    ainsum += sir[3]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    aoutsum += sir[3]
                }
            }
            stackpointer = rad

            for (xIdx in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                a[yi] = dv[asum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - rad + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (yIdx == 0) {
                    vmin[xIdx] = min(xIdx + rad + 1, wm)
                }
                p = pix[yw + vmin[xIdx]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                sir[3] = (p ushr 24)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                aoutsum += sir[3]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                ainsum -= sir[3]

                yi++
            }
            yw += w
        }

        for (xIdx in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            ainsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            aoutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0
            yp = -rad * w
            for (iIdx in -rad..rad) {
                yi = max(0, yp) + xIdx
                sir = stack[iIdx + rad]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                sir[3] = a[yi]

                rbs = r1 - kotlin.math.abs(iIdx)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                asum += a[yi] * rbs

                if (iIdx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    ainsum += sir[3]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    aoutsum += sir[3]
                }

                if (iIdx < hm) {
                    yp += w
                }
            }
            yi = xIdx
            stackpointer = rad
            for (yIdx in 0 until h) {
                val alpha = dv[asum].coerceIn(0, 255)
                val red = dv[rsum].coerceIn(0, 255)
                val green = dv[gsum].coerceIn(0, 255)
                val blue = dv[bsum].coerceIn(0, 255)

                pix[yi] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - rad + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (xIdx == 0) {
                    vmin[yIdx] = min(yIdx + r1, hm) * w
                }
                p = xIdx + vmin[yIdx]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                sir[3] = a[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                aoutsum += sir[3]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                ainsum -= sir[3]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
