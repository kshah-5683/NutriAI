package com.app.nutriai.presentation.screens.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.nutriai.domain.model.DailyMacroSummary
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.MonthlyMacroSummary
import com.app.nutriai.presentation.components.DailyAverageCard
import com.app.nutriai.presentation.components.MacroBarChart
import com.app.nutriai.presentation.components.MacroLineChart
import com.app.nutriai.presentation.components.MacroYearChart
import com.app.nutriai.presentation.components.PeriodSelector
import com.app.nutriai.presentation.theme.NutriAiTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Insights tab — displays weekly/monthly macro trend charts and period averages.
 *
 * Layout (scrollable LazyColumn):
 *  1. "Insights" title header
 *  2. Average calorie headline
 *  3. [PeriodSelector] — Week / Month segmented toggle
 *  4. [MacroBarChart] (Week) or [MacroLineChart] (Month) — animated crossfade on switch
 *  5. [DailyAverageCard] — period averages vs goals
 *  6. Bottom spacer for nav bar clearance
 */
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    InsightsScreenContent(
        uiState = uiState,
        onPeriodSelected = viewModel::selectPeriod
    )
}

@Composable
private fun InsightsScreenContent(
    uiState: InsightsUiState,
    onPeriodSelected: (InsightsPeriod) -> Unit
) {
    Scaffold { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen title
            item {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Average calorie headline
            item {
                val avgCal = uiState.averageCalories.roundToInt()
                val periodLabel = when (uiState.selectedPeriod) {
                    InsightsPeriod.WEEK -> "this week"
                    InsightsPeriod.MONTH -> "this month"
                    InsightsPeriod.YEAR -> "this year"
                }
                Text(
                    text = "Avg $avgCal kcal/day $periodLabel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Period selector
            item {
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = onPeriodSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Chart — crossfades between bar (week) and line (month) charts
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    AnimatedContent(
                        targetState = uiState.selectedPeriod,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "chart_transition"
                    ) { period ->
                        when (period) {
                            InsightsPeriod.WEEK -> {
                                if (uiState.dailySummaries.isEmpty()) {
                                    EmptyChartState(modifier = Modifier.padding(24.dp))
                                } else {
                                    MacroBarChart(
                                        summaries = uiState.dailySummaries,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            InsightsPeriod.MONTH -> {
                                if (uiState.dailySummaries.isEmpty()) {
                                    EmptyChartState(modifier = Modifier.padding(24.dp))
                                } else {
                                    MacroLineChart(
                                        summaries = uiState.dailySummaries,
                                        macroGoals = uiState.macroGoals,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            InsightsPeriod.YEAR -> {
                                if (uiState.monthlySummaries.isEmpty()) {
                                    EmptyChartState(modifier = Modifier.padding(24.dp))
                                } else {
                                    MacroYearChart(
                                        summaries = uiState.monthlySummaries,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily average summary card
            item {
                val periodLabel = when (uiState.selectedPeriod) {
                    InsightsPeriod.WEEK -> "This Week"
                    InsightsPeriod.MONTH -> "This Month"
                    InsightsPeriod.YEAR -> "This Year"
                }
                DailyAverageCard(
                    averageCalories = uiState.averageCalories,
                    averageProtein = uiState.averageProtein,
                    averageCarbs = uiState.averageCarbs,
                    averageFat = uiState.averageFat,
                    macroGoals = uiState.macroGoals,
                    periodLabel = periodLabel
                )
            }

            // Bottom spacer for bottom nav bar clearance
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun EmptyChartState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No data yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Start logging food on the Home screen\nto see your trends here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsScreenPreview() {
    NutriAiTheme {
        val today = LocalDate.now()
        InsightsScreenContent(
            uiState = InsightsUiState(
                isLoading = false,
                selectedPeriod = InsightsPeriod.WEEK,
                dailySummaries = (0 until 7).map { i ->
                    DailyMacroSummary(
                        date = today.minusDays((6 - i).toLong()),
                        totalCalories = (1400 + i * 80).toDouble(),
                        totalProtein = (80 + i * 5).toDouble(),
                        totalCarbs = (160 + i * 10).toDouble(),
                        totalFat = (40 + i * 3).toDouble()
                    )
                },
                averageCalories = 1680.0,
                averageProtein = 97.0,
                averageCarbs = 195.0,
                averageFat = 49.0,
                macroGoals = MacroGoals()
            ),
            onPeriodSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsScreenEmptyPreview() {
    NutriAiTheme {
        InsightsScreenContent(
            uiState = InsightsUiState(isLoading = false),
            onPeriodSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsScreenYearPreview() {
    NutriAiTheme {
        val currentMonth = YearMonth.now()
        InsightsScreenContent(
            uiState = InsightsUiState(
                isLoading = false,
                selectedPeriod = InsightsPeriod.YEAR,
                monthlySummaries = (0 until 12).map { i ->
                    val ym = currentMonth.minusMonths((11 - i).toLong())
                    MonthlyMacroSummary(
                        yearMonth = ym,
                        totalCalories = (38000 + i * 700).toDouble(),
                        totalProtein = (2200 + i * 90).toDouble(),
                        totalCarbs = (4800 + i * 160).toDouble(),
                        totalFat = (1100 + i * 35).toDouble(),
                        daysWithData = 18 + (i % 10)
                    )
                },
                averageCalories = 1820.0,
                averageProtein = 108.0,
                averageCarbs = 205.0,
                averageFat = 52.0,
                macroGoals = MacroGoals()
            ),
            onPeriodSelected = {}
        )
    }
}
