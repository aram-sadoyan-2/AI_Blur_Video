package com.naiyados.aiblurvideo.autoplate

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

/** Reuses one retriever to sample frames at playback time. */
class VideoFrameGrabber(
    context: Context
) {
    private val appContext = context.applicationContext
    private var retriever: MediaMetadataRetriever? = null
    private var openUri: Uri? = null

    @Synchronized
    fun open(uri: Uri) {
        if (openUri == uri && retriever != null) return
        close()
        openUri = uri
        retriever = MediaMetadataRetriever().apply {
            setDataSource(appContext, uri)
        }
    }

    @Synchronized
    fun frameAt(timeMs: Long): Bitmap? {
        return retriever?.getFrameAtTime(
            timeMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST
        )
    }

    @Synchronized
    fun close() {
        retriever?.release()
        retriever = null
        openUri = null
    }
}
