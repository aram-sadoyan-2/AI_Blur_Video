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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.queue.BatchQueueManager
import com.naiyados.aiblurvideo.queue.QueueItemStatus
import com.naiyados.aiblurvideo.queue.QueueVideoItem
import com.naiyados.aiblurvideo.ui.components.ExportSettingsDialog
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import com.naiyados.aiblurvideo.util.ShareHelper
import com.naiyados.aiblurvideo.util.rememberVideoThumbnail

@Composable
fun BatchQueueScreen(
    onBackClick: () -> Unit,
    onOpenVideo: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by BatchQueueManager.queueItems.collectAsState()
    val isProcessing by BatchQueueManager.isProcessing.collectAsState()
    val currentProcessingId by BatchQueueManager.currentProcessingId.collectAsState()
    val exportSettings by BatchQueueManager.exportSettings.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AiBlurColors.Background)
    ) {
        // Header
        Surface(
            color = Color(0xFF161622),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "Batch Processing Queue",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (totalCount == 0) "Queue empty"
                            else if (isProcessing) "Processing ($completedCount/$totalCount completed)"
                            else "$totalCount videos ($completedCount completed, $pendingCount waiting)",
                            color = if (isProcessing) AiBlurColors.Pink else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Settings Button
                    Surface(
                        onClick = { showSettingsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Batch Settings",
                            tint = Color.White,
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
                                tint = Color.White.copy(alpha = 0.7f),
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
                color = Color(0xFF1E1E2C),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
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
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Settings: ${exportSettings.resolution.label} · ${exportSettings.bitrate.label}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "${(overallProgress * 100).toInt()}%",
                            color = AiBlurColors.Pink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AiBlurColors.Pink,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isProcessing) {
                            OutlinedButton(
                                onClick = { BatchQueueManager.stopProcessing() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Stop Processing", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AiBlurColors.Pink)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pendingCount > 0) "Start Queue ($pendingCount)" else "Queue Done",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { multipleVideoPicker.launch("video/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Add Videos", fontSize = 13.sp, color = Color.White)
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
                    .padding(32.dp),
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
                                        AiBlurColors.Pink.copy(alpha = 0.2f),
                                        AiBlurColors.Purple.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = AiBlurColors.Pink,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Batch Queue is Empty",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Select multiple videos from your gallery to automatically scan, track, blur license plates, and export them sequentially.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    Button(
                        onClick = { multipleVideoPicker.launch("video/*") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AiBlurColors.Pink),
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
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    QueueItemCard(
                        item = item,
                        isCurrentActive = item.id == currentProcessingId,
                        onOpen = { onOpenVideo(item.savedUri ?: item.uri) },
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
    val thumbnailBitmap = rememberVideoThumbnail(context = context, uri = item.savedUri ?: item.uri)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentActive) Color(0xFF221D2C) else Color(0xFF1A1A26),
        border = BorderStroke(
            1.dp,
            if (isCurrentActive) AiBlurColors.Pink.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
        ),
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
                        .background(Color(0xFF101016))
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
                                tint = Color.White.copy(alpha = 0.3f),
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = item.currentStepText,
                        color = if (item.status == QueueItemStatus.FAILED) Color(0xFFFF5252)
                        else if (item.status == QueueItemStatus.COMPLETED) Color(0xFF66BB6A)
                        else if (isCurrentActive) AiBlurColors.Pink
                        else Color.White.copy(alpha = 0.6f),
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
                        color = AiBlurColors.Pink,
                        trackColor = Color.White.copy(alpha = 0.1f)
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
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Preview",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            onClick = onShare,
                            shape = RoundedCornerShape(8.dp),
                            color = AiBlurColors.Pink.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AiBlurColors.Pink.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Share",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
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
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Retry",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Retry",
                                    color = Color.White,
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
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: QueueItemStatus) {
    val (bgColor, textColor, text) = when (status) {
        QueueItemStatus.PENDING -> Triple(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.8f), "Queued")
        QueueItemStatus.SCANNING_AI -> Triple(AiBlurColors.Purple.copy(alpha = 0.25f), Color(0xFFBA68C8), "AI Scan")
        QueueItemStatus.BLURRING_EXPORT -> Triple(AiBlurColors.Pink.copy(alpha = 0.25f), AiBlurColors.Pink, "Exporting")
        QueueItemStatus.COMPLETED -> Triple(Color(0xFF4CAF50).copy(alpha = 0.2f), Color(0xFF81C784), "Done")
        QueueItemStatus.FAILED -> Triple(Color(0xFFFF5252).copy(alpha = 0.2f), Color(0xFFFF8A80), "Failed")
        QueueItemStatus.CANCELLED -> Triple(Color(0xFFFF9800).copy(alpha = 0.2f), Color(0xFFFFB74D), "Cancelled")
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
