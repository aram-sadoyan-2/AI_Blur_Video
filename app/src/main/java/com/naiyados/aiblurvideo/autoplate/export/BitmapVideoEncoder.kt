package com.naiyados.aiblurvideo.autoplate.export

import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import java.nio.ByteBuffer

internal class BitmapVideoEncoder(
    private val width: Int,
    private val height: Int,
    private val frameRate: Int,
    private val muxer: MediaMuxer,
    private val bitRate: Int = 8_000_000
) {
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    private val inputSurface: Surface
    private val bufferInfo = MediaCodec.BufferInfo()
    private val pendingSamples = mutableListOf<EncodedSample>()

    var videoTrackIndex: Int = -1
        private set

    private var muxerStarted = false

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        codec.start()
    }

    fun encodeFrame(bitmap: Bitmap, presentationTimeUs: Long) {
        val canvas = inputSurface.lockHardwareCanvas()
        try {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            if (bitmap.width == width && bitmap.height == height) {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } else {
                canvas.drawColor(android.graphics.Color.BLACK)
                val scale = minOf(
                    width.toFloat() / bitmap.width,
                    height.toFloat() / bitmap.height
                )
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val left = (width - drawW) / 2f
                val top = (height - drawH) / 2f
                canvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(left, top, left + drawW, top + drawH),
                    paint
                )
            }
        } finally {
            inputSurface.unlockCanvasAndPost(canvas)
        }

        drainEncoder(endOfStream = false, presentationTimeUs)
    }

    fun startMuxerWriting() {
        muxerStarted = true
        flushPending()
    }

    fun finish(endTimeUs: Long) {
        codec.signalEndOfInputStream()
        drainEncoder(endOfStream = true, presentationTimeUs = endTimeUs)
        flushPending()
        codec.stop()
        codec.release()
        inputSurface.release()
    }

    private fun drainEncoder(endOfStream: Boolean, presentationTimeUs: Long) {
        while (true) {
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(videoTrackIndex == -1) { "Video format changed twice" }
                    videoTrackIndex = muxer.addTrack(codec.outputFormat)
                }
                encoderStatus >= 0 -> {
                    val encodedData = codec.getOutputBuffer(encoderStatus)
                        ?: throw IllegalStateException("Encoder output buffer was null")

                    if (bufferInfo.size > 0) {
                        val copy = ByteBuffer.allocate(bufferInfo.size)
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        copy.put(encodedData)
                        copy.flip()

                        val info = MediaCodec.BufferInfo().apply {
                            set(0, bufferInfo.size, presentationTimeUs, bufferInfo.flags)
                        }
                        pendingSamples += EncodedSample(copy, info)
                        if (muxerStarted) {
                            flushPending()
                        }
                    }

                    codec.releaseOutputBuffer(encoderStatus, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
            }
        }
    }

    private fun flushPending() {
        if (!muxerStarted || videoTrackIndex < 0) return
        pendingSamples.forEach { sample ->
            muxer.writeSampleData(videoTrackIndex, sample.data, sample.info)
        }
        pendingSamples.clear()
    }

    private data class EncodedSample(
        val data: ByteBuffer,
        val info: MediaCodec.BufferInfo
    )
}
