package com.app.nutriai.data.local.mapper

import com.app.nutriai.data.local.entity.DailyLogEntity
import com.app.nutriai.data.local.entity.DailyLogWithFood
import com.app.nutriai.domain.model.DailyLog

/**
 * Extension functions mapping between [DailyLogEntity] (data layer) and [DailyLog] (domain layer).
 */

fun DailyLogEntity.toDomain(): DailyLog = DailyLog(
    id = id,
    userId = userId,
    foodItemId = foodItemId,
    foodName = foodName,
    dateTimestamp = dateTimestamp,
    consumedQty = consumedQty,
    consumedUnit = consumedUnit,
    totalCalories = totalCalories,
    totalProtein = totalProtein,
    totalCarbs = totalCarbs,
    totalFat = totalFat,
    isSynced = isSynced,
    lastModifiedAt = lastModifiedAt,
    deletedAt = deletedAt
)

/**
 * Maps a [DailyLogWithFood] to a [DailyLog] domain model using **dynamic macro calculation**.
 *
 * When the joined [DailyLogWithFood.food] is non-null (food item exists and is not soft-deleted):
 *   - Macros are computed as `food.baseMacro × log.consumedQty` — always reflects current
 *     food item values even if they were updated after the log was created.
 *   - [DailyLog.foodName] is resolved from the live food item name.
 *
 * When [DailyLogWithFood.food] is null (food item was soft-deleted or hard-deleted/purged):
 *   - Falls back to the stored snapshot values in [DailyLogEntity] — macros and food name
 *     are preserved as they were at log-creation time.
 *
 * This mapping is the **display path** only. Stored [DailyLogEntity] values are kept current
 * by [com.app.nutriai.data.local.dao.DailyLogDao.recalculateLogsForFoodItem] (called from
 * [com.app.nutriai.data.repository.FoodRepositoryImpl.updateFood] and the sync pull path) so
 * that sync push sends correct totals to other devices.
 *
 * Unit note: [DailyLogEntity.consumedQty] is always in "number of servings" — unit conversion
 * happens at food-item creation time and is baked into [food.baseCalories] etc.
 * The formula `baseMacro × consumedQty` is therefore correct for all unit types.
 */
fun DailyLogWithFood.toDomain(): DailyLog {
    val food = this.food
    return DailyLog(
        id            = log.id,
        userId        = log.userId,
        foodItemId    = log.foodItemId,
        // Live name when food exists; stored snapshot when deleted/purged.
        foodName      = food?.name ?: log.foodName,
        dateTimestamp = log.dateTimestamp,
        consumedQty   = log.consumedQty,
        consumedUnit  = log.consumedUnit,
        // Dynamic calc when food exists; stored snapshot fallback when null.
        totalCalories = if (food != null) food.baseCalories * log.consumedQty else log.totalCalories,
        totalProtein  = if (food != null) food.baseProtein  * log.consumedQty else log.totalProtein,
        totalCarbs    = if (food != null) food.baseCarbs    * log.consumedQty else log.totalCarbs,
        totalFat      = if (food != null) food.baseFat      * log.consumedQty else log.totalFat,
        isSynced      = log.isSynced,
        lastModifiedAt = log.lastModifiedAt,
        deletedAt     = log.deletedAt
    )
}

fun DailyLog.toEntity(): DailyLogEntity = DailyLogEntity(
    id = id,
    userId = userId,
    foodItemId = foodItemId,
    foodName = foodName,
    dateTimestamp = dateTimestamp,
    consumedQty = consumedQty,
    consumedUnit = consumedUnit,
    totalCalories = totalCalories,
    totalProtein = totalProtein,
    totalCarbs = totalCarbs,
    totalFat = totalFat,
    isSynced = isSynced,
    lastModifiedAt = lastModifiedAt,
    deletedAt = deletedAt
)
