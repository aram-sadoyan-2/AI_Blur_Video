package com.naiyados.aiblurvideo.ui.model

import android.graphics.RectF
import com.naiyados.aiblurvideo.autoplate.DetectionTarget

/**
 * Represents a manual or AI-corrected blur/mask position at a specific video frame timestamp.
 */
data class KeyframeBlurBox(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeMs: Long,
    val rect: RectF,
    val rotationDegrees: Float = 0f,
    val shape: CustomBlurShape = CustomBlurShape.ROUNDED_RECT,
    val targetType: DetectionTarget = DetectionTarget.PLATE,
    val label: String = "Manual Box",
    val radiusRangeMs: Long = 333L // Default +-10 frames (~333ms at 30fps)
) {
    /**
     * Checks if this keyframe's fixed position applies to a given timestamp within its radius range.
     */
    fun coversTime(queryTimeMs: Long): Boolean {
        return kotlin.math.abs(queryTimeMs - timeMs) <= radiusRangeMs
    }
}

object KeyframeBoxHelper {

    private const val DEFAULT_FPS = 30
    private const val FRAME_DURATION_MS = 1000L / DEFAULT_FPS // ~33ms

    /**
     * Propagates a box position from the current frame timestamp by +- [frameOffset] frames.
     * E.g. frameOffset = 10 covers ~333ms before and ~333ms after (total ~21 frames).
     */
    fun createPropagatedKeyframes(
        centerTimeMs: Long,
        rect: RectF,
        rotationDegrees: Float = 0f,
        shape: CustomBlurShape = CustomBlurShape.ROUNDED_RECT,
        targetType: DetectionTarget = DetectionTarget.PLATE,
        frameOffset: Int = 10,
        fps: Int = 30
    ): List<KeyframeBlurBox> {
        val frameMs = 1000L / fps.coerceIn(15, 60)
        val radiusMs = frameOffset * frameMs
        val startMs = (centerTimeMs - radiusMs).coerceAtLeast(0L)
        val endMs = centerTimeMs + radiusMs

        val keyframes = mutableListOf<KeyframeBlurBox>()
        var curMs = startMs
        while (curMs <= endMs) {
            keyframes.add(
                KeyframeBlurBox(
                    timeMs = curMs,
                    rect = RectF(rect.left, rect.top, rect.right, rect.bottom),
                    rotationDegrees = rotationDegrees,
                    shape = shape,
                    targetType = targetType,
                    radiusRangeMs = frameMs / 2 + 10L
                )
            )
            curMs += frameMs
        }
        return keyframes
    }

    /**
     * Merges newly propagated keyframes into an existing list, replacing any overlapping timestamps.
     */
    fun mergeKeyframes(
        existing: List<KeyframeBlurBox>,
        newKeyframes: List<KeyframeBlurBox>
    ): List<KeyframeBlurBox> {
        val newTimes = newKeyframes.map { it.timeMs }.toSet()
        val filteredExisting = existing.filterNot { item ->
            newTimes.any { newT -> kotlin.math.abs(item.timeMs - newT) <= 25L }
        }
        return (filteredExisting + newKeyframes).sortedBy { it.timeMs }
    }

    /**
     * Resolves the interpolated or closest active box at a given timestamp [timeMs].
     */
    fun getBoxAtTime(
        keyframes: List<KeyframeBlurBox>,
        timeMs: Long,
        fallbackRect: RectF? = null
    ): KeyframeBlurBox? {
        if (keyframes.isEmpty()) {
            return fallbackRect?.let {
                KeyframeBlurBox(
                    timeMs = timeMs,
                    rect = it
                )
            }
        }

        // 1. Direct or radius match
        val directMatch = keyframes.find { it.coversTime(timeMs) }
        if (directMatch != null) {
            return directMatch
        }

        // 2. Linear interpolation between bounding keyframes
        val previous = keyframes.filter { it.timeMs <= timeMs }.maxByOrNull { it.timeMs }
        val next = keyframes.filter { it.timeMs >= timeMs }.minByOrNull { it.timeMs }

        return when {
            previous != null && next != null && previous.id != next.id -> {
                val span = (next.timeMs - previous.timeMs).toFloat()
                if (span > 0f && span <= 2500L) {
                    val progress = ((timeMs - previous.timeMs) / span).coerceIn(0f, 1f)
                    val interpLeft = previous.rect.left + (next.rect.left - previous.rect.left) * progress
                    val interpTop = previous.rect.top + (next.rect.top - previous.rect.top) * progress
                    val interpRight = previous.rect.right + (next.rect.right - previous.rect.right) * progress
                    val interpBottom = previous.rect.bottom + (next.rect.bottom - previous.rect.bottom) * progress
                    val interpRot = previous.rotationDegrees + (next.rotationDegrees - previous.rotationDegrees) * progress

                    KeyframeBlurBox(
                        timeMs = timeMs,
                        rect = RectF(interpLeft, interpTop, interpRight, interpBottom),
                        rotationDegrees = interpRot,
                        shape = previous.shape,
                        targetType = previous.targetType
                    )
                } else {
                    if (timeMs - previous.timeMs < next.timeMs - timeMs) previous else next
                }
            }
            previous != null -> previous
            next != null -> next
            else -> fallbackRect?.let { KeyframeBlurBox(timeMs = timeMs, rect = it) }
        }
    }
}
