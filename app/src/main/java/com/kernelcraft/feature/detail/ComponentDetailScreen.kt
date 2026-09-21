package com.kernelcraft.feature.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.teardown.annotation.DefaultTeardownAnnotations
import com.kernelcraft.feature.teardown.annotation.TeardownAnnotation

private val StudioOrange = Color(0xFFDD5622)
private val BadgeDark = Color(0xFF1A1A1A)
private val CardDark = Color(0xFF222222)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val AccentMustard = Color(0xFFE8A020)

/**
 * Full-screen deep-dive architectural view for individual hardware components.
 */
@Composable
fun ComponentDetailScreen(
    componentId: String,
    onNavigateBack: () -> Unit,
    onDiveIntoKernel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val annotation: TeardownAnnotation? = remember(componentId) {
        DefaultTeardownAnnotations.annotations.find { it.id == componentId }
            ?: DefaultTeardownAnnotations.annotations.firstOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioOrange)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar Chrome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactile Circular Back Button (Design.md §3.1)
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BadgeDark)
                ) {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "COMPONENT TELEMETRY",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Main Content Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = BadgeDark,
                shadowElevation = 16.dp
            ) {
                if (annotation == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Component not found", color = TextMuted)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        var isXRayMode by remember { mutableStateOf(false) }

                        // Category Tag & X-Ray Toggle
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

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1E1E26))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (!isXRayMode) StudioOrange else Color.Transparent)
                                        .clickable { isXRayMode = false }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "360° Optical",
                                        color = if (!isXRayMode) Color.White else Color(0xFF9E9EAA),
                                        fontSize = 11.sp,
                                        fontWeight = if (!isXRayMode) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isXRayMode) StudioOrange else Color.Transparent)
                                        .clickable { isXRayMode = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "X-Ray Layer",
                                        color = if (isXRayMode) Color.White else Color(0xFF9E9EAA),
                                        fontSize = 11.sp,
                                        fontWeight = if (isXRayMode) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = annotation.name,
                            color = TextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = annotation.tag,
                            color = AccentMustard,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Subsystem Visualizer
                        when (annotation.id) {
                            "camera_array" -> {
                                com.kernelcraft.feature.detail.ui.CameraOpticsView(
                                    isXRayMode = isXRayMode,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                            "battery_pack", "vapor_chamber", "qi_coil" -> {
                                com.kernelcraft.feature.detail.ui.BatteryThermalsView(
                                    componentId = annotation.id,
                                    isXRayMode = isXRayMode,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ARCHITECTURAL SPECIFICATIONS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailMetricCard(label = "Silicon / Core Architecture", value = annotation.telemetry.architecture)
                        DetailMetricCard(label = "Material Substrate", value = annotation.telemetry.material)
                        DetailMetricCard(label = "System Bus Interface", value = annotation.telemetry.busInterface)
                        DetailMetricCard(label = "Power Draw & Rating", value = annotation.telemetry.powerDraw)
                        DetailMetricCard(label = "Operating Frequency Band", value = annotation.telemetry.operatingFreq)
                        DetailMetricCard(label = "Thermal Envelope & Cooling", value = annotation.telemetry.thermalEnvelope)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Engineering Insight Callout
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardDark,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "SYSTEM ARCHITECTURE INSIGHT",
                                    color = AccentMustard,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = annotation.telemetry.engineeringNotes,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        if (componentId == "soc_processor") {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onDiveIntoKernel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentMustard)
                            ) {
                                Text(
                                    text = "Enter Silicon & Kernel Stack →",
                                    color = BadgeDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDark,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
