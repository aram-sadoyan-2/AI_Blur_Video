package com.naiyados.aiblurvideo.autoplate.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.AutoPlateTimeline
import com.naiyados.aiblurvideo.autoplate.VideoOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

class AutoPlateVideoExporter(
    private val context: Context
) {
    data class ExportResult(
        val outputUri: Uri,
        val frameCount: Int,
        val blurredFrames: Int
    )

    private data class AudioSample(
        val data: ByteBuffer,
        val presentationTimeUs: Long,
        val flags: Int
    )

    suspend fun export(
        inputUri: Uri,
        timeline: AutoPlateTimeline,
        durationMs: Long,
        blurStrength: Float = 0.65f,
        onProgress: (Float) -> Unit = {}
    ): ExportResult = withContext(Dispatchers.IO) {
        val orientation = VideoOrientation.load(context, inputUri)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, inputUri)

        val fileDurationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: durationMs

        val exportDurationMs = max(durationMs, fileDurationMs)
        val frameRate = 24
        val frameStepUs = 1_000_000L / frameRate
        val totalFrames = max(1, (exportDurationMs * frameRate / 1000).toInt())

        val probeRaw = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: throw IllegalStateException("Could not read video frame")

        val rotation = orientation.rotationDegrees
        val probe = VideoOrientation.toDisplayBitmap(probeRaw, rotation)
        val encodeWidth = probe.width
        val encodeHeight = probe.height
        probe.recycle()

        Log.d(
            TAG,
            "Export rotation=$rotation storage=${orientation.storageWidth}x${orientation.storageHeight} " +
                "encode=${encodeWidth}x$encodeHeight durationMs=$exportDurationMs"
        )

        val cacheFile = File(context.cacheDir, "plate_export_${System.currentTimeMillis()}.mp4")
        val (audioFormat, audioSamples) = extractAudio(inputUri)

        val muxer = MediaMuxer(cacheFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val videoEncoder = BitmapVideoEncoder(
            width = encodeWidth,
            height = encodeHeight,
            frameRate = frameRate,
            muxer = muxer
        )

        var frameIndex = 0
        var blurredFrames = 0
        var timeUs = 0L
        var muxerStarted = false
        var loggedSampleRect = false

        while (timeUs <= exportDurationMs * 1000L && frameIndex < totalFrames) {
            val timeMs = timeUs / 1000L
            val rawFrame = retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST
            )

            if (rawFrame != null) {
                val storageW = rawFrame.width
                val storageH = rawFrame.height

                val plateBox = PlateBitmapBlur.plateBoxAt(timeline, timeMs)
                val plateRect = plateBox?.let { box ->
                    val inStorage = VideoOrientation.scaleRectToFrame(
                        rect = box.rect,
                        fromWidth = box.frameWidth,
                        fromHeight = box.frameHeight,
                        toWidth = storageW,
                        toHeight = storageH
                    )
                    VideoOrientation.mapRectToDisplay(
                        rect = inStorage,
                        sourceWidth = storageW,
                        sourceHeight = storageH,
                        rotationDegrees = rotation
                    )
                }

                val frame = VideoOrientation.toDisplayBitmap(rawFrame, rotation)

                if (!loggedSampleRect && plateRect != null) {
                    loggedSampleRect = true
                    Log.d(
                        TAG,
                        "Sample blur rect=$plateRect on ${frame.width}x${frame.height} at ${timeMs}ms " +
                            "box=${plateBox?.rect} boxFrame=${plateBox?.frameWidth}x${plateBox?.frameHeight}"
                    )
                }

                val toEncode = if (plateRect != null && isRectOnFrame(plateRect, frame.width, frame.height)) {
                    blurredFrames++
                    val blurred = PlateBitmapBlur.blurPlateRegion(frame, plateRect, blurStrength)
                    PlateBitmapBlur.drawCoverOn(blurred, plateRect)
                    blurred
                } else {
                    if (frameIndex < 5) {
                        Log.d(
                            TAG,
                            "Skip blur at ${timeMs}ms box=${plateBox != null} rect=$plateRect"
                        )
                    }
                    frame.copy(Bitmap.Config.ARGB_8888, true)
                }

                if (toEncode !== frame) {
                    frame.recycle()
                }

                videoEncoder.encodeFrame(toEncode, timeUs)
                toEncode.recycle()

                if (!muxerStarted && videoEncoder.videoTrackIndex >= 0) {
                    startMuxer(muxer, videoEncoder, audioFormat, audioSamples)
                    muxerStarted = true
                }

                frameIndex++
            }

            onProgress((frameIndex.toFloat() / totalFrames).coerceIn(0f, 0.99f))
            timeUs += frameStepUs
        }

        check(muxerStarted) { "Video encoder produced no output" }

        videoEncoder.finish(endTimeUs = timeUs)
        muxer.stop()
        muxer.release()
        retriever.release()

        val outputUri = publishToGallery(cacheFile)
        onProgress(1f)

        Log.d(
            TAG,
            "Export done frames=$frameIndex blurred=$blurredFrames uri=$outputUri"
        )

        ExportResult(
            outputUri = outputUri,
            frameCount = frameIndex,
            blurredFrames = blurredFrames
        )
    }

    private fun isRectOnFrame(rect: RectF, frameW: Int, frameH: Int): Boolean {
        val overlapW = (rect.right.coerceAtMost(frameW.toFloat()) -
            rect.left.coerceAtLeast(0f)).coerceAtLeast(0f)
        val overlapH = (rect.bottom.coerceAtMost(frameH.toFloat()) -
            rect.top.coerceAtLeast(0f)).coerceAtLeast(0f)
        return overlapW >= 8f && overlapH >= 8f
    }

    private fun startMuxer(
        muxer: MediaMuxer,
        videoEncoder: BitmapVideoEncoder,
        audioFormat: MediaFormat?,
        audioSamples: List<AudioSample>
    ) {
        if (audioFormat != null) {
            val audioTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()
            videoEncoder.startMuxerWriting()

            val bufferInfo = MediaCodec.BufferInfo()
            audioSamples.forEach { sample ->
                bufferInfo.set(
                    0,
                    sample.data.remaining(),
                    sample.presentationTimeUs,
                    sample.flags
                )
                muxer.writeSampleData(audioTrackIndex, sample.data, bufferInfo)
            }
        } else {
            muxer.start()
            videoEncoder.startMuxerWriting()
        }
    }

    private fun extractAudio(inputUri: Uri): Pair<MediaFormat?, List<AudioSample>> {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, inputUri, null)

        var audioTrack = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrack = i
                break
            }
        }

        if (audioTrack < 0) {
            extractor.release()
            return null to emptyList()
        }

        extractor.selectTrack(audioTrack)
        val format = extractor.getTrackFormat(audioTrack)
        val samples = mutableListOf<AudioSample>()
        val buffer = ByteBuffer.allocate(512 * 1024)

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            val sampleData = ByteBuffer.allocate(sampleSize)
            buffer.position(0)
            buffer.limit(sampleSize)
            sampleData.put(buffer)
            sampleData.flip()
            samples += AudioSample(
                data = sampleData,
                presentationTimeUs = extractor.sampleTime,
                flags = extractor.sampleFlags
            )
            buffer.clear()
            extractor.advance()
        }

        extractor.release()
        return format to samples
    }

    private fun publishToGallery(cacheFile: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, cacheFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IllegalStateException("Could not create gallery entry")

        context.contentResolver.openOutputStream(uri)?.use { output ->
            cacheFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not write exported video")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        cacheFile.delete()
        return uri
    }

    companion object {
        private const val TAG = "AutoPlateExport"
    }
}
