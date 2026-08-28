package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import com.naiyados.aiblurvideo.ui.theme.AppThemeMode
import com.naiyados.aiblurvideo.ui.theme.LocalAppColors
import com.naiyados.aiblurvideo.ui.theme.ThemeManager

@Composable
fun ThemeSelectionDialog(
    onDismissRequest: () -> Unit
) {
    val currentMode by ThemeManager.themeMode.collectAsState()
    val appColors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("theme_selection_dialog"),
            color = appColors.surface,
            border = BorderStroke(1.dp, appColors.border),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(appColors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentMode) {
                                    AppThemeMode.DARK -> Icons.Rounded.DarkMode
                                    AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                                    AppThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                },
                                contentDescription = null,
                                tint = appColors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "App Theme",
                                color = appColors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Dark Mode saves battery during export",
                                color = appColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = appColors.textSecondary
                        )
                    }
                }

                // Theme Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        val icon = when (mode) {
                            AppThemeMode.DARK -> Icons.Rounded.DarkMode
                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                            AppThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                        }

                        Surface(
                            onClick = {
                                ThemeManager.setThemeMode(mode)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) appColors.primary.copy(alpha = if (appColors.isDark) 0.16f else 0.10f) else appColors.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) appColors.primary else appColors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) appColors.primary.copy(alpha = 0.2f)
                                            else appColors.surfaceElevated
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) appColors.primary else appColors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        color = if (isSelected) appColors.textPrimary else appColors.textPrimary.copy(alpha = 0.85f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        color = appColors.textSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(appColors.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Battery Saver Note
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AiBlurColors.Green.copy(alpha = if (appColors.isDark) 0.12f else 0.08f),
                    border = BorderStroke(1.dp, AiBlurColors.Green.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatterySaver,
                            contentDescription = null,
                            tint = AiBlurColors.Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Dark Mode uses true OLED blacks to cut display power draw by up to 40% while rendering & exporting videos.",
                            color = appColors.textPrimary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Done Button
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.primary
                    )
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
