package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single food log entry for a day.
 * Maps to the "daily_logs" table.
 *
 * Foreign keys to [UserEntity] and [FoodItemEntity] ensure referential integrity.
 * Sync tracking via [isSynced] for WorkManager background sync.
 * Soft-delete support via [deletedAt] (tombstone for cloud sync).
 * Last-Write-Wins conflict resolution via [lastModifiedAt].
 *
 * [foodItemId] is nullable: it becomes null when the referenced food item is
 * hard-deleted (tombstone purge) thanks to [ForeignKey.SET_NULL]. This preserves
 * the daily log row with its stored snapshot macros and food name.
 */
@Entity(
    tableName = "daily_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_item_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["food_item_id"]),
        Index(value = ["date_timestamp"])
    ]
)
data class DailyLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "food_item_id")
    val foodItemId: String? = null,

    /** Snapshot of the food display name at log-creation time.
     *  Survives soft-deletion or renaming of the referenced FoodItem. */
    @ColumnInfo(name = "food_name", defaultValue = "")
    val foodName: String = "",

    @ColumnInfo(name = "date_timestamp")
    val dateTimestamp: Long,

    @ColumnInfo(name = "consumed_qty")
    val consumedQty: Double,

    @ColumnInfo(name = "consumed_unit")
    val consumedUnit: String,

    @ColumnInfo(name = "total_calories")
    val totalCalories: Double,

    @ColumnInfo(name = "total_protein")
    val totalProtein: Double,

    @ColumnInfo(name = "total_carbs")
    val totalCarbs: Double,

    @ColumnInfo(name = "total_fat")
    val totalFat: Double,

    @ColumnInfo(name = "meal_type")
    val mealType: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
