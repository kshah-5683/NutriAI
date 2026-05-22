package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.DailyLog
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for daily food log operations.
 * Implementations live in the data layer (data/repository/).
 */
interface DailyLogRepository {
    fun getLogsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<DailyLog>>

    /**
     * Returns daily logs for the given date range with macros computed dynamically from
     * the current [com.app.nutriai.domain.model.FoodItem] values.
     *
     * If the referenced food item has been deleted, macro values fall back to the stored
     * snapshot in the log row. Used by the Home screen to always show up-to-date totals
     * whenever a food item's macros are edited in the Catalog.
     */
    fun getLogsWithFoodByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<DailyLog>>

    fun getUnsyncedLogs(): Flow<List<DailyLog>>
    suspend fun insertLog(log: DailyLog)
    suspend fun updateLog(log: DailyLog)
    suspend fun softDeleteLog(id: String)
    suspend fun hardDeleteLog(id: String)
}
