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
import androidx.compose.runtime.Immutable
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Multi-camera sensor and optical group configurations.
 */
enum class CameraLensModule(
    val title: String,
    val focalLengthMm: Float,
    val apertureStr: String,
    val apertureFNumber: Float,
    val fovDegrees: Int,
    val sensorDesc: String,
    val lensElementsCount: Int,
    val isPeriscope: Boolean,
    val hasOis: Boolean
) {
    ULTRA_WIDE(
        title = "13mm Ultra-Wide",
        focalLengthMm = 13f,
        apertureStr = "f/2.2",
        apertureFNumber = 2.2f,
        fovDegrees = 120,
        sensorDesc = "12MP 1/2.55\" Sony IMX563",
        lensElementsCount = 5,
        isPeriscope = false,
        hasOis = false
    ),
    PRIMARY_OIS(
        title = "24mm Primary OIS",
        focalLengthMm = 24f,
        apertureStr = "f/1.8",
        apertureFNumber = 1.8f,
        fovDegrees = 84,
        sensorDesc = "50MP 1/1.28\" Sony IMX989",
        lensElementsCount = 7,
        isPeriscope = false,
        hasOis = true
    ),
    TELEPHOTO_PERISCOPE(
        title = "120mm 5x Periscope",
        focalLengthMm = 120f,
        apertureStr = "f/3.0",
        apertureFNumber = 3.0f,
        fovDegrees = 20,
        sensorDesc = "12MP 1/3.52\" Folded Prism OIS",
        lensElementsCount = 5,
        isPeriscope = true,
        hasOis = true
    )
}

/**
 * Optical calculations model for ray-tracing and aperture telemetry.
 */
object CameraOpticsMath {
    fun computeEntrancePupilMm(focalLengthMm: Float, fNumber: Float): Float {
        return if (fNumber > 0f) focalLengthMm / fNumber else 0f
    }

    fun computeHyperfocalDistanceMeters(focalLengthMm: Float, fNumber: Float, cocMm: Float = 0.015f): Float {
        return if (fNumber > 0f && cocMm > 0f) {
            (focalLengthMm * focalLengthMm) / (fNumber * cocMm * 1000f)
        } else 0f
    }

    fun computeSensorConvergenceY(
        entryYOffset: Float,
        opticalCenterY: Float,
        focusNormalized: Float,
        sensorX: Float,
        lensX: Float
    ): Float {
        // Focus adjusts refractive convergence towards optical center on the sensor plane
        val focusFactor = 0.15f + focusNormalized * 0.85f
        return opticalCenterY + (entryYOffset * (1f - focusFactor))
    }
}

/**
 * Interactive optical ray-trace visualizer for the smartphone camera subsystem.
 *
 * Renders:
 * - Multi-element aspherical lens stack (5P / 7P) with chromatic anti-reflective coatings.
 * - Dynamic light rays refracting through lens elements onto the CMOS image sensor.
 * - Iris aperture blades and 90° folded reflection periscope prism.
 * - Voice coil actuator magnets (VCM) and Bayer pattern in X-Ray mode.
 * - Focus distance scrubber and camera module switcher (Ultra-Wide, Primary OIS, Periscope).
 */
