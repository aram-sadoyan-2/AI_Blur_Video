package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naiyados.aiblurvideo.ui.theme.LocalAppColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appColors.isDark) Color.White.copy(alpha = 0.07f) else appColors.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (appColors.isDark) Color.White.copy(alpha = 0.12f) else appColors.border
        )
    ) {
        Column(content = content)
    }
}
