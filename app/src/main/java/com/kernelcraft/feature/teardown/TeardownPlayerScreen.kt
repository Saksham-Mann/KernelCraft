package com.kernelcraft.feature.teardown

import android.net.Uri
import android.view.LayoutInflater
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.roundToInt
import com.kernelcraft.R
import androidx.annotation.OptIn
import com.kernelcraft.feature.teardown.annotation.TeardownAnnotationOverlay
import com.kernelcraft.feature.specs.SpecsDetailSheet
import com.kernelcraft.feature.detail.ComponentDetailSheet
import com.kernelcraft.core.audio.TeardownAudioSynthesizer
import com.kernelcraft.core.player.KernelCraftPlayerFactory
import com.kernelcraft.core.player.DecoderTelemetryListener
import com.kernelcraft.core.player.PlayerMetrics
import com.kernelcraft.feature.teardown.ui.TeardownDebugHud
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

// Design.md token palette
private val StudioOrange = Color(0xFFDD5622)
private val BadgeDark = Color(0xFF1A1A1A)
private val SheetWhite = Color(0xFFFFFFFF)
private val InkDark = Color(0xFF141414)
private val InkMuted = Color(0xFF6B6B6B)
private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Stateful entry point for the Teardown Player screen.
 */
@Composable
fun TeardownPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: TeardownViewModel = viewModel(),
    videoRawResId: Int? = null,
    videoUri: Uri? = null,
    dragSensitivity: Float = 2.5f,
    onNavigateBack: () -> Unit = {},
    onOpenComponentDetail: (String) -> Unit = {},
    onOpenFullSpecs: () -> Unit = {},
    onDiveInClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeardownPlayerContent(
        modifier = modifier,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        videoRawResId = videoRawResId,
        videoUri = videoUri,
        dragSensitivity = dragSensitivity,
        onNavigateBack = onNavigateBack,
        onOpenComponentDetail = onOpenComponentDetail,
        onOpenFullSpecs = onOpenFullSpecs,
        onDiveInClick = onDiveInClick
    )
}

/**
 * Stateless Teardown Player viewport.
 *
 * Implements:
 * 1. Velocity & Smooth Scrubbing Physics via `pointerInput` with `detectVerticalDragGestures`,
 *    `VelocityTracker`, and `Animatable.animateDecay`.
 * 2. Milestone Detection Engine via `TeardownMilestone`.
 * 3. Haptic Feedback ticks on milestone crossings and peak explosion thresholds.
 * 4. Zero-seek-storm pipeline via conflated `MutableSharedFlow` and `collectLatest`.
 * 5. High-FPS Compose rendering using `derivedStateOf` to prevent unneeded recompositions.
 */
