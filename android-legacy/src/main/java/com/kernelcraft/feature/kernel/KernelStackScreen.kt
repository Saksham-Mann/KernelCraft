package com.kernelcraft.feature.kernel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.kernelcraft.feature.kernel.ui.TierInteractiveVisualizer

// Design.md token palette
private val SteelBlue = Color(0xFF4E7F9E)
private val Mustard = Color(0xFFE8A020)
private val BadgeDark = Color(0xFF1A1A1A)
private val SheetWhite = Color(0xFFFFFFFF)
private val InkDark = Color(0xFF141414)
private val InkMuted = Color(0xFF6B6B6B)
private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Screen 4: OS & Kernel Architecture Explorer (`KernelStackScreen.kt`).
 *
 * Interactive 5-tier hierarchical stack representing:
 * 1. Physical Silicon & Microarchitecture
 * 2. Linux Kernel (Scheduler, ZRAM, Power, Device Drivers)
 * 3. Hardware Abstraction Layer (HAL)
 * 4. Android Runtime (ART) & Native Libraries
 * 5. Userspace & Jetpack Compose Framework
 *
 * Adheres to Design.md:
 * - Solid saturated background colors: Steel Blue (#4E7F9E) for lower layers,
 *   Mustard (#E8A020) for upper layers (discrete hue shift).
 * - Opaque white content sheet with 32dp top corners.
 * - Circular dark icon buttons (48dp, #1A1A1A).
 * - High typographic hierarchy.
 */
@Composable
fun KernelStackScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTier: OsTier = OsTier.PHYSICAL_SILICON
) {
    var currentTier by remember { mutableStateOf(initialTier) }

    // Discrete background color shift per Design.md §1.3
    val backgroundColor by animateColorAsState(
        targetValue = currentTier.themeColor,
        animationSpec = tween(durationMillis = 250),
        label = "LayerBackgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar Chrome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactile Circular Back Button (returns cleanly to Teardown Viewport)
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BadgeDark)
                ) {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tier Status Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BadgeDark
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIER ${currentTier.tierIndex + 1} OF 5",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Tier Scrubber / Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsTier.entries.forEach { tier ->
                    val isSelected = tier == currentTier
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) BadgeDark else Color.White.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Transparent),
                        modifier = Modifier.clickable { currentTier = tier }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0${tier.tierIndex + 1}. ${tier.identifier.uppercase()}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Opaque White Content Sheet (Design.md §2: 32dp top radius, stark white, opaque)
            Surface(
                shape = SheetShape,
                color = SheetWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentTier,
                    transitionSpec = {
                        if (targetState.tierIndex > initialState.tierIndex) {
                            (slideInHorizontally { it / 3 } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally { -it / 3 } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally { it / 3 } + fadeOut(tween(200)))
                        }
                    },
                    label = "TierContentAnimation"
                ) { tier ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                            .navigationBarsPadding()
                    ) {
                        // Badge Tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BadgeDark
                        ) {
                            Text(
                                text = tier.badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title & Subtitle
                        Text(
                            text = tier.title,
                            color = InkDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = tier.subtitle,
                            color = InkMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Privilege & Security Domain Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecurityPill(label = "PRIVILEGE", value = tier.privilegeRing, modifier = Modifier.weight(1f))
                            SecurityPill(label = "SECURITY", value = tier.securityDomain, modifier = Modifier.weight(1.2f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LIVE INTERACTIVE TELEMETRY & SIMULATION VISUALIZER
                        Text(
                            text = "LIVE SYSTEM TELEMETRY & SIMULATION",
                            color = InkDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TierInteractiveVisualizer(tier = tier)

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFE5E5E5))
                        Spacer(modifier = Modifier.height(16.dp))

                        // ARCHITECTURAL SUBSYSTEMS BREAKDOWN
                        Text(
                            text = "ARCHITECTURAL SUBSYSTEMS",
                            color = InkDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        TierRepository.getSubsystems(tier).forEach { subsystem ->
                            SubsystemCard(subsystem = subsystem)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // KEY SYSTEM CALLS / APIS
                        Text(
                            text = "CORE INTERFACES & SYSTEM CALLS",
                            color = InkDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tier.keySyscallsOrApis.forEach { api ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF4F4F4),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ">",
                                            color = BadgeDark,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = api,
                                            color = InkDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tier Navigation Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentTier.tierIndex > 0) {
                                Button(
                                    onClick = {
                                        currentTier = OsTier.fromIndex(currentTier.tierIndex - 1)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAEAEA))
                                ) {
                                    Text(
                                        text = "← Lower Tier",
                                        color = InkDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            if (currentTier.tierIndex < OsTier.entries.size - 1) {
                                Button(
                                    onClick = {
                                        currentTier = OsTier.fromIndex(currentTier.tierIndex + 1)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BadgeDark)
                                ) {
                                    Text(
                                        text = "Upper Tier →",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onNavigateBack,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BadgeDark)
                                ) {
                                    Text(
                                        text = "Exit to Hardware Teardown",
                                        color = Color.White,
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
}

@Composable
private fun SecurityPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF7F7F7),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = InkMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = InkDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SubsystemCard(subsystem: TierSubsystem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F8F8),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subsystem.title,
                    color = InkDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE2E2E2)
                ) {
                    Text(
                        text = subsystem.tag,
                        color = InkDark,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subsystem.description,
                color = InkMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = subsystem.metricLabel,
                    color = InkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subsystem.metricValue,
                    color = InkDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
