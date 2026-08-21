package com.naiyados.aiblurvideo

import com.naiyados.aiblurvideo.autoplate.PlateTrackConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AutoPlateDebugTest {

    @Test
    fun testConfidenceEnum() {
        assertEquals("Low", PlateTrackConfidence.Low.name)
        assertEquals("Medium", PlateTrackConfidence.Medium.name)
        assertEquals("High", PlateTrackConfidence.High.name)
    }

    @Test
    fun testDebugFormatting() {
        val confidence = 0.92f
        val confidenceText = "${(confidence * 100).toInt()}%"
        assertEquals("92%", confidenceText)

        val plateText = "ABC 123"
        val formatted = "$plateText • $confidenceText"
        assertEquals("ABC 123 • 92%", formatted)
        assertNotNull(formatted)
    }

    @Test
    fun testPlateInferenceStatsDefaultsAndCustom() {
        val defaultStats = com.naiyados.aiblurvideo.autoplate.PlateInferenceStats()
        assertEquals(0, defaultStats.detectedCount)
        assertEquals(0L, defaultStats.latencyMs)
        assertEquals(false, defaultStats.isLiveTracking)

        val customStats = com.naiyados.aiblurvideo.autoplate.PlateInferenceStats(
            detectedCount = 2,
            latencyMs = 18L,
            isLiveTracking = true,
            confidence = 0.95f,
            dominantText = "CAL 992",
            detectorEngine = "TFLite SSD Tracker"
        )
        assertEquals(2, customStats.detectedCount)
        assertEquals(18L, customStats.latencyMs)
        assertEquals(true, customStats.isLiveTracking)
        assertEquals(0.95f, customStats.confidence, 0.001f)
        assertEquals("CAL 992", customStats.dominantText)
    }
}

