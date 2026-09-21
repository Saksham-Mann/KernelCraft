package com.kernelcraft.feature.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.feature.silicon.SiliconDieVisualizer
import kotlinx.coroutines.launch

private val StudioOrange = Color(0xFFDD5622)
private val SteelBlue = Color(0xFF4E7F9E)
private val BadgeDark = Color(0xFF1A1A1A)

/**
 * Screen 3: Micro-Zoom Transition ("Entering the Silicon").
 *
 * Implements the hardware-to-OS handoff sequence (AppFlow.md §Screen 3):
 * Zooms into the SoC physical coordinate (0.46, 0.32) using Compose graphicsLayer,
 * cross-fading from the board hardware still into the interactive monolithic silicon die.
 */
@Composable
fun SiliconZoomTransitionScreen(
    onTransitionFinished: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stillDrawableRes = remember(context) {
        context.resources.getIdentifier("chip_highlight_still", "drawable", context.packageName)
    }

    val animProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Single-shot automated zoom sequence (~800ms)
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
        onTransitionFinished()
    }

    val progress = animProgress.value

    // Transform calculations anchored on SoC (0.46f, 0.32f)
    val stillScale = 1.0f + (progress * 3.5f) // 1.0x -> 4.5x zoom
    val stillAlpha = (1f - (progress * 1.5f)).coerceIn(0f, 1f)

    val dieAlpha = ((progress - 0.35f) / 0.50f).coerceIn(0f, 1f)
    val dieScale = 0.82f + (0.18f * progress)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SteelBlue)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap to skip transition immediately
                scope.launch {
                    animProgress.snapTo(1f)
                    onTransitionFinished()
                }
            }
    ) {
        // Physical Board Layer (Zooming In)
        if (stillDrawableRes != 0 && stillAlpha > 0.01f) {
            Image(
                painter = painterResource(id = stillDrawableRes),
                contentDescription = "Physical SoC Motherboard Zoom",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = stillScale
                        scaleY = stillScale
                        alpha = stillAlpha
                        // Anchor zoom around SoC center (0.46, 0.32)
                        transformOrigin = TransformOrigin(0.46f, 0.32f)
                    }
            )
        }

        // Silicon Die Visualizer Layer (Crossfading In)
        if (dieAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .graphicsLayer {
                        scaleX = dieScale
                        scaleY = dieScale
                        alpha = dieAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                SiliconDieVisualizer(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Top Navigation Bar (Tactile Back button & Breadcrumb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Spacer(modifier = Modifier.width(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BadgeDark.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8A020))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress < 0.6f) "ENTERING SILICON DIE..." else "TRANSITIONING TO OS KERNEL",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Skip Prompt Banner at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tap anywhere to skip",
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
