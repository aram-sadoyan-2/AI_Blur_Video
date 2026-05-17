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
                val transform = VideoCoordinateMapper.fitTransform(
                    frameWidth = box.frameWidth.toFloat(),
                    frameHeight = box.frameHeight.toFloat(),
                    viewWidth = viewWidth,
                    viewHeight = viewHeight
                )

                val mappedRect = VideoCoordinateMapper.frameToView(
                    frameRect = box.rect.expand(percent = 0.12f),
                    transform = transform
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
