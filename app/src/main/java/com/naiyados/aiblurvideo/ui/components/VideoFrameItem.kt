package com.naiyados.aiblurvideo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.unit.IntOffset
import java.util.Locale

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
    onScrubbingStateChanged: (Boolean) -> Unit = {},
    isFullBlurMode: Boolean = false,
    isFullVideoBlur: Boolean = true,
    blurFrameRangeStartMs: Long = 0L,
    blurFrameRangeEndMs: Long = 0L,
    selectedBlurFrameTimestampsMs: Set<Long> = emptySet(),
    onToggleFrameBlur: (Long) -> Unit = {},
    trimStartMs: Long = 0L,
    trimEndMs: Long = 0L,
    playbackSpeed: Float = 1.0f,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit = { _, _ -> }
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
            onScrubbingStateChanged = onScrubbingStateChanged,
            isFullBlurMode = isFullBlurMode,
            isFullVideoBlur = isFullVideoBlur,
            blurFrameRangeStartMs = blurFrameRangeStartMs,
            blurFrameRangeEndMs = blurFrameRangeEndMs,
            selectedBlurFrameTimestampsMs = selectedBlurFrameTimestampsMs,
            onToggleFrameBlur = onToggleFrameBlur,
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            playbackSpeed = playbackSpeed,
            onTrimChange = onTrimChange
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
    onScrubbingStateChanged: (Boolean) -> Unit,
    isFullBlurMode: Boolean = false,
    isFullVideoBlur: Boolean = true,
    blurFrameRangeStartMs: Long = 0L,
    blurFrameRangeEndMs: Long = 0L,
    selectedBlurFrameTimestampsMs: Set<Long> = emptySet(),
    onToggleFrameBlur: (Long) -> Unit = {},
    trimStartMs: Long = 0L,
    trimEndMs: Long = 0L,
    playbackSpeed: Float = 1.0f,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)
    val latestOnScrubFrameChange by rememberUpdatedState(onScrubFrameChange)
    val latestOnScrubbingStateChanged by rememberUpdatedState(onScrubbingStateChanged)
    val latestOnTrimChange by rememberUpdatedState(onTrimChange)
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

    val clipTrackWidth = secondWidth * secondCount
    val clipTrackWidthPx = secondWidthPx * secondCount

    val effectiveTrimStartMs = trimStartMs.coerceIn(0L, safeDurationMs)
    val effectiveTrimEndMs = if (trimEndMs > trimStartMs) trimEndMs.coerceIn(0L, safeDurationMs) else safeDurationMs
    val effectiveDurationMs = safeDurationMs

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
        val totalWidth = sidePadding + clipTrackWidth + 56.dp + sidePadding

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
                // 1. Time Ruler Bar with clean timestamp dots
                Row(
                    modifier = Modifier
                        .height(22.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(sidePadding))

                    repeat(secondCount) { second ->
                        val secondMs = second * 1000L
                        val isSecondBlurred = if (isFullBlurMode) {
                            if (isFullVideoBlur) true
                            else if (blurFrameRangeEndMs > blurFrameRangeStartMs && secondMs in blurFrameRangeStartMs..blurFrameRangeEndMs) true
                            else selectedBlurFrameTimestampsMs.contains(secondMs)
                        } else false

                        Row(
                            modifier = Modifier
                                .width(secondWidth)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatSecondLabel(second),
                                color = if (isSecondBlurred) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (isSecondBlurred) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(sidePadding + 56.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Video Clips Track with Integrated Bordered Trimmer
                Row(
                    modifier = Modifier
                        .height(54.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(sidePadding))

                    // Video Track Box with frames, dimmed areas, and mint border & drag handles
                    Box(
                        modifier = Modifier
                            .width(clipTrackWidth)
                            .height(54.dp)
                    ) {
                        // 2a. Base Strip of video thumbnails
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E2026),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(secondCount) { second ->
                                    val secondMs = second * 1000L
                                    val frame = frames.firstOrNull {
                                        it.timeMs >= secondMs && it.timeMs < (second + 1) * 1000L
                                    }

                                    val isBlurred = if (isFullBlurMode) {
                                        if (isFullVideoBlur) true
                                        else if (blurFrameRangeEndMs > blurFrameRangeStartMs && secondMs in blurFrameRangeStartMs..blurFrameRangeEndMs) true
                                        else selectedBlurFrameTimestampsMs.contains(secondMs)
                                    } else false

                                    TimelineSecondThumbnailItem(
                                        frame = frame,
                                        width = secondWidth,
                                        height = 54.dp,
                                        isFullBlurMode = isFullBlurMode,
                                        isBlurred = isBlurred,
                                        onToggleBlur = {
                                            onToggleFrameBlur(secondMs)
                                        }
                                    )
                                }
                            }
                        }

                        // Calculate Trim Geometry
                        val startFraction = (effectiveTrimStartMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
                        val endFraction = (effectiveTrimEndMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
                        val startPx = startFraction * clipTrackWidthPx
                        val endPx = endFraction * clipTrackWidthPx
                        val handleWidthPx = with(density) { 14.dp.toPx() }
                        val trimWidthPx = (endPx - startPx).coerceAtLeast(handleWidthPx * 2)

                        val mintBorderColor = Color(0xFF4EF2C8)

                        // 2b. Dimmed region before trim start
                        if (startPx > 1f) {
                            val startDimWidthDp = with(density) { startPx.toDp() }
                            Box(
                                modifier = Modifier
                                    .width(startDimWidthDp)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.65f))
                            )
                        }

                        // 2c. Dimmed region after trim end
                        if (clipTrackWidthPx - endPx > 1f) {
                            val endDimWidthDp = with(density) { (clipTrackWidthPx - endPx).toDp() }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(endDimWidthDp)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.65f))
                            )
                        }

                        // 2d. Bordered Lines Box with Left & Right Handles (Exact Match for photo IMG_3820.jpg)
                        val startOffsetDp = with(density) { startPx.toDp() }
                        val trimWidthDp = with(density) { trimWidthPx.toDp() }

                        Box(
                            modifier = Modifier
                                .offset(x = startOffsetDp)
                                .width(trimWidthDp)
                                .fillMaxHeight()
                        ) {
                            // Top Border Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.TopCenter)
                                    .background(mintBorderColor)
                            )

                            // Bottom Border Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(mintBorderColor)
                            )

                            // Left Drag Handle (Rounded bar)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(14.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = mintBorderColor,
                                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 2.dp, bottomEnd = 2.dp)
                                    )
                                    .pointerInput(clipTrackWidthPx, effectiveDurationMs, effectiveTrimEndMs) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                                latestOnScrubbingStateChanged(true)
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                change.consume()
                                                val deltaMs = ((dragAmount / clipTrackWidthPx) * effectiveDurationMs).toLong()
                                                val newStart = (effectiveTrimStartMs + deltaMs).coerceIn(0L, effectiveTrimEndMs - 500L)
                                                latestOnTrimChange(newStart, effectiveTrimEndMs)
                                                latestOnSeekTo(newStart)
                                            },
                                            onDragEnd = {
                                                latestOnScrubbingStateChanged(false)
                                            },
                                            onDragCancel = {
                                                latestOnScrubbingStateChanged(false)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner grip notch
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(Color(0xFF101B17), RoundedCornerShape(1.dp))
                                )
                            }

                            // Right Drag Handle (Rounded bar)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(14.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = mintBorderColor,
                                        shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp, topStart = 2.dp, bottomStart = 2.dp)
                                    )
                                    .pointerInput(clipTrackWidthPx, effectiveDurationMs, effectiveTrimStartMs) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                                latestOnScrubbingStateChanged(true)
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                change.consume()
                                                val deltaMs = ((dragAmount / clipTrackWidthPx) * effectiveDurationMs).toLong()
                                                val newEnd = (effectiveTrimEndMs + deltaMs).coerceIn(effectiveTrimStartMs + 500L, effectiveDurationMs)
                                                latestOnTrimChange(effectiveTrimStartMs, newEnd)
                                                latestOnSeekTo(newEnd)
                                            },
                                            onDragEnd = {
                                                latestOnScrubbingStateChanged(false)
                                            },
                                            onDragCancel = {
                                                latestOnScrubbingStateChanged(false)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner grip notch
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(Color(0xFF101B17), RoundedCornerShape(1.dp))
                                )
                            }

                            // Speed Badge in bottom-left inside active clip (matches IMG_3820.jpg)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 18.dp, bottom = 5.dp)
                            ) {
                                Text(
                                    text = "x" + String.format(Locale.US, "%.2f", playbackSpeed),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            // Duration badge in top-right inside active clip (matches IMG_3820.jpg)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 18.dp, top = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    val clipDurationMs = (effectiveTrimEndMs - effectiveTrimStartMs).coerceAtLeast(0L)
                                    Text(
                                        text = formatSecondDuration(clipDurationMs),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
    height: Dp,
    isFullBlurMode: Boolean = false,
    isBlurred: Boolean = false,
    onToggleBlur: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clickable(enabled = isFullBlurMode) {
                onToggleBlur()
            }
    ) {
        if (frame?.bitmap != null) {
            Image(
                bitmap = frame.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isBlurred) {
                            Modifier.background(Color.Black)
                        } else Modifier
                    ),
                contentScale = ContentScale.Crop,
                alpha = if (isBlurred) 0.65f else 1.0f
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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

        // Visual Blur Overlay Badge when blurred in FullBlur mode
        if (isFullBlurMode && isBlurred) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x5500A2FF))
                    .border(1.5.dp, Color(0xFF64B5F6))
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xDD0D47A1),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "💧",
                            fontSize = 8.sp
                        )
                    }
                }
            }
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
    val videoKey = "v14_320px_" + videoUri.toString()
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
                            maxSide = 320
                        )

                        if (smallBitmap != bitmap) {
                            bitmap.recycle()
                        }

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
                92,
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

private fun formatSecondDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}