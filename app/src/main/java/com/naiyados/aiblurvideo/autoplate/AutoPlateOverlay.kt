package com.naiyados.aiblurvideo.autoplate

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun AutoPlateOverlay(
    modifier: Modifier = Modifier,
    boxes: List<AutoPlateBox>,
    isBlurEnabled: Boolean = true,
    showDebugBoundingBoxes: Boolean = true,
    blurStrength: Float = 0.65f
) {
    if (boxes.isEmpty()) return

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val badgeBgPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(220, 15, 23, 42)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    val badgeBorderPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(255, 0, 230, 118)
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            boxes.forEach { box ->
                val transform = VideoCoordinateMapper.fitTransform(
                    frameWidth = box.frameWidth.toFloat(),
                    frameHeight = box.frameHeight.toFloat(),
                    viewWidth = viewWidth,
                    viewHeight = viewHeight
                )

                // 1. Padded region used for blur coverage
                val paddedRect = PlateMaskInsets.paddingForCover(box.rect)
                val mappedPaddedRect = VideoCoordinateMapper.frameToView(
                    frameRect = paddedRect,
                    transform = transform
                )

                // 2. Raw unpadded detection bounding box
                val mappedRawRect = VideoCoordinateMapper.frameToView(
                    frameRect = box.rect,
                    transform = transform
                )

                // Render Blur / Privacy Shield if enabled
                if (isBlurEnabled) {
                    // Solid black privacy cover over license plate
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.94f),
                        topLeft = Offset(mappedPaddedRect.left, mappedPaddedRect.top),
                        size = Size(mappedPaddedRect.width(), mappedPaddedRect.height()),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // Accent glowing border around blurred area
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFFF2B85).copy(alpha = 0.85f),
                                Color(0xFF7A22FF).copy(alpha = 0.85f)
                            )
                        ),
                        topLeft = Offset(mappedPaddedRect.left, mappedPaddedRect.top),
                        size = Size(mappedPaddedRect.width(), mappedPaddedRect.height()),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 2.5f)
                    )
                }

                // Render Debug Bounding Box Overlay if toggle is active
                if (showDebugBoundingBoxes) {
                    val rawLeft = mappedRawRect.left
                    val rawTop = mappedRawRect.top
                    val rawRight = mappedRawRect.right
                    val rawBottom = mappedRawRect.bottom
                    val rawW = mappedRawRect.width()
                    val rawH = mappedRawRect.height()

                    // Neon Green bounding box outline
                    drawRoundRect(
                        color = Color(0xFF00E676),
                        topLeft = Offset(rawLeft, rawTop),
                        size = Size(rawW, rawH),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2.5f)
                    )

                    // Corner reticle accents for high-tech CV tracking visual
                    val reticleLen = (rawW.coerceAtMost(rawH) * 0.25f).coerceIn(8f, 22f)
                    val reticleColor = Color(0xFF00E5FF)
                    val reticleStroke = Stroke(width = 3.5f)

                    // Top-Left corner
                    drawLine(reticleColor, Offset(rawLeft, rawTop), Offset(rawLeft + reticleLen, rawTop), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawLeft, rawTop), Offset(rawLeft, rawTop + reticleLen), reticleStroke.width)

                    // Top-Right corner
                    drawLine(reticleColor, Offset(rawRight, rawTop), Offset(rawRight - reticleLen, rawTop), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawRight, rawTop), Offset(rawRight, rawTop + reticleLen), reticleStroke.width)

                    // Bottom-Left corner
                    drawLine(reticleColor, Offset(rawLeft, rawBottom), Offset(rawLeft + reticleLen, rawBottom), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawLeft, rawBottom), Offset(rawLeft, rawBottom - reticleLen), reticleStroke.width)

                    // Bottom-Right corner
                    drawLine(reticleColor, Offset(rawRight, rawBottom), Offset(rawRight - reticleLen, rawBottom), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawRight, rawBottom), Offset(rawRight, rawBottom - reticleLen), reticleStroke.width)

                    // Padded boundary indicator (fine dotted/dashed cyan outline)
                    drawRoundRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                        topLeft = Offset(mappedPaddedRect.left, mappedPaddedRect.top),
                        size = Size(mappedPaddedRect.width(), mappedPaddedRect.height()),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1.5f)
                    )

                    // Telemetry Debug Badge Tag above the bounding box
                    val confidenceText = if (box.confidence > 0f) {
                        "${(box.confidence * 100).toInt()}%"
                    } else {
                        "DETECTED"
                    }
                    val displayText = if (box.text.isNotBlank()) {
                        "${box.text.trim()} • $confidenceText"
                    } else {
                        "PLATE • $confidenceText"
                    }

                    val textWidth = textPaint.measureText(displayText)
                    val badgePaddingH = 16f
                    val badgeHeight = 38f
                    val badgeWidth = textWidth + badgePaddingH * 2

                    var badgeTop = rawTop - badgeHeight - 6f
                    if (badgeTop < 10f) {
                        badgeTop = rawBottom + 6f
                    }
                    val badgeLeft = rawLeft.coerceIn(8f, (viewWidth - badgeWidth - 8f).coerceAtLeast(8f))

                    // Draw native debug badge
                    val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)
                    drawContext.canvas.nativeCanvas.drawRoundRect(badgeRect, 10f, 10f, badgeBgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(badgeRect, 10f, 10f, badgeBorderPaint)
                    drawContext.canvas.nativeCanvas.drawText(
                        displayText,
                        badgeLeft + badgePaddingH,
                        badgeTop + badgeHeight - 11f,
                        textPaint
                    )
                } else if (!isBlurEnabled) {
                    // Minimal tracking wireframe when both debug boxes & blur are off
                    drawRoundRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                        topLeft = Offset(mappedRawRect.left, mappedRawRect.top),
                        size = Size(mappedRawRect.width(), mappedRawRect.height()),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }
    }
}


