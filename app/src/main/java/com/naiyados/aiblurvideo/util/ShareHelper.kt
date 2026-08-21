package com.naiyados.aiblurvideo.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareHelper {
    fun shareVideo(context: Context, uri: Uri, title: String = "Share Processed Video") {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("ShareHelper", "Error sharing video", e)
        }
    }
}

@Composable
fun rememberVideoThumbnail(context: Context, uri: Uri?): Bitmap? {
    val bitmapState = remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri == null) {
            bitmapState.value = null
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val bmp = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)
                retriever.release()
                withContext(Dispatchers.Main) {
                    bitmapState.value = bmp
                }
            } catch (e: Exception) {
                Log.e("rememberVideoThumbnail", "Failed to retrieve thumbnail", e)
            }
        }
    }

    return bitmapState.value
}
