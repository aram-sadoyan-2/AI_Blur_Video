package com.naiyados.aiblurvideo

import com.naiyados.aiblurvideo.autoplate.export.ExportBitrate
import com.naiyados.aiblurvideo.autoplate.export.ExportResolution
import com.naiyados.aiblurvideo.autoplate.export.ExportSettings
import com.naiyados.aiblurvideo.queue.BatchQueueManager
import com.naiyados.aiblurvideo.queue.QueueItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BatchQueueManagerTest {

    @Test
    fun testExportSettingsDefaults() {
        val settings = ExportSettings()
        assertEquals(ExportResolution.ORIGINAL, settings.resolution)
        assertEquals(ExportBitrate.STANDARD, settings.bitrate)
    }

    @Test
    fun testQueueItemStatusValues() {
        assertEquals("CANCELLED", QueueItemStatus.CANCELLED.name)
        assertEquals("SCANNING_AI", QueueItemStatus.SCANNING_AI.name)
        assertEquals("BLURRING_EXPORT", QueueItemStatus.BLURRING_EXPORT.name)
    }

    @Test
    fun testStopProcessingClearsProcessingState() {
        BatchQueueManager.stopProcessing()
        assertFalse(BatchQueueManager.isProcessing.value)
    }
}
