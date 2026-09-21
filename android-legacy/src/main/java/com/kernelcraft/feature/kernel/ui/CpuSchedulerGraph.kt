package com.kernelcraft.feature.kernel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.kernel.ClusterType
import com.kernelcraft.feature.kernel.CoreTrack
import com.kernelcraft.feature.kernel.ProcessSlice

private val StudioOrange = Color(0xFFDD5622)
private val EdgeGlowOrange = Color(0xFFFF7033)
private val PerfBlue = Color(0xFF4E7F9E)
private val EffSlate = Color(0xFF5A6678)
private val TrackBackground = Color(0xFF1F1F2A)
private val SelectedBorder = Color(0xFFFFFFFF)

/**
 * Interactive Thread Scheduling Canvas.
 *
 * Real-time scrolling horizontal Gantt chart rendering live process execution slices
 * across Prime, Performance, and Efficiency CPU clusters.
 * Zero-allocation drawing loop eliminates GC pressure during active 60 FPS updates.
 */
@Composable
fun CpuSchedulerGraph(
    coreTracks: List<CoreTrack>,
    selectedSlice: ProcessSlice?,
    onSliceSelected: (ProcessSlice?) -> Unit,
    modifier: Modifier = Modifier
) {
    val laneHeightDp = 24.dp
    val laneSpacingDp = 10.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16161C))
            .padding(14.dp)
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
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StudioOrange)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CFS / EAS RUNQUEUE GANTT",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "TAP PROCESS BAR TO INSPECT",
                color = Color(0xFF9E9EAA),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Horizontal Canvas Gantt Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(coreTracks) {
                    detectTapGestures { tapOffset ->
                        val trackCount = coreTracks.size
                        if (trackCount == 0) return@detectTapGestures

                        val totalHeight = size.height
                        val laneHeight = totalHeight / trackCount
                        val trackIndex = (tapOffset.y / laneHeight).toInt().coerceIn(0, trackCount - 1)
                        val track = coreTracks[trackIndex]

                        val tapRatioX = tapOffset.x / size.width

                        // Find matching slice in track
                        val hitSlice = track.activeSlices.find { slice ->
                            val start = slice.startOffsetRatio
                            val end = (slice.startOffsetRatio + slice.durationRatio)
                            if (end <= 1.0f) {
                                tapRatioX in start..end
                            } else {
                                // Wrapped slice
                                tapRatioX >= start || tapRatioX <= (end - 1.0f)
                            }
                        }

                        onSliceSelected(hitSlice)
                    }
                }
        ) {
            val totalWidth = size.width
            val totalHeight = size.height
            val trackCount = coreTracks.size
            if (trackCount == 0) return@Canvas

            val laneHeight = (totalHeight - ((trackCount - 1) * 8.dp.toPx())) / trackCount
            val cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            val selectedStroke = Stroke(width = 2.dp.toPx())

            coreTracks.forEachIndexed { index, track ->
                val laneY = index * (laneHeight + 8.dp.toPx())

                // 1. Lane Track Background
                drawRoundRect(
                    color = TrackBackground,
                    topLeft = Offset(0f, laneY),
                    size = Size(totalWidth, laneHeight),
                    cornerRadius = cornerRadius
                )

                // 2. Execution Slices
                val clusterColor = when (track.clusterType) {
                    ClusterType.PRIME -> StudioOrange
                    ClusterType.PERFORMANCE -> PerfBlue
                    ClusterType.EFFICIENCY -> EffSlate
                }

                track.activeSlices.forEach { slice ->
                    val isSelected = selectedSlice?.id == slice.id
                    val startX = slice.startOffsetRatio * totalWidth
                    val sliceWidth = slice.durationRatio * totalWidth

                    // Handle wrapping around screen edge cleanly
                    if (startX + sliceWidth <= totalWidth) {
                        drawRoundRect(
                            color = if (isSelected) EdgeGlowOrange else clusterColor,
                            topLeft = Offset(startX, laneY),
                            size = Size(sliceWidth, laneHeight),
                            cornerRadius = cornerRadius
                        )
                        if (isSelected) {
                            drawRoundRect(
                                color = SelectedBorder,
                                topLeft = Offset(startX, laneY),
                                size = Size(sliceWidth, laneHeight),
                                cornerRadius = cornerRadius,
                                style = selectedStroke
                            )
                        }
                    } else {
                        // Part 1
                        val firstWidth = totalWidth - startX
                        drawRoundRect(
                            color = if (isSelected) EdgeGlowOrange else clusterColor,
                            topLeft = Offset(startX, laneY),
                            size = Size(firstWidth, laneHeight),
                            cornerRadius = cornerRadius
                        )
                        // Part 2 (Wrapped)
                        val secondWidth = sliceWidth - firstWidth
                        drawRoundRect(
                            color = if (isSelected) EdgeGlowOrange else clusterColor,
                            topLeft = Offset(0f, laneY),
                            size = Size(secondWidth, laneHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Core Labels & Frequencies
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            coreTracks.forEach { track ->
                val clusterColor = when (track.clusterType) {
                    ClusterType.PRIME -> StudioOrange
                    ClusterType.PERFORMANCE -> PerfBlue
                    ClusterType.EFFICIENCY -> EffSlate
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(clusterColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${track.name.take(10)} • ${String.format("%.2f", track.currentFreqGhz)}GHz",
                        color = Color(0xFFB0B0C0),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Selected Slice Telemetry Inspector Card
        if (selectedSlice != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF222230),
                border = BorderStroke(1.dp, StudioOrange.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedSlice.processName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioOrange.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "nice: ${selectedSlice.niceValue} (vruntime: +${selectedSlice.vruntimeDeltaUs}µs)",
                                color = StudioOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${selectedSlice.threadTag} • Slice: ${selectedSlice.durationMs}ms on ${selectedSlice.clusterType.label}",
                        color = Color(0xFF9E9EAA),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
