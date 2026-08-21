package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay

@Composable
fun PlaybackControlBar(
    player: Player?,
    isPlaying: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    canUndo: Boolean = true,
    canRedo: Boolean = true,
    onUndoClick: () -> Unit = {},
    onRedoClick: () -> Unit = {}
) {
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        while (player != null) {
            val duration = player.duration
            val position = player.currentPosition
            durationMs = if (duration > 0L) duration else 0L
            currentPositionMs = if (position > 0L) position else 0L
            delay(200)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timecode display (left)
        Text(
            text = formatTime(if (durationMs > 0L) durationMs else currentPositionMs),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.width(60.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Center Play / Pause button
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("play_pause_button")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right quick actions (Undo, Redo, Mute)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Undo button (left of Redo & Sound icon)
            IconButton(
                onClick = onUndoClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. Redo button (left of Sound icon)
            IconButton(
                onClick = onRedoClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3. Sound icon button (Mute / Unmute)
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toggle_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color(0xFFFF5252) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}