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
import com.naiyados.aiblurvideo.history.ProcessedVideoHistoryManager
import com.naiyados.aiblurvideo.queue.BatchQueueManager
import com.naiyados.aiblurvideo.queue.QueueItemStatus
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

        if (historyItems.isNotEmpty()) {
            item {
                HistorySection(
                    items = historyItems,
                    onOpenVideo = onOpenVideo,
                    onDeleteVideo = { id ->
                        ProcessedVideoHistoryManager.removeVideo(context, id)
                    }
                )
            }
        }

        item {
            BatchQueueHeroBanner(
                queueCount = queueItems.size,
                isProcessing = isQueueProcessing,
                onClick = onOpenBatchQueue
            )
        }

        item {
            HeroSection()
        }

        item {
            FeatureSectionCompact()
        }

        item {
            ImportVideoBigButton(
                onClick = onImportVideoClick
            )
        }

        item {
            SmallInstructionCard()
        }

        item {
            TipCard()
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
        color = if (appColors.isDark) Color(0xFF1E1C2B) else appColors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isProcessing) AiBlurColors.Pink else appColors.border
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
                                    AiBlurColors.Pink.copy(alpha = 0.3f),
                                    AiBlurColors.Purple.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AiBlurColors.Pink,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = AiBlurColors.Pink,
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
                                    .background(AiBlurColors.Pink)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Processing",
                                    color = Color.White,
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
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AI Blur Video",
                color = AiBlurColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Smart blur editor for videos",
                color = AiBlurColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Toggle Button
            Surface(
                onClick = onThemeClick,
                shape = RoundedCornerShape(16.dp),
                color = if (appColors.isDark) Color.White.copy(alpha = 0.08f) else appColors.surfaceElevated,
                border = BorderStroke(1.dp, appColors.border),
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = "Change Theme",
                        tint = if (appColors.isDark) AiBlurColors.Purple else AiBlurColors.Orange,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = when (themeMode) {
                            AppThemeMode.DARK -> "Dark"
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.SYSTEM -> "Auto"
                        },
                        color = appColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(AiBlurColors.AccentGradient)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.36f))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White
                        )

                        Text(
                            text = "AI powered",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "Blur videos\nlike a pro",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Choose a video below and start applying smart blur.",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp
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
    Card(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = AiBlurColors.Panel
        ),
        border = BorderStroke(
            width = 1.dp,
            color = appColors.border
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (appColors.isDark) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = AiBlurColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = AiBlurColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1
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
        shape = RoundedCornerShape(22.dp),
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
                    text = "Choose Video",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SmallInstructionCard() {
    val appColors = LocalAppColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (appColors.isDark) Color.White.copy(alpha = 0.10f) else appColors.surfaceVariant
            ) {
                Icon(
                    modifier = Modifier.padding(10.dp),
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = if (appColors.isDark) Color.White else appColors.textPrimary
                )
            }

            Column {
                Text(
                    text = "Start by choosing a video",
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )

                Text(
                    text = "The gallery picker opens after tapping Choose Video.",
                    color = AiBlurColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun TipCard() {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appColors.isDark) Color.White.copy(alpha = 0.06f) else appColors.surface
        ),
        border = BorderStroke(1.dp, appColors.border),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (appColors.isDark) 0.dp else 1.dp
        )
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Free users can preview AI blur. Premium unlocks HD export and removes watermark.",
            color = AiBlurColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}
