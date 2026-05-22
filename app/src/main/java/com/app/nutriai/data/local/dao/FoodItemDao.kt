package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.nutriai.data.local.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FoodItemEntity] operations.
 * Queries filter out soft-deleted records by default.
 * Search uses LIKE for partial name matching.
 *
 * Phase 4.5: Added [searchFoodByNameExact] for catalog cache matching.
 */
@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_items WHERE catalog_id = :catalogId AND deleted_at IS NULL ORDER BY name ASC")
    fun getFoodsByCatalogId(catalogId: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' AND deleted_at IS NULL ORDER BY name ASC")
    fun searchFoodsByName(query: String): Flow<List<FoodItemEntity>>

    /**
     * LIKE-based partial name match scoped to a specific catalog.
     * Used by CatalogScreen search within a single tab (Recipes or Ingredients).
     */
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' AND catalog_id = :catalogId AND deleted_at IS NULL ORDER BY name ASC")
    fun searchFoodsByNameInCatalog(query: String, catalogId: String): Flow<List<FoodItemEntity>>

    /**
     * Case-insensitive exact name match within a catalog.
     * Used by [com.app.nutriai.domain.usecase.ResolveCatalogCacheUseCase] to check
     * if an ingredient already exists in the local catalog with cached macros.
     */
    @Query("SELECT * FROM food_items WHERE LOWER(name) = LOWER(:name) AND catalog_id = :catalogId AND deleted_at IS NULL LIMIT 1")
    suspend fun searchFoodByNameExact(name: String, catalogId: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getFoodById(id: String): FoodItemEntity?

    /** Returns the row regardless of soft-delete state. Used by conflict resolution logic. */
    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodByIdIncludingDeleted(id: String): FoodItemEntity?

    /**
     * Returns all non-deleted food names for a given catalog.
     * Used to build the "existing ingredients / existing recipes" context
     * injected into the Gemma 4 prompt for name standardization.
     * Intentionally lightweight — fetches only the name column.
     */
    @Query("SELECT name FROM food_items WHERE catalog_id = :catalogId AND deleted_at IS NULL ORDER BY name ASC")
    suspend fun getAllFoodNames(catalogId: String): List<String>

    @Query("SELECT * FROM food_items WHERE is_synced = 0")
    suspend fun getUnsyncedFoods(): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItemEntity)

    @Update
    suspend fun updateFood(food: FoodItemEntity)

    @Query("UPDATE food_items SET deleted_at = :timestamp, last_modified_at = :timestamp, is_synced = 0 WHERE id = :id")
    suspend fun softDeleteFood(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE food_items SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    /**
     * Permanently removes soft-deleted rows that have already been synced to Supabase.
     *
     * Called after a successful push to reclaim local storage. Tombstones with
     * [is_synced] = 0 are intentionally kept until the next successful push,
     * since they still need to be uploaded to Supabase.
     */
    @Query("DELETE FROM food_items WHERE deleted_at IS NOT NULL AND is_synced = 1")
    suspend fun purgeSyncedTombstones()

    /** Removes all rows. Called on sign-out to prevent cross-user data leakage. */
    @Query("DELETE FROM food_items")
    suspend fun clearAll()
}
