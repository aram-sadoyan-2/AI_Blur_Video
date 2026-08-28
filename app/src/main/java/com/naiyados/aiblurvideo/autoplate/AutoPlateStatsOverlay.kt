package com.naiyados.aiblurvideo.autoplate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

/**
 * Real-time overlay displaying AI inference telemetry:
 * - Number of license plates detected
 * - Real-time inference latency in milliseconds
 * - Live inference tracker pulse and detection confidence
 */
@Composable
fun AutoPlateStatsOverlay(
    stats: PlateInferenceStats,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var isExpanded by remember { mutableStateOf(false) }

    // Pulsing live indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val latencyColor = when {
        stats.latencyMs == 0L -> Color.White.copy(alpha = 0.6f)
        stats.latencyMs <= 35L -> Color(0xFF10B981) // Emerald
        stats.latencyMs <= 70L -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    Surface(
        modifier = modifier
            .testTag("inference_stats_overlay")
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xDD0F172A),
        border = BorderStroke(
            width = 1.dp,
            color = if (stats.detectedCount > 0) Color(0xFF6366F1).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Header: Live Indicator & Micro Summary
            Row(
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(if (stats.isLiveTracking) pulseAlpha else 0.5f)
                        .background(
                            color = if (stats.isLiveTracking) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )

                Text(
                    text = if (stats.detectedCount > 0) "AI • ${stats.detectedCount} plate${if (stats.detectedCount > 1) "s" else ""}" else "AI SCAN",
                    color = if (stats.detectedCount > 0) Color(0xFF818CF8) else Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.3.sp
                )

                if (stats.latencyMs > 0L) {
                    Text(
                        text = "• ${stats.latencyMs}ms",
                        color = latencyColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse stats" else "Expand stats",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Metric 1: Plates Detected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = if (stats.detectedCount > 0) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Plates Detected:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${stats.detectedCount}",
                            color = if (stats.detectedCount > 0) Color(0xFF818CF8) else Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("stats_plates_detected_count")
                        )
                    }

                    // Metric 2: Inference Latency in Milliseconds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = latencyColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Inference Latency:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (stats.latencyMs > 0L) "${stats.latencyMs} ms" else "< 16 ms",
                            color = latencyColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("stats_inference_latency_value")
                        )
                    }

                    // Metric 3: Confidence / Plate Text (if present)
                    if (stats.confidence > 0f || !stats.dominantText.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = AiBlurColors.Pink,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Confidence:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(stats.confidence * 100).toInt()}%" +
                                        if (!stats.dominantText.isNullOrBlank()) " (${stats.dominantText})" else "",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Metric 4: Engine info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = stats.detectorEngine,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
