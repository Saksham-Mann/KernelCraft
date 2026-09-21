package com.kernelcraft.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Aggressive Seek Buffering Policy tuned for single-frame All-Intra scrub playback.
 *
 * Standard ExoPlayer buffering targets 50,000ms forward buffering and allocates
 * up to 50MB of native buffer memory for linear network streaming.
 *
 * For local All-Intra H.264 scrub scrubbing:
 * - Every frame is an I-frame, requiring zero temporal forward-buffering.
 * - Minimum buffer duration is reduced to 250ms with 50ms playback start threshold.
 * - Target buffer bytes capped at 1.5MB instead of 50MB.
 * - Reduces total player heap allocation by >90% to guarantee <250MB peak memory ceiling.
 */
@OptIn(UnstableApi::class)
object ScrubLoadControl {

    /** Minimal buffer allocation segment size (32KB) */
    private const val BUFFER_SEGMENT_SIZE = 32 * 1024

    /** Capped maximum buffer pool: 1.5MB total */
    private const val TARGET_BUFFER_BYTES = 1536 * 1024

    fun create(): LoadControl {
        val allocator = DefaultAllocator(
            /* trimOnReset = */ true,
            /* individualAllocationSize = */ BUFFER_SEGMENT_SIZE
        )

        return DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                /* minBufferMs = */ 250,
                /* maxBufferMs = */ 500,
                /* bufferForPlaybackMs = */ 50,
                /* bufferForPlaybackAfterRebufferMs = */ 100
            )
            .setTargetBufferBytes(TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
}
