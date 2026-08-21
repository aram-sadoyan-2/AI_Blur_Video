package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlin.math.max

@Composable
fun VideoTrimmerControl(
    totalDurationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSafe = max(1000L, totalDurationMs)
    val effectiveEndMs = if (trimEndMs > trimStartMs) trimEndMs else durationSafe
    val selectedDurationMs = max(0L, effectiveEndMs - trimStartMs)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_trimmer_control"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCut,
                        contentDescription = null,
                        tint = AiBlurColors.Pink,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Video Trimmer & Cutter",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Clip: ${formatTime(selectedDurationMs)}",
                    color = AiBlurColors.Pink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            RangeSlider(
                value = (trimStartMs.toFloat() / durationSafe.toFloat())..(effectiveEndMs.toFloat() / durationSafe.toFloat()),
                onValueChange = { range ->
                    val newStart = (range.start * durationSafe).toLong()
                    val newEnd = (range.endInclusive * durationSafe).toLong()
                    onTrimChange(newStart, newEnd)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = AiBlurColors.Pink,
                    activeTrackColor = AiBlurColors.Pink,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start: ${formatTime(trimStartMs)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                TextButton(
                    onClick = { onTrimChange(0L, durationSafe) }
                ) {
                    Text(
                        text = "Reset Full",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "End: ${formatTime(effectiveEndMs)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val tenths = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", min, sec, tenths)
}
