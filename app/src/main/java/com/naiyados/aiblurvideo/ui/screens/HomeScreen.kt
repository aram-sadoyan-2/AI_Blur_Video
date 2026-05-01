package com.naiyados.aiblurvideo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.components.GlassCard
import com.naiyados.aiblurvideo.ui.components.PremiumBadge
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

@Composable
fun HomeScreen(
    isPremium: Boolean,
    onImportVideoClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onOpenProjectsClick: () -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBlurColors.Background),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = topPadding + 10.dp,
            bottom = bottomPadding + 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HomeHeader(
                isPremium = isPremium,
                onPremiumClick = onPremiumClick
            )
        }

        item {
            HeroSection()
        }

        item {
            FeatureSectionCompact()
        }

        item {
            ImportVideoBigButton(
                onClick = onImportVideoClick
            )
        }

        item {
            SmallInstructionCard()
        }

        item {
            TipCard()
        }
    }
}

@Composable
private fun HomeHeader(
    isPremium: Boolean,
    onPremiumClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AI Blur Video",
                color = AiBlurColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Smart blur editor for videos",
                color = AiBlurColors.TextSecondary,
                fontSize = 13.sp
            )
        }

        PremiumBadge(
            isPremium = isPremium,
            onClick = onPremiumClick
        )
    }
}

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(AiBlurColors.AccentGradient)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.36f))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White
                        )

                        Text(
                            text = "AI powered",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "Blur videos\nlike a pro",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Choose a video below and start applying smart blur.",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureSectionCompact() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactFeatureTile(
                title = "Background blur",
                subtitle = "Portrait video",
                modifier = Modifier.weight(1f)
            )

            CompactFeatureTile(
                title = "Face blur",
                subtitle = "Privacy tool",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactFeatureTile(
                title = "HD export",
                subtitle = "Premium",
                modifier = Modifier.weight(1f)
            )

            CompactFeatureTile(
                title = "No watermark",
                subtitle = "Premium",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactFeatureTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = AiBlurColors.Panel
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = AiBlurColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = AiBlurColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ImportVideoBigButton(
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AiBlurColors.AccentGradient),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = Color.White
                )

                Text(
                    text = "Choose Video",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SmallInstructionCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.10f)
            ) {
                Icon(
                    modifier = Modifier.padding(10.dp),
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Column {
                Text(
                    text = "Start by choosing a video",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )

                Text(
                    text = "The gallery picker opens after tapping Choose Video.",
                    color = AiBlurColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun TipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Free users can preview AI blur. Premium unlocks HD export and removes watermark.",
            color = AiBlurColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}