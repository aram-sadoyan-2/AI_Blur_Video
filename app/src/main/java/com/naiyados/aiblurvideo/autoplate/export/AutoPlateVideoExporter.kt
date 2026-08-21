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
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.model.VideoEditConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
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
        exportSettings: ExportSettings = ExportSettings(),
        onProgress: (Float) -> Unit = {}
    ): ExportResult {
        val config = VideoEditConfig(
            blurStrength = blurStrength,
            exportSettings = exportSettings
        )
        return exportWithConfig(
            inputUri = inputUri,
            config = config,
            timeline = timeline,
            durationMs = durationMs,
            onProgress = onProgress
        )
    }

    suspend fun exportWithConfig(
        inputUri: Uri,
        config: VideoEditConfig,
        timeline: AutoPlateTimeline? = null,
        durationMs: Long = 0L,
        onProgress: (Float) -> Unit = {}
    ): ExportResult = withContext(Dispatchers.IO) {
        val orientation = VideoOrientation.load(context, inputUri)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, inputUri)

        val fileDurationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: durationMs

        val fullDurationMs = max(durationMs, fileDurationMs)
        val startMs = config.trimStartMs.coerceIn(0L, fullDurationMs)
        val endMs = if (config.trimEndMs > startMs) {
            config.trimEndMs.coerceAtMost(fullDurationMs)
        } else {
            fullDurationMs
        }
        val targetDurationMs = max(500L, endMs - startMs)

        val frameRate = 24
        // Speed affects how fast we step through the source video vs output timestamps
        val speedFactor = config.playbackSpeed.coerceIn(0.25f, 4.0f)
        val sourceStepUs = (1_000_000L / frameRate * speedFactor).toLong()
        val totalFrames = max(1, ((targetDurationMs / speedFactor) * frameRate / 1000).toInt())

        val probeRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(startMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST, 1280, 720)
                ?: retriever.getFrameAtTime(startMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
        } else {
            retriever.getFrameAtTime(startMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
        } ?: throw IllegalStateException("Could not read video frame")

        val rotation = orientation.rotationDegrees
        val probe = VideoOrientation.toDisplayBitmap(probeRaw, rotation)
        
        // Calculate dimensions with aspect ratio
        val (baseW, baseH) = if (config.aspectRatio != VideoAspectRatio.ORIGINAL) {
            val crop = config.aspectRatio.calculateCropRect(probe.width, probe.height)
            Pair(crop.width().toInt(), crop.height().toInt())
        } else {
            Pair(probe.width, probe.height)
        }
        val (encodeWidth, encodeHeight) = config.exportSettings.calculateOutputDimensions(baseW, baseH)
        probe.recycle()

        // Required dimension for retriever scaling taking rotation into account
        val (extractW, extractH) = if (rotation == 90 || rotation == 270) {
            encodeHeight to encodeWidth
        } else {
            encodeWidth to encodeHeight
        }

        FrameEffectProcessor.resetFaceCache()

        Log.d(
            TAG,
            "Export startMs=$startMs endMs=$endMs speed=$speedFactor dim=${encodeWidth}x$encodeHeight " +
                    "bitrate=${config.exportSettings.bitrate.bps} filter=${config.filter.title} mode=${config.blurMode.label}"
        )

        val cacheFile = File(context.cacheDir, "blur_export_${System.currentTimeMillis()}.mp4")
        var muxer: MediaMuxer? = null
        var videoEncoder: BitmapVideoEncoder? = null
        var isExportSuccessful = false

        try {
            val (audioFormat, audioSamples) = if (!config.isMuted && speedFactor == 1.0f) {
                extractAudio(inputUri, startMs * 1000L, endMs * 1000L)
            } else {
                null to emptyList()
            }

            muxer = MediaMuxer(cacheFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            videoEncoder = BitmapVideoEncoder(
                width = encodeWidth,
                height = encodeHeight,
                frameRate = frameRate,
                muxer = muxer,
                bitRate = config.exportSettings.bitrate.bps
            )

            var frameIndex = 0
            var processedFrames = 0
            var sourceTimeUs = startMs * 1000L
            var muxerStarted = false

            while (sourceTimeUs <= endMs * 1000L && frameIndex < totalFrames) {
                coroutineContext.ensureActive()

                val timeMs = sourceTimeUs / 1000L
                val rawFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        sourceTimeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        extractW,
                        extractH
                    ) ?: retriever.getFrameAtTime(sourceTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } else {
                    retriever.getFrameAtTime(sourceTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                }

                if (rawFrame != null) {
                    val frame = VideoOrientation.toDisplayBitmap(rawFrame, rotation)

                    // Process frame through full multi-tool effects pipeline
                    val processedBitmap = FrameEffectProcessor.processFrame(
                        source = frame,
                        timeMs = timeMs,
                        config = config,
                        timeline = timeline
                    )

                    val scaledForEncoder = if (processedBitmap.width != encodeWidth || processedBitmap.height != encodeHeight) {
                        Bitmap.createScaledBitmap(processedBitmap, encodeWidth, encodeHeight, true)
                    } else {
                        processedBitmap
                    }

                    val presentationTimeUs = (frameIndex * 1_000_000L / frameRate)
                    videoEncoder.encodeFrame(scaledForEncoder, presentationTimeUs)

                    if (scaledForEncoder !== processedBitmap) {
                        scaledForEncoder.recycle()
                    }
                    if (processedBitmap !== frame) {
                        processedBitmap.recycle()
                    }
                    frame.recycle()

                    if (!muxerStarted && videoEncoder.videoTrackIndex >= 0) {
                        startMuxer(muxer, videoEncoder, audioFormat, audioSamples)
                        muxerStarted = true
                    }

                    frameIndex++
                    processedFrames++
                }

                onProgress((frameIndex.toFloat() / totalFrames).coerceIn(0f, 0.99f))
                sourceTimeUs += sourceStepUs
            }

            check(muxerStarted) { "Video encoder produced no output" }

            val finalPtsUs = (frameIndex * 1_000_000L / frameRate)
            videoEncoder.finish(endTimeUs = finalPtsUs)
            try {
                muxer.stop()
            } catch (_: Throwable) {}
            try {
                muxer.release()
            } catch (_: Throwable) {}
            muxer = null

            val outputUri = publishToGallery(cacheFile)
            isExportSuccessful = true
            onProgress(1f)

            Log.d(TAG, "Export completed successfully! frames=$frameIndex output=$outputUri")

            ExportResult(
                outputUri = outputUri,
                frameCount = frameIndex,
                blurredFrames = processedFrames
            )
        } catch (ce: CancellationException) {
            Log.d(TAG, "Export task cancelled by user, releasing resources")
            throw ce
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
            try {
                videoEncoder?.releaseSilently()
            } catch (_: Throwable) {}
            try {
                muxer?.release()
            } catch (_: Throwable) {}
            if (!isExportSuccessful && cacheFile.exists()) {
                cacheFile.delete()
            }
        }
    }

    private fun startMuxer(
        muxer: MediaMuxer,
        videoEncoder: BitmapVideoEncoder,
        audioFormat: MediaFormat?,
        audioSamples: List<AudioSample>
    ) {
        if (audioFormat != null && audioSamples.isNotEmpty()) {
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

    private fun extractAudio(
        inputUri: Uri,
        startUs: Long = 0L,
        endUs: Long = Long.MAX_VALUE
    ): Pair<MediaFormat?, List<AudioSample>> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, inputUri, null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open audio data source", e)
            return null to emptyList()
        }

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

        if (startUs > 0) {
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            val sampleTime = extractor.sampleTime
            if (sampleTime > endUs) break

            if (sampleTime >= startUs) {
                val sampleData = ByteBuffer.allocate(sampleSize)
                buffer.position(0)
                buffer.limit(sampleSize)
                sampleData.put(buffer)
                sampleData.flip()
                samples += AudioSample(
                    data = sampleData,
                    presentationTimeUs = max(0L, sampleTime - startUs),
                    flags = extractor.sampleFlags
                )
            }
            buffer.clear()
            extractor.advance()
        }

        extractor.release()
        return format to samples
    }

    private fun publishToGallery(cacheFile: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "AI_Blur_Edited_${System.currentTimeMillis()}.mp4")
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
