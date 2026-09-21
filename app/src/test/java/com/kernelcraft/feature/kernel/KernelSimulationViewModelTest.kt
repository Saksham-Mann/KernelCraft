package com.kernelcraft.feature.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for KernelSimulationViewModel state transitions, governor policies,
 * memory simulation actions, and runqueue core tracks.
 */
class KernelSimulationViewModelTest {

    @Test
    fun `initial state initializes with 3 CPU core tracks and 32 memory blocks`() {
        val vm = KernelSimulationViewModel()
        val state = vm.uiState.value

        assertEquals(3, state.coreTracks.size)
        assertEquals(32, state.memoryBlocks.size)
        assertEquals(CpuGovernorMode.SCHEDUTIL, state.governorMode)
        assertFalse(state.isThermalThrottling)

        val primeCore = state.coreTracks.first { it.clusterType == ClusterType.PRIME }
        val effCore = state.coreTracks.first { it.clusterType == ClusterType.EFFICIENCY }

        assertTrue(primeCore.maxFreqGhz > effCore.maxFreqGhz)
    }

    @Test
    fun `setGovernor PERFORMANCE locks core frequency to max rated speed`() {
        val vm = KernelSimulationViewModel()
        vm.setGovernor(CpuGovernorMode.PERFORMANCE)
        val state = vm.uiState.value

        assertEquals(CpuGovernorMode.PERFORMANCE, state.governorMode)
        val prime = state.coreTracks.first { it.clusterType == ClusterType.PRIME }
        assertEquals(prime.maxFreqGhz, prime.currentFreqGhz, 0.01f)
    }

    @Test
    fun `setGovernor POWERSAVE locks core frequency to minimum clock speed`() {
        val vm = KernelSimulationViewModel()
        vm.setGovernor(CpuGovernorMode.POWERSAVE)
        val state = vm.uiState.value

        assertEquals(CpuGovernorMode.POWERSAVE, state.governorMode)
        val prime = state.coreTracks.first { it.clusterType == ClusterType.PRIME }
        assertEquals(0.80f, prime.currentFreqGhz, 0.01f)
    }

    @Test
    fun `thermal throttling over 45 Celsius clamps Prime clock speed`() {
        val vm = KernelSimulationViewModel()
        vm.setTemperature(50.0f)
        val state = vm.uiState.value

        assertTrue(state.isThermalThrottling)
        val prime = state.coreTracks.first { it.clusterType == ClusterType.PRIME }
        assertTrue(prime.maxFreqGhz < 3.0f)
    }

    @Test
    fun `simulateAppLaunch expands userspace memory and flags allocated blocks`() {
        val vm = KernelSimulationViewModel()
        val initialUserspace = vm.uiState.value.userspaceUsedMb
        vm.simulateAppLaunch()
        val updated = vm.uiState.value

        assertTrue(updated.userspaceUsedMb > initialUserspace)
        val allocatedUserspace = updated.memoryBlocks.filter { !it.isKernelSpace && it.isAllocated }
        assertTrue(allocatedUserspace.size >= 12)
    }

    @Test
    fun `simulateHeavyWorkload increases zRAM swap usage and compression ratio`() {
        val vm = KernelSimulationViewModel()
        vm.simulateHeavyWorkload()
        val state = vm.uiState.value

        assertTrue(state.zramCompressedMb > 700)
        assertTrue(state.memoryBlocks.any { it.isZramSwapped })
    }

    @Test
    fun `simulateMemoryTrim reduces memory pressure and frees cold pages`() {
        val vm = KernelSimulationViewModel()
        vm.simulateHeavyWorkload()
        vm.simulateMemoryTrim()
        val state = vm.uiState.value

        assertEquals(2350, state.userspaceUsedMb)
        assertEquals(480, state.zramCompressedMb)
    }

    @Test
    fun `selectProcessSlice sets inspection target`() {
        val vm = KernelSimulationViewModel()
        val slice = ProcessSlice(
            id = "test_slice",
            processName = "test.process",
            threadTag = "Test Thread",
            niceValue = -5,
            durationMs = 3.5f,
            vruntimeDeltaUs = 150,
            clusterType = ClusterType.PRIME,
            startOffsetRatio = 0.2f,
            durationRatio = 0.3f
        )

        vm.selectProcessSlice(slice)
        assertEquals("test_slice", vm.uiState.value.selectedSlice?.id)

        vm.selectProcessSlice(null)
        assertEquals(null, vm.uiState.value.selectedSlice)
    }
}
