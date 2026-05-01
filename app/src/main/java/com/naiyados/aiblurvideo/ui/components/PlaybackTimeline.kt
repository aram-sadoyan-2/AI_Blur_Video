package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.delay

@Composable
fun PlaybackTimeline(
    player: Player?,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    DisposableEffect(player) {
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    durationMs = safeDuration(player)
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    if (!isSeeking) {
                        sliderValue = positionToSlider(positionMs, durationMs)
                    }
                }
            }

            player.addListener(listener)

            onDispose {
                player.removeListener(listener)
            }
        }
    }

    LaunchedEffect(player, isSeeking) {
        while (player != null) {
            durationMs = safeDuration(player)
            positionMs = player.currentPosition.coerceAtLeast(0L)

            if (!isSeeking) {
                sliderValue = positionToSlider(positionMs, durationMs)
            }

            delay(250)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(positionMs),
                    color = Color.White.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    modifier = Modifier.weight(1f),
                    text = " / ${formatTime(durationMs)}",
                    color = Color.White.copy(alpha = 0.45f)
                )

                IconButton(
                    onClick = {
                        player?.seekTo(0L)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        player?.let {
                            val newPos = (it.currentPosition - 10_000L).coerceAtLeast(0L)
                            it.seekTo(newPos)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Back 10 seconds",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        player?.let {
                            val max = safeDuration(it)
                            val newPos = (it.currentPosition + 10_000L).coerceAtMost(max)
                            it.seekTo(newPos)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White
                    )
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isSeeking = true
                    sliderValue = value
                },
                onValueChangeFinished = {
                    val target = (durationMs * sliderValue).toLong()
                    player?.seekTo(target)
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AiBlurColors.Pink,
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                )
            )
        }
    }
}

private fun safeDuration(player: Player): Long {
    val duration = player.duration
    return if (duration > 0L) duration else 0L
}

private fun positionToSlider(
    positionMs: Long,
    durationMs: Long
): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}