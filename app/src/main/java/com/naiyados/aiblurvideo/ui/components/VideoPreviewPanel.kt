package com.naiyados.aiblurvideo.ui.components

import android.graphics.Matrix
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun VideoPreviewPanel(
    player: Player?,
    selectedMode: BlurMode,
    blurStrength: Float,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blurRadiusPx = when (selectedMode) {
        BlurMode.FullBlur -> blurStrength * 42f
        else -> 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1.08f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(9f / 16f)
                    .background(Color.Black)
            ) {
                if (player == null) {
                    EmptyPreview()
                } else {
                    PlayerTextureViewFit(
                        player = player,
                        blurRadiusPx = blurRadiusPx
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color.Black.copy(alpha = 0.34f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black.copy(alpha = 0.30f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(58.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTextureViewFit(
    player: Player,
    blurRadiusPx: Float
) {
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    AndroidView(
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
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    } else {
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
private fun EmptyPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.VideoFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = "No video selected",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}