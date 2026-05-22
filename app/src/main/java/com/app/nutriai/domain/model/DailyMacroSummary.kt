package com.app.nutriai.domain.model

import java.time.LocalDate

/**
 * Aggregated macro totals for a single calendar day.
 * Used by the Insights screen to display weekly/monthly trend charts.
 *
 * Days with no food logs are represented with all macro values as 0.0
 * so chart axes remain continuous without gaps.
 */
data class DailyMacroSummary(
    val date: LocalDate,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double
)
