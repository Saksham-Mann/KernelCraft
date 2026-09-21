package com.kernelcraft.feature.transition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests validating SoCTransitionLayout transformation math,
 * camera scale equations, pivot anchors, and microscope vignette alpha curves.
 */
class SoCTransitionMathTest {

    @Test
    fun `SoC anchor coordinates match peak explosion normalized position`() {
        assertEquals(0.46f, SOC_ANCHOR_X, 0.001f)
        assertEquals(0.32f, SOC_ANCHOR_Y, 0.001f)
    }

    @Test
    fun `board scale transitions from 1_0x to 8_0x over progress 0 to 1`() {
        fun computeBoardScale(t: Float) = 1.0f + (t * 7.0f)

        assertEquals(1.0f, computeBoardScale(0.0f), 0.001f)
        assertEquals(4.5f, computeBoardScale(0.5f), 0.001f)
        assertEquals(8.0f, computeBoardScale(1.0f), 0.001f)
    }

    @Test
    fun `board alpha fades out completely as progress reaches 0_625`() {
        fun computeBoardAlpha(t: Float) = (1f - (t * 1.6f)).coerceIn(0f, 1f)

        assertEquals(1.0f, computeBoardAlpha(0.0f), 0.001f)
        assertEquals(0.2f, computeBoardAlpha(0.5f), 0.001f)
        assertEquals(0.0f, computeBoardAlpha(0.625f), 0.001f)
        assertEquals(0.0f, computeBoardAlpha(1.0f), 0.001f)
    }

    @Test
    fun `microscope vignette alpha envelope peaks at mid-zoom and zero at boundaries`() {
        fun computeVignetteAlpha(t: Float): Float {
            return if (t in 0.20f..0.85f) {
                (1f - (abs(t - 0.50f) / 0.35f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

        assertEquals(0.0f, computeVignetteAlpha(0.0f), 0.001f)
        assertEquals(0.0f, computeVignetteAlpha(0.15f), 0.001f)
        assertEquals(1.0f, computeVignetteAlpha(0.50f), 0.001f) // Peak speed lines
        assertTrue(computeVignetteAlpha(0.35f) > 0.5f)
        assertTrue(computeVignetteAlpha(0.65f) > 0.5f)
        assertEquals(0.0f, computeVignetteAlpha(0.90f), 0.001f)
    }

    @Test
    fun `stack explorer scale settles from 0_85x into 1_0x`() {
        fun computeStackScale(t: Float) = 0.85f + (0.15f * t)
        fun computeStackAlpha(t: Float) = ((t - 0.35f) / 0.55f).coerceIn(0f, 1f)

        assertEquals(0.85f, computeStackScale(0.0f), 0.001f)
        assertEquals(1.00f, computeStackScale(1.0f), 0.001f)

        assertEquals(0.0f, computeStackAlpha(0.0f), 0.001f)
        assertEquals(0.0f, computeStackAlpha(0.35f), 0.001f)
        assertEquals(1.0f, computeStackAlpha(0.90f), 0.001f)
        assertEquals(1.0f, computeStackAlpha(1.0f), 0.001f)
    }
}
