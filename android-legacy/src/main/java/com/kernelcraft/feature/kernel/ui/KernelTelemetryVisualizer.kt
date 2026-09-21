package com.kernelcraft.feature.kernel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.kernel.BinderSimulationState
import com.kernelcraft.feature.kernel.BinderTransactionPhase
import com.kernelcraft.feature.kernel.ClusterType
import com.kernelcraft.feature.kernel.CoreTelemetry
import com.kernelcraft.feature.kernel.CpuGovernorMode
import com.kernelcraft.feature.kernel.CpuGovernorState
import com.kernelcraft.feature.kernel.OsTier
import com.kernelcraft.feature.silicon.SiliconDieVisualizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Top-level dynamic visualizer dispatcher for each OS tier.
 */
@Composable
fun TierInteractiveVisualizer(
    tier: OsTier,
    modifier: Modifier = Modifier
) {
    when (tier) {
        OsTier.PHYSICAL_SILICON -> {
            SiliconDieVisualizer(modifier = modifier)
        }
        OsTier.LINUX_KERNEL -> {
            KernelSchedulerVisualizer(modifier = modifier)
        }
        OsTier.HAL_LAYER -> {
            HalTrebleBridgeVisualizer(modifier = modifier)
        }
        OsTier.ART_RUNTIME -> {
            ArtCompilationVisualizer(modifier = modifier)
        }
        OsTier.USERSPACE_FRAMEWORK -> {
            FrameworkIpcVisualizer(modifier = modifier)
        }
    }
}

// -----------------------------------------------------------------------------
// 1. Linux Kernel Telemetry & Governor Visualizer
// -----------------------------------------------------------------------------

@Composable
fun KernelSchedulerVisualizer(
    modifier: Modifier = Modifier
) {
    var selectedGovernor by remember { mutableStateOf(CpuGovernorMode.SCHEDUTIL) }
    var temperatureCelsius by remember { mutableFloatStateOf(39.0f) }

    val governorState = remember(selectedGovernor, temperatureCelsius) {
        CpuGovernorState(
            governorMode = selectedGovernor,
            thermalTempCelsius = temperatureCelsius,
            isThermalThrottling = temperatureCelsius >= 45f,
            cores = CpuGovernorState.defaultCores(selectedGovernor, temperatureCelsius)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141414))
            .padding(18.dp)
    ) {
        // Telemetry Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (governorState.isThermalThrottling) Color(0xFFE53935) else Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CPU GOVERNOR & RUNQUEUES",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${governorState.thermalTempCelsius.toInt()}°C SoC THERMAL",
                color = if (governorState.isThermalThrottling) Color(0xFFE53935) else Color(0xFF9E9E9E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Governor Mode Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CpuGovernorMode.entries.forEach { mode ->
                val isSelected = selectedGovernor == mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF4E7F9E) else Color(0xFF222222),
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedGovernor = mode }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Thermal Throttling Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulate Thermal Pressure:",
                    color = Color(0xFF9E9E9E),
                    fontSize = 11.sp
                )
                Text(
                    text = if (governorState.isThermalThrottling) "THROTTLING ACTIVE" else "NOMINAL ENVELOPE",
                    color = if (governorState.isThermalThrottling) Color(0xFFE8A020) else Color(0xFF4CAF50),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = temperatureCelsius,
                onValueChange = { temperatureCelsius = it },
                valueRange = 32f..54f,
                colors = SliderDefaults.colors(
                    thumbColor = if (governorState.isThermalThrottling) Color(0xFFE53935) else Color(0xFF4E7F9E),
                    activeTrackColor = if (governorState.isThermalThrottling) Color(0xFFE53935) else Color(0xFF4E7F9E),
                    inactiveTrackColor = Color(0xFF333333)
                ),
                modifier = Modifier.height(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Core Multi-Bar Telemetry
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1A1A))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            governorState.cores.forEach { core ->
                CoreFrequencyBarRow(core = core)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Memory Paging / ZRAM Mini-Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF222222))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ZRAM COMPRESSION RATIO",
                    color = Color(0xFF9E9E9E),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "1.85 GB Anonymous Pages in 648 MB RAM (2.85x LZ4)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "kswapd0",
                color = Color(0xFFE8A020),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CoreFrequencyBarRow(core: CoreTelemetry) {
    val animatedFreq by animateFloatAsState(
        targetValue = core.currentFreqGhz,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "CoreFreq"
    )

    val clusterColor = when (core.clusterType) {
        ClusterType.PRIME -> Color(0xFFE8A020)
        ClusterType.PERFORMANCE -> Color(0xFF4E7F9E)
        ClusterType.EFFICIENCY -> Color(0xFF7A9E5B)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${core.name} (${core.clusterType.label})",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = String.format("%.2f GHz • %s", animatedFreq, core.activeTask),
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF2C2C2C))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(core.normalizedFrequency)
                    .clip(RoundedCornerShape(3.dp))
                    .background(clusterColor)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Framework & Userspace IPC / Binder Visualizer
