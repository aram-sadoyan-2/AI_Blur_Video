package com.naiyados.aiblurvideo.autoplate

import android.graphics.RectF
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.detection.PlateDetectorPipeline
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

        val plateTrack = if (isDetectorPlaceholderTrack(boxes)) {
            rawTrack.ifEmpty { boxes.sortedBy { it.timeMs } }
        } else {
            buildPlateTrackForPlayback(boxes, rawTrack)
        }
        val spread = computePositionSpread(plateTrack)
        val textConsistency = PlateScoring.textConsistencyRatio(plateTrack)

        // Constant mask only when the plate stays in one place (parked / static camera).
        // If the plate moves in the frame, follow it with smoothed keyframes.
        val useConstantMask = plateTrack.size >= 2 && !spread.movesSignificantly

        maskMode = when {
            plateTrack.isEmpty() -> MaskMode.None
            useConstantMask -> MaskMode.Constant
            plateTrack.size >= 2 -> MaskMode.Keyframes
            else -> MaskMode.None
        }

        constantMask = if (maskMode == MaskMode.Constant) {
            buildConstantMask(plateTrack, videoDurationMs)
        } else {
            null
        }

        keyframeTrack = if (maskMode == MaskMode.Keyframes) {
            smoothTrackForPlayback(
                densifyKeyframeTrack(
                    medianSmoothTrack(
                        removeBigJumps(
                            filterVerticalOutliers(plateTrack)
                        ),
                        windowRadius = 2
                    ),
                    stepMs = KEYFRAME_STEP_MS,
                    endTimeMs = videoDurationMs
                ),
                alpha = 0.32f
            )
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
            "Timeline mode=$maskMode confidence=$confidence dominant=$dominantText " +
                "trackSize=${plateTrack.size} textConsistency=$textConsistency spread=$spread"
        )
    }

    private fun isDetectorPlaceholderTrack(boxes: List<AutoPlateBox>): Boolean {
        if (boxes.isEmpty()) return false
        val placeholderCount = boxes.count {
            PlateScoring.normalizeSimilarPlateText(it.text) ==
                PlateDetectorPipeline.PLATE_PLACEHOLDER
        }
        return placeholderCount.toFloat() / boxes.size >= 0.65f
    }

    private fun buildPlateTrackForPlayback(
        allBoxes: List<AutoPlateBox>,
        spatialTrack: List<AutoPlateBox>
    ): List<AutoPlateBox> {
        val dominant = PlateScoring.dominantText(spatialTrack)
            ?: PlateScoring.dominantText(allBoxes)

        if (dominant == PlateDetectorPipeline.PLATE_PLACEHOLDER) {
            return spatialTrack.sortedBy { it.timeMs }
        }

        val byTime = if (dominant != null) {
            allBoxes.filter {
                PlateScoring.normalizeSimilarPlateText(it.text) == dominant
            }
        } else {
            spatialTrack
        }.sortedBy { it.timeMs }

        return byTime.ifEmpty { spatialTrack.sortedBy { it.timeMs } }
    }

    /** Drops OCR reads on the hood/grille (wrong vertical band). */
    private fun filterVerticalOutliers(track: List<AutoPlateBox>): List<AutoPlateBox> {
        if (track.size < 4) return track

        val medianY = medianFloat(track.map { it.rect.centerY() })
        val medianHeight = medianFloat(track.map { it.rect.height() }).coerceAtLeast(1f)

        return track.filter { box ->
            abs(box.rect.centerY() - medianY) <= medianHeight * 2.2f
        }
    }

    private fun medianSmoothTrack(
        track: List<AutoPlateBox>,
        windowRadius: Int = 1
    ): List<AutoPlateBox> {
        if (track.size < 3) {
            return track.map { it.copy(rect = RectF(it.rect)) }
        }

        return track.mapIndexed { index, box ->
            val from = (index - windowRadius).coerceAtLeast(0)
            val to = (index + windowRadius).coerceAtMost(track.lastIndex)
            val window = track.subList(from, to + 1)
            box.copy(rect = medianRect(window))
        }
    }

    /** Fill gaps so playback never jumps across multi-second holes. */
    private fun densifyKeyframeTrack(
        track: List<AutoPlateBox>,
        stepMs: Long,
        endTimeMs: Long = track.lastOrNull()?.timeMs ?: 0L
    ): List<AutoPlateBox> {
        if (track.size < 2 || stepMs <= 0L) return track

        val template = track.first()
        val result = mutableListOf<AutoPlateBox>()
        var timeMs = track.first().timeMs
        val lastTimeMs = maxOf(track.last().timeMs, endTimeMs)

        while (timeMs <= lastTimeMs) {
            result += template.copy(
                timeMs = timeMs,
                rect = interpolateRectAt(track, timeMs)
            )
            timeMs += stepMs
        }

        return result
    }

    private fun interpolateRectAt(track: List<AutoPlateBox>, timeMs: Long): RectF {
        val first = track.first()
        val last = track.last()

        if (timeMs <= first.timeMs) return RectF(first.rect)
        if (timeMs >= last.timeMs) return RectF(last.rect)

        val before = track.filter { it.timeMs <= timeMs }.maxByOrNull { it.timeMs } ?: first
        val after = track.filter { it.timeMs >= timeMs }.minByOrNull { it.timeMs } ?: last

        if (before.timeMs == after.timeMs) return RectF(before.rect)

        val gapMs = (after.timeMs - before.timeMs).coerceAtLeast(1L)
        val progress = ((timeMs - before.timeMs).toFloat() / gapMs.toFloat()).coerceIn(0f, 1f)
        return interpolateRect(before.rect, after.rect, smoothStep(progress))
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
        val rect = if (gapMs <= 40L) {
            before.rect
        } else {
            val progress = ((currentTimeMs - before.timeMs).toFloat() / gapMs.toFloat())
                .coerceIn(0f, 1f)
            interpolateRect(before.rect, after.rect, smoothStep(progress))
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

        val template = pickAnchorBox(core)
        val rect = RectF(template.rect)

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
                        rect = smoothRect(last.rect, box.rect, 0.14f)
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

    private data class PositionSpread(
        val movesSignificantly: Boolean,
        val deltaX: Float = 0f,
        val deltaY: Float = 0f,
        val refWidth: Float = 0f
    )

    private fun computePositionSpread(track: List<AutoPlateBox>): PositionSpread {
        if (track.size < 2) {
            return PositionSpread(movesSignificantly = false)
        }

        val refWidth = medianFloat(track.map { it.rect.width() }).coerceAtLeast(1f)
        val deltaX = track.maxOf { it.rect.centerX() } - track.minOf { it.rect.centerX() }
        val deltaY = track.maxOf { it.rect.centerY() } - track.minOf { it.rect.centerY() }

        val movesSignificantly =
            deltaX > refWidth * 0.38f || deltaY > refWidth * 0.28f

        return PositionSpread(
            movesSignificantly = movesSignificantly,
            deltaX = deltaX,
            deltaY = deltaY,
            refWidth = refWidth
        )
    }

    private fun smoothTrackForPlayback(
        track: List<AutoPlateBox>,
        alpha: Float = 0.28f
    ): List<AutoPlateBox> {
        if (track.isEmpty()) return track

        var smoothed = RectF(track.first().rect)
        return track.map { box ->
            smoothed = smoothRect(smoothed, box.rect, alpha)
            box.copy(rect = RectF(smoothed))
        }
    }

    /** Use the earliest strong detection — matches the good first-frame preview. */
    private fun pickAnchorBox(boxes: List<AutoPlateBox>): AutoPlateBox {
        if (boxes.size == 1) return boxes.first()

        val topScore = boxes.maxOf { PlateScoring.score(it) }
        val threshold = topScore * 0.88f
        val strong = boxes.filter { PlateScoring.score(it) >= threshold }
        return strong.minByOrNull { it.timeMs } ?: boxes.minBy { it.timeMs }
    }

    private fun confidenceWeightedRect(boxes: List<AutoPlateBox>): RectF {
        if (boxes.isEmpty()) return RectF()
        if (boxes.size == 1) return RectF(boxes.first().rect)

        var totalWeight = 0f
        var left = 0f
        var top = 0f
        var right = 0f
        var bottom = 0f

        boxes.forEach { box ->
            val weight = box.confidence.coerceAtLeast(0.05f)
            totalWeight += weight
            left += box.rect.left * weight
            top += box.rect.top * weight
            right += box.rect.right * weight
            bottom += box.rect.bottom * weight
        }

        return RectF(
            left / totalWeight,
            top / totalWeight,
            right / totalWeight,
            bottom / totalWeight
        )
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

    companion object {
        private const val KEYFRAME_STEP_MS = 100L
    }
}
