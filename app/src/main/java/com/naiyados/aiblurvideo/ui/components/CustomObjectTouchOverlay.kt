package com.naiyados.aiblurvideo.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag

@Composable
fun CustomObjectTouchOverlay(
    normalizedRect: RectF?,
    onRectChanged: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentNormRect by remember(normalizedRect) {
        mutableStateOf(normalizedRect ?: RectF(0.3f, 0.35f, 0.7f, 0.65f))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("custom_object_touch_overlay")
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val normX = (tapOffset.x / size.width).coerceIn(0.1f, 0.9f)
                    val normY = (tapOffset.y / size.height).coerceIn(0.1f, 0.9f)
                    val halfW = 0.18f
                    val halfH = 0.12f
                    val newRect = RectF(
                        (normX - halfW).coerceAtLeast(0f),
                        (normY - halfH).coerceAtLeast(0f),
                        (normX + halfW).coerceAtMost(1f),
                        (normY + halfH).coerceAtMost(1f)
                    )
                    currentNormRect = newRect
                    onRectChanged(newRect)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / size.width
                    val dy = dragAmount.y / size.height

                    val width = currentNormRect.width()
                    val height = currentNormRect.height()

                    var newLeft = (currentNormRect.left + dx).coerceIn(0f, 1f - width)
                    var newTop = (currentNormRect.top + dy).coerceIn(0f, 1f - height)

                    val updated = RectF(newLeft, newTop, newLeft + width, newTop + height)
                    currentNormRect = updated
                    onRectChanged(updated)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = currentNormRect.left * size.width
            val top = currentNormRect.top * size.height
            val width = currentNormRect.width() * size.width
            val height = currentNormRect.height() * size.height

            // Semi-transparent frosted box interior
            drawRoundRect(
                color = Color(0x33FF007F),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Neon dashed outline
            drawRoundRect(
                color = Color(0xFFFF007F),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                )
            )

            // Corner brackets
            val bracketLen = 22f
            val strokeW = 6f
            val cornerColor = Color(0xFF00E5FF)

            // Top-left
            drawLine(cornerColor, Offset(left, top), Offset(left + bracketLen, top), strokeW)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + bracketLen), strokeW)
            // Top-right
            drawLine(cornerColor, Offset(left + width, top), Offset(left + width - bracketLen, top), strokeW)
            drawLine(cornerColor, Offset(left + width, top), Offset(left + width, top + bracketLen), strokeW)
            // Bottom-left
            drawLine(cornerColor, Offset(left, top + height), Offset(left + bracketLen, top + height), strokeW)
            drawLine(cornerColor, Offset(left, top + height), Offset(left, top + height - bracketLen), strokeW)
            // Bottom-right
            drawLine(cornerColor, Offset(left + width, top + height), Offset(left + width - bracketLen, top + height), strokeW)
            drawLine(cornerColor, Offset(left + width, top + height), Offset(left + width, top + height - bracketLen), strokeW)
        }
    }
}
