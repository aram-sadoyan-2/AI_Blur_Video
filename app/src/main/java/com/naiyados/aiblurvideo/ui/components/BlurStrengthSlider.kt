package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BlurStrengthSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Slider(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f
    )
}