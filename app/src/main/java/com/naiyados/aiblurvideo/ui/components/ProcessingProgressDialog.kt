package com.naiyados.aiblurvideo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun ProcessingProgressDialog(
    progress: Float,
    exportSettings: ExportSettings = ExportSettings(),
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val percent = (progress * 100).toInt().coerceIn(0, 100)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(26.dp)),
            color = Color(0xFF1B1B28),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Icon Header
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AiBlurColors.Pink.copy(alpha = 0.35f),
                                    AiBlurColors.Purple.copy(alpha = 0.35f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = AiBlurColors.Pink,
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Processing Video",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "Applying AI blur and re-encoding video frames",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Settings Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = AiBlurColors.Pink,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${exportSettings.resolution.label} • ${exportSettings.bitrate.label}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Progress Bar & Percentage
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame-by-frame encoding & saving",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$percent%",
                            color = AiBlurColors.Pink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.testTag("saving_progress_percentage")
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("saving_progress_bar"),
                        color = AiBlurColors.Pink,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_processing_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFF5252).copy(alpha = 0.10f),
                        contentColor = Color(0xFFFF5252)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StopCircle,
                            contentDescription = "Cancel Operation",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Cancel Processing",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Stops encoding immediately and frees system memory",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
