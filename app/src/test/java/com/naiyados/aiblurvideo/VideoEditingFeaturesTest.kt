package com.naiyados.aiblurvideo

import com.naiyados.aiblurvideo.autoplate.export.ExportBitrate
import com.naiyados.aiblurvideo.autoplate.export.ExportResolution
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.model.VideoEditConfig
import com.naiyados.aiblurvideo.ui.model.VideoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoEditingFeaturesTest {

    @Test
    fun testBlurModesAvailability() {
        val modes = BlurMode.values()
        assertTrue(modes.contains(BlurMode.AutoPlate))
        assertTrue(modes.contains(BlurMode.FullBlur))
        assertTrue(modes.contains(BlurMode.Face))
        assertTrue(modes.contains(BlurMode.Object))
        assertTrue(modes.contains(BlurMode.Background))
        assertTrue(modes.contains(BlurMode.Pixelate))
        assertTrue(modes.contains(BlurMode.Effects))
        assertTrue(modes.contains(BlurMode.Speed))
        assertTrue(modes.contains(BlurMode.Crop))
        assertEquals(9, modes.size)
    }

    @Test
    fun testVideoFiltersCountAndDefaults() {
        val filters = VideoFilter.values()
        assertTrue(filters.contains(VideoFilter.NONE))
        assertTrue(filters.contains(VideoFilter.CINEMATIC))
        assertTrue(filters.contains(VideoFilter.CYBERPUNK))
        assertTrue(filters.contains(VideoFilter.VINTAGE))
        assertTrue(filters.contains(VideoFilter.NOIR))
        assertTrue(filters.contains(VideoFilter.COOL_ICE))
        assertTrue(filters.contains(VideoFilter.NIGHT_VISION))
        assertTrue(filters.contains(VideoFilter.SEPIA))
        assertEquals(8, filters.size)
    }

    @Test
    fun testAspectRatios() {
        assertEquals(null, VideoAspectRatio.ORIGINAL.ratioValue)
        assertEquals(16f / 9f, VideoAspectRatio.LANDSCAPE_16_9.ratioValue)
        assertEquals(9f / 16f, VideoAspectRatio.PORTRAIT_9_16.ratioValue)
        assertEquals(1f, VideoAspectRatio.SQUARE_1_1.ratioValue)
        assertEquals(4f / 5f, VideoAspectRatio.FEED_4_5.ratioValue)
    }

    @Test
    fun testExportSettingsDimensionCalculations() {
        val originalSettings = ExportSettings(
            resolution = ExportResolution.ORIGINAL,
            bitrate = ExportBitrate.STANDARD
        )
        val (origW, origH) = originalSettings.calculateOutputDimensions(1920, 1080)
        assertEquals(1920, origW)
        assertEquals(1080, origH)

        val hd720Settings = ExportSettings(
            resolution = ExportResolution.HD_720P
        )
        val (hdW, hdH) = hd720Settings.calculateOutputDimensions(1920, 1080)
        assertEquals(1280, hdW)
        assertEquals(720, hdH)

        // Ensure dimensions are even (divisible by 2) for H.264 video encoding
        val oddSettings = ExportSettings(resolution = ExportResolution.HD_720P)
        val (w, h) = oddSettings.calculateOutputDimensions(1921, 1079)
        assertEquals(0, w % 2)
        assertEquals(0, h % 2)
    }

    @Test
    fun testExportSettingsBitrateEstimations() {
        val highSettings = ExportSettings(bitrate = ExportBitrate.HIGH)
        val lowSettings = ExportSettings(bitrate = ExportBitrate.LOW)
        assertTrue(highSettings.estimateMegabytesPerMinute() > lowSettings.estimateMegabytesPerMinute())
    }

    @Test
    fun testVideoEditConfigDefaults() {
        val config = VideoEditConfig()
        assertEquals(BlurMode.AutoPlate, config.blurMode)
        assertEquals(0.65f, config.blurStrength, 0.001f)
        assertEquals(VideoFilter.NONE, config.filter)
        assertEquals(1.0f, config.playbackSpeed, 0.001f)
        assertEquals(VideoAspectRatio.ORIGINAL, config.aspectRatio)
        assertNotNull(config.exportSettings)
    }
}


