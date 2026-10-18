package com.kernelcraft.feature.teardown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating TeardownMilestone timestamp threshold resolution,
 * progress mapping, and hardware boundary checks.
 */
class TeardownMilestoneTest {

    @Test
    fun `fromPositionMs correctly maps timestamp to ASSEMBLED stage`() {
        assertEquals(TeardownMilestone.ASSEMBLED, TeardownMilestone.fromPositionMs(0L))
        assertEquals(TeardownMilestone.ASSEMBLED, TeardownMilestone.fromPositionMs(500L))
        assertEquals(TeardownMilestone.ASSEMBLED, TeardownMilestone.fromPositionMs(1_499L))
    }

    @Test
    fun `fromPositionMs correctly maps timestamp to BACK_COVER_REMOVED stage`() {
        assertEquals(TeardownMilestone.BACK_COVER_REMOVED, TeardownMilestone.fromPositionMs(1_500L))
        assertEquals(TeardownMilestone.BACK_COVER_REMOVED, TeardownMilestone.fromPositionMs(2_500L))
        assertEquals(TeardownMilestone.BACK_COVER_REMOVED, TeardownMilestone.fromPositionMs(3_199L))
    }

    @Test
    fun `fromPositionMs correctly maps timestamp to EXPLODED_LAYERS stage`() {
        assertEquals(TeardownMilestone.EXPLODED_LAYERS, TeardownMilestone.fromPositionMs(3_200L))
        assertEquals(TeardownMilestone.EXPLODED_LAYERS, TeardownMilestone.fromPositionMs(4_800L)) // Peak explosion frame
        assertEquals(TeardownMilestone.EXPLODED_LAYERS, TeardownMilestone.fromPositionMs(5_199L))
    }

    @Test
    fun `fromPositionMs correctly maps timestamp to CHASSIS_FOCUS stage`() {
        assertEquals(TeardownMilestone.CHASSIS_FOCUS, TeardownMilestone.fromPositionMs(5_200L))
        assertEquals(TeardownMilestone.CHASSIS_FOCUS, TeardownMilestone.fromPositionMs(8_000L))
        assertEquals(TeardownMilestone.CHASSIS_FOCUS, TeardownMilestone.fromPositionMs(10_000L))
    }

    @Test
    fun `fromProgress resolves accurately for full 10-second duration`() {
        val durationMs = 10_000L
        assertEquals(TeardownMilestone.ASSEMBLED, TeardownMilestone.fromProgress(0.10f, durationMs))
        assertEquals(TeardownMilestone.BACK_COVER_REMOVED, TeardownMilestone.fromProgress(0.25f, durationMs))
        assertEquals(TeardownMilestone.EXPLODED_LAYERS, TeardownMilestone.fromProgress(0.48f, durationMs))
        assertEquals(TeardownMilestone.CHASSIS_FOCUS, TeardownMilestone.fromProgress(0.85f, durationMs))
    }

    @Test
    fun `contains check respects exact start and end bounds`() {
        assertTrue(TeardownMilestone.BACK_COVER_REMOVED.contains(1_500L))
        assertTrue(TeardownMilestone.BACK_COVER_REMOVED.contains(3_199L))
        assertFalse(TeardownMilestone.BACK_COVER_REMOVED.contains(3_200L))
        assertFalse(TeardownMilestone.BACK_COVER_REMOVED.contains(1_499L))
    }

    @Test
    fun `peak explosion milestone constant is within exploded layers range`() {
        assertEquals(4_800L, TeardownMilestone.PEAK_EXPLOSION_MS)
        assertTrue(TeardownMilestone.EXPLODED_LAYERS.contains(TeardownMilestone.PEAK_EXPLOSION_MS))
    }
}
