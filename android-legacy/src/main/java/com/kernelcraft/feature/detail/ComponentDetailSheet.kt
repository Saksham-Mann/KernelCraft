package com.kernelcraft.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.detail.ui.BatteryThermalsView
import com.kernelcraft.feature.detail.ui.CameraOpticsView
import com.kernelcraft.feature.teardown.annotation.DefaultTeardownAnnotations
import com.kernelcraft.feature.teardown.annotation.TeardownAnnotation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SheetSurfaceDark = Color(0xF0121216)
private val CardDark = Color(0xFF1C1C24)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9EAA)
private val AccentMustard = Color(0xFFE8A020)
private val AccentOrange = Color(0xFFDD5622)
private val HandleColor = Color(0x66FFFFFF)

/**
 * Subsystem inspection modal framework for primary non-SoC hardware components.
 *
 * Overlays the frozen teardown video frame with a frosted studio theme, gesture-dismissible
 * bottom sheet, segmented header with Model/Fab ID, interactive "360° Inspect" vs "X-Ray Layer"
 * toggle, and component-specific interactive Canvas visualizers.
 */
@Composable
fun ComponentDetailSheet(
    componentId: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onDiveInClick: () -> Unit = {}
) {
    val isVisible = componentId != null
    val annotation: TeardownAnnotation? = remember(componentId) {
        if (componentId != null) {
            DefaultTeardownAnnotations.annotations.find { it.id == componentId }
                ?: DefaultTeardownAnnotations.annotations.firstOrNull()
        } else null
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        val haptic = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        var isXRayMode by remember { mutableStateOf(false) }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val sheetHeightPx = constraints.maxHeight.toFloat()
            val offsetY = remember { Animatable(sheetHeightPx) }

            // Slide in sheet when visible
            LaunchedEffect(isVisible) {
                if (isVisible) {
                    offsetY.snapTo(sheetHeightPx)
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }

            // Dismiss animation helper
            val dismissSheet: () -> Unit = {
                coroutineScope.launch {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    offsetY.animateTo(
                        targetValue = sheetHeightPx,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                    onDismiss()
                }
            }

            // 1. Frosted Scrim Backdrop (tap to dismiss)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismissSheet
                    )
            )

            // 2. Gesture-dismissible Bottom Sheet Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, offsetY.value.roundToInt().coerceAtLeast(0)) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                val newOffset = (offsetY.value + delta).coerceAtLeast(0f)
                                offsetY.snapTo(newOffset)
                            }
                        },
                        onDragStopped = { velocity ->
                            if (offsetY.value > sheetHeightPx * 0.25f || velocity > 1200f) {
                                dismissSheet()
                            } else {
                                coroutineScope.launch {
                                    offsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SheetSurfaceDark,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 24.dp
            ) {
                if (annotation != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(HandleColor)
                            )
                        }

                        // Segmented Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentMustard
                                ) {
                                    Text(
                                        text = annotation.category,
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Interactive "360° Inspect" vs "X-Ray Layer" Toggle
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF22222E))
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    InspectionTogglePill(
                                        title = "360° Optical",
                                        isSelected = !isXRayMode,
                                        onClick = {
                                            isXRayMode = false
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                    InspectionTogglePill(
                                        title = "X-Ray Layer",
                                        isSelected = isXRayMode,
                                        onClick = {
                                            isXRayMode = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = annotation.name,
                                color = TextWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = annotation.tag,
                                color = AccentMustard,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        // Scrollable Content Body
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            // Subsystem Visualizer
                            when (annotation.id) {
                                "camera_array" -> {
                                    CameraOpticsView(
                                        isXRayMode = isXRayMode,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                                "battery_pack", "vapor_chamber", "qi_coil" -> {
                                    BatteryThermalsView(
                                        componentId = annotation.id,
                                        isXRayMode = isXRayMode,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            // Architectural Specifications Section
                            Text(
                                text = "ARCHITECTURAL SPECIFICATIONS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SheetMetricCard(label = "Silicon / Core Architecture", value = annotation.telemetry.architecture)
                            SheetMetricCard(label = "Material Substrate", value = annotation.telemetry.material)
                            SheetMetricCard(label = "System Bus Interface", value = annotation.telemetry.busInterface)
                            SheetMetricCard(label = "Power Draw & Rating", value = annotation.telemetry.powerDraw)
                            SheetMetricCard(label = "Operating Frequency Band", value = annotation.telemetry.operatingFreq)
                            SheetMetricCard(label = "Thermal Envelope & Cooling", value = annotation.telemetry.thermalEnvelope)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Engineering Insight Callout
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CardDark,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "ENGINEERING INSIGHT",
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

                            if (annotation.id == "soc_processor") {
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        dismissSheet()
                                        onDiveInClick()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                                ) {
                                    Text(
                                        text = "Enter Silicon & Kernel Stack →",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectionTogglePill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) AccentOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF9E9EAA),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SheetMetricCard(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CardDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
