package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.repository.DailyLogRepository
import javax.inject.Inject

/**
 * Soft-deletes a daily log entry via [DailyLogRepository].
 * The record is kept for tombstone sync in Phase 6 (Supabase).
 */
class DeleteLogUseCase @Inject constructor(
    private val dailyLogRepository: DailyLogRepository
) {
    suspend operator fun invoke(logId: String) {
        require(logId.isNotBlank()) { "Log ID cannot be blank" }
        dailyLogRepository.softDeleteLog(logId)
    }
}
