package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.nutriai.data.local.entity.IfctFoodEntity

/**
 * Room DAO for the IFCT 2017 foods table.
 *
 * Search strategy: SQLite full-text LIKE matching against the food name.
 * Each word in the query is checked individually so "moong dal" finds
 * "Moong dal raw" and "Moong dal cooked".
 *
 * Phase 5.5: Part of the offline fallback in the FDC → IFCT → null chain.
 */
@Dao
interface IfctFoodDao {

    /**
     * Full-name fuzzy search — returns up to [limit] rows whose name contains [query].
     * Case-insensitive via SQLite LIKE (no ICU extension assumed).
     */
    @Query("SELECT * FROM ifct_foods WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchByName(query: String, limit: Int = 5): List<IfctFoodEntity>

    /**
     * Single-word prefix search — useful when [searchByName] returns nothing for a
     * multi-word query. The caller tries each word independently.
     */
    @Query("SELECT * FROM ifct_foods WHERE name LIKE '%' || :word || '%' LIMIT :limit")
    suspend fun searchByWord(word: String, limit: Int = 3): List<IfctFoodEntity>

    /** Bulk insert — IGNORE strategy skips rows already present (idempotent seeding). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<IfctFoodEntity>)

    /** Returns the total number of rows; used by [com.app.nutriai.util.IfctCsvLoader] to
     *  determine whether seeding has already happened. */
    @Query("SELECT COUNT(*) FROM ifct_foods")
    suspend fun count(): Int
}
