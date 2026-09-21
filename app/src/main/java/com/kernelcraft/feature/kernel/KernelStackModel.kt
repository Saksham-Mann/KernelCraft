package com.kernelcraft.feature.kernel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The 5 hierarchical tiers of the Android & Linux operating system stack.
 * Arranged from physical silicon up to the user-facing application runtime.
 */
enum class OsTier(
    val tierIndex: Int,
    val identifier: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val privilegeRing: String,
    val securityDomain: String,
    val themeColor: Color,
    val keySyscallsOrApis: List<String>
) {
    PHYSICAL_SILICON(
        tierIndex = 0,
        identifier = "silicon",
        title = "Physical Silicon & SoC",
        subtitle = "Monolithic 4nm FinFET Microarchitecture & Interconnect",
        badge = "HARDWARE LAYER",
        privilegeRing = "Bare Metal / ARM TrustZone",
        securityDomain = "Root of Trust & Secure Processing Unit (SPU)",
        themeColor = Color(0xFF4E7F9E), // Steel Blue (Design.md §1.2)
        keySyscallsOrApis = listOf(
            "AXI/NoC Interconnect",
            "LPDDR5X Memory Bus (8533 Mbps)",
            "DVS/DVFS Voltage Regulators",
            "Hardware GICv3 Interrupt Controller"
        )
    ),

    LINUX_KERNEL(
        tierIndex = 1,
        identifier = "kernel",
        title = "Linux Kernel",
        subtitle = "Process Scheduling, Memory Paging, Power & Drivers",
        badge = "KERNEL SPACE (RING 0)",
        privilegeRing = "EL1 Supervisor Mode",
        securityDomain = "SELinux Enforcing, App Sandboxing (UID/GID)",
        themeColor = Color(0xFF4E7F9E), // Steel Blue
        keySyscallsOrApis = listOf(
            "clone() / fork() / execve()",
            "epoll_wait() / futex()",
            "mmap() / madvise(MADV_DONTNEED)",
            "ioctl(/dev/binder, BINDER_WRITE_READ)"
        )
    ),

    HAL_LAYER(
        tierIndex = 2,
        identifier = "hal",
        title = "Hardware Abstraction Layer (HAL)",
        subtitle = "Vendor-Neutral Interfaces & AIDL/HIDL Daemons",
        badge = "HARDWARE ABSTRACTION",
        privilegeRing = "EL0 Isolated User Mode",
        securityDomain = "Restricted hwservice_manager, Treble Boundary",
        themeColor = Color(0xFF4E7F9E), // Steel Blue
        keySyscallsOrApis = listOf(
            "android.hardware.camera.provider@2.7",
            "android.hardware.graphics.composer@3.0 (HWC)",
            "android.hardware.audio@7.1",
            "android.hardware.biometrics.fingerprint"
        )
    ),

    ART_RUNTIME(
        tierIndex = 3,
        identifier = "art",
        title = "Android Runtime (ART) & Bionic",
        subtitle = "AOT/JIT Compilation, Concurrent GC & Native Libs",
        badge = "MANAGED RUNTIME",
        privilegeRing = "EL0 User Mode",
        securityDomain = "Isolated Dalvik/ART VM, Bionic Libc Sandboxing",
        themeColor = Color(0xFFE8A020), // Mustard (Design.md §1.2)
        keySyscallsOrApis = listOf(
            "art::gc::ConcurrentCopyingCollector",
            "Baseline Profiles & Cloud DEX PGO",
            "Bionic libc / libm / libdl",
            "Zygote forkAndSpecializeAppProcess()"
        )
    ),

    USERSPACE_FRAMEWORK(
        tierIndex = 4,
        identifier = "framework",
        title = "Userspace & Jetpack Compose",
        subtitle = "System Services, IPC Dispatch & UI Rendering Pipeline",
        badge = "APPLICATION LAYER",
        privilegeRing = "EL0 Unprivileged User Mode",
        securityDomain = "App UID sandbox, Runtime Permissions",
        themeColor = Color(0xFFE8A020), // Mustard
        keySyscallsOrApis = listOf(
            "androidx.compose.runtime (Snapshot State)",
            "android.view.Choreographer (120Hz VSYNC)",
            "android.os.ServiceManager / ActivityManager",
            "android.graphics.HardwareRenderer (RenderThread)"
        )
    );

    companion object {
        val defaultTier: OsTier = PHYSICAL_SILICON

        fun fromIndex(index: Int): OsTier {
            return entries.getOrElse(index.coerceIn(0, entries.size - 1)) { PHYSICAL_SILICON }
        }
    }
}

