package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.theme.LocalAppColors

@Composable
fun AspectRatioSelectorBar(
    selectedRatio: VideoAspectRatio,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
    sourceWidth: Int = 1080,
    sourceHeight: Int = 1920
) {
    val appColors = LocalAppColors.current
    val scrollState = rememberScrollState()

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
                .padding(12.dp)
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
                        tint = appColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Aspect Ratio & Framing Crop",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedRatio != VideoAspectRatio.ORIGINAL) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRatioSelected(VideoAspectRatio.ORIGINAL) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset",
                            tint = appColors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Reset",
                            color = appColors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Original",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontally Scrollable Aspect Ratio Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoAspectRatio.values().forEach { ratio ->
                    val isSelected = ratio == selectedRatio
                    val (outW, outH) = ratio.getOutputDimensions(sourceWidth, sourceHeight)

                    Surface(
                        modifier = Modifier
                            .clickable { onRatioSelected(ratio) }
                            .testTag("aspect_chip_${ratio.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) appColors.primary else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) appColors.primary else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Aspect Ratio Geometric Mini Icon Preview
                            Box(
                                modifier = Modifier
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val (iconW, iconH) = when (ratio) {
                                    VideoAspectRatio.FREEFORM -> Pair(15.dp, 13.dp)
                                    VideoAspectRatio.ORIGINAL -> Pair(16.dp, 16.dp)
                                    VideoAspectRatio.PORTRAIT_9_16 -> Pair(10.dp, 18.dp)
                                    VideoAspectRatio.LANDSCAPE_16_9 -> Pair(20.dp, 11.dp)
                                    VideoAspectRatio.SQUARE_1_1 -> Pair(14.dp, 14.dp)
                                    VideoAspectRatio.FEED_4_5 -> Pair(12.dp, 15.dp)
                                    VideoAspectRatio.PORTRAIT_3_4 -> Pair(12.dp, 16.dp)
                                    VideoAspectRatio.LANDSCAPE_4_3 -> Pair(16.dp, 12.dp)
                                    VideoAspectRatio.CINEMATIC_21_9 -> Pair(22.dp, 9.dp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = iconW, height = iconH)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                        .background(
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }

                            Text(
                                text = ratio.title.split(" ").first(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )

                            if (outW > 0 && outH > 0) {
                                Text(
                                    text = "${outW}×${outH}",
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

