package com.kernelcraft.feature.kernel.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kernelcraft.feature.kernel.MemoryPageBlock

private val UserspaceColor = Color(0xFFE8A020)
private val KernelColor = Color(0xFF4E7F9E)
private val ZramColor = Color(0xFFFF7033)
private val FreeSlotColor = Color(0xFF22222E)
private val FlashColor = Color(0xFFFFFFFF)

/**
 * Interactive Memory Allocation Matrix Canvas.
 *
 * Renders a 32-block grid representing Physical RAM vs Kernel Space allocation,
 * with animated flashing on allocation/deallocation and workload simulation triggers.
 */
@Composable
fun MemorySpaceCanvas(
    memoryBlocks: List<MemoryPageBlock>,
    userspaceUsedMb: Int,
    kernelUsedMb: Int,
    zramCompressedMb: Int,
    pageCacheHitRatePct: Int,
    pageFaultsPerSec: Int,
    onAppLaunchClick: () -> Unit,
    onHeavyTaskClick: () -> Unit,
    onTrimMemoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlashPulse")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MemoryFlash"
    )

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
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEMORY ALLOCATION MATRIX",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "${(userspaceUsedMb + kernelUsedMb) / 1024f}GB / 12GB USED",
                color = Color(0xFF9E9EAA),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 32-Block Memory Page Grid Canvas (16 Userspace left, 16 Kernel right)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E28))
                .padding(8.dp)
        ) {
            val totalWidth = size.width
            val totalHeight = size.height
            val cols = 8
            val rows = 4
            val cellW = (totalWidth - (cols - 1) * 4.dp.toPx()) / cols
            val cellH = (totalHeight - (rows - 1) * 4.dp.toPx()) / rows
            val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

            memoryBlocks.forEachIndexed { index, block ->
                val col = index % cols
                val row = index / cols
                val x = col * (cellW + 4.dp.toPx())
                val y = row * (cellH + 4.dp.toPx())

                val baseColor = when {
                    block.isZramSwapped -> ZramColor
                    !block.isAllocated -> FreeSlotColor
                    block.isKernelSpace -> KernelColor
                    else -> UserspaceColor
                }

                // Render Page Block
                drawRoundRect(
                    color = baseColor,
                    topLeft = Offset(x, y),
                    size = Size(cellW, cellH),
                    cornerRadius = cornerRadius
                )

                // Render Flash Highlight on State Transition
                if (block.isFlashing) {
                    drawRoundRect(
                        color = FlashColor.copy(alpha = flashAlpha),
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Domain Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot(color = UserspaceColor, label = "Userspace ($userspaceUsedMb MB)")
                LegendDot(color = KernelColor, label = "Kernel Space ($kernelUsedMb MB)")
                LegendDot(color = ZramColor, label = "zRAM ($zramCompressedMb MB)")
            }
            Text(
                text = "$pageCacheHitRatePct% Hit • $pageFaultsPerSec flt/s",
                color = Color(0xFF4CAF50),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Workload Simulation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAppLaunchClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262634)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = "App Launch",
                    color = UserspaceColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onHeavyTaskClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262634)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = "Heavy Task",
                    color = ZramColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onTrimMemoryClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262634)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = "Trim Memory",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
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
            color = Color(0xFF9E9EAA),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
