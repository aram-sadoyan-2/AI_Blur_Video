package com.naiyados.aiblurvideo.autoplate

import kotlin.math.abs

class AutoPlateTimeline(
    private val boxes: List<AutoPlateBox>
) {
    fun boxesAt(
        currentTimeMs: Long,
        toleranceMs: Long = 600L
    ): List<AutoPlateBox> {
        return boxes
            .filter { abs(it.timeMs - currentTimeMs) <= toleranceMs }
            .groupBy { it.text }
            .mapNotNull { (_, sameTextBoxes) ->
                sameTextBoxes.minByOrNull { abs(it.timeMs - currentTimeMs) }
            }
    }
}