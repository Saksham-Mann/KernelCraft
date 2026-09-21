package com.kernelcraft.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Procedural low-latency PCM audio synthesis engine for KernelCraft.
 *
 * Generates zero-disk-footprint algorithmic audio feedback directly via Android [AudioTrack]:
 * 1. Mechanical Ratchet Click: Micro-clicks pitched dynamically based on scrub velocity (simulating torque dial detents).
 * 2. Layer Separation Whirr: Ascending whoosh sweep when floating component layers explode outward.
 * 3. Reassemble Snap: Descending metallic transient and lock-in click when components reassemble.
 * 4. Silicon Resonance Hum: Looping low-frequency electronic drone modulating with CPU clock speeds.
 */
class TeardownAudioSynthesizer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val RATCHET_THROTTLE_MS = 28L
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    // Pre-synthesized waveform buffers
    private val ratchetBuffer: ShortArray = synthesizeRatchetClick()
    private val explodeBuffer: ShortArray = synthesizeExplodeWhirr()
    private val snapBuffer: ShortArray = synthesizeReassembleSnap()
    private val humBuffer: ShortArray = synthesizeSiliconHum(baseFreqHz = 55f)

    // AudioTrack instances
    private var ratchetTrack: AudioTrack? = null
    private var explodeTrack: AudioTrack? = null
    private var snapTrack: AudioTrack? = null
    private var humTrack: AudioTrack? = null

    private var lastRatchetTimeMs = 0L
    private var isMuted = false
    private var isBackgrounded = false
    private var humModulationJob: Job? = null

    init {
        initTracks()
    }

    private fun initTracks() {
        try {
            ratchetTrack = createStaticTrack(ratchetBuffer)
            explodeTrack = createStaticTrack(explodeBuffer)
            snapTrack = createStaticTrack(snapBuffer)
            humTrack = createLoopingTrack(humBuffer)
        } catch (e: Exception) {
            // Graceful fallback if device audio service is temporarily restricted
            ratchetTrack = null
            explodeTrack = null
            snapTrack = null
            humTrack = null
        }
    }

    private fun createStaticTrack(pcmData: ShortArray): AudioTrack {
        val bufferSizeBytes = pcmData.size * 2
        val track = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSizeBytes,
            AudioTrack.MODE_STATIC,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(pcmData, 0, pcmData.size)
        return track
    }

    private fun createLoopingTrack(pcmData: ShortArray): AudioTrack {
        val bufferSizeBytes = pcmData.size * 2
        val track = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSizeBytes,
            AudioTrack.MODE_STATIC,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(pcmData, 0, pcmData.size)
        track.setLoopPoints(0, pcmData.size, -1) // Infinite loop
        return track
    }

    /**
     * Plays a mechanical ratchet click with pitch and amplitude dynamically modulated by scrub velocity.
     * Throttled to [RATCHET_THROTTLE_MS] to prevent audio buffer queue overflows during 120Hz flings.
     */
    fun playRatchetClick(velocity: Float) {
        if (isMuted || isBackgrounded) return
        val now = System.currentTimeMillis()
        if (now - lastRatchetTimeMs < RATCHET_THROTTLE_MS) return
        lastRatchetTimeMs = now

        ratchetTrack?.let { track ->
            try {
                // Modulate playback rate (pitch): 0.85x up to 1.55x
                val pitchMultiplier = (0.85f + (velocity.coerceIn(0f, 1f) * 0.70f)).coerceIn(0.5f, 2.0f)
                val rate = (SAMPLE_RATE * pitchMultiplier).toInt().coerceIn(4000, 96000)
                track.playbackRate = rate

                // Volume: 0.35f up to 0.85f
                val volume = (0.35f + (velocity.coerceIn(0f, 1f) * 0.50f)).coerceIn(0f, 1f)
                track.setVolume(volume)

                track.stop()
                track.reloadStaticData()
                track.play()
            } catch (_: Exception) {}
        }
    }

    /**
     * Triggers the ascending layer separation whoosh when components float apart.
     */
    fun playLayerExplodeWhirr() {
        if (isMuted || isBackgrounded) return
        explodeTrack?.let { track ->
            try {
                track.stop()
                track.reloadStaticData()
                track.setVolume(0.70f)
                track.play()
            } catch (_: Exception) {}
        }
    }

    /**
     * Triggers the metallic reassemble snap when components collapse back together.
     */
    fun playReassembleSnap() {
        if (isMuted || isBackgrounded) return
        snapTrack?.let { track ->
            try {
                track.stop()
                track.reloadStaticData()
                track.setVolume(0.85f)
                track.play()
            } catch (_: Exception) {}
        }
    }

    /**
     * Starts the looping low-frequency electronic drone during silicon micro-zoom immersion.
     */
    fun startSiliconHum() {
        if (isMuted || isBackgrounded) return
        humTrack?.let { track ->
            try {
                track.setVolume(0.40f)
                track.play()
            } catch (_: Exception) {}
        }
    }

    /**
     * Modulates the silicon hum pitch and intensity based on simulated CPU clock frequency (GHz).
     */
    fun updateSiliconHum(clockSpeedGhz: Float, isThrottled: Boolean) {
        if (isMuted || isBackgrounded) return
        humTrack?.let { track ->
            try {
                // Pitch shifts between 0.85x (idle 1.2GHz) and 1.35x (boost 3.3GHz)
                val normalizedFreq = ((clockSpeedGhz - 1.2f) / (3.3f - 1.2f)).coerceIn(0f, 1f)
                val pitchMultiplier = 0.85f + (normalizedFreq * 0.50f)
                val rate = (SAMPLE_RATE * pitchMultiplier).toInt()
                track.playbackRate = rate

                // Throttling adds subtle volume dip
                val targetVolume = if (isThrottled) 0.25f else 0.45f
                track.setVolume(targetVolume)
            } catch (_: Exception) {}
        }
    }

    /**
     * Stops and resets the silicon resonance drone.
     */
    fun stopSiliconHum() {
        humTrack?.let { track ->
            try {
                track.pause()
                track.reloadStaticData()
            } catch (_: Exception) {}
        }
    }

    /**
     * Toggles global audio muting.
     */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopSiliconHum()
            ratchetTrack?.pause()
            explodeTrack?.pause()
            snapTrack?.pause()
        }
    }

    fun isMuted(): Boolean = isMuted

    /**
     * Pauses all audio playback upon app backgrounding.
     */
    fun onLifecyclePause() {
        isBackgrounded = true
        try {
            ratchetTrack?.pause()
            explodeTrack?.pause()
            snapTrack?.pause()
            humTrack?.pause()
        } catch (_: Exception) {}
    }

    /**
     * Resumes audio capability when app returns to foreground.
     */
    fun onLifecycleResume() {
        isBackgrounded = false
    }

    /**
     * Releases all native AudioTrack instances.
     */
    fun release() {
        humModulationJob?.cancel()
        try {
            ratchetTrack?.release()
            explodeTrack?.release()
            snapTrack?.release()
            humTrack?.release()
        } catch (_: Exception) {}
        ratchetTrack = null
        explodeTrack = null
        snapTrack = null
        humTrack = null
    }

    // --- Procedural PCM Synthesis Algorithms ---

    /**
     * Synthesizes a 14ms mechanical torque click (~2.2kHz decaying impulse).
     */
    private fun synthesizeRatchetClick(): ShortArray {
        val durationSec = 0.014f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val freq = 2200f
        val decayTau = 0.0035f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val envelope = exp(-t / decayTau)
            val sample = sin(2.0 * PI * freq * t) * envelope
            // Second metallic harmonic at 3.8kHz
            val harmonic = sin(2.0 * PI * 3800.0 * t) * (envelope * 0.35)
            val combined = (sample + harmonic) * Short.MAX_VALUE * 0.90
            buffer[i] = combined.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes a 260ms ascending frequency whirr sweep (130Hz -> 650Hz).
     */
    private fun synthesizeExplodeWhirr(): ShortArray {
        val durationSec = 0.260f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val f0 = 130f
        val f1 = 650f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = t / durationSec
            // Linear frequency sweep
            val currentFreq = f0 + (f1 - f0) * progress
            // Smooth cosine attack & decay envelope
            val envelope = sin(PI * progress)
            val sample = sin(2.0 * PI * currentFreq * t) * envelope * Short.MAX_VALUE * 0.75
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes a 70ms descending metallic snap (450Hz -> 160Hz + impact click).
     */
    private fun synthesizeReassembleSnap(): ShortArray {
        val durationSec = 0.070f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = t / durationSec
            val freq = 450f - (290f * progress)
            val envelope = exp(-t / 0.015f)
            val tone = sin(2.0 * PI * freq * t) * envelope
            // High-frequency transient impact click at start
            val click = if (i < 80) sin(2.0 * PI * 2800.0 * t) * 0.6 else 0.0
            val combined = (tone + click) * Short.MAX_VALUE * 0.85
            buffer[i] = combined.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes a 1.0-second seamless looping electronic drone (55Hz fundamental + harmonics).
     */
    private fun synthesizeSiliconHum(baseFreqHz: Float): ShortArray {
        val durationSec = 1.0f
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            // 55Hz fundamental
            val f0 = sin(2.0 * PI * baseFreqHz * t)
            // 110Hz second harmonic
            val f1 = sin(2.0 * PI * (baseFreqHz * 2.0) * t) * 0.40
            // 165Hz third harmonic
            val f2 = sin(2.0 * PI * (baseFreqHz * 3.0) * t) * 0.20

            val combined = (f0 + f1 + f2) * (Short.MAX_VALUE * 0.35)
            buffer[i] = combined.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
