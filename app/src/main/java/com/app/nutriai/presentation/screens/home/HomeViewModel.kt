package com.app.nutriai.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.data.local.preferences.UserPreferences
import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.MealType
import com.app.nutriai.domain.model.Recommendation
import com.app.nutriai.domain.repository.FoodRepository
import com.app.nutriai.domain.usecase.DeleteLogUseCase
import com.app.nutriai.domain.usecase.GetDailyLogsUseCase
import com.app.nutriai.domain.usecase.GetTimeBasedRecsUseCase
import com.app.nutriai.domain.usecase.InitializeUserUseCase
import com.app.nutriai.domain.usecase.UpdateDailyLogUseCase
import com.app.nutriai.data.sync.SyncThrottleManager
import com.app.nutriai.util.ConnectivityObserver
import com.app.nutriai.util.Constants
import com.app.nutriai.util.Resource
import com.app.nutriai.util.UnitConverter
import com.app.nutriai.util.formatMacro
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * UI state for the Home screen dashboard.
 *
 * Food names are embedded in each [DailyLog.foodName] — no separate resolution map needed.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val dailyLogs: List<DailyLog> = emptyList(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Home screen. Observes daily logs as StateFlow,
 * computes macro totals, and handles date navigation.
 *
 * Food names are read directly from [DailyLog.foodName] — no FoodRepository
 * lookup required, so names survive soft-deletion or renaming of food items.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyLogsUseCase: GetDailyLogsUseCase,
    private val deleteLogUseCase: DeleteLogUseCase,
    private val initializeUserUseCase: InitializeUserUseCase,
    private val updateDailyLogUseCase: UpdateDailyLogUseCase,
    private val userPreferences: UserPreferences,
    private val syncThrottleManager: SyncThrottleManager,
    private val getTimeBasedRecsUseCase: GetTimeBasedRecsUseCase,
    private val foodRepository: FoodRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    /** True when the device has a validated internet connection. */
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    /** User-configured daily macro goals — drives [MacroSummaryCard] progress arcs. */
    val macroGoals: StateFlow<MacroGoals> = userPreferences.macroGoalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MacroGoals()
        )

    // ── Recommendations ───────────────────────────────────────────────────

    /** Recommendation state — lifecycle independent of daily logs flow. */
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    private val _recsLoading = MutableStateFlow(false)
    val recsLoading: StateFlow<Boolean> = _recsLoading.asStateFlow()

    private val _recsError = MutableStateFlow<String?>(null)
    val recsError: StateFlow<String?> = _recsError.asStateFlow()

    /** The next meal slot being recommended for (e.g., BREAKFAST, DINNER). */
    private val _nextMeal = MutableStateFlow<MealType?>(null)
    val nextMeal: StateFlow<MealType?> = _nextMeal.asStateFlow()

    /** Standard meals the user has missed today (excludes snack). */
    private val _missedMeals = MutableStateFlow<List<MealType>>(emptyList())
    val missedMeals: StateFlow<List<MealType>> = _missedMeals.asStateFlow()

    /** Track "Add to My Foods" state per recommendation name (internet recs have no ID). */
    private val _addedToFoods = MutableStateFlow<Set<String>>(emptySet())
    val addedToFoods: StateFlow<Set<String>> = _addedToFoods.asStateFlow()

    /** Prevents re-fetching recs on recomposition — only refetch on pull-to-refresh or new session. */
    private var recsFetchedForDate: LocalDate? = null

    // ── Pull-to-refresh ───────────────────────────────────────────────────

    /** True while a pull-to-refresh sync is in flight — drives the PTR spinner. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * One-shot snackbar messages emitted after a pull-to-refresh attempt.
     * Replay = 0 so messages are not re-shown on recomposition.
     */
    private val _refreshMessage = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val refreshMessage: SharedFlow<String> = _refreshMessage.asSharedFlow()

    /**
     * Called when the user performs a pull-to-refresh gesture.
     * Delegates to [SyncThrottleManager] — at most one sync per 5 minutes.
     */
    fun onPullToRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = syncThrottleManager.triggerSync()
            _isRefreshing.value = false
            _refreshMessage.tryEmit(result.message)

            // Also refresh recommendations
            recsFetchedForDate = null  // force re-fetch
            fetchRecommendations()
        }
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Separate mutable state for the edit bottom sheet.
    // Null = sheet closed; non-null = sheet open with this log's editable fields.
    private val _editSheet = MutableStateFlow<EditLogSheet?>(null)
    val editSheet: StateFlow<EditLogSheet?> = _editSheet.asStateFlow()

    // ID of the log pending deletion confirmation.
    // Null = dialog hidden; non-null = ConfirmDeleteDialog is visible.
    private val _pendingDeleteLogId = MutableStateFlow<String?>(null)
    val pendingDeleteLogId: StateFlow<String?> = _pendingDeleteLogId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = _selectedDate
        .flatMapLatest { date ->
            val (startMillis, endMillis) = getDateBounds(date)
            getDailyLogsUseCase(startMillis, endMillis)
                .map { logs ->
                    HomeUiState(
                        isLoading = false,
                        selectedDate = date,
                        dailyLogs = logs,
                        totalCalories = logs.sumOf { it.totalCalories },
                        totalProtein = logs.sumOf { it.totalProtein },
                        totalCarbs = logs.sumOf { it.totalCarbs },
                        totalFat = logs.sumOf { it.totalFat }
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    init {
        // Bootstrap local user + default catalog on first launch
        viewModelScope.launch {
            try {
                initializeUserUseCase()
            } catch (e: Exception) {
                // Non-critical — user may already exist
            }
        }

        // Fetch recommendations once daily logs are loaded for today
        viewModelScope.launch {
            uiState.first { !it.isLoading && it.selectedDate == LocalDate.now() }
            fetchRecommendations()
        }
    }

    /**
     * Navigate to the previous day.
     */
    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    /**
     * Navigate to the next day (capped at today).
     */
    fun goToNextDay() {
        val next = _selectedDate.value.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            _selectedDate.value = next
        }
    }

    /**
     * Jump to a specific date.
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Jump to today.
     */
    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    /**
     * Stage a log entry for deletion — shows the ConfirmDeleteDialog.
     * The actual delete happens only when [confirmDeleteLog] is called.
     */
    fun requestDeleteLog(logId: String) {
        _pendingDeleteLogId.value = logId
    }

    /**
     * The user confirmed deletion — perform the soft-delete and hide the dialog.
     * After success, triggers prefetch to refresh the recommendation cache.
     */
    fun confirmDeleteLog() {
        val logId = _pendingDeleteLogId.value ?: return
        _pendingDeleteLogId.value = null
        viewModelScope.launch {
            try {
                deleteLogUseCase(logId)
                // Invalidate rec cache + trigger background prefetch
                recsFetchedForDate = null
                triggerPrefetchInBackground()
            } catch (e: Exception) {
                _refreshMessage.tryEmit("Failed to delete entry. Please try again.")
            }
        }
    }

    /**
     * The user cancelled deletion — hide the dialog without deleting.
     */
    fun cancelDeleteLog() {
        _pendingDeleteLogId.value = null
    }

    // ── Edit log bottom sheet ──────────────────────────────────────────

    /**
     * Open the edit bottom sheet for a log entry.
     * Pre-fills all editable fields from the selected [DailyLog].
     */
    fun startEditLog(logId: String) {
        val log = uiState.value.dailyLogs.find { it.id == logId } ?: return
        val foodName = log.foodName
        // Derive per-serving base macros: total / storedQty.
        // Guard against division by zero (shouldn't happen — qty is validated on save).
        val storedQty = log.consumedQty.coerceAtLeast(0.001)
        _editSheet.value = EditLogSheet(
            log = log,
            foodName = foodName,
            // Convert stored multiplier back to human-readable qty for display/editing.
            // e.g. consumedQty=2.0, unit="g" → shows "200" in the edit field.
            qty = UnitConverter.toDisplayQty(log.consumedQty, log.consumedUnit).formatMacro(),
            unit = log.consumedUnit,
            calories = log.totalCalories.formatMacro(),
            protein = log.totalProtein.formatMacro(),
            carbs = log.totalCarbs.formatMacro(),
            fat = log.totalFat.formatMacro(),
            baseCalories = log.totalCalories / storedQty,
            baseProtein = log.totalProtein / storedQty,
            baseCarbs = log.totalCarbs / storedQty,
            baseFat = log.totalFat / storedQty,
            mealType = MealType.fromString(log.mealType) ?: MealType.inferFromCurrentTime()
        )
    }

    /**
     * Updates the quantity field and auto-recalculates total macros.
     *
     * Recalculation uses the per-100g base macros captured at pre-fill time:
     * `newTotal = base × newStoredQty` where `newStoredQty` accounts for
     * gram→multiplier conversion via [UnitConverter.fromDisplayQty].
     *
     * After per-100g normalization, base macros are always per-100g regardless
     * of the original unit system, so cross-unit-type scaling works correctly.
     */
    fun updateEditQty(value: String) {
        val sheet = _editSheet.value ?: return
        val displayQty = value.toDoubleOrNull()
        if (displayQty != null && displayQty > 0) {
            val unit = sheet.unit.trim().ifBlank { "serving" }
            val storedQty = UnitConverter.fromDisplayQty(displayQty, unit)
            _editSheet.value = sheet.copy(
                qty = value,
                calories = (sheet.baseCalories * storedQty).formatMacro(),
                protein = (sheet.baseProtein * storedQty).formatMacro(),
                carbs = (sheet.baseCarbs * storedQty).formatMacro(),
                fat = (sheet.baseFat * storedQty).formatMacro(),
                errorMessage = null
            )
        } else {
            // Keep the text as-is while user is typing (e.g. empty field, partial input)
            _editSheet.value = sheet.copy(qty = value, errorMessage = null)
        }
    }

    /**
     * Updates the unit field and recalculates total macros for the current qty.
     * Changing units affects the stored multiplier (e.g. "200" in "g" → storedQty 2.0,
     * but "200" in "serving" → storedQty 200.0), so totals must be recomputed.
     *
     * After per-100g normalization, base macros are always per-100g, so
     * unit-type changes scale correctly without guards.
     */
    fun updateEditUnit(value: String) {
        val sheet = _editSheet.value ?: return
        val displayQty = sheet.qty.toDoubleOrNull()
        if (displayQty != null && displayQty > 0) {
            val unit = value.trim().ifBlank { "serving" }
            val storedQty = UnitConverter.fromDisplayQty(displayQty, unit)
            _editSheet.value = sheet.copy(
                unit = value,
                calories = (sheet.baseCalories * storedQty).formatMacro(),
                protein = (sheet.baseProtein * storedQty).formatMacro(),
                carbs = (sheet.baseCarbs * storedQty).formatMacro(),
                fat = (sheet.baseFat * storedQty).formatMacro()
            )
        } else {
            _editSheet.value = sheet.copy(unit = value)
        }
    }

    fun updateEditCalories(value: String) {
        _editSheet.value = _editSheet.value?.copy(calories = value)
    }

    fun updateEditProtein(value: String) {
        _editSheet.value = _editSheet.value?.copy(protein = value)
    }

    fun updateEditCarbs(value: String) {
        _editSheet.value = _editSheet.value?.copy(carbs = value)
    }

    fun updateEditFat(value: String) {
        _editSheet.value = _editSheet.value?.copy(fat = value)
    }

    fun updateEditMealType(type: MealType) {
        _editSheet.value = _editSheet.value?.copy(mealType = type)
    }

    fun cancelEdit() {
        _editSheet.value = null
    }

    /**
     * Save the edited log entry.
     * Validates quantity, then calls [DailyLogRepository.updateLog] with the
     * new values. The log's food item, date, and ID are preserved unchanged.
     */
    fun saveEditedLog() {
        val sheet = _editSheet.value ?: return
        val qty = sheet.qty.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            _editSheet.value = sheet.copy(errorMessage = "Quantity must be a positive number")
            return
        }
        val calories = sheet.calories.toDoubleOrNull() ?: 0.0
        val protein = sheet.protein.toDoubleOrNull() ?: 0.0
        val carbs = sheet.carbs.toDoubleOrNull() ?: 0.0
        val fat = sheet.fat.toDoubleOrNull() ?: 0.0

        _editSheet.value = sheet.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val resolvedUnit = sheet.unit.trim().ifBlank { "serving" }
                val updatedLog = sheet.log.copy(
                    // Convert display qty back to the stored multiplier form.
                    // e.g. user enters "400" with unit "g" → stores 4.0 (400÷100).
                    consumedQty = UnitConverter.fromDisplayQty(qty, resolvedUnit),
                    consumedUnit = resolvedUnit,
                    totalCalories = calories,
                    totalProtein = protein,
                    totalCarbs = carbs,
                    totalFat = fat,
                    mealType = sheet.mealType.value,
                    isSynced = false,
                    lastModifiedAt = System.currentTimeMillis()
                )
                updateDailyLogUseCase(updatedLog)
                _editSheet.value = null  // close sheet on success
                // Invalidate rec cache + trigger background prefetch
                recsFetchedForDate = null
                triggerPrefetchInBackground()
            } catch (e: Exception) {
                _editSheet.value = _editSheet.value?.copy(
                    isSaving = false,
                    errorMessage = "Failed to save changes. Please try again."
                )
            }
        }
    }

    // ── Recommendations ────────────────────────────────────────────────

    /**
     * Fetch time-based recommendations for today.
     *
     * Only fetches if:
     * - The selected date is today.
     * - We haven't already fetched for today in this session (unless pull-to-refresh).
     *
     * Updates [_nextMeal] and [_missedMeals] for the UI to display meal-aware headers.
     */
    private fun fetchRecommendations() {
        val state = uiState.value
        val date = _selectedDate.value

        // Only show recs for today
        if (date != LocalDate.now()) return
        // Skip if already fetched for this date (unless force-cleared by pull-to-refresh)
        if (recsFetchedForDate == date) return

        // Update next meal + missed meals from current logs
        val loggedMeals = state.dailyLogs.mapNotNull { MealType.fromString(it.mealType) }
        val hour = LocalTime.now().hour
        _nextMeal.value = getTimeBasedRecsUseCase.determineNextMealSlot(loggedMeals, hour)
        _missedMeals.value = getTimeBasedRecsUseCase.deriveMissedMeals(loggedMeals, hour)

        // No recs to fetch if no next meal (late night)
        if (_nextMeal.value == null) {
            _recommendations.value = emptyList()
            recsFetchedForDate = date
            return
        }

        viewModelScope.launch {
            _recsLoading.value = true
            _recsError.value = null

            val result = getTimeBasedRecsUseCase(
                totalCalories = state.totalCalories,
                totalProtein = state.totalProtein,
                totalCarbs = state.totalCarbs,
                totalFat = state.totalFat,
                goals = macroGoals.value,
                loggedMealTypes = state.dailyLogs.map { it.mealType }
            )

            when (result) {
                is Resource.Success -> {
                    _recommendations.value = result.data
                    _recsError.value = null
                    recsFetchedForDate = date
                }
                is Resource.Error -> {
                    _recsError.value = result.message
                    _recommendations.value = emptyList()
                }
                is Resource.Loading -> { /* no-op */ }
            }
            _recsLoading.value = false
        }
    }

    /**
     * Fire-and-forget: trigger prefetch-recommendations Edge Function to refresh
     * the cache for the next meal slot. Runs in a separate coroutine so it never
     * blocks the caller. Errors are silently swallowed.
     *
     * Called after food log, edit, and delete — mirrors the webapp's
     * `triggerPrefetch()` pattern.
     */
    private fun triggerPrefetchInBackground() {
        viewModelScope.launch {
            try {
                getTimeBasedRecsUseCase.triggerPrefetch()
            } catch (_: Exception) {
                // Best-effort — never block the UI
            }
        }
    }

    /**
     * Add an internet recommendation to the user's food catalog.
     * Stores per-serving macros (total / suggestedQuantity) with default serving size of 100g.
     * Background sync will push this to Supabase automatically.
     */
    fun addRecommendationToCatalog(rec: Recommendation) {
        viewModelScope.launch {
            try {
                val food = FoodItem(
                    id = UUID.randomUUID().toString(),
                    catalogId = Constants.INGREDIENT_CATALOG_ID,
                    name = rec.name,
                    baseServingG = 100.0,
                    // Per-serving macros: divide total by suggested quantity
                    baseCalories = rec.calories / rec.suggestedQuantity,
                    baseProtein = rec.protein / rec.suggestedQuantity,
                    baseCarbs = rec.carbs / rec.suggestedQuantity,
                    baseFat = rec.fat / rec.suggestedQuantity,
                    lastModifiedAt = System.currentTimeMillis()
                )
                foodRepository.insertFood(food)
                _addedToFoods.value = _addedToFoods.value + rec.name
            } catch (e: Exception) {
                _refreshMessage.tryEmit("Failed to add ${rec.name} to your foods")
            }
        }
    }

    /**
     * Returns (startOfDay, startOfNextDay) epoch millis for a given [LocalDate].
     */
    private fun getDateBounds(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return startOfDay to startOfNextDay
    }
}

/**
 * State for the edit log bottom sheet.
 * Created when the user taps the edit button on a [FoodLogItem].
 *
 * [baseCalories], [baseProtein], [baseCarbs], [baseFat] are per-serving macro
 * values derived at pre-fill time (`total / consumedQty`).  When the user
 * changes only the quantity, [HomeViewModel.updateEditQty] auto-recalculates
 * total macros as `base × newStoredQty` so pushed totals always match the qty.
 */
data class EditLogSheet(
    val log: DailyLog,
    val foodName: String,
    val qty: String,
    val unit: String,
    val calories: String,
    val protein: String,
    val carbs: String,
    val fat: String,
    // Per-serving base macros — used to auto-recalculate totals on qty change
    val baseCalories: Double = 0.0,
    val baseProtein: Double = 0.0,
    val baseCarbs: Double = 0.0,
    val baseFat: Double = 0.0,
    /** Meal type — pre-filled from the log being edited. */
    val mealType: MealType = MealType.inferFromCurrentTime(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)
