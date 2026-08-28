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

    val ElectricCyan = Color(0xFF0EA5E9)
    val ElectricBlue = Color(0xFF4F46E5)
    val Cobalt = Color(0xFF4F46E5)
    val Pink = Color(0xFF4F46E5)
    val Orange = Color(0xFFF59E0B)
    val Purple = Color(0xFF7C3AED)
    val Blue = Color(0xFF3B82F6)
    val Cyan = Color(0xFF0EA5E9)
    val Green = Color(0xFF10B981)

    val AccentGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF6366F1)
        )
    )

    val DarkGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.backgroundGradient
}
