package com.naiyados.aiblurvideo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naiyados.aiblurvideo.ui.components.FeatureTile
import com.naiyados.aiblurvideo.ui.components.GlassCard
import com.naiyados.aiblurvideo.ui.components.GradientButton
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiBlurColors.Background)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = topPadding + 12.dp,
                bottom = bottomPadding + 16.dp
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeHeader(
                isPremium = isPremium,
                onPremiumClick = onPremiumClick
            )

            HeroSection(
                onImportVideoClick = onImportVideoClick
            )

            FeatureSection()

            RecentProjectCard(
                onOpenProjectsClick = onOpenProjectsClick
            )

            Spacer(modifier = Modifier.weight(1f))

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
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Smart blur editor for videos",
                color = AiBlurColors.TextSecondary,
                fontSize = 14.sp
            )
        }

        PremiumBadge(
            isPremium = isPremium,
            onClick = onPremiumClick
        )
    }
}

@Composable
private fun HeroSection(
    onImportVideoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(AiBlurColors.AccentGradient)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black.copy(alpha = 0.36f))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White
                        )

                        Text(
                            text = "AI powered",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Blur videos\nlike a pro",
                    color = Color.White,
                    fontSize = 36.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Import a video, apply cinematic background blur, hide faces, or blur objects in seconds.",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )

                GradientButton(
                    text = "Import video",
                    icon = Icons.Rounded.Movie,
                    onClick = onImportVideoClick
                )
            }
        }
    }
}

@Composable
private fun FeatureSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureTile(
                title = "Background blur",
                subtitle = "Cinematic portrait video",
                modifier = Modifier.weight(1f)
            )

            FeatureTile(
                title = "Face blur",
                subtitle = "Privacy protection",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureTile(
                title = "HD export",
                subtitle = "Premium quality",
                modifier = Modifier.weight(1f)
            )

            FeatureTile(
                title = "No watermark",
                subtitle = "Premium export",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecentProjectCard(
    onOpenProjectsClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = onOpenProjectsClick,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.10f)
                ) {
                    Icon(
                        modifier = Modifier.padding(13.dp),
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = "Recent projects",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Continue editing your saved videos",
                        color = AiBlurColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "Open",
                    color = AiBlurColors.Pink,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TipCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.padding(18.dp),
            text = "Free users can preview AI blur. Premium unlocks HD export and removes watermark.",
            color = AiBlurColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}