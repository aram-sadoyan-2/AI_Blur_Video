package com.naiyados.aiblurvideo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.naiyados.aiblurvideo.ui.screens.BatchQueueScreen
import com.naiyados.aiblurvideo.ui.screens.BlurMode
import com.naiyados.aiblurvideo.ui.screens.HomeScreen
import com.naiyados.aiblurvideo.ui.theme.AIBlurVideoTheme
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import com.naiyados.aiblurvideo.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager.init(this)
        enableEdgeToEdge()

        setContent {
            AIBlurVideoTheme {
                Surface(
                    color = AiBlurColors.Background
                ) {
                    val isPremium = remember { mutableStateOf(false) }
                    val showEditor = remember { mutableStateOf(false) }
                    val showBatchQueue = remember { mutableStateOf(false) }
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
                        BlurMode(
                            videoUri = selectedVideoUri.value,
                            isPremium = isPremium.value,
                            onBackClick = {
                                showEditor.value = false
                            },
                            onPremiumClick = {
                                // Premium action
                            },
                            onSaveClick = {
                                showEditor.value = false
                            }
                        )
                    } else if (showBatchQueue.value) {
                        BatchQueueScreen(
                            onBackClick = {
                                showBatchQueue.value = false
                            },
                            onOpenVideo = { uri ->
                                selectedVideoUri.value = uri
                                showBatchQueue.value = false
                                showEditor.value = true
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
                            onOpenBatchQueue = {
                                showBatchQueue.value = true
                            },
                            onOpenVideo = { uri ->
                                selectedVideoUri.value = uri
                                showEditor.value = true
                            },
                            onPremiumClick = {
                                // Premium action
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
