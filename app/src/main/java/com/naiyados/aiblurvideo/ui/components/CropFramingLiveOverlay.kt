package com.naiyados.aiblurvideo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.theme.LocalAppColors

@Composable
fun CropFramingLiveOverlay(
    aspectRatio: VideoAspectRatio,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val (outW, outH) = aspectRatio.getOutputDimensions(videoWidth, videoHeight)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("crop_framing_live_overlay")
    ) {
        // 1. Rule of Thirds Composition Grid & Corner Brackets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (w <= 0f || h <= 0f) return@Canvas

            val gridColor = Color.White.copy(alpha = 0.28f)
            val strokeWidth = 1.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // Rule of thirds vertical lines
            drawLine(
                color = gridColor,
                start = Offset(w / 3f, 0f),
                end = Offset(w / 3f, h),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
            drawLine(
                color = gridColor,
                start = Offset(2f * w / 3f, 0f),
                end = Offset(2f * w / 3f, h),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )

            // Rule of thirds horizontal lines
            drawLine(
                color = gridColor,
                start = Offset(0f, h / 3f),
                end = Offset(w, h / 3f),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, 2f * h / 3f),
                end = Offset(w, 2f * h / 3f),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )

            // High-precision Corner Brackets
            val bracketLen = (w.coerceAtMost(h) * 0.12f).coerceIn(16.dp.toPx(), 32.dp.toPx())
            val bracketStroke = 2.5.dp.toPx()
            val bracketColor = Color(0xFF6366F1) // Indigo accent

            // Top-Left Corner Bracket
            drawLine(bracketColor, Offset(0f, 0f), Offset(bracketLen, 0f), bracketStroke)
            drawLine(bracketColor, Offset(0f, 0f), Offset(0f, bracketLen), bracketStroke)

            // Top-Right Corner Bracket
            drawLine(bracketColor, Offset(w, 0f), Offset(w - bracketLen, 0f), bracketStroke)
            drawLine(bracketColor, Offset(w, 0f), Offset(w, bracketLen), bracketStroke)

            // Bottom-Left Corner Bracket
            drawLine(bracketColor, Offset(0f, h), Offset(bracketLen, h), bracketStroke)
            drawLine(bracketColor, Offset(0f, h), Offset(0f, h - bracketLen), bracketStroke)

            // Bottom-Right Corner Bracket
            drawLine(bracketColor, Offset(w, h), Offset(w - bracketLen, h), bracketStroke)
            drawLine(bracketColor, Offset(w, h), Offset(w, h - bracketLen), bracketStroke)

            // Center subtle crosshair
            val crosshairLen = 8.dp.toPx()
            val crosshairColor = Color.White.copy(alpha = 0.45f)
            val cx = w / 2f
            val cy = h / 2f
            drawLine(crosshairColor, Offset(cx - crosshairLen, cy), Offset(cx + crosshairLen, cy), 1.dp.toPx())
            drawLine(crosshairColor, Offset(cx, cy - crosshairLen), Offset(cx, cy + crosshairLen), 1.dp.toPx())
        }

        // 2. Floating Crop Info Badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .testTag("crop_dimension_badge"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xDD0F172A),
            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), CircleShape)
                )

                Text(
                    text = aspectRatio.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )

                Text(
                    text = if (outW > 0 && outH > 0) "${outW} × ${outH}" else if (videoWidth > 0) "${videoWidth} × ${videoHeight}" else "Crop Active",
                    color = Color(0xFF818CF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
