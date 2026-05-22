package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.nutriai.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserPreferencesEntity].
 *
 * Single-row table keyed by [userId]. Supports bidirectional sync with
 * the Supabase `user_preferences` table (migration 006).
 *
 * Phase 14: Part of the macro goals cross-platform sync migration.
 */
@Dao
interface UserPreferencesDao {

    /**
     * Observes the preferences row for the given user.
     * Returns null if no row exists (first launch before any goals are set).
     */
    @Query("SELECT * FROM user_preferences WHERE user_id = :userId LIMIT 1")
    fun getPreferencesFlow(userId: String): Flow<UserPreferencesEntity?>

    /** Blocking read for sync and migration logic. */
    @Query("SELECT * FROM user_preferences WHERE user_id = :userId LIMIT 1")
    suspend fun getPreferences(userId: String): UserPreferencesEntity?

    /** Returns the row if it has unsent local changes. */
    @Query("SELECT * FROM user_preferences WHERE user_id = :userId AND is_synced = 0 LIMIT 1")
    suspend fun getUnsyncedPreferences(userId: String): UserPreferencesEntity?

    /** Upsert — INSERT or REPLACE on PK conflict. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(entity: UserPreferencesEntity)

    @Query("UPDATE user_preferences SET is_synced = 1 WHERE user_id = :userId")
    suspend fun markAsSynced(userId: String)

    /** Removes all rows. Called on sign-out to prevent cross-user data leakage. */
    @Query("DELETE FROM user_preferences")
    suspend fun clearAll()
}
