package com.app.nutriai.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.model.Recommendation
import com.app.nutriai.presentation.components.ConfirmDeleteDialog
import com.app.nutriai.presentation.screens.log.LogUiState
import com.app.nutriai.presentation.components.FoodLogItem
import com.app.nutriai.domain.model.MealType
import com.app.nutriai.presentation.components.MacroSummaryCard
import com.app.nutriai.presentation.components.OfflineBanner
import com.app.nutriai.presentation.components.RecommendationCard
import com.app.nutriai.presentation.theme.NutriAiTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Home screen displaying the daily nutrition dashboard.
 * Shows a date navigator, macro summary card with progress arcs,
 * and a scrollable list of today's food logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLog: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val editSheet by viewModel.editSheet.collectAsState()
    val pendingDeleteLogId by viewModel.pendingDeleteLogId.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val macroGoals by viewModel.macroGoals.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    // Recommendation state (separate StateFlows — lifecycle independent of daily logs)
    val recommendations by viewModel.recommendations.collectAsState()
    val recsLoading by viewModel.recsLoading.collectAsState()
    val recsError by viewModel.recsError.collectAsState()
    val nextMeal by viewModel.nextMeal.collectAsState()
    val missedMeals by viewModel.missedMeals.collectAsState()
    val addedToFoods by viewModel.addedToFoods.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    // Show pull-to-refresh result messages as snackbars
    LaunchedEffect(Unit) {
        viewModel.refreshMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        isOnline = isOnline,
        isRefreshing = isRefreshing,
        calorieGoal = macroGoals.calorieGoal,
        proteinGoal = macroGoals.proteinGoal,
        carbsGoal = macroGoals.carbsGoal,
        fatGoal = macroGoals.fatGoal,
        recommendations = recommendations,
        recsLoading = recsLoading,
        recsError = recsError,
        nextMeal = nextMeal,
        missedMeals = missedMeals,
        addedToFoods = addedToFoods,
        snackbarHostState = snackbarHostState,
        onNavigateToLog = onNavigateToLog,
        onPullToRefresh = viewModel::onPullToRefresh,
        onPreviousDay = viewModel::goToPreviousDay,
        onNextDay = viewModel::goToNextDay,
        onGoToToday = viewModel::goToToday,
        onDeleteLog = viewModel::requestDeleteLog,
        onEditLog = viewModel::startEditLog,
        onAddRecommendationToCatalog = viewModel::addRecommendationToCatalog
    )

    // Delete confirmation dialog
    if (pendingDeleteLogId != null) {
        val foodName = uiState.dailyLogs.find { it.id == pendingDeleteLogId }
            ?.foodName?.ifBlank { null } ?: "this entry"
        ConfirmDeleteDialog(
            title = "Delete Log Entry",
            message = "Remove \"$foodName\" from your log? This cannot be undone.",
            onConfirm = viewModel::confirmDeleteLog,
            onDismiss = viewModel::cancelDeleteLog
        )
    }

    // Edit log bottom sheet
    if (editSheet != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelEdit,
            sheetState = sheetState
        ) {
            EditLogSheetContent(
                sheet = editSheet!!,
                onQtyChange = viewModel::updateEditQty,
                onUnitChange = viewModel::updateEditUnit,
                onCaloriesChange = viewModel::updateEditCalories,
                onProteinChange = viewModel::updateEditProtein,
                onCarbsChange = viewModel::updateEditCarbs,
                onFatChange = viewModel::updateEditFat,
                onMealTypeChange = viewModel::updateEditMealType,
                onSave = viewModel::saveEditedLog,
                onCancel = viewModel::cancelEdit
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    isOnline: Boolean = true,
    isRefreshing: Boolean = false,
    calorieGoal: Double = 2000.0,
    proteinGoal: Double = 150.0,
    carbsGoal: Double = 250.0,
    fatGoal: Double = 65.0,
    recommendations: List<Recommendation> = emptyList(),
    recsLoading: Boolean = false,
    recsError: String? = null,
    nextMeal: MealType? = null,
    missedMeals: List<MealType> = emptyList(),
    addedToFoods: Set<String> = emptySet(),
    snackbarHostState: SnackbarHostState,
    onNavigateToLog: () -> Unit,
    onPullToRefresh: () -> Unit = {},
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    onDeleteLog: (String) -> Unit,
    onEditLog: (String) -> Unit = {},
    onAddRecommendationToCatalog: (Recommendation) -> Unit = {}
) {
    // Pull-to-refresh via raw pointer tracking (PointerEventPass.Initial).
    // NestedScrollConnection was abandoned because LazyColumn's built-in overscroll effect
    // (rubber-band stretch) consumes all downward-pull events before onPostScroll fires.
    // Instead we intercept at Initial pass — before any child processes the event — and
    // independently measure the downward drag when the list is at the top.
    val refreshThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    var pullProgress by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log food",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Initial pass fires before any child (LazyColumn / overscroll) processes
                    // the event. We observe the raw y-delta without consuming, so the list
                    // scrolls and overscrolls exactly as normal.
                    .pointerInput(onPullToRefresh, isRefreshing) {
                        if (isRefreshing) {
                            pullProgress = 0f
                            return@pointerInput
                        }
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pullProgress = 0f

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break

                                if (!change.pressed) {
                                    // Finger lifted — trigger if threshold met
                                    if (pullProgress >= refreshThresholdPx) onPullToRefresh()
                                    pullProgress = 0f
                                    break
                                }

                                val dy = change.position.y - change.previousPosition.y
                                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                                              listState.firstVisibleItemScrollOffset == 0

                                when {
                                    isAtTop && dy > 0 ->
                                        pullProgress = (pullProgress + dy)
                                            .coerceAtMost(refreshThresholdPx * 1.5f)
                                    dy < 0 ->
                                        pullProgress = (pullProgress + dy).coerceAtLeast(0f)
                                }
                            }
                        }
                    }
                    .padding(innerPadding)
            ) {
                // Determinate circular indicator while dragging (fills toward threshold)
                if (pullProgress > 0f && !isRefreshing) {
                    CircularProgressIndicator(
                        progress = { (pullProgress / refreshThresholdPx).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        strokeWidth = 3.dp
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Offline banner (animated — slides in when offline)
                item {
                    OfflineBanner(isVisible = !isOnline)
                }

                // App title
                item {
                    Text(
                        text = "NutriAI",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Date navigation header
                item {
                    DateNavigationHeader(
                        selectedDate = uiState.selectedDate,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay,
                        onGoToToday = onGoToToday
                    )
                }

                // Macro summary card (goals come from user DataStore prefs)
                item {
                    MacroSummaryCard(
                        calories = uiState.totalCalories,
                        protein = uiState.totalProtein,
                        carbs = uiState.totalCarbs,
                        fat = uiState.totalFat,
                        calorieGoal = calorieGoal,
                        proteinGoal = proteinGoal,
                        carbsGoal = carbsGoal,
                        fatGoal = fatGoal
                    )
                }

                // Recommendation card (only for today)
                if (uiState.selectedDate == LocalDate.now()) {
                    item(key = "recommendation_card") {
                        Spacer(modifier = Modifier.height(8.dp))
                        RecommendationCard(
                            recommendations = recommendations,
                            isLoading = recsLoading,
                            error = recsError,
                            nextMeal = nextMeal,
                            missedMeals = missedMeals,
                            addedToFoods = addedToFoods,
                            onAddToMyFoods = onAddRecommendationToCatalog
                        )
                    }
                }

                // Section header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Food Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.dailyLogs.size} item${if (uiState.dailyLogs.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Food logs — grouped by meal type (Breakfast → Lunch → Snack → Dinner)
                if (uiState.dailyLogs.isEmpty()) {
                    item {
                        EmptyLogState()
                    }
                } else {
                    val groupedLogs = uiState.dailyLogs.groupBy { MealType.fromString(it.mealType) }
                    val sectionOrder = listOf(
                        MealType.BREAKFAST,
                        MealType.LUNCH,
                        MealType.SNACK,
                        MealType.DINNER
                    )

                    for (type in sectionOrder) {
                        val logsInSection = groupedLogs[type] ?: continue
                        val sectionKcal = logsInSection.sumOf { it.totalCalories }.toInt()

                        // Meal type section header
                        item(key = "header_${type.value}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = type.emoji,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = type.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$sectionKcal kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            items = logsInSection,
                            key = { it.id }
                        ) { log ->
                            FoodLogItem(
                                log = log,
                                foodName = log.foodName.ifBlank { "Unknown Food" },
                                onEdit = { logId -> onEditLog(logId) },
                                onDelete = onDeleteLog
                            )
                        }
                    }

                    // Uncategorized logs (legacy entries with null mealType)
                    val uncategorized = groupedLogs[null]
                    if (uncategorized != null) {
                        item(key = "header_other") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OTHER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uncategorized.sumOf { it.totalCalories }.toInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(
                            items = uncategorized,
                            key = { it.id }
                        ) { log ->
                            FoodLogItem(
                                log = log,
                                foodName = log.foodName.ifBlank { "Unknown Food" },
                                onEdit = { logId -> onEditLog(logId) },
                                onDelete = onDeleteLog
                            )
                        }
                    }
                }

                    // Bottom spacer for FAB clearance
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }

                // Indeterminate circular spinner pinned at top while sync is running
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        strokeWidth = 3.dp
                    )
                }

                // Snackbar overlay pinned to top — outside Scaffold's snackbarHost
                // slot so it renders at the top of the screen instead of the bottom.
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            } // end Box
        }
    }
}

