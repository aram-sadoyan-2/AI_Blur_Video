package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF
import android.util.Log
import kotlin.math.abs

class AutoPlateTimeline(
    boxes: List<AutoPlateBox>,
    videoDurationMs: Long
) {
    val confidence: PlateTrackConfidence
    val dominantText: String?

    private val maskMode: MaskMode
    private val constantMask: AutoPlateBox?
    private val keyframeTrack: List<AutoPlateBox>

    init {
        val rawTrack = buildStableTrack(boxes)
        dominantText = PlateScoring.dominantText(rawTrack)
        val stablePosition = isPositionStable(rawTrack)

        maskMode = when {
            rawTrack.size < 3 -> MaskMode.None
            stablePosition -> MaskMode.Constant
            rawTrack.size >= 4 -> MaskMode.Keyframes
            else -> MaskMode.None
        }

        constantMask = if (maskMode == MaskMode.Constant) {
            buildConstantMask(rawTrack, videoDurationMs)
        } else {
            null
        }

        keyframeTrack = if (maskMode == MaskMode.Keyframes) {
            rawTrack
        } else {
            emptyList()
        }

        confidence = when (maskMode) {
            MaskMode.Constant -> PlateTrackConfidence.High
            MaskMode.Keyframes -> PlateTrackConfidence.Medium
            MaskMode.None -> PlateTrackConfidence.Low
        }

        Log.d(
            "AutoPlate",
            "Timeline mode=$maskMode confidence=$confidence dominant=$dominantText trackSize=${rawTrack.size}"
        )
    }

    fun boxesAt(currentTimeMs: Long): List<AutoPlateBox> {
        constantMask?.let { mask ->
            if (currentTimeMs < 0L) return emptyList()
            return listOf(mask.copy(timeMs = currentTimeMs))
        }

        if (keyframeTrack.isEmpty()) return emptyList()

        val first = keyframeTrack.first()
        val last = keyframeTrack.last()

        if (currentTimeMs < first.timeMs) {
            return listOf(first.copy(timeMs = currentTimeMs, rect = RectF(first.rect)))
        }

        if (currentTimeMs > last.timeMs) {
            return listOf(last.copy(timeMs = currentTimeMs, rect = RectF(last.rect)))
        }

        val before = keyframeTrack
            .filter { it.timeMs <= currentTimeMs }
            .maxByOrNull { it.timeMs }
            ?: return emptyList()

        val after = keyframeTrack
            .filter { it.timeMs >= currentTimeMs }
            .minByOrNull { it.timeMs }
            ?: return listOf(before.copy(timeMs = currentTimeMs))

        if (before.timeMs == after.timeMs) {
            return listOf(before.copy(timeMs = currentTimeMs))
        }

        val gapMs = after.timeMs - before.timeMs
        val rect = when {
            gapMs <= 40L -> before.rect
            PlateScoring.isOutlierJump(before.rect, after.rect) -> {
                if (currentTimeMs - before.timeMs <= after.timeMs - currentTimeMs) {
                    before.rect
                } else {
                    after.rect
                }
            }
            else -> {
                val progress = ((currentTimeMs - before.timeMs).toFloat() / gapMs.toFloat())
                    .coerceIn(0f, 1f)
                interpolateRect(before.rect, after.rect, smoothStep(progress))
            }
        }

        return listOf(
            before.copy(
                timeMs = currentTimeMs,
                rect = rect
            )
        )
    }

    private enum class MaskMode {
        None,
        Constant,
        Keyframes
    }

    private fun buildConstantMask(
        track: List<AutoPlateBox>,
        videoDurationMs: Long
    ): AutoPlateBox {
        val dominant = PlateScoring.dominantText(track)
        val core = if (dominant != null) {
            track.filter {
                PlateScoring.normalizeSimilarPlateText(it.text) == dominant
            }
        } else {
            track
        }.ifEmpty { track }

        val template = core.maxByOrNull { PlateScoring.score(it) } ?: track.first()
        val rect = medianRect(core)

        return template.copy(
            timeMs = 0L,
            rect = rect,
            text = dominant ?: template.text,
            frameWidth = template.frameWidth,
            frameHeight = template.frameHeight
        ).also {
            Log.d(
                "AutoPlate",
                "Constant mask rect=$rect durationMs=$videoDurationMs text=${it.text}"
            )
        }
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
                if (PlateScoring.isOutlierJump(last.rect, box.rect)) {
                    tracks += mutableListOf(box)
                } else {
                    bestTrack += box.copy(
                        rect = smoothRect(last.rect, box.rect, 0.22f)
                    )
                }
            } else {
                tracks += mutableListOf(box)
            }
        }

        val best = tracks
            .filter { it.size >= 3 }
            .maxWithOrNull(
                compareBy<MutableList<AutoPlateBox>> { trackScore(it) }
                    .thenBy { it.size }
                    .thenBy { PlateScoring.textConsistencyBonus(it) }
            )
            ?: tracks.maxByOrNull { it.size }
            ?: emptyList()

        return removeBigJumps(best)
    }

    private fun isPositionStable(track: List<AutoPlateBox>): Boolean {
        if (track.size < 4) return false

        val dominant = PlateScoring.dominantText(track) ?: return false
        val core = track.filter {
            PlateScoring.normalizeSimilarPlateText(it.text) == dominant
        }
        if (core.size < 4) return false

        val refWidth = medianFloat(core.map { it.rect.width() }).coerceAtLeast(1f)
        val minX = core.minOf { it.rect.centerX() }
        val maxX = core.maxOf { it.rect.centerX() }
        val minY = core.minOf { it.rect.centerY() }
        val maxY = core.maxOf { it.rect.centerY() }

        return (maxX - minX) <= refWidth * 0.32f &&
            (maxY - minY) <= refWidth * 0.22f
    }

    private fun removeBigJumps(track: List<AutoPlateBox>): List<AutoPlateBox> {
        if (track.size <= 2) return track

        val result = mutableListOf(track.first())
        track.drop(1).forEach { box ->
            val last = result.last()
            if (!PlateScoring.isOutlierJump(last.rect, box.rect)) {
                result += box
            }
        }
        return result
    }

    private fun trackScore(track: List<AutoPlateBox>): Float {
        if (track.isEmpty()) return 0f
        val detectorScore = track.sumOf { it.confidence.toDouble() }.toFloat()
        val textScore = track.sumOf { PlateScoring.score(it).toDouble() }.toFloat()
        return detectorScore * 2f + textScore + PlateScoring.textConsistencyBonus(track)
    }

    private fun medianRect(boxes: List<AutoPlateBox>): RectF {
        return RectF(
            medianFloat(boxes.map { it.rect.left }),
            medianFloat(boxes.map { it.rect.top }),
            medianFloat(boxes.map { it.rect.right }),
            medianFloat(boxes.map { it.rect.bottom })
        )
    }

    private fun medianFloat(values: List<Float>): Float {
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    private fun isNear(old: RectF, new: RectF): Boolean {
        val dx = abs(old.centerX() - new.centerX())
        val dy = abs(old.centerY() - new.centerY())
        val averageWidth = (old.width() + new.width()) / 2f
        val averageHeight = (old.height() + new.height()) / 2f
        return dx <= averageWidth * 1.8f && dy <= averageHeight * 2.2f
    }

    private fun centerDistance(first: RectF, second: RectF): Float {
        val dx = first.centerX() - second.centerX()
        val dy = first.centerY() - second.centerY()
        return dx * dx + dy * dy
    }

    private fun smoothRect(old: RectF, new: RectF, alpha: Float): RectF {
        return RectF(
            lerp(old.left, new.left, alpha),
            lerp(old.top, new.top, alpha),
            lerp(old.right, new.right, alpha),
            lerp(old.bottom, new.bottom, alpha)
        )
    }

    private fun interpolateRect(start: RectF, end: RectF, progress: Float): RectF {
        return RectF(
            lerp(start.left, end.left, progress),
            lerp(start.top, end.top, progress),
            lerp(start.right, end.right, progress),
            lerp(start.bottom, end.bottom, progress)
        )
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float {
        return start + (end - start) * progress
    }

    private fun smoothStep(value: Float): Float {
        return value * value * (3f - 2f * value)
    }
}
