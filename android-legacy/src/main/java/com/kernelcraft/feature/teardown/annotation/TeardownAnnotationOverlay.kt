package com.kernelcraft.feature.teardown.annotation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Design.md tokens
private val BadgeDark = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF9E9E9E)
private val AccentMustard = Color(0xFFE8A020)
private val CardBorder = Color(0x33FFFFFF)

/**
 * Interactive telemetry overlay displaying dynamic spatial pins, pulsing reticles,
 * dynamic leader lines, and a modal telemetry expansion card for floating hardware components.
 *
 * @param progress Current normalized scrub progress [0.0f..1.0f].
 * @param modifier Modifier for root container.
 * @param annotations Hardware annotation definitions. Defaults to [DefaultTeardownAnnotations.annotations].
 * @param onExpandedChanged Invoked when an annotation card opens or closes to pause/resume scrub gestures.
 */
@Composable
fun TeardownAnnotationOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
    annotations: List<TeardownAnnotation> = DefaultTeardownAnnotations.annotations,
    onExpandedChanged: (Boolean) -> Unit = {},
    onOpenDetail: (String) -> Unit = {},
    onDiveInClick: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    var selectedAnnotation by remember { mutableStateOf<TeardownAnnotation?>(null) }

    // Pulse animation for active target reticles
    val infiniteTransition = rememberInfiniteTransition(label = "ReticlePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        // Compute exact 9:16 letterbox bounds
        val viewportBounds = remember(containerWidthPx, containerHeightPx) {
            calculateFitBounds(containerWidthPx, containerHeightPx, 9f / 16f)
        }

        // 1. Hardware-accelerated Canvas: draws reticles & leader line exclusively for active annotations
        val activeAnnotations = if (progress < 0.75f) {
            emptyList()
        } else {
            annotations
                .map { it to it.computeVisibilityWeight(progress) }
                .filter { it.second > 0.35f }
                .sortedByDescending { it.second }
                .take(1)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (progress < 0.75f) return@Canvas

            for ((annotation, weight) in activeAnnotations) {
                val anchor = viewportBounds.toScreenPoint(annotation.anchorX, annotation.anchorY)
                val effectiveAlpha = weight

                val pillWidthPx = 120.dp.toPx()
                val pillHeightPx = 34.dp.toPx()
                val marginPx = 16.dp.toPx()
                val clampedX = (anchor.x - pillWidthPx / 2f).coerceIn(
                    marginPx,
                    (containerWidthPx - pillWidthPx - marginPx).coerceAtLeast(marginPx)
                )
                val isAbove = anchor.y > containerHeightPx * 0.55f
                val rawY = if (isAbove) anchor.y - pillHeightPx - 16.dp.toPx()
                           else anchor.y + 20.dp.toPx()
                val clampedY = rawY.coerceIn(
                    70.dp.toPx(),
                    (containerHeightPx - 210.dp.toPx()).coerceAtLeast(70f)
                )

                // Leader line directly linking reticle to label pill
                val pillTargetY = if (isAbove) clampedY + pillHeightPx else clampedY
                drawLine(
                    color = (if (annotation.id == "soc_processor") Color(0xFFFF7033) else AccentMustard).copy(alpha = 0.75f * effectiveAlpha),
                    start = anchor,
                    end = Offset(clampedX + pillWidthPx / 2f, pillTargetY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // --- Pulsing Outer Reticle Ring ---
                drawCircle(
                    color = Color.White.copy(alpha = pulseAlpha * effectiveAlpha * 0.7f),
                    radius = 14.dp.toPx() * pulseScale,
                    center = anchor,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // --- Middle Static Reticle Target ---
                drawCircle(
                    color = BadgeDark.copy(alpha = 0.85f * effectiveAlpha),
                    radius = 8.dp.toPx(),
                    center = anchor
                )
                drawCircle(
                    color = Color.White.copy(alpha = effectiveAlpha),
                    radius = 8.dp.toPx(),
                    center = anchor,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // --- Center Core Dot ---
                drawCircle(
                    color = (if (annotation.id == "soc_processor") Color(0xFFFF7033) else AccentMustard).copy(alpha = effectiveAlpha),
                    radius = 3.5.dp.toPx(),
                    center = anchor
                )

                // Dedicated glowing bounding reticle for SoC Processor die
                if (annotation.id == "soc_processor") {
                    val boxHalf = 22.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFFFF7033).copy(alpha = 0.85f * effectiveAlpha),
                        topLeft = Offset(anchor.x - boxHalf, anchor.y - boxHalf),
                        size = Size(boxHalf * 2, boxHalf * 2),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }

        // 2. Interactive Pin Labels (Screen-Clamped: Zero overflow, single focal pin)
        for ((annotation, weight) in activeAnnotations) {
            val anchor = viewportBounds.toScreenPoint(annotation.anchorX, annotation.anchorY)
            val pillWidthPx = with(density) { 120.dp.toPx() }
            val pillHeightPx = with(density) { 34.dp.toPx() }
            val marginPx = with(density) { 16.dp.toPx() }

            val clampedX = (anchor.x - pillWidthPx / 2f).coerceIn(
                marginPx,
                (containerWidthPx - pillWidthPx - marginPx).coerceAtLeast(marginPx)
            )
            val isAbove = anchor.y > containerHeightPx * 0.55f
            val rawY = if (isAbove) anchor.y - pillHeightPx - with(density) { 16.dp.toPx() }
                       else anchor.y + with(density) { 20.dp.toPx() }
            val clampedY = rawY.coerceIn(
                with(density) { 70.dp.toPx() },
                (containerHeightPx - with(density) { 210.dp.toPx() }).coerceAtLeast(70f)
            )

            AnnotationLabelPill(
                annotation = annotation,
                alpha = weight,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (annotation.id == "soc_processor") {
                        onDiveInClick()
                    } else {
                        selectedAnnotation = annotation
                        onExpandedChanged(true)
                    }
                },
                modifier = Modifier
                    .wrapContentSize()
                    .graphicsLayer {
                        translationX = clampedX
                        translationY = clampedY
                        this.alpha = weight
                        scaleX = 0.92f + (0.08f * weight)
                        scaleY = 0.92f + (0.08f * weight)
                    }
            )
        }

        // 3. Modal Telemetry Micro-Card
        AnimatedVisibility(
            visible = selectedAnnotation != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(250), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(200), targetScale = 0.92f)
        ) {
            selectedAnnotation?.let { annotation ->
                TelemetryExpansionDialog(
                    annotation = annotation,
                    onDismiss = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedAnnotation = null
                        onExpandedChanged(false)
                    },
                    onOpenDetail = onOpenDetail,
                    onDiveInClick = onDiveInClick
                )
            }
        }
    }
}

/**
 * Compact high-contrast chip anchored at the end of the leader line.
 */
@Composable
private fun AnnotationLabelPill(
    annotation: TeardownAnnotation,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSoc = annotation.id == "soc_processor"
    val pillTitle = when (annotation.id) {
        "soc_processor" -> "Silicon Die"
        "camera_array" -> "Camera System"
        "battery_pack" -> "Battery Cell"
        "qi_coil", "wireless_coil" -> "Wireless Coil"
        "vapor_chamber" -> "Vapor Chamber"
        else -> annotation.name
    }
    val pillAccent = if (isSoc) Color(0xFFFF7033) else AccentMustard

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSoc) Color(0xFF221612) else BadgeDark.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (isSoc) Color(0xFFFF7033).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.20f * alpha)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(pillAccent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = pillTitle,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * In-depth Component Telemetry Card presented upon pin selection.
 */
@Composable
private fun TelemetryExpansionDialog(
    annotation: TeardownAnnotation,
    onDismiss: () -> Unit,
    onOpenDetail: (String) -> Unit = {},
    onDiveInClick: () -> Unit = {}
) {
    // Semi-transparent scrim to isolate focus
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}, // Prevent dismiss when tapping card content
            shape = RoundedCornerShape(24.dp),
            color = BadgeDark,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Category Pill & Close Button Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentMustard
                    ) {
                        Text(
                            text = annotation.category,
                            color = BadgeDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Close Circular Icon Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF2C2C2C), shape = CircleShape)
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawLine(
                                color = Color.White,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(size.width, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Component Title & Subtitle Tag
                Text(
                    text = annotation.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = annotation.tag,
                    color = AccentMustard,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))

                // Telemetry Data Grid
                Text(
                    text = "HARDWARE ARCHITECTURE SPEC",
                    color = InkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                TelemetryRow(label = "Architecture", value = annotation.telemetry.architecture)
                TelemetryRow(label = "Material Substrate", value = annotation.telemetry.material)
                TelemetryRow(label = "Bus Interface", value = annotation.telemetry.busInterface)
                TelemetryRow(label = "Power Rating", value = annotation.telemetry.powerDraw)
                TelemetryRow(label = "Operating Band", value = annotation.telemetry.operatingFreq)
                TelemetryRow(label = "Thermal Envelope", value = annotation.telemetry.thermalEnvelope)

                Spacer(modifier = Modifier.height(16.dp))

                // Engineering Notes Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF242424),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SYSTEM ARCHITECTURE INSIGHT",
                            color = AccentMustard,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = annotation.telemetry.engineeringNotes,
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                if (annotation.id == "soc_processor") {
                    Button(
                        onClick = {
                            onDismiss()
                            onDiveInClick()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMustard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Dive into Silicon & Kernel Stack",
                            color = BadgeDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        val id = annotation.id
                        onDismiss()
                        onOpenDetail(id)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "View Component Deep-Dive",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tap-to-dismiss hint
                Text(
                    text = "Tap outside to resume teardown scrubbing",
                    color = InkMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = InkMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.40f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(0.60f)
        )
    }
}
