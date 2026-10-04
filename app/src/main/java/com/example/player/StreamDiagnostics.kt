package com.example.player

data class StreamDiagnostics(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val resolution: String = "—",
    val fps: Int = 0,
    val videoCodec: String = "—",
    val audioCodec: String = "—",
    val bitrateKbps: Long = 0L,
    val bufferMs: Long = 0L,
    val bufferPercentage: Int = 0,
    val protocol: String = "—",
    val latencyMs: Long = 0L,
    val downloadSpeedKbps: Long = 0L,
    val droppedFrames: Int = 0,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isLive: Boolean = true,
    val errorMessage: String? = null
)