@Composable
fun CameraOpticsView(
    isXRayMode: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedModule by remember { mutableStateOf(CameraLensModule.PRIMARY_OIS) }
    var focusDistanceNorm by remember { mutableFloatStateOf(0.72f) } // 0 = 0.1m (macro), 1 = infinity

    // Infinite wave pulse animating light rays
    val transition = rememberInfiniteTransition(label = "RayTracePulse")
    val rayPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RayPhase"
    )

    val entrancePupil = remember(selectedModule) {
        CameraOpticsMath.computeEntrancePupilMm(selectedModule.focalLengthMm, selectedModule.apertureFNumber)
    }
    val hyperfocalMeters = remember(selectedModule) {
        CameraOpticsMath.computeHyperfocalDistanceMeters(selectedModule.focalLengthMm, selectedModule.apertureFNumber)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141418))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Module Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E26))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CameraLensModule.entries.forEach { module ->
                val isSelected = module == selectedModule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFDD5622) else Color.Transparent)
                        .clickable { selectedModule = module }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.title,
                        color = if (isSelected) Color.White else Color(0xFF9E9EAA),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Optical Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF09090D))
                .border(BorderStroke(1.dp, Color(0xFF262634)), RoundedCornerShape(14.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                val w = size.width
                val h = size.height
                val opticalCenterY = h / 2f

                // Optical Axis Dotted Line
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, opticalCenterY),
                    end = Offset(w, opticalCenterY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                if (selectedModule.isPeriscope) {
                    drawPeriscopeRaytrace(
                        width = w,
                        height = h,
                        opticalCenterY = opticalCenterY,
                        rayPhase = rayPhase,
                        focusNorm = focusDistanceNorm,
                        isXRay = isXRayMode
                    )
                } else {
                    drawStraightLensRaytrace(
                        width = w,
                        height = h,
                        opticalCenterY = opticalCenterY,
                        module = selectedModule,
                        rayPhase = rayPhase,
                        focusNorm = focusDistanceNorm,
                        isXRay = isXRayMode
                    )
                }
            }

            // Sensor Specs Overlay Chip
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xCC000000),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = selectedModule.sensorDesc,
                    color = Color(0xFFE8A020),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Focus Distance Scrubber
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Object Focus Distance:",
                color = Color(0xFF9E9EAA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            val focusLabel = when {
                focusDistanceNorm < 0.2f -> "Macro (0.1m)"
                focusDistanceNorm > 0.85f -> "Infinity (∞)"
                else -> "${String.format("%.1f", 0.2f + focusDistanceNorm * 3.5f)}m"
            }
            Text(
                text = focusLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = focusDistanceNorm,
            onValueChange = { focusDistanceNorm = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFDD5622),
                activeTrackColor = Color(0xFFDD5622),
                inactiveTrackColor = Color(0xFF2B2B36)
            ),
            modifier = Modifier.height(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Reactive Optical Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill(
                label = "Entrance Pupil",
                value = "${String.format("%.1f", entrancePupil)} mm",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "Hyperfocal",
                value = "${String.format("%.1f", hyperfocalMeters)} m",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "Field of View",
                value = "${selectedModule.fovDegrees}° FOV",
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "Aperture",
                value = selectedModule.apertureStr,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
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
        }
    }
}

/**
 * Renders standard multi-element coaxial lens group (5P / 7P) with light refraction.
 */
