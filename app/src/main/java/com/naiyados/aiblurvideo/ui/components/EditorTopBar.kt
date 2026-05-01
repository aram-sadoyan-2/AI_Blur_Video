package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun EditorTopBar(
    modifier: Modifier = Modifier,
    isPremium: Boolean,
    onBackClick: () -> Unit,
    onPremiumClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = "AI Editor",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f)
        )

        Surface(
            onClick = onPremiumClick,
            shape = RoundedCornerShape(50),
            color = if (isPremium) {
                AiBlurColors.Purple.copy(alpha = 0.26f)
            } else {
                Color.White.copy(alpha = 0.08f)
            },
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremium) AiBlurColors.Orange else Color.White,
                    modifier = Modifier.size(17.dp)
                )

                Text(
                    text = if (isPremium) "PRO" else "Upgrade",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}