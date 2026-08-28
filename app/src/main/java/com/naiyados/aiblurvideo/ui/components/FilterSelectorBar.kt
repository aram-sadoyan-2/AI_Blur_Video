package com.naiyados.aiblurvideo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.model.VideoFilter
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun FilterSelectorBar(
    selectedFilter: VideoFilter,
    filterIntensity: Float,
    onFilterSelected: (VideoFilter) -> Unit,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("filter_selector_bar"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = AiBlurColors.Purple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Cinematic Color Filters",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = selectedFilter.title,
                    color = AiBlurColors.Purple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Scroll Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VideoFilter.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    val gradient = getFilterGradient(filter)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onFilterSelected(filter) }
                            .testTag("filter_chip_${filter.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(brush = gradient)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(14.dp)
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = filter.title,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Intensity slider if active filter is selected
            if (selectedFilter != VideoFilter.NONE) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Intensity",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    Slider(
                        value = filterIntensity,
                        onValueChange = onIntensityChange,
                        valueRange = 0.2f..1.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AiBlurColors.Purple,
                            activeTrackColor = AiBlurColors.Purple,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )

                    Text(
                        text = "${(filterIntensity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getFilterGradient(filter: VideoFilter): Brush {
    return when (filter) {
        VideoFilter.NONE -> Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF64748B)))
        VideoFilter.CINEMATIC -> Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFFD97706)))
        VideoFilter.NOIR -> Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF64748B), Color(0xFFE2E8F0)))
        VideoFilter.VIBRANT -> Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF0EA5E9), Color(0xFF10B981)))
        VideoFilter.VINTAGE -> Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFB45309)))
        VideoFilter.COOL_ICE -> Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFFE0F2FE)))
        VideoFilter.NIGHT_VISION -> Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF10B981)))
        VideoFilter.SEPIA -> Brush.linearGradient(listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFEF3C7)))
    }
}
