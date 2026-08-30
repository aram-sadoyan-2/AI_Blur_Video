package com.naiyados.aiblurvideo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.LastPage
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.model.KeyframeBlurBox
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FrameSteppingControlBar(
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onStepFrames: (Int) -> Unit, // e.g. -10, -1, +1, +10
    keyframes: List<KeyframeBlurBox>,
    onApplyPropagation: (Int) -> Unit, // e.g. 5, 10, 20 frames
    onAddSingleKeyframe: () -> Unit,
    onClearKeyframeAtCurrentTime: () -> Unit,
    modifier: Modifier = Modifier,
    fps: Int = 30
) {
    val frameDurationMs = 1000L / fps.coerceIn(15, 60)
    val currentFrameIndex = (currentPositionMs / frameDurationMs).toInt()
    val totalFrames = ((totalDurationMs.coerceAtLeast(1000L)) / frameDurationMs).toInt()

    val hasKeyframeAtCurrent = remember(keyframes, currentPositionMs) {
        keyframes.any { kotlin.math.abs(it.timeMs - currentPositionMs) <= frameDurationMs * 2 }
    }

    var lastAppliedOffset by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF14161F), Color(0xFF0D0E15))
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Frame Stepping Buttons + Frame Indicator + Play/Pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: -10 and -1 Frame Steppers
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10 Frames Button
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onStepFrames(-10) }
                        .testTag("step_minus_10_frames_btn"),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222533),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = "-10 Frames",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "-10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6)
                        )
                    }
                }

                // -1 Frame Button
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onStepFrames(-1) }
                        .testTag("step_minus_1_frame_btn"),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222533),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NavigateBefore,
                            contentDescription = "-1 Frame",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "-1f",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Center: Frame Counter & Timestamp Badge + Mini Play Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Play / Pause mini toggle
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onPlayPauseClick)
                        .testTag("frame_bar_play_pause_btn"),
                    shape = CircleShape,
                    color = if (isPlaying) AiBlurColors.Green else Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, if (isPlaying) AiBlurColors.Green else Color.White.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Frame #$currentFrameIndex / $totalFrames",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = formatPreciseTime(currentPositionMs),
                        fontSize = 10.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: +1 and +10 Frame Steppers
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // +1 Frame Button
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onStepFrames(1) }
                        .testTag("step_plus_1_frame_btn"),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222533),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "+1f",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Rounded.NavigateNext,
                            contentDescription = "+1 Frame",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // +10 Frames Button
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onStepFrames(10) }
                        .testTag("step_plus_10_frames_btn"),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222533),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "+10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6)
                        )
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = "+10 Frames",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Row 2: Manual Position Propagation Across Frames (User's requested feature)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Apply Box:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 2.dp)
            )

            // Primary requested action: +-10 Frames
            val is10Active = lastAppliedOffset == 10
            val btn10Bg by animateColorAsState(
                targetValue = if (is10Active) Color(0xFF00E676) else Color(0xFF1E3A2F),
                animationSpec = tween(200)
            )
            Surface(
                modifier = Modifier
                    .height(30.dp)
                    .clickable {
                        lastAppliedOffset = 10
                        onApplyPropagation(10)
                        scope.launch {
                            delay(600)
                            lastAppliedOffset = -1
                        }
                    }
                    .testTag("apply_propagate_10_frames_btn"),
                shape = RoundedCornerShape(8.dp),
                color = btn10Bg,
                border = BorderStroke(1.2.dp, if (is10Active) Color.White else Color(0xFF00E676).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (is10Active) Icons.Rounded.BookmarkAdded else Icons.Rounded.ShareLocation,
                        contentDescription = null,
                        tint = if (is10Active) Color.Black else Color(0xFF00E676),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "±10 Frames",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is10Active) Color.Black else Color.White
                    )
                }
            }

            // Quick alternative: +-5 Frames
            val is5Active = lastAppliedOffset == 5
            Surface(
                modifier = Modifier
                    .height(30.dp)
                    .clickable {
                        lastAppliedOffset = 5
                        onApplyPropagation(5)
                        scope.launch {
                            delay(600)
                            lastAppliedOffset = -1
                        }
                    }
                    .testTag("apply_propagate_5_frames_btn"),
                shape = RoundedCornerShape(8.dp),
                color = if (is5Active) Color(0xFF64B5F6) else Color(0xFF1A2634),
                border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.5f))
            ) {
                Text(
                    text = "±5 Frames",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (is5Active) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Quick alternative: +-20 Frames
            val is20Active = lastAppliedOffset == 20
            Surface(
                modifier = Modifier
                    .height(30.dp)
                    .clickable {
                        lastAppliedOffset = 20
                        onApplyPropagation(20)
                        scope.launch {
                            delay(600)
                            lastAppliedOffset = -1
                        }
                    }
                    .testTag("apply_propagate_20_frames_btn"),
                shape = RoundedCornerShape(8.dp),
                color = if (is20Active) Color(0xFFFFB74D) else Color(0xFF2E241A),
                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f))
            ) {
                Text(
                    text = "±20 Frames",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (is20Active) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Single keyframe pin / unpin
            Surface(
                modifier = Modifier
                    .height(30.dp)
                    .clickable {
                        if (hasKeyframeAtCurrent) {
                            onClearKeyframeAtCurrentTime()
                        } else {
                            onAddSingleKeyframe()
                        }
                    }
                    .testTag("toggle_single_keyframe_btn"),
                shape = RoundedCornerShape(8.dp),
                color = if (hasKeyframeAtCurrent) Color(0xFF3B1E22) else Color(0xFF222533),
                border = BorderStroke(1.dp, if (hasKeyframeAtCurrent) Color(0xFFFF5252) else Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (hasKeyframeAtCurrent) Icons.Rounded.DeleteOutline else Icons.Rounded.AddLocationAlt,
                        contentDescription = null,
                        tint = if (hasKeyframeAtCurrent) Color(0xFFFF5252) else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (hasKeyframeAtCurrent) "Remove Pin" else "Pin Frame",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hasKeyframeAtCurrent) Color(0xFFFF5252) else Color.White
                    )
                }
            }

            if (keyframes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "${keyframes.size} saved frames",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

private fun formatPreciseTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val millisPart = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(minutes, seconds, millisPart)
}
