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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val StudioOrange = Color(0xFFDD5622)
private val EdgeGlowOrange = Color(0xFFFF7033)
private val DieBackground = Color(0xFF141418)
private val GridColor = Color(0x22FFFFFF)
private val BadgeDark = Color(0xFF1A1A1A)

/**
 * Upper Viewport & Silicon Immersion Canvas.
 *
 * Renders an isometric 3D projected layer diagram over the silicon die with subtle
 * glowing edge lines (#DD5622 / #FF7033), holographic projection rays, simulation badges,
 * and a tactile "Zoom Out to Hardware" pill button.
 */
@Composable
fun IsometricSiliconCanvas(
    onZoomOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HoloPulse")

    // Subtle edge glow breathing animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Holographic scanline offset
    val scanlinePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanlinePhase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(DieBackground)
    ) {
        // Isometric Silicon Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width * 0.50f
            val centerY = height * 0.52f

            // 1. Holographic Ray Field (Vertical projection from silicon substrate)
            drawHolographicRays(centerX, centerY, width, height, glowAlpha)

            // 2. Base Silicon Substrate (Isometric rhombus)
            val baseWidth = width * 0.78f
            val baseHeight = height * 0.46f

            drawIsometricBlock(
                centerX = centerX,
                centerY = centerY + 18.dp.toPx(),
                width = baseWidth,
                height = baseHeight,
                depth = 14.dp.toPx(),
                fillColor = Color(0xFF1C1C24),
                strokeColor = Color.White.copy(alpha = 0.12f)
            )

            // 3. Middle Metal Layer / Microarchitecture Clusters
            drawIsometricBlock(
                centerX = centerX,
                centerY = centerY,
                width = baseWidth * 0.88f,
                height = baseHeight * 0.88f,
                depth = 8.dp.toPx(),
                fillColor = Color(0xFF232330),
                strokeColor = StudioOrange.copy(alpha = glowAlpha)
            )

            // 4. Prime Core & GPU Die Silhouettes with Glowing Edges
            drawDieClusters(centerX, centerY, baseWidth * 0.88f, baseHeight * 0.88f, glowAlpha)

            // 5. Scanline Ray Traversing
            val scanY = (centerY - 50.dp.toPx()) + (scanlinePhase * 90.dp.toPx())
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        EdgeGlowOrange.copy(alpha = 0.35f),
                        EdgeGlowOrange.copy(alpha = 0.80f),
                        EdgeGlowOrange.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                start = Offset(centerX - (baseWidth * 0.45f), scanY),
                end = Offset(centerX + (baseWidth * 0.45f), scanY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Top Control Chrome: Status Badges & Zoom Out Pill Button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Row 1: "Zoom Out to Hardware" Pill Button + Mode Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactile "Zoom Out to Hardware" Pill
                Surface(
                    shape = CircleShape,
                    color = BadgeDark,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier.clickable(onClick = onZoomOutClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "←",
                            color = StudioOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Zoom Out to Hardware",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Inspection Mode Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF22222A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EdgeGlowOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KERNEL IMMERSION",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Row 2: Bottom Status Telemetry Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FPS Counter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1A1A22).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "120 FPS LOCKED • 8.33ms VSYNC",
                        color = Color(0xFF4CAF50),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // SoC Spec Pill
                Text(
                    text = "4nm MONOLITHIC DIE • TSMC N4P",
                    color = Color(0xFF9E9EAA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Draws an isometric projection prism block with top face and extruded sides.
 */
private fun DrawScope.drawIsometricBlock(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    depth: Float,
    fillColor: Color,
    strokeColor: Color
) {
    val halfW = width * 0.5f
    val halfH = height * 0.5f

    // Isometric diamond vertices (top face)
    val topP = Offset(centerX, centerY - halfH)
    val rightP = Offset(centerX + halfW, centerY)
    val bottomP = Offset(centerX, centerY + halfH)
    val leftP = Offset(centerX - halfW, centerY)

    // Extruded side paths
    val leftSidePath = Path().apply {
        moveTo(leftP.x, leftP.y)
        lineTo(bottomP.x, bottomP.y)
        lineTo(bottomP.x, bottomP.y + depth)
        lineTo(leftP.x, leftP.y + depth)
        close()
    }
    drawPath(leftSidePath, color = fillColor.copy(alpha = 0.75f))
    drawPath(leftSidePath, color = strokeColor, style = Stroke(width = 1.dp.toPx()))

    val rightSidePath = Path().apply {
        moveTo(bottomP.x, bottomP.y)
        lineTo(rightP.x, rightP.y)
        lineTo(rightP.x, rightP.y + depth)
        lineTo(bottomP.x, bottomP.y + depth)
        close()
    }
    drawPath(rightSidePath, color = fillColor.copy(alpha = 0.55f))
    drawPath(rightSidePath, color = strokeColor, style = Stroke(width = 1.dp.toPx()))

    // Top face
    val topFacePath = Path().apply {
        moveTo(topP.x, topP.y)
        lineTo(rightP.x, rightP.y)
        lineTo(bottomP.x, bottomP.y)
        lineTo(leftP.x, leftP.y)
        close()
    }
    drawPath(topFacePath, color = fillColor)
    drawPath(topFacePath, color = strokeColor, style = Stroke(width = 1.5.dp.toPx()))
}

/**
 * Draws cluster subdivisions on the top isometric plane.
 */
private fun DrawScope.drawDieClusters(
    centerX: Float,
    centerY: Float,
    topW: Float,
    topH: Float,
    glowAlpha: Float
) {
    val innerStroke = Stroke(
        width = 1.2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )

    // Prime Core (Top quadrant)
    val primePath = Path().apply {
        moveTo(centerX, centerY - topH * 0.40f)
        lineTo(centerX + topW * 0.18f, centerY - topH * 0.22f)
        lineTo(centerX, centerY - topH * 0.04f)
        lineTo(centerX - topW * 0.18f, centerY - topH * 0.22f)
        close()
    }
    drawPath(primePath, color = Color(0xFFDD5622).copy(alpha = 0.25f))
    drawPath(primePath, color = EdgeGlowOrange.copy(alpha = glowAlpha), style = Stroke(width = 1.5.dp.toPx()))

    // GPU / NPU clusters (Lower quadrant)
    val gpuPath = Path().apply {
        moveTo(centerX - topW * 0.28f, centerY)
        lineTo(centerX, centerY + topH * 0.26f)
        lineTo(centerX - topW * 0.12f, centerY + topH * 0.38f)
        lineTo(centerX - topW * 0.38f, centerY + topH * 0.12f)
        close()
    }
    drawPath(gpuPath, color = Color(0xFF4E7F9E).copy(alpha = 0.20f))
    drawPath(gpuPath, color = Color(0xFF4E7F9E).copy(alpha = glowAlpha), style = innerStroke)
}

/**
 * Draws vertical glowing projection rays extending into the holographic upper space.
 */
private fun DrawScope.drawHolographicRays(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    alpha: Float
) {
    val rayBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            StudioOrange.copy(alpha = alpha * 0.35f),
            Color.Transparent
        )
    )

    val rayOffsets = listOf(-0.25f, -0.12f, 0.0f, 0.12f, 0.25f)
    rayOffsets.forEach { factor ->
        val x = centerX + (width * factor)
        drawLine(
            brush = rayBrush,
            start = Offset(x, centerY + 20.dp.toPx()),
            end = Offset(x, centerY - 65.dp.toPx()),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}
