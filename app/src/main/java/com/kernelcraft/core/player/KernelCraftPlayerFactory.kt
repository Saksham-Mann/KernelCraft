package com.kernelcraft.core.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer

/**
 * Factory for creating an ultra-low-latency, video-only ExoPlayer instance
 * configured specifically for local All-Intra H.264 scrub scrubbing.
 *
 * Architectural optimizations:
 * 1. Audio-stripped RenderersFactory: Zero AudioTrack allocation, zero audio-sync thread overhead.
 * 2. Tuned ScrubLoadControl: Caps native buffer pools to 1.5MB instead of 50MB.
 * 3. Exact seek parameter: Takes advantage of All-Intra GOP-1 encoding for instantaneous seek latency.
 * 4. Surface decoupling: Prevents MediaCodec.CodecException on screen rotation or background transitions.
 */
@OptIn(UnstableApi::class)
object KernelCraftPlayerFactory {

    fun createScrubPlayer(
        context: Context,
        telemetryListener: DecoderTelemetryListener? = null
    ): ExoPlayer {
        // Video-only renderer factory: Completely bypasses audio pipeline allocation
        val videoOnlyRenderersFactory = RenderersFactory { handler: Handler, videoListener, _, _, _ ->
            arrayOf(
                MediaCodecVideoRenderer(
                    context,
                    MediaCodecSelector.DEFAULT,
                    /* allowedVideoJoiningTimeMs = */ 0L,
                    /* enableDecoderFallback = */ true,
                    handler,
                    videoListener,
                    /* maxDroppedFramesToNotify = */ 50
                )
            )
        }

        val player = ExoPlayer.Builder(context, videoOnlyRenderersFactory)
            .setLoadControl(ScrubLoadControl.create())
            .setSeekParameters(SeekParameters.EXACT)
            .setLooper(Looper.getMainLooper())
            .build()
            .apply {
                playWhenReady = false // Strictly headless & scrub-driven
            }

        telemetryListener?.let {
            player.addAnalyticsListener(it)
        }

        return player
    }

    /**
     * Enforces scheme validation to prevent cleartext HTTP media exfiltration or SSRF.
     * Rejects insecure http:// schemes with [SecurityException].
     */
    fun validateMediaUri(uri: Uri): Uri {
        if (uri == Uri.EMPTY) return uri
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http") {
            throw SecurityException("Insecure cleartext HTTP scheme is prohibited for media playback: $uri")
        }
        val allowedSchemes = setOf("https", "android.resource", "rawresource", "file", "content")
        if (scheme != null && scheme !in allowedSchemes) {
            throw SecurityException("Unauthorized media URI scheme '$scheme'. Permitted schemes: $allowedSchemes")
        }
        return uri
    }

    /**
     * Resolves the target media item from raw resource ID, direct Uri, or fallback identifier.
     */
    fun buildMediaItem(
        context: Context,
        videoRawResId: Int? = null,
        videoUri: Uri? = null,
        fallbackResourceName: String = "teardown_chip01"
    ): MediaItem {
        val targetUri = when {
            videoRawResId != null -> RawResourceDataSource.buildRawResourceUri(videoRawResId)
            videoUri != null -> videoUri
            else -> {
                val resId = context.resources.getIdentifier(fallbackResourceName, "raw", context.packageName)
                if (resId != 0) {
                    RawResourceDataSource.buildRawResourceUri(resId)
                } else {
                    Uri.EMPTY
                }
            }
        }
        val validatedUri = validateMediaUri(targetUri)
        return MediaItem.fromUri(validatedUri)
    }

    /**
     * Cleanly flushes the MediaCodec surface to avoid native crashes on backgrounding or rotation.
     */
    fun flushAndClearSurface(player: ExoPlayer) {
        player.pause()
        player.clearVideoSurface()
    }
}
