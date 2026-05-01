package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.delay

@Composable
fun VideoSeekBarSection(
    player: Player?,
    modifier: Modifier = Modifier
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(player, isSeeking) {
        while (player != null) {
            val duration = player.duration
            durationMs = if (duration > 0L) duration else 0L
            positionMs = player.currentPosition.coerceAtLeast(0L)

            if (!isSeeking) {
                sliderValue = if (durationMs <= 0L) 0f
                else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            }

            delay(200)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = {
                isSeeking = true
                sliderValue = it
            },
            onValueChangeFinished = {
                val target = (durationMs * sliderValue).toLong()
                player?.seekTo(target)
                isSeeking = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = AiBlurColors.Pink,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )

        Text(
            text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}