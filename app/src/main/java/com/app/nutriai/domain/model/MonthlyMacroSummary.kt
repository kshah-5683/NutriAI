package com.app.nutriai.domain.model

import java.time.YearMonth

/**
 * Aggregated macro totals for a single calendar month.
 * Used by the Insights screen yearly chart (month-wise bar chart).
 *
 * Months with no food logs are represented with all macro values as 0.0
 * so the chart x-axis stays continuous without gaps.
 *
 * @param yearMonth     The calendar month this summary covers.
 * @param totalCalories Sum of all logged calories for the month.
 * @param totalProtein  Sum of all logged protein (g) for the month.
 * @param totalCarbs    Sum of all logged carbohydrates (g) for the month.
 * @param totalFat      Sum of all logged fat (g) for the month.
 * @param daysWithData  Number of days in the month that had at least one log entry.
 *                      Used to compute meaningful daily averages per month.
 */
data class MonthlyMacroSummary(
    val yearMonth: YearMonth,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val daysWithData: Int
)
