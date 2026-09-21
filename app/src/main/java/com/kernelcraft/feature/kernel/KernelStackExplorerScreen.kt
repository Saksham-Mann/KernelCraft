package com.kernelcraft.feature.kernel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kernelcraft.feature.kernel.ui.BinderTracerCanvas
import com.kernelcraft.feature.kernel.ui.CpuSchedulerGraph
import com.kernelcraft.feature.kernel.ui.IsometricSiliconCanvas
import com.kernelcraft.feature.kernel.ui.MemorySpaceCanvas

private val StudioOrange = Color(0xFFDD5622)
private val EdgeGlowOrange = Color(0xFFFF7033)
private val DarkBackground = Color(0xFF0D0D12)
private val CardBackground = Color(0xFF181822)
private val SelectedCardBackground = Color(0xFF222230)
private val BadgeDark = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF9E9EAA)

@Immutable
data class StackTierItem(
    val tierId: Int,
    val title: String,
    val tag: String,
    val subtitle: String,
    val privilegeTag: String,
    val accentColor: Color
)

/**
 * Inside the Mobile Phone: Kernel Stack Explorer Screen.
 *
 * Architecture:
 * 1. Top Viewport & Silicon Immersion Canvas (Isometric 3D layer diagram over silicon die).
 * 2. Interactive Vertical Stack Selector (4 core tiers with timeline rail on left margin).
 * 3. Live Telemetry & Process Simulator (Linux Kernel Tier Focus: Gantt chart, Memory Space, Diagnostics).
 * 4. Micro-interactions & Bottom Floating Dock (Stack Overview, Layer Detail, Reset Session).
 */
