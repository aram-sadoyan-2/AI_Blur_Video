package com.naiyados.aiblurvideo.queue

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.naiyados.aiblurvideo.autoplate.AutoPlateScanner
import com.naiyados.aiblurvideo.autoplate.AutoPlateTimeline
import com.naiyados.aiblurvideo.autoplate.export.AutoPlateVideoExporter
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings
import com.naiyados.aiblurvideo.history.ProcessedVideoHistoryManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

object BatchQueueManager {
    private const val TAG = "BatchQueueManager"

    private val _queueItems = MutableStateFlow<List<QueueVideoItem>>(emptyList())
    val queueItems: StateFlow<List<QueueVideoItem>> = _queueItems.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentProcessingId = MutableStateFlow<String?>(null)
    val currentProcessingId: StateFlow<String?> = _currentProcessingId.asStateFlow()

    private val _exportSettings = MutableStateFlow(ExportSettings())
    val exportSettings: StateFlow<ExportSettings> = _exportSettings.asStateFlow()

    private val _blurStrength = MutableStateFlow(0.65f)
    val blurStrength: StateFlow<Float> = _blurStrength.asStateFlow()

    private var queueJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun updateExportSettings(settings: ExportSettings) {
        _exportSettings.value = settings
    }

    fun updateBlurStrength(strength: Float) {
        _blurStrength.value = strength
    }

    fun addVideos(context: Context, uris: List<Uri>) {
        val newItems = uris.map { uri ->
            var name = "Video_${System.currentTimeMillis()}"
            var durationMs = 0L

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                }
            } catch (e: Exception) {
                // Use fallback name
            }

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }

            val sec = (durationMs / 1000) % 60
            val min = (durationMs / (1000 * 60)) % 60
            val durFormatted = if (durationMs > 0) String.format(Locale.US, "%d:%02d", min, sec) else "--:--"

            QueueVideoItem(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = name,
                durationMs = durationMs,
                durationFormatted = durFormatted,
                status = QueueItemStatus.PENDING,
                progress = 0f,
                currentStepText = "Waiting in queue"
            )
        }

        _queueItems.update { current -> current + newItems }
    }

    fun removeVideo(id: String) {
        if (_currentProcessingId.value == id) {
            // Cancel current processing if it's the active one
            stopProcessing()
        }
        _queueItems.update { list -> list.filter { it.id != id } }
    }

    fun clearCompleted() {
        _queueItems.update { list ->
            list.filter { it.status != QueueItemStatus.COMPLETED }
        }
    }

    fun clearAll() {
        stopProcessing()
        _queueItems.value = emptyList()
    }

    fun startProcessing(context: Context) {
        if (_isProcessing.value) return

        queueJob = scope.launch {
            _isProcessing.value = true

            try {
                while (true) {
                    val nextItem = _queueItems.value.firstOrNull {
                        it.status == QueueItemStatus.PENDING || it.status == QueueItemStatus.CANCELLED
                    } ?: break

                    processItem(context, nextItem.id)
                }
            } finally {
                _isProcessing.value = false
                _currentProcessingId.value = null
            }
        }
    }

    fun stopProcessing() {
        queueJob?.cancel()
        queueJob = null
        _isProcessing.value = false
        _currentProcessingId.value?.let { currentId ->
            updateItemStatus(currentId, QueueItemStatus.CANCELLED, progress = 0f, stepText = "Cancelled")
        }
        _currentProcessingId.value = null
    }

    fun cancelItem(id: String) {
        if (_currentProcessingId.value == id) {
            stopProcessing()
        } else {
            updateItemStatus(id, QueueItemStatus.CANCELLED, progress = 0f, stepText = "Cancelled")
        }
    }

    fun retryItem(context: Context, id: String) {
        updateItemStatus(id, QueueItemStatus.PENDING, progress = 0f, stepText = "Waiting in queue")
        if (!_isProcessing.value) {
            startProcessing(context)
        }
    }

    private suspend fun processItem(context: Context, itemId: String) {
        _currentProcessingId.value = itemId
        val item = _queueItems.value.firstOrNull { it.id == itemId } ?: return

        try {
            // Phase 1: AI Plate Scan (0% -> 40%)
            updateItemStatus(
                id = itemId,
                status = QueueItemStatus.SCANNING_AI,
                progress = 0.05f,
                stepText = "Scanning frames with AI..."
            )

            val scanner = AutoPlateScanner(context)
            val scanResult = scanner.scan(item.uri) { framesScanned, totalFrames, detectionsFound, progress ->
                val pct = (progress * 100).toInt()
                updateItemStatus(
                    id = itemId,
                    status = QueueItemStatus.SCANNING_AI,
                    progress = 0.05f + (0.35f * progress),
                    stepText = "AI scanning $pct%: $framesScanned/$totalFrames frames ($detectionsFound plates)"
                )
            }

            // Phase 2: Video Export & Blur (40% -> 100%)
            updateItemStatus(
                id = itemId,
                status = QueueItemStatus.BLURRING_EXPORT,
                progress = 0.40f,
                stepText = "Blurring plates & encoding..."
            )

            val exporter = AutoPlateVideoExporter(context)
            val currentSettings = _exportSettings.value
            val strength = _blurStrength.value

            val timeline = AutoPlateTimeline(
                boxes = scanResult.boxes,
                videoDurationMs = item.durationMs
            )

            val exportResult = exporter.export(
                inputUri = item.uri,
                timeline = timeline,
                durationMs = item.durationMs,
                blurStrength = strength,
                exportSettings = currentSettings,
                onProgress = { exportProg ->
                    val totalProg = 0.40f + (0.60f * exportProg.coerceIn(0f, 1f))
                    updateItemStatus(
                        id = itemId,
                        status = QueueItemStatus.BLURRING_EXPORT,
                        progress = totalProg,
                        stepText = "Rendering video: ${(exportProg * 100).toInt()}%"
                    )
                }
            )

            // Save to history
            ProcessedVideoHistoryManager.addProcessedVideo(
                context = context,
                uri = exportResult.outputUri,
                blurredFrames = exportResult.blurredFrames,
                durationMs = item.durationMs,
                resolutionLabel = currentSettings.resolution.label
            )

            _queueItems.update { list ->
                list.map { qItem ->
                    if (qItem.id == itemId) {
                        qItem.copy(
                            status = QueueItemStatus.COMPLETED,
                            progress = 1.0f,
                            currentStepText = "Finished! Blurred on ${exportResult.blurredFrames} frames",
                            blurredFrames = exportResult.blurredFrames,
                            savedUri = exportResult.outputUri
                        )
                    } else qItem
                }
            }

        } catch (ce: CancellationException) {
            updateItemStatus(itemId, QueueItemStatus.CANCELLED, progress = 0f, stepText = "Cancelled")
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error processing item $itemId", e)
            _queueItems.update { list ->
                list.map { qItem ->
                    if (qItem.id == itemId) {
                        qItem.copy(
                            status = QueueItemStatus.FAILED,
                            progress = 0f,
                            currentStepText = "Failed: ${e.localizedMessage ?: "Unknown error"}",
                            errorMessage = e.message
                        )
                    } else qItem
                }
            }
        }
    }

    private fun updateItemStatus(
        id: String,
        status: QueueItemStatus,
        progress: Float,
        stepText: String
    ) {
        _queueItems.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(
                        status = status,
                        progress = progress,
                        currentStepText = stepText
                    )
                } else item
            }
        }
    }
}