/**
 * Date navigation header with previous/next arrows and a "Today" shortcut.
 */
@Composable
private fun DateNavigationHeader(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous day",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isToday) "Today" else selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isToday) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onNextDay,
                enabled = !isToday
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = if (isToday)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isToday) {
            TextButton(onClick = onGoToToday) {
                Text("Go to Today")
            }
        }
    }
}

/**
 * Empty state shown when no food has been logged for the selected date.
 */
@Composable
private fun EmptyLogState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No food logged",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap + to start tracking your nutrition!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NutriAiTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isLoading = false,
                selectedDate = LocalDate.now(),
                dailyLogs = listOf(
                    DailyLog(
                        id = "1",
                        userId = "local_user",
                        foodItemId = "food1",
                        foodName = "Oatmeal with Honey",
                        dateTimestamp = System.currentTimeMillis(),
                        consumedQty = 1.0,
                        consumedUnit = "bowl",
                        totalCalories = 280.0,
                        totalProtein = 8.0,
                        totalCarbs = 48.0,
                        totalFat = 6.0,
                        lastModifiedAt = System.currentTimeMillis()
                    ),
                    DailyLog(
                        id = "2",
                        userId = "local_user",
                        foodItemId = "food2",
                        foodName = "Grilled Chicken Breast",
                        dateTimestamp = System.currentTimeMillis(),
                        consumedQty = 1.0,
                        consumedUnit = "serving",
                        totalCalories = 165.0,
                        totalProtein = 31.0,
                        totalCarbs = 0.0,
                        totalFat = 3.6,
                        lastModifiedAt = System.currentTimeMillis()
                    )
                ),
                totalCalories = 445.0,
                totalProtein = 39.0,
                totalCarbs = 48.0,
                totalFat = 9.6
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateToLog = {},
            onPreviousDay = {},
            onNextDay = {},
            onGoToToday = {},
            onDeleteLog = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    NutriAiTheme {
        HomeScreenContent(
            uiState = HomeUiState(isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateToLog = {},
            onPreviousDay = {},
            onNextDay = {},
            onGoToToday = {},
            onDeleteLog = {}
        )
    }
}

