package com.kernelcraft.feature.detail.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Charging protocols simulated in the battery and thermal simulator.
 */
enum class ChargeMode(
    val title: String,
    val defaultWattage: Float,
    val efficiency: Float,
    val maxTempCeiling: Float,
    val description: String
) {
    QI_WIRELESS(
        title = "15W Qi Wireless",
        defaultWattage = 15f,
        efficiency = 0.74f,
        maxTempCeiling = 38.5f,
        description = "Resonant inductive coupling through ferrite backing shield"
    ),
    WIRED_FAST(
        title = "45W SuperCharge",
        defaultWattage = 45f,
        efficiency = 0.92f,
        maxTempCeiling = 43.0f,
        description = "Direct dual-charge pump injection into series cells"
    ),
    WIRED_EXTREME(
        title = "65W Turbo Injection",
        defaultWattage = 65f,
        efficiency = 0.89f,
        maxTempCeiling = 47.5f,
        description = "Maximum current draw with active phase-change dissipation"
    )
}

/**
 * Thermal and battery electrochemical equations.
 */
object BatteryThermalMath {
    fun computeChassisTemperature(ambientTemp: Float, wattage: Float, efficiency: Float, hasVaporChamber: Boolean): Float {
        val heatDissipatedWatts = wattage * (1f - efficiency)
        val thermalResistance = if (hasVaporChamber) 0.32f else 0.75f // °C / Watt
        return ambientTemp + (heatDissipatedWatts * thermalResistance) + (wattage * 0.12f)
    }

    fun computeTimeToFullMinutes(wattage: Float, efficiency: Float, batteryCapacityWh: Float = 19.25f): Int {
        val effectivePower = (wattage * efficiency).coerceAtLeast(2f)
        val hours = batteryCapacityWh / effectivePower
        return (hours * 60f * 1.15f).toInt().coerceAtLeast(15) // +15% constant voltage tail
    }

    fun compute800CycleRetentionPercent(wattage: Float, temperatureCelsius: Float): Float {
        // High thermal stress and high C-rates accelerate SEI layer growth and capacity fade
        val thermalDegradation = ((temperatureCelsius - 30f).coerceAtLeast(0f) * 0.35f)
        val cRateDegradation = (wattage / 65f) * 4.5f
        return (92.5f - thermalDegradation - cRateDegradation).coerceIn(75f, 96f)
    }
}

/**
 * Visual inspection of the Dual-Cell Li-ion pack and the Vapor Chamber cooling pad.
 *
 * Renders:
 * - Dynamic thermal dissipation heatmap with outward vector arrows spreading across chassis.
 * - Dual-cell series battery layout with real-time balance metrics.
 * - Sintered powder capillary wick and two-phase phase-change loop in X-Ray mode.
 * - Interactive charge-cycle wattage scrubber with reactive temperature & degradation models.
 */
