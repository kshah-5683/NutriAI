package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.nutriai.data.local.entity.CatalogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [CatalogEntity] operations.
 * Queries filter out soft-deleted records (deleted_at IS NULL) by default.
 * Unsynced queries include soft-deleted records for tombstone sync.
 */
@Dao
interface CatalogDao {

    @Query("SELECT * FROM catalogs WHERE user_id = :userId AND deleted_at IS NULL ORDER BY name ASC")
    fun getCatalogsByUserId(userId: String): Flow<List<CatalogEntity>>

    @Query("SELECT * FROM catalogs WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getCatalogById(id: String): CatalogEntity?

    /** Returns the row regardless of soft-delete state. Used by conflict resolution logic. */
    @Query("SELECT * FROM catalogs WHERE id = :id LIMIT 1")
    suspend fun getCatalogByIdIncludingDeleted(id: String): CatalogEntity?

    @Query("SELECT * FROM catalogs WHERE is_synced = 0")
    suspend fun getUnsyncedCatalogs(): List<CatalogEntity>

    /**
     * Returns the total number of non-deleted catalog rows.
     * Used by [SyncRepositoryImpl] as an empty-DB sentinel: if 0, the local database
     * was just created or wiped, and a full pull is required regardless of [lastSyncAt].
     */
    @Query("SELECT COUNT(*) FROM catalogs WHERE deleted_at IS NULL")
    suspend fun getCatalogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalog(catalog: CatalogEntity)

    @Update
    suspend fun updateCatalog(catalog: CatalogEntity)

    @Query("UPDATE catalogs SET deleted_at = :timestamp, last_modified_at = :timestamp, is_synced = 0 WHERE id = :id")
    suspend fun softDeleteCatalog(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE catalogs SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    /**
     * Permanently removes soft-deleted rows that have already been synced to Supabase.
     *
     * Called after a successful push to reclaim local storage. Tombstones with
     * [is_synced] = 0 are intentionally kept until the next successful push,
     * since they still need to be uploaded to Supabase.
     */
    @Query("DELETE FROM catalogs WHERE deleted_at IS NOT NULL AND is_synced = 1")
    suspend fun purgeSyncedTombstones()

    /** Removes all rows. Called on sign-out to prevent cross-user data leakage. */
    @Query("DELETE FROM catalogs")
    suspend fun clearAll()
}
