package com.naiyados.aiblurvideo.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        isPremium = false,
        onImportVideoClick = {},
        onPremiumClick = {},
        onOpenProjectsClick = {}
    )
}