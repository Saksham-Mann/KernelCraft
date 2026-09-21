package com.kernelcraft.feature.kernel.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class GanttThreadLane(
    val id: String,
    val processName: String,
    val threadTag: String,
    val assignedCore: String,
    val priority: String,
    val slices: List<GanttSlice>
)

@Immutable
data class GanttSlice(
    val startOffsetRatio: Float, // 0.0f..1.0f along time window
    val durationRatio: Float,
    val state: ThreadExecutionState
)

enum class ThreadExecutionState(val label: String, val color: Color) {
    RUNNING("Running", Color(0xFFE8A020)),       // Mustard
    WAITING("Wait / I/O", Color(0xFF4E7F9E)),    // Steel Blue
    PREEMPTED("Preempted", Color(0xFFB0223F))    // Crimson
}

/**
 * Real-time CPU Thread Scheduling Gantt chart.
 * Depicts dynamic task dispatch across Process A (UI Thread), Process B (Media Codec),
 * and Process C (SurfaceFlinger) across a sliding time window.
 */
@Composable
fun ThreadSchedulingGanttChart(
    modifier: Modifier = Modifier
) {
    val lanes = listOf(
        GanttThreadLane(
            id = "lane_a",
            processName = "Process A: com.kernelcraft.ui",
            threadTag = "UI Thread (Choreographer VSYNC)",
            assignedCore = "Core 0 (Cortex-X4 Prime)",
            priority = "SCHED_FIFO (rt)",
            slices = listOf(
                GanttSlice(0.00f, 0.22f, ThreadExecutionState.RUNNING),
                GanttSlice(0.22f, 0.15f, ThreadExecutionState.WAITING),
                GanttSlice(0.37f, 0.28f, ThreadExecutionState.RUNNING),
                GanttSlice(0.65f, 0.08f, ThreadExecutionState.PREEMPTED),
                GanttSlice(0.73f, 0.27f, ThreadExecutionState.RUNNING)
            )
        ),
        GanttThreadLane(
            id = "lane_b",
            processName = "Process B: media.codec",
            threadTag = "ExoPlayer All-Intra Seeker",
            assignedCore = "Core 1 (Cortex-A720 Perf)",
            priority = "SCHED_NORMAL (nice -4)",
            slices = listOf(
                GanttSlice(0.00f, 0.10f, ThreadExecutionState.WAITING),
                GanttSlice(0.10f, 0.32f, ThreadExecutionState.RUNNING),
                GanttSlice(0.42f, 0.18f, ThreadExecutionState.PREEMPTED),
                GanttSlice(0.60f, 0.25f, ThreadExecutionState.RUNNING),
                GanttSlice(0.85f, 0.15f, ThreadExecutionState.WAITING)
            )
        ),
        GanttThreadLane(
            id = "lane_c",
            processName = "Process C: surfaceflinger",
            threadTag = "HWC3 Overlay Compositor",
            assignedCore = "Core 2 (Cortex-A720 Perf)",
            priority = "SCHED_FIFO (rt 98)",
            slices = listOf(
                GanttSlice(0.00f, 0.18f, ThreadExecutionState.WAITING),
                GanttSlice(0.18f, 0.24f, ThreadExecutionState.RUNNING),
                GanttSlice(0.42f, 0.12f, ThreadExecutionState.WAITING),
                GanttSlice(0.54f, 0.30f, ThreadExecutionState.RUNNING),
                GanttSlice(0.84f, 0.16f, ThreadExecutionState.RUNNING)
            )
        )
    )

    // Animated sliding epoch playhead
    val infiniteTransition = rememberInfiniteTransition(label = "EpochSlide")
    val playheadXRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PlayheadPosition"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16161C))
            .padding(14.dp)
    ) {
        // Gantt Chart Title & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8A020))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CFS / EAS RUNQUEUE GANTT CHART",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "100ms TIME SLICE",
                color = Color(0xFF9E9EAA),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Process Lanes
        lanes.forEach { lane ->
            GanttLaneView(lane = lane, playheadXRatio = playheadXRatio)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Legend and Dispatch Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendChip(color = Color(0xFFE8A020), label = "RUNNING")
                LegendChip(color = Color(0xFF4E7F9E), label = "WAITING")
                LegendChip(color = Color(0xFFB0223F), label = "PREEMPTED")
            }
            Text(
                text = "PELT Load: 84%",
                color = Color(0xFF4CAF50),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun GanttLaneView(
    lane: GanttThreadLane,
    playheadXRatio: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lane.processName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${lane.assignedCore} • ${lane.priority}",
                color = Color(0xFF8E8E9E),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Canvas Lane Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF22222C))
        ) {
            val totalWidth = size.width
            val height = size.height

            // Render execution slices
            lane.slices.forEach { slice ->
                val startX = slice.startOffsetRatio * totalWidth
                val sliceWidth = slice.durationRatio * totalWidth

                drawRoundRect(
                    color = slice.state.color,
                    topLeft = Offset(startX, 0f),
                    size = Size(sliceWidth, height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // Real-time Epoch Needle
            val needleX = playheadXRatio * totalWidth
            drawLine(
                color = Color.White,
                start = Offset(needleX, 0f),
                end = Offset(needleX, height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color(0xFF8E8E9E),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
