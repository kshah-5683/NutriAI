package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.nutriai.data.local.entity.DailyLogEntity
import com.app.nutriai.data.local.entity.DailyLogWithFood
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [DailyLogEntity] operations.
 * Supports date range queries for dashboard aggregation.
 * Unsynced queries include soft-deleted records (tombstones) for cloud sync.
 * Hard delete removes records permanently after successful sync.
 */
@Dao
interface DailyLogDao {

    @Query(
        """
        SELECT * FROM daily_logs
        WHERE date_timestamp >= :startTimestamp AND date_timestamp < :endTimestamp
        AND deleted_at IS NULL
        ORDER BY date_timestamp DESC
        """
    )
    fun getLogsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<DailyLogEntity>>

    /**
     * Returns daily logs for a date range joined with their referenced [FoodItemEntity].
     *
     * Uses a LEFT JOIN so logs whose food item has been soft-deleted or purged are still
     * returned — the [DailyLogWithFood.food] field will be null in those cases and the
     * mapper falls back to stored snapshot macro values.
     *
     * The food_items columns are aliased with the [fi_] prefix to match the
     * [@Embedded(prefix = "fi_")] annotation on [DailyLogWithFood.food] and to avoid
     * a column-name collision with [DailyLogEntity]'s own [food_name] column.
     *
     * Room observes both [daily_logs] and [food_items] tables and will re-emit the Flow
     * whenever either table is written to, so macro updates in the Catalog screen are
     * reflected on the Home screen automatically — no explicit notification needed.
     */
    @Query(
        """
        SELECT
            dl.*,
            fi.id                AS fi_id,
            fi.catalog_id        AS fi_catalog_id,
            fi.name              AS fi_name,
            fi.brand             AS fi_brand,
            fi.base_serving_g    AS fi_base_serving_g,
            fi.base_calories     AS fi_base_calories,
            fi.base_protein      AS fi_base_protein,
            fi.base_carbs        AS fi_base_carbs,
            fi.base_fat          AS fi_base_fat,
            fi.external_api_id   AS fi_external_api_id,
            fi.last_modified_at  AS fi_last_modified_at,
            fi.is_synced         AS fi_is_synced,
            fi.deleted_at        AS fi_deleted_at
        FROM daily_logs dl
        LEFT JOIN food_items fi
            ON dl.food_item_id = fi.id AND fi.deleted_at IS NULL
        WHERE dl.date_timestamp >= :startTimestamp
          AND dl.date_timestamp < :endTimestamp
          AND dl.deleted_at IS NULL
        ORDER BY dl.date_timestamp DESC
        """
    )
    fun getLogsWithFoodByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<DailyLogWithFood>>

    @Query("SELECT * FROM daily_logs WHERE is_synced = 0")
    fun getUnsyncedLogs(): Flow<List<DailyLogEntity>>

    /**
     * Returns the count of non-deleted log rows.
     * Used by [SyncRepositoryImpl] as a partial-restore sentinel: if catalogs exist but
     * log count is 0, a previous pull likely crashed mid-flight and daily logs were never
     * written — force a full pull to recover them.
     */
    @Query("SELECT COUNT(*) FROM daily_logs WHERE deleted_at IS NULL")
    suspend fun getNonDeletedLogCount(): Int

    @Query("SELECT * FROM daily_logs WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getLogById(id: String): DailyLogEntity?

    /** Returns the row regardless of soft-delete state. Used by conflict resolution logic. */
    @Query("SELECT * FROM daily_logs WHERE id = :id LIMIT 1")
    suspend fun getLogByIdIncludingDeleted(id: String): DailyLogEntity?

    /**
     * Recalculates total macros for all non-deleted logs that reference [foodItemId],
     * using the food item's updated base macros.
     *
     * Formula: total_macro = base_macro × consumed_qty
     *
     * [baseCalories], [baseProtein], [baseCarbs], [baseFat] are **per-serving** values
     * (exactly as stored in [food_items] and displayed in the UI form).
     * [consumed_qty] is the number of servings — so total = base × qty with no further
     * division required.
     *
     * Also marks affected rows as unsynced so they are pushed on the next sync.
     * Called by [FoodRepositoryImpl.updateFood] to keep log totals consistent with
     * food item macro changes.
     *
     * @param foodItemId   ID of the food item whose macros changed.
     * @param baseCalories Updated calories per serving.
     * @param baseProtein  Updated protein (g) per serving.
     * @param baseCarbs    Updated carbs (g) per serving.
     * @param baseFat      Updated fat (g) per serving.
     * @param now          Timestamp for [lastModifiedAt] — defaults to current time.
     */
    @Query("""
        UPDATE daily_logs SET
            total_calories   = (:baseCalories * consumed_qty),
            total_protein    = (:baseProtein  * consumed_qty),
            total_carbs      = (:baseCarbs    * consumed_qty),
            total_fat        = (:baseFat      * consumed_qty),
            last_modified_at = :now,
            is_synced        = 0
        WHERE food_item_id = :foodItemId
          AND deleted_at IS NULL
    """)
    suspend fun recalculateLogsForFoodItem(
        foodItemId: String,
        baseCalories: Double,
        baseProtein: Double,
        baseCarbs: Double,
        baseFat: Double,
        now: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLogEntity)

    @Update
    suspend fun updateLog(log: DailyLogEntity)

    @Query("UPDATE daily_logs SET deleted_at = :timestamp, last_modified_at = :timestamp, is_synced = 0 WHERE id = :id")
    suspend fun softDeleteLog(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM daily_logs WHERE id = :id")
    suspend fun hardDeleteLog(id: String)

    @Query("UPDATE daily_logs SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    /**
     * Permanently removes soft-deleted rows that have already been synced to Supabase.
     *
     * Called after a successful push to reclaim local storage. Tombstones with
     * [is_synced] = 0 are intentionally kept until the next successful push,
     * since they still need to be uploaded to Supabase.
     */
    @Query("DELETE FROM daily_logs WHERE deleted_at IS NOT NULL AND is_synced = 1")
    suspend fun purgeSyncedTombstones()

    /** Removes all rows. Called on sign-out to prevent cross-user data leakage. */
    @Query("DELETE FROM daily_logs")
    suspend fun clearAll()
}
