package com.kernelcraft.feature.kernel.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.kernel.BinderSimulationState
import com.kernelcraft.feature.kernel.BinderTracerState
import com.kernelcraft.feature.kernel.BinderTransactionPhase

private val Mustard = Color(0xFFE8A020)
private val KernelBlue = Color(0xFF4E7F9E)
private val ServerGreen = Color(0xFF4CAF50)

/**
 * Android Framework Tier: Binder IPC Tracer Canvas.
 *
 * Visualizes transactional message passing across App Process -> /dev/binder -> System Server,
 * with real-time animated packet pulses and microsecond latency readouts.
 */
@Composable
fun BinderTracerCanvas(
    tracerState: BinderTracerState,
    onTriggerTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseIdle")
    val idlePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
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
                        .background(Mustard)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BINDER IPC INTER-PROCESS TRACER",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "${tracerState.transactionCount} TX • ${tracerState.latencyMicros}µs",
                color = Mustard,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Tier Interactive IPC Node Diagram
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E28))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Node 1: Client Process
            TracerNode(
                badge = "USERSPACE CLIENT",
                name = tracerState.clientPid,
                detail = "Proxy: android.view.IWindowSession",
                isActive = tracerState.phase == BinderTransactionPhase.MARSHALLING || tracerState.phase == BinderTransactionPhase.RETURNED,
                accentColor = Mustard
            )

            // Canvas Connector 1 (Client -> Kernel)
            TracerBusCanvas(
                progress = tracerState.inFlightProgress,
                activeRange = 0.1f..0.5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            // Node 2: Kernel Space /dev/binder
            TracerNode(
                badge = "LINUX KERNEL (RING 0)",
                name = "ioctl(/dev/binder, BINDER_WRITE_READ)",
                detail = "Single-copy mmap() transfer • IPC thread pool dispatch",
                isActive = tracerState.phase == BinderTransactionPhase.KERNEL_IN_FLIGHT,
                accentColor = KernelBlue
            )

            // Canvas Connector 2 (Kernel -> System Server)
            TracerBusCanvas(
                progress = tracerState.inFlightProgress,
                activeRange = 0.5f..0.95f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            // Node 3: System Server
            TracerNode(
                badge = "SYSTEM SERVER (UID 1000)",
                name = tracerState.targetPid,
                detail = "Stub: WindowManagerService / SurfaceFlinger",
                isActive = tracerState.phase == BinderTransactionPhase.DISPATCHED_IN_SERVICE,
                accentColor = ServerGreen
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction Trigger Button & Payload Descriptor
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF222230),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tracerState.phase.stepName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tracerState.activePayload,
                        color = Color(0xFF9E9EAA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onTriggerTransaction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mustard),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Send IPC",
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
private fun TracerNode(
    badge: String,
    name: String,
    detail: String,
    isActive: Boolean,
    accentColor: Color
) {
    val bgColor = if (isActive) accentColor.copy(alpha = 0.18f) else Color(0xFF242432)
    val borderColor = if (isActive) accentColor else Color.White.copy(alpha = 0.08f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = badge,
                color = if (isActive) accentColor else Color(0xFF9E9EAA),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = Color(0xFF8E8E9E),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun TracerBusCanvas(
    progress: Float,
    activeRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val height = size.height

        // Dashed connection line
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(centerX, 0f),
            end = Offset(centerX, height),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )

        // Animated traveling packet
        if (progress in activeRange) {
            val normalizedRatio = (progress - activeRange.start) / (activeRange.endInclusive - activeRange.start)
            val packetY = height * normalizedRatio

            drawCircle(
                color = Mustard.copy(alpha = 0.35f),
                radius = 8.dp.toPx(),
                center = Offset(centerX, packetY)
            )
            drawCircle(
                color = Mustard,
                radius = 4.dp.toPx(),
                center = Offset(centerX, packetY)
            )
        }
    }
}
