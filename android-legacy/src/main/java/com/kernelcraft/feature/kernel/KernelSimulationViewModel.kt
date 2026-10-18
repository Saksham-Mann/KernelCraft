package com.kernelcraft.feature.kernel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Individual process execution time-slice in the CFS/EAS scheduler runqueue.
 */
@Immutable
data class ProcessSlice(
    val id: String,
    val processName: String,
    val threadTag: String,
    val niceValue: Int, // e.g. -10 for UI Thread, 0 for normal, 19 for idle
    val durationMs: Float,
    val vruntimeDeltaUs: Long,
    val clusterType: ClusterType,
    val startOffsetRatio: Float, // 0.0f..1.0f position in current window
    val durationRatio: Float
)

/**
 * Live scheduler track for an individual CPU core.
 */
@Immutable
data class CoreTrack(
    val coreIndex: Int,
    val name: String,
    val clusterType: ClusterType,
    val currentFreqGhz: Float,
    val maxFreqGhz: Float,
    val utilizationPct: Int,
    val activeSlices: List<ProcessSlice>
)

/**
 * Individual memory page block in the 32-block virtual/physical memory matrix.
 */
@Immutable
data class MemoryPageBlock(
    val index: Int,
    val isKernelSpace: Boolean,
    val label: String,
    val isAllocated: Boolean,
    val isFlashing: Boolean = false,
    val isZramSwapped: Boolean = false
)

/**
 * Framework Binder IPC Tracer state.
 */
@Immutable
data class BinderTracerState(
    val phase: BinderTransactionPhase = BinderTransactionPhase.IDLE,
    val inFlightProgress: Float = 0f,
    val transactionCount: Int = 2410,
    val latencyMicros: Int = 142,
    val activePayload: String = "android.view.IWindowSession::relayoutWindow",
    val clientPid: String = "com.kernelcraft (PID 5120)",
    val targetPid: String = "system_server (PID 1420)"
)

/**
 * Complete UI state container for the dynamic OS simulation engine.
 */
@Immutable
data class KernelSimulationState(
    val governorMode: CpuGovernorMode = CpuGovernorMode.SCHEDUTIL,
    val temperatureCelsius: Float = 38.5f,
    val isThermalThrottling: Boolean = false,
    val coreTracks: List<CoreTrack> = emptyList(),
    val selectedSlice: ProcessSlice? = null,
    val memoryBlocks: List<MemoryPageBlock> = emptyList(),
    val totalRamMb: Int = 12_288,
    val userspaceUsedMb: Int = 2_720,
    val kernelUsedMb: Int = 1_648,
    val zramCompressedMb: Int = 648,
    val zramOriginalMb: Int = 1_850,
    val pageCacheHitRatePct: Int = 94,
    val pageFaultsPerSec: Int = 28,
    val binderTracer: BinderTracerState = BinderTracerState(),
    val simulationEpochMs: Long = 0L
)

/**
 * ViewModel managing the tick-driven simulation loop for CPU scheduling,
 * memory paging dynamics, thermal governor scaling, and Binder IPC tracing.
 */
class KernelSimulationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<KernelSimulationState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var binderJob: Job? = null

    init {
        startSimulationLoop()
    }

    private fun createInitialState(): KernelSimulationState {
        val memoryBlocks = List(32) { index ->
            val isKernel = index >= 16
            MemoryPageBlock(
                index = index,
                isKernelSpace = isKernel,
                label = if (isKernel) "K-PG#$index" else "U-PG#$index",
                isAllocated = index < 12 || (index in 16..26),
                isFlashing = false,
                isZramSwapped = index in 10..11
            )
        }

        return KernelSimulationState(
            governorMode = CpuGovernorMode.SCHEDUTIL,
            temperatureCelsius = 38.5f,
            isThermalThrottling = false,
            coreTracks = generateCoreTracks(CpuGovernorMode.SCHEDUTIL, 38.5f, 0L),
            memoryBlocks = memoryBlocks,
            userspaceUsedMb = 2720,
            kernelUsedMb = 1648
        )
    }

    /**
     * Starts the 50ms tick-driven coroutine loop simulating real-time OS scheduling
     * and memory dynamics without heavy CPU overhead.
     */
    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var epoch = 0L
            while (isActive) {
                delay(50) // ~20Hz logical state clock
                epoch += 50

                _uiState.update { current ->
                    val newEpoch = current.simulationEpochMs + 50
                    val updatedTracks = generateCoreTracks(
                        current.governorMode,
                        current.temperatureCelsius,
                        newEpoch
                    )

                    // Slight organic telemetry fluctuations
                    val randomHitRate = (93 + (Random.nextInt(4))).coerceIn(90, 99)
                    val randomFaults = (24 + (Random.nextInt(12))).coerceIn(10, 80)

                    // Clear any single-tick flash states on memory blocks
                    val settledBlocks = current.memoryBlocks.map {
                        if (it.isFlashing) it.copy(isFlashing = false) else it
                    }

                    current.copy(
                        coreTracks = updatedTracks,
                        memoryBlocks = settledBlocks,
                        pageCacheHitRatePct = randomHitRate,
                        pageFaultsPerSec = randomFaults,
                        simulationEpochMs = newEpoch
                    )
                }
            }
        }
    }

    /**
     * Generates active runqueue slices and clock speeds across Prime, Performance, and Efficiency cores.
     */
    private fun generateCoreTracks(
        mode: CpuGovernorMode,
        tempCelsius: Float,
        epochMs: Long
    ): List<CoreTrack> {
        val throttleFactor = if (tempCelsius > 48f) 0.65f else if (tempCelsius > 44f) 0.85f else 1.0f

        // Phase offset for scrolling execution slices
        val phase = ((epochMs % 2000) / 2000f)

        // Core 0: Prime Cortex-X4 (UI Thread & RenderThread)
        val primeSlices = listOf(
            ProcessSlice(
                id = "p_ui",
                processName = "com.kernelcraft.ui",
                threadTag = "UI Thread (VSYNC 120Hz)",
                niceValue = -10,
                durationMs = 4.2f,
                vruntimeDeltaUs = 120,
                clusterType = ClusterType.PRIME,
                startOffsetRatio = (0.05f + phase) % 1f,
                durationRatio = 0.32f
            ),
            ProcessSlice(
                id = "p_render",
                processName = "RenderThread",
                threadTag = "Skia / Vulkan HardwareRenderer",
                niceValue = -10,
                durationMs = 3.6f,
                vruntimeDeltaUs = 95,
                clusterType = ClusterType.PRIME,
                startOffsetRatio = (0.45f + phase) % 1f,
                durationRatio = 0.28f
            )
        )

        // Core 1: Performance Cortex-A720 (Binder IPC & ExoPlayer Seeker)
        val perfSlices = listOf(
            ProcessSlice(
                id = "p_binder",
                processName = "Binder:5120_2",
                threadTag = "Binder Worker IPC Transaction",
                niceValue = 0,
                durationMs = 2.1f,
                vruntimeDeltaUs = 340,
                clusterType = ClusterType.PERFORMANCE,
                startOffsetRatio = (0.12f + phase) % 1f,
                durationRatio = 0.25f
            ),
            ProcessSlice(
                id = "p_exo",
                processName = "media.codec",
                threadTag = "ExoPlayer All-Intra Frame Parser",
                niceValue = -4,
                durationMs = 5.8f,
                vruntimeDeltaUs = 210,
                clusterType = ClusterType.PERFORMANCE,
                startOffsetRatio = (0.50f + phase) % 1f,
                durationRatio = 0.35f
            )
        )

        // Core 2: Efficiency Cortex-A520 (Background Workers & kswapd0)
        val effSlices = listOf(
            ProcessSlice(
                id = "p_kswapd",
                processName = "kswapd0",
                threadTag = "Linux Memory Paging & Compaction",
                niceValue = 19,
                durationMs = 1.4f,
                vruntimeDeltaUs = 890,
                clusterType = ClusterType.EFFICIENCY,
                startOffsetRatio = (0.20f + phase) % 1f,
                durationRatio = 0.20f
            ),
            ProcessSlice(
                id = "p_jit",
                processName = "JIT Compiler Pool",
                threadTag = "ART Baseline Profile Compiler",
                niceValue = 10,
                durationMs = 6.2f,
                vruntimeDeltaUs = 650,
                clusterType = ClusterType.EFFICIENCY,
                startOffsetRatio = (0.55f + phase) % 1f,
                durationRatio = 0.30f
            )
        )

        return listOf(
            CoreTrack(
                coreIndex = 0,
                name = "Core 0 (Prime Cortex-X4)",
                clusterType = ClusterType.PRIME,
                currentFreqGhz = when (mode) {
                    CpuGovernorMode.PERFORMANCE -> 3.30f * throttleFactor
                    CpuGovernorMode.POWERSAVE -> 0.80f
                    CpuGovernorMode.SCHEDUTIL -> 2.65f * throttleFactor
                },
                maxFreqGhz = 3.30f * throttleFactor,
                utilizationPct = if (mode == CpuGovernorMode.POWERSAVE) 45 else 82,
                activeSlices = primeSlices
            ),
            CoreTrack(
                coreIndex = 1,
                name = "Core 1 (Perf Cortex-A720)",
                clusterType = ClusterType.PERFORMANCE,
                currentFreqGhz = when (mode) {
                    CpuGovernorMode.PERFORMANCE -> 3.15f
                    CpuGovernorMode.POWERSAVE -> 0.60f
                    CpuGovernorMode.SCHEDUTIL -> 2.10f
                },
                maxFreqGhz = 3.15f,
                utilizationPct = 68,
                activeSlices = perfSlices
            ),
            CoreTrack(
                coreIndex = 2,
                name = "Core 2 (Eff Cortex-A520)",
                clusterType = ClusterType.EFFICIENCY,
                currentFreqGhz = when (mode) {
                    CpuGovernorMode.PERFORMANCE -> 2.27f
                    CpuGovernorMode.POWERSAVE -> 0.40f
                    CpuGovernorMode.SCHEDUTIL -> 1.15f
                },
                maxFreqGhz = 2.27f,
                utilizationPct = 32,
                activeSlices = effSlices
            )
        )
    }

    /**
     * Toggles CPU Governor policy and updates clock frequency scaling.
     */
    fun setGovernor(mode: CpuGovernorMode) {
        _uiState.update { current ->
            current.copy(
                governorMode = mode,
                coreTracks = generateCoreTracks(mode, current.temperatureCelsius, current.simulationEpochMs)
            )
        }
    }

    /**
     * Sets target thermal temperature and computes throttling effects.
     */
    fun setTemperature(tempCelsius: Float) {
        _uiState.update { current ->
            val isThrottling = tempCelsius >= 45.0f
            current.copy(
                temperatureCelsius = tempCelsius,
                isThermalThrottling = isThrottling,
                coreTracks = generateCoreTracks(current.governorMode, tempCelsius, current.simulationEpochMs)
            )
        }
    }

    /**
     * Selects an individual slice for detailed inspection card display.
     */
    fun selectProcessSlice(slice: ProcessSlice?) {
        _uiState.update { it.copy(selectedSlice = slice) }
    }

    /**
     * Simulates an "App Launch" event: bursts userspace memory allocation and triggers page table creation.
     */
    fun simulateAppLaunch() {
        _uiState.update { current ->
            val updatedBlocks = current.memoryBlocks.mapIndexed { idx, block ->
                if (!block.isKernelSpace && idx in 0..14) {
                    block.copy(isAllocated = true, isFlashing = true, isZramSwapped = false)
                } else {
                    block
                }
            }
            current.copy(
                memoryBlocks = updatedBlocks,
                userspaceUsedMb = (current.userspaceUsedMb + 850).coerceAtMost(5500),
                pageFaultsPerSec = 78
            )
        }
    }

    /**
     * Simulates a "Heavy Task": increases memory pressure and forces cold anonymous pages into ZRAM.
     */
    fun simulateHeavyWorkload() {
        _uiState.update { current ->
            val updatedBlocks = current.memoryBlocks.mapIndexed { idx, block ->
                if (!block.isKernelSpace && idx in 8..15) {
                    block.copy(isAllocated = true, isFlashing = true, isZramSwapped = true)
                } else {
                    block
                }
            }
            current.copy(
                memoryBlocks = updatedBlocks,
                userspaceUsedMb = (current.userspaceUsedMb + 1400).coerceAtMost(6500),
                zramCompressedMb = 920,
                zramOriginalMb = 2650,
                temperatureCelsius = (current.temperatureCelsius + 2.5f).coerceAtMost(52f)
            )
        }
    }

    /**
     * Simulates Memory Trim (kswapd0 compaction): frees cached pages and compacts slab allocators.
     */
    fun simulateMemoryTrim() {
        _uiState.update { current ->
            val updatedBlocks = current.memoryBlocks.mapIndexed { idx, block ->
                if (!block.isKernelSpace && idx >= 10) {
                    block.copy(isAllocated = false, isFlashing = true, isZramSwapped = false)
                } else {
                    block
                }
            }
            current.copy(
                memoryBlocks = updatedBlocks,
                userspaceUsedMb = 2350,
                zramCompressedMb = 480,
                zramOriginalMb = 1320,
                pageFaultsPerSec = 14
            )
        }
    }

    /**
     * Triggers an animated Binder IPC transaction across the 3-tier boundary.
     */
    fun triggerBinderTransaction(payload: String = "android.view.IWindowSession::relayoutWindow") {
        binderJob?.cancel()
        binderJob = viewModelScope.launch {
            // Phase 1: Marshalling
            _uiState.update {
                it.copy(
                    binderTracer = it.binderTracer.copy(
                        phase = BinderTransactionPhase.MARSHALLING,
                        inFlightProgress = 0.2f,
                        activePayload = payload
                    )
                )
            }
            delay(160)

            // Phase 2: In Kernel ioctl (/dev/binder)
            _uiState.update {
                it.copy(
                    binderTracer = it.binderTracer.copy(
                        phase = BinderTransactionPhase.KERNEL_IN_FLIGHT,
                        inFlightProgress = 0.5f
                    )
                )
            }
            delay(220)

            // Phase 3: Executing in System Server Stub
            _uiState.update {
                it.copy(
                    binderTracer = it.binderTracer.copy(
                        phase = BinderTransactionPhase.DISPATCHED_IN_SERVICE,
                        inFlightProgress = 0.85f
                    )
                )
            }
            delay(200)

            // Phase 4: Returned
            _uiState.update {
                it.copy(
                    binderTracer = it.binderTracer.copy(
                        phase = BinderTransactionPhase.RETURNED,
                        inFlightProgress = 1.0f,
                        transactionCount = it.binderTracer.transactionCount + 1,
                        latencyMicros = 120 + Random.nextInt(45)
                    )
                )
            }
            delay(300)

            // Reset to Idle
            _uiState.update {
                it.copy(
                    binderTracer = it.binderTracer.copy(
                        phase = BinderTransactionPhase.IDLE,
                        inFlightProgress = 0f
                    )
                )
            }
        }
    }
}