@OptIn(UnstableApi::class)
@Composable
fun TeardownPlayerContent(
    modifier: Modifier = Modifier,
    uiState: TeardownUiState,
    onEvent: (TeardownEvent) -> Unit,
    videoRawResId: Int? = null,
    videoUri: Uri? = null,
    dragSensitivity: Float = 2.5f,
    onNavigateBack: () -> Unit = {},
    onOpenComponentDetail: (String) -> Unit = {},
    onOpenFullSpecs: () -> Unit = {},
    onDiveInClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    // Calibrated virtual drag distance: factor * viewport height maps 0.0 -> 1.0 progress
    val totalScrollDistancePx = remember(screenHeightPx, dragSensitivity) {
        screenHeightPx * dragSensitivity
    }

    // Local gesture tracking states
    var scrubProgress by remember { mutableFloatStateOf(uiState.scrollProgress) }
    var durationMs by remember { mutableLongStateOf(uiState.durationMs) }
    var isDragging by remember { mutableStateOf(false) }
    var isCoasting by remember { mutableStateOf(false) }
    var currentVelocity by remember { mutableFloatStateOf(0f) }
    var isOverlayExpanded by remember { mutableStateOf(false) }
    var isSpecsSheetVisible by remember { mutableStateOf(false) }
    var activeInspectionComponentId by remember { mutableStateOf<String?>(null) }

    // Pre-extracted peak layer explosion still asset (res/drawable/chip_highlight_still.webp)
    val stillDrawableRes = remember(context) {
        context.resources.getIdentifier("chip_highlight_still", "drawable", context.packageName)
    }

    // Seamless Viewport Handoff: Crossfade between live ExoPlayer and static still
    val stillAlpha by animateFloatAsState(
        targetValue = if (isSpecsSheetVisible || activeInspectionComponentId != null) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "StillTextureCrossfade"
    )

    // Procedural low-latency PCM audio synthesis engine
    val audioSynthesizer = remember { TeardownAudioSynthesizer() }

    // Sync audio muting with user preferences
    LaunchedEffect(uiState.isAudioEnabled) {
        audioSynthesizer.setMuted(!uiState.isAudioEnabled)
    }

    // Physics decay controller
    val decayAnimatable = remember { Animatable(0f) }
    var decayJob by remember { mutableStateOf<Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }

    // Telemetry and Profiling State
    var playerMetrics by remember { mutableStateOf(PlayerMetrics()) }
    val telemetryListener = remember {
        DecoderTelemetryListener { metrics ->
            playerMetrics = metrics
        }
    }

    // Current position in ms derived from progress & duration
    val currentPositionMs = remember(scrubProgress, durationMs) {
        (scrubProgress * durationMs).toLong().coerceIn(0L, durationMs)
    }

    // Reactively derived milestone: recomposes consumers ONLY when milestone changes
    val currentMilestone by remember {
        derivedStateOf { TeardownMilestone.fromPositionMs(currentPositionMs) }
    }

    // Conflated seek pipeline: coalesces 120Hz gestures into at most 1 seek per frame
    val seekFlow = remember {
        MutableSharedFlow<Long>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    // Haptic feedback management: tracks threshold crossings
    var lastHapticPositionMs by remember { mutableLongStateOf(0L) }
    val hapticThresholds = remember {
        TeardownMilestone.BOUNDARY_THRESHOLDS_MS + TeardownMilestone.PEAK_EXPLOSION_MS
    }

    fun triggerHapticIfCrossed(oldPos: Long, newPos: Long) {
        if (!uiState.isHapticsEnabled) return
        val minPos = minOf(oldPos, newPos)
        val maxPos = maxOf(oldPos, newPos)
        for (threshold in hapticThresholds) {
            if (threshold in (minPos + 1)..maxPos) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                break
            }
        }
    }

    // Back navigation: reassembles if deconstructed, otherwise exits screen
    BackHandler {
        if (scrubProgress > 0.05f) {
            decayJob?.cancel()
            decayJob = coroutineScope.launch {
                audioSynthesizer.playReassembleSnap()
                val anim = Animatable(scrubProgress)
                try {
                    anim.animateTo(
                        targetValue = 0.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) {
                        scrubProgress = value
                        val posMs = (value * durationMs).toLong()
                        seekFlow.tryEmit(posMs)
                        onEvent(
                            TeardownEvent.ScrubUpdated(
                                progress = value,
                                positionMs = posMs,
                                velocity = -0.5f,
                                isDragging = false,
                                isCoasting = true
                            )
                        )
                    }
                } finally {
                    scrubProgress = 0.0f
                    seekFlow.tryEmit(0L)
                    onEvent(TeardownEvent.ReassemblePhone)
                }
            }
        } else {
            onNavigateBack()
        }
    }

    // Sync state when ViewModel triggers spring reassemble animation
    LaunchedEffect(uiState.isReassembling, uiState.scrollProgress) {
        if (uiState.isReassembling) {
            scrubProgress = uiState.scrollProgress
            seekFlow.tryEmit(uiState.currentPositionMs)
        }
    }

    // Preload all 40 teardown frames for instant, zero-lag scrub animation starting from assembled device
    LaunchedEffect(Unit) {
        TeardownFrameManager.preloadFrames(context, coroutineScope)
        if (durationMs <= 0L) {
            val totalDuration = 10000L // 10.0 seconds matching video length
            durationMs = totalDuration
            onEvent(TeardownEvent.DurationResolved(totalDuration))
        }
    }

    // Resolve frame corresponding to scrub progress (0.0 = Assembled, 1.0 = Exploded)
    val currentFrameIndex = (scrubProgress.coerceIn(0f, 1f) * (TeardownFrameManager.TOTAL_FRAMES - 1)).roundToInt()
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentFrameIndex) {
        currentBitmap = TeardownFrameManager.getFrame(context, currentFrameIndex)
    }

    val isEmulator = remember {
        android.os.Build.FINGERPRINT.startsWith("generic") ||
        android.os.Build.FINGERPRINT.startsWith("unknown") ||
        android.os.Build.MODEL.contains("google_sdk") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK built for x86") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.HARDWARE.contains("ranchu")
    }

    // Configure ultra-low-latency, video-only ExoPlayer via KernelCraftPlayerFactory (skipped on emulator to prevent host GPU crash)
    val exoPlayer = remember(context, isEmulator) {
        if (isEmulator) {
            null
        } else {
            KernelCraftPlayerFactory.createScrubPlayer(context, telemetryListener).apply {
                val mediaItem = KernelCraftPlayerFactory.buildMediaItem(
                    context = context,
                    videoRawResId = videoRawResId,
                    videoUri = videoUri,
                    fallbackResourceName = "teardown_chip01"
                )
                setMediaItem(mediaItem)
                prepare()
            }
        }
    }

    // Freeze / Pause ExoPlayer hardware decoding while viewing technical specs or component inspection
    LaunchedEffect(isSpecsSheetVisible, activeInspectionComponentId, exoPlayer) {
        if (isSpecsSheetVisible || activeInspectionComponentId != null) {
            exoPlayer?.pause()
        }
    }

    // Extract exact duration upon player ready state
    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val dur = exoPlayer.duration
                        if (dur > 0L && dur != C.TIME_UNSET) {
                            durationMs = dur
                            onEvent(TeardownEvent.DurationResolved(dur))
                        }
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }
    }

    // Lifecycle observer: cleanly flush surface on backgrounding to avoid CodecException
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.let { KernelCraftPlayerFactory.flushAndClearSurface(it) }
                    audioSynthesizer.onLifecyclePause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    audioSynthesizer.onLifecycleResume()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer?.release()
                    audioSynthesizer.release()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
            audioSynthesizer.release()
        }
    }

    // Play layer separation sound when crossing into exploded milestone
    LaunchedEffect(currentMilestone) {
        if (currentMilestone == TeardownMilestone.EXPLODED_LAYERS ||
            currentMilestone == TeardownMilestone.CHASSIS_FOCUS) {
            audioSynthesizer.playLayerExplodeWhirr()
        }
    }

    // Throttled seek collector with latency profiling
    LaunchedEffect(exoPlayer) {
        seekFlow.collectLatest { targetPositionMs ->
            telemetryListener.onSeekDispatched()
            exoPlayer?.seekTo(targetPositionMs)
            telemetryListener.updatePosition(targetPositionMs, durationMs)
        }
    }

    // Gesture input modifier with velocity tracking and inertia decay
    val dragModifier = Modifier.pointerInput(totalScrollDistancePx, durationMs) {
        detectVerticalDragGestures(
            onDragStart = {
                decayJob?.cancel()
                decayJob = null
                velocityTracker.resetTracking()
                isDragging = true
                isCoasting = false
                currentVelocity = 0f
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                // Dragging UP (negative dragAmount) advances progress (+)
                val deltaProgress = -dragAmount / totalScrollDistancePx
                val oldPos = currentPositionMs
                scrubProgress = (scrubProgress + deltaProgress).coerceIn(0f, 1f)
                val newPositionMs = (scrubProgress * durationMs).toLong()
                seekFlow.tryEmit(newPositionMs)
                triggerHapticIfCrossed(oldPos, newPositionMs)
                audioSynthesizer.playRatchetClick(kotlin.math.abs(deltaProgress * 25f))

                onEvent(
                    TeardownEvent.ScrubUpdated(
                        progress = scrubProgress,
                        positionMs = newPositionMs,
                        velocity = currentVelocity,
                        isDragging = true,
                        isCoasting = false
                    )
                )
            },
            onDragEnd = {
                isDragging = false
                val velocityY = velocityTracker.calculateVelocity().y
                // Upward flick (negative velocityY) yields positive scrub velocity
                val velocityProgressPerSec = -velocityY / totalScrollDistancePx
                currentVelocity = velocityProgressPerSec

                if (abs(velocityProgressPerSec) > 0.08f) {
                    decayJob = coroutineScope.launch {
                        isCoasting = true
                        decayAnimatable.snapTo(scrubProgress)
                        decayAnimatable.updateBounds(0f, 1f)
                        try {
                            decayAnimatable.animateDecay(
                                initialVelocity = velocityProgressPerSec,
                                animationSpec = exponentialDecay(frictionMultiplier = 2.2f)
                            ) {
                                scrubProgress = value
                                currentVelocity = velocity
                                val newPositionMs = (value * durationMs).toLong()

                                triggerHapticIfCrossed(lastHapticPositionMs, newPositionMs)
                                lastHapticPositionMs = newPositionMs

                                seekFlow.tryEmit(newPositionMs)

                                onEvent(
                                    TeardownEvent.ScrubUpdated(
                                        progress = value,
                                        positionMs = newPositionMs,
                                        velocity = velocity,
                                        isDragging = false,
                                        isCoasting = true
                                    )
                                )
                            }
                        } finally {
                            isCoasting = false
                            currentVelocity = 0f
                            onEvent(
                                TeardownEvent.ScrubUpdated(
                                    progress = scrubProgress,
                                    positionMs = (scrubProgress * durationMs).toLong(),
                                    velocity = 0f,
                                    isDragging = false,
                                    isCoasting = false
                                )
                            )
                        }
                    }
                } else {
                    currentVelocity = 0f
                    onEvent(
                        TeardownEvent.ScrubUpdated(
                            progress = scrubProgress,
                            positionMs = (scrubProgress * durationMs).toLong(),
                            velocity = 0f,
                            isDragging = false,
                            isCoasting = false
                        )
                    )
                }
            },
            onDragCancel = {
                isDragging = false
                isCoasting = false
                currentVelocity = 0f
                velocityTracker.resetTracking()
            }
        )
    }

    // Disable vertical scrub gesture when inspecting hardware telemetry or specs
    val activeDragModifier = if (isOverlayExpanded || isSpecsSheetVisible) Modifier else dragModifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioOrange)
            .then(activeDragModifier)
    ) {
        // 1. Interactive Teardown Viewport (Starts at 0.0s Assembled, scrubs to Exploded, reassembles back)
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap!!.asImageBitmap(),
                contentDescription = "Teardown Viewport",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (stillDrawableRes != 0) {
            Image(
                painter = painterResource(id = stillDrawableRes),
                contentDescription = "Teardown Viewport Still",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 3. Top Status HUD (Back, Reassemble, Specs)
        TopStatusHeader(
            progress = scrubProgress,
            isReassembling = uiState.isReassembling,
            onNavigateBack = onNavigateBack,
            onReassembleClick = {
                decayJob?.cancel()
                decayJob = coroutineScope.launch {
                    audioSynthesizer.playReassembleSnap()
                    val anim = Animatable(scrubProgress)
                    try {
                        anim.animateTo(
                            targetValue = 0.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) {
                            scrubProgress = value
                            val posMs = (value * durationMs).toLong()
                            seekFlow.tryEmit(posMs)
                            onEvent(
                                TeardownEvent.ScrubUpdated(
                                    progress = value,
                                    positionMs = posMs,
                                    velocity = -0.5f,
                                    isDragging = false,
                                    isCoasting = true
                                )
                            )
                        }
                    } finally {
                        scrubProgress = 0.0f
                        seekFlow.tryEmit(0L)
                        onEvent(TeardownEvent.ReassemblePhone)
                    }
                }
            },
            onOpenSpecsClick = { isSpecsSheetVisible = true },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        )

        // 3b. Interactive Tutorial Coach-Mark on first launch
        AnimatedVisibility(
            visible = uiState.showTutorialCoachMark,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300)) + slideOutVertically(tween(400)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BadgeDark.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DRAG UP TO DECONSTRUCT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vertical thumb drag scrubs smartphone hardware layers",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Bottom Contextual Bottom Sheet
        MilestoneBottomSheet(
            milestone = currentMilestone,
            progress = scrubProgress,
            positionMs = currentPositionMs,
            durationMs = durationMs,
            showDiveIn = scrubProgress >= 0.90f,
            onDiveInClick = onDiveInClick,
            onOpenSpecsClick = { isSpecsSheetVisible = true },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )

        // 5. Dynamic Hardware Telemetry Overlay (Renders pins, leader lines & modal detail card)
        TeardownAnnotationOverlay(
            progress = scrubProgress,
            onExpandedChanged = { expanded ->
                isOverlayExpanded = expanded
                if (expanded) {
                    decayJob?.cancel()
                    decayJob = null
                    isDragging = false
                    isCoasting = false
                }
            },
            onOpenDetail = { componentId ->
                activeInspectionComponentId = componentId
            },
            onDiveInClick = onDiveInClick
        )

        // 6. Sliding Technical Specifications Panel
        SpecsDetailSheet(
            isVisible = isSpecsSheetVisible,
            onDismiss = {
                isSpecsSheetVisible = false
            }
        )

        // 7. Subsystem Inspection Modal (Camera Optics & Battery/Thermals)
        ComponentDetailSheet(
            componentId = activeInspectionComponentId,
            onDismiss = {
                activeInspectionComponentId = null
            },
            onDiveInClick = {
                activeInspectionComponentId = null
                onDiveInClick()
            }
        )
    }
}

/**
 * Top Status Header showing back button, reassemble CTA, and specs trigger.
 */
@Composable
private fun TopStatusHeader(
    progress: Float,
    isReassembling: Boolean,
    onNavigateBack: () -> Unit,
    onReassembleClick: () -> Unit,
    onOpenSpecsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BadgeDark)
        ) {
            Text(
                text = "<",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Right Cluster: Reassemble + Specs Pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Reassemble Button (visible when disassembled)
            if (progress > 0.05f || isReassembling) {
                Surface(
                    shape = CircleShape,
                    color = BadgeDark,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onReassembleClick)
                ) {
                    Text(
                        text = "Reassemble",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Specs Trigger Pill
            Surface(
                shape = CircleShape,
                color = BadgeDark,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSpecsClick)
            ) {
                Text(
                    text = "SPECS",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Opaque White Bottom Sheet per Design.md (§2 Surfaces & Shape: 32dp top corners).
 * Displays current disassembly chapter, scrub progress bar, and the Dive-In CTA.
 */
@Composable
private fun MilestoneBottomSheet(
    milestone: TeardownMilestone,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    showDiveIn: Boolean,
    onDiveInClick: () -> Unit,
    onOpenSpecsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = SheetShape,
        color = SheetWhite,
        shadowElevation = 0.dp // Separation comes from color contrast against #DD5622
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            // Milestone Chapter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = milestone.title,
                    color = InkDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // Chapter Index Badge (e.g., "02/04")
                val chapterIndex = milestone.ordinal + 1
                Surface(
                    shape = CircleShape,
                    color = BadgeDark
                ) {
                    Text(
                        text = "0$chapterIndex",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Milestone Description Body Text
            Text(
                text = milestone.description,
                color = InkMuted,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Thin progress track indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = InkDark,
                trackColor = Color(0xFFEFEFEF),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp string
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSec = positionMs / 1000f
                val totalSec = durationMs / 1000f
                Text(
                    text = String.format("%.1fs / %.1fs", currentSec, totalSec),
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Drag vertically to inspect",
                    color = InkMuted,
                    fontSize = 12.sp
                )
            }

            // Specs Quick Inspection Button (Active in peak disassembly window)
            if (progress >= 0.35f) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onOpenSpecsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424))
                ) {
                    Text(
                        text = "Technical Specifications",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Dive-In CTA Button (appears when progress >= 0.90)
            AnimatedVisibility(
                visible = showDiveIn,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onDiveInClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BadgeDark)
                    ) {
                        Text(
                            text = "Dive into SoC Architecture",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
