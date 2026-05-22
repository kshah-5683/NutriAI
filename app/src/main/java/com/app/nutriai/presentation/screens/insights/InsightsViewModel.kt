package com.app.nutriai.presentation.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.data.local.preferences.UserPreferences
import com.app.nutriai.domain.model.DailyMacroSummary
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.MonthlyMacroSummary
import com.app.nutriai.domain.usecase.GetDailyLogsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** Selectable time window for the Insights charts. */
enum class InsightsPeriod(val days: Int, val label: String) {
    WEEK(7, "Week"),
    MONTH(30, "Month"),
    YEAR(365, "Year")
}

/**
 * UI state for the Insights screen.
 *
 * @param dailySummaries    Per-day macro totals for WEEK / MONTH periods (zero-filled for empty days).
 * @param monthlySummaries  Per-month macro totals for the YEAR period (zero-filled for empty months).
 * @param averageCalories   Mean daily calories over the period (days with data only).
 * @param averageProtein    Mean daily protein (g) over the period.
 * @param averageCarbs      Mean daily carbs (g) over the period.
 * @param averageFat        Mean daily fat (g) over the period.
 * @param macroGoals        User-configured daily targets for goal reference lines.
 * @param selectedPeriod    Which time window is currently selected.
 * @param isLoading         True while the initial data load is in flight.
 */
data class InsightsUiState(
    val dailySummaries: List<DailyMacroSummary> = emptyList(),
    val monthlySummaries: List<MonthlyMacroSummary> = emptyList(),
    val averageCalories: Double = 0.0,
    val averageProtein: Double = 0.0,
    val averageCarbs: Double = 0.0,
    val averageFat: Double = 0.0,
    val macroGoals: MacroGoals = MacroGoals(),
    val selectedPeriod: InsightsPeriod = InsightsPeriod.WEEK,
    val isLoading: Boolean = true
)

/**
 * ViewModel for the Insights screen.
 *
 * Reuses [GetDailyLogsUseCase] (arbitrary date ranges) and aggregates logs in-memory.
 * No additional DAO queries are required.
 *
 * Data flow:
 *  selectedPeriod → date range → GetDailyLogsUseCase → aggregate → summaries
 *
 * WEEK / MONTH: groups logs by day into [DailyMacroSummary], zero-fills missing days.
 * YEAR: groups logs by [YearMonth] into [MonthlyMacroSummary], zero-fills missing months.
 *
 * Daily averages exclude fully-empty days to avoid deflating the mean.
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getDailyLogsUseCase: GetDailyLogsUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(InsightsPeriod.WEEK)
    val selectedPeriod: StateFlow<InsightsPeriod> = _selectedPeriod.asStateFlow()

    val macroGoals: StateFlow<MacroGoals> = userPreferences.macroGoalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MacroGoals()
        )

    /** Pair of (dailySummaries, monthlySummaries) recomputed whenever period changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val aggregated: StateFlow<Pair<List<DailyMacroSummary>, List<MonthlyMacroSummary>>> =
        _selectedPeriod
            .flatMapLatest { period ->
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now()
                val startDate = today.minusDays(period.days.toLong() - 1)
                val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

                getDailyLogsUseCase(startMillis, endMillis).map { logs ->
                    // Group all logs by their calendar date
                    val byDate = logs.groupBy { log ->
                        Instant.ofEpochMilli(log.dateTimestamp).atZone(zone).toLocalDate()
                    }

                    when (period) {
                        InsightsPeriod.WEEK, InsightsPeriod.MONTH -> {
                            // Build a continuous day-by-day list, zero-filling gaps
                            val daily = (0 until period.days).map { offset ->
                                val date = startDate.plusDays(offset.toLong())
                                val dayLogs = byDate[date] ?: emptyList()
                                DailyMacroSummary(
                                    date = date,
                                    totalCalories = dayLogs.sumOf { it.totalCalories },
                                    totalProtein = dayLogs.sumOf { it.totalProtein },
                                    totalCarbs = dayLogs.sumOf { it.totalCarbs },
                                    totalFat = dayLogs.sumOf { it.totalFat }
                                )
                            }
                            daily to emptyList()
                        }

                        InsightsPeriod.YEAR -> {
                            // Build 12 calendar months ending with the current month
                            val currentMonth = YearMonth.now()
                            val startMonth = currentMonth.minusMonths(11)

                            // Group logs by YearMonth
                            val byMonth = byDate.entries.groupBy(
                                keySelector = { (date, _) -> YearMonth.from(date) },
                                valueTransform = { (date, dayLogs) ->
                                    Triple(date, dayLogs, dayLogs.isNotEmpty())
                                }
                            )

                            val monthly = (0L until 12L).map { offset ->
                                val ym = startMonth.plusMonths(offset)
                                val monthEntries = byMonth[ym]
                                val daysWithData = monthEntries
                                    ?.count { (_, _, hasLogs) -> hasLogs } ?: 0
                                // Aggregate all day-logs for this month
                                val allLogsInMonth = byDate.entries
                                    .filter { (date, _) -> YearMonth.from(date) == ym }
                                    .flatMap { it.value }
                                MonthlyMacroSummary(
                                    yearMonth = ym,
                                    totalCalories = allLogsInMonth.sumOf { it.totalCalories },
                                    totalProtein = allLogsInMonth.sumOf { it.totalProtein },
                                    totalCarbs = allLogsInMonth.sumOf { it.totalCarbs },
                                    totalFat = allLogsInMonth.sumOf { it.totalFat },
                                    daysWithData = daysWithData
                                )
                            }
                            emptyList<DailyMacroSummary>() to monthly
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList<DailyMacroSummary>() to emptyList()
            )

    /**
     * Full UI state combining summaries, computed averages, goals, and period selection.
     *
     * For WEEK/MONTH: daily average excludes zero-log days.
     * For YEAR: daily average is computed across all days that have data in any month.
     */
    val uiState: StateFlow<InsightsUiState> = combine(
        aggregated,
        macroGoals,
        _selectedPeriod
    ) { (daily, monthly), goals, period ->
        val avgCalories: Double
        val avgProtein: Double
        val avgCarbs: Double
        val avgFat: Double

        when (period) {
            InsightsPeriod.WEEK, InsightsPeriod.MONTH -> {
                val daysWithData = daily.filter { it.totalCalories > 0 }
                val count = daysWithData.size.coerceAtLeast(1).toDouble()
                avgCalories = daysWithData.sumOf { it.totalCalories } / count
                avgProtein = daysWithData.sumOf { it.totalProtein } / count
                avgCarbs = daysWithData.sumOf { it.totalCarbs } / count
                avgFat = daysWithData.sumOf { it.totalFat } / count
            }
            InsightsPeriod.YEAR -> {
                // Average across days that had data, summed across all months
                val totalDays = monthly.sumOf { it.daysWithData }.coerceAtLeast(1).toDouble()
                avgCalories = monthly.sumOf { it.totalCalories } / totalDays
                avgProtein = monthly.sumOf { it.totalProtein } / totalDays
                avgCarbs = monthly.sumOf { it.totalCarbs } / totalDays
                avgFat = monthly.sumOf { it.totalFat } / totalDays
            }
        }

        InsightsUiState(
            dailySummaries = daily,
            monthlySummaries = monthly,
            averageCalories = avgCalories,
            averageProtein = avgProtein,
            averageCarbs = avgCarbs,
            averageFat = avgFat,
            macroGoals = goals,
            selectedPeriod = period,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState()
    )

    fun selectPeriod(period: InsightsPeriod) {
        _selectedPeriod.value = period
    }
}
