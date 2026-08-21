package com.naiyados.aiblurvideo.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.naiyados.aiblurvideo.autoplate.export.FrameEffectProcessor
import com.naiyados.aiblurvideo.ui.model.BlurMode
import com.naiyados.aiblurvideo.ui.model.VideoAspectRatio
import com.naiyados.aiblurvideo.ui.model.VideoFilter
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun VideoPreviewPanel(
    player: Player?,
    selectedMode: BlurMode,
    blurStrength: Float,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrubPreviewBitmap: Bitmap? = null,
    selectedFilter: VideoFilter = VideoFilter.NONE,
    filterIntensity: Float = 1.0f,
    pixelateBlockSize: Int = 24,
    aspectRatio: VideoAspectRatio = VideoAspectRatio.ORIGINAL,
    customObjectNormalizedRect: RectF? = null,
    customObjectRotationDegrees: Float = 0f,
    customObjectShape: com.naiyados.aiblurvideo.ui.model.CustomBlurShape = com.naiyados.aiblurvideo.ui.model.CustomBlurShape.ROUNDED_RECT,
    onCustomObjectRectChange: (RectF) -> Unit = {},
    onCustomObjectRotationChange: (Float) -> Unit = {}
) {
    val blurRadiusPx = when (selectedMode) {
        BlurMode.FullBlur -> if (blurStrength > 0.02f) (blurStrength * 90f).coerceIn(4f, 180f) else 0f
        else -> 0f
    }

    val previewRatio = aspectRatio.ratioValue ?: (9f / 16f)

    val effectiveScrubBitmap = remember(scrubPreviewBitmap, selectedMode, blurStrength) {
        if (scrubPreviewBitmap != null && selectedMode == BlurMode.FullBlur && blurStrength > 0.05f) {
            try {
                FrameEffectProcessor.blurBitmap(scrubPreviewBitmap, blurStrength)
            } catch (e: Exception) {
                scrubPreviewBitmap
            }
        } else {
            scrubPreviewBitmap
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(previewRatio)
                .background(Color.Black)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (player == null) {
                EmptyPreview()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val blurEffect = if (blurRadiusPx > 0.5f) {
                                    AndroidRenderEffect.createBlurEffect(
                                        blurRadiusPx,
                                        blurRadiusPx,
                                        Shader.TileMode.CLAMP
                                    )
                                } else null

                                val colorFilterEffect = if (selectedFilter != VideoFilter.NONE) {
                                    val cm = selectedFilter.createColorMatrix(filterIntensity)
                                    AndroidRenderEffect.createColorFilterEffect(
                                        ColorMatrixColorFilter(cm)
                                    )
                                } else null

                                renderEffect = when {
                                    blurEffect != null && colorFilterEffect != null -> {
                                        AndroidRenderEffect.createChainEffect(blurEffect, colorFilterEffect)
                                            .asComposeRenderEffect()
                                    }
                                    blurEffect != null -> blurEffect.asComposeRenderEffect()
                                    colorFilterEffect != null -> colorFilterEffect.asComposeRenderEffect()
                                    else -> null
                                }
                            }
                        }
                ) {
                    PlayerTextureViewFit(
                        player = player,
                        blurRadiusPx = blurRadiusPx,
                        selectedFilter = selectedFilter,
                        filterIntensity = filterIntensity
                    )

                    if (effectiveScrubBitmap != null) {
                        Image(
                            bitmap = effectiveScrubBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Real-time Bokeh Background Blur Overlay
                if (selectedMode == BlurMode.Background && blurStrength > 0.02f) {
                    BackgroundBlurLiveOverlay(
                        blurStrength = blurStrength,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Real-time Pixelate Grid Overlay
                if (selectedMode == BlurMode.Pixelate) {
                    PixelateLiveOverlay(
                        blockSize = pixelateBlockSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Interactive Custom Object Box Overlay
                if (selectedMode == BlurMode.Object) {
                    CustomObjectTouchOverlay(
                        normalizedRect = customObjectNormalizedRect,
                        rotationDegrees = customObjectRotationDegrees,
                        shape = customObjectShape,
                        blurStrength = blurStrength,
                        onRectChanged = onCustomObjectRectChange,
                        onRotationChanged = onCustomObjectRotationChange,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundBlurLiveOverlay(
    blurStrength: Float,
    modifier: Modifier = Modifier
) {
    val clamped = blurStrength.coerceIn(0.05f, 1f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val clearRadius = maxOf(size.width, size.height) * (0.50f - clamped * 0.18f)

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFF0A0D18).copy(alpha = 0.50f * clamped),
                    Color(0xFF050810).copy(alpha = 0.85f * clamped)
                ),
                center = Offset(cx, cy),
                radius = clearRadius * 1.5f
            )
        )
    }
}

@Composable
fun PixelateLiveOverlay(
    blockSize: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = blockSize.toFloat().coerceIn(12f, 48f)
        val gridColor = Color.Black.copy(alpha = 0.28f)
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.5f
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            y += step
        }
    }
}

@Composable
fun PlayerTextureViewFit(
    player: Player,
    blurRadiusPx: Float = 0f,
    selectedFilter: VideoFilter = VideoFilter.NONE,
    filterIntensity: Float = 1.0f
) {
    var textureViewRef by remember {
        mutableStateOf<TextureView?>(null)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val container = FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipChildren = true
            }

            val textureView = TextureView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            container.addView(textureView)
            player.setVideoTextureView(textureView)
            textureViewRef = textureView
            container
        },
        update = { container ->
            val currentTexture = textureViewRef ?: container.getChildAt(0) as? TextureView
            currentTexture?.let { texture ->
                player.setVideoTextureView(texture)
                applyFitMatrix(
                    view = texture,
                    videoWidth = player.videoSize.width,
                    videoHeight = player.videoSize.height
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val blurEffect = if (blurRadiusPx > 0.5f) {
                        android.graphics.RenderEffect.createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            Shader.TileMode.CLAMP
                        )
                    } else null

                    val colorFilterEffect = if (selectedFilter != VideoFilter.NONE) {
                        val cm = selectedFilter.createColorMatrix(filterIntensity)
                        android.graphics.RenderEffect.createColorFilterEffect(
                            ColorMatrixColorFilter(cm)
                        )
                    } else null

                    val combinedEffect = when {
                        blurEffect != null && colorFilterEffect != null -> {
                            android.graphics.RenderEffect.createChainEffect(blurEffect, colorFilterEffect)
                        }
                        blurEffect != null -> blurEffect
                        colorFilterEffect != null -> colorFilterEffect
                        else -> null
                    }

                    texture.setRenderEffect(combinedEffect)
                    container.setRenderEffect(combinedEffect)
                }
            }
        }
    )

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                textureViewRef?.let { texture ->
                    applyFitMatrix(
                        view = texture,
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height
                    )
                }
            }
        }

        player.addListener(listener)

        textureViewRef?.let { texture ->
            applyFitMatrix(
                view = texture,
                videoWidth = player.videoSize.width,
                videoHeight = player.videoSize.height
            )
        }

        onDispose {
            player.removeListener(listener)
            player.clearVideoTextureView(textureViewRef)
        }
    }
}

private fun applyFitMatrix(
    view: TextureView,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()

    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) {
        return
    }

    val viewAspect = viewWidth / viewHeight
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

    var scaleX = 1f
    var scaleY = 1f

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

    view.setTransform(matrix)
    view.invalidate()
}

@Composable
private fun EmptyPreview() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.VideoFile,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.28f),
            modifier = Modifier.size(54.dp)
        )

        Text(
            text = "No Video Selected",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