@Composable
fun BatteryThermalsView(
    componentId: String,
    isXRayMode: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(ChargeMode.WIRED_FAST) }
    var wattageInput by remember(selectedMode) { mutableFloatStateOf(selectedMode.defaultWattage) }

    // Wave animation for thermal vector spreading
    val transition = rememberInfiniteTransition(label = "ThermalPulse")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val currentTemp = remember(wattageInput, selectedMode) {
        BatteryThermalMath.computeChassisTemperature(
            ambientTemp = 24.5f,
            wattage = wattageInput,
            efficiency = selectedMode.efficiency,
            hasVaporChamber = true
        )
    }
    val timeToFull = remember(wattageInput, selectedMode) {
        BatteryThermalMath.computeTimeToFullMinutes(wattageInput, selectedMode.efficiency)
    }
    val retention800 = remember(wattageInput, currentTemp) {
        BatteryThermalMath.compute800CycleRetentionPercent(wattageInput, currentTemp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141418))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Charging Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E26))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChargeMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFDD5622) else Color.Transparent)
                        .clickable { selectedMode = mode }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) Color.White else Color(0xFF9E9EAA),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Thermal & Electrochemical Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF09090D))
                .border(BorderStroke(1.dp, Color(0xFF262634)), RoundedCornerShape(14.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                val w = size.width
                val h = size.height

                drawThermalHeatmap(
                    width = w,
                    height = h,
                    wattage = wattageInput,
                    tempCelsius = currentTemp,
                    wavePhase = wavePhase,
                    isXRay = isXRayMode,
                    isWireless = selectedMode == ChargeMode.QI_WIRELESS
                )
            }

            // Temperature Live Gauge Overlay
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xDD000000),
                border = BorderStroke(
                    1.dp,
                    if (currentTemp > 44f) Color(0xFFE53935) else if (currentTemp > 38f) Color(0xFFE8A020) else Color(0xFF4CAF50)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentTemp > 44f) Color(0xFFE53935) else if (currentTemp > 38f) Color(0xFFE8A020) else Color(0xFF4CAF50)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.1f", currentTemp)}°C",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Charging Wattage Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Simulate Charging Current / Wattage:",
                color = Color(0xFF9E9EAA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${wattageInput.toInt()} W",
                color = Color(0xFFE8A020),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = wattageInput,
            onValueChange = { wattageInput = it },
            valueRange = 5f..65f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFDD5622),
                activeTrackColor = Color(0xFFDD5622),
                inactiveTrackColor = Color(0xFF2B2B36)
            ),
            modifier = Modifier.height(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Telemetry Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThermalsPill(
                label = "Full Charge In",
                value = "$timeToFull min",
                subtitle = "0% -> 100%",
                modifier = Modifier.weight(1f)
            )
            ThermalsPill(
                label = "800-Cycle Health",
                value = "${String.format("%.1f", retention800)}%",
                subtitle = "Retention",
                modifier = Modifier.weight(1f)
            )
            ThermalsPill(
                label = "Vapor Chamber",
                value = "4,200 mm²",
                subtitle = "Active Copper",
                modifier = Modifier.weight(1f)
            )
            ThermalsPill(
                label = "Cell Config",
                value = "2S 2500mAh",
                subtitle = "8.8V Series",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThermalsPill(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1B1B22),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(text = label, color = Color(0xFF888896), fontSize = 9.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(text = subtitle, color = Color(0xFFDD5622), fontSize = 8.sp)
        }
    }
}

/**
 * Draws the composite thermal dissipation heatmap and dual pouch cell matrix.
 */
