package com.naiyados.aiblurvideo.history

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ProcessedVideoRecord(
    val id: String,
    val uriString: String,
    val timestamp: Long,
    val blurredFrames: Int,
    val durationMs: Long,
    val resolutionLabel: String
) {
    val uri: Uri get() = Uri.parse(uriString)
}

object ProcessedVideoHistoryManager {
    private const val PREFS_NAME = "ai_blur_history_prefs"
    private const val KEY_HISTORY = "history_records"

    private val _historyFlow = MutableStateFlow<List<ProcessedVideoRecord>>(emptyList())
    val historyFlow: StateFlow<List<ProcessedVideoRecord>> = _historyFlow.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadHistory()
        }
    }

    private fun loadHistory() {
        val json = prefs?.getString(KEY_HISTORY, null) ?: return
        try {
            val array = JSONArray(json)
            val list = mutableListOf<ProcessedVideoRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProcessedVideoRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        uriString = obj.getString("uri"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        blurredFrames = obj.optInt("blurredFrames", 0),
                        durationMs = obj.optLong("durationMs", 0L),
                        resolutionLabel = obj.optString("resolutionLabel", "Original")
                    )
                )
            }
            _historyFlow.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addProcessedVideo(
        context: Context,
        uri: Uri,
        blurredFrames: Int,
        durationMs: Long,
        resolutionLabel: String
    ) {
        init(context)
        val newRecord = ProcessedVideoRecord(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            timestamp = System.currentTimeMillis(),
            blurredFrames = blurredFrames,
            durationMs = durationMs,
            resolutionLabel = resolutionLabel
        )

        val updated = (listOf(newRecord) + _historyFlow.value).distinctBy { it.uriString }.take(20)
        _historyFlow.value = updated
        saveHistory(updated)
    }

    fun removeVideo(context: Context, id: String) {
        init(context)
        val updated = _historyFlow.value.filter { it.id != id }
        _historyFlow.value = updated
        saveHistory(updated)
    }

    private fun saveHistory(list: List<ProcessedVideoRecord>) {
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("uri", item.uriString)
                    put("timestamp", item.timestamp)
                    put("blurredFrames", item.blurredFrames)
                    put("durationMs", item.durationMs)
                    put("resolutionLabel", item.resolutionLabel)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString(KEY_HISTORY, array.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
