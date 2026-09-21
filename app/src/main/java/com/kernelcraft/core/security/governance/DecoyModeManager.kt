package com.kernelcraft.core.security.governance

import androidx.compose.ui.graphics.Color
import com.kernelcraft.feature.kernel.ProcessSlice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Anti-Reverse Engineering Decoy & Honeypot Mode Manager.
 *
 * Implements subtle deception countermeasures for non-fatal dynamic analysis heuristics
 * (Frida runtime hooks, debugger probes, dynamic proxy interception, unverified app stores).
 *
 * Rather than immediately terminating the process (which alerts attackers to specific detection routines),
 * this module quietly steers the application into an artificial sandbox state:
 * - Scrambles kernel CPU scheduler and frequency telemetry.
 * - Throttles video scrubbing pipelines to introduce synthetic latency.
 * - Injects honeypot processes and network canary endpoints to profile the attacker.
 */
object DecoyModeManager {

    private val _isDecoyActive = MutableStateFlow(false)
    val isDecoyActive: StateFlow<Boolean> = _isDecoyActive.asStateFlow()

    private val decoyEngaged = AtomicBoolean(false)

    /**
     * Activates or deactivates stealth Decoy / Honeypot mode.
     */
    fun setDecoyMode(enabled: Boolean) {
        val wasActive = decoyEngaged.getAndSet(enabled)
        _isDecoyActive.value = enabled

        if (enabled && !wasActive) {
            SecurityTelemetryDispatcher.reportThreat(ThreatVector.DECOY_MODE_TRIGGERED)
        }
    }

    fun isDecoyMode(): Boolean = decoyEngaged.get()

    /**
     * Injects synthetic jitter and absurd clock frequencies to confuse dynamic profiling.
     */
    fun getDecoyCpuFrequency(realFreqGhz: Float): Float {
        if (!decoyEngaged.get()) return realFreqGhz
        // Oscillate wildly between synthetic underclock and overclock states
        val noise = (Random.nextFloat() * 2.5f) - 1.0f
        return (realFreqGhz + noise).coerceIn(0.20f, 4.85f)
    }

    /**
     * Returns throttled playback rate for media decoders to waste attacker reversing time.
     */
    fun getDecoyPlaybackSpeedMultiplier(): Float {
        return if (decoyEngaged.get()) 0.45f else 1.0f
    }

    /**
     * Produces canary honeypot process slices designed to bait decompiler inspection.
     */
    fun generateDecoyProcessSlices(): List<ProcessSlice> {
        return listOf(
            ProcessSlice(
                id = "decoy_frida_probe",
                processName = "ptrace_sentinel_0xdead",
                threadTag = "DECOY_SYS",
                niceValue = -10,
                durationMs = 120.0f,
                vruntimeDeltaUs = 9481L,
                clusterType = com.kernelcraft.feature.kernel.ClusterType.PRIME,
                startOffsetRatio = 0.0f,
                durationRatio = 0.25f
            ),
            ProcessSlice(
                id = "decoy_sandbox_trap",
                processName = "kworker/u64:9-mem_scrub",
                threadTag = "DECOY_KWORKER",
                niceValue = 0,
                durationMs = 280.0f,
                vruntimeDeltaUs = 312L,
                clusterType = com.kernelcraft.feature.kernel.ClusterType.PERFORMANCE,
                startOffsetRatio = 0.25f,
                durationRatio = 0.35f
            ),
            ProcessSlice(
                id = "decoy_canary_dispatch",
                processName = "system_server:integrity_canary",
                threadTag = "DECOY_CANARY",
                niceValue = 19,
                durationMs = 200.0f,
                vruntimeDeltaUs = 1450L,
                clusterType = com.kernelcraft.feature.kernel.ClusterType.EFFICIENCY,
                startOffsetRatio = 0.60f,
                durationRatio = 0.40f
            )
        )
    }

    /**
     * Generates a unique honeytoken / canary URL. If an attacker or proxy requests this URI,
     * backend security systems immediately flag the client IP and device fingerprint.
     */
    fun generateCanaryHoneypotUrl(): String {
        val canaryToken = "KC_CANARY_" + Random.nextInt(100000, 999999)
        return "https://api.kernelcraft.com/v1/debug/symbols_trace?probe=$canaryToken"
    }
}
