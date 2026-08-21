package com.naiyados.aiblurvideo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun DetectionProgressCard(
    isScanning: Boolean,
    progress: Float,
    framesScanned: Int,
    totalFrames: Int,
    detectionsFound: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = (progress * 100).toInt().coerceIn(0, 100)

    AnimatedVisibility(
        visible = isScanning,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .testTag("detection_progress_card"),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xEB131622),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Compact Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )

                        Text(
                            text = if (totalFrames > 0) "Scanning: $framesScanned/$totalFrames ($detectionsFound plates)" else "Scanning: $framesScanned frames ($detectionsFound plates)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$percentage%",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("detection_progress_percentage")
                        )

                        Surface(
                            onClick = onCancel,
                            shape = CircleShape,
                            color = Color(0xFFFF5252).copy(alpha = 0.20f),
                            modifier = Modifier
                                .size(22.dp)
                                .testTag("cancel_detection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel Scan",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .testTag("detection_progress_bar"),
                    color = Color(0xFF00E5FF),
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }
        }
    }
}
