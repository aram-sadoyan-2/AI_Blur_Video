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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import com.naiyados.aiblurvideo.ui.model.CustomBlurShape
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private enum class HandleType {
    NONE, BODY, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT, ROTATE
}

@Composable
fun CustomObjectTouchOverlay(
    normalizedRect: RectF?,
    rotationDegrees: Float = 0f,
    shape: CustomBlurShape = CustomBlurShape.ROUNDED_RECT,
    blurStrength: Float = 0.65f,
    onRectChanged: (RectF) -> Unit,
    onRotationChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentNormRect by remember(normalizedRect) {
        mutableStateOf(normalizedRect ?: RectF(0.25f, 0.30f, 0.75f, 0.70f))
    }

    var currentRotation by remember(rotationDegrees) {
        mutableFloatStateOf(rotationDegrees)
    }

    var activeHandle by remember {
        mutableStateOf(HandleType.NONE)
    }

    val minSizeNorm = 0.06f

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("custom_object_touch_overlay")
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

                    val handleHitRadius = 56f // Generous hit area for comfortable touch
                    val rotateKnobY = top - 52f

                    activeHandle = when {
                        // Rotation knob
                        distance(unrotatedTouch.x, unrotatedTouch.y, cx, rotateKnobY) <= handleHitRadius + 10f -> HandleType.ROTATE
                        // Corners
                        distance(unrotatedTouch.x, unrotatedTouch.y, left, top) <= handleHitRadius -> HandleType.TOP_LEFT
                        distance(unrotatedTouch.x, unrotatedTouch.y, right, top) <= handleHitRadius -> HandleType.TOP_RIGHT
                        distance(unrotatedTouch.x, unrotatedTouch.y, left, bottom) <= handleHitRadius -> HandleType.BOTTOM_LEFT
                        distance(unrotatedTouch.x, unrotatedTouch.y, right, bottom) <= handleHitRadius -> HandleType.BOTTOM_RIGHT
                        // Edges
                        distance(unrotatedTouch.x, unrotatedTouch.y, cx, top) <= handleHitRadius -> HandleType.EDGE_TOP
                        distance(unrotatedTouch.x, unrotatedTouch.y, cx, bottom) <= handleHitRadius -> HandleType.EDGE_BOTTOM
                        distance(unrotatedTouch.x, unrotatedTouch.y, left, cy) <= handleHitRadius -> HandleType.EDGE_LEFT
                        distance(unrotatedTouch.x, unrotatedTouch.y, right, cy) <= handleHitRadius -> HandleType.EDGE_RIGHT
                        // Body
                        unrotatedTouch.x in (left - 16f)..(right + 16f) && unrotatedTouch.y in (top - 16f)..(bottom + 16f) -> HandleType.BODY
                        else -> {
                            // Tap outside repositions the box center to touch point
                            val normX = (firstDown.position.x / w).coerceIn(0.1f, 0.9f)
                            val normY = (firstDown.position.y / h).coerceIn(0.1f, 0.9f)
                            val bw = currentNormRect.width()
                            val bh = currentNormRect.height()
                            val nLeft = (normX - bw / 2f).coerceIn(0f, 1f - bw)
                            val nTop = (normY - bh / 2f).coerceIn(0f, 1f - bh)
                            val updated = RectF(nLeft, nTop, nLeft + bw, nTop + bh)
                            currentNormRect = updated
                            onRectChanged(updated)
                            HandleType.BODY
                        }
                    }

                    firstDown.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        if (activePointers.isEmpty()) {
                            activeHandle = HandleType.NONE
                            break
                        }

                        if (activePointers.size >= 2) {
                            // Multi-touch: Pinch to scale + two-finger rotation + pan
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            val pan = event.calculatePan()

                            activePointers.forEach { it.consume() }

                            var updatedCx = (currentNormRect.centerX() * w + pan.x).coerceIn(0f, w)
                            var updatedCy = (currentNormRect.centerY() * h + pan.y).coerceIn(0f, h)
                            var updatedW = (currentNormRect.width() * w * zoom).coerceIn(minSizeNorm * w, w * 0.95f)
                            var updatedH = (currentNormRect.height() * h * zoom).coerceIn(minSizeNorm * h, h * 0.95f)

                            val nLeft = ((updatedCx - updatedW / 2f) / w).coerceIn(0f, 1f - updatedW / w)
                            val nTop = ((updatedCy - updatedH / 2f) / h).coerceIn(0f, 1f - updatedH / h)
                            val nRight = nLeft + updatedW / w
                            val nBottom = nTop + updatedH / h

                            val updatedRect = RectF(nLeft, nTop, nRight, nBottom)
                            currentNormRect = updatedRect
                            onRectChanged(updatedRect)

                            if (rotation != 0f) {
                                var newAngle = (currentRotation + rotation) % 360f
                                if (newAngle < 0f) newAngle += 360f
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
                                curH = currentNormRect.height()

                                if (activeHandle == HandleType.ROTATE) {
                                    val currentTouch = pointer.position
                                    val angleRad = atan2((currentTouch.y - cy).toDouble(), (currentTouch.x - cx).toDouble())
                                    var deg = (Math.toDegrees(angleRad).toFloat() + 90f) % 360f
                                    if (deg < 0f) deg += 360f
                                    currentRotation = deg
                                    onRotationChanged(deg)
                                } else if (activeHandle == HandleType.BODY) {
                                    val dxNorm = dragDelta.x / w
                                    val dyNorm = dragDelta.y / h
                                    val boxW = currentNormRect.width()
                                    val boxH = currentNormRect.height()

                                    val newLeft = (currentNormRect.left + dxNorm).coerceIn(0f, 1f - boxW)
                                    val newTop = (currentNormRect.top + dyNorm).coerceIn(0f, 1f - boxH)
                                    val updated = RectF(newLeft, newTop, newLeft + boxW, newTop + boxH)
                                    currentNormRect = updated
                                    onRectChanged(updated)
                                } else {
                                    // Handles: Corner / Edge resizing with exact rotation transformation
                                    val rad = Math.toRadians(currentRotation.toDouble())
                                    val cosA = cos(rad).toFloat()
                                    val sinA = sin(rad).toFloat()

                                    // Local basis vectors
                                    // u = local X axis (cosA, sinA)
                                    // v = local Y axis (-sinA, cosA)
                                    val localDx = dragDelta.x * cosA + dragDelta.y * sinA
                                    val localDy = -dragDelta.x * sinA + dragDelta.y * cosA

                                    var deltaW = 0f
                                    var deltaH = 0f
                                    var shiftLocalX = 0f
                                    var shiftLocalY = 0f

                                    when (activeHandle) {
                                        HandleType.TOP_LEFT -> {
                                            deltaW = -localDx
                                            deltaH = -localDy
                                            shiftLocalX = localDx / 2f
                                            shiftLocalY = localDy / 2f
                                        }
                                        HandleType.TOP_RIGHT -> {
                                            deltaW = localDx
                                            deltaH = -localDy
                                            shiftLocalX = localDx / 2f
                                            shiftLocalY = localDy / 2f
                                        }
                                        HandleType.BOTTOM_LEFT -> {
                                            deltaW = -localDx
                                            deltaH = localDy
                                            shiftLocalX = localDx / 2f
                                            shiftLocalY = localDy / 2f
                                        }
                                        HandleType.BOTTOM_RIGHT -> {
                                            deltaW = localDx
                                            deltaH = localDy
                                            shiftLocalX = localDx / 2f
                                            shiftLocalY = localDy / 2f
                                        }
                                        HandleType.EDGE_TOP -> {
                                            deltaH = -localDy
                                            shiftLocalY = localDy / 2f
                                        }
                                        HandleType.EDGE_BOTTOM -> {
                                            deltaH = localDy
                                            shiftLocalY = localDy / 2f
                                        }
                                        HandleType.EDGE_LEFT -> {
                                            deltaW = -localDx
                                            shiftLocalX = localDx / 2f
                                        }
                                        HandleType.EDGE_RIGHT -> {
                                            deltaW = localDx
                                            shiftLocalX = localDx / 2f
                                        }
                                        else -> {}
                                    }

                                    val newPixelW = (curW + deltaW).coerceIn(minSizeNorm * w, w * 0.95f)
                                    val newPixelH = (curH + deltaH).coerceIn(minSizeNorm * h, h * 0.95f)

                                    // Rotate shift back to screen coordinates
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
                                    onRectChanged(updated)
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

            val primaryNeon = Color(0xFF6366F1) // Clean Indigo primary accent
            val cyanAccent = Color(0xFF3B82F6)  // Clean Blue accent
            val handleFill = Color(0xFFFFFFFF)  // Clean White handle fill
            val activeHighlight = Color(0xFF4F46E5) // Clean Deep Indigo active indicator

            rotate(degrees = currentRotation, pivot = Offset(cx, cy)) {
                // 1. Clean Privacy Simulation Core
                val blurAlpha = (0.35f + blurStrength * 0.40f).coerceIn(0.25f, 0.85f)
                val path = Path()
                when (shape) {
                    CustomBlurShape.RECTANGLE -> {
                        path.addRect(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
                    }
                    CustomBlurShape.ROUNDED_RECT -> {
                        val corner = min(boxW, boxH) * 0.20f
                        path.addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = left,
                                top = top,
                                right = right,
                                bottom = bottom,
                                cornerRadius = CornerRadius(corner, corner)
                            )
                        )
                    }
                    CustomBlurShape.OVAL -> {
                        path.addOval(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
                    }
                }

                // Semi-transparent dark blur preview mask
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = blurAlpha * 0.65f),
                    style = Fill
                )

                // 2. Clean Dashed Selection Boundary
                drawPath(
                    path = path,
                    color = primaryNeon,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                )

                // 3. Rotation Handle
                val stemStartY = top
                val stemEndY = top - 44f
                drawLine(
                    color = cyanAccent.copy(alpha = 0.85f),
                    start = Offset(cx, stemStartY),
                    end = Offset(cx, stemEndY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                // Rotation Knob Body
                val isRotateActive = activeHandle == HandleType.ROTATE
                drawCircle(
                    color = if (isRotateActive) activeHighlight.copy(alpha = 0.25f) else cyanAccent.copy(alpha = 0.15f),
                    radius = if (isRotateActive) 22f else 18f,
                    center = Offset(cx, stemEndY)
                )
                drawCircle(
                    color = handleFill,
                    radius = 14f,
                    center = Offset(cx, stemEndY)
                )
                drawCircle(
                    color = if (isRotateActive) activeHighlight else cyanAccent,
                    radius = 14f,
                    center = Offset(cx, stemEndY),
                    style = Stroke(width = 2.5f)
                )
                // Mini rotation arc
                drawArc(
                    color = if (isRotateActive) activeHighlight else cyanAccent,
                    startAngle = 40f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(cx - 6f, stemEndY - 6f),
                    size = Size(12f, 12f),
                    style = Stroke(width = 2f)
                )

                // 4. Corner Handles (TL, TR, BL, BR)
                drawResizeHandle(left, top, activeHandle == HandleType.TOP_LEFT, cyanAccent, handleFill, activeHighlight)
                drawResizeHandle(right, top, activeHandle == HandleType.TOP_RIGHT, cyanAccent, handleFill, activeHighlight)
                drawResizeHandle(left, bottom, activeHandle == HandleType.BOTTOM_LEFT, cyanAccent, handleFill, activeHighlight)
                drawResizeHandle(right, bottom, activeHandle == HandleType.BOTTOM_RIGHT, cyanAccent, handleFill, activeHighlight)

                // 5. Edge Pill Handles (Top, Bottom, Left, Right)
                drawEdgeHandle(cx, top, isHorizontal = true, activeHandle == HandleType.EDGE_TOP, cyanAccent, handleFill, activeHighlight)
                drawEdgeHandle(cx, bottom, isHorizontal = true, activeHandle == HandleType.EDGE_BOTTOM, cyanAccent, handleFill, activeHighlight)
                drawEdgeHandle(left, cy, isHorizontal = false, activeHandle == HandleType.EDGE_LEFT, cyanAccent, handleFill, activeHighlight)
                drawEdgeHandle(right, cy, isHorizontal = false, activeHandle == HandleType.EDGE_RIGHT, cyanAccent, handleFill, activeHighlight)

                // 6. Corner L-Brackets
                val bracketLen = min(20f, min(boxW, boxH) * 0.25f)
                val strokeW = 3.5f
                drawLine(cyanAccent, Offset(left, top), Offset(left + bracketLen, top), strokeW)
                drawLine(cyanAccent, Offset(left, top), Offset(left, top + bracketLen), strokeW)

                drawLine(cyanAccent, Offset(right, top), Offset(right - bracketLen, top), strokeW)
                drawLine(cyanAccent, Offset(right, top), Offset(right, top + bracketLen), strokeW)

                drawLine(cyanAccent, Offset(left, bottom), Offset(left + bracketLen, bottom), strokeW)
                drawLine(cyanAccent, Offset(left, bottom), Offset(left, bottom - bracketLen), strokeW)

                drawLine(cyanAccent, Offset(right, bottom), Offset(right - bracketLen, bottom), strokeW)
                drawLine(cyanAccent, Offset(right, bottom), Offset(right, bottom - bracketLen), strokeW)
            }
        }
    }
}

private fun DrawScope.drawResizeHandle(
    x: Float,
    y: Float,
    isActive: Boolean,
    borderColor: Color,
    fillColor: Color,
    activeColor: Color
) {
    val ringColor = if (isActive) activeColor else borderColor
    val radius = if (isActive) 13f else 11f

    // Outer glow halo
    drawCircle(
        color = ringColor.copy(alpha = if (isActive) 0.50f else 0.25f),
        radius = radius + 6f,
        center = Offset(x, y)
    )
    // Handle body
    drawCircle(
        color = fillColor,
        radius = radius,
        center = Offset(x, y)
    )
    // Handle ring
    drawCircle(
        color = ringColor,
        radius = radius,
        center = Offset(x, y),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawEdgeHandle(
    x: Float,
    y: Float,
    isHorizontal: Boolean,
    isActive: Boolean,
    borderColor: Color,
    fillColor: Color,
    activeColor: Color
) {
    val ringColor = if (isActive) activeColor else borderColor
    val pillW = if (isHorizontal) 28f else 10f
    val pillH = if (isHorizontal) 10f else 28f

    drawRoundRect(
        color = ringColor.copy(alpha = if (isActive) 0.40f else 0.20f),
        topLeft = Offset(x - (pillW + 4f) / 2f, y - (pillH + 4f) / 2f),
        size = Size(pillW + 4f, pillH + 4f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = fillColor,
        topLeft = Offset(x - pillW / 2f, y - pillH / 2f),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = ringColor,
        topLeft = Offset(x - pillW / 2f, y - pillH / 2f),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 2f)
    )
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
