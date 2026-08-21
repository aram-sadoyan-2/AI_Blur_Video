package com.naiyados.aiblurvideo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun EditorHeaderBar(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onSaveClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Video Editor",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Export Settings",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Pro White Export Pill Button
        Button(
            onClick = onSaveClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = "Export",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}