/**
 * Architectural breakdown topics for a specific OS tier.
 */
@Immutable
data class TierSubsystem(
    val title: String,
    val tag: String,
    val description: String,
    val metricLabel: String,
    val metricValue: String
)

/**
 * Supported Linux CPU Governor modes for interactive simulation.
 */
enum class CpuGovernorMode(val displayName: String, val description: String) {
    SCHEDUTIL(
        displayName = "Schedutil (EAS)",
        description = "Energy-Aware Scheduling: queries CFS runqueues to dynamically adjust core clock frequencies based on PELT (Per-Entity Load Tracking)."
    ),
    PERFORMANCE(
        displayName = "Performance",
        description = "Pins all CPU clusters at their maximum rated clock speed. Maximizes frame throughput at the cost of high thermal dissipation."
    ),
    POWERSAVE(
        displayName = "Powersave",
        description = "Locks CPU cores at the lowest operational frequency (F_min). Minimizes energy consumption during standby or low-priority background jobs."
    )
}

/**
 * Live telemetry state for an individual CPU core.
 */
@Immutable
data class CoreTelemetry(
    val coreIndex: Int,
    val name: String,
    val clusterType: ClusterType,
    val minFreqGhz: Float,
    val maxFreqGhz: Float,
    val currentFreqGhz: Float,
    val loadPercentage: Int,
    val activeTask: String
) {
    val normalizedFrequency: Float
        get() = ((currentFreqGhz - minFreqGhz) / (maxFreqGhz - minFreqGhz)).coerceIn(0f, 1f)
}

enum class ClusterType(val label: String, val colorHex: Long) {
    PRIME("PRIME (Cortex-X4)", 0xFFE8A020),
    PERFORMANCE("PERF (5x A720)", 0xFF4E7F9E),
    EFFICIENCY("EFF (2x A520)", 0xFF7A9E5B)
}

/**
 * State container for the Linux Kernel CPU Governor simulation.
 */
@Immutable
data class CpuGovernorState(
    val governorMode: CpuGovernorMode = CpuGovernorMode.SCHEDUTIL,
    val thermalTempCelsius: Float = 38.5f,
    val isThermalThrottling: Boolean = false,
    val cores: List<CoreTelemetry> = defaultCores(CpuGovernorMode.SCHEDUTIL, 38.5f)
) {
    companion object {
        fun defaultCores(mode: CpuGovernorMode, tempCelsius: Float): List<CoreTelemetry> {
            val throttleFactor = if (tempCelsius > 48f) 0.65f else if (tempCelsius > 44f) 0.85f else 1.0f

            return listOf(
                // Core 0: Prime Cortex-X4
                CoreTelemetry(
                    coreIndex = 0,
                    name = "Core 0",
                    clusterType = ClusterType.PRIME,
                    minFreqGhz = 0.80f,
                    maxFreqGhz = 3.30f * throttleFactor,
                    currentFreqGhz = when (mode) {
                        CpuGovernorMode.PERFORMANCE -> 3.30f * throttleFactor
                        CpuGovernorMode.POWERSAVE -> 0.80f
                        CpuGovernorMode.SCHEDUTIL -> (2.65f * throttleFactor)
                    },
                    loadPercentage = if (mode == CpuGovernorMode.POWERSAVE) 42 else 78,
                    activeTask = "RenderThread (120fps)"
                ),
                // Core 1..5: Performance Cortex-A720
                CoreTelemetry(
                    coreIndex = 1,
                    name = "Core 1",
                    clusterType = ClusterType.PERFORMANCE,
                    minFreqGhz = 0.60f,
                    maxFreqGhz = 3.15f,
                    currentFreqGhz = when (mode) {
                        CpuGovernorMode.PERFORMANCE -> 3.15f
                        CpuGovernorMode.POWERSAVE -> 0.60f
                        CpuGovernorMode.SCHEDUTIL -> 2.10f
                    },
                    loadPercentage = 64,
                    activeTask = "SurfaceFlinger"
                ),
                CoreTelemetry(
                    coreIndex = 2,
                    name = "Core 2",
                    clusterType = ClusterType.PERFORMANCE,
                    minFreqGhz = 0.60f,
                    maxFreqGhz = 3.15f,
                    currentFreqGhz = when (mode) {
                        CpuGovernorMode.PERFORMANCE -> 3.15f
                        CpuGovernorMode.POWERSAVE -> 0.60f
                        CpuGovernorMode.SCHEDUTIL -> 1.85f
                    },
                    loadPercentage = 52,
                    activeTask = "ExoPlayer Codec"
                ),
                CoreTelemetry(
                    coreIndex = 3,
                    name = "Core 3",
                    clusterType = ClusterType.PERFORMANCE,
                    minFreqGhz = 0.60f,
                    maxFreqGhz = 3.15f,
                    currentFreqGhz = when (mode) {
                        CpuGovernorMode.PERFORMANCE -> 3.15f
                        CpuGovernorMode.POWERSAVE -> 0.60f
                        CpuGovernorMode.SCHEDUTIL -> 1.45f
                    },
                    loadPercentage = 38,
                    activeTask = "JIT Compiler Pool"
                ),
                // Core 4..5: Efficiency Cortex-A520
                CoreTelemetry(
                    coreIndex = 4,
                    name = "Core 4",
                    clusterType = ClusterType.EFFICIENCY,
                    minFreqGhz = 0.40f,
                    maxFreqGhz = 2.27f,
                    currentFreqGhz = when (mode) {
                        CpuGovernorMode.PERFORMANCE -> 2.27f
                        CpuGovernorMode.POWERSAVE -> 0.40f
                        CpuGovernorMode.SCHEDUTIL -> 1.15f
                    },
                    loadPercentage = 28,
                    activeTask = "kswapd0 (Paging)"
                ),
                CoreTelemetry(
                    coreIndex = 5,
                    name = "Core 5",
                    clusterType = ClusterType.EFFICIENCY,
                    minFreqGhz = 0.40f,
                    maxFreqGhz = 2.27f,
                    currentFreqGhz = when (mode) {
                        CpuGovernorMode.PERFORMANCE -> 2.27f
                        CpuGovernorMode.POWERSAVE -> 0.40f
                        CpuGovernorMode.SCHEDUTIL -> 0.95f
                    },
                    loadPercentage = 18,
                    activeTask = "Binder Worker #1"
                )
            )
        }
    }
}

