package com.app.nutriai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.nutriai.data.local.dao.CatalogDao
import com.app.nutriai.data.local.dao.DailyLogDao
import com.app.nutriai.data.local.dao.FoodItemDao
import com.app.nutriai.data.local.dao.IfctFoodDao
import com.app.nutriai.data.local.dao.LabelPhotoDao
import com.app.nutriai.data.local.dao.UserDao
import com.app.nutriai.data.local.dao.UserPreferencesDao
import com.app.nutriai.data.local.entity.CatalogEntity
import com.app.nutriai.data.local.entity.DailyLogEntity
import com.app.nutriai.data.local.entity.FoodItemEntity
import com.app.nutriai.data.local.entity.IfctFoodEntity
import com.app.nutriai.data.local.entity.LabelPhotoEntity
import com.app.nutriai.data.local.entity.UserEntity
import com.app.nutriai.data.local.entity.UserPreferencesEntity

/**
 * Room database for NutriAI.
 *
 * This is the single source of truth for all local data.
 * All UI reads come from this database via Kotlin Flows.
 *
 * Migration strategy: explicit migrations from v3 onward (see [com.app.nutriai.data.local.migrations.Migrations]).
 * Destructive fallback is only permitted for pre-release versions 1 and 2.
 *
 * Version history:
 *   v1 — Initial schema (User, Catalog, FoodItem, DailyLog)
 *   v2 — Phase 5.5: Added IfctFoodEntity (offline IFCT 2017 nutrition data)
 *   v3 — Phase 7 fix: Added food_name column to daily_logs (snapshotted food name)
 *   v4 — Phase 8 Pre-work II: Migration infrastructure scaffolding (no schema change)
 *   v5 — FK fix: Changed daily_logs.food_item_id from ON DELETE CASCADE to ON DELETE SET NULL.
 *        Prevents tombstone purge from cascade-deleting historical log entries.
 *   v6 — Phase 11: Added LabelPhotoEntity (local-only label photo metadata, 10-day TTL)
 *   v7 — Phase 14: Added UserPreferencesEntity (macro goals cross-platform sync)
 */
@Database(
    entities = [
        UserEntity::class,
        CatalogEntity::class,
        FoodItemEntity::class,
        DailyLogEntity::class,
        IfctFoodEntity::class,
        LabelPhotoEntity::class,
        UserPreferencesEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class NutriAiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun catalogDao(): CatalogDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun ifctFoodDao(): IfctFoodDao
    abstract fun labelPhotoDao(): LabelPhotoDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}
