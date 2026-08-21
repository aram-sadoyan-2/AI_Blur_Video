package com.naiyados.aiblurvideo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier,
    currentPositionMs: Long = 0L,
    isPlaying: Boolean = false,
    maxDurationSeconds: Int = 30,
    onSeekTo: (Long) -> Unit,
    onScrubFrameChange: (Bitmap?) -> Unit = {},
    onScrubbingStateChanged: (Boolean) -> Unit = {}
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        FixedCenterTimeline(
            frames = frames,
            durationMs = durationMs,
            currentPositionMs = currentPositionMs,
            isPlaying = isPlaying,
            scrollState = scrollState,
            maxDurationSeconds = maxDurationSeconds,
            onSeekTo = onSeekTo,
            onScrubFrameChange = onScrubFrameChange,
            onScrubbingStateChanged = onScrubbingStateChanged
        )
    }
}

@Composable
private fun FixedCenterTimeline(
    frames: List<TimelineFrame>,
    durationMs: Long,
    currentPositionMs: Long,
    isPlaying: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    maxDurationSeconds: Int,
    onSeekTo: (Long) -> Unit,
    onScrubFrameChange: (Bitmap?) -> Unit,
    onScrubbingStateChanged: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)
    val latestOnScrubFrameChange by rememberUpdatedState(onScrubFrameChange)
    val latestOnScrubbingStateChanged by rememberUpdatedState(onScrubbingStateChanged)
    val latestFrames by rememberUpdatedState(frames)
    val latestDurationMs by rememberUpdatedState(durationMs)

    val secondWidth = 44.dp
    val secondWidthPx = with(density) { secondWidth.toPx() }
    val latestSecondWidthPx by rememberUpdatedState(secondWidthPx)

    val safeDurationMs = durationMs.coerceAtLeast(1L)
    val secondCount = ((safeDurationMs + 999L) / 1000L)
        .toInt()
        .coerceAtLeast(1)
        .coerceAtMost(maxDurationSeconds)

    var isUserDragging by remember { mutableStateOf(false) }

    // Synchronize timeline scroll position with video playback
    LaunchedEffect(currentPositionMs, isUserDragging, secondWidthPx) {
        if (!isUserDragging && secondWidthPx > 0f) {
            val targetScroll = ((currentPositionMs / 1000f) * secondWidthPx).toInt().coerceAtLeast(0)
            if (kotlin.math.abs(scrollState.value - targetScroll) > 1) {
                scrollState.scrollTo(targetScroll)
            }
        }
    }

    fun currentSeekMsFromScroll(): Long {
        val sWidthPx = latestSecondWidthPx
        if (sWidthPx <= 0f) return 0L
        val maxDur = latestDurationMs.coerceAtLeast(1L)
        val seconds = scrollState.value / sWidthPx
        return (seconds * 1000f)
            .roundToLong()
            .coerceIn(0L, maxDur)
    }

    fun updateScrubFrame(seekMs: Long) {
        val currentFrames = latestFrames
        if (currentFrames.isNotEmpty()) {
            val previewFrame = currentFrames.minByOrNull { frame ->
                kotlin.math.abs(frame.timeMs - seekMs)
            }
            latestOnScrubFrameChange(previewFrame?.bitmap)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val sidePadding = maxWidth / 2
        val totalWidth = sidePadding + (secondWidth * secondCount) + 56.dp + sidePadding

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isUserDragging = true
                            latestOnScrubbingStateChanged(true)
                            val seekMs = currentSeekMsFromScroll()
                            latestOnSeekTo(seekMs)
                            updateScrubFrame(seekMs)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scrollState.dispatchRawDelta(-dragAmount)
                            val seekMs = currentSeekMsFromScroll()
                            latestOnSeekTo(seekMs)
                            updateScrubFrame(seekMs)
                        },
                        onDragEnd = {
                            val seekMs = currentSeekMsFromScroll()
                            latestOnSeekTo(seekMs)
                            updateScrubFrame(seekMs)
                            isUserDragging = false
                            latestOnScrubbingStateChanged(false)
                        },
                        onDragCancel = {
                            val seekMs = currentSeekMsFromScroll()
                            latestOnSeekTo(seekMs)
                            updateScrubFrame(seekMs)
                            isUserDragging = false
                            latestOnScrubbingStateChanged(false)
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .horizontalScroll(state = scrollState, enabled = false)
                    .requiredWidth(totalWidth)
                    .fillMaxHeight()
            ) {
                // 1. Time Ruler Bar
                Row(
                    modifier = Modifier
                        .height(22.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(sidePadding))

                    repeat(secondCount) { second ->
                        Box(
                            modifier = Modifier
                                .width(secondWidth)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatSecondLabel(second),
                                color = Color.White.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(sidePadding + 56.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Video Clips Track
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(sidePadding))

                    // Strip of video thumbnails with rounded corners
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2026),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(secondCount) { second ->
                                val frame = frames.firstOrNull {
                                    it.timeMs >= second * 1000L && it.timeMs < (second + 1) * 1000L
                                }
                                TimelineSecondThumbnailItem(
                                    frame = frame,
                                    width = secondWidth,
                                    height = 52.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Pro Add (+) button at the end of the track
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add clip",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(sidePadding))
                }

                // 3. Dark Secondary Audio/Timeline space
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .padding(horizontal = sidePadding)
                )
            }

            // Fixed Center White Playhead Line (Spans the entire height)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun TimelineSecondThumbnailItem(
    frame: TimelineFrame?,
    width: Dp,
    height: Dp
) {
    if (frame?.bitmap != null) {
        Image(
            bitmap = frame.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .width(width)
                .height(height),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .background(Color(0xFF23252E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
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
    val videoKey = "v13_125ms_" + videoUri.toString()
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

                timeMs += 125L
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