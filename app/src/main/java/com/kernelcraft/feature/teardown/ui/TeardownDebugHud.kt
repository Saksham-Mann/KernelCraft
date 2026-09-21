package com.kernelcraft.feature.teardown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.core.player.PlayerMetrics

private val HudBackground = Color(0xDD0D0D0D)
private val BorderDark = Color(0x33FFFFFF)
private val TextWhite = Color(0xFFEEEEEE)
private val TextMuted = Color(0xFF888888)
private val GreenAccent = Color(0xFF4CAF50)
private val AmberAccent = Color(0xFFFFB300)
private val RedAccent = Color(0xFFF44336)

/**
 * Developer Telemetry HUD for 60/120Hz scrub profiling, MediaCodec latency,
 * and frame drop monitoring.
 */
@Composable
fun TeardownDebugHud(
    metrics: PlayerMetrics,
    progress: Float,
    velocity: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = HudBackground,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // HUD Header
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
                            .background(GreenAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KERNELCRAFT MEDIA3 TELEMETRY",
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (metrics.isHardwareAccelerated) GreenAccent.copy(alpha = 0.20f) else AmberAccent.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = if (metrics.isHardwareAccelerated) "HW CODEC" else "SW FALLBACK",
                        color = if (metrics.isHardwareAccelerated) GreenAccent else AmberAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricBox(
                    label = "FRAME INDEX",
                    value = "${metrics.currentFrame} / ${metrics.totalFrames}",
                    valueColor = TextWhite
                )

                val latencyColor = when {
                    metrics.seekLatencyMs <= 8L -> GreenAccent
                    metrics.seekLatencyMs <= 16L -> AmberAccent
                    else -> RedAccent
                }
                TelemetryMetricBox(
                    label = "SEEK LATENCY",
                    value = "${metrics.seekLatencyMs} ms",
                    valueColor = latencyColor
                )

                val dropColor = if (metrics.droppedFrames == 0) GreenAccent else RedAccent
                TelemetryMetricBox(
                    label = "FRAME DROPS",
                    value = "${metrics.droppedFrames}",
                    valueColor = dropColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Decoder Info & Velocity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Decoder: ${metrics.decoderName}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = "Velocity: ${(velocity * 100).toInt()}%/s",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricBox(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
