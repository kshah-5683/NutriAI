package com.app.nutriai.domain.model

/**
 * Domain model representing a single food log entry for a day.
 * Contains the consumed quantity/unit and computed macro totals.
 *
 * [foodName] is snapshotted at log-creation time so the display name
 * survives soft-deletion or renaming of the referenced [FoodItem].
 */
data class DailyLog(
    val id: String,
    val userId: String,
    /** Null when the referenced food item has been hard-deleted (tombstone purge). */
    val foodItemId: String? = null,
    /** Snapshot of the food's display name at the time it was logged. */
    val foodName: String = "",
    val dateTimestamp: Long,
    val consumedQty: Double,
    val consumedUnit: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val isSynced: Boolean = false,
    val lastModifiedAt: Long,
    val deletedAt: Long? = null
)
