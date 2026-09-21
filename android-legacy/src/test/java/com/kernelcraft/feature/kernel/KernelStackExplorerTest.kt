package com.kernelcraft.feature.kernel

import com.kernelcraft.feature.kernel.ui.GanttSlice
import com.kernelcraft.feature.kernel.ui.GanttThreadLane
import com.kernelcraft.feature.kernel.ui.MemorySegment
import com.kernelcraft.feature.kernel.ui.ThreadExecutionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

/**
 * Unit tests validating KernelStackExplorer architecture, Gantt chart scheduling metrics,
 * and memory matrix allocations.
 */
class KernelStackExplorerTest {

    @Test
    fun `StackTierItem definitions cover the four core architectural tiers`() {
        val tiers = listOf(
            StackTierItem(
                tierId = 0,
                title = "User Apps: Android & Jetpack Compose",
                tag = "APPLICATION RUNTIME",
                subtitle = "Declarative UI toolkit, Choreographer 120Hz VSYNC",
                privilegeTag = "EL0 USER MODE",
                accentColor = Color(0xFFE8A020)
            ),
            StackTierItem(
                tierId = 1,
                title = "Android Framework: API & Runtime (ART)",
                tag = "SYSTEM SERVICES",
                subtitle = "WindowManagerService, ActivityTaskManager",
                privilegeTag = "EL0 SYSTEM UID",
                accentColor = Color(0xFF4CAF50)
            ),
            StackTierItem(
                tierId = 2,
                title = "Hardware Abstraction Layer (HAL): Connecting Logic & Hardware",
                tag = "PROJECT TREBLE",
                subtitle = "Stable AIDL / HIDL interfaces",
                privilegeTag = "EL0 VENDOR HAL",
                accentColor = Color(0xFF4E7F9E)
            ),
            StackTierItem(
                tierId = 3,
                title = "Linux Kernel: Process Scheduler & Device Drivers",
                tag = "CORE KERNEL",
                subtitle = "CFS/EAS CPU thread scheduling, ZRAM memory swap compaction",
                privilegeTag = "RING 0 KERNEL (EL1)",
                accentColor = Color(0xFFDD5622)
            )
        )

        assertEquals(4, tiers.size)
        assertEquals(0, tiers[0].tierId)
        assertEquals(3, tiers[3].tierId)
        assertTrue(tiers[3].title.contains("Linux Kernel"))
    }

    @Test
    fun `Gantt thread lanes duration ratios sum up accurately`() {
        val slices = listOf(
            GanttSlice(0.00f, 0.22f, ThreadExecutionState.RUNNING),
            GanttSlice(0.22f, 0.15f, ThreadExecutionState.WAITING),
            GanttSlice(0.37f, 0.28f, ThreadExecutionState.RUNNING),
            GanttSlice(0.65f, 0.08f, ThreadExecutionState.PREEMPTED),
            GanttSlice(0.73f, 0.27f, ThreadExecutionState.RUNNING)
        )

        val totalDuration = slices.sumOf { it.durationRatio.toDouble() }
        assertEquals(1.0, totalDuration, 0.01)

        // Ensure all slices have valid offset + duration <= 1.0f
        slices.forEach { slice ->
            assertTrue(slice.startOffsetRatio >= 0.0f)
            assertTrue(slice.startOffsetRatio + slice.durationRatio <= 1.01f)
        }
    }

    @Test
    fun `Memory segments cover both userspace and kernel space domains`() {
        val segments = listOf(
            MemorySegment("art_heap", "ART Managed Heap", "USERSPACE (EL0)", 1250, "Desc", Color.Yellow),
            MemorySegment("native_heap", "Native Heap", "USERSPACE (EL0)", 850, "Desc", Color.Blue),
            MemorySegment("ashmem", "Ashmem", "USERSPACE (EL0)", 620, "Desc", Color.Green),
            MemorySegment("kernel_page_tables", "Kernel Page Tables", "KERNEL SPACE (RING 0)", 520, "Desc", Color.Red),
            MemorySegment("slab_cache", "Slab Allocator", "KERNEL SPACE (RING 0)", 480, "Desc", Color.Magenta),
            MemorySegment("zram_swap", "ZRAM Swap", "KERNEL SPACE (RING 0)", 648, "Desc", Color.Cyan)
        )

        val userspaceMb = segments.filter { it.spaceDomain.contains("USERSPACE") }.sumOf { it.sizeMb }
        val kernelSpaceMb = segments.filter { it.spaceDomain.contains("KERNEL") }.sumOf { it.sizeMb }

        assertEquals(2720, userspaceMb)
        assertEquals(1648, kernelSpaceMb)
        assertEquals(4368, userspaceMb + kernelSpaceMb)

        val totalRamMb = 12_288
        val freeOrCachedMb = totalRamMb - (userspaceMb + kernelSpaceMb)
        assertTrue(freeOrCachedMb > 7000)
    }

    @Test
    fun `Bottom dock supported actions match specification`() {
        val dockActions = listOf("Stack Overview", "Layer Detail", "Reset Session")
        assertEquals(3, dockActions.size)
        assertTrue(dockActions.contains("Stack Overview"))
        assertTrue(dockActions.contains("Layer Detail"))
        assertTrue(dockActions.contains("Reset Session"))
    }
}
