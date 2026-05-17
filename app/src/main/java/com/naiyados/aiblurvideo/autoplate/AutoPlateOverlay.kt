package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun AutoPlateOverlay(
    modifier: Modifier = Modifier,
    boxes: List<AutoPlateBox>
) {
    if (boxes.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            boxes.forEach { box ->
                val mappedRect = mapRectFitCenter(
                    source = box.rect.expand(percent = 0.25f),
                    frameWidth = box.frameWidth.toFloat(),
                    frameHeight = box.frameHeight.toFloat(),
                    viewWidth = viewWidth,
                    viewHeight = viewHeight
                )

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.86f),
                    topLeft = Offset(mappedRect.left, mappedRect.top),
                    size = Size(mappedRect.width(), mappedRect.height()),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
    }
}

private fun RectF.expand(percent: Float): RectF {
    val dx = width() * percent
    val dy = height() * percent

    return RectF(
        left - dx,
        top - dy,
        right + dx,
        bottom + dy
    )
}

private fun mapRectFitCenter(
    source: RectF,
    frameWidth: Float,
    frameHeight: Float,
    viewWidth: Float,
    viewHeight: Float
): RectF {
    val frameRatio = frameWidth / frameHeight
    val viewRatio = viewWidth / viewHeight

    val scale: Float
    val offsetX: Float
    val offsetY: Float

    if (frameRatio > viewRatio) {
        scale = viewWidth / frameWidth
        offsetX = 0f
        offsetY = (viewHeight - frameHeight * scale) / 2f
    } else {
        scale = viewHeight / frameHeight
        offsetX = (viewWidth - frameWidth * scale) / 2f
        offsetY = 0f
    }

    return RectF(
        offsetX + source.left * scale,
        offsetY + source.top * scale,
        offsetX + source.right * scale,
        offsetY + source.bottom * scale
    )
}