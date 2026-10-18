package com.kernelcraft.feature.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating OS tier ordering, Schedutil CPU Governor frequency math,
 * thermal throttling clamping logic, and Binder simulation states.
 */
class KernelStackModelTest {

    @Test
    fun `OsTier ordering matches bottom-up physical silicon to userspace hierarchy`() {
        assertEquals(0, OsTier.PHYSICAL_SILICON.tierIndex)
        assertEquals(1, OsTier.LINUX_KERNEL.tierIndex)
        assertEquals(2, OsTier.HAL_LAYER.tierIndex)
        assertEquals(3, OsTier.ART_RUNTIME.tierIndex)
        assertEquals(4, OsTier.USERSPACE_FRAMEWORK.tierIndex)
    }

    @Test
    fun `OsTier fromIndex handles out of bounds gracefully`() {
        assertEquals(OsTier.PHYSICAL_SILICON, OsTier.fromIndex(0))
        assertEquals(OsTier.USERSPACE_FRAMEWORK, OsTier.fromIndex(4))
        // Clamped bounds
        assertEquals(OsTier.PHYSICAL_SILICON, OsTier.fromIndex(-1))
        assertEquals(OsTier.USERSPACE_FRAMEWORK, OsTier.fromIndex(100))
    }

    @Test
    fun `CpuGovernorState PERFORMANCE mode locks cores to maximum frequency`() {
        val cores = CpuGovernorState.defaultCores(CpuGovernorMode.PERFORMANCE, 38.0f)
        val primeCore = cores.first { it.clusterType == ClusterType.PRIME }

        assertEquals(3.30f, primeCore.maxFreqGhz, 0.01f)
        assertEquals(3.30f, primeCore.currentFreqGhz, 0.01f)
        assertEquals(1.0f, primeCore.normalizedFrequency, 0.01f)
    }

    @Test
    fun `CpuGovernorState POWERSAVE mode locks cores to minimum frequency`() {
        val cores = CpuGovernorState.defaultCores(CpuGovernorMode.POWERSAVE, 38.0f)
        val primeCore = cores.first { it.clusterType == ClusterType.PRIME }

        assertEquals(0.80f, primeCore.minFreqGhz, 0.01f)
        assertEquals(0.80f, primeCore.currentFreqGhz, 0.01f)
        assertEquals(0.0f, primeCore.normalizedFrequency, 0.01f)
    }

    @Test
    fun `Thermal throttling over 48 Celsius clamps Prime Core maximum frequency`() {
        val nominalCores = CpuGovernorState.defaultCores(CpuGovernorMode.PERFORMANCE, 38.0f)
        val throttledCores = CpuGovernorState.defaultCores(CpuGovernorMode.PERFORMANCE, 50.0f)

        val nominalPrime = nominalCores.first { it.clusterType == ClusterType.PRIME }
        val throttledPrime = throttledCores.first { it.clusterType == ClusterType.PRIME }

        assertEquals(3.30f, nominalPrime.maxFreqGhz, 0.01f)
        // 3.30 * 0.65 = 2.145 GHz
        assertEquals(2.145f, throttledPrime.maxFreqGhz, 0.01f)
        assertTrue(throttledPrime.maxFreqGhz < nominalPrime.maxFreqGhz)
    }

    @Test
    fun `TierRepository contains structured subsystems for all five tiers`() {
        OsTier.entries.forEach { tier ->
            val subsystems = TierRepository.getSubsystems(tier)
            assertTrue("Subsystems for $tier should not be empty", subsystems.isNotEmpty())
            subsystems.forEach { subsystem ->
                assertTrue(subsystem.title.isNotBlank())
                assertTrue(subsystem.metricLabel.isNotBlank())
                assertTrue(subsystem.metricValue.isNotBlank())
            }
        }
    }

    @Test
    fun `BinderSimulationState initial state is Idle with valid transaction count`() {
        val state = BinderSimulationState()
        assertEquals(BinderTransactionPhase.IDLE, state.phase)
        assertTrue(state.transactionCount > 0)
        assertTrue(state.clientProcess.contains("com.kernelcraft"))
        assertTrue(state.targetProcess.contains("system_server"))
    }
}
