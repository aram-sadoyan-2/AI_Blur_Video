package com.naiyados.aiblurvideo.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String, val subtitle: String) {
    DARK("Dark Mode (OLED)", "Deep black for OLED battery saving & reduced eye strain"),
    LIGHT("Light Mode", "Crisp, clean high-contrast appearance"),
    SYSTEM("System Default", "Automatically match your device system settings")
}

data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val cardBackground: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val secondary: Color,
    val accentPink: Color,
    val accentPurple: Color,
    val accentOrange: Color,
    val accentGreen: Color,
    val accentBlue: Color,
    val accentCyan: Color,
    val backgroundGradient: Brush,
    val accentGradient: Brush
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF080A12),
    surface = Color(0xFF141824),
    surfaceVariant = Color(0xFF1B1F2E),
    surfaceElevated = Color(0xFF22283A),
    cardBackground = Color(0xFF181C2A),
    border = Color.White.copy(alpha = 0.12f),
    textPrimary = Color.White,
    textSecondary = Color.White.copy(alpha = 0.65f),
    textTertiary = Color.White.copy(alpha = 0.40f),
    primary = Color(0xFFFF5FA2),
    secondary = Color(0xFFC86BFF),
    accentPink = Color(0xFFFF5FA2),
    accentPurple = Color(0xFFC86BFF),
    accentOrange = Color(0xFFFF9E2C),
    accentGreen = Color(0xFF00E676),
    accentBlue = Color(0xFF7AA8FF),
    accentCyan = Color(0xFF00E5FF),
    backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF151827), Color(0xFF080A12))
    ),
    accentGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF5FA2),
            Color(0xFFFF9E2C),
            Color(0xFFC86BFF),
            Color(0xFF7AA8FF)
        )
    )
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF3F5FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EDF5),
    surfaceElevated = Color(0xFFDFE6F0),
    cardBackground = Color(0xFFFFFFFF),
    border = Color(0xFFDCE2EC),
    textPrimary = Color(0xFF101426),
    textSecondary = Color(0xFF556075),
    textTertiary = Color(0xFF8692A6),
    primary = Color(0xFFE02E7A),
    secondary = Color(0xFF9932CC),
    accentPink = Color(0xFFFF5FA2),
    accentPurple = Color(0xFFC86BFF),
    accentOrange = Color(0xFFFF9E2C),
    accentGreen = Color(0xFF00C853),
    accentBlue = Color(0xFF2979FF),
    accentCyan = Color(0xFF00B8D4),
    backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBF0F8))
    ),
    accentGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF5FA2),
            Color(0xFFFF9E2C),
            Color(0xFFC86BFF),
            Color(0xFF7AA8FF)
        )
    )
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object ThemeManager {
    private const val PREFS_NAME = "ai_blur_theme_prefs"
    private const val KEY_THEME_MODE = "key_app_theme_mode"

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedName = prefs?.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
            val mode = try {
                AppThemeMode.valueOf(savedName)
            } catch (e: Exception) {
                AppThemeMode.DARK
            }
            _themeMode.value = mode
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun toggleDarkMode(isCurrentlyDark: Boolean) {
        val nextMode = if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(nextMode)
    }
}
