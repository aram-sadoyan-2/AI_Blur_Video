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
    isPlateBlurEnabled: Boolean = true,
    isFaceBlurEnabled: Boolean = true,
    showDebugBoundingBoxes: Boolean = true,
    blurStrength: Float = 0.65f
) {
    if (boxes.isEmpty()) return

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    val badgeBgPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(230, 13, 17, 26)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    val plateBadgeBorderPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(255, 0, 230, 118)
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    val faceBadgeBorderPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(255, 0, 229, 255)
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
                val isFace = box.targetType == DetectionTarget.FACE
                val isTargetBlurActive = if (isFace) (isFaceBlurEnabled && isBlurEnabled) else (isPlateBlurEnabled && isBlurEnabled)

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

                // Render Blur / Privacy Shield if enabled for this target
                if (isTargetBlurActive) {
                    val strengthClamped = blurStrength.coerceIn(0.1f, 1.0f)
                    val maskAlpha = 0.45f + strengthClamped * 0.54f
                    val strokeAlpha = 0.5f + strengthClamped * 0.45f

                    val cornerRadius = if (isFace) mappedPaddedRect.width() * 0.35f else 10f

                    // Solid/diffused privacy cover based on strength
                    drawRoundRect(
                        color = Color.Black.copy(alpha = maskAlpha),
                        topLeft = Offset(mappedPaddedRect.left, mappedPaddedRect.top),
                        size = Size(mappedPaddedRect.width(), mappedPaddedRect.height()),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )

                    // Accent glowing border around blurred area
                    val borderGradient = if (isFace) {
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = strokeAlpha),
                            Color(0xFF7A22FF).copy(alpha = strokeAlpha)
                        )
                    } else {
                        listOf(
                            Color(0xFFFF2B85).copy(alpha = strokeAlpha),
                            Color(0xFF7A22FF).copy(alpha = strokeAlpha)
                        )
                    }

                    drawRoundRect(
                        brush = Brush.linearGradient(borderGradient),
                        topLeft = Offset(mappedPaddedRect.left, mappedPaddedRect.top),
                        size = Size(mappedPaddedRect.width(), mappedPaddedRect.height()),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
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

                    val boxColor = if (isFace) Color(0xFF00E5FF) else Color(0xFF00E676)
                    val reticleColor = if (isFace) Color(0xFF9D4EDD) else Color(0xFF00E5FF)
                    val cornerRadius = if (isFace) 8f else 4f

                    // Bounding box outline
                    drawRoundRect(
                        color = boxColor,
                        topLeft = Offset(rawLeft, rawTop),
                        size = Size(rawW, rawH),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = 2.5f)
                    )

                    // Corner reticle accents for high-tech CV tracking visual
                    val reticleLen = (rawW.coerceAtMost(rawH) * 0.25f).coerceIn(8f, 22f)
                    val reticleStroke = Stroke(width = 3.5f)

                    // Top-Left corner
                    drawLine(reticleColor, Offset(rawLeft, rawTop), Offset(rawLeft + reticleLen, rawTop), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawLeft, rawTop), Offset(rawLeft, rawTop + reticleLen), reticleStroke.width)

                    // Top-Right corner
                    drawLine(reticleColor, Offset(rawRight, rawTop), Offset(rawRight - reticleLen, rawTop), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawRight, rawTop), Offset(rawRight, rawTop - reticleLen), reticleStroke.width)

                    // Bottom-Left corner
                    drawLine(reticleColor, Offset(rawLeft, rawBottom), Offset(rawLeft + reticleLen, rawBottom), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawLeft, rawBottom), Offset(rawLeft, rawBottom - reticleLen), reticleStroke.width)

                    // Bottom-Right corner
                    drawLine(reticleColor, Offset(rawRight, rawBottom), Offset(rawRight - reticleLen, rawBottom), reticleStroke.width)
                    drawLine(reticleColor, Offset(rawRight, rawBottom), Offset(rawRight, rawBottom - reticleLen), reticleStroke.width)

                    // Padded boundary indicator
                    drawRoundRect(
                        color = boxColor.copy(alpha = 0.45f),
                        topLeft = Offset(mappedPaddedRect.left, mappedPaddedRect.top),
                        size = Size(mappedPaddedRect.width(), mappedPaddedRect.height()),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 1.5f)
                    )

                    // Telemetry Debug Badge Tag above the bounding box
                    val confidenceText = if (box.confidence > 0f) {
                        "${(box.confidence * 100).toInt()}%"
                    } else {
                        "DETECTED"
                    }
                    val displayText = when {
                        isFace -> if (box.text.isNotBlank()) "${box.text.trim()} • $confidenceText" else "FACE • $confidenceText"
                        box.text.isNotBlank() -> "${box.text.trim()} • $confidenceText"
                        else -> "PLATE • $confidenceText"
                    }

                    val textWidth = textPaint.measureText(displayText)
                    val badgePaddingH = 14f
                    val badgeHeight = 34f
                    val badgeWidth = textWidth + badgePaddingH * 2

                    var badgeTop = rawTop - badgeHeight - 6f
                    if (badgeTop < 10f) {
                        badgeTop = rawBottom + 6f
                    }
                    val badgeLeft = rawLeft.coerceIn(8f, (viewWidth - badgeWidth - 8f).coerceAtLeast(8f))

                    // Draw native debug badge
                    val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)
                    val currentBorderPaint = if (isFace) faceBadgeBorderPaint else plateBadgeBorderPaint

                    drawContext.canvas.nativeCanvas.drawRoundRect(badgeRect, 8f, 8f, badgeBgPaint)
                    drawContext.canvas.nativeCanvas.drawRoundRect(badgeRect, 8f, 8f, currentBorderPaint)
                    drawContext.canvas.nativeCanvas.drawText(
                        displayText,
                        badgeLeft + badgePaddingH,
                        badgeTop + badgeHeight - 10f,
                        textPaint
                    )
                } else if (!isTargetBlurActive) {
                    // Minimal tracking wireframe when both debug boxes & blur are off
                    val wireColor = if (isFace) Color(0xFF00E5FF).copy(alpha = 0.45f) else Color(0xFF00E676).copy(alpha = 0.45f)
                    drawRoundRect(
                        color = wireColor,
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


