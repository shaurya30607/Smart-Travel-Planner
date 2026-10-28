package com.example.smarttravelplanner.ui.screens.tripdetail

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.example.smarttravelplanner.ui.components.RagSourcesCard
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.ui.screens.tripedit.ActivityFormSheet
import com.example.smarttravelplanner.ui.theme.*
import com.example.smarttravelplanner.ui.screens.agentshare.SendToAgentButton
import com.example.smarttravelplanner.util.DestinationImageHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    /** Opens the Edit trip screen (pencil in the header). */
    onEditClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Interactive target for editing / regenerating activity (dayIndex, activityIndex, activity)
    var editingActivityTarget by remember { mutableStateOf<Triple<Int, Int, Activity>?>(null) }
    var regeneratingActivityTarget by remember { mutableStateOf<Triple<Int, Int, Activity>?>(null) }

    // "Removed X" with Undo
    LaunchedEffect(uiState.lastDeletedActivity) {
        val deleted = uiState.lastDeletedActivity ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Removed “${deleted.title}” from Day ${deleted.dayNumber}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        } else {
            viewModel.dismissUndo()
        }
    }

    LaunchedEffect(uiState.userNotice) {
        val notice = uiState.userNotice ?: return@LaunchedEffect
        if (uiState.lastDeletedActivity == null) {
            snackbarHostState.showSnackbar(
                message = notice,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserNotice()
        }
    }

    // Coming back from Edit trip: reload the trip's details (name, dates, budget…)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshTrip() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.error != null -> FullScreenError(
                message = uiState.error!!,
                onRetry = onBackClick
            )
            uiState.trip != null -> TripDetailContent(
                uiState              = uiState,
                onBackClick          = onBackClick,
                onEditClick          = onEditClick,
                onDaySelect          = { viewModel.selectDay(it) },
                onViewModeSelect     = { viewModel.setViewMode(it) },
                onActivitySelect     = { viewModel.selectActivity(it) },
                onDeleteActivity     = { dayIdx, actIdx -> viewModel.deleteActivity(dayIdx, actIdx) },
                onEditActivity       = { dayIdx, actIdx, act -> editingActivityTarget = Triple(dayIdx, actIdx, act) },
                onRegenerateActivity = { dayIdx, actIdx, act -> regeneratingActivityTarget = Triple(dayIdx, actIdx, act) }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp)
                .zIndex(99f)
        )

        // Activity Edit Sheet (Left Swipe action / Full swipe)
        val editTarget = editingActivityTarget
        if (editTarget != null) {
            val (dayIdx, actIdx, act) = editTarget
            val itin = uiState.itineraries.getOrNull(dayIdx)
            if (itin != null) {
                ActivityFormSheet(
                    dayNumber = itin.dayNumber,
                    existing = act,
                    onSave = { updated ->
                        viewModel.updateActivity(dayIdx, actIdx, updated)
                        editingActivityTarget = null
                    },
                    onDismiss = { editingActivityTarget = null },
                    onRegenerateWithAi = { base, instructions, onComplete ->
                        viewModel.regenerateActivity(
                            dayIndex = dayIdx,
                            activityIndex = actIdx,
                            instructions = instructions,
                            baseActivity = base
                        ) { res ->
                            res.onSuccess { onComplete(it) }
                        }
                    },
                    isRegenerating = uiState.isRegenerating
                )
            }
        }

        // Quick AI Regenerate Dialog
        val regenTarget = regeneratingActivityTarget
        if (regenTarget != null) {
            val (dayIdx, actIdx, act) = regenTarget
            val itin = uiState.itineraries.getOrNull(dayIdx)
            if (itin != null) {
                RegenerateActivityDialog(
                    dayNumber = itin.dayNumber,
                    activity = act,
                    onDismiss = { regeneratingActivityTarget = null },
                    onRegenerate = { instructions, onGenerated ->
                        viewModel.regenerateActivity(
                            dayIndex = dayIdx,
                            activityIndex = actIdx,
                            instructions = instructions,
                            baseActivity = act
                        ) { res ->
                            res.onSuccess { onGenerated(it) }
                        }
                    },
                    onApplyReplacement = { replacement ->
                        viewModel.updateActivity(dayIdx, actIdx, replacement)
                        regeneratingActivityTarget = null
                    }
                )
            }
        }

        // Travel-agent feature: send this trip to agents (hidden on the map view)
        if (uiState.trip != null && uiState.viewMode != TripDetailViewMode.VISUALIZE) {
            SendToAgentButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Main content: Anchored Header + Selector + Animated Mode Transitions
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TripDetailContent(
    uiState: TripDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDaySelect: (Int) -> Unit,
    onViewModeSelect: (TripDetailViewMode) -> Unit,
    onActivitySelect: (Long?) -> Unit,
    onDeleteActivity: (dayIndex: Int, activityIndex: Int) -> Unit = { _, _ -> },
    onEditActivity: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _, _, _ -> },
    onRegenerateActivity: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _, _, _ -> }
) {

    val trip        = uiState.trip!!
    val itineraries = uiState.itineraries
    val weather     = uiState.weather
    val context     = LocalContext.current

    val density     = LocalDensity.current

    // Hoist list states for Timeline, Budget, and DaySelector
    val timelineListState = rememberLazyListState()
    val budgetListState = rememberLazyListState()
    val daySelectorListState = rememberLazyListState()

    // Collapse range: 340dp (expanded) down to 200dp (collapsed) -> 140dp delta
    val collapseRangeDp = 140.dp
    val collapseRangePx = with(density) { collapseRangeDp.toPx() }

    // Scroll collapse pixel offsets for Timeline and Budget
    var timelineCollapsePx by rememberSaveable { mutableFloatStateOf(0f) }
    var budgetCollapsePx by rememberSaveable { mutableFloatStateOf(0f) }

    // Derive scroll progress without unnecessary recomposition
    val timelineCollapseProgress by remember {
        derivedStateOf {
            if (timelineListState.firstVisibleItemIndex > 0 || timelineListState.firstVisibleItemScrollOffset > 0) {
                1f
            } else {
                (timelineCollapsePx / collapseRangePx).coerceIn(0f, 1f)
            }
        }
    }

    val budgetCollapseProgress by remember {
        derivedStateOf {
            if (budgetListState.firstVisibleItemIndex > 0 || budgetListState.firstVisibleItemScrollOffset > 0) {
                1f
            } else {
                (budgetCollapsePx / collapseRangePx).coerceIn(0f, 1f)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Coordinated Mode Transition (0f = Expanded/Timeline/Budget, 1f = Compact/Visualize)
    // ─────────────────────────────────────────────────────────────────────────
    val modeTransition = updateTransition(
        targetState = uiState.viewMode,
        label = "trip_detail_mode_transition"
    )

    val modeCollapseProgress by modeTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 450, easing = FastOutSlowInEasing) },
        label = "mode_collapse_progress"
    ) { mode ->
        if (mode == TripDetailViewMode.VISUALIZE) 1f else 0f
    }

    // Preserve non-visualize scroll collapse progress when switching to/from Visualize
    var lastNonVisualizeScrollProgress by rememberSaveable { mutableFloatStateOf(0f) }

    val scrollCollapseProgress = when (uiState.viewMode) {
        TripDetailViewMode.TIMELINE -> timelineCollapseProgress
        TripDetailViewMode.BUDGET -> budgetCollapseProgress
        TripDetailViewMode.VISUALIZE -> lastNonVisualizeScrollProgress
    }

    if (uiState.viewMode != TripDetailViewMode.VISUALIZE) {
        lastNonVisualizeScrollProgress = scrollCollapseProgress
    }

    // Derive ONE final collapse progress value from mode and scroll:
    val finalCollapseProgress = maxOf(modeCollapseProgress, scrollCollapseProgress).coerceIn(0f, 1f)
    val headerSpacerHeight = lerpDp(10.dp, 6.dp, finalCollapseProgress)

    // NestedScrollConnection to coordinate scroll collapse 1:1 with finger
    val nestedScrollConnection = remember(uiState.viewMode, collapseRangePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (uiState.viewMode == TripDetailViewMode.VISUALIZE) return Offset.Zero

                val dy = available.y
                // Scrolling up (dy < 0): collapse header first before scrolling list content
                if (dy < 0) {
                    val currentCollapse = if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                        timelineCollapsePx
                    } else {
                        budgetCollapsePx
                    }

                    if (currentCollapse < collapseRangePx) {
                        val newCollapse = (currentCollapse - dy).coerceIn(0f, collapseRangePx)
                        val consumedY = -(newCollapse - currentCollapse)
                        if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                            timelineCollapsePx = newCollapse
                        } else {
                            budgetCollapsePx = newCollapse
                        }
                        return Offset(0f, consumedY)
                    }
                }

                // Scrolling down (dy > 0): expand header immediately if list is at the very top
                if (dy > 0) {
                    val listState = if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                        timelineListState
                    } else {
                        budgetListState
                    }

                    val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (isAtTop) {
                        val currentCollapse = if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                            timelineCollapsePx
                        } else {
                            budgetCollapsePx
                        }
                        if (currentCollapse > 0f) {
                            val newCollapse = (currentCollapse - dy).coerceIn(0f, collapseRangePx)
                            val consumedY = -(newCollapse - currentCollapse)
                            if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                                timelineCollapsePx = newCollapse
                            } else {
                                budgetCollapsePx = newCollapse
                            }
                            return Offset(0f, consumedY)
                        }
                    }
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (uiState.viewMode == TripDetailViewMode.VISUALIZE) return Offset.Zero

                val dy = available.y
                if (dy > 0) {
                    val currentCollapse = if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                        timelineCollapsePx
                    } else {
                        budgetCollapsePx
                    }

                    if (currentCollapse > 0f) {
                        val newCollapse = (currentCollapse - dy).coerceIn(0f, collapseRangePx)
                        val consumedY = -(newCollapse - currentCollapse)
                        if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                            timelineCollapsePx = newCollapse
                        } else {
                            budgetCollapsePx = newCollapse
                        }
                        return Offset(0f, consumedY)
                    }
                }

                return Offset.Zero
            }
        }
    }

    val sheetOverlap = 24.dp
    val sheetCornerRadius = 24.dp
    val sheetShape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)
    val finalHeaderHeight = lerpDp(340.dp, 200.dp, finalCollapseProgress)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val handleVerticalDrag: (Float) -> Unit = { dragAmount ->
        if (uiState.viewMode != TripDetailViewMode.VISUALIZE) {
            val currentCollapse = if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                timelineCollapsePx
            } else {
                budgetCollapsePx
            }
            val newCollapse = (currentCollapse - dragAmount).coerceIn(0f, collapseRangePx)
            val consumed = -(newCollapse - currentCollapse)
            if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                timelineCollapsePx = newCollapse
            } else {
                budgetCollapsePx = newCollapse
            }
            val unconsumed = dragAmount - consumed
            if (unconsumed < 0) {
                val listState = if (uiState.viewMode == TripDetailViewMode.TIMELINE) {
                    timelineListState
                } else {
                    budgetListState
                }
                listState.dispatchRawDelta(-unconsumed)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. One reusable collapsing destination header whose size and child properties morph based on mode & scroll
        AnimatedDestinationHeader(
            trip = trip,
            collapseProgress = finalCollapseProgress,
            viewMode = uiState.viewMode,
            onBackClick = onBackClick,
            onShareClick = { shareTrip(context, trip, itineraries) },
            onEditClick = onEditClick,
            modifier = Modifier.align(Alignment.TopCenter),
            onVerticalDrag = handleVerticalDrag
        )

        // 2. Overlapping Tab-style Content Sheet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = (finalHeaderHeight - sheetOverlap).coerceAtLeast(0.dp))
                .shadow(
                    elevation = if (isDark) 12.dp else 8.dp,
                    shape = sheetShape,
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.60f else 0.25f),
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.15f)
                )
                .clip(sheetShape)
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = if (isDark) 1.dp else 0.8.dp,
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.02f)
                            )
                        )
                    },
                    shape = sheetShape
                )
        ) {
            // Decorative top handle pill (drag indicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .pointerInput(uiState.viewMode) {
                        detectVerticalDragGestures { _, dragAmount ->
                            handleVerticalDrag(dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.22f)
                            else Color.Black.copy(alpha = 0.15f)
                        )
                )
            }

            // Anchored View Mode Switcher (Timeline | Visualize | Budget)
            ViewModeSelector(
            selectedMode = uiState.viewMode,
            onModeSelect = onViewModeSelect
        )

        // 3. One Shared Day Selector (Shown for Timeline & Visualize, removed from Budget)
        AnimatedVisibility(
            visible = uiState.viewMode != TripDetailViewMode.BUDGET && itineraries.isNotEmpty(),
            enter = fadeIn(tween(250)) + expandVertically(tween(300, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(250, easing = FastOutSlowInEasing))
        ) {
            Column {
                Spacer(Modifier.height(headerSpacerHeight))
                DaySelector(
                    itineraries = itineraries,
                    selectedIndex = uiState.selectedDayIndex,
                    onDaySelect = onDaySelect,
                    lazyListState = daySelectorListState
                )
            }
        }

        Spacer(Modifier.height(headerSpacerHeight))

        // 4. Mode Content (Timeline / Visualize / Budget)
        val modes = remember {
            listOf(
                TripDetailViewMode.TIMELINE,
                TripDetailViewMode.VISUALIZE,
                TripDetailViewMode.BUDGET
            )
        }

        // Cache whether user has visited Visualize mode so map stays composed & retains camera position
        var hasVisitedVisualize by rememberSaveable { mutableStateOf(false) }
        if (uiState.viewMode == TripDetailViewMode.VISUALIZE) {
            hasVisitedVisualize = true
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            // Visualize (Map) Layer: Retained in memory once visited to preserve map GL state, camera position, and route
            if (hasVisitedVisualize) {
                val isVisualizeActive = uiState.viewMode == TripDetailViewMode.VISUALIZE
                val visualizeAlpha by animateFloatAsState(
                    targetValue = if (isVisualizeActive) 1f else 0f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "visualize_alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isVisualizeActive) 2f else 0f)
                        .graphicsLayer {
                            alpha = visualizeAlpha
                        }
                ) {
                    VisualizeContent(
                        trip = trip,
                        uiState = uiState,
                        onActivitySelect = onActivitySelect
                    )
                }
            }

            // Timeline & Budget Layers (Animated with M3 Expressive Shared Axis when active)
            val nonMapMode = when (uiState.viewMode) {
                TripDetailViewMode.TIMELINE -> TripDetailViewMode.TIMELINE
                TripDetailViewMode.BUDGET   -> TripDetailViewMode.BUDGET
                TripDetailViewMode.VISUALIZE -> null
            }

            AnimatedContent(
                targetState = nonMapMode,
                transitionSpec = {
                    val target = targetState
                    val initial = initialState
                    if (target == null) {
                        // Leaving Timeline/Budget to Visualize
                        val dir = if (initial == TripDetailViewMode.TIMELINE) -1 else 1
                        EnterTransition.None.togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { (it * 0.06f * dir).toInt() },
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(180, easing = FastOutLinearInEasing))
                        )
                    } else if (initial == null) {
                        // Entering Timeline/Budget from Visualize
                        val dir = if (target == TripDetailViewMode.TIMELINE) -1 else 1
                        (slideInHorizontally(
                            initialOffsetX = { (it * 0.06f * dir).toInt() },
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(220, easing = LinearOutSlowInEasing))).togetherWith(ExitTransition.None)
                    } else {
                        // Between Timeline and Budget directly
                        val dir = if (target == TripDetailViewMode.BUDGET) 1 else -1
                        (slideInHorizontally(
                            initialOffsetX = { (it * 0.06f * dir).toInt() },
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(200, easing = LinearOutSlowInEasing))).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { (-it * 0.06f * dir).toInt() },
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(180, easing = FastOutLinearInEasing))
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (uiState.viewMode != TripDetailViewMode.VISUALIZE) 1f else 0f),
                label = "timeline_budget_content"
            ) { mode ->
                when (mode) {
                    TripDetailViewMode.TIMELINE -> {
                        TimelineContent(
                            trip                 = trip,
                            uiState              = uiState,
                            itineraries          = itineraries,
                            weather              = weather,
                            context              = context,
                            onActivitySelect     = onActivitySelect,
                            onViewModeSelect     = onViewModeSelect,
                            onDeleteActivity     = onDeleteActivity,
                            onEditActivity       = onEditActivity,
                            onRegenerateActivity = onRegenerateActivity,
                            lazyListState        = timelineListState
                        )
                    }
                    TripDetailViewMode.BUDGET -> {
                        BudgetContent(
                            uiState = uiState,
                            lazyListState = budgetListState
                        )
                    }
                    TripDetailViewMode.VISUALIZE, null -> {
                        Spacer(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
}

// ─────────────────────────────────────────────────────────────────────────────
//  Timeline Mode View / Content (Weather + Activities + Swipe Interactions)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TimelineContent(
    trip: Trip,
    uiState: TripDetailUiState,
    itineraries: List<Itinerary>,
    weather: List<DayWeather>,
    context: Context,
    onActivitySelect: (Long?) -> Unit,
    onViewModeSelect: (TripDetailViewMode) -> Unit,
    onDeleteActivity: (dayIndex: Int, activityIndex: Int) -> Unit = { _, _ -> },
    onEditActivity: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _, _, _ -> },
    onRegenerateActivity: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _, _, _ -> },
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        if (itineraries.isNotEmpty()) {
            // 1. Compact Weather Capsule for Selected Day (Matches user reference design)
            val currentDayWeather = uiState.selectedDayWeather
                ?: weather.getOrNull(uiState.selectedDayIndex)
                ?: weather.firstOrNull()

            if (currentDayWeather != null) {
                item(key = "day_weather_capsule") {
                    Spacer(Modifier.height(4.dp))
                    DayWeatherBar(dayWeather = currentDayWeather)
                    Spacer(Modifier.height(6.dp))
                }
            } else if (uiState.isLoadingWeather) {
                item(key = "day_weather_loading") {
                    Spacer(Modifier.height(4.dp))
                    DayWeatherLoadingBar()
                    Spacer(Modifier.height(6.dp))
                }
            }

            // 2. Day Itinerary Activities with Material 3 Expressive Transition & Swipeable Cards
            item(key = "day_activities_wrapper") {
                AnimatedContent(
                    targetState = uiState.selectedDayIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> (fullWidth * 0.10f * direction).toInt() },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(
                            animationSpec = tween(200, easing = LinearOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.10f * direction).toInt() },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(
                                animationSpec = tween(160, easing = FastOutLinearInEasing)
                            ) + scaleOut(
                                targetScale = 0.98f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        )
                    },
                    modifier = Modifier.clipToBounds(),
                    label = "day_activities_slide"
                ) { dayIdx ->
                    val currentItinerary = itineraries.getOrNull(dayIdx)
                    if (currentItinerary == null || currentItinerary.activities.isEmpty()) {
                        EmptyDayCard()
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            currentItinerary.activities.forEachIndexed { idx, activity ->
                                key(activity.id.takeIf { it != 0L } ?: "${dayIdx}_${activity.title}_$idx") {
                                    SwipeableActivityCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        onDelete = { onDeleteActivity(dayIdx, idx) },
                                        onEdit = { onEditActivity(dayIdx, idx, activity) },
                                        onRegenerate = { onRegenerateActivity(dayIdx, idx, activity) }
                                    ) {
                                        ActivityCard(
                                            activityIndex = idx,
                                            activity = activity,
                                            isLast = idx == currentItinerary.activities.lastIndex,
                                            onViewOnMap = {
                                                onActivitySelect(activity.id)
                                                onViewModeSelect(TripDetailViewMode.VISUALIZE)
                                            },
                                            onNavigate = {
                                                if (MapUtils.isValidCoord(activity.lat, activity.lng)) {
                                                    MapUtils.launchGoogleMapsDirections(
                                                        context = context,
                                                        lat     = activity.lat!!,
                                                        lng     = activity.lng!!,
                                                        label   = activity.location.ifBlank { activity.title }
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Amber)
                }
            }
        }

        // TripWeaver: knowledge-base passages that informed this itinerary.
        // Renders nothing when the trip was generated without retrieval.
        item(key = "rag_sources") {
            RagSourcesCard(tripId = trip.id)
        }
    }
}

@Composable
private fun TimelineModeView(
    trip: Trip,
    uiState: TripDetailUiState,
    itineraries: List<Itinerary>,
    weather: List<DayWeather>,
    context: Context,
    onDaySelect: (Int) -> Unit = {},
    onActivitySelect: (Long?) -> Unit,
    onViewModeSelect: (TripDetailViewMode) -> Unit,
    onDeleteActivity: (dayIndex: Int, activityIndex: Int) -> Unit = { _, _ -> },
    onEditActivity: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _, _, _ -> },
    onRegenerateActivity: (dayIndex: Int, activityIndex: Int, activity: Activity) -> Unit = { _, _, _ -> },
    lazyListState: LazyListState = rememberLazyListState()
) = TimelineContent(
    trip                 = trip,
    uiState              = uiState,
    itineraries          = itineraries,
    weather              = weather,
    context              = context,
    onActivitySelect     = onActivitySelect,
    onViewModeSelect     = onViewModeSelect,
    onDeleteActivity     = onDeleteActivity,
    onEditActivity       = onEditActivity,
    onRegenerateActivity = onRegenerateActivity,
    lazyListState        = lazyListState
)


// ─────────────────────────────────────────────────────────────────────────────
//  Visualize (Map) Mode View / Content (Cached State & Minimal First-Time Loader)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VisualizeContent(
    trip: Trip,
    uiState: TripDetailUiState,
    onActivitySelect: (Long?) -> Unit
) {
    // Cache map loaded state: only loads the very first time, never shows loader on return!
    var hasMapEverLoaded by rememberSaveable { mutableStateOf(false) }
    var isMapDeferredReady by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasMapEverLoaded) {
            delay(120)
            isMapDeferredReady = true
        } else {
            isMapDeferredReady = true
        }
    }

    // Safety fallback: dismiss loader after 1500ms on first load if network or map tiles take too long
    LaunchedEffect(isMapDeferredReady) {
        if (isMapDeferredReady && !hasMapEverLoaded) {
            delay(1500)
            hasMapEverLoaded = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isMapDeferredReady || hasMapEverLoaded) {
            TripMapView(
                trip = trip,
                itinerary = uiState.selectedItinerary,
                selectedActivityId = uiState.selectedActivityId,
                hasMapsApiKey = uiState.hasMapsApiKey,
                onActivitySelected = onActivitySelect,
                onMapLoaded = { hasMapEverLoaded = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Minimal Material 3 Loading Screen (Shown ONLY on first initial load, zero text)
        if (!hasMapEverLoaded) {
            val skeletonAlpha by animateFloatAsState(
                targetValue = if (hasMapEverLoaded) 0f else 1f,
                animationSpec = tween(300, easing = LinearOutSlowInEasing),
                label = "map_skeleton_fade"
            )

            if (skeletonAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = skeletonAlpha }
                ) {
                    MapLoadingSkeleton()
                }
            }
        }
    }
}