@Composable
fun KernelStackExplorerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KernelSimulationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    val stackTiers = remember {
        listOf(
            StackTierItem(
                tierId = 0,
                title = "User Apps: Android & Jetpack Compose",
                tag = "APPLICATION RUNTIME",
                subtitle = "Declarative UI toolkit, Choreographer 120Hz VSYNC, Compose Snapshot State & UID sandboxing.",
                privilegeTag = "EL0 USER MODE",
                accentColor = Color(0xFFE8A020)
            ),
            StackTierItem(
                tierId = 1,
                title = "Android Framework: API & Runtime (ART)",
                tag = "SYSTEM SERVICES",
                subtitle = "WindowManagerService, ActivityTaskManager, Bionic libc & Baseline Profiles Ahead-Of-Time compilation.",
                privilegeTag = "EL0 SYSTEM UID",
                accentColor = Color(0xFF4CAF50)
            ),
            StackTierItem(
                tierId = 2,
                title = "Hardware Abstraction Layer (HAL): Connecting Logic & Hardware",
                tag = "PROJECT TREBLE",
                subtitle = "Stable AIDL / HIDL interfaces decoupling Android framework from Qualcomm/Samsung BSP drivers.",
                privilegeTag = "EL0 VENDOR HAL",
                accentColor = Color(0xFF4E7F9E)
            ),
            StackTierItem(
                tierId = 3,
                title = "Linux Kernel: Process Scheduler & Device Drivers",
                tag = "CORE KERNEL",
                subtitle = "CFS/EAS CPU thread scheduling, ZRAM memory swap compaction, power domains & /dev/binder IPC driver.",
                privilegeTag = "RING 0 KERNEL (EL1)",
                accentColor = Color(0xFFDD5622)
            )
        )
    }

    // Default selected tier is Linux Kernel (tierId = 3)
    var selectedTierId by remember { mutableIntStateOf(3) }
    var activeDockAction by remember { mutableStateOf("Layer Detail") }

    val scrollState = rememberScrollState()

    // Pulse animation for active timeline ring
    val infiniteTransition = rememberInfiniteTransition(label = "TimelinePulse")
    val railPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RailPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. Top Viewport & Silicon Immersion Canvas
            IsometricSiliconCanvas(
                onZoomOutClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateBack()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPERATING SYSTEM STACK",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "TIER ${selectedTierId + 1} OF 4 ACTIVE",
                    color = StudioOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Interactive Vertical Stack Selector with Left Timeline Rail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Animated Vertical Timeline Rail along Left Margin
                TimelineRail(
                    totalTiers = stackTiers.size,
                    selectedTier = selectedTierId,
                    pulseAlpha = railPulseAlpha,
                    modifier = Modifier
                        .width(28.dp)
                        .padding(top = 18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Tier Cards List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    stackTiers.forEach { tier ->
                        val isSelected = tier.tierId == selectedTierId

                        StackTierCard(
                            tier = tier,
                            isSelected = isSelected,
                            uiState = uiState,
                            viewModel = viewModel,
                            onCardClick = {
                                if (selectedTierId != tier.tierId) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTierId = tier.tierId
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for floating bottom dock
        }

        // 4. Bottom Floating Action Dock
        BottomFloatingDock(
            activeAction = activeDockAction,
            onActionSelected = { action ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                activeDockAction = action
                when (action) {
                    "Reset Session" -> {
                        selectedTierId = 3 // Reset to Linux Kernel
                        viewModel.setGovernor(CpuGovernorMode.SCHEDUTIL)
                        viewModel.simulateMemoryTrim()
                    }
                    "Stack Overview" -> {
                        selectedTierId = 0
                    }
                    "Layer Detail" -> {
                        selectedTierId = 3
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * Vertical Timeline Rail rendering dots and connecting line snapping to selected index.
 */
@Composable
private fun TimelineRail(
    totalTiers: Int,
    selectedTier: Int,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .height(520.dp)
    ) {
        val totalHeight = size.height
        val step = totalHeight / (totalTiers - 1)
        val centerX = size.width / 2

        // Continuous connecting line
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(centerX, 0f),
            end = Offset(centerX, totalHeight),
            strokeWidth = 2.dp.toPx()
        )

        // Tier Indicator Dots
        for (i in 0 until totalTiers) {
            val y = i * step
            val isSelected = i == selectedTier

            if (isSelected) {
                // Outer Pulse Ring
                drawCircle(
                    color = StudioOrange.copy(alpha = pulseAlpha * 0.35f),
                    radius = 11.dp.toPx(),
                    center = Offset(centerX, y)
                )
                // Inner Solid Dot
                drawCircle(
                    color = StudioOrange,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, y)
                )
            } else {
                drawCircle(
                    color = Color(0xFF333340),
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, y)
                )
            }
        }
    }
}

/**
 * Individual Tier Card with fluid height transition (animateContentSize).
 */
@Composable
private fun StackTierCard(
    tier: StackTierItem,
    isSelected: Boolean,
    uiState: KernelSimulationState,
    viewModel: KernelSimulationViewModel,
    onCardClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tier.accentColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(250),
        label = "BorderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) SelectedCardBackground else CardBackground,
        animationSpec = tween(250),
        label = "CardBackground"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .animateContentSize(animationSpec = spring(stiffness = 500f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Tag & Privilege Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tier.accentColor.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = tier.tag,
                        color = tier.accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = tier.privilegeTag,
                    color = InkMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier Title
            Text(
                text = tier.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tier.subtitle,
                color = InkMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            // Dynamic Sub-Dashboard Expanded When Selected
            if (isSelected) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                when (tier.tierId) {
                    // Linux Kernel: Real-time Gantt, Memory Matrix, and Diagnostics
                    3 -> {
                        LinuxKernelSubDashboard(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                    // Android Framework: Binder IPC Tracer
                    1 -> {
                        BinderTracerCanvas(
                            tracerState = uiState.binderTracer,
                            onTriggerTransaction = {
                                viewModel.triggerBinderTransaction()
                            }
                        )
                    }
                    // Userspace Apps
                    0 -> {
                        UserspaceAppsSubDashboard()
                    }
                    // HAL Layer
                    2 -> {
                        HalSubDashboard()
                    }
                }
            }
        }
    }
}

/**
 * Linux Kernel Sub-Dashboard featuring the Thread Scheduling Gantt chart,
 * Memory Space Visualizer, and System Diagnostics Panel.
 */
@Composable
private fun LinuxKernelSubDashboard(
    uiState: KernelSimulationState,
    viewModel: KernelSimulationViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Governor Mode Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CpuGovernorMode.entries.forEach { mode ->
                val isSelected = uiState.governorMode == mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) StudioOrange else Color(0xFF22222E),
                    border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setGovernor(mode) }
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

        // Thermal Throttling Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1E28))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thermal Dissipation (${uiState.temperatureCelsius.toInt()}°C):",
                    color = InkMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = if (uiState.isThermalThrottling) "THROTTLING ACTIVE (-35% CAP)" else "NOMINAL ENVELOPE",
                    color = if (uiState.isThermalThrottling) Color(0xFFE53935) else Color(0xFF4CAF50),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = uiState.temperatureCelsius,
                onValueChange = { viewModel.setTemperature(it) },
                valueRange = 34f..54f,
                colors = SliderDefaults.colors(
                    thumbColor = if (uiState.isThermalThrottling) Color(0xFFE53935) else StudioOrange,
                    activeTrackColor = if (uiState.isThermalThrottling) Color(0xFFE53935) else StudioOrange,
                    inactiveTrackColor = Color(0xFF333340)
                ),
                modifier = Modifier.height(26.dp)
            )
        }

        // A. CPU Thread Scheduling Gantt chart
        CpuSchedulerGraph(
            coreTracks = uiState.coreTracks,
            selectedSlice = uiState.selectedSlice,
            onSliceSelected = { viewModel.selectProcessSlice(it) }
        )

        // B. Memory Space Visualizer (Split matrix)
        MemorySpaceCanvas(
            memoryBlocks = uiState.memoryBlocks,
            userspaceUsedMb = uiState.userspaceUsedMb,
            kernelUsedMb = uiState.kernelUsedMb,
            zramCompressedMb = uiState.zramCompressedMb,
            pageCacheHitRatePct = uiState.pageCacheHitRatePct,
            pageFaultsPerSec = uiState.pageFaultsPerSec,
            onAppLaunchClick = { viewModel.simulateAppLaunch() },
            onHeavyTaskClick = { viewModel.simulateHeavyWorkload() },
            onTrimMemoryClick = { viewModel.simulateMemoryTrim() }
        )

        // C. Live System Diagnostics Panel
        SystemDiagnosticsPanel(uiState = uiState)
    }
}

/**
 * System Diagnostics panel showing Schedutil, Active Threads, Memory, and Kernel Version.
 */
@Composable
private fun SystemDiagnosticsPanel(uiState: KernelSimulationState) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16161C),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "LIVE SYSTEM DIAGNOSTICS",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            DiagnosticMetricRow(label = "CPU Scheduler", value = "${uiState.governorMode.displayName} (EAS)")
            DiagnosticMetricRow(label = "Active Runqueues", value = "142 Threads (8 Running, 134 Sleeping)")
            DiagnosticMetricRow(label = "System Memory", value = "${(uiState.userspaceUsedMb + uiState.kernelUsedMb) / 1024f} GB / 12.0 GB used • ZRAM ${uiState.zramCompressedMb}MB")
            DiagnosticMetricRow(label = "Kernel Version", value = "Linux 6.1.43-android14-11-g89b2")
            DiagnosticMetricRow(
                label = "Thermal Envelope",
                value = "${String.format("%.1f", uiState.temperatureCelsius)}°C • ${if (uiState.isThermalThrottling) "Throttling" else "Nominal"}"
            )
        }
    }
}

