package com.naiyados.aiblurvideo

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.naiyados.aiblurvideo.analytics.AppAnalytics
import com.naiyados.aiblurvideo.ui.screens.BatchQueueScreen
import com.naiyados.aiblurvideo.ui.screens.BlurMode
import com.naiyados.aiblurvideo.ui.screens.HomeScreen
import com.naiyados.aiblurvideo.ui.theme.AIBlurVideoTheme
import com.naiyados.aiblurvideo.ui.theme.AiBlurColors
import com.naiyados.aiblurvideo.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Analytics safely
        AppAnalytics.init(this)

        // Check and track first app open
        val prefs = getSharedPreferences("aiblur_analytics_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            AppAnalytics.trackFirstAppOpen()
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }

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
                            AppAnalytics.trackVideoImported(source = "system_picker")
                        }
                    }

                    if (showEditor.value) {
                        LaunchedEffect(Unit) {
                            AppAnalytics.trackScreenView("VideoEditorScreen", "BlurMode")
                        }
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
                        LaunchedEffect(Unit) {
                            AppAnalytics.trackScreenView("BatchQueueScreen", "BatchQueueScreen")
                        }
                        BatchQueueScreen(
                            onBackClick = {
                                showBatchQueue.value = false
                            },
                            onOpenVideo = { uri ->
                                selectedVideoUri.value = uri
                                showBatchQueue.value = false
                                showEditor.value = true
                                AppAnalytics.trackVideoImported(source = "batch_queue")
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            AppAnalytics.trackScreenView("HomeScreen", "HomeScreen")
                        }
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
                                AppAnalytics.trackVideoImported(source = "history_or_gallery")
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