// -----------------------------------------------------------------------------

@Composable
fun FrameworkIpcVisualizer(
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(BinderSimulationState()) }
    val scope = rememberCoroutineScope()
    var inFlightProgress by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "ChoreographerPulse")
    val vsyncAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(833, easing = LinearEasing), // ~120Hz VSYNC cadence representation
            repeatMode = RepeatMode.Reverse
        ),
        label = "VsyncPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141414))
            .padding(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8A020).copy(alpha = vsyncAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BINDER IPC INTER-PROCESS PIPE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${state.transactionCount} TX / SEC",
                color = Color(0xFFE8A020),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Tier Architectural Cross-Section Diagram
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C1C))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Layer A: Client App Space
            IpcLayerNode(
                badge = "USERSPACE (UID 10243)",
                title = state.clientProcess,
                subtitle = "App Process Proxy • android.view.IWindowSession",
                isActive = state.phase == BinderTransactionPhase.MARSHALLING || state.phase == BinderTransactionPhase.RETURNED,
                activeColor = Color(0xFFE8A020)
            )

            // Dynamic Packet Path Down
            BinderPacketCanvas(
                progress = inFlightProgress,
                directionDown = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            // Layer B: Linux Kernel /dev/binder
            IpcLayerNode(
                badge = "KERNEL SPACE (RING 0)",
                title = "ioctl(/dev/binder, BINDER_WRITE_READ)",
                subtitle = "Single-Copy Physical Page Mapping • IPC Thread Pool",
                isActive = state.phase == BinderTransactionPhase.KERNEL_IN_FLIGHT,
                activeColor = Color(0xFF4E7F9E)
            )

            // Dynamic Packet Path Down
            BinderPacketCanvas(
                progress = inFlightProgress,
                directionDown = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            // Layer C: Target System Server
            IpcLayerNode(
                badge = "SYSTEM SERVER (UID 1000)",
                title = state.targetProcess,
                subtitle = "WindowManagerService Stub • SurfaceFlinger BufferQueue",
                isActive = state.phase == BinderTransactionPhase.DISPATCHED_IN_SERVICE,
                activeColor = Color(0xFF4CAF50)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger Button & Live Phase Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF242424),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.phase.stepName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.phase.description,
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            // Phase 1: Marshalling
                            state = state.copy(phase = BinderTransactionPhase.MARSHALLING)
                            inFlightProgress = 0.2f
                            delay(220)

                            // Phase 2: In Flight
                            state = state.copy(phase = BinderTransactionPhase.KERNEL_IN_FLIGHT)
                            inFlightProgress = 0.5f
                            delay(280)

                            // Phase 3: Service Stub Execution
                            state = state.copy(phase = BinderTransactionPhase.DISPATCHED_IN_SERVICE)
                            inFlightProgress = 0.85f
                            delay(250)

                            // Phase 4: Return Complete
                            state = state.copy(
                                phase = BinderTransactionPhase.RETURNED,
                                transactionCount = state.transactionCount + 1
                            )
                            inFlightProgress = 1.0f
                            delay(350)

                            // Reset
                            state = state.copy(phase = BinderTransactionPhase.IDLE)
                            inFlightProgress = 0f
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8A020)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "Transmit IPC",
                        color = Color(0xFF141414),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun IpcLayerNode(
    badge: String,
    title: String,
    subtitle: String,
    isActive: Boolean,
    activeColor: Color
) {
    val borderColor = if (isActive) activeColor else Color.White.copy(alpha = 0.08f)
    val bgColor = if (isActive) activeColor.copy(alpha = 0.15f) else Color(0xFF222222)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = badge,
                color = if (isActive) activeColor else Color(0xFF9E9E9E),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun BinderPacketCanvas(
    progress: Float,
    directionDown: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val centerX = size.width / 2

        // Dashed connection leader
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Animated Traveling Packet
        if (progress > 0.05f && progress < 0.95f) {
            val yPos = if (directionDown) size.height * progress else size.height * (1f - progress)
            drawCircle(
                color = Color(0xFFE8A020),
                radius = 5.dp.toPx(),
                center = Offset(centerX, yPos)
            )
            drawCircle(
                color = Color(0xFFE8A020).copy(alpha = 0.35f),
                radius = 9.dp.toPx(),
                center = Offset(centerX, yPos)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Android Runtime (ART) & Native Libraries Visualizer
// -----------------------------------------------------------------------------

@Composable
fun ArtCompilationVisualizer(
    modifier: Modifier = Modifier
) {
    var selectedPhaseIndex by remember { mutableIntStateOf(1) }

    val phases = listOf(
        Triple("Interpreted DEX", "First Launch / Cold", "Fast startup with zero compilation delay; runs through ART interpreter bytecode loop."),
        Triple("JIT + Profiling", "Active Execution", "Profiles hot methods and JIT-compiles performance-critical call sites into RAM cache."),
        Triple("AOT Ahead-Of-Time", "Idle / Background", "DEX2OAT compiles profiled classes into native machine code (.oat/.odex) using Baseline Profiles.")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141414))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8A020))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ART COMPILATION & EXECUTION PIPELINE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "DEX2OAT JIT/AOT",
                color = Color(0xFF9E9E9E),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Stage Compilation Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            phases.forEachIndexed { index, phase ->
                val isSelected = selectedPhaseIndex == index
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF2E2619) else Color(0xFF222222),
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFFE8A020) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPhaseIndex = index }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "STAGE 0${index + 1}",
                            color = if (isSelected) Color(0xFFE8A020) else Color(0xFF9E9E9E),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phase.first,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = phase.second,
                            color = Color(0xFF9E9E9E),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Stage Breakdown
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF222222),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = phases[selectedPhaseIndex].first,
                    color = Color(0xFFE8A020),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phases[selectedPhaseIndex].third,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Hardware Abstraction Layer (HAL) Treble Bridge Visualizer
