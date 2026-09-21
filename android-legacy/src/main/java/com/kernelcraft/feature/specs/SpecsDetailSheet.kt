package com.kernelcraft.feature.specs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Three-state anchored positioning for the Specs sliding panel.
 */
enum class SpecsSheetValue {
    COLLAPSED, // Fully dismissed
    PEEK,      // Partial expansion showing summary chips
    EXPANDED   // Full screen deep dive
}

// Studio Theme Palette
private val SheetSurfaceDark = Color(0xF0141414)
private val CardSurfaceDark = Color(0xFF1F1F1F)
private val AccentOrange = Color(0xFFDD5622)
private val AccentMustard = Color(0xFFE8A020)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)
private val SheetBorder = Color(0x28FFFFFF)
private val HandleColor = Color(0x55FFFFFF)

/**
 * Interactive, gesture-driven Technical Specifications panel.
 *
 * Implements:
 * - Three-state sliding sheet: COLLAPSED, PEEK, EXPANDED.
 * - Frosted studio dark aesthetic matching edge-to-edge #DD5622 backdrop.
 * - Bidirectional handoff: swiping down smoothly dismisses the sheet and unfreezes teardown scrubbing.
 * - Strict edge-to-edge window inset compliance across status and navigation bars.
 *
 * @param isVisible Whether the sheet is currently active.
 * @param onDismiss Invoked when the user dismisses the sheet (swipe down or close button).
 * @param modifier Optional root modifier.
 * @param categories Hardware specifications dataset.
 */
