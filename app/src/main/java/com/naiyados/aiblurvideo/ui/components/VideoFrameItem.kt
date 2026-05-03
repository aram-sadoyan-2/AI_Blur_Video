package com.naiyados.aiblurvideo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToLong

private data class TimelineFrame(
    val timeMs: Long,
    val bitmap: Bitmap
)

@Composable
fun VideoFrameStripSection(
    videoUri: Uri?,
    player: Player?,
    modifier: Modifier = Modifier,
    maxDurationSeconds: Int = 30
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val frames = remember(videoUri) {
        mutableStateListOf<TimelineFrame>()
    }

    var durationMs by remember(videoUri) {
        mutableLongStateOf(0L)
    }

    var loading by remember(videoUri) {
        mutableStateOf(false)
    }

    var errorText by remember(videoUri) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(videoUri) {
        frames.clear()
        durationMs = 0L
        errorText = null

        if (videoUri == null) {
            errorText = "No video"
            return@LaunchedEffect
        }

        loading = true

        loadCachedTimelineFramesProgressive(
            context = context,
            videoUri = videoUri,
            maxDurationSeconds = maxDurationSeconds,
            onDuration = { duration ->
                durationMs = duration
            },
            onFrame = { frame ->
                val exists = frames.any { it.timeMs == frame.timeMs }
                if (!exists) {
                    frames.add(frame)
                    frames.sortBy { it.timeMs }
                }
            }
        )

        loading = false

        if (frames.isEmpty()) {
            errorText = "Frames not loaded"
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
                        loading && frames.isEmpty() -> "Loading timeline..."
                        errorText != null -> errorText ?: "Timeline"
                        else -> "Timeline"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            FixedCenterTimeline(
                frames = frames,
                durationMs = durationMs,
                player = player,
                scrollState = scrollState,
                maxDurationSeconds = maxDurationSeconds
            )
        }
    }
}