private fun DrawScope.drawThermalHeatmap(
    width: Float,
    height: Float,
    wattage: Float,
    tempCelsius: Float,
    wavePhase: Float,
    isXRay: Boolean,
    isWireless: Boolean
) {
    val batteryX = width * 0.06f
    val batteryY = height * 0.12f
    val batteryW = width * 0.44f
    val batteryH = height * 0.76f

    val vcX = width * 0.54f
    val vcY = height * 0.12f
    val vcW = width * 0.40f
    val vcH = height * 0.76f

    // 1. Draw Dual-Cell Pouch Battery (Left Column)
    val cellGap = 8f
    val singleCellH = (batteryH - cellGap) / 2f

    // Cell A (Top)
    drawRect(
        color = Color(0xFF1E1E28),
        topLeft = Offset(batteryX, batteryY),
        size = Size(batteryW, singleCellH)
    )
    // Cell B (Bottom)
    drawRect(
        color = Color(0xFF1E1E28),
        topLeft = Offset(batteryX, batteryY + singleCellH + cellGap),
        size = Size(batteryW, singleCellH)
    )

    // Battery Terminals
    drawRect(color = Color(0xFFFFB300), topLeft = Offset(batteryX + 10f, batteryY - 6f), size = Size(18f, 6f))
    drawRect(color = Color(0xFFFFB300), topLeft = Offset(batteryX + batteryW - 28f, batteryY - 6f), size = Size(18f, 6f))

    // Cell balance indicators & borders
    drawRect(
        color = if (isXRay) Color(0xFF00E5FF) else Color(0x44FFFFFF),
        topLeft = Offset(batteryX, batteryY),
        size = Size(batteryW, singleCellH),
        style = Stroke(1.5f)
    )
    drawRect(
        color = if (isXRay) Color(0xFF00E5FF) else Color(0x44FFFFFF),
        topLeft = Offset(batteryX, batteryY + singleCellH + cellGap),
        size = Size(batteryW, singleCellH),
        style = Stroke(1.5f)
    )

    // Current flow dynamic dots
    val flowOffset = (wavePhase * singleCellH) % singleCellH
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 3f,
        center = Offset(batteryX + batteryW / 2f, batteryY + flowOffset)
    )
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 3f,
        center = Offset(batteryX + batteryW / 2f, batteryY + singleCellH + cellGap + flowOffset)
    )

    // 2. Draw Vapor Chamber Spreader (Right Column)
    // Copper baseplate
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFB85E32), Color(0xFF8D4321)),
            startY = vcY,
            endY = vcY + vcH
        ),
        topLeft = Offset(vcX, vcY),
        size = Size(vcW, vcH)
    )
    drawRect(
        color = Color(0x66FFFFFF),
        topLeft = Offset(vcX, vcY),
        size = Size(vcW, vcH),
        style = Stroke(1.5f)
    )

    // Hotspot Origin (SoC contact area)
    val hotspotX = vcX + vcW * 0.45f
    val hotspotY = vcY + vcH * 0.35f
    val maxHeatRadius = vcW * (0.5f + (wattage / 65f) * 0.6f)

    // Thermal Heatmap Radial Gradient
    val heatColor = when {
        tempCelsius > 44f -> Color(0xFFE53935)
        tempCelsius > 38f -> Color(0xFFE8A020)
        else -> Color(0xFF43A047)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                heatColor.copy(alpha = 0.85f),
                Color(0xFFFF9800).copy(alpha = 0.50f),
                Color(0xFF2196F3).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(hotspotX, hotspotY),
            radius = maxHeatRadius
        ),
        center = Offset(hotspotX, hotspotY),
        radius = maxHeatRadius
    )

    // Thermal Wavefront Rings spreading out
    val waveRadius = (wavePhase * maxHeatRadius) % maxHeatRadius
    drawCircle(
        color = Color.White.copy(alpha = (1f - (waveRadius / maxHeatRadius)) * 0.6f),
        radius = waveRadius,
        center = Offset(hotspotX, hotspotY),
        style = Stroke(1.5f)
    )

    // 3. In X-Ray Mode: draw Sintered Copper Capillary Wick Matrix
    if (isXRay) {
        val wickRows = 6
        val wickCols = 5
        val dx = vcW / (wickCols + 1)
        val dy = vcH / (wickRows + 1)

        for (r in 1..wickRows) {
            for (c in 1..wickCols) {
                val cx = vcX + c * dx
                val cy = vcY + r * dy
                drawCircle(color = Color(0x44FFD54F), radius = 1.5f, center = Offset(cx, cy))
            }
        }

        // Dual NTC Thermistors
        val ntc1 = Offset(batteryX + batteryW * 0.8f, batteryY + singleCellH - 12f)
        val ntc2 = Offset(hotspotX, hotspotY + 30f)
        drawCircle(color = Color(0xFFE91E63), radius = 4f, center = ntc1)
        drawCircle(color = Color(0xFFE91E63), radius = 4f, center = ntc2)
    }

    // 4. Qi Wireless Coil Overlay (if Qi wireless mode active)
    if (isWireless) {
        val coilCenter = Offset(batteryX + batteryW * 0.85f, batteryY + batteryH * 0.5f)
        for (ring in 1..3) {
            drawCircle(
                color = Color(0xFFE8A020).copy(alpha = 0.7f),
                radius = ring * 12f,
                center = coilCenter,
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            )
        }
    }
}
