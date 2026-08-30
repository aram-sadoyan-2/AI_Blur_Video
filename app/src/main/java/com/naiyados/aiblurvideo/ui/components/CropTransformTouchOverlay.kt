package com.naiyados.aiblurvideo.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private enum class CropHandleType {
    NONE,
    BODY,
    RESET_BUTTON,    // Top-Left (X)
    ROTATE_BUTTON,   // Top-Right (Rotate ⟳)
    SCALE_BUTTON,    // Bottom-Right (Scale ⤢)
    SCALE_BL_BUTTON, // Bottom-Left (Scale ⤢)
    EDGE_TOP,        // Top edge center dot
    EDGE_BOTTOM,     // Bottom edge center dot
    EDGE_LEFT,       // Left edge center dot
    EDGE_RIGHT       // Right edge center dot
}

@Composable
fun CropTransformTouchOverlay(
    cropRect: RectF?,
    rotationDegrees: Float = 0f,
    videoWidth: Int = 1080,
    videoHeight: Int = 1920,
    aspectRatio: VideoAspectRatio = VideoAspectRatio.ORIGINAL,
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onCropRectChanged: (RectF) -> Unit,
    onRotationChanged: (Float) -> Unit = {},
    onResetCrop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentNormRect by remember(cropRect) {
        mutableStateOf(cropRect ?: RectF(0.05f, 0.05f, 0.95f, 0.95f))
    }

    var currentRotation by remember(rotationDegrees) {
        mutableFloatStateOf(rotationDegrees)
    }

    var activeHandle by remember {
        mutableStateOf(CropHandleType.NONE)
    }

    val minSizeNorm = 0.08f

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("crop_transform_touch_overlay")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()

                    if (w <= 0f || h <= 0f) return@awaitEachGesture

                    var cx = currentNormRect.centerX() * w
                    var cy = currentNormRect.centerY() * h
                    var curW = currentNormRect.width() * w
                    var curH = currentNormRect.height() * h

                    // Check which handle or body was touched
                    val unrotatedTouch = rotatePoint(firstDown.position.x, firstDown.position.y, cx, cy, -currentRotation)
                    val halfW = curW / 2f
                    val halfH = curH / 2f
                    val left = cx - halfW
                    val right = cx + halfW
                    val top = cy - halfH
                    val bottom = cy + halfH

                    val buttonHitRadius = 48f // Generous hit area for circular buttons
                    val dotHitRadius = 38f    // Generous hit area for edge midpoint dots

                    activeHandle = when {
                        // Top-Left (X) reset handle
                        distance(unrotatedTouch.x, unrotatedTouch.y, left, top) <= buttonHitRadius -> {
                            CropHandleType.RESET_BUTTON
                        }
                        // Top-Right (Rotate ⟳) handle
                        distance(unrotatedTouch.x, unrotatedTouch.y, right, top) <= buttonHitRadius -> {
                            CropHandleType.ROTATE_BUTTON
                        }
                        // Bottom-Right (Scale ⤢) handle
                        distance(unrotatedTouch.x, unrotatedTouch.y, right, bottom) <= buttonHitRadius -> {
                            CropHandleType.SCALE_BUTTON
                        }
                        // Bottom-Left (Scale ⤢) handle
                        distance(unrotatedTouch.x, unrotatedTouch.y, left, bottom) <= buttonHitRadius -> {
                            CropHandleType.SCALE_BL_BUTTON
                        }
                        // 4 Edge midpoint dots
                        distance(unrotatedTouch.x, unrotatedTouch.y, cx, top) <= dotHitRadius -> CropHandleType.EDGE_TOP
                        distance(unrotatedTouch.x, unrotatedTouch.y, cx, bottom) <= dotHitRadius -> CropHandleType.EDGE_BOTTOM
                        distance(unrotatedTouch.x, unrotatedTouch.y, left, cy) <= dotHitRadius -> CropHandleType.EDGE_LEFT
                        distance(unrotatedTouch.x, unrotatedTouch.y, right, cy) <= dotHitRadius -> CropHandleType.EDGE_RIGHT
                        // Inside Box Body
                        unrotatedTouch.x in left..right && unrotatedTouch.y in top..bottom -> CropHandleType.BODY
                        else -> {
                            // Outside touch - reposition center to touch
                            val normX = (firstDown.position.x / w).coerceIn(0.1f, 0.9f)
                            val normY = (firstDown.position.y / h).coerceIn(0.1f, 0.9f)
                            val bw = currentNormRect.width()
                            val bh = currentNormRect.height()
                            val nLeft = (normX - bw / 2f).coerceIn(0f, 1f - bw)
                            val nTop = (normY - bh / 2f).coerceIn(0f, 1f - bh)
                            val updated = RectF(nLeft, nTop, nLeft + bw, nTop + bh)
                            currentNormRect = updated
                            onCropRectChanged(updated)
                            CropHandleType.BODY
                        }
                    }

                    firstDown.consume()

                    // If user tapped Top-Left Reset button, trigger reset on tap up
                    if (activeHandle == CropHandleType.RESET_BUTTON) {
                        val fullRect = RectF(0f, 0f, 1f, 1f)
                        currentNormRect = fullRect
                        currentRotation = 0f
                        onCropRectChanged(fullRect)
                        onRotationChanged(0f)
                        onResetCrop()
                    }

                    // If user tapped Bottom-Left Play/Pause button
                    if (activeHandle == CropHandleType.PLAY_BUTTON) {
                        onPlayPauseClick()
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.isEmpty()) {
                            activeHandle = CropHandleType.NONE
                            break
                        }

                        if (activePointers.size >= 2) {
                            // Multi-touch: Pinch to Zoom & Rotate & Pan
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            val pan = event.calculatePan()

                            activePointers.forEach { it.consume() }

                            var updatedCx = (currentNormRect.centerX() * w + pan.x).coerceIn(0f, w)
                            var updatedCy = (currentNormRect.centerY() * h + pan.y).coerceIn(0f, h)
                            var updatedW = (currentNormRect.width() * w * zoom).coerceIn(minSizeNorm * w, w * 1.0f)
                            var updatedH = (currentNormRect.height() * h * zoom).coerceIn(minSizeNorm * h, h * 1.0f)

                            val nLeft = ((updatedCx - updatedW / 2f) / w).coerceIn(0f, 1f - updatedW / w)
                            val nTop = ((updatedCy - updatedH / 2f) / h).coerceIn(0f, 1f - updatedH / h)
                            val nRight = nLeft + updatedW / w
                            val nBottom = nTop + updatedH / h

                            val updatedRect = RectF(nLeft, nTop, nRight, nBottom)
                            currentNormRect = updatedRect
                            onCropRectChanged(updatedRect)

                            if (rotation != 0f) {
                                var newAngle = (currentRotation + rotation) % 360f
                                if (newAngle < 0f) newAngle += 360f
                                // Snap close to 0, 90, 180, 270
                                if (newAngle in 357f..360f || newAngle in 0f..3f) newAngle = 0f
                                else if (newAngle in 87f..93f) newAngle = 90f
                                else if (newAngle in 177f..183f) newAngle = 180f
                                else if (newAngle in 267f..273f) newAngle = 270f

                                currentRotation = newAngle
                                onRotationChanged(newAngle)
                            }
                        } else {
                            // Single pointer drag
                            val pointer = activePointers.first()
                            val dragDelta = pointer.positionChange()
                            pointer.consume()

                            if (dragDelta != Offset.Zero) {
                                cx = currentNormRect.centerX() * w
                                cy = currentNormRect.centerY() * h
                                curW = currentNormRect.width() * w
                                curH = currentNormRect.height() * h

                                if (activeHandle == CropHandleType.ROTATE_BUTTON) {
                                    val currentTouch = pointer.position
                                    val angleRad = atan2((currentTouch.y - cy).toDouble(), (currentTouch.x - cx).toDouble())
                                    // Top-right angle offset is -45 degrees
                                    var deg = (Math.toDegrees(angleRad).toFloat() + 45f) % 360f
                                    if (deg < 0f) deg += 360f
                                    // Snapping
                                    if (deg in 357f..360f || deg in 0f..3f) deg = 0f
                                    else if (deg in 87f..93f) deg = 90f
                                    else if (deg in 177f..183f) deg = 180f
                                    else if (deg in 267f..273f) deg = 270f

                                    currentRotation = deg
                                    onRotationChanged(deg)
                                } else if (activeHandle == CropHandleType.BODY) {
                                    val dxNorm = dragDelta.x / w
                                    val dyNorm = dragDelta.y / h
                                    val boxW = currentNormRect.width()
                                    val boxH = currentNormRect.height()

                                    val newLeft = (currentNormRect.left + dxNorm).coerceIn(0f, 1f - boxW)
                                    val newTop = (currentNormRect.top + dyNorm).coerceIn(0f, 1f - boxH)
                                    val updated = RectF(newLeft, newTop, newLeft + boxW, newTop + boxH)
                                    currentNormRect = updated
                                    onCropRectChanged(updated)
                                } else {
                                    // Scale & Edge resizing with rotation transformation
                                    val rad = Math.toRadians(currentRotation.toDouble())
                                    val cosA = cos(rad).toFloat()
                                    val sinA = sin(rad).toFloat()

                                    val localDx = dragDelta.x * cosA + dragDelta.y * sinA
                                    val localDy = -dragDelta.x * sinA + dragDelta.y * cosA

                                    var deltaW = 0f
                                    var deltaH = 0f
                                    var shiftLocalX = 0f
                                    var shiftLocalY = 0f

                                    when (activeHandle) {
                                        CropHandleType.SCALE_BUTTON -> {
                                            // Bottom-Right: scale width & height together or by drag vector
                                            val scaleDelta = max(localDx, localDy)
                                            deltaW = scaleDelta * 2f
                                            deltaH = scaleDelta * 2f
                                        }
                                        CropHandleType.SCALE_BL_BUTTON -> {
                                            // Bottom-Left: scale
                                            val scaleDelta = max(-localDx, localDy)
                                            deltaW = scaleDelta * 2f
                                            deltaH = scaleDelta * 2f
                                        }
                                        CropHandleType.EDGE_TOP -> {
                                            deltaH = -localDy
                                            shiftLocalY = localDy / 2f
                                        }
                                        CropHandleType.EDGE_BOTTOM -> {
                                            deltaH = localDy
                                            shiftLocalY = localDy / 2f
                                        }
                                        CropHandleType.EDGE_LEFT -> {
                                            deltaW = -localDx
                                            shiftLocalX = localDx / 2f
                                        }
                                        CropHandleType.EDGE_RIGHT -> {
                                            deltaW = localDx
                                            shiftLocalX = localDx / 2f
                                        }
                                        else -> {}
                                    }

                                    val newPixelW = (curW + deltaW).coerceIn(minSizeNorm * w, w * 1.0f)
                                    val newPixelH = (curH + deltaH).coerceIn(minSizeNorm * h, h * 1.0f)

                                    val shiftScreenX = shiftLocalX * cosA - shiftLocalY * sinA
                                    val shiftScreenY = shiftLocalX * sinA + shiftLocalY * cosA

                                    val newCenterPxX = (cx + shiftScreenX).coerceIn(newPixelW / 2f, w - newPixelW / 2f)
                                    val newCenterPxY = (cy + shiftScreenY).coerceIn(newPixelH / 2f, h - newPixelH / 2f)

                                    val nLeft = (newCenterPxX - newPixelW / 2f) / w
                                    val nTop = (newCenterPxY - newPixelH / 2f) / h
                                    val nRight = nLeft + newPixelW / w
                                    val nBottom = nTop + newPixelH / h

                                    val updated = RectF(
                                        nLeft.coerceIn(0f, 1f - minSizeNorm),
                                        nTop.coerceIn(0f, 1f - minSizeNorm),
                                        nRight.coerceIn(minSizeNorm, 1f),
                                        nBottom.coerceIn(minSizeNorm, 1f)
                                    )
                                    currentNormRect = updated
                                    onCropRectChanged(updated)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val left = currentNormRect.left * w
            val top = currentNormRect.top * h
            val right = currentNormRect.right * w
            val bottom = currentNormRect.bottom * h
            val boxW = right - left
            val boxH = bottom - top
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f

            val buttonRadius = 24f  // Size of the circular white buttons
            val dotRadius = 9f      // Size of the 4 edge midpoint dots

            // 1. Outside Scrim (Dim the video content outside the crop box)
            rotate(degrees = currentRotation, pivot = Offset(cx, cy)) {
                // Dimming path with hole
                val fullPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(-w * 2, -h * 2, w * 3, h * 3))
                }
                val cropPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
                }
                val scrimPath = Path.combine(
                    androidx.compose.ui.graphics.PathOperation.Difference,
                    fullPath,
                    cropPath
                )
                drawPath(
                    path = scrimPath,
                    color = Color.Black.copy(alpha = 0.58f),
                    style = Fill
                )

                // 2. Rule of Thirds Guide Lines Inside Crop Box
                val gridColor = Color.White.copy(alpha = 0.22f)
                val strokeW = 1f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Verticals
                drawLine(gridColor, Offset(left + boxW / 3f, top), Offset(left + boxW / 3f, bottom), strokeW, pathEffect = dashEffect)
                drawLine(gridColor, Offset(left + 2f * boxW / 3f, top), Offset(left + 2f * boxW / 3f, bottom), strokeW, pathEffect = dashEffect)
                // Horizontals
                drawLine(gridColor, Offset(left, top + boxH / 3f), Offset(right, top + boxH / 3f), strokeW, pathEffect = dashEffect)
                drawLine(gridColor, Offset(left, top + 2f * boxH / 3f), Offset(right, top + 2f * boxH / 3f), strokeW, pathEffect = dashEffect)

                // 3. Crisp Solid White Box Border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    style = Stroke(width = 2.2f)
                )

                // 4. Midpoint Edge Circular White Dots (Top, Bottom, Left, Right)
                drawEdgeMidpointDot(cx, top, dotRadius)
                drawEdgeMidpointDot(cx, bottom, dotRadius)
                drawEdgeMidpointDot(left, cy, dotRadius)
                drawEdgeMidpointDot(right, cy, dotRadius)

                // 5. Top-Left (X) Close/Reset Button
                drawButtonWithIcon(
                    center = Offset(left, top),
                    radius = buttonRadius,
                    iconType = IconType.CROSS
                )

                // 6. Top-Right (⟳) Rotate Button
                drawButtonWithIcon(
                    center = Offset(right, top),
                    radius = buttonRadius,
                    iconType = IconType.ROTATE
                )

                // 7. Bottom-Right (⤢) Scale/Zoom Button
                drawButtonWithIcon(
                    center = Offset(right, bottom),
                    radius = buttonRadius,
                    iconType = IconType.SCALE
                )

                // 8. Bottom-Left (⤢) Scale Button
                drawButtonWithIcon(
                    center = Offset(left, bottom),
                    radius = buttonRadius,
                    iconType = IconType.SCALE
                )
            }
        }
    }
}

