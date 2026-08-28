package com.naiyados.aiblurvideo.autoplate.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.naiyados.aiblurvideo.autoplate.AutoPlateTimeline
import com.naiyados.aiblurvideo.autoplate.PlateMaskInsets
import com.naiyados.aiblurvideo.autoplate.face.FaceBlurDetector
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.model.VideoEditConfig
import com.naiyados.aiblurvideo.ui.model.VideoFilter
import kotlin.math.max
import kotlin.math.roundToInt

object FrameEffectProcessor {

    private var lastFaceDetectTimeMs = -1000L
    private var cachedFaceRects: List<RectF> = emptyList()

    fun resetFaceCache() {
        lastFaceDetectTimeMs = -1000L
        cachedFaceRects = emptyList()
    }

    suspend fun processFrame(
        source: Bitmap,
        timeMs: Long,
        config: VideoEditConfig,
        timeline: AutoPlateTimeline? = null
    ): Bitmap {
        var current = source

        // 1. Apply Aspect Ratio Crop or Custom Draggable Crop
        val normCrop = config.customCropRect
        val hasCustomCrop = normCrop != null && (normCrop.left > 0.005f || normCrop.top > 0.005f || normCrop.right < 0.995f || normCrop.bottom < 0.995f)

        if (hasCustomCrop && normCrop != null) {
            val left = (normCrop.left * current.width).roundToInt().coerceIn(0, current.width - 4)
            val top = (normCrop.top * current.height).roundToInt().coerceIn(0, current.height - 4)
            val rawW = (normCrop.width() * current.width).roundToInt().coerceIn(8, current.width - left)
            val rawH = (normCrop.height() * current.height).roundToInt().coerceIn(8, current.height - top)
            val width = (rawW / 2) * 2
            val height = (rawH / 2) * 2

            if (width > 8 && height > 8) {
                var cropped = Bitmap.createBitmap(current, left, top, width, height)
                if (config.customCropRotation != 0f) {
                    val matrix = Matrix().apply { postRotate(config.customCropRotation) }
                    val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
                    if (rotated != cropped) {
                        cropped.recycle()
                        cropped = rotated
                    }
                }
                if (current != source && current != cropped) {
                    current.recycle()
                }
                current = cropped
            }
        } else if (config.aspectRatio != VideoAspectRatio.ORIGINAL && config.aspectRatio != VideoAspectRatio.FREEFORM) {
            val cropRect = config.aspectRatio.calculateCropRect(current.width, current.height)
            val left = cropRect.left.roundToInt().coerceIn(0, current.width - 2)
            val top = cropRect.top.roundToInt().coerceIn(0, current.height - 2)
            val width = (cropRect.width().roundToInt() / 2) * 2
            val height = (cropRect.height().roundToInt() / 2) * 2

            if (width > 8 && height > 8 && (width < current.width || height < current.height)) {
                val cropped = Bitmap.createBitmap(
                    current,
                    left,
                    top,
                    width.coerceAtMost(current.width - left),
                    height.coerceAtMost(current.height - top)
                )
                if (current != source && current != cropped) {
                    current.recycle()
                }
                current = cropped
            }
        }

        // 2. Apply Blur / Mask Effects based on BlurMode directly on the framed video
        when (config.blurMode) {
            BlurMode.AutoPlate -> {
                val includePlates = config.isPlateBlurActive
                val includeFaces = config.isFaceBlurActive
                if ((includePlates || includeFaces) && timeline != null) {
                    val boxes = timeline.boxesAt(timeMs, includePlates = includePlates, includeFaces = includeFaces)
                    for (box in boxes) {
                        val processed = PlateBitmapBlur.blurPlateRegion(current, box.rect, config.blurStrength)
                        if (processed != current && current != source) {
                            current.recycle()
                        }
                        current = processed
                    }
                }
            }
            BlurMode.FullBlur -> {
                if (config.shouldBlurFrame(timeMs)) {
                    val blurred = FastStackBlur.blur(current, config.blurStrength)
                    if (current != source && current != blurred) current.recycle()
                    current = blurred
                }
            }
            BlurMode.Face -> {
                val timelineFaces = timeline?.faceBoxesAt(timeMs) ?: emptyList()
                if (timelineFaces.isNotEmpty()) {
                    for (box in timelineFaces) {
                        val padded = PlateMaskInsets.paddingForCover(box.rect)
                        val processed = PlateBitmapBlur.blurPlateRegion(current, padded, config.blurStrength)
                        if (processed != current && current != source) {
                            current.recycle()
                        }
                        current = processed
                    }
                } else {
                    // Throttle face detection to every ~150ms for 5x export acceleration
                    val faces = if (timeMs - lastFaceDetectTimeMs >= 150L || cachedFaceRects.isEmpty()) {
                        val detected = FaceBlurDetector.detectFaces(current)
                        if (detected.isNotEmpty() || cachedFaceRects.isEmpty()) {
                            cachedFaceRects = detected
                            lastFaceDetectTimeMs = timeMs
                        }
                        cachedFaceRects
                    } else {
                        cachedFaceRects
                    }

                    for (faceRect in faces) {
                        val padded = PlateMaskInsets.paddingForCover(faceRect)
                        val processed = PlateBitmapBlur.blurPlateRegion(current, padded, config.blurStrength)
                        if (processed != current && current != source) {
                            current.recycle()
                        }
                        current = processed
                    }
                }
            }
            BlurMode.Object -> {
                val normRect = config.customObjectNormalizedRect
                if (normRect != null) {
                    val absoluteRect = RectF(
                        normRect.left * current.width,
                        normRect.top * current.height,
                        normRect.right * current.width,
                        normRect.bottom * current.height
                    )
                    val processed = FastStackBlur.blurRotatedRegion(
                        source = current,
                        rect = absoluteRect,
                        rotationDegrees = config.customObjectRotationDegrees,
                        shape = config.customObjectShape,
                        strength = config.blurStrength
                    )
                    if (processed != current && current != source) {
                        current.recycle()
                    }
                    current = processed
                }
            }
            BlurMode.Background -> {
                val processed = applyBokehBackgroundBlur(current, config.blurStrength)
                if (current != source) current.recycle()
                current = processed
            }
            BlurMode.Pixelate -> {
                val blockSize = config.pixelateBlockSize.coerceIn(8, 64)
                val pixelated = pixelate(current, blockSize)
                if (current != source) current.recycle()
                current = pixelated
            }
            BlurMode.Effects, BlurMode.Speed, BlurMode.Crop -> {
                // Blur not active for pure color filter/speed/crop modes
            }
        }

        // 3. Apply Color Filter / Cinematic FX
        if (config.filter != VideoFilter.NONE) {
            val filtered = config.filter.applyToBitmap(current, config.filterIntensity)
            if (current != source && current != filtered) {
                current.recycle()
            }
            current = filtered
        }

        return current
    }

