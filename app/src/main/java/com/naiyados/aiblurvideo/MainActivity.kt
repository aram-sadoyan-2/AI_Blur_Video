package com.naiyados.aiblurvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.naiyados.aiblurvideo.ui.screens.HomeScreen
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    color = AiBlurColors.Background
                ) {
                    val isPremium = remember { mutableStateOf(false) }

                    HomeScreen(
                        isPremium = isPremium.value,
                        onImportVideoClick = {
                            // TODO: Open video picker
                        },
                        onPremiumClick = {
                            // TODO: Open premium offer screen
                        },
                        onOpenProjectsClick = {
                            // TODO: Open recent projects screen
                        }
                    )
                }
            }
        }
    }
}