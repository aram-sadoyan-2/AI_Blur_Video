package com.naiyados.aiblurvideo.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AiBlurColors {
    val Background = Color(0xFF080A12)
    val Panel = Color(0xFF141824)

    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.65f)

    val Pink = Color(0xFFFF5FA2)
    val Orange = Color(0xFFFF9E2C)
    val Purple = Color(0xFFC86BFF)
    val Blue = Color(0xFF7AA8FF)

    val AccentGradient = Brush.linearGradient(
        colors = listOf(
            Pink,
            Orange,
            Purple,
            Blue
        )
    )

    val DarkGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF151827),
            Color(0xFF080A12)
        )
    )
}