@Composable
private fun VisualizeModeView(
    trip: Trip,
    uiState: TripDetailUiState,
    itineraries: List<Itinerary>,
    onDaySelect: (Int) -> Unit = {},
    onActivitySelect: (Long?) -> Unit
) = VisualizeContent(
    trip = trip,
    uiState = uiState,
    onActivitySelect = onActivitySelect
)

// ─────────────────────────────────────────────────────────────────────────────
//  Minimal Material 3 Map Loading Element (Zero Text, Minimalist Design)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MapLoadingSkeleton() {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) BackgroundDark else BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        // Minimal floating circular pill with amber progress indicator
        Surface(
            shape = CircleShape,
            color = if (isDark) SurfaceElevated else Color.White,
            border = BorderStroke(1.dp, if (isDark) CardBorderDark else CardBorderLight),
            shadowElevation = if (isDark) 4.dp else 6.dp,
            modifier = Modifier.size(54.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = Amber,
                    strokeWidth = 3.dp,
                    trackColor = Amber.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Budget Allocation Mode View / Content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BudgetContent(
    uiState: TripDetailUiState,
    lazyListState: LazyListState = rememberLazyListState()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TripBudgetAllocationView(
            uiState = uiState,
            modifier = Modifier.fillMaxSize(),
            lazyListState = lazyListState
        )
    }
}

@Composable
private fun BudgetModeView(
    uiState: TripDetailUiState,
    lazyListState: LazyListState = rememberLazyListState()
) = BudgetContent(uiState = uiState, lazyListState = lazyListState)

// ─────────────────────────────────────────────────────────────────────────────
//  Interpolation Utilities for Header Collapse
// ─────────────────────────────────────────────────────────────────────────────
private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun lerpSp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

// ─────────────────────────────────────────────────────────────────────────────
//  Collapsing & Animated Destination Header (Single Reusable Hero)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CollapsingDestinationHeader(
    trip: Trip,
    headerHeight: Dp,
    titleFontSize: Float,
    titleLineHeight: Float,
    bottomPadding: Dp,
    scrollProgress: Float,
    viewMode: TripDetailViewMode,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onVerticalDrag: ((Float) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val imageModel = remember(trip.destination, trip.coverImageUrl) {
        DestinationImageHelper.getDestinationImage(trip.destination, trip.coverImageUrl)
    }
    val imageRequest = remember(imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .setHeader("User-Agent", "SmartTravelPlanner/1.0 (Android travel planner app)")
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .build()
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Clamp progress safely between 0f (expanded) and 1f (collapsed)
    val progress = scrollProgress.coerceIn(0f, 1f)

    // Subtitle visibility and scale: fully visible at 0f, fading away by ~0.55f, completely hidden at 1f
    val subtitleAlpha = (1f - progress * 1.8f).coerceIn(0f, 1f)
    val subtitleScale = lerpFloat(1f, 0.8f, (progress * 1.5f).coerceIn(0f, 1f))

    // Preserve the houseboat subject in the Kerala image with adaptive vertical alignment
    val imageAlignmentY = lerpFloat(-0.02f, 0.12f, progress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        // Image & Adaptive Gradient layer with vertical drag detection forwarding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onVerticalDrag != null) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                onVerticalDrag(dragAmount)
                            }
                        }
                    } else Modifier
                )
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = trip.destination,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, imageAlignmentY),
                modifier = Modifier.fillMaxSize()
            )

            // Adaptive Gradient Overlay for maximum text readability and button contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.52f),
                                    0.28f to Color.Black.copy(alpha = 0.15f),
                                    0.55f to Color.Black.copy(alpha = 0.40f),
                                    1.0f to Color.Black.copy(alpha = lerpFloat(0.88f, 0.94f, progress))
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.35f),
                                    0.28f to Color.Transparent,
                                    0.55f to Color.Black.copy(alpha = 0.30f),
                                    1.0f to Color.Black.copy(alpha = lerpFloat(0.80f, 0.88f, progress))
                                )
                            )
                        }
                    )
            )
        }

        // Top Navigation & Action Row (Back and Share buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = lerpDp(8.dp, 4.dp, progress)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button with frosted glass container
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (isDark) 0.45f else 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Edit button (trip editing) — same frosted style as Share
                if (onEditClick != null) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (isDark) 0.45f else 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit trip",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }

                // Share button with frosted glass container
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (isDark) 0.45f else 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Itinerary",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Destination title & subtitle at the bottom of the hero
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = bottomPadding)
        ) {
            Text(
                text = trip.destination,
                fontFamily = PlayfairFontFamily,
                color = Color.White,
                fontSize = titleFontSize.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = titleLineHeight.sp,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.65f),
                        blurRadius = 8f
                    )
                )
            )

            if (trip.title.isNotBlank()) {
                AnimatedVisibility(
                    visible = viewMode != TripDetailViewMode.VISUALIZE && progress < 0.65f,
                    enter = fadeIn(tween(300, delayMillis = 100)) + expandVertically(tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(350, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = trip.title,
                            fontFamily = DmSansFontFamily,
                            color = Color.White.copy(alpha = 0.92f * subtitleAlpha),
                            fontSize = (13.5f * subtitleScale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = subtitleAlpha
                                scaleX = subtitleScale
                                scaleY = subtitleScale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            },
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.65f * subtitleAlpha),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedDestinationHeader(
    trip: Trip,
    collapseProgress: Float,
    viewMode: TripDetailViewMode,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onVerticalDrag: ((Float) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    val progress = collapseProgress.coerceIn(0f, 1f)
    val headerHeight = lerpDp(340.dp, 200.dp, progress)
    val titleFontSize = lerpSp(36f, 24f, progress)
    val titleLineHeight = lerpSp(42f, 28f, progress)
    val bottomPadding = lerpDp(42.dp, 30.dp, progress)

    CollapsingDestinationHeader(
        trip = trip,
        headerHeight = headerHeight,
        titleFontSize = titleFontSize,
        titleLineHeight = titleLineHeight,
        bottomPadding = bottomPadding,
        scrollProgress = progress,
        viewMode = viewMode,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        modifier = modifier,
        onVerticalDrag = onVerticalDrag,
        onEditClick = onEditClick
    )
}

@Composable
fun CollapsingDestinationHeader(
    trip: Trip,
    collapseProgress: Float,
    viewMode: TripDetailViewMode,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onVerticalDrag: ((Float) -> Unit)? = null
) = AnimatedDestinationHeader(
    trip = trip,
    collapseProgress = collapseProgress,
    viewMode = viewMode,
    onBackClick = onBackClick,
    onShareClick = onShareClick,
    modifier = modifier,
    onVerticalDrag = onVerticalDrag
)

// ─────────────────────────────────────────────────────────────────────────────
//  View Mode Selector (Segmented Control matching MyProfile Gender tab style)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ViewModeSelector(
    selectedMode: TripDetailViewMode,
    onModeSelect: (TripDetailViewMode) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val modes = remember {
        listOf(
            TripDetailViewMode.TIMELINE,
            TripDetailViewMode.VISUALIZE,
            TripDetailViewMode.BUDGET
        )
    }
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    val segmentedTrackColor = if (isDark) SurfaceElevated else SegmentedTrackLight
    val segmentedPillColor = if (isDark) Color(0xFF262A36) else Color.White
    val selectedTextColor = if (isDark) Amber else Color(0xFFB45309)
    val unselectedTextColor = if (isDark) OnBackgroundGray else OnBackgroundMedium

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(segmentedTrackColor)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / modes.size
        val targetOffset = tabWidth * selectedIndex
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            label = "mode_pill_offset"
        )

        // Sliding indicator pill with smooth transition, subtle elevation and border
        Box(
            modifier = Modifier
                .width(tabWidth)
                .fillMaxHeight()
                .offset(x = animatedOffset)
                .then(
                    if (!isDark) Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                    else Modifier.shadow(1.dp, RoundedCornerShape(20.dp))
                )
                .clip(RoundedCornerShape(20.dp))
                .background(segmentedPillColor)
                .border(
                    width = if (isDark) 1.dp else 0.5.dp,
                    color = if (isDark) CardBorderDark else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(20.dp)
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            modes.forEach { mode ->
                val isSelected = mode == selectedMode
                val label = when (mode) {
                    TripDetailViewMode.TIMELINE  -> "Timeline"
                    TripDetailViewMode.VISUALIZE -> "Visualize"
                    TripDetailViewMode.BUDGET    -> "Budget"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onModeSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Compact Day Weather Bar (Per-Day Capsule matching Reference Design)
// ─────────────────────────────────────────────────────────────────────────────
private data class WeatherDisplayMeta(
    val icon: ImageVector,
    val iconColor: Color,
    val conditionLabel: String,
    val rainText: String,
    val windText: String,
    val uvText: String
)

private fun getWeatherMeta(day: DayWeather, isDark: Boolean): WeatherDisplayMeta {
    val code = day.weatherCode
    val (icon, color, label) = when (code) {
        0 -> Triple(Icons.Outlined.WbSunny, Color(0xFFF59E0B), "Sunny")
        1 -> Triple(Icons.Outlined.WbSunny, Color(0xFFF59E0B), "Mainly Clear")
        2, 3 -> Triple(Icons.Outlined.Cloud, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), "Partly Cloudy")
        45, 48 -> Triple(Icons.Outlined.Grain, if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), "Foggy")
        51, 53, 55 -> Triple(Icons.Outlined.WaterDrop, Color(0xFF0284C7), "Drizzle")
        61, 63, 65 -> Triple(Icons.Outlined.WaterDrop, Color(0xFF0284C7), "Rain")
        66, 67 -> Triple(Icons.Outlined.AcUnit, Color(0xFF06B6D4), "Freezing Rain")
        71, 73, 75, 77, 85, 86 -> Triple(Icons.Outlined.AcUnit, Color(0xFF06B6D4), "Snow")
        80, 81, 82 -> Triple(Icons.Outlined.WaterDrop, Color(0xFF0284C7), "Showers")
        95, 96, 99 -> Triple(Icons.Outlined.Thunderstorm, Color(0xFF8B5CF6), "Thunderstorm")
        else -> Triple(
            Icons.Outlined.WbSunny,
            Color(0xFFF59E0B),
            day.description.ifBlank { "Sunny" }
        )
    }

    val maxTemp = day.maxTempC.toInt()
    val tempSeed = if (maxTemp < 0) -maxTemp else maxTemp

    // Rain percentage: derived from precipitationMm or weather code
    val rainPct = when {
        day.precipitationMm > 0.0 -> ((day.precipitationMm * 14.0) + 25.0).toInt().coerceIn(25, 95)
        code in listOf(0, 1) -> (tempSeed % 15).coerceIn(0, 15)
        code in listOf(2, 3) -> 32
        code >= 51 -> 75
        else -> 20
    }
    val rainText = "$rainPct%"

    // Wind speed: ~10-24 km/h depending on conditions
    val baseWind = when {
        code in listOf(95, 96, 99, 80, 81, 82) -> 24
        code in listOf(2, 3) -> 14
        code in listOf(0, 1) -> 12
        else -> 12
    }
    val windText = "$baseWind km/h"

    // UV Index: UV High, UV Mod, UV Low
    val uvText = when {
        (code == 0 || code == 1) && day.maxTempC >= 22.0 -> "UV High"
        day.maxTempC >= 18.0 && code <= 3 -> "UV Mod"
        else -> "UV Low"
    }

    return WeatherDisplayMeta(
        icon = icon,
        iconColor = color,
        conditionLabel = label,
        rainText = rainText,
        windText = windText,
        uvText = uvText
    )
}

@Composable
private fun DayWeatherBar(dayWeather: DayWeather) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val barBg = if (isDark) Color(0xFF1E222D) else Color(0xFFF1F5F9)
    val barBorder = if (isDark) Color(0xFF2C3240) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CircleShape,
        color = barBg,
        border = BorderStroke(1.dp, barBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        AnimatedContent(
            targetState = dayWeather,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(160)))
            },
            label = "day_weather_capsule_transition"
        ) { weatherForDay ->
            val meta = getWeatherMeta(weatherForDay, isDark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Weather Condition Icon
                Icon(
                    imageVector = meta.icon,
                    contentDescription = meta.conditionLabel,
                    tint = meta.iconColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(10.dp))

                // Max / Min Temp (Primary information)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${weatherForDay.maxTempC.toInt()}°",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / ${weatherForDay.minTempC.toInt()}°C",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Right: 3 Metric Columns (Precipitation %, Wind Speed, UV Index)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeatherMetricColumn(
                        icon = Icons.Outlined.WaterDrop,
                        value = meta.rainText
                    )
                    WeatherMetricColumn(
                        icon = Icons.Outlined.Air,
                        value = meta.windText
                    )
                    WeatherMetricColumn(
                        icon = Icons.Outlined.Schedule,
                        value = meta.uvText
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricColumn(
    icon: ImageVector,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DmSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
        )
    }
}

@Composable
private fun DayWeatherLoadingBar() {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val barBg = if (isDark) Color(0xFF1E222D) else Color(0xFFF1F5F9)
    val barBorder = if (isDark) Color(0xFF2C3240) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CircleShape,
        color = barBg,
        border = BorderStroke(1.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isDark) SurfaceElevated else Color(0xFFE2E8F0))
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 15.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) SurfaceElevated else Color(0xFFE2E8F0))
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(width = 75.dp, height = 15.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) SurfaceElevated else Color(0xFFE2E8F0))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Day Navigation Selector (Shared Persistent LazyRow across all modes)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DaySelector(
    itineraries: List<Itinerary>,
    selectedIndex: Int,
    onDaySelect: (Int) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    LazyRow(
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            items = itineraries,
            key = { if (it.id != 0L) it.id else it.dayNumber.toLong() }
        ) { itinerary ->
            val idx = itineraries.indexOf(itinerary)
            val isSelected = idx == selectedIndex
            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) Amber else MaterialTheme.colorScheme.surface,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "day_tab_bg_${itinerary.dayNumber}"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurface,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "day_tab_text_${itinerary.dayNumber}"
            )
            Surface(
                onClick = { onDaySelect(idx) },
                shape = RoundedCornerShape(12.dp),
                color = animatedBgColor,
                border = if (isSelected) null else BorderStroke(0.8.dp, if (isDark) CardBorderDark else CardBorderLight),
                shadowElevation = if (isSelected) 3.dp else if (isDark) 0.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Day ${itinerary.dayNumber}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        color = animatedTextColor,
                        fontSize = 13.5.sp
                    )
                    Spacer(Modifier.height(1.dp))
                    val dateFormatted = runCatching {
                        LocalDate.parse(itinerary.date).format(DateTimeFormatter.ofPattern("d MMM"))
                    }.getOrNull()

                    val stopsText = "${itinerary.activities.size} stops"
                    val subText = if (!dateFormatted.isNullOrBlank()) "$dateFormatted • $stopsText" else stopsText

                    Text(
                        text = subText,
                        fontSize = 11.sp,
                        fontFamily = DmSansFontFamily,
                        color = if (isSelected) BackgroundDark.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTabs(
    itineraries: List<Itinerary>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    compact: Boolean = false
) = DaySelector(
    itineraries = itineraries,
    selectedIndex = selectedIndex,
    onDaySelect = onTabSelect
)

// ─────────────────────────────────────────────────────────────────────────────
//  Timeline Activity Card (Enhanced Readability & Spacing)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActivityCard(
    activityIndex: Int,
    activity: Activity,
    isLast: Boolean,
    onViewOnMap: () -> Unit = {},
    onNavigate: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentText = if (isDark) Amber else AmberDark
    val weatherCardBg = if (isDark) Color(0xFF132338) else Color(0xFFF0F7FF)
    val weatherCardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFCCE4FF)
    val weatherCardText = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
    val costColor = if (isDark) CompletedGreen else Color(0xFF1B873F)
    val costBg = if (isDark) CompletedGreenBg else CompletedGreenBgLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline Column (Node + Spine)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
        ) {
            // Circular Step Badge
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isDark) SurfaceDark else SurfaceLight)
                    .border(2.dp, Amber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${activityIndex + 1}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = accentText
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 3.dp)
                        .background(
                            if (isDark) CardBorderDark.copy(alpha = 0.55f) else CardBorderLight.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Card Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.8.dp, if (isDark) CardBorderDark else CardBorderLight),
            shadowElevation = if (isDark) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Time pill & Category Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activity.startTime.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (activity.endTime.isNotBlank())
                                        "${activity.startTime} – ${activity.endTime}"
                                    else activity.startTime,
                                    fontSize = 12.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    CategoryBadge(activity.category, isDark)
                }

                Spacer(Modifier.height(10.dp))

                // Activity Title
                Text(
                    text = activity.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.5.sp,
                    lineHeight = 22.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Location Row
                if (activity.location.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = accentText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = activity.location,
                            fontSize = 13.5.sp,
                            fontFamily = DmSansFontFamily,
                            color = accentText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description with improved readability
                if (activity.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = activity.description,
                        fontSize = 14.sp,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Weather note (Theme-Adaptive)
                if (!activity.weatherNote.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    val noteEmoji = when {
                        activity.weatherNote.contains("golden hour", ignoreCase = true) ||
                        activity.weatherNote.contains("sunrise", ignoreCase = true) ||
                        activity.weatherNote.contains("sunset", ignoreCase = true) -> "🌅"
                        activity.weatherNote.contains("altitude", ignoreCase = true) ||
                        activity.weatherNote.contains("mountain", ignoreCase = true) ||
                        activity.weatherNote.contains("hill", ignoreCase = true) -> "🏔️"
                        activity.weatherNote.contains("rain", ignoreCase = true) ||
                        activity.weatherNote.contains("umbrella", ignoreCase = true) ||
                        activity.weatherNote.contains("storm", ignoreCase = true) ||
                        activity.weatherNote.contains("drizzle", ignoreCase = true) -> "🌧️"
                        activity.weatherNote.contains("heat", ignoreCase = true) ||
                        activity.weatherNote.contains("sun", ignoreCase = true) -> "☀️"
                        activity.weatherNote.contains("cold", ignoreCase = true) ||
                        activity.weatherNote.contains("chilly", ignoreCase = true) ||
                        activity.weatherNote.contains("snow", ignoreCase = true) -> "❄️"
                        else -> "🌤️"
                    }
                    Surface(
                        color = weatherCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, weatherCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(noteEmoji, fontSize = 14.sp)
                            Text(
                                text = activity.weatherNote,
                                fontSize = 12.5.sp,
                                fontFamily = DmSansFontFamily,
                                color = weatherCardText,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Bottom Row: Cost + Actions
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cost Pill
                    if (activity.estimatedCost > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = costBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CurrencyRupee,
                                    contentDescription = null,
                                    tint = costColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "₹${activity.estimatedCost.toInt()} est.",
                                    fontSize = 12.5.sp,
                                    fontFamily = DmSansFontFamily,
                                    color = costColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Free Entry",
                                fontSize = 12.sp,
                                fontFamily = DmSansFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Map & Directions Buttons
                    if (MapUtils.isValidCoord(activity.lat, activity.lng)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onViewOnMap,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp),
                                border = BorderStroke(1.dp, if (isDark) Amber.copy(alpha = 0.7f) else AmberDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = null,
                                    tint = accentText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Map",
                                    fontSize = 12.5.sp,
                                    fontFamily = DmSansFontFamily,
                                    color = accentText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onNavigate,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Amber,
                                    contentColor = BackgroundDark
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Directions,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Go",
                                    fontSize = 12.5.sp,
                                    fontFamily = DmSansFontFamily,
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

// ─────────────────────────────────────────────────────────────────────────────
//  Category Badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryBadge(category: ActivityCategory, isDark: Boolean) {
    val (label, icon, color) = when (category) {
        ActivityCategory.ACCOMMODATION -> Triple("Stay", Icons.Outlined.Hotel, Color(0xFF818CF8))
        ActivityCategory.FOOD          -> Triple("Food", Icons.Outlined.Restaurant, Color(0xFFF97316))
        ActivityCategory.TRANSPORT     -> Triple("Transit", Icons.Outlined.DirectionsBus, Color(0xFF38BDF8))
        ActivityCategory.NATURE        -> Triple("Nature", Icons.Outlined.Forest, Color(0xFF34D399))
        ActivityCategory.CULTURE       -> Triple("Culture", Icons.Outlined.Museum, Color(0xFFA855F7))
        ActivityCategory.SHOPPING      -> Triple("Shopping", Icons.Outlined.ShoppingBag, Color(0xFFEC4899))
        else                           -> Triple("Attraction", Icons.Outlined.Attractions, Amber)
    }

    val badgeBg = if (isDark) color.copy(alpha = 0.16f) else color.copy(alpha = 0.12f)
    val badgeText = color

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeText,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontFamily = DmSansFontFamily,
                color = badgeText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Utility composables (Loading, Error, Empty, Share)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Amber)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Loading your trip...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧭", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSansFontFamily,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = BackgroundDark)
            ) {
                Text("Go Back", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
            }
        }
    }
}

@Composable
private fun EmptyDayCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗓️", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No activities planned for this day",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp
            )
        }
    }
}

private fun shareTrip(context: Context, trip: Trip, itineraries: List<Itinerary>) {
    val summary = buildString {
        appendLine("✈️ Trip Itinerary: ${trip.destination}")
        if (trip.title.isNotBlank()) appendLine(trip.title)
        appendLine("📅 ${trip.startDate} to ${trip.endDate}")
        if (trip.budget > 0) appendLine("💰 Budget: ₹${trip.budget.toInt()}")
        appendLine()
        itineraries.forEach { day ->
            appendLine("📍 Day ${day.dayNumber} (${day.date})")
            day.activities.forEach { act ->
                val time = if (act.startTime.isNotBlank()) "[${act.startTime}] " else ""
                val loc = if (act.location.isNotBlank()) " @ ${act.location}" else ""
                appendLine("  • $time${act.title}$loc")
            }
            appendLine()
        }
        appendLine("Created with Smart Travel Planner")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Trip to ${trip.destination}")
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(intent, "Share Itinerary"))
}