    fun pixelate(source: Bitmap, scale: Int): Bitmap {
        val smallW = max(1, source.width / scale)
        val smallH = max(1, source.height / scale)
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        val pixelated = Bitmap.createScaledBitmap(small, source.width, source.height, false)
        small.recycle()
        return pixelated
    }

    fun blurBitmap(source: Bitmap, blurStrength: Float): Bitmap {
        return FastStackBlur.blur(source, blurStrength)
    }

    fun fastBlur(source: Bitmap, blurStrength: Float): Bitmap {
        return FastStackBlur.blur(source, blurStrength)
    }

    fun fastBlur(source: Bitmap, scale: Int, passes: Int): Bitmap {
        val rad = (scale * passes).coerceIn(2, 80)
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        return FastStackBlur.stackBlurInPlace(copy, rad)
    }

    private fun applyBokehBackgroundBlur(source: Bitmap, strength: Float): Bitmap {
        val blurred = FastStackBlur.blur(source, strength)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw blurred background
        canvas.drawBitmap(blurred, 0f, 0f, null)
        blurred.recycle()

        // Create vignette mask for crisp center foreground
        val maskBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val cx = source.width / 2f
        val cy = source.height / 2f
        val radius = max(source.width, source.height) * (0.55f - strength * 0.15f)

        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        maskCanvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), maskPaint)

        // Draw sharp original inside the mask
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        val sharpPortion = source.copy(Bitmap.Config.ARGB_8888, true)
        val sharpCanvas = Canvas(sharpPortion)
        sharpCanvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        maskBitmap.recycle()

        canvas.drawBitmap(sharpPortion, 0f, 0f, null)
        sharpPortion.recycle()

        return output
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
            val rowOffset = y * width

            for (i in -radius..radius) {
                val clampedX = i.coerceIn(0, width - 1)
                val c = pixels[rowOffset + clampedX]
                sumA += (c ushr 24) and 0xff
                sumR += (c ushr 16) and 0xff
                sumG += (c ushr 8) and 0xff
                sumB += c and 0xff
            }

            for (x in 0 until width) {
                horizontal[rowOffset + x] =
                    ((sumA / window) shl 24) or
                            ((sumR / window) shl 16) or
                            ((sumG / window) shl 8) or
                            (sumB / window)

                val leftX = (x - radius).coerceIn(0, width - 1)
                val rightX = (x + radius + 1).coerceIn(0, width - 1)
                val leftColor = pixels[rowOffset + leftX]
                val rightColor = pixels[rowOffset + rightX]

                sumA += ((rightColor ushr 24) and 0xff) - ((leftColor ushr 24) and 0xff)
                sumR += ((rightColor ushr 16) and 0xff) - ((leftColor ushr 16) and 0xff)
                sumG += ((rightColor ushr 8) and 0xff) - ((leftColor ushr 8) and 0xff)
                sumB += (rightColor and 0xff) - (leftColor and 0xff)
            }
        }

        val outPixels = IntArray(pixels.size)
        for (x in 0 until width) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -radius..radius) {
                val clampedY = i.coerceIn(0, height - 1)
                val c = horizontal[clampedY * width + x]
                sumA += (c ushr 24) and 0xff
                sumR += (c ushr 16) and 0xff
                sumG += (c ushr 8) and 0xff
                sumB += c and 0xff
            }

            for (y in 0 until height) {
                outPixels[y * width + x] =
                    ((sumA / window) shl 24) or
                            ((sumR / window) shl 16) or
                            ((sumG / window) shl 8) or
                            (sumB / window)

                val topY = (y - radius).coerceIn(0, height - 1)
                val bottomY = (y + radius + 1).coerceIn(0, height - 1)
                val topColor = horizontal[topY * width + x]
                val bottomColor = horizontal[bottomY * width + x]

                sumA += ((bottomColor ushr 24) and 0xff) - ((topColor ushr 24) and 0xff)
                sumR += ((bottomColor ushr 16) and 0xff) - ((topColor ushr 16) and 0xff)
                sumG += ((bottomColor ushr 8) and 0xff) - ((topColor ushr 8) and 0xff)
                sumB += (bottomColor and 0xff) - (topColor and 0xff)
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
    }
}
