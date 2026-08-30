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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    customCropRect: RectF? = null,
    customCropRotation: Float = 0f,
    onCropRectChange: (RectF) -> Unit = {},
    onCropRotationChange: (Float) -> Unit = {},
    onResetCrop: () -> Unit = {},
    customObjectNormalizedRect: RectF? = null,
    customObjectRotationDegrees: Float = 0f,
    customObjectShape: com.naiyados.aiblurvideo.ui.model.CustomBlurShape = com.naiyados.aiblurvideo.ui.model.CustomBlurShape.ROUNDED_RECT,
    onCustomObjectRectChange: (RectF) -> Unit = {},
    onCustomObjectRotationChange: (Float) -> Unit = {},
    isFrameBlurActive: Boolean = true
) {
    var rawVideoWidth by remember { mutableIntStateOf(player?.videoSize?.width?.takeIf { it > 0 } ?: 1080) }
    var rawVideoHeight by remember { mutableIntStateOf(player?.videoSize?.height?.takeIf { it > 0 } ?: 1920) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    rawVideoWidth = videoSize.width
                    rawVideoHeight = videoSize.height
                }
            }
        }
        player.addListener(listener)
        if (player.videoSize.width > 0 && player.videoSize.height > 0) {
            rawVideoWidth = player.videoSize.width
            rawVideoHeight = player.videoSize.height
        }
        onDispose {
            player.removeListener(listener)
        }
    }

    val blurRadiusPx = when (selectedMode) {
        BlurMode.FullBlur -> if (isFrameBlurActive && blurStrength > 0.02f) (blurStrength * 90f).coerceIn(4f, 180f) else 0f
        else -> 0f
    }

    val naturalVideoRatio = if (rawVideoWidth > 0 && rawVideoHeight > 0) {
        rawVideoWidth.toFloat() / rawVideoHeight.toFloat()
    } else {
        9f / 16f
    }

    val previewRatio = if (selectedMode == BlurMode.Crop) naturalVideoRatio else (aspectRatio.ratioValue ?: naturalVideoRatio)
    val isCropToFill = (selectedMode != BlurMode.Crop && aspectRatio != VideoAspectRatio.ORIGINAL)

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
                        .clipToBounds()
                ) {
                    PlayerTextureViewFit(
                        player = player,
                        blurRadiusPx = blurRadiusPx,
                        selectedFilter = selectedFilter,
                        filterIntensity = filterIntensity,
                        cropToFill = isCropToFill
                    )
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

                // Active Draggable Zoomable Crop Transform Overlay
                if (selectedMode == BlurMode.Crop) {
                    CropTransformTouchOverlay(
                        cropRect = customCropRect,
                        rotationDegrees = customCropRotation,
                        videoWidth = rawVideoWidth,
                        videoHeight = rawVideoHeight,
                        aspectRatio = aspectRatio,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onCropRectChanged = onCropRectChange,
                        onRotationChanged = onCropRotationChange,
                        onResetCrop = onResetCrop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Center Floating Play Indicator for intuitive video playback toggle
                if (selectedMode != BlurMode.Crop && selectedMode != BlurMode.Object) {
                    AnimatedVisibility(
                        visible = !isPlaying,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onPlayPauseClick
                                )
                                .testTag("preview_center_play_button"),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.75f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
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
    filterIntensity: Float = 1.0f,
    cropToFill: Boolean = false
) {
    var textureViewRef by remember {
        mutableStateOf<TextureView?>(null)
    }

    // Keep track of applied visual properties to prevent pipeline resets on identical frames
    var lastAppliedBlur by remember { mutableFloatStateOf(-1f) }
    var lastAppliedFilter by remember { mutableStateOf<VideoFilter?>(null) }
    var lastAppliedIntensity by remember { mutableFloatStateOf(-1f) }
    var lastAppliedCropToFill by remember { mutableStateOf<Boolean?>(null) }
    var lastAppliedWidth by remember { mutableIntStateOf(-1) }
    var lastAppliedHeight by remember { mutableIntStateOf(-1) }

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
                val vWidth = player.videoSize.width
                val vHeight = player.videoSize.height

                // Only re-apply transform matrix if dimensions or crop mode actually changed
                if (vWidth != lastAppliedWidth || vHeight != lastAppliedHeight || cropToFill != lastAppliedCropToFill) {
                    applyVideoTransformMatrix(
                        view = texture,
                        videoWidth = vWidth,
                        videoHeight = vHeight,
                        cropToFill = cropToFill
                    )
                    lastAppliedWidth = vWidth
                    lastAppliedHeight = vHeight
                    lastAppliedCropToFill = cropToFill
                }

                // Only re-apply RenderEffect if blur or filter parameters actually changed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (blurRadiusPx != lastAppliedBlur || selectedFilter != lastAppliedFilter || filterIntensity != lastAppliedIntensity) {
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
                        lastAppliedBlur = blurRadiusPx
                        lastAppliedFilter = selectedFilter
                        lastAppliedIntensity = filterIntensity
                    }
                }
            }
        }
    )

    DisposableEffect(player, cropToFill) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                textureViewRef?.let { texture ->
                    applyVideoTransformMatrix(
                        view = texture,
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height,
                        cropToFill = cropToFill
                    )
                }
            }
        }

        player.addListener(listener)

        textureViewRef?.let { texture ->
            applyVideoTransformMatrix(
                view = texture,
                videoWidth = player.videoSize.width,
                videoHeight = player.videoSize.height,
                cropToFill = cropToFill
            )
        }

        onDispose {
            player.removeListener(listener)
            player.clearVideoTextureView(textureViewRef)
        }
    }
}

private fun applyVideoTransformMatrix(
    view: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    cropToFill: Boolean
) {
    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()

    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) {
        return
    }

    val viewAspect = viewWidth / viewHeight
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

    val scaleX: Float
    val scaleY: Float

    if (cropToFill) {
        // CROP TO FILL (Center Crop): Fill entire viewport with video without black borders
        if (videoAspect > viewAspect) {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        }
    } else {
        // FIT: Fit within viewport
        if (videoAspect > viewAspect) {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        } else {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        }
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
