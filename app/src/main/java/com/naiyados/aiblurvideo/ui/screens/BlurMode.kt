package com.naiyados.aiblurvideo.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.naiyados.aiblurvideo.autoplate.AutoPlateBox
import com.naiyados.aiblurvideo.autoplate.AutoPlateOverlay
import com.naiyados.aiblurvideo.autoplate.AutoPlateScanner
import com.naiyados.aiblurvideo.autoplate.AutoPlateTimeline
import com.naiyados.aiblurvideo.autoplate.PlateTrackConfidence
import com.naiyados.aiblurvideo.ui.components.BlurStrengthSlider
import com.naiyados.aiblurvideo.ui.components.EditorActionTool
import com.naiyados.aiblurvideo.ui.components.EditorHeaderBar
import com.naiyados.aiblurvideo.ui.components.PlaybackControlBar
import com.naiyados.aiblurvideo.ui.components.VideoFrameStripSection
import com.naiyados.aiblurvideo.ui.components.VideoPreviewPanel
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoEditorScreen(
    videoUri: Uri?,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = remember(videoUri) {
        if (videoUri == null) null else ExoPlayer.Builder(context).build()
    }

    var selectedMode by remember { mutableStateOf(BlurMode.FullBlur) }
    var blurStrength by remember { mutableFloatStateOf(0.45f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    var scrubPreviewBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var wasPlayingBeforeScrub by remember {
        mutableStateOf(false)
    }

    var clearScrubPreviewSignal by remember {
        mutableLongStateOf(0L)
    }

    var isAutoPlateScanning by remember {
        mutableStateOf(false)
    }

    var autoPlateFoundCount by remember {
        mutableIntStateOf(0)
    }

    var autoPlateBoxes by remember {
        mutableStateOf<List<AutoPlateBox>>(emptyList())
    }

    var autoPlateVideoDurationMs by remember {
        mutableLongStateOf(0L)
    }

    var autoPlateConfidence by remember {
        mutableStateOf(PlateTrackConfidence.Low)
    }

    var autoPlateDominantText by remember {
        mutableStateOf<String?>(null)
    }

    var currentPositionMs by remember {
        mutableLongStateOf(0L)
    }

    val timeline = remember(autoPlateBoxes, autoPlateVideoDurationMs) {
        AutoPlateTimeline(
            boxes = autoPlateBoxes,
            videoDurationMs = autoPlateVideoDurationMs
        )
    }

    val currentAutoPlateBoxes = if (selectedMode == BlurMode.AutoPlate) {
        timeline.boxesAt(currentPositionMs)
    } else {
        emptyList()
    }

    LaunchedEffect(player) {
        while (player != null) {
            currentPositionMs = player.currentPosition
            delay(80)
        }
    }

    LaunchedEffect(clearScrubPreviewSignal) {
        if (clearScrubPreviewSignal > 0L) {
            delay(420)
            scrubPreviewBitmap = null

            if (wasPlayingBeforeScrub && player != null) {
                isPlaying = true
                player.play()
            }

            wasPlayingBeforeScrub = false
        }
    }

    LaunchedEffect(player, videoUri) {
        if (player != null && videoUri != null) {
            player.setMediaItem(MediaItem.fromUri(videoUri))
            player.setSeekParameters(SeekParameters.EXACT)
            player.prepare()
            player.playWhenReady = false
            isPlaying = false
        }
    }

    LaunchedEffect(player, isPlaying) {
        player?.let {
            if (isPlaying) {
                it.play()
            } else {
                it.pause()
            }
        }
    }

    LaunchedEffect(player, isMuted) {
        player?.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(player) {
        onDispose {
            player?.release()
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBlurColors.Background)
            .padding(
                top = topPadding + 4.dp,
                bottom = bottomPadding + 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditorHeaderBar(
            modifier = Modifier.padding(horizontal = 14.dp),
            onBackClick = {
                isPlaying = false
                player?.pause()
                onBackClick()
            },
            onSaveClick = {
                isPlaying = false
                player?.pause()
                onSaveClick()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            VideoPreviewPanel(
                modifier = Modifier.fillMaxSize(),
                player = player,
                selectedMode = selectedMode,
                blurStrength = blurStrength,
                isPlaying = isPlaying,
                scrubPreviewBitmap = scrubPreviewBitmap,
                onPlayPauseClick = {
                    if (player != null) {
                        isPlaying = !isPlaying
                    }
                }
            )

            if (selectedMode == BlurMode.AutoPlate && currentAutoPlateBoxes.isNotEmpty()) {
                AutoPlateOverlay(
                    modifier = Modifier.fillMaxSize(),
                    boxes = currentAutoPlateBoxes
                )
            }

            if (selectedMode == BlurMode.AutoPlate) {
                Text(
                    text = when {
                        isAutoPlateScanning -> "AI scanning plate... $autoPlateFoundCount frames"
                        autoPlateConfidence == PlateTrackConfidence.Low ->
                            "Plate not found — try a clearer video"
                        autoPlateConfidence == PlateTrackConfidence.High ->
                            "Plate locked (AI detector)"
                        else ->
                            "Plate tracked (AI detector)"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        PlaybackControlBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            player = player,
            isPlaying = isPlaying,
            isMuted = isMuted,
            onPlayPauseClick = {
                if (player != null) {
                    isPlaying = !isPlaying
                }
            },
            onToggleMute = {
                isMuted = !isMuted
            }
        )

        VideoFrameStripSection(
            modifier = Modifier.padding(horizontal = 16.dp),
            videoUri = videoUri,
            maxDurationSeconds = 30,
            onSeekTo = { seekMs ->
                player?.seekTo(seekMs)
                clearScrubPreviewSignal = SystemClock.elapsedRealtime()
            },
            onScrubFrameChange = { bitmap ->
                if (bitmap != null) {
                    if (scrubPreviewBitmap == null) {
                        wasPlayingBeforeScrub = isPlaying
                        isPlaying = false
                        player?.pause()
                    }

                    scrubPreviewBitmap = bitmap
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorActionTool(
                title = "Auto Plate",
                icon = Icons.Rounded.DirectionsCar,
                selected = selectedMode == BlurMode.AutoPlate,
                onClick = {
                    selectedMode = BlurMode.AutoPlate
                    Log.d("AutoPlate", "Auto Plate clicked")

                    if (videoUri != null && !isAutoPlateScanning) {
                        scope.launch {
                            isAutoPlateScanning = true
                            autoPlateFoundCount = 0
                            autoPlateBoxes = emptyList()
                            autoPlateConfidence = PlateTrackConfidence.Low
                            autoPlateDominantText = null

                            val scanner = AutoPlateScanner(context)
                            val scanResult = scanner.scan(
                                videoUri = videoUri,
                                onFoundCountChanged = { count ->
                                    autoPlateFoundCount = count
                                }
                            )

                            autoPlateBoxes = scanResult.boxes
                            autoPlateVideoDurationMs = scanResult.durationMs
                            autoPlateConfidence = scanResult.confidence
                            autoPlateDominantText = scanResult.dominantText
                            autoPlateFoundCount = scanResult.boxes.size
                            isAutoPlateScanning = false

                            Log.d(
                                "AutoPlate",
                                "Scan finished. Found=${scanResult.boxes.size} confidence=${scanResult.confidence}"
                            )
                        }
                    }
                }
            )

            EditorActionTool(
                title = "Full Blur",
                icon = Icons.Rounded.BlurOn,
                selected = selectedMode == BlurMode.FullBlur,
                onClick = {
                    selectedMode = BlurMode.FullBlur
                }
            )

            EditorActionTool(
                title = "Background",
                icon = Icons.Rounded.Person,
                selected = selectedMode == BlurMode.Background,
                onClick = {
                    selectedMode = BlurMode.Background
                }
            )

            EditorActionTool(
                title = "Face",
                icon = Icons.Rounded.Face,
                selected = selectedMode == BlurMode.Face,
                onClick = {
                    selectedMode = BlurMode.Face
                }
            )

            EditorActionTool(
                title = "Object",
                icon = Icons.Rounded.Interests,
                selected = selectedMode == BlurMode.Object,
                premium = !isPremium,
                onClick = {
                    if (isPremium) {
                        selectedMode = BlurMode.Object
                    } else {
                        onPremiumClick()
                    }
                }
            )

            EditorActionTool(
                title = "Pixelate",
                icon = Icons.Rounded.GridView,
                selected = selectedMode == BlurMode.Pixelate,
                premium = !isPremium,
                onClick = {
                    if (isPremium) {
                        selectedMode = BlurMode.Pixelate
                    } else {
                        onPremiumClick()
                    }
                }
            )

            EditorActionTool(
                title = "Effects",
                icon = Icons.Rounded.AutoFixHigh,
                selected = false,
                onClick = { }
            )
        }

        BlurStrengthSlider(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = blurStrength,
            onValueChange = { value ->
                blurStrength = value
            }
        )
    }
}