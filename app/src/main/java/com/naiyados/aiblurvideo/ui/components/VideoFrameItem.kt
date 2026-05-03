package com.naiyados.aiblurvideo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private data class TimelineFrame(
    val timeMs: Long,
    val bitmap: Bitmap
)

@Composable
fun VideoFrameStripSection(
    videoUri: Uri?,
    player: Player?,
    modifier: Modifier = Modifier,
    framesPerSecond: Int = 4,
    maxDurationSeconds: Int = 60
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val frames = remember(videoUri) {
        mutableStateListOf<TimelineFrame>()
    }

    var durationMs by remember(videoUri) {
        mutableLongStateOf(0L)
    }

    var currentPositionMs by remember {
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

        val result = withContext(Dispatchers.IO) {
            loadTimelineFrames(
                context = context,
                videoUri = videoUri,
                framesPerSecond = framesPerSecond,
                maxDurationSeconds = maxDurationSeconds
            )
        }

        loading = false

        durationMs = result.durationMs

        if (result.frames.isEmpty()) {
            errorText = "Frames not loaded"
        } else {
            frames.addAll(result.frames)
        }
    }

    LaunchedEffect(player) {
        while (player != null) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            delay(80)
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val secondCount = ((safeDuration + 999L) / 1000L).toInt().coerceAtLeast(1)

    val secondWidthDp = 76.dp
    val timelineWidthDp = secondWidthDp * secondCount

    val timelineWidthPx = with(density) {
        timelineWidthDp.toPx()
    }

    val playheadX = if (safeDuration > 0L) {
        (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) * timelineWidthPx
    } else {
        0f
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
                        loading -> "Loading timeline..."
                        errorText != null -> errorText ?: "Timeline"
                        else -> "Timeline"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (frames.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .requiredWidth(timelineWidthDp)
                            .height(76.dp)
                            .pointerInput(safeDuration, timelineWidthPx) {
                                detectTapGestures { offset ->
                                    val progress = (offset.x / timelineWidthPx).coerceIn(0f, 1f)
                                    val seekMs = (safeDuration * progress).toLong()
                                    player?.seekTo(seekMs)
                                    currentPositionMs = seekMs
                                }
                            }
                            .pointerInput(safeDuration, timelineWidthPx) {
                                detectDragGestures { change, _ ->
                                    val progress = (change.position.x / timelineWidthPx).coerceIn(0f, 1f)
                                    val seekMs = (safeDuration * progress).toLong()
                                    player?.seekTo(seekMs)
                                    currentPositionMs = seekMs
                                    change.consume()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .requiredWidth(timelineWidthDp)
                                .height(76.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            repeat(secondCount) { second ->
                                TimelineSecondBlock(
                                    second = second,
                                    frames = frames.filter {
                                        it.timeMs >= second * 1000L &&
                                                it.timeMs < (second + 1) * 1000L
                                    },
                                    width = secondWidthDp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = playheadX.roundToInt(),
                                        y = 0
                                    )
                                }
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = playheadX.roundToInt() - with(density) { 5.dp.toPx().roundToInt() },
                                        y = 0
                                    )
                                }
                                .size(10.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSecondBlock(
    second: Int,
    frames: List<TimelineFrame>,
    width: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = Modifier
            .width(width)
            .height(76.dp)
            .padding(end = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = formatSecondLabel(second),
                color = Color.White.copy(alpha = 0.62f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (frames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.08f))
                )
            } else {
                frames.forEach { frame ->
                    Image(
                        bitmap = frame.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private data class TimelineLoadResult(
    val durationMs: Long,
    val frames: List<TimelineFrame>
)

private fun loadTimelineFrames(
    context: Context,
    videoUri: Uri,
    framesPerSecond: Int,
    maxDurationSeconds: Int
): TimelineLoadResult {
    val retriever = MediaMetadataRetriever()

    return try {
        retriever.setDataSource(context, videoUri)

        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?: 0L

        if (durationMs <= 0L) {
            return TimelineLoadResult(
                durationMs = 0L,
                frames = emptyList()
            )
        }

        val cappedDurationMs = durationMs.coerceAtMost(maxDurationSeconds * 1000L)
        val frameIntervalMs = (1000L / framesPerSecond.coerceAtLeast(1)).coerceAtLeast(1L)

        val result = mutableListOf<TimelineFrame>()

        var timeMs = 0L

        while (timeMs <= cappedDurationMs) {
            val bitmap = retriever.getFrameAtTime(
                timeMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST
            )

            if (bitmap != null) {
                val smallBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    80,
                    80,
                    true
                )

                result.add(
                    TimelineFrame(
                        timeMs = timeMs,
                        bitmap = smallBitmap
                    )
                )
            }

            timeMs += frameIntervalMs
        }

        TimelineLoadResult(
            durationMs = cappedDurationMs,
            frames = result
        )
    } catch (_: Throwable) {
        TimelineLoadResult(
            durationMs = 0L,
            frames = emptyList()
        )
    } finally {
        retriever.release()
    }
}

private fun formatSecondLabel(second: Int): String {
    val minutes = second / 60
    val secs = second % 60
    return "%02d:%02d".format(minutes, secs)
}