private fun DrawScope.drawStraightLensRaytrace(
    width: Float,
    height: Float,
    opticalCenterY: Float,
    module: CameraLensModule,
    rayPhase: Float,
    focusNorm: Float,
    isXRay: Boolean
) {
    val lensStackStartX = width * 0.18f
    val lensStackEndX = width * 0.76f
    val sensorX = width * 0.88f
    val numLenses = module.lensElementsCount
    val lensSpacing = (lensStackEndX - lensStackStartX) / (numLenses + 1)

    // 1. Draw Lens Elements
    for (i in 1..numLenses) {
        val lensX = lensStackStartX + i * lensSpacing
        val lensHeight = height * (0.40f + (0.35f * (numLenses - i) / numLenses))
        val lensThickness = 10f

        val lensPath = Path().apply {
            moveTo(lensX - lensThickness / 2f, opticalCenterY - lensHeight / 2f)
            // Left curved surface
            quadraticTo(
                lensX - lensThickness * 1.5f, opticalCenterY,
                lensX - lensThickness / 2f, opticalCenterY + lensHeight / 2f
            )
            lineTo(lensX + lensThickness / 2f, opticalCenterY + lensHeight / 2f)
            // Right curved surface
            quadraticTo(
                lensX + lensThickness * 1.5f, opticalCenterY,
                lensX + lensThickness / 2f, opticalCenterY - lensHeight / 2f
            )
            close()
        }

        // Lens body with anti-reflective optical sheen
        drawPath(
            path = lensPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x3300E5FF),
                    Color(0x22E040FB),
                    Color(0x4400E5FF)
                ),
                start = Offset(lensX, opticalCenterY - lensHeight / 2f),
                end = Offset(lensX, opticalCenterY + lensHeight / 2f)
            )
        )
        drawPath(
            path = lensPath,
            color = if (isXRay) Color(0xAA00E5FF) else Color(0x66FFFFFF),
            style = Stroke(width = 1.5f)
        )

        // Draw mechanical aperture iris between middle elements
        if (i == numLenses / 2) {
            val irisApertureHeight = 24f * (module.apertureFNumber / 1.8f)
            // Upper blade
            drawRect(
                color = Color(0xFFDD5622),
                topLeft = Offset(lensX - 2f, opticalCenterY - lensHeight / 2f),
                size = Size(4f, (lensHeight / 2f) - irisApertureHeight)
            )
            // Lower blade
            drawRect(
                color = Color(0xFFDD5622),
                topLeft = Offset(lensX - 2f, opticalCenterY + irisApertureHeight),
                size = Size(4f, (lensHeight / 2f) - irisApertureHeight)
            )
        }
    }

    // 2. Draw CMOS Sensor Plane
    val sensorHeight = height * 0.65f
    // Gold contact pads & silicon backing
    drawRect(
        color = Color(0xFF22222A),
        topLeft = Offset(sensorX, opticalCenterY - sensorHeight / 2f),
        size = Size(10f, sensorHeight)
    )
    drawRect(
        color = if (isXRay) Color(0xFF4CAF50) else Color(0xFF1E88E5),
        topLeft = Offset(sensorX + 2f, opticalCenterY - sensorHeight / 2f + 4f),
        size = Size(4f, sensorHeight - 8f)
    )

    // 3. Draw Refracting Light Rays
    val rayOffsets = listOf(-45f, -25f, 0f, 25f, 45f)
    val rayColor = if (isXRay) Color(0xFFFFB300) else Color(0xFF00E5FF)

    for ((index, yOff) in rayOffsets.withIndex()) {
        val startPoint = Offset(0f, opticalCenterY + yOff * 1.3f)
        val firstLensHit = Offset(lensStackStartX + lensSpacing, opticalCenterY + yOff)
        val lastLensHit = Offset(lensStackEndX, opticalCenterY + yOff * 0.5f)

        // Focus point on sensor: converges accurately based on focus distance
        val sensorConvergenceY = CameraOpticsMath.computeSensorConvergenceY(
            entryYOffset = yOff,
            opticalCenterY = opticalCenterY,
            focusNormalized = focusNorm,
            sensorX = sensorX,
            lensX = lensStackEndX
        )
        val endPoint = Offset(sensorX, sensorConvergenceY)

        val rayPath = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            lineTo(firstLensHit.x, firstLensHit.y)
            lineTo(lastLensHit.x, lastLensHit.y)
            lineTo(endPoint.x, endPoint.y)
        }

        drawPath(
            path = rayPath,
            color = rayColor.copy(alpha = 0.55f),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )

        // Animated traveling photons along ray
        val photonT = (rayPhase + index * 0.2f) % 1f
        val photonX = width * photonT
        if (photonX in startPoint.x..endPoint.x) {
            val photonY = when {
                photonX < firstLensHit.x -> startPoint.y + (firstLensHit.y - startPoint.y) * (photonX / firstLensHit.x)
                photonX < lastLensHit.x -> firstLensHit.y + (lastLensHit.y - firstLensHit.y) * ((photonX - firstLensHit.x) / (lastLensHit.x - firstLensHit.x))
                else -> lastLensHit.y + (endPoint.y - lastLensHit.y) * ((photonX - lastLensHit.x) / (endPoint.x - lastLensHit.x))
            }
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(photonX, photonY)
            )
        }
    }

    // 4. In X-Ray mode: draw Voice Coil Actuators (VCM) magnetic assemblies
    if (isXRay) {
        val vcmX = (lensStackStartX + lensStackEndX) / 2f
        drawRect(
            color = Color(0xFFDD5622).copy(alpha = 0.7f),
            topLeft = Offset(vcmX - 25f, opticalCenterY - height * 0.42f),
            size = Size(50f, 10f)
        )
        drawRect(
            color = Color(0xFFDD5622).copy(alpha = 0.7f),
            topLeft = Offset(vcmX - 25f, opticalCenterY + height * 0.38f),
            size = Size(50f, 10f)
        )
    }
}

