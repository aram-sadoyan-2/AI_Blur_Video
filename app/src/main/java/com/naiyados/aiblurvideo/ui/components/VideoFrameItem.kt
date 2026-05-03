package com.naiyados.aiblurvideo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoFrameItem(
    val timeMs: Long,
    val bitmap: Bitmap
)

@Composable
fun VideoFrameStripSection(
    videoUri: Uri?,
    player: Player?,
    modifier: Modifier = Modifier,
    frameIntervalMs: Long = 500L,
    maxFrames: Int = 36
) {
    val context = LocalContext.current

    val frames = remember(videoUri) {
        mutableStateListOf<VideoFrameItem>()
    }

    var loading by remember(videoUri) {
        mutableStateOf(false)
    }

    var errorText by remember(videoUri) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(videoUri) {
        frames.clear()
        errorText = null

        if (videoUri == null) {
            errorText = "No video"
            return@LaunchedEffect
        }

        loading = true

        val result = withContext(Dispatchers.IO) {
            loadVideoFrames(
                context = context,
                videoUri = videoUri,
                frameIntervalMs = frameIntervalMs,
                maxFrames = maxFrames
            )
        }

        loading = false

        if (result.isEmpty()) {
            errorText = "Frames not loaded"
        } else {
            frames.addAll(result)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = AiBlurColors.Pink,
                    modifier = Modifier.size(17.dp)
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = when {
                        loading -> "Loading frames..."
                        errorText != null -> errorText ?: "Frames"
                        else -> "Frames"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (frames.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    frames.forEach { frame ->
                        FrameThumb(
                            frame = frame,
                            onClick = {
                                player?.seekTo(frame.timeMs)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameThumb(
    frame: VideoFrameItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
    ) {
        Image(
            bitmap = frame.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .width(52.dp)
                .height(78.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.60f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatFrameTime(frame.timeMs),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

private fun loadVideoFrames(
    context: Context,
    videoUri: Uri,
    frameIntervalMs: Long,
    maxFrames: Int
): List<VideoFrameItem> {
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(context, videoUri)

        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?: 0L

        if (durationMs <= 0L) {
            return emptyList()
        }

        val result = mutableListOf<VideoFrameItem>()

        var timeMs = 0L

        while (timeMs <= durationMs && result.size < maxFrames) {
            val bitmap = retriever.getFrameAtTime(
                timeMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST
            )

            if (bitmap != null) {
                val smallBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    104,
                    156,
                    true
                )

                result.add(
                    VideoFrameItem(
                        timeMs = timeMs,
                        bitmap = smallBitmap
                    )
                )
            }

            timeMs += frameIntervalMs
        }

        result
    } catch (e: Throwable) {
        emptyList()
    } finally {
        retriever.release()
    }
}

private fun formatFrameTime(timeMs: Long): String {
    val seconds = timeMs / 1000L
    val millis = timeMs % 1000L
    return "$seconds.${millis.toString().padStart(3, '0')}s"
}