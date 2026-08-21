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
import androidx.compose.material.icons.rounded.Crop
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
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun AspectRatioSelectorBar(
    selectedRatio: VideoAspectRatio,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("aspect_ratio_selector_bar"),
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
                        imageVector = Icons.Rounded.Crop,
                        contentDescription = null,
                        tint = AiBlurColors.Cyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Aspect Ratio & Frame Crop",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = selectedRatio.title,
                    color = AiBlurColors.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoAspectRatio.values().forEach { ratio ->
                    val isSelected = ratio == selectedRatio
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onRatioSelected(ratio) }
                            .testTag("aspect_chip_${ratio.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AiBlurColors.Cyan else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) AiBlurColors.Cyan else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ratio.title.split(" ").first(),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