/**
 * Content of the edit log bottom sheet.
 * Allows the user to update quantity, unit, and macro values for an existing log entry.
 * The food name and date are shown read-only — only consumption details are editable.
 */
@Composable
private fun EditLogSheetContent(
    sheet: EditLogSheet,
    onQtyChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .imePadding()
    ) {
        // Header
        Text(
            text = "Edit Log Entry",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sheet.foodName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Meal type selector
        com.app.nutriai.presentation.components.MealTypeSelector(
            selected = sheet.mealType,
            onSelect = onMealTypeChange
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Quantity + Unit row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = sheet.qty,
                onValueChange = onQtyChange,
                label = { Text("Quantity *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = sheet.errorMessage != null
            )
            // Unit dropdown — constrained to the same options as the Log screen
            var unitDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = sheet.unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(pass = PointerEventPass.Initial)
                                unitDropdownExpanded = true
                            }
                        },
                    singleLine = true
                )
                DropdownMenu(
                    expanded = unitDropdownExpanded,
                    onDismissRequest = { unitDropdownExpanded = false }
                ) {
                    LogUiState.UNIT_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onUnitChange(option)
                                unitDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (sheet.errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sheet.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Macro fields in 2x2 grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = sheet.calories,
                onValueChange = onCaloriesChange,
                label = { Text("Calories (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = sheet.protein,
                onValueChange = onProteinChange,
                label = { Text("Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = sheet.carbs,
                onValueChange = onCarbsChange,
                label = { Text("Carbs (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = sheet.fat,
                onValueChange = onFatChange,
                label = { Text("Fat (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !sheet.isSaving
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !sheet.isSaving
            ) {
                if (sheet.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}
