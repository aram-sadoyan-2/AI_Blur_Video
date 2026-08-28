package com.naiyados.aiblurvideo.autoplate.export

enum class ExportResolution(
    val label: String,
    val maxDimension: Int?,
    val description: String
) {
    ORIGINAL("Original", null, "Match source video resolution"),
    FHD_1080P("1080p FHD", 1080, "Max 1080p for high definition"),
    HD_720P("720p HD", 720, "Max 720p for fast export & moderate size"),
    SD_480P("480p SD", 480, "Max 480p for smallest compact file size")
}

enum class ExportBitrate(
    val label: String,
    val bps: Int,
    val description: String
) {
    ULTRA("Ultra (24 Mbps)", 24_000_000, "Studio-grade pristine fidelity"),
    HIGH("High (16 Mbps)", 16_000_000, "Crystal clear HD, outstanding detail"),
    STANDARD("Standard (12 Mbps)", 12_000_000, "Recommended crisp high-quality export"),
    MEDIUM("Medium (8 Mbps)", 8_000_000, "Balanced compression & file size"),
    LOW("Compact (4 Mbps)", 4_000_000, "Smaller file size for quick sharing")
}

data class ExportSettings(
    val resolution: ExportResolution = ExportResolution.ORIGINAL,
    val bitrate: ExportBitrate = ExportBitrate.STANDARD
) {
    fun estimateMegabytesPerMinute(): Float {
        // Bitrate in bits per second -> bytes per minute = (bps * 60) / (8 * 1024 * 1024)
        return (bitrate.bps.toFloat() * 60f) / (8f * 1024f * 1024f)
    }

    fun calculateOutputDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        val maxDim = resolution.maxDimension
        if (maxDim == null) {
            val w = (sourceWidth / 2) * 2
            val h = (sourceHeight / 2) * 2
            return Pair(maxOf(w, 16), maxOf(h, 16))
        }

        val minSide = minOf(sourceWidth, sourceHeight)
        if (minSide <= maxDim) {
            val w = (sourceWidth / 2) * 2
            val h = (sourceHeight / 2) * 2
            return Pair(maxOf(w, 16), maxOf(h, 16))
        }

        val scale = maxDim.toFloat() / minSide.toFloat()
        var targetW = (sourceWidth * scale).toInt()
        var targetH = (sourceHeight * scale).toInt()
        targetW = (targetW / 2) * 2
        targetH = (targetH / 2) * 2
        return Pair(maxOf(targetW, 16), maxOf(targetH, 16))
    }
}