/**
 * Lifecycle state of an IPC Binder transaction.
 */
enum class BinderTransactionPhase(val stepName: String, val description: String) {
    IDLE("Idle / Ready", "Binder client thread waiting for dispatch."),
    MARSHALLING("Parcel Marshalling", "Packing data arguments into contiguous shared parcel memory."),
    KERNEL_IN_FLIGHT("Kernel ioctl (/dev/binder)", "Linux kernel driver maps pages into target process address space via BINDER_WRITE_READ."),
    DISPATCHED_IN_SERVICE("Service Execution", "Target AIDL stub executes requested method in System Server."),
    RETURNED("Transaction Complete", "Result unmarshalled back into client thread.")
}

/**
 * State container for Framework IPC / Binder transactions.
 */
@Immutable
data class BinderSimulationState(
    val phase: BinderTransactionPhase = BinderTransactionPhase.IDLE,
    val transactionCount: Int = 1420,
    val latencyMicros: Int = 185,
    val payloadDescriptor: String = "android.view.IWindowSession::relayoutWindow",
    val clientProcess: String = "com.kernelcraft (PID 4821)",
    val targetProcess: String = "system_server (PID 1420)"
)

/**
 * Educational repository providing structured breakdowns for each OS tier.
 */
object TierRepository {
    fun getSubsystems(tier: OsTier): List<TierSubsystem> {
        return when (tier) {
            OsTier.PHYSICAL_SILICON -> listOf(
                TierSubsystem(
                    title = "Monolithic 4nm FinFET Die",
                    tag = "PROCESS NODE",
                    description = "Manufactured on TSMC N4P process with ~16 billion transistors, integrating CPU, GPU, NPU, and modem on a single silicon substrate.",
                    metricLabel = "Transistor Density",
                    metricValue = "142 MTr/mm²"
                ),
                TierSubsystem(
                    title = "DynamIQ Cluster Interconnect",
                    tag = "CPU COHERENCY",
                    description = "ARM CoreLink CI-700 system interconnect linking Cortex-X4, A720, and A520 clusters to a unified 8MB L3 system cache.",
                    metricLabel = "L3 Cache Bandwidth",
                    metricValue = "1.2 TB/s"
                ),
                TierSubsystem(
                    title = "Hardware Security Island",
                    tag = "ARM TRUSTZONE",
                    description = "Cryptographically isolated secure enclave executing Trusty OS for biometric key storage and Android KeyMint operations.",
                    metricLabel = "Hardware Root",
                    metricValue = "eFuse OTP Ring"
                )
            )

            OsTier.LINUX_KERNEL -> listOf(
                TierSubsystem(
                    title = "Energy-Aware Scheduler (EAS)",
                    tag = "SCHEDULING",
                    description = "Extends the Completely Fair Scheduler (CFS) with energy models, placing interactive threads on Prime cores and background tasks on Efficiency cores.",
                    metricLabel = "Context Switch Overhead",
                    metricValue = "< 1.2 µs"
                ),
                TierSubsystem(
                    title = "ZRAM Swap & Memory Compaction",
                    tag = "VIRTUAL MEMORY",
                    description = "Compresses anonymous memory pages using LZ4/ZSTD in a RAM-backed block device, preventing sluggish NAND flash wear.",
                    metricLabel = "Compression Ratio",
                    metricValue = "2.85x (1.8GB in 630MB)"
                ),
                TierSubsystem(
                    title = "Binder Driver (/dev/binder)",
                    tag = "KERNEL IPC",
                    description = "Custom Linux character driver providing high-speed zero-copy IPC using single-copy memory mapping between process address spaces.",
                    metricLabel = "IPC Throughput",
                    metricValue = "~65,000 tx/sec"
                )
            )

            OsTier.HAL_LAYER -> listOf(
                TierSubsystem(
                    title = "Stable AIDL Interfaces",
                    tag = "TREBLE ARCHITECTURE",
                    description = "Replaces legacy HIDL with Stable AIDL, decoupling the Android Framework from vendor proprietary BSP drivers across Android OS upgrades.",
                    metricLabel = "ABI Compatibility",
                    metricValue = "Strict Stable AIDL"
                ),
                TierSubsystem(
                    title = "Hardware Composer (HWC3)",
                    tag = "SURFACE FLINGER",
                    description = "Directly programs the display processor (DPU) overlays to composite UI surfaces directly in hardware, bypassing GPU overhead.",
                    metricLabel = "Display Pipeline",
                    metricValue = "120Hz Zero-Jank"
                ),
                TierSubsystem(
                    title = "Camera HAL3 Pipeline",
                    tag = "IMAGE SIGNAL PROCESSOR",
                    description = "Directly coordinates capture requests between the Android Camera2 API and the vendor ISP hardware processing blocks.",
                    metricLabel = "Capture Latency",
                    metricValue = "< 18ms Frame Interval"
                )
            )

            OsTier.ART_RUNTIME -> listOf(
                TierSubsystem(
                    title = "Concurrent Copying (CC) GC",
                    tag = "MEMORY MANAGEMENT",
                    description = "Generational garbage collector executing concurrently with app threads using read barriers, eliminating visible frame drops.",
                    metricLabel = "GC Pause Time",
                    metricValue = "< 1.5ms per collection"
                ),
                TierSubsystem(
                    title = "Baseline Profiles & Cloud PGO",
                    tag = "COMPILATION ENGINE",
                    description = "Pre-compiles critical user interaction pathways into native machine code before first app launch, boosting startup by up to 40%.",
                    metricLabel = "Startup Acceleration",
                    metricValue = "+38% First Frame"
                ),
                TierSubsystem(
                    title = "Zygote Template Forking",
                    tag = "PROCESS LAUNCH",
                    description = "Pre-loads all core framework classes and Android resources, using Linux Copy-on-Write (COW) memory sharing when spawning apps.",
                    metricLabel = "Warm Fork Time",
                    metricValue = "~22ms from Intent"
                )
            )

            OsTier.USERSPACE_FRAMEWORK -> listOf(
                TierSubsystem(
                    title = "Jetpack Compose Runtime",
                    tag = "UI TOOLKIT",
                    description = "Reactive declarative UI engine using positional memoization and snapshot state tracking to intelligently recompose only dirty nodes.",
                    metricLabel = "Frame Budget",
                    metricValue = "8.33ms (120 FPS Target)"
                ),
                TierSubsystem(
                    title = "Choreographer & RenderThread",
                    tag = "SYSTEM PIPELINE",
                    description = "Locks UI traversal to hardware VSYNC pulses, dispatching draw commands to Skia/Vulkan on a dedicated background RenderThread.",
                    metricLabel = "VSYNC Cadence",
                    metricValue = "Hardware Locked"
                ),
                TierSubsystem(
                    title = "App Sandboxing (UID Protection)",
                    tag = "ANDROID SECURITY",
                    description = "Assigns each installed app a unique Linux user ID (e.g. u0_a184), enforcing strict filesystem and network isolation.",
                    metricLabel = "SELinux Context",
                    metricValue = "u:r:untrusted_app:s0"
                )
            )
        }
    }
}
