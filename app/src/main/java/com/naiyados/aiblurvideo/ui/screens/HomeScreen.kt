package com.naiyados.aiblurvideo.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.analytics.AppAnalytics
import com.naiyados.aiblurvideo.history.ProcessedVideoHistoryManager
import com.naiyados.aiblurvideo.queue.BatchQueueManager
import com.naiyados.aiblurvideo.queue.QueueItemStatus
import com.naiyados.aiblurvideo.ui.components.BatchVideoPreviewDialog
import com.naiyados.aiblurvideo.ui.components.GlassCard
import com.naiyados.aiblurvideo.ui.components.HistorySection
import com.naiyados.aiblurvideo.ui.components.PremiumBadge
import com.naiyados.aiblurvideo.ui.components.ThemeSelectionDialog
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import com.naiyados.aiblurvideo.ui.theme.AppThemeMode
import com.naiyados.aiblurvideo.ui.theme.LocalAppColors
import com.naiyados.aiblurvideo.ui.theme.ThemeManager

@Composable
fun HomeScreen(
    isPremium: Boolean,
    onImportVideoClick: () -> Unit,
    onOpenBatchQueue: () -> Unit = {},
    onOpenVideo: (Uri) -> Unit = {},
    onPremiumClick: () -> Unit,
    onOpenProjectsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var showThemeDialog by remember { mutableStateOf(false) }
    var previewVideoUri by remember { mutableStateOf<Uri?>(null) }
    var previewVideoTitle by remember { mutableStateOf("Exported Video") }

    LaunchedEffect(Unit) {
        ProcessedVideoHistoryManager.init(context)
    }

    val historyItems by ProcessedVideoHistoryManager.historyFlow.collectAsState()
    val queueItems by BatchQueueManager.queueItems.collectAsState()
    val isQueueProcessing by BatchQueueManager.isProcessing.collectAsState()

    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (previewVideoUri != null) {
        BatchVideoPreviewDialog(
            videoUri = previewVideoUri!!,
            videoTitle = previewVideoTitle,
            onDismissRequest = { previewVideoUri = null },
            onOpenInStudio = { uri ->
                previewVideoUri = null
                onOpenVideo(uri)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBlurColors.Background),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = topPadding + 10.dp,
            bottom = bottomPadding + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HomeHeader(
                isPremium = isPremium,
                onPremiumClick = onPremiumClick,
                onThemeClick = { showThemeDialog = true }
            )
        }

        item {
            ImportVideoBigButton(
                onClick = onImportVideoClick
            )
        }

        item {
            BatchQueueHeroBanner(
                queueCount = queueItems.size,
                isProcessing = isQueueProcessing,
                onClick = onOpenBatchQueue
            )
        }

        if (historyItems.isNotEmpty()) {
            item {
                HistorySection(
                    items = historyItems,
                    onOpenVideo = { uri ->
                        val record = historyItems.find { it.uriString == uri.toString() }
                        val res = record?.resolutionLabel ?: "Exported Video"
                        previewVideoTitle = if (record != null) "Exported • ${record.resolutionLabel}" else "Exported Video"
                        previewVideoUri = uri
                        AppAnalytics.trackHistoryVideoPlayed(res)
                    },
                    onDeleteVideo = { id ->
                        ProcessedVideoHistoryManager.removeVideo(context, id)
                        AppAnalytics.trackHistoryVideoDeleted()
                    }
                )
            }
        }

        item {
            FeatureSectionCompact()
        }
    }
}

@Composable
private fun BatchQueueHeroBanner(
    queueCount: Int,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (appColors.isDark) Color(0xFF161928) else appColors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isProcessing) appColors.primary else appColors.border
        ),
        shadowElevation = if (appColors.isDark) 0.dp else 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    appColors.primary.copy(alpha = 0.25f),
                                    appColors.secondary.copy(alpha = 0.25f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = appColors.primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = appColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Batch Queue Processing",
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        if (isProcessing) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(appColors.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Processing",
                                    color = if (appColors.isDark) Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (queueCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (appColors.isDark) Color.White.copy(alpha = 0.15f) else appColors.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$queueCount items",
                                    color = if (appColors.isDark) Color.White else appColors.textPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isProcessing) "Currently processing multiple videos sequentially"
                        else if (queueCount > 0) "$queueCount videos ready to be processed sequentially"
                        else "Queue & process multiple videos automatically",
                        color = appColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = "Open Queue",
                tint = appColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(
    isPremium: Boolean,
    onPremiumClick: () -> Unit,
    onThemeClick: () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val appColors = LocalAppColors.current

    val themeIcon = when (themeMode) {
        AppThemeMode.DARK -> Icons.Rounded.DarkMode
        AppThemeMode.LIGHT -> Icons.Rounded.LightMode
        AppThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "AI Blur Video",
                color = appColors.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )

            Text(
                text = "Smart blur editor for videos",
                color = appColors.textSecondary,
                fontSize = 12.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Toggle Button
            Surface(
                onClick = onThemeClick,
                shape = RoundedCornerShape(14.dp),
                color = if (appColors.isDark) Color.White.copy(alpha = 0.08f) else appColors.surfaceElevated,
                border = BorderStroke(1.dp, appColors.border),
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = "Change Theme",
                        tint = if (appColors.isDark) AiBlurColors.Purple else AiBlurColors.Orange,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = when (themeMode) {
                            AppThemeMode.DARK -> "Dark"
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.SYSTEM -> "Auto"
                        },
                        color = appColors.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            PremiumBadge(
                isPremium = isPremium,
                onClick = onPremiumClick
            )
        }
    }
}

@Composable
private fun ImportVideoBigButton(
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("choose_video_button"),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AiBlurColors.AccentGradient),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = Color.White
                )

                Text(
                    text = "Choose Video to Blur",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun FeatureSectionCompact() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactFeatureTile(
                modifier = Modifier.weight(1f),
                title = "Auto License Plate",
                subtitle = "Smart AI OCR detection"
            )

            CompactFeatureTile(
                modifier = Modifier.weight(1f),
                title = "Smart Face Blur",
                subtitle = "Tracking & obscuring"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactFeatureTile(
                modifier = Modifier.weight(1f),
                title = "Custom Touch Box",
                subtitle = "Any manual region"
            )

            CompactFeatureTile(
                modifier = Modifier.weight(1f),
                title = "Color Filters & FX",
                subtitle = "Cinematic tone grades"
            )
        }
    }
}

@Composable
private fun CompactFeatureTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (appColors.isDark) Color(0xFF141724) else appColors.surface,
        border = BorderStroke(1.dp, appColors.border),
        shadowElevation = if (appColors.isDark) 0.dp else 1.dp,
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = appColors.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = appColors.textSecondary,
                fontSize = 11.5.sp,
                maxLines = 1
            )
        }
    }
}
