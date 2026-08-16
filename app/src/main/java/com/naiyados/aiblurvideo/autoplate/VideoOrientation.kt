package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoOrientation {

    data class Info(
        val rotationDegrees: Int,
        val storageWidth: Int,
        val storageHeight: Int
    )

    fun load(context: Context, uri: Uri): Info {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)

        var rotation = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )?.toIntOrNull() ?: 0

        var width = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
        )?.toIntOrNull() ?: 0

        var height = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
        )?.toIntOrNull() ?: 0

        retriever.release()

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue

                if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                    rotation = format.getInteger(MediaFormat.KEY_ROTATION)
                }
                if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                }
                if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                }
                break
            }
            extractor.release()
        } catch (_: Exception) {
            // Retriever values are enough.
        }

        return Info(
            rotationDegrees = rotation,
            storageWidth = width.coerceAtLeast(1),
            storageHeight = height.coerceAtLeast(1)
        )
    }

    fun displaySize(sourceWidth: Int, sourceHeight: Int, rotationDegrees: Int): Pair<Int, Int> {
        return if (rotationDegrees == 90 || rotationDegrees == 270) {
            sourceHeight to sourceWidth
        } else {
            sourceWidth to sourceHeight
        }
    }

    /** Same matrix for bitmap draw and rect mapping. */
    fun displayMatrix(sourceWidth: Int, sourceHeight: Int, rotationDegrees: Int): Matrix {
        val matrix = Matrix()
        when (rotationDegrees) {
            90 -> {
                matrix.setRotate(90f)
                matrix.postTranslate(sourceHeight.toFloat(), 0f)
            }
            180 -> {
                matrix.setRotate(180f)
                matrix.postTranslate(sourceWidth.toFloat(), sourceHeight.toFloat())
            }
            270 -> {
                matrix.setRotate(270f)
                matrix.postTranslate(0f, sourceWidth.toFloat())
            }
        }
        return matrix
    }

    fun toDisplayBitmap(source: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return source

        val (outW, outH) = displaySize(source.width, source.height, rotationDegrees)
        val matrix = displayMatrix(source.width, source.height, rotationDegrees)
        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        if (output !== source) {
            source.recycle()
        }
        return output
    }

    fun mapRectToDisplay(
        rect: RectF,
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegrees: Int
    ): RectF {
        if (rotationDegrees == 0) return RectF(rect)

        val matrix = displayMatrix(sourceWidth, sourceHeight, rotationDegrees)
        val mapped = RectF(rect)
        matrix.mapRect(mapped)
        return mapped
    }

    /** Scan boxes may use slightly different frame size than this export frame. */
    fun scaleRectToFrame(
        rect: RectF,
        fromWidth: Int,
        fromHeight: Int,
        toWidth: Int,
        toHeight: Int
    ): RectF {
        if (fromWidth == toWidth && fromHeight == toHeight) return RectF(rect)
        if (fromWidth <= 0 || fromHeight <= 0) return RectF(rect)

        val sx = toWidth.toFloat() / fromWidth.toFloat()
        val sy = toHeight.toFloat() / fromHeight.toFloat()
        return RectF(
            rect.left * sx,
            rect.top * sy,
            rect.right * sx,
            rect.bottom * sy
        )
    }
}
