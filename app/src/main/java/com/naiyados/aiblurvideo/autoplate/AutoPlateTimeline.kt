package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AutoPlateTimeline(
    boxes: List<AutoPlateBox>
) {
    private val stableTrack: List<AutoPlateBox> = buildStableTrack(boxes)

    fun boxesAt(
        currentTimeMs: Long,
        toleranceMs: Long = 900L
    ): List<AutoPlateBox> {
        if (stableTrack.isEmpty()) return emptyList()

        val before = stableTrack
            .filter { it.timeMs <= currentTimeMs }
            .minByOrNull { abs(currentTimeMs - it.timeMs) }

        val after = stableTrack
            .filter { it.timeMs >= currentTimeMs }
            .minByOrNull { abs(currentTimeMs - it.timeMs) }

        if (before == null && after == null) return emptyList()

        if (before == null) {
            return listOf(after!!).filter {
                abs(it.timeMs - currentTimeMs) <= toleranceMs
            }
        }

        if (after == null) {
            return listOf(before).filter {
                abs(it.timeMs - currentTimeMs) <= toleranceMs
            }
        }

        if (
            abs(currentTimeMs - before.timeMs) > toleranceMs &&
            abs(currentTimeMs - after.timeMs) > toleranceMs
        ) {
            return emptyList()
        }

        if (abs(before.timeMs - after.timeMs) <= 40L) {
            return listOf(before)
        }

        val progress = ((currentTimeMs - before.timeMs).toFloat() /
                (after.timeMs - before.timeMs).toFloat()
                ).coerceIn(0f, 1f)

        return listOf(
            before.copy(
                timeMs = currentTimeMs,
                rect = interpolateRect(
                    start = before.rect,
                    end = after.rect,
                    progress = smoothStep(progress)
                )
            )
        )
    }

    private fun buildStableTrack(
        boxes: List<AutoPlateBox>
    ): List<AutoPlateBox> {
        if (boxes.isEmpty()) return emptyList()

        val sorted = boxes.sortedBy { it.timeMs }

        val tracks = mutableListOf<MutableList<AutoPlateBox>>()

        sorted.forEach { box ->
            val bestTrack = tracks
                .filter { track ->
                    val last = track.lastOrNull() ?: return@filter false
                    val timeDiff = box.timeMs - last.timeMs

                    timeDiff in 0L..1200L && isNear(last.rect, box.rect)
                }
                .minByOrNull { track ->
                    centerDistance(track.last().rect, box.rect)
                }

            if (bestTrack != null) {
                val last = bestTrack.last()

                val smoothedBox = box.copy(
                    rect = smoothRect(
                        old = last.rect,
                        new = box.rect,
                        alpha = 0.18f
                    )
                )

                bestTrack += smoothedBox
            } else {
                tracks += mutableListOf(box)
            }
        }

        val best = tracks
            .maxWithOrNull(
                compareBy<MutableList<AutoPlateBox>> { it.size }
                    .thenBy { averageArea(it) }
            )
            ?: return emptyList()

        return best
    }

    private fun isNear(
        old: RectF,
        new: RectF
    ): Boolean {
        val dx = abs(old.centerX() - new.centerX())
        val dy = abs(old.centerY() - new.centerY())

        val averageWidth = (old.width() + new.width()) / 2f
        val averageHeight = (old.height() + new.height()) / 2f

        return dx <= averageWidth * 1.4f &&
                dy <= averageHeight * 2.4f
    }

    private fun centerDistance(
        first: RectF,
        second: RectF
    ): Float {
        val dx = first.centerX() - second.centerX()
        val dy = first.centerY() - second.centerY()
        return dx * dx + dy * dy
    }

    private fun averageArea(
        boxes: List<AutoPlateBox>
    ): Float {
        if (boxes.isEmpty()) return 0f

        return boxes.map {
            it.rect.width() * it.rect.height()
        }.average().toFloat()
    }

    private fun smoothRect(
        old: RectF,
        new: RectF,
        alpha: Float
    ): RectF {
        return RectF(
            lerp(old.left, new.left, alpha),
            lerp(old.top, new.top, alpha),
            lerp(old.right, new.right, alpha),
            lerp(old.bottom, new.bottom, alpha)
        )
    }

    private fun interpolateRect(
        start: RectF,
        end: RectF,
        progress: Float
    ): RectF {
        return RectF(
            lerp(start.left, end.left, progress),
            lerp(start.top, end.top, progress),
            lerp(start.right, end.right, progress),
            lerp(start.bottom, end.bottom, progress)
        )
    }

    private fun lerp(
        start: Float,
        end: Float,
        progress: Float
    ): Float {
        return start + (end - start) * progress
    }

    private fun smoothStep(value: Float): Float {
        return value * value * (3f - 2f * value)
    }
}