// -----------------------------------------------------------------------------

@Composable
fun HalTrebleBridgeVisualizer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141414))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4E7F9E))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROJECT TREBLE HARDWARE BOUNDARY",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "STABLE AIDL",
                color = Color(0xFF4E7F9E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Flow Diagram
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1E1E))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HalNodeItem(
                tierLabel = "FRAMEWORK (Google)",
                name = "Camera2 API / SurfaceFlinger",
                detail = "Generic Android OS image (GSI) communicates across Binder RPC without vendor drivers."
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            ) {
                drawLine(
                    color = Color(0xFF4E7F9E),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }

            HalNodeItem(
                tierLabel = "VENDOR HAL DAEMON (Qualcomm/Samsung)",
                name = "android.hardware.camera.provider@2.7-service",
                detail = "Runs in isolated vendor sandbox process, interfacing with proprietary ISP algorithms."
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            ) {
                drawLine(
                    color = Color(0xFF4E7F9E),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }

            HalNodeItem(
                tierLabel = "LINUX KERNEL DEVICE NODE",
                name = "/dev/video0 • V4L2 Subdev",
                detail = "Direct memory DMA buffer sharing via dma-buf ioctl with zero user-space copying."
            )
        }
    }
}

@Composable
private fun HalNodeItem(
    tierLabel: String,
    name: String,
    detail: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF262626),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = tierLabel,
                color = Color(0xFF4E7F9E),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }
}
