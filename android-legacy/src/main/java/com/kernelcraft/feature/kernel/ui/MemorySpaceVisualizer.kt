package com.kernelcraft.feature.kernel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class MemorySegment(
    val id: String,
    val name: String,
    val spaceDomain: String, // "USERSPACE" or "KERNEL SPACE"
    val sizeMb: Int,
    val description: String,
    val color: Color
)

/**
 * Split matrix visualizer comparing Physical RAM layout vs Kernel Space allocation.
 */
@Composable
fun MemorySpaceVisualizer(
    modifier: Modifier = Modifier
) {
    val segments = remember {
        listOf(
            MemorySegment(
                id = "art_heap",
                name = "ART Managed Heap",
                spaceDomain = "USERSPACE (EL0)",
                sizeMb = 1250,
                description = "Concurrent Copying (CC) GC managed heap holding live Java and Kotlin Compose runtime objects.",
                color = Color(0xFFE8A020)
            ),
            MemorySegment(
                id = "native_heap",
                name = "Native Heap (jemalloc)",
                spaceDomain = "USERSPACE (EL0)",
                sizeMb = 850,
                description = "C++ allocations in Skia renderer, Vulkan pipeline pipelines, and ExoPlayer video buffer ring.",
                color = Color(0xFF4E7F9E)
            ),
            MemorySegment(
                id = "ashmem",
                name = "Ashmem / Graphic Buffers",
                spaceDomain = "USERSPACE (EL0)",
                sizeMb = 620,
                description = "Shared memory pools backing WindowManager surface layers passed to SurfaceFlinger.",
                color = Color(0xFF5E9C76)
            ),
            MemorySegment(
                id = "kernel_page_tables",
                name = "Kernel Image & Page Tables",
                spaceDomain = "KERNEL SPACE (RING 0)",
                sizeMb = 520,
                description = "Non-swappable Linux kernel text, translation lookaside buffers (TLB), and 4-level MMU page tables.",
                color = Color(0xFFB0223F)
            ),
            MemorySegment(
                id = "slab_cache",
                name = "Slab Allocator & DMA",
                spaceDomain = "KERNEL SPACE (RING 0)",
                sizeMb = 480,
                description = "Kernel kmalloc caches, socket buffers (sk_buff), dentries, and hardware camera DMA buffers.",
                color = Color(0xFF7E57C2)
            ),
            MemorySegment(
                id = "zram_swap",
                name = "ZRAM Swap Compaction",
                spaceDomain = "KERNEL SPACE (RING 0)",
                sizeMb = 648,
                description = "RAM-backed compressed block device. Holds 1.85 GB of cold anonymous memory compressed via LZ4.",
                color = Color(0xFFFF7033)
            )
        )
    }

    var selectedSegment by remember { mutableStateOf(segments[0]) }

    val totalUsedMb = segments.sumOf { it.sizeMb } // ~4368 MB (~4.3GB used)
    val totalRamMb = 12_288 // 12GB Physical RAM

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
                    text = "VIRTUAL MEMORY & RAM MATRIX",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "${totalUsedMb / 1024f}GB / 12GB USED",
                color = Color(0xFF9E9EAA),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Proportional Memory Split Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF262632))
        ) {
            segments.forEach { segment ->
                val weight = segment.sizeMb.toFloat() / totalRamMb
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(segment.color)
                        .clickable { selectedSegment = segment }
                )
            }
            // Free / Buffer Cache slice
            val freeWeight = (totalRamMb - totalUsedMb).toFloat() / totalRamMb
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(freeWeight)
                    .background(Color(0xFF2C2C38))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Segment Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.take(3).forEach { seg ->
                SegmentTagPill(
                    segment = seg,
                    isSelected = selectedSegment.id == seg.id,
                    onClick = { selectedSegment = seg },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.drop(3).forEach { seg ->
                SegmentTagPill(
                    segment = seg,
                    isSelected = selectedSegment.id == seg.id,
                    onClick = { selectedSegment = seg },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Segment Detail Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF20202A),
            border = BorderStroke(1.dp, selectedSegment.color.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedSegment.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedSegment.sizeMb} MB • ${selectedSegment.spaceDomain}",
                        color = selectedSegment.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedSegment.description,
                    color = Color(0xFFB0B0C0),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SegmentTagPill(
    segment: MemorySegment,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) segment.color.copy(alpha = 0.25f) else Color(0xFF1E1E28),
        border = BorderStroke(1.dp, if (isSelected) segment.color else Color.Transparent),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(segment.color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = segment.name.take(14),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
