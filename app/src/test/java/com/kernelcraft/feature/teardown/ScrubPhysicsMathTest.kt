package com.kernelcraft.feature.teardown

import com.kernelcraft.feature.teardown.annotation.calculateFitBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating scroll-to-seek mathematics, thumb drag inversion,
 * boundary clamping, and aspect-ratio letterbox calculations.
 */
class ScrubPhysicsMathTest {

    @Test
    fun `upward thumb drag advances scrub progress`() {
        val currentProgress = 0.50f
        val dragDeltaY = -250f // Upward drag (negative delta in screen coordinates)
        val totalScrollDistancePx = 2500f // 2.5x screen height

        val deltaProgress = -dragDeltaY / totalScrollDistancePx
        val newProgress = (currentProgress + deltaProgress).coerceIn(0f, 1f)

        assertEquals(0.60f, newProgress, 0.001f)
        assertTrue(newProgress > currentProgress)
    }

    @Test
    fun `downward thumb drag reverses scrub progress`() {
        val currentProgress = 0.50f
        val dragDeltaY = 250f // Downward drag (positive delta in screen coordinates)
        val totalScrollDistancePx = 2500f

        val deltaProgress = -dragDeltaY / totalScrollDistancePx
        val newProgress = (currentProgress + deltaProgress).coerceIn(0f, 1f)

        assertEquals(0.40f, newProgress, 0.001f)
        assertTrue(newProgress < currentProgress)
    }

    @Test
    fun `scrub progress is strictly clamped within 0 and 1`() {
        val totalScrollDistancePx = 1000f

        // Massive upward drag past end
        val progressOver = (0.90f + (-2000f / -totalScrollDistancePx)).coerceIn(0f, 1f)
        assertEquals(1.0f, progressOver, 0.0001f)

        // Massive downward drag past start
        val progressUnder = (0.10f + (-2000f / totalScrollDistancePx)).coerceIn(0f, 1f)
        assertEquals(0.0f, progressUnder, 0.0001f)
    }

    @Test
    fun `seek timestamp calculation strictly clamps within duration bounds`() {
        val durationMs = 10_000L

        val positionAtZero = (0.0f * durationMs).toLong().coerceIn(0L, durationMs)
        assertEquals(0L, positionAtZero)

        val positionAtHalf = (0.5f * durationMs).toLong().coerceIn(0L, durationMs)
        assertEquals(5_000L, positionAtHalf)

        val positionAtEnd = (1.0f * durationMs).toLong().coerceIn(0L, durationMs)
        assertEquals(10_000L, positionAtEnd)

        val positionNegative = (-0.2f * durationMs).toLong().coerceIn(0L, durationMs)
        assertEquals(0L, positionNegative)

        val positionOverflow = (1.5f * durationMs).toLong().coerceIn(0L, durationMs)
        assertEquals(10_000L, positionOverflow)
    }

    @Test
    fun `calculateFitBounds accurately letterboxes tall phone screens`() {
        // Container: 1080 x 2400 (aspect 9:20, taller than 9:16)
        val bounds = calculateFitBounds(1080f, 2400f, 9f / 16f)

        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(1080f, bounds.width, 0.001f)

        // Expected height = 1080 * (16 / 9) = 1920
        assertEquals(1920f, bounds.height, 0.001f)

        // Expected top letterbox = (2400 - 1920) / 2 = 240
        assertEquals(240f, bounds.top, 0.001f)
    }

    @Test
    fun `calculateFitBounds accurately pillarboxes tablet or foldable screens`() {
        // Container: 2000 x 1600 (wider than 9:16)
        val bounds = calculateFitBounds(2000f, 1600f, 9f / 16f)

        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(1600f, bounds.height, 0.001f)

        // Expected width = 1600 * (9 / 16) = 900
        assertEquals(900f, bounds.width, 0.001f)

        // Expected left pillarbox = (2000 - 900) / 2 = 550
        assertEquals(550f, bounds.left, 0.001f)
    }
}
