package com.kernelcraft.feature.silicon

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Immutable
data class SiliconBlockInfo(
    val id: String,
    val name: String,
    val clusterTag: String,
    val coresCount: Int,
    val maxClock: String,
    val cacheSize: String,
    val powerEnvelope: String,
    val description: String
)

/**
 * Interactive, stylized block architecture of the silicon die microarchitecture.
 * Renders CPU clusters (Prime, Performance, Efficiency), Adreno GPU, Hexagon NPU,
 * and LPDDR5X Memory Controller with simulated real-time clock pulses.
 */
@Composable
fun SiliconDieVisualizer(
    modifier: Modifier = Modifier,
    onBlockSelected: (SiliconBlockInfo) -> Unit = {}
) {
    val blocks = remember {
        listOf(
            SiliconBlockInfo(
                id = "prime_core",
                name = "Cortex-X4 Prime Core",
                clusterTag = "PRIME CPU",
                coresCount = 1,
                maxClock = "3.30 GHz",
                cacheSize = "2MB L2 + 8MB System Cache",
                powerEnvelope = "3.8W Peak",
                description = "Single-threaded heavy workload accelerator. Drives app launch bursts, UI rendering deadlines, and heavy compiler pipelines."
            ),
            SiliconBlockInfo(
                id = "perf_cores",
                name = "Cortex-A720 Cluster",
                clusterTag = "PERFORMANCE CPU",
                coresCount = 5,
                maxClock = "3.15 GHz",
                cacheSize = "5x 512KB L2 Cache",
                powerEnvelope = "2.1W / core",
                description = "Mid-tier compute workhorses. Runs Android runtime compilation, multi-threaded game loops, and media transcoding."
            ),
            SiliconBlockInfo(
                id = "efficiency_cores",
                name = "Cortex-A520 Cluster",
                clusterTag = "EFFICIENCY CPU",
                coresCount = 2,
                maxClock = "2.27 GHz",
                cacheSize = "2x 256KB L2 Cache",
                powerEnvelope = "0.45W / core",
                description = "Background task processors. Handles Linux kernel housekeeping, I/O dispatch, sensor polling, and audio playback with minimal energy."
            ),
            SiliconBlockInfo(
                id = "gpu_block",
                name = "Adreno 750 Graphics Engine",
                clusterTag = "GPU RENDERER",
                coresCount = 12,
                maxClock = "900 MHz",
                cacheSize = "Unified Shader Cache",
                powerEnvelope = "4.2W Peak",
                description = "Hardware-accelerated ray tracing unit and Vulkan 1.3 compositor. Powers 120Hz display refresh buffers and game geometry."
            ),
            SiliconBlockInfo(
                id = "npu_block",
                name = "Hexagon Neural Processor",
                clusterTag = "NPU ENGINE",
                coresCount = 8,
                maxClock = "45 TOPS INT8",
                cacheSize = "Direct DMA Vector Memory",
                powerEnvelope = "1.8W Active",
                description = "Fused vector-tensor accelerator running on-device LLMs, real-time photographic neural segmentation, and voice inference."
            ),
            SiliconBlockInfo(
                id = "mem_controller",
                name = "Quad-Channel LPDDR5X Controller",
                clusterTag = "MEMORY BUS",
                coresCount = 4,
                maxClock = "8533 Mbps",
                cacheSize = "64-bit Quad Channel",
                powerEnvelope = "1.1W Bandwidth Peak",
                description = "High-throughput interconnect linking the CPU/GPU/NPU subsystems to the PoP DRAM stack with <65ns latency."
            )
        )
    }

    var activeBlock by remember { mutableStateOf(blocks[0]) }

    val infiniteTransition = rememberInfiniteTransition(label = "SiliconPulse")
    val busPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BusPulse"
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
                        .background(Color(0xFFE8A020))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "4nm MONOLITHIC SILICON DIE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "TSMC N4P FinFET",
                color = Color(0xFF9E9E9E),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Die Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C1C))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Prime Core (Cortex-X4) + Efficiency Cluster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DieBlockTile(
                    block = blocks[0],
                    isSelected = activeBlock.id == blocks[0].id,
                    onClick = {
                        activeBlock = blocks[0]
                        onBlockSelected(blocks[0])
                    },
                    modifier = Modifier.weight(0.55f)
                )
                DieBlockTile(
                    block = blocks[2],
                    isSelected = activeBlock.id == blocks[2].id,
                    onClick = {
                        activeBlock = blocks[2]
                        onBlockSelected(blocks[2])
                    },
                    modifier = Modifier.weight(0.45f)
                )
            }

            // Row 2: Performance Cluster (5x Cortex-A720)
            DieBlockTile(
                block = blocks[1],
                isSelected = activeBlock.id == blocks[1].id,
                onClick = {
                    activeBlock = blocks[1]
                    onBlockSelected(blocks[1])
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Interconnect Bus Line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                drawRoundRect(
                    color = Color(0xFFE8A020).copy(alpha = busPulseAlpha),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            // Row 3: GPU + NPU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DieBlockTile(
                    block = blocks[3],
                    isSelected = activeBlock.id == blocks[3].id,
                    onClick = {
                        activeBlock = blocks[3]
                        onBlockSelected(blocks[3])
                    },
                    modifier = Modifier.weight(0.52f)
                )
                DieBlockTile(
                    block = blocks[4],
                    isSelected = activeBlock.id == blocks[4].id,
                    onClick = {
                        activeBlock = blocks[4]
                        onBlockSelected(blocks[4])
                    },
                    modifier = Modifier.weight(0.48f)
                )
            }

            // Row 4: Memory Controller
            DieBlockTile(
                block = blocks[5],
                isSelected = activeBlock.id == blocks[5].id,
                onClick = {
                    activeBlock = blocks[5]
                    onBlockSelected(blocks[5])
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Block Telemetry Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF242424),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeBlock.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeBlock.maxClock,
                        color = Color(0xFFE8A020),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${activeBlock.clusterTag} • ${activeBlock.cacheSize} • ${activeBlock.powerEnvelope}",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = activeBlock.description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun DieBlockTile(
    block: SiliconBlockInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFFE8A020) else Color.White.copy(alpha = 0.12f)
    val backgroundColor = if (isSelected) Color(0xFF28241C) else Color(0xFF222222)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = block.clusterTag,
                color = if (isSelected) Color(0xFFE8A020) else Color(0xFF9E9E9E),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = block.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
