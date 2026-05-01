package com.naiyados.aiblurvideo.ui.components

import android.graphics.Matrix
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import kotlinx.coroutines.delay

@Composable
fun VideoPreviewCard(
    videoUri: Uri?,
    player: Player?,
    selectedMode: BlurMode,
    blurStrength: Float,
    modifier: Modifier = Modifier,
    isProcessing: Boolean,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }

    val blurRadiusPx = when (selectedMode) {
        BlurMode.FullBlur -> blurStrength * 42f
        else -> 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    controlsVisible = !controlsVisible
                }
            }
    ) {
        if (videoUri == null || player == null) {
            EmptyVideoPreview()
        } else {
            PlayerTextureViewFit(
                player = player,
                blurRadiusPx = blurRadiusPx
            )
        }

        TopModeChip(
            selectedMode = selectedMode
        )

        AnimatedVisibility(
            visible = controlsVisible || !isPlaying,
            modifier = Modifier.align(Alignment.Center)
        ) {
            CenterPlayButton(
                isPlaying = isPlaying,
                enabled = videoUri != null && player != null,
                onClick = {
                    if (videoUri != null && player != null) {
                        onPlayingChange(!isPlaying)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CleanVideoTimelineOverlay(
                player = player,
                isPlaying = isPlaying,
                onPlayPauseClick = {
                    if (videoUri != null && player != null) {
                        onPlayingChange(!isPlaying)
                    }
                }
            )
        }

        ModePreviewOverlay(
            selectedMode = selectedMode,
            blurStrength = blurStrength,
            controlsVisible = controlsVisible
        )

        if (isProcessing) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "Analyzing video...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AiBlurColors.Pink,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun TopModeChip(
    selectedMode: BlurMode
) {
    Surface(
        modifier = Modifier
            .padding(14.dp),
        color = Color.Black.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = AiBlurColors.Orange,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = selectedMode.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun CenterPlayButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = {
            if (enabled) onClick()
        },
        color = Color.Black.copy(alpha = 0.36f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Icon(
            modifier = Modifier
                .padding(16.dp)
                .size(34.dp),
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White
        )
    }
}

@Composable
private fun CleanVideoTimelineOverlay(
    player: Player?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(player, isSeeking) {
        while (player != null) {
            durationMs = safeDuration(player)
            positionMs = player.currentPosition.coerceAtLeast(0L)

            if (!isSeeking) {
                sliderValue = positionToSlider(positionMs, durationMs)
            }

            delay(250)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.78f)
                    )
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                value = sliderValue,
                onValueChange = { value ->
                    isSeeking = true
                    sliderValue = value
                },
                onValueChangeFinished = {
                    val target = (durationMs * sliderValue).toLong()
                    player?.seekTo(target)
                    isSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AiBlurColors.Pink,
                    inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                    color = Color.White.copy(alpha = 0.86f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                SmallControlButton(
                    icon = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset",
                    onClick = {
                        player?.seekTo(0L)
                    }
                )

                SmallControlButton(
                    icon = Icons.Rounded.Replay10,
                    contentDescription = "Back 10 seconds",
                    onClick = {
                        player?.let {
                            val newPosition = (it.currentPosition - 10_000L).coerceAtLeast(0L)
                            it.seekTo(newPosition)
                        }
                    }
                )

                SmallControlButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = onPlayPauseClick
                )

                SmallControlButton(
                    icon = Icons.Rounded.Forward10,
                    contentDescription = "Forward 10 seconds",
                    onClick = {
                        player?.let {
                            val max = safeDuration(it)
                            val newPosition = (it.currentPosition + 10_000L).coerceAtMost(max)
                            it.seekTo(newPosition)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SmallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier.size(34.dp),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

@Composable
private fun PlayerTextureViewFit(
    player: Player,
    blurRadiusPx: Float
) {
    var textureViewRef by remember {
        mutableStateOf<TextureView?>(null)
    }

    AndroidView<TextureView>(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                textureViewRef = this
                player.setVideoTextureView(this)
                applyRenderBlur(blurRadiusPx)

                addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    (view as? TextureView)?.post {
                        view.applyFitTransform(player.videoSize)
                    }
                }

                post {
                    applyFitTransform(player.videoSize)
                }
            }
        },
        update = { textureView ->
            textureViewRef = textureView
            player.setVideoTextureView(textureView)
            textureView.applyRenderBlur(blurRadiusPx)

            textureView.post {
                textureView.applyFitTransform(player.videoSize)
            }
        },
        onRelease = { textureView ->
            player.clearVideoTextureView(textureView)
            if (textureViewRef === textureView) {
                textureViewRef = null
            }
        }
    )

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                textureViewRef?.post {
                    textureViewRef?.applyFitTransform(videoSize)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                textureViewRef?.post {
                    textureViewRef?.applyFitTransform(player.videoSize)
                }
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
        }
    }
}

private fun TextureView.applyRenderBlur(
    blurRadiusPx: Float
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (blurRadiusPx > 0f) {
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    blurRadiusPx,
                    blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
            )
        } else {
            setRenderEffect(null)
        }
    }
}

private fun TextureView.applyFitTransform(
    videoSize: VideoSize
) {
    val viewWidth = width.toFloat()
    val viewHeight = height.toFloat()

    val rawVideoWidth = videoSize.width
    val rawVideoHeight = videoSize.height

    if (
        viewWidth <= 0f ||
        viewHeight <= 0f ||
        rawVideoWidth <= 0 ||
        rawVideoHeight <= 0
    ) {
        setTransform(null)
        return
    }

    val pixelRatio = if (videoSize.pixelWidthHeightRatio > 0f) {
        videoSize.pixelWidthHeightRatio
    } else {
        1f
    }

    val videoWidth = rawVideoWidth * pixelRatio
    val videoHeight = rawVideoHeight.toFloat()

    val viewAspect = viewWidth / viewHeight
    val videoAspect = videoWidth / videoHeight

    val scaleX: Float
    val scaleY: Float

    if (videoAspect > viewAspect) {
        // Video is wider than view. Fit width, reduce height.
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    } else {
        // Video is taller than view. Fit height, reduce width.
        scaleX = videoAspect / viewAspect
        scaleY = 1f
    }

    val matrix = Matrix()
    matrix.setScale(
        scaleX,
        scaleY,
        viewWidth / 2f,
        viewHeight / 2f
    )

    setTransform(matrix)
}

@Composable
private fun ModePreviewOverlay(
    selectedMode: BlurMode,
    blurStrength: Float,
    controlsVisible: Boolean
) {
    val bottomPadding = if (controlsVisible) 108.dp else 14.dp

    when (selectedMode) {
        BlurMode.FullBlur -> {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, bottom = bottomPadding),
                    color = Color.Black.copy(alpha = 0.32f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        text = "Full blur ${(blurStrength * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        BlurMode.Background -> ComingSoonChip("Background AI mask soon", bottomPadding)
        BlurMode.Face -> ComingSoonChip("Face blur AI soon", bottomPadding)
        BlurMode.Object -> ComingSoonChip("Object blur is Pro", bottomPadding)
        BlurMode.Pixelate -> ComingSoonChip("Pixelate is Pro", bottomPadding)
    }
}

@Composable
private fun ComingSoonChip(
    text: String,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = bottomPadding),
            color = Color.Black.copy(alpha = 0.34f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptyVideoPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF252A3D),
                        Color(0xFF121622),
                        Color(0xFF080A12)
                    )
                )
            )
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )

                Text(
                    text = "No video selected",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun safeDuration(player: Player): Long {
    val duration = player.duration
    return if (duration > 0L) duration else 0L
}

private fun positionToSlider(
    positionMs: Long,
    durationMs: Long
): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}