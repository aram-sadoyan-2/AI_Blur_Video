package com.naiyados.aiblurvideo.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.naiyados.aiblurvideo.ui.components.BlurModeChip
import com.naiyados.aiblurvideo.ui.components.BlurStrengthSlider
import com.naiyados.aiblurvideo.ui.components.EditorTopBar
import com.naiyados.aiblurvideo.ui.components.ExportBottomBar
import com.naiyados.aiblurvideo.ui.components.VideoPreviewCard
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun VideoEditorScreen(
    videoUri: Uri?,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val context = LocalContext.current

    val player = remember(videoUri) {
        if (videoUri == null) {
            null
        } else {
            ExoPlayer.Builder(context).build()
        }
    }

    var selectedMode by remember { mutableStateOf(BlurMode.FullBlur) }
    var blurStrength by remember { mutableFloatStateOf(0.45f) }
    var isProcessing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(player, videoUri) {
        if (player != null && videoUri != null) {
            player.setMediaItem(MediaItem.fromUri(videoUri))
            player.prepare()
            player.playWhenReady = false
            isPlaying = false
        }
    }

    LaunchedEffect(player, isPlaying) {
        if (player != null) {
            if (isPlaying) {
                player.play()
            } else {
                player.pause()
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            player?.release()
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBlurColors.Background),
        contentPadding = PaddingValues(
            top = topPadding + 10.dp,
            bottom = bottomPadding + 14.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EditorTopBar(
                modifier = Modifier.padding(horizontal = 16.dp),
                isPremium = isPremium,
                onBackClick = {
                    isPlaying = false
                    player?.pause()
                    onBackClick()
                },
                onPremiumClick = onPremiumClick
            )
        }

        item {
            VideoPreviewCard(
                modifier = Modifier.fillMaxWidth(),
                videoUri = videoUri,
                player = player,
                selectedMode = selectedMode,
                blurStrength = blurStrength,
                isProcessing = isProcessing,
                isPlaying = isPlaying,
                onPlayingChange = { playing ->
                    isPlaying = playing
                }
            )
        }

        item {
            BlurModeRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedMode = selectedMode,
                isPremium = isPremium,
                onModeSelected = { mode ->
                    val premiumLocked = mode == BlurMode.Object || mode == BlurMode.Pixelate

                    if (premiumLocked && !isPremium) {
                        onPremiumClick()
                    } else {
                        selectedMode = mode
                    }
                }
            )
        }

        item {
            BlurStrengthSlider(
                modifier = Modifier.padding(horizontal = 16.dp),
                value = blurStrength,
                onValueChange = { value ->
                    blurStrength = value
                }
            )
        }

        item {
            ExportBottomBar(
                modifier = Modifier.padding(horizontal = 16.dp),
                isPremium = isPremium,
                onExportClick = {
                    isPlaying = false
                    player?.pause()
                    isProcessing = true
                    onExportClick()
                },
                onPremiumClick = onPremiumClick
            )
        }
    }
}

@Composable
private fun BlurModeRow(
    modifier: Modifier = Modifier,
    selectedMode: BlurMode,
    isPremium: Boolean,
    onModeSelected: (BlurMode) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BlurModeChip(
            text = "Full blur",
            icon = Icons.Rounded.BlurOn,
            selected = selectedMode == BlurMode.FullBlur,
            locked = false,
            onClick = {
                onModeSelected(BlurMode.FullBlur)
            }
        )

        BlurModeChip(
            text = "Background",
            icon = Icons.Rounded.Person,
            selected = selectedMode == BlurMode.Background,
            locked = false,
            onClick = {
                onModeSelected(BlurMode.Background)
            }
        )

        BlurModeChip(
            text = "Face",
            icon = Icons.Rounded.Face,
            selected = selectedMode == BlurMode.Face,
            locked = false,
            onClick = {
                onModeSelected(BlurMode.Face)
            }
        )

        BlurModeChip(
            text = "Object",
            icon = Icons.Rounded.Interests,
            selected = selectedMode == BlurMode.Object,
            locked = !isPremium,
            onClick = {
                onModeSelected(BlurMode.Object)
            }
        )

        BlurModeChip(
            text = "Pixelate",
            icon = Icons.Rounded.GridView,
            selected = selectedMode == BlurMode.Pixelate,
            locked = !isPremium,
            onClick = {
                onModeSelected(BlurMode.Pixelate)
            }
        )
    }
}