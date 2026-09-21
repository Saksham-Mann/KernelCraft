package com.kernelcraft.feature.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.kernel.KernelStackExplorerScreen
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val StudioOrange = Color(0xFFDD5622)
private val EdgeGlowOrange = Color(0xFFFF7033)
private val BadgeDark = Color(0xFF1A1A1A)
private val DarkBackground = Color(0xFF0D0D12)

// Normalized SoC Anchor on Disassembled Motherboard (Peak Explosion Frame ~00:00:04.800)
const val SOC_ANCHOR_X = 0.46f
const val SOC_ANCHOR_Y = 0.32f

/**
 * Shared Camera Zoom Animation Layout (`SoCTransitionLayout.kt`).
 *
 * Orchestrates the visual "micro-zoom" shared camera transition into the SoC die:
 * 1. Zoom Phase 1: Motherboard scales exponentially (1.0f -> 8.0f) anchored precisely at (0.46, 0.32).
 * 2. Optical Microscope Vignette: Directional speed lines and radial diffusion simulate moving under high magnification.
 * 3. Zoom Phase 2: Crossfades seamlessly into the macro silicon die and the full Kernel Stack Explorer.
 * 4. Audio-Tactile Immersion: Progressive haptic micro-tick escalation culminating in a firm confirmation tick.
 * 5. Reverse Dive: Smoothly de-scales from macro die back to hardware teardown.
 */
@Composable
fun SoCTransitionLayout(
    onZoomOutToTeardown: () -> Unit,
    modifier: Modifier = Modifier,
    initialTransitionForward: Boolean = true
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val stillDrawableRes = remember(context) {
        context.resources.getIdentifier("chip_highlight_still", "drawable", context.packageName)
    }

    // 0.0f = Physical Teardown Board, 1.0f = Inside Kernel Stack Explorer
    val zoomAnim = remember { Animatable(if (initialTransitionForward) 0f else 1f) }
    var isReversePlaying by remember { mutableStateOf(false) }

    // Haptic escalation thresholds tracker
    var lastHapticStage by remember { mutableIntStateOf(0) }

    // Custom non-linear optical microscope zoom curve
    val zoomEasing = remember { CubicBezierEasing(0.40f, 0.0f, 0.20f, 1.0f) }

    // Automated Forward Transition on First Appearance
    LaunchedEffect(Unit) {
        if (initialTransitionForward && zoomAnim.value < 0.99f) {
            zoomAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = zoomEasing)
            )
        }
    }

    // Audio-Tactile Immersion: Progressive Haptic Escalation
    val progress = zoomAnim.value
    LaunchedEffect(progress) {
        if (!isReversePlaying) {
            when {
                progress in 0.22f..0.40f && lastHapticStage < 1 -> {
                    lastHapticStage = 1
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                progress in 0.45f..0.65f && lastHapticStage < 2 -> {
                    lastHapticStage = 2
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                progress in 0.70f..0.85f && lastHapticStage < 3 -> {
                    lastHapticStage = 3
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                progress >= 0.90f && lastHapticStage < 4 -> {
                    lastHapticStage = 4
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    // Transform Calculations
    // Exponential scale from 1.0x up to 8.0x
    val boardScale = 1.0f + (progress * 7.0f)
    val boardAlpha = (1f - (progress * 1.6f)).coerceIn(0f, 1f)

    // Stack explorer settles into 1.0x scale from 0.85x
    val stackAlpha = ((progress - 0.35f) / 0.55f).coerceIn(0f, 1f)
    val stackScale = 0.85f + (0.15f * progress)

    // Microscope speed line vignette intensity peaks around mid-zoom (0.35..0.70)
    val vignetteAlpha = if (progress in 0.20f..0.85f) {
        (1f - (kotlin.math.abs(progress - 0.50f) / 0.35f)).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // LAYER 1: Physical Motherboard Still (Scale 1.0x -> 8.0x, Pivot on SoC)
        if (stillDrawableRes != 0 && boardAlpha > 0.01f) {
            Image(
                painter = painterResource(id = stillDrawableRes),
                contentDescription = "Physical Board Micro-Zoom",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = boardScale
                        scaleY = boardScale
                        alpha = boardAlpha
                        // Hardware layer caching for locked 60/120 FPS
                        compositingStrategy = CompositingStrategy.Offscreen
                        // Camera Pivot anchored precisely at SoC center
                        transformOrigin = TransformOrigin(SOC_ANCHOR_X, SOC_ANCHOR_Y)
                    }
            )
        }

        // LAYER 2: Optical Microscope Speed-Line Vignette
        if (vignetteAlpha > 0.02f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = vignetteAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                drawMicroscopeVignette(
                    focalPoint = Offset(size.width * SOC_ANCHOR_X, size.height * SOC_ANCHOR_Y),
                    alpha = vignetteAlpha
                )
            }
        }

        // LAYER 3: Macro Silicon Die & Kernel Stack Explorer Screen (Crossfading In)
        if (stackAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = stackScale
                        scaleY = stackScale
                        alpha = stackAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                KernelStackExplorerScreen(
                    onNavigateBack = {
                        // Reverse Dive ("Zoom Out to Hardware")
                        scope.launch {
                            isReversePlaying = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            zoomAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
                            )
                            onZoomOutToTeardown()
                        }
                    }
                )
            }
        }

        // In-flight Transition Overlay: Fast Skip / Reverse Tap
        if (progress in 0.05f..0.85f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Tap to complete transition immediately
                        scope.launch {
                            zoomAnim.snapTo(1f)
                        }
                    }
            ) {
                // Top Progress Status Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BadgeDark.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EdgeGlowOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ENTERING SILICON DIE: ${String.format("%.1f", boardScale)}x MAGNIFICATION",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Tap to skip zoom",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                )
            }
        }
    }
}

/**
 * Draws directional speed lines and a radial glow vignette simulating moving under an optical microscope.
 */
private fun DrawScope.drawMicroscopeVignette(
    focalPoint: Offset,
    alpha: Float
) {
    val radius = size.maxDimension * 0.75f

    // 1. Radial Vignette Gradient centered on SoC focal point
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x22DD5622).copy(alpha = alpha * 0.30f),
                Color(0xDD0D0D12).copy(alpha = alpha * 0.70f)
            ),
            center = focalPoint,
            radius = radius
        )
    )

    // 2. Optical Speed Rays extending radially outwards from the focal center
    val rayCount = 16
    val innerR = 40.dp.toPx()
    val outerR = radius * 0.90f

    for (i in 0 until rayCount) {
        val angleRad = (i * (360f / rayCount)) * (Math.PI / 180f).toFloat()
        val startX = focalPoint.x + (innerR * cos(angleRad))
        val startY = focalPoint.y + (innerR * sin(angleRad))
        val endX = focalPoint.x + (outerR * cos(angleRad))
        val endY = focalPoint.y + (outerR * sin(angleRad))

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    EdgeGlowOrange.copy(alpha = alpha * 0.45f),
                    Color.Transparent
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 1.8.dp.toPx()
        )
    }
}
