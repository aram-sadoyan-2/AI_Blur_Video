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

    val Primary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.primary

    val ElectricCyan = Color(0xFF00E5FF)
    val ElectricBlue = Color(0xFF2979FF)
    val Cobalt = Color(0xFF0066FF)
    val Pink = Color(0xFF00E5FF) // Replaced with modern Electric Cyan
    val Orange = Color(0xFFFF9E2C)
    val Purple = Color(0xFF8B5CF6)
    val Blue = Color(0xFF2979FF)
    val Cyan = Color(0xFF00E5FF)
    val Green = Color(0xFF10B981)

    val AccentGradient = Brush.linearGradient(
        colors = listOf(
            ElectricCyan,
            ElectricBlue,
            Purple,
            ElectricCyan
        )
    )

    val DarkGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.backgroundGradient
}
