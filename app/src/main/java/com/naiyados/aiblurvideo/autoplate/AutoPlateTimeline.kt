package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF
import android.util.Log
import kotlin.math.abs

class AutoPlateTimeline(
    boxes: List<AutoPlateBox>
) {
    private val stableTrack: List<AutoPlateBox> = buildStableTrack(boxes)

    init {
        Log.d(
            "AutoPlate",
            "Timeline stableTrack size=${stableTrack.size}, texts=${stableTrack.take(12).joinToString { it.text }}"
        )
    }

    fun boxesAt(
        currentTimeMs: Long,
        toleranceMs: Long = 900L
    ): List<AutoPlateBox> {
        if (stableTrack.isEmpty()) return emptyList()

        val first = stableTrack.first()
        val last = stableTrack.last()

        if (currentTimeMs < first.timeMs - toleranceMs) {
            return emptyList()
        }

        if (currentTimeMs > last.timeMs + toleranceMs) {
            return emptyList()
        }

        val before = stableTrack
            .filter { it.timeMs <= currentTimeMs }
            .maxByOrNull { it.timeMs }

        val after = stableTrack
            .filter { it.timeMs >= currentTimeMs }
            .minByOrNull { it.timeMs }

        if (before == null && after == null) return emptyList()

        if (before == null) {
            return listOf(after!!)
        }

        if (after == null) {
            return listOf(before)
        }

        val gapMs = after.timeMs - before.timeMs

        if (gapMs <= 40L) {
            return listOf(before)
        }

        if (gapMs > toleranceMs) {
            return listOf(before)
        }

        val progress = ((currentTimeMs - before.timeMs).toFloat() / gapMs.toFloat())
            .coerceIn(0f, 1f)

        val rect = interpolateRect(
            start = before.rect,
            end = after.rect,
            progress = smoothStep(progress)
        )

        return listOf(
            before.copy(
                timeMs = currentTimeMs,
                rect = rect
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

                    timeDiff in 0L..1400L && isNear(last.rect, box.rect)
                }
                .minByOrNull { track ->
                    centerDistance(track.last().rect, box.rect)
                }

            if (bestTrack != null) {
                val last = bestTrack.last()

                val smoothedRect = smoothRect(
                    old = last.rect,
                    new = box.rect,
                    alpha = 0.20f
                )

                bestTrack += box.copy(rect = smoothedRect)
            } else {
                tracks += mutableListOf(box)
            }
        }

        val best = tracks
            .filter { it.size >= 3 }
            .maxWithOrNull(
                compareBy<MutableList<AutoPlateBox>> { it.size }
                    .thenBy { PlateScoring.textConsistencyBonus(it) }
                    .thenBy { averageArea(it) }
                    .thenBy { trackScore(it) }
            )
            ?: tracks.maxByOrNull { it.size }
            ?: emptyList()

        Log.d(
            "AutoPlate",
            "Tracks=${tracks.size}, bestSize=${best.size}, bestTexts=${best.take(10).joinToString { it.text }}"
        )

        return removeBigJumps(best)
    }

    private fun removeBigJumps(
        track: List<AutoPlateBox>
    ): List<AutoPlateBox> {
        if (track.size <= 2) return track

        val result = mutableListOf<AutoPlateBox>()
        result += track.first()

        track.drop(1).forEach { box ->
            val last = result.last()

            if (!PlateScoring.isOutlierJump(last.rect, box.rect)) {
                result += box
            }
        }

        return result
    }

    private fun isNear(
        old: RectF,
        new: RectF
    ): Boolean {
        val dx = abs(old.centerX() - new.centerX())
        val dy = abs(old.centerY() - new.centerY())

        val averageWidth = (old.width() + new.width()) / 2f
        val averageHeight = (old.height() + new.height()) / 2f

        return dx <= averageWidth * 2.4f &&
                dy <= averageHeight * 3.0f
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
        track: List<AutoPlateBox>
    ): Float {
        if (track.isEmpty()) return 0f

        return track
            .map { it.rect.width() * it.rect.height() }
            .average()
            .toFloat()
    }

    private fun trackScore(
        track: List<AutoPlateBox>
    ): Float {
        if (track.isEmpty()) return 0f

        val avgScore = track
            .map { PlateScoring.score(it) }
            .average()
            .toFloat()

        return avgScore + PlateScoring.textConsistencyBonus(track)
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