@Composable
fun SpecsDetailSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<SpecCategory> = HardwareSpecsData.categories,
    summaryChips: List<SummaryChip> = HardwareSpecsData.summaryChips
) {
    if (!isVisible) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val statusBarPaddingPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarPaddingPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeightPx = with(density) { maxHeight.toPx() }

        // Anchor heights
        val peekHeightPx = with(density) { 230.dp.toPx() } + navBarPaddingPx
        val expandedTopInsetPx = statusBarPaddingPx + with(density) { 16.dp.toPx() }

        val collapsedOffsetY = totalHeightPx
        val peekOffsetY = totalHeightPx - peekHeightPx
        val expandedOffsetY = expandedTopInsetPx

        // Animated vertical position of the sheet
        val sheetOffsetY = remember { Animatable(collapsedOffsetY) }
        var currentSheetState by remember { mutableStateOf(SpecsSheetValue.PEEK) }

        // Animate entrance into PEEK state when first presented
        LaunchedEffect(isVisible) {
            if (isVisible) {
                sheetOffsetY.snapTo(collapsedOffsetY)
                sheetOffsetY.animateTo(
                    targetValue = peekOffsetY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                currentSheetState = SpecsSheetValue.PEEK
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        fun animateToState(targetState: SpecsSheetValue) {
            coroutineScope.launch {
                val targetOffset = when (targetState) {
                    SpecsSheetValue.COLLAPSED -> collapsedOffsetY
                    SpecsSheetValue.PEEK -> peekOffsetY
                    SpecsSheetValue.EXPANDED -> expandedOffsetY
                }
                sheetOffsetY.animateTo(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                currentSheetState = targetState
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (targetState == SpecsSheetValue.COLLAPSED) {
                    onDismiss()
                }
            }
        }

        // Scrim opacity linked to expansion progress
        val scrimAlpha by remember {
            derivedStateOf {
                val progress = ((peekOffsetY - sheetOffsetY.value) / (peekOffsetY - expandedOffsetY))
                    .coerceIn(0f, 1f)
                0.55f * progress
            }
        }

        // Background dimming scrim (visible during expanded state)
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { animateToState(SpecsSheetValue.PEEK) }
                    )
            )
        }

        // Nested scroll connection coordinating sheet expansion and internal list scrolling
        val nestedScrollConnection = remember(peekOffsetY, expandedOffsetY, collapsedOffsetY) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val deltaY = available.y
                    // Dragging UP while sheet is not fully expanded -> expand sheet
                    if (deltaY < 0 && sheetOffsetY.value > expandedOffsetY) {
                        val newOffset = (sheetOffsetY.value + deltaY).coerceAtLeast(expandedOffsetY)
                        val consumedY = newOffset - sheetOffsetY.value
                        coroutineScope.launch { sheetOffsetY.snapTo(newOffset) }
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val deltaY = available.y
                    // Dragging DOWN while list is scrolled to top -> collapse sheet
                    if (deltaY > 0 && available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                        val newOffset = (sheetOffsetY.value + deltaY).coerceAtMost(collapsedOffsetY)
                        val consumedY = newOffset - sheetOffsetY.value
                        coroutineScope.launch { sheetOffsetY.snapTo(newOffset) }
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val velocityY = available.y
                    if (velocityY < -800f) {
                        animateToState(SpecsSheetValue.EXPANDED)
                        return available
                    } else if (velocityY > 800f) {
                        if (sheetOffsetY.value < peekOffsetY) {
                            animateToState(SpecsSheetValue.PEEK)
                        } else {
                            animateToState(SpecsSheetValue.COLLAPSED)
                        }
                        return available
                    } else {
                        // Settle to nearest anchor
                        val current = sheetOffsetY.value
                        val distToExpanded = kotlin.math.abs(current - expandedOffsetY)
                        val distToPeek = kotlin.math.abs(current - peekOffsetY)
                        val distToCollapsed = kotlin.math.abs(current - collapsedOffsetY)

                        val nearest = minOf(distToExpanded, distToPeek, distToCollapsed)
                        when (nearest) {
                            distToExpanded -> animateToState(SpecsSheetValue.EXPANDED)
                            distToPeek -> animateToState(SpecsSheetValue.PEEK)
                            else -> animateToState(SpecsSheetValue.COLLAPSED)
                        }
                        return available
                    }
                }
            }
        }

        // Header drag gesture listener
        val headerDraggableState = rememberDraggableState { deltaY ->
            coroutineScope.launch {
                val newOffset = (sheetOffsetY.value + deltaY).coerceIn(expandedOffsetY, collapsedOffsetY)
                sheetOffsetY.snapTo(newOffset)
            }
        }

        // Sliding Panel Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, sheetOffsetY.value.roundToInt()) }
                .height(with(density) { (totalHeightPx - expandedOffsetY).toDp() })
                .nestedScroll(nestedScrollConnection),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = SheetSurfaceDark,
            border = BorderStroke(1.dp, SheetBorder),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Drag Handle & Header Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .draggable(
                            state = headerDraggableState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocityY ->
                                if (velocityY < -600f) {
                                    animateToState(SpecsSheetValue.EXPANDED)
                                } else if (velocityY > 600f) {
                                    if (sheetOffsetY.value < peekOffsetY) {
                                        animateToState(SpecsSheetValue.PEEK)
                                    } else {
                                        animateToState(SpecsSheetValue.COLLAPSED)
                                    }
                                } else {
                                    val mid = (expandedOffsetY + peekOffsetY) / 2f
                                    if (sheetOffsetY.value < mid) {
                                        animateToState(SpecsSheetValue.EXPANDED)
                                    } else {
                                        animateToState(SpecsSheetValue.PEEK)
                                    }
                                }
                            }
                        )
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grab Handle Pill
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(HandleColor)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Bar with Expand Toggle & Close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TECHNICAL SPECIFICATIONS",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            Text(
                                text = "Hardware Architecture & Silicon Matrix",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Close / Collapse Circular Button
                        IconButton(
                            onClick = { animateToState(SpecsSheetValue.COLLAPSED) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                        ) {
                            Text(
                                text = "X",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Summary Chips Row (Always visible in PEEK & EXPANDED)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (chip in summaryChips) {
                        SummaryChipItem(
                            chip = chip,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentSheetState != SpecsSheetValue.EXPANDED) {
                                    animateToState(SpecsSheetValue.EXPANDED)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = SheetBorder, thickness = 1.dp)

                // Scrollable Detailed Specs List (Active during EXPANDED state)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        SpecCategorySection(category = category)
                    }

                    item {
                        // Return to Teardown Button
                        Button(
                            onClick = { animateToState(SpecsSheetValue.COLLAPSED) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "← Resume Hardware Teardown",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Metric summary chip for the peek header.
 */
@Composable
private fun SummaryChipItem(
    chip: SummaryChip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CardSurfaceDark,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = chip.iconSymbol,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chip.title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = chip.metric,
                color = AccentMustard,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Categorized hardware architecture section card.
 */
@Composable
private fun SpecCategorySection(category: SpecCategory) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSurfaceDark,
        border = BorderStroke(1.dp, SheetBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = category.iconSymbol, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = category.title,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = category.subtitle,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                // Accent Tag Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = category.accentTag,
                        color = AccentMustard,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            // Spec Rows
            for (spec in category.specs) {
                SpecItemRow(spec = spec)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Individual specification data row with high-contrast badge and technical detail.
 */
@Composable
private fun SpecItemRow(spec: SpecItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spec.label,
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.45f)
            )

            Row(
                modifier = Modifier.weight(0.55f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = spec.value,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                if (spec.badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentOrange.copy(alpha = 0.20f),
                        border = BorderStroke(0.5.dp, AccentOrange.copy(alpha = 0.50f))
                    ) {
                        Text(
                            text = spec.badge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (spec.detail != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.detail,
                color = TextMuted.copy(alpha = 0.70f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
        }
    }
}
