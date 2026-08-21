package com.naiyados.aiblurvideo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkM3ColorScheme = darkColorScheme(
    primary = Color(0xFFFF5FA2),
    secondary = Color(0xFFC86BFF),
    tertiary = Color(0xFFFF9E2C),
    background = Color(0xFF080A12),
    surface = Color(0xFF141824),
    surfaceVariant = Color(0xFF1B1F2E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color.White.copy(alpha = 0.15f)
)

private val LightM3ColorScheme = lightColorScheme(
    primary = Color(0xFFE02E7A),
    secondary = Color(0xFF9932CC),
    tertiary = Color(0xFFFF9E2C),
    background = Color(0xFFF3F5FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EDF5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF101426),
    onSurface = Color(0xFF101426),
    outline = Color(0xFFDCE2EC)
)

@Composable
fun AIBlurVideoTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDark
    }

    val appColors = if (isDark) DarkAppColors else LightAppColors
    val colorScheme = if (isDark) DarkM3ColorScheme else LightM3ColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
