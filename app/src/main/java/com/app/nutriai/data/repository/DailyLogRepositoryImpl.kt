package com.app.nutriai.data.repository

import com.app.nutriai.data.local.dao.DailyLogDao
import com.app.nutriai.data.local.mapper.toDomain
import com.app.nutriai.data.local.mapper.toEntity
import com.app.nutriai.data.sync.SyncEntityType
import com.app.nutriai.data.sync.SyncPushManager
import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.repository.DailyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DailyLogRepository] backed by Room via [DailyLogDao].
 * Injected by Hilt as a singleton.
 *
 * - Date range queries for dashboard macro aggregation.
 * - Unsynced queries include tombstones for cloud sync via WorkManager.
 * - Hard delete removes synced tombstones permanently.
 * - Every mutation schedules a debounced push-on-write via [SyncPushManager].
 */
@Singleton
class DailyLogRepositoryImpl @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val syncPushManager: SyncPushManager
) : DailyLogRepository {

    override fun getLogsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<DailyLog>> {
        return dailyLogDao.getLogsByDateRange(startTimestamp, endTimestamp).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsWithFoodByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<DailyLog>> {
        return dailyLogDao.getLogsWithFoodByDateRange(startTimestamp, endTimestamp).map { rows ->
            rows.map { it.toDomain() }
        }
    }

    override fun getUnsyncedLogs(): Flow<List<DailyLog>> {
        return dailyLogDao.getUnsyncedLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertLog(log: DailyLog) {
        dailyLogDao.insertLog(log.toEntity())
        syncPushManager.schedulePush(SyncEntityType.DAILY_LOG, listOf(log.id))
    }

    override suspend fun updateLog(log: DailyLog) {
        dailyLogDao.updateLog(log.toEntity())
        syncPushManager.schedulePush(SyncEntityType.DAILY_LOG, listOf(log.id))
    }

    override suspend fun softDeleteLog(id: String) {
        dailyLogDao.softDeleteLog(id)
        syncPushManager.schedulePush(SyncEntityType.DAILY_LOG, listOf(id))
    }

    override suspend fun hardDeleteLog(id: String) {
        dailyLogDao.hardDeleteLog(id)
        // Note: hard delete is for already-synced tombstones; no push needed.
    }
}
