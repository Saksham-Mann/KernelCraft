package com.kernelcraft.core.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener

/**
 * Real-time decoder and rendering performance metrics.
 */
@Immutable
data class PlayerMetrics(
    val currentFrame: Int = 0,
    val totalFrames: Int = 1200,
    val seekLatencyMs: Long = 0L,
    val droppedFrames: Int = 0,
    val decoderName: String = "Detecting...",
    val isHardwareAccelerated: Boolean = true,
    val isFallbackActive: Boolean = false
)

/**
 * Analytics listener capturing decoder metrics and frame drops for scrub profiling.
 */
@OptIn(UnstableApi::class)
class DecoderTelemetryListener(
    private val onMetricsUpdated: (PlayerMetrics) -> Unit
) : AnalyticsListener {

    private var currentMetrics = PlayerMetrics()
    private var lastSeekDispatchTimeNs: Long = 0L

    fun onSeekDispatched() {
        lastSeekDispatchTimeNs = System.nanoTime()
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long
    ) {
        val isHw = !decoderName.lowercase().contains("sw") && !decoderName.lowercase().contains("google")
        currentMetrics = currentMetrics.copy(
            decoderName = decoderName,
            isHardwareAccelerated = isHw
        )
        onMetricsUpdated(currentMetrics)
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        currentMetrics = currentMetrics.copy(
            droppedFrames = currentMetrics.droppedFrames + droppedFrames
        )
        onMetricsUpdated(currentMetrics)
    }

    override fun onVideoFrameProcessingOffset(
        eventTime: AnalyticsListener.EventTime,
        totalProcessingOffsetUs: Long,
        frameCount: Int
    ) {
        if (lastSeekDispatchTimeNs > 0L) {
            val latencyMs = (System.nanoTime() - lastSeekDispatchTimeNs) / 1_000_000L
            currentMetrics = currentMetrics.copy(seekLatencyMs = latencyMs)
            onMetricsUpdated(currentMetrics)
            lastSeekDispatchTimeNs = 0L
        }
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
        ) {
            currentMetrics = currentMetrics.copy(isFallbackActive = true)
            onMetricsUpdated(currentMetrics)
        }
    }

    fun updatePosition(positionMs: Long, durationMs: Long, fps: Float = 120f) {
        val frame = ((positionMs / 1000f) * fps).toInt().coerceIn(0, 1200)
        currentMetrics = currentMetrics.copy(currentFrame = frame)
        onMetricsUpdated(currentMetrics)
    }
}
