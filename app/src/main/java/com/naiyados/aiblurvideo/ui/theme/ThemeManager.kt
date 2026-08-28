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
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    surfaceElevated = Color(0xFF273549),
    cardBackground = Color(0xFF1E293B),
    border = Color(0xFF334155),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF0EA5E9),
    accentPink = Color(0xFF6366F1),
    accentPurple = Color(0xFF8B5CF6),
    accentOrange = Color(0xFFF59E0B),
    accentGreen = Color(0xFF10B981),
    accentBlue = Color(0xFF3B82F6),
    accentCyan = Color(0xFF0EA5E9),
    backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF0F172A))
    ),
    accentGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF6366F1)
        )
    )
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    surfaceElevated = Color(0xFFE2E8F0),
    cardBackground = Color(0xFFFFFFFF),
    border = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textTertiary = Color(0xFF94A3B8),
    primary = Color(0xFF4F46E5),
    secondary = Color(0xFF0284C7),
    accentPink = Color(0xFF4F46E5),
    accentPurple = Color(0xFF7C3AED),
    accentOrange = Color(0xFFD97706),
    accentGreen = Color(0xFF059669),
    accentBlue = Color(0xFF2563EB),
    accentCyan = Color(0xFF0284C7),
    backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF8FAFC), Color(0xFFF8FAFC))
    ),
    accentGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF4338CA)
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
