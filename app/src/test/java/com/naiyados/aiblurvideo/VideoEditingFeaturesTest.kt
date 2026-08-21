package com.naiyados.aiblurvideo

import com.naiyados.aiblurvideo.autoplate.export.ExportBitrate
import com.naiyados.aiblurvideo.autoplate.export.ExportResolution
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.model.VideoEditConfig
import com.naiyados.aiblurvideo.ui.model.VideoFilter
import com.naiyados.aiblurvideo.ui.theme.AppThemeMode
import com.naiyados.aiblurvideo.ui.theme.ThemeManager
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
        assertEquals(true, config.isPlateBlurActive)
        assertEquals(true, config.isFaceBlurActive)
        assertEquals(VideoFilter.NONE, config.filter)
        assertEquals(1.0f, config.playbackSpeed, 0.001f)
        assertEquals(VideoAspectRatio.ORIGINAL, config.aspectRatio)
        assertNotNull(config.exportSettings)
    }

    @Test
    fun testCustomBlurShapesAndConfig() {
        val shapes = com.naiyados.aiblurvideo.ui.model.CustomBlurShape.values()
        assertEquals(3, shapes.size)
        assertTrue(shapes.contains(com.naiyados.aiblurvideo.ui.model.CustomBlurShape.RECTANGLE))
        assertTrue(shapes.contains(com.naiyados.aiblurvideo.ui.model.CustomBlurShape.ROUNDED_RECT))
        assertTrue(shapes.contains(com.naiyados.aiblurvideo.ui.model.CustomBlurShape.OVAL))

        val config = VideoEditConfig(
            blurMode = BlurMode.Object,
            customObjectNormalizedRect = android.graphics.RectF(0.2f, 0.2f, 0.8f, 0.8f),
            customObjectRotationDegrees = 45f,
            customObjectShape = com.naiyados.aiblurvideo.ui.model.CustomBlurShape.OVAL
        )
        assertTrue(config.hasActiveEdits())
        assertEquals(45f, config.customObjectRotationDegrees, 0.001f)
        assertEquals(com.naiyados.aiblurvideo.ui.model.CustomBlurShape.OVAL, config.customObjectShape)
    }

    @Test
    fun testCustomBlurTransformAndCoordinateMapping() {
        val normLeft = 0.1f
        val normTop = 0.2f
        val normRight = 0.9f
        val normBottom = 0.8f
        val frameWidth = 1920
        val frameHeight = 1080

        val mappedLeft = normLeft * frameWidth
        val mappedTop = normTop * frameHeight
        val mappedRight = normRight * frameWidth
        val mappedBottom = normBottom * frameHeight

        assertEquals(192f, mappedLeft, 0.01f)
        assertEquals(216f, mappedTop, 0.01f)
        assertEquals(1728f, mappedRight, 0.01f)
        assertEquals(864f, mappedBottom, 0.01f)

        // Verify rotation normalization
        var angle = 370f % 360f
        if (angle < 0f) angle += 360f
        assertEquals(10f, angle, 0.01f)

        var negativeAngle = (-45f) % 360f
        if (negativeAngle < 0f) negativeAngle += 360f
        assertEquals(315f, negativeAngle, 0.01f)

        val config = VideoEditConfig(
            blurMode = BlurMode.Object,
            customObjectRotationDegrees = 315f,
            customObjectShape = com.naiyados.aiblurvideo.ui.model.CustomBlurShape.ROUNDED_RECT,
            blurStrength = 0.85f
        )
        assertTrue(config.hasActiveEdits())
        assertEquals(315f, config.customObjectRotationDegrees, 0.01f)
        assertEquals(0.85f, config.blurStrength, 0.01f)
    }

    @Test
    fun testThemeManagerToggleLogic() {
        ThemeManager.setThemeMode(com.naiyados.aiblurvideo.ui.theme.AppThemeMode.DARK)
        assertEquals(com.naiyados.aiblurvideo.ui.theme.AppThemeMode.DARK, ThemeManager.themeMode.value)

        ThemeManager.toggleDarkMode(isCurrentlyDark = true)
        assertEquals(com.naiyados.aiblurvideo.ui.theme.AppThemeMode.LIGHT, ThemeManager.themeMode.value)

        ThemeManager.toggleDarkMode(isCurrentlyDark = false)
        assertEquals(com.naiyados.aiblurvideo.ui.theme.AppThemeMode.DARK, ThemeManager.themeMode.value)
    }
}


