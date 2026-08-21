package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.naiyados.aiblurvideo.autoplate.export.ExportBitrate
import com.naiyados.aiblurvideo.autoplate.export.ExportResolution
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun ExportSettingsDialog(
    initialSettings: ExportSettings = ExportSettings(),
    videoDurationMs: Long = 0L,
    isPreExportFlow: Boolean = true,
    onDismissRequest: () -> Unit,
    onConfirmExport: (ExportSettings) -> Unit
) {
    var selectedResolution by remember { mutableStateOf(initialSettings.resolution) }
    var selectedBitrate by remember { mutableStateOf(initialSettings.bitrate) }

    val currentSettings = remember(selectedResolution, selectedBitrate) {
        ExportSettings(
            resolution = selectedResolution,
            bitrate = selectedBitrate
        )
    }

    val durationSeconds = (videoDurationMs / 1000L).coerceAtLeast(1L)
    val estimatedMb = remember(selectedBitrate, durationSeconds) {
        val mbPerSec = (selectedBitrate.bps.toFloat()) / (8f * 1024f * 1024f)
        val totalMb = mbPerSec * durationSeconds
        if (totalMb < 0.1f) 0.1f else totalMb
    }

    val speedLabel = remember(selectedResolution, selectedBitrate) {
        when {
            selectedResolution == ExportResolution.SD_480P || selectedBitrate == ExportBitrate.LOW ->
                "⚡⚡⚡ Ultra Fast Export"
            selectedResolution == ExportResolution.HD_720P || selectedBitrate == ExportBitrate.MEDIUM ->
                "⚡ Fast Export Speed"
            selectedResolution == ExportResolution.FHD_1080P || selectedBitrate == ExportBitrate.STANDARD ->
                "⚖️ Balanced Quality & Speed"
            else ->
                "🎬 Studio High Fidelity"
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("export_configuration_dialog"),
            color = Color(0xFF191924),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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
                                .background(
                                    Brush.linearGradient(
                                        listOf(AiBlurColors.Pink, AiBlurColors.Purple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPreExportFlow) Icons.Rounded.FileDownload else Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isPreExportFlow) "Export Video" else "Export Settings",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Configure resolution, bitrate & speed",
                                color = Color.White.copy(alpha = 0.6f),
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
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Section 1: Output Resolution
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OUTPUT RESOLUTION",
                            color = AiBlurColors.Pink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = selectedResolution.label,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExportResolution.values().forEach { res ->
                            val isSelected = selectedResolution == res
                            SettingOptionRow(
                                title = res.label,
                                subtitle = res.description,
                                isSelected = isSelected,
                                onClick = { selectedResolution = res }
                            )
                        }
                    }
                }

                // Section 2: Video Bitrate & Speed
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "VIDEO BITRATE & SPEED",
                            color = AiBlurColors.Purple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = selectedBitrate.label,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExportBitrate.values().forEach { br ->
                            val isSelected = selectedBitrate == br
                            SettingOptionRow(
                                title = br.label,
                                subtitle = br.description,
                                isSelected = isSelected,
                                onClick = { selectedBitrate = br }
                            )
                        }
                    }
                }

                // Size & Speed Estimation Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = AiBlurColors.Green,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = speedLabel,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (videoDurationMs > 0L) {
                                    String.format("Estimated Size: ~%.1f MB (%ds video)", estimatedMb, durationSeconds)
                                } else {
                                    String.format("Estimated Size: ~%.1f MB per minute", currentSettings.estimateMegabytesPerMinute())
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            onConfirmExport(currentSettings)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("start_export_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AiBlurColors.Pink
                        )
                    ) {
                        Icon(
                            imageVector = if (isPreExportFlow) Icons.Rounded.FileDownload else Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPreExportFlow) "Start Export" else "Save Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AiBlurColors.Pink.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) AiBlurColors.Pink else Color.White.copy(alpha = 0.07f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp
                )
                Text(
                    text = subtitle,
                    color = if (isSelected) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AiBlurColors.Pink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
