package com.app.nutriai.data.local.entity

import androidx.room.Embedded

/**
 * Room result POJO pairing a [DailyLogEntity] with its optional [FoodItemEntity].
 *
 * Used by the LEFT JOIN query in [com.app.nutriai.data.local.dao.DailyLogDao]
 * to enable dynamic macro calculation at read time:
 *   - When [food] is non-null: macros are computed as `food.baseMacro × log.consumedQty`
 *   - When [food] is null (soft-deleted or purged): mapper falls back to stored snapshot values
 *
 * Column prefix [fi_] is used to avoid conflicts with [DailyLogEntity]'s own [food_name] column.
 */
data class DailyLogWithFood(
    @Embedded
    val log: DailyLogEntity,

    @Embedded(prefix = "fi_")
    val food: FoodItemEntity?
)
