package com.naiyados.aiblurvideo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.naiyados.aiblurvideo.ui.screens.HomeScreen
import com.naiyados.aiblurvideo.ui.screens.VideoEditorScreen
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            MaterialTheme {
                Surface(
                    color = AiBlurColors.Background
                ) {
                    val isPremium = remember { mutableStateOf(false) }
                    val showEditor = remember { mutableStateOf(false) }
                    val selectedVideoUri = remember { mutableStateOf<Uri?>(null) }

                    val videoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null) {
                            selectedVideoUri.value = uri
                            showEditor.value = true
                        }
                    }

                    if (showEditor.value) {
                        VideoEditorScreen(
                            videoUri = selectedVideoUri.value,
                            isPremium = isPremium.value,
                            onBackClick = {
                                showEditor.value = false
                            },
                            onPremiumClick = {
                                // TODO: Open PremiumOfferScreen
                            },
                            onSaveClick = {
                                // TODO: Export processed video
                            }
                        )
                    } else {
                        HomeScreen(
                            isPremium = isPremium.value,
                            onImportVideoClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )
                            },
                            onPremiumClick = {
                                // TODO: Open PremiumOfferScreen
                            },
                            onOpenProjectsClick = {
                                showEditor.value = true
                            }
                        )
                    }
                }
            }
        }
    }
}