private enum class IconType {
    CROSS,
    ROTATE,
    SCALE,
    PLAY,
    PAUSE
}

private fun DrawScope.drawEdgeMidpointDot(x: Float, y: Float, radius: Float) {
    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.40f),
        radius = radius + 2f,
        center = Offset(x, y)
    )
    // Solid white dot
    drawCircle(
        color = Color.White,
        radius = radius,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawButtonWithIcon(center: Offset, radius: Float, iconType: IconType) {
    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = radius + 3f,
        center = center
    )
    // White circular button body
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center
    )

    val iconColor = Color.Black
    val stroke = 2.2f

    when (iconType) {
        IconType.CROSS -> {
            // (✕) Icon
            val r = radius * 0.42f
            drawLine(iconColor, Offset(center.x - r, center.y - r), Offset(center.x + r, center.y + r), stroke)
            drawLine(iconColor, Offset(center.x - r, center.y + r), Offset(center.x + r, center.y - r), stroke)
        }
        IconType.ROTATE -> {
            // (⟳) Icon: Arc with arrow head
            val arcR = radius * 0.45f
            drawArc(
                color = iconColor,
                startAngle = 45f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - arcR, center.y - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = stroke)
            )
            // Arrowhead at top
            val tipX = center.x + arcR * 0.70f
            val tipY = center.y - arcR * 0.70f
            val arrowLen = 5.5f
            drawLine(iconColor, Offset(tipX, tipY), Offset(tipX - arrowLen, tipY - 1f), stroke)
            drawLine(iconColor, Offset(tipX, tipY), Offset(tipX - 1f, tipY + arrowLen), stroke)
        }
        IconType.SCALE -> {
            // (⤢) Icon: Double-ended diagonal scale arrow
            val d = radius * 0.42f
            drawLine(iconColor, Offset(center.x - d, center.y - d), Offset(center.x + d, center.y + d), stroke)
            // Top-left arrowhead
            val ah = 4.5f
            drawLine(iconColor, Offset(center.x - d, center.y - d), Offset(center.x - d + ah, center.y - d), stroke)
            drawLine(iconColor, Offset(center.x - d, center.y - d), Offset(center.x - d, center.y - d + ah), stroke)
            // Bottom-right arrowhead
            val bh = 4.5f
            drawLine(iconColor, Offset(center.x + d, center.y + d), Offset(center.x + d - bh, center.y + d), stroke)
            drawLine(iconColor, Offset(center.x + d, center.y + d), Offset(center.x + d, center.y + d - bh), stroke)
        }
        IconType.PLAY -> {
            // (▶) Play triangle glyph
            val r = radius * 0.44f
            val playPath = Path().apply {
                moveTo(center.x - r * 0.55f, center.y - r * 0.85f)
                lineTo(center.x + r * 0.85f, center.y)
                lineTo(center.x - r * 0.55f, center.y + r * 0.85f)
                close()
            }
            drawPath(playPath, iconColor, style = Fill)
        }
        IconType.PAUSE -> {
            // (❚❚) Pause bars glyph
            val r = radius * 0.40f
            val barW = 3.5f
            val barH = r * 1.6f
            val gap = 3.5f
            drawRect(iconColor, Offset(center.x - gap - barW, center.y - barH / 2f), Size(barW, barH))
            drawRect(iconColor, Offset(center.x + gap, center.y - barH / 2f), Size(barW, barH))
        }
    }
}

private fun rotatePoint(px: Float, py: Float, cx: Float, cy: Float, angleDeg: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val dx = px - cx
    val dy = py - cy
    val rx = cx + (dx * cosA - dy * sinA)
    val ry = cy + (dx * sinA + dy * cosA)
    return Offset(rx, ry)
}

private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