/**
 * Renders folded 90-degree internal reflection periscope prism optics.
 */
private fun DrawScope.drawPeriscopeRaytrace(
    width: Float,
    height: Float,
    opticalCenterY: Float,
    rayPhase: Float,
    focusNorm: Float,
    isXRay: Boolean
) {
    val prismLeftX = width * 0.25f
    val prismSize = height * 0.50f
    val foldingLensX = width * 0.55f
    val sensorBottomY = height * 0.88f
    val sensorX = width * 0.78f

    // 1. Draw 90° Reflection Prism
    val prismPath = Path().apply {
        moveTo(prismLeftX, opticalCenterY - prismSize / 2f)
        lineTo(prismLeftX + prismSize, opticalCenterY - prismSize / 2f)
        lineTo(prismLeftX, opticalCenterY + prismSize / 2f)
        close()
    }
    drawPath(
        path = prismPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0x4400E5FF), Color(0x337C4DFF)),
            start = Offset(prismLeftX, opticalCenterY - prismSize / 2f),
            end = Offset(prismLeftX + prismSize, opticalCenterY + prismSize / 2f)
        )
    )
    // Reflective hypotenuse mirror surface
    drawLine(
        color = Color.White,
        start = Offset(prismLeftX + prismSize, opticalCenterY - prismSize / 2f),
        end = Offset(prismLeftX, opticalCenterY + prismSize / 2f),
        strokeWidth = 2.5f
    )

    // 2. Horizontal Secondary Lens Group & Periscope Sensor
    val lensPath = Path().apply {
        moveTo(foldingLensX, opticalCenterY - 25f)
        lineTo(foldingLensX + 12f, opticalCenterY - 25f)
        lineTo(foldingLensX + 12f, opticalCenterY + 25f)
        lineTo(foldingLensX, opticalCenterY + 25f)
        close()
    }
    drawPath(path = lensPath, color = Color(0x6600E5FF))

    // Periscope CMOS Sensor (folded laterally)
    drawRect(
        color = Color(0xFF4CAF50),
        topLeft = Offset(sensorX, opticalCenterY - 30f),
        size = Size(8f, 60f)
    )

    // 3. Draw 90° Folded Light Rays
    val rayOffsets = listOf(-20f, 0f, 20f)
    val rayColor = if (isXRay) Color(0xFFFFB300) else Color(0xFF00E5FF)

    for ((index, yOff) in rayOffsets.withIndex()) {
        val inPoint = Offset(0f, opticalCenterY + yOff)
        val mirrorHit = Offset(prismLeftX + (prismSize / 2f) - yOff, opticalCenterY + yOff)
        val lensHit = Offset(foldingLensX, opticalCenterY + yOff * 0.4f)
        val focalY = opticalCenterY + (yOff * (1f - (focusNorm * 0.8f)))
        val sensorHit = Offset(sensorX, focalY)

        val foldedRay = Path().apply {
            moveTo(inPoint.x, inPoint.y)
            lineTo(mirrorHit.x, mirrorHit.y)
            lineTo(lensHit.x, lensHit.y)
            lineTo(sensorHit.x, sensorHit.y)
        }

        drawPath(
            path = foldedRay,
            color = rayColor.copy(alpha = 0.65f),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )

        // Photon pulse
        val t = (rayPhase + index * 0.33f) % 1f
        val pulseX = inPoint.x + (sensorHit.x - inPoint.x) * t
        val pulseY = if (pulseX < mirrorHit.x) inPoint.y else lensHit.y + (sensorHit.y - lensHit.y) * ((pulseX - mirrorHit.x) / (sensorHit.x - mirrorHit.x))
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(pulseX, pulseY))
    }
}
