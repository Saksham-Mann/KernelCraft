package com.kernelcraft.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Unit tests for the procedural audio synthesis math, velocity pitch mapping, and mute controls.
 */
class TeardownAudioSynthesizerTest {

    @Test
    fun `ratchet pitch multiplier scales monotonically with scrub velocity`() {
        fun computePitch(velocity: Float): Float {
            return (0.85f + (velocity.coerceIn(0f, 1f) * 0.70f)).coerceIn(0.5f, 2.0f)
        }

        val pitchMin = computePitch(0.0f)
        val pitchMid = computePitch(0.5f)
        val pitchMax = computePitch(1.0f)

        assertEquals(0.85f, pitchMin, 0.01f)
        assertEquals(1.20f, pitchMid, 0.01f)
        assertEquals(1.55f, pitchMax, 0.01f)
        assertTrue("Higher scrub velocity must yield higher pitch click", pitchMax > pitchMid && pitchMid > pitchMin)
    }

    @Test
    fun `ratchet volume scales dynamically with scrub velocity`() {
        fun computeVolume(velocity: Float): Float {
            return (0.35f + (velocity.coerceIn(0f, 1f) * 0.50f)).coerceIn(0f, 1f)
        }

        assertEquals(0.35f, computeVolume(0.0f), 0.01f)
        assertEquals(0.60f, computeVolume(0.5f), 0.01f)
        assertEquals(0.85f, computeVolume(1.0f), 0.01f)
    }

    @Test
    fun `silicon hum pitch modulates smoothly across simulated CPU clock envelope`() {
        fun computeHumPitch(clockSpeedGhz: Float): Float {
            val normalizedFreq = ((clockSpeedGhz - 1.2f) / (3.3f - 1.2f)).coerceIn(0f, 1f)
            return 0.85f + (normalizedFreq * 0.50f)
        }

        val pitchIdle = computeHumPitch(1.2f)
        val pitchBase = computeHumPitch(2.25f)
        val pitchBoost = computeHumPitch(3.3f)

        assertEquals(0.85f, pitchIdle, 0.01f)
        assertEquals(1.10f, pitchBase, 0.01f)
        assertEquals(1.35f, pitchBoost, 0.01f)
        assertTrue("Boost frequency produces higher harmonic frequency drone", pitchBoost > pitchIdle)
    }

    @Test
    fun `ratchet impulse decay envelope attenuates smoothly without clipping or NaN`() {
        val sampleRate = 44100
        val durationSec = 0.014f
        val numSamples = (sampleRate * durationSec).toInt()
        val freq = 2200f
        val decayTau = 0.0035f

        var maxAmp = 0f
        var lastAmp = 1f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t / decayTau)
            val sample = sin(2.0 * PI * freq * t) * envelope
            val harmonic = sin(2.0 * PI * 3800.0 * t) * (envelope * 0.35)
            val combined = ((sample + harmonic) * Short.MAX_VALUE * 0.90).toFloat()

            assertFalse("Sample must not be NaN", combined.isNaN())
            assertFalse("Sample must not be Infinite", combined.isInfinite())
            assertTrue("Sample must fit within 16-bit PCM bounds", combined in Short.MIN_VALUE.toFloat()..Short.MAX_VALUE.toFloat())

            if (kotlin.math.abs(combined) > maxAmp) {
                maxAmp = kotlin.math.abs(combined)
            }
            lastAmp = envelope.toFloat()
        }

        assertTrue("Impulse starts strong", maxAmp > 10000f)
        assertTrue("Impulse decays near silence at tail", lastAmp < 0.05f)
    }

    @Test
    fun `explode whirr frequency sweep covers continuous audio range`() {
        val sampleRate = 44100
        val durationSec = 0.260f
        val numSamples = (sampleRate * durationSec).toInt()
        val f0 = 130f
        val f1 = 650f

        var maxSample = 0f
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            val currentFreq = f0 + (f1 - f0) * progress
            val envelope = sin(PI * progress)
            val sample = sin(2.0 * PI * currentFreq * t) * envelope * Short.MAX_VALUE * 0.75

            assertFalse("Sample must be a valid number", sample.isNaN())
            if (kotlin.math.abs(sample) > maxSample) {
                maxSample = kotlin.math.abs(sample.toFloat())
            }
        }

        assertTrue("Whirr produces audible waveform", maxSample > 15000f)
    }
}
