package com.kernelcraft.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val StudioOrange = Color(0xFFDD5622)
private val BadgeDark = Color(0xFF1A1A1A)

/**
 * Screen 0: Splash / Onboarding Brand Reveal.
 *
 * Renders the initial cold-start brand reveal over the studio background (#DD5622),
 * guaranteeing a seamless, zero-flash transition into the teardown video viewport.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0.0f) }

    var hasCompleted by remember { mutableStateOf(false) }
    val triggerComplete = remember(onSplashComplete) {
        {
            if (!hasCompleted) {
                hasCompleted = true
                onSplashComplete()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Organic reveal
        alphaAnim.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))
        delay(700)
        triggerComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioOrange)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = triggerComplete
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                alpha = alphaAnim.value
            }
        ) {
            // Brand Logo Glyph
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                color = BadgeDark,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "KC",
                        color = Color(0xFFE8A020),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "KERNELCRAFT",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tactile Smartphone Teardown",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }

        // Tap to skip hint at bottom
        Text(
            text = "Tap to skip",
            color = Color.White.copy(alpha = 0.50f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        )
    }
}
