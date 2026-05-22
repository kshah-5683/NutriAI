package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.repository.DailyLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fetches daily logs for a given date range via [DailyLogRepository].
 * Returns a reactive [Flow] that emits whenever the underlying data changes.
 *
 * Uses [DailyLogRepository.getLogsWithFoodByDateRange] so macros are always computed
 * dynamically from the current [com.app.nutriai.domain.model.FoodItem] values at read time.
 * If a food item has been deleted, macro values fall back to the stored snapshot.
 *
 * Room observes both [daily_logs] and [food_items] tables, so the Flow re-emits
 * automatically whenever either table is updated — no manual invalidation needed.
 */
class GetDailyLogsUseCase @Inject constructor(
    private val dailyLogRepository: DailyLogRepository
) {
    operator fun invoke(startTimestamp: Long, endTimestamp: Long): Flow<List<DailyLog>> {
        return dailyLogRepository.getLogsWithFoodByDateRange(startTimestamp, endTimestamp)
    }
}