@Composable
private fun DiagnosticMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = InkMuted,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Userspace Apps Preview sub-dashboard.
 */
@Composable
private fun UserspaceAppsSubDashboard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E28),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "COMPOSE DECLARATIVE RENDERING",
                color = Color(0xFFE8A020),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Compose runtime observes Snapshot State changes and emits Skia draw instructions directly into the hardware-accelerated RenderThread, achieving zero layout jank.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Hardware Abstraction Layer sub-dashboard.
 */
@Composable
private fun HalSubDashboard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E28),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "STABLE AIDL HARDWARE BOUNDARY",
                color = Color(0xFF4E7F9E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Decouples system partitions from vendor hardware drivers. SurfaceFlinger HWC3 interfaces directly with display overlays with zero CPU copy overhead.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Bottom Floating Dock with "Stack Overview", "Layer Detail", and "Reset Session" controls.
 */
@Composable
private fun BottomFloatingDock(
    activeAction: String,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf("Stack Overview", "Layer Detail", "Reset Session")

    Surface(
        shape = CircleShape,
        color = Color(0xFF181822).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                val isSelected = action == activeAction
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) StudioOrange else Color.Transparent,
                    modifier = Modifier.clickable { onActionSelected(action) }
                ) {
                    Text(
                        text = action,
                        color = if (isSelected) Color.White else InkMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
