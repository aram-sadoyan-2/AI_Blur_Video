package com.naiyados.aiblurvideo.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.queue.BatchQueueManager
import com.naiyados.aiblurvideo.queue.QueueItemStatus
import com.naiyados.aiblurvideo.queue.QueueVideoItem
import com.naiyados.aiblurvideo.ui.components.BatchVideoPreviewDialog
import com.naiyados.aiblurvideo.ui.components.ExportSettingsDialog
import com.naiyados.aiblurvideo.ui.components.ThemeSelectionDialog
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import com.naiyados.aiblurvideo.ui.theme.AppThemeMode
import com.naiyados.aiblurvideo.ui.theme.LocalAppColors
import com.naiyados.aiblurvideo.ui.theme.ThemeManager
import com.naiyados.aiblurvideo.util.ShareHelper
import com.naiyados.aiblurvideo.util.rememberVideoThumbnail

@Composable
fun BatchQueueScreen(
    onBackClick: () -> Unit,
    onOpenVideo: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val themeMode by ThemeManager.themeMode.collectAsState()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val items by BatchQueueManager.queueItems.collectAsState()
    val isProcessing by BatchQueueManager.isProcessing.collectAsState()
    val currentProcessingId by BatchQueueManager.currentProcessingId.collectAsState()
    val exportSettings by BatchQueueManager.exportSettings.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var previewVideoItem by remember { mutableStateOf<QueueVideoItem?>(null) }

    val multipleVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            BatchQueueManager.addVideos(context, uris)
            Toast.makeText(context, "Added ${uris.size} videos to queue", Toast.LENGTH_SHORT).show()
        }
    }

    val completedCount = items.count { it.status == QueueItemStatus.COMPLETED }
    val pendingCount = items.count { it.status == QueueItemStatus.PENDING }
    val totalCount = items.size

    val overallProgress = if (totalCount > 0) {
        val completedProgress = completedCount.toFloat()
        val currentItem = items.firstOrNull { it.id == currentProcessingId }
        val currentProg = currentItem?.progress ?: 0f
        ((completedProgress + currentProg) / totalCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val themeIcon = when (themeMode) {
        AppThemeMode.DARK -> Icons.Rounded.DarkMode
        AppThemeMode.LIGHT -> Icons.Rounded.LightMode
        AppThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AiBlurColors.Background)
    ) {
        // Header
        Surface(
            color = appColors.surface,
            border = BorderStroke(1.dp, appColors.border),
            shadowElevation = if (appColors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = topPadding + 8.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = appColors.textPrimary
                        )
                    }

                    Column {
                        Text(
                            text = "Batch Processing Queue",
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (totalCount == 0) "Queue empty"
                            else if (isProcessing) "Processing ($completedCount/$totalCount completed)"
                            else "$totalCount videos ($completedCount completed, $pendingCount waiting)",
                            color = if (isProcessing) appColors.primary else appColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isProcessing) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Theme Switcher Button
                    Surface(
                        onClick = { showThemeDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (appColors.isDark) Color.White.copy(alpha = 0.08f) else appColors.surfaceElevated,
                        border = BorderStroke(1.dp, appColors.border),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = "Theme Toggle",
                            tint = if (appColors.isDark) appColors.primary else AiBlurColors.Orange,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Settings Button
                    Surface(
                        onClick = { showSettingsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (appColors.isDark) Color.White.copy(alpha = 0.08f) else appColors.surfaceElevated,
                        border = BorderStroke(1.dp, appColors.border),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Batch Settings",
                            tint = appColors.textPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (items.isNotEmpty()) {
                        IconButton(
                            onClick = { BatchQueueManager.clearCompleted() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ClearAll,
                                contentDescription = "Clear Completed",
                                tint = appColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Overall progress banner if processing or has items
        if (totalCount > 0) {
            Surface(
                color = appColors.surfaceElevated,
                border = BorderStroke(1.dp, appColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isProcessing) "Sequential Processing Active" else "Queue Ready",
                                color = if (isProcessing) appColors.primary else appColors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Settings: ${exportSettings.resolution.label} · ${exportSettings.bitrate.label}",
                                color = appColors.textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "${(overallProgress * 100).toInt()}%",
                            color = appColors.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = appColors.primary,
                        trackColor = if (appColors.isDark) Color.White.copy(alpha = 0.12f) else appColors.surfaceVariant
                    )

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isProcessing) {
                            OutlinedButton(
                                onClick = { BatchQueueManager.stopProcessing() },
                                modifier = Modifier.weight(1.15f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stop Processing",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (pendingCount > 0) {
                                        BatchQueueManager.startProcessing(context)
                                    } else {
                                        Toast.makeText(context, "All videos already completed or queue is empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.15f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appColors.primary,
                                    contentColor = if (appColors.isDark) Color.Black else Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pendingCount > 0) "Start Queue ($pendingCount)" else "Queue Done",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { multipleVideoPicker.launch("video/*") },
                            modifier = Modifier.weight(0.95f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Videos",
                                fontSize = 12.5.sp,
                                color = appColors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // List of queued videos or Empty state
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = bottomPadding + 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        appColors.primary.copy(alpha = 0.2f),
                                        appColors.secondary.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = appColors.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Batch Queue is Empty",
                            color = appColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Select multiple videos from your gallery to automatically scan, track, blur license plates, and export them sequentially.",
                            color = appColors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    Button(
                        onClick = { multipleVideoPicker.launch("video/*") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.primary,
                            contentColor = if (appColors.isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Multiple Videos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = bottomPadding + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    QueueItemCard(
                        item = item,
                        isCurrentActive = item.id == currentProcessingId,
                        onOpen = { previewVideoItem = item },
                        onShare = {
                            (item.savedUri ?: item.uri).let { uri ->
                                ShareHelper.shareVideo(
                                    context = context,
                                    uri = uri,
                                    title = "Share ${item.name}"
                                )
                            }
                        },
                        onRetry = { BatchQueueManager.retryItem(context, item.id) },
                        onCancel = { BatchQueueManager.cancelItem(item.id) },
                        onDelete = { BatchQueueManager.removeVideo(item.id) }
                    )
                }
            }
        }
    }

    if (previewVideoItem != null) {
        val item = previewVideoItem!!
        BatchVideoPreviewDialog(
            videoUri = item.savedUri ?: item.uri,
            videoTitle = item.name,
            onDismissRequest = { previewVideoItem = null },
            onOpenInStudio = { uri ->
                previewVideoItem = null
                onOpenVideo(uri)
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showSettingsDialog) {
        ExportSettingsDialog(
            initialSettings = exportSettings,
            isPreExportFlow = false,
            onDismissRequest = { showSettingsDialog = false },
            onConfirmExport = { updated ->
                BatchQueueManager.updateExportSettings(updated)
                showSettingsDialog = false
                Toast.makeText(
                    context,
                    "Batch export set to ${updated.resolution.label} at ${updated.bitrate.label}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun QueueItemCard(
    item: QueueVideoItem,
    isCurrentActive: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val thumbnailBitmap = rememberVideoThumbnail(context = context, uri = item.savedUri ?: item.uri)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentActive) {
            if (appColors.isDark) Color(0xFF161C2C) else appColors.surfaceElevated
        } else {
            if (appColors.isDark) Color(0xFF131622) else appColors.surface
        },
        border = BorderStroke(
            1.dp,
            if (isCurrentActive) appColors.primary.copy(alpha = 0.6f) else appColors.border
        ),
        shadowElevation = if (appColors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail container
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 55.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (appColors.isDark) Color(0xFF0D0F18) else appColors.surfaceVariant)
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap.asImageBitmap(),
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = null,
                                tint = appColors.textTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Duration pill
                    if (item.durationFormatted.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.durationFormatted,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Title and step details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        color = appColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = item.currentStepText,
                        color = if (item.status == QueueItemStatus.FAILED) Color(0xFFFF5252)
                        else if (item.status == QueueItemStatus.COMPLETED) Color(0xFF10B981)
                        else if (isCurrentActive) appColors.primary
                        else appColors.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Pill
                StatusBadge(status = item.status)
            }

            // Progress bar if in progress
            if (item.status == QueueItemStatus.SCANNING_AI || item.status == QueueItemStatus.BLURRING_EXPORT) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = appColors.primary,
                        trackColor = if (appColors.isDark) Color.White.copy(alpha = 0.1f) else appColors.surfaceVariant
                    )
                }
            }

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.status == QueueItemStatus.COMPLETED) {
                        Surface(
                            onClick = onOpen,
                            shape = RoundedCornerShape(8.dp),
                            color = appColors.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, appColors.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Preview",
                                    tint = appColors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Preview",
                                    color = appColors.primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = onShare,
                            shape = RoundedCornerShape(8.dp),
                            color = if (appColors.isDark) Color.White.copy(alpha = 0.08f) else appColors.surfaceElevated,
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = appColors.textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Share",
                                    color = appColors.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (item.status == QueueItemStatus.SCANNING_AI || item.status == QueueItemStatus.BLURRING_EXPORT) {
                        Surface(
                            onClick = onCancel,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF5252).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cancel Task",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Cancel Task",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (item.status == QueueItemStatus.FAILED || item.status == QueueItemStatus.CANCELLED) {
                        Surface(
                            onClick = onRetry,
                            shape = RoundedCornerShape(8.dp),
                            color = if (appColors.isDark) Color.White.copy(alpha = 0.08f) else appColors.surfaceElevated,
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Retry",
                                    tint = appColors.textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Retry",
                                    color = appColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = appColors.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: QueueItemStatus) {
    val appColors = LocalAppColors.current
    val (bgColor, textColor, text) = when (status) {
        QueueItemStatus.PENDING -> Triple(
            if (appColors.isDark) Color.White.copy(alpha = 0.1f) else appColors.surfaceVariant,
            appColors.textSecondary,
            "Queued"
        )
        QueueItemStatus.SCANNING_AI -> Triple(appColors.secondary.copy(alpha = 0.20f), appColors.secondary, "AI Scan")
        QueueItemStatus.BLURRING_EXPORT -> Triple(appColors.primary.copy(alpha = 0.20f), appColors.primary, "Exporting")
        QueueItemStatus.COMPLETED -> Triple(Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF10B981), "Done")
        QueueItemStatus.FAILED -> Triple(Color(0xFFFF5252).copy(alpha = 0.2f), Color(0xFFFF5252), "Failed")
        QueueItemStatus.CANCELLED -> Triple(Color(0xFFFF9800).copy(alpha = 0.2f), Color(0xFFFF9800), "Cancelled")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (status == QueueItemStatus.SCANNING_AI || status == QueueItemStatus.BLURRING_EXPORT) {
                CircularProgressIndicator(
                    modifier = Modifier.size(9.dp),
                    strokeWidth = 1.5.dp,
                    color = textColor
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
