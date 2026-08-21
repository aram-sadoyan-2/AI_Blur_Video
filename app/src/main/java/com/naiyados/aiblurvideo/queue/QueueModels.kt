package com.naiyados.aiblurvideo.queue

import android.net.Uri

enum class QueueItemStatus {
    PENDING,
    SCANNING_AI,
    BLURRING_EXPORT,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class QueueVideoItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val durationMs: Long = 0L,
    val durationFormatted: String = "",
    val status: QueueItemStatus = QueueItemStatus.PENDING,
    val progress: Float = 0f,
    val currentStepText: String = "Waiting in queue...",
    val blurredFrames: Int = 0,
    val savedUri: Uri? = null,
    val errorMessage: String? = null
)
