package com.naiyados.aiblurvideo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AiBlurColors {
    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.background

    val Panel: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.surface

    val SurfaceVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.surfaceVariant

    val CardBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.cardBackground

    val Border: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.border

    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.textPrimary

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.textSecondary

    val TextTertiary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.textTertiary

    val Pink = Color(0xFFFF5FA2)
    val Orange = Color(0xFFFF9E2C)
    val Purple = Color(0xFFC86BFF)
    val Blue = Color(0xFF7AA8FF)
    val Cyan = Color(0xFF00E5FF)
    val Green = Color(0xFF00E676)

    val AccentGradient = Brush.linearGradient(
        colors = listOf(
            Pink,
            Orange,
            Purple,
            Blue
        )
    )

    val DarkGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.backgroundGradient
}
