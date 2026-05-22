package com.app.nutriai.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.data.local.preferences.UserPreferences
import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.usecase.DeleteLogUseCase
import com.app.nutriai.domain.usecase.GetDailyLogsUseCase
import com.app.nutriai.domain.usecase.InitializeUserUseCase
import com.app.nutriai.domain.usecase.UpdateDailyLogUseCase
import com.app.nutriai.data.sync.SyncThrottleManager
import com.app.nutriai.util.ConnectivityObserver
import com.app.nutriai.util.UnitConverter
import com.app.nutriai.util.formatMacro
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
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
     */
    fun confirmDeleteLog() {
        val logId = _pendingDeleteLogId.value ?: return
        _pendingDeleteLogId.value = null
        viewModelScope.launch {
            try {
                deleteLogUseCase(logId)
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
            baseFat = log.totalFat / storedQty
        )
    }

    /**
     * Updates the quantity field and auto-recalculates total macros.
     *
     * Recalculation uses the per-serving base macros captured at pre-fill time:
     * `newTotal = base × newStoredQty` where `newStoredQty` accounts for
     * gram→multiplier conversion via [UnitConverter.fromDisplayQty].
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
                    isSynced = false,
                    lastModifiedAt = System.currentTimeMillis()
                )
                updateDailyLogUseCase(updatedLog)
                _editSheet.value = null  // close sheet on success
            } catch (e: Exception) {
                _editSheet.value = _editSheet.value?.copy(
                    isSaving = false,
                    errorMessage = "Failed to save changes. Please try again."
                )
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
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)
