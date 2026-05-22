package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.repository.DailyLogRepository
import javax.inject.Inject

/**
 * Validates and updates an existing [DailyLog] entry.
 *
 * Enforces that the consumed quantity is positive before persisting.
 */
class UpdateDailyLogUseCase @Inject constructor(
    private val dailyLogRepository: DailyLogRepository
) {
    suspend operator fun invoke(log: DailyLog) {
        require(log.consumedQty > 0) { "Quantity must be positive" }
        dailyLogRepository.updateLog(log)
    }
}
