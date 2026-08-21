package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun SpeedSelectorBar(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("speed_selector_bar"),
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
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = AiBlurColors.Orange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Playback & Export Speed",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${currentSpeed}x" + when (currentSpeed) {
                        0.5f -> " (Slow-Mo)"
                        1.0f -> " (Normal)"
                        2.0f -> " (Timelapse)"
                        else -> ""
                    },
                    color = AiBlurColors.Orange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                speedOptions.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clickable { onSpeedSelected(speed) }
                            .testTag("speed_chip_${speed}x"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AiBlurColors.Orange else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) AiBlurColors.Orange else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