@Composable
private fun FixedCenterTimeline(
    frames: List<TimelineFrame>,
    durationMs: Long,
    player: Player?,
    scrollState: androidx.compose.foundation.ScrollState,
    maxDurationSeconds: Int
) {
    val density = LocalDensity.current

    val secondWidth = 38.dp
    val secondWidthPx = with(density) { secondWidth.toPx() }

    val safeDurationMs = durationMs.coerceAtLeast(1L)
    val secondCount = ((safeDurationMs + 999L) / 1000L)
        .toInt()
        .coerceAtLeast(1)
        .coerceAtMost(maxDurationSeconds)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
    ) {
        val sidePadding = maxWidth / 2
        val totalWidth = sidePadding + (secondWidth * secondCount) + sidePadding

        LaunchedEffect(scrollState, player, safeDurationMs, secondWidthPx) {
            snapshotFlow { scrollState.value }
                .collect { scrollPx ->
                    if (scrollState.isScrollInProgress) {
                        val seconds = scrollPx / secondWidthPx
                        val seekMs = (seconds * 1000f)
                            .roundToLong()
                            .coerceIn(0L, safeDurationMs)

                        player?.seekTo(seekMs)
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .requiredWidth(totalWidth)
                    .height(82.dp),
                verticalAlignment = Alignment.Top
            ) {
                Spacer(modifier = Modifier.width(sidePadding))

                repeat(secondCount) { second ->
                    val frame = frames.firstOrNull {
                        it.timeMs >= second * 1000L &&
                                it.timeMs < (second + 1) * 1000L
                    }

                    TimelineSecondItem(
                        second = second,
                        frame = frame,
                        width = secondWidth
                    )
                }

                Spacer(modifier = Modifier.width(sidePadding))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(10.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun TimelineSecondItem(
    second: Int,
    frame: TimelineFrame?,
    width: Dp
) {
    val imageHeight = 58.dp

    val imageWidth = remember(frame?.bitmap) {
        if (frame?.bitmap == null) {
            30.dp
        } else {
            val ratio = frame.bitmap.width.toFloat() / frame.bitmap.height.toFloat()
            (58f * ratio).dp.coerceIn(24.dp, width)
        }
    }

    Column(
        modifier = Modifier
            .width(width)
            .height(82.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatSecondLabel(second),
                color = Color.White.copy(alpha = 0.62f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }

        if (frame != null) {
            Image(
                bitmap = frame.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .width(imageWidth)
                    .height(imageHeight),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .width(imageWidth)
                    .height(imageHeight)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }
    }
}

private suspend fun loadCachedTimelineFramesProgressive(
    context: Context,
    videoUri: Uri,
    maxDurationSeconds: Int,
    onDuration: suspend (Long) -> Unit,
    onFrame: suspend (TimelineFrame) -> Unit
) {
    val videoKey = "v6_" + videoUri.toString()
        .hashCode()
        .toString()
        .replace("-", "m")

    val cacheDir = File(context.cacheDir, "video_frames/$videoKey")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }

    val durationFile = File(cacheDir, "duration.txt")
    val cachedDurationMs = durationFile
        .takeIf { it.exists() }
        ?.readText()
        ?.toLongOrNull()

    if (cachedDurationMs != null && cachedDurationMs > 0L) {
        onDuration(cachedDurationMs)
    }

    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, videoUri)

            val realDurationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L

            if (realDurationMs <= 0L) return@withContext

            val cappedDurationMs = realDurationMs.coerceAtMost(maxDurationSeconds * 1000L)

            durationFile.writeText(cappedDurationMs.toString())

            withContext(Dispatchers.Main) {
                onDuration(cappedDurationMs)
            }

            var timeMs = 0L

            while (timeMs <= cappedDurationMs) {
                val cachedFile = File(cacheDir, "frame_$timeMs.jpg")

                val cachedBitmap = if (cachedFile.exists()) {
                    BitmapFactory.decodeFile(cachedFile.absolutePath)
                } else {
                    null
                }

                if (cachedBitmap != null) {
                    withContext(Dispatchers.Main) {
                        onFrame(
                            TimelineFrame(
                                timeMs = timeMs,
                                bitmap = cachedBitmap
                            )
                        )
                    }
                } else {
                    val bitmap = retriever.getFrameAtTime(
                        timeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )

                    if (bitmap != null) {
                        val smallBitmap = createTimelineThumbnail(
                            source = bitmap,
                            maxSide = 160
                        )

                        saveBitmapToJpg(
                            bitmap = smallBitmap,
                            file = cachedFile
                        )

                        withContext(Dispatchers.Main) {
                            onFrame(
                                TimelineFrame(
                                    timeMs = timeMs,
                                    bitmap = smallBitmap
                                )
                            )
                        }
                    }
                }

                timeMs += 1000L
            }
        } catch (_: Throwable) {
            withContext(Dispatchers.Main) {
                onDuration(0L)
            }
        } finally {
            retriever.release()
        }
    }
}

private fun createTimelineThumbnail(
    source: Bitmap,
    maxSide: Int
): Bitmap {
    val sourceWidth = source.width
    val sourceHeight = source.height

    if (sourceWidth <= 0 || sourceHeight <= 0) {
        return source
    }

    val scale = if (sourceWidth >= sourceHeight) {
        maxSide.toFloat() / sourceWidth.toFloat()
    } else {
        maxSide.toFloat() / sourceHeight.toFloat()
    }

    val targetWidth = (sourceWidth * scale).toInt().coerceAtLeast(1)
    val targetHeight = (sourceHeight * scale).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(
        source,
        targetWidth,
        targetHeight,
        true
    )
}

private fun saveBitmapToJpg(
    bitmap: Bitmap,
    file: File
) {
    try {
        FileOutputStream(file).use { output ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                76,
                output
            )
        }
    } catch (_: Throwable) {
        // Ignore cache save errors
    }
}

private fun formatSecondLabel(second: Int): String {
    val minutes = second / 60
    val secs = second % 60
    return "%02d:%02d".format(minutes, secs)
}