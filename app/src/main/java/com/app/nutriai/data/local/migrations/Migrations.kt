package com.app.nutriai.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central registry of all Room schema migrations for NutriAI.
 *
 * Rules:
 * - Every version bump from v3 onward MUST have an explicit Migration object here.
 * - No-op migrations (infrastructure bumps with no real schema change) use an empty body.
 * - Destructive fallback is only allowed for pre-release versions 1 and 2.
 *   Any version ≥ 3 must migrate cleanly to avoid user data loss.
 *
 * Version history:
 *   v3 → v4  Phase 8 Pre-work II: Migration infrastructure scaffolding (no schema change)
 *   v5 → v6  Phase 11: Added label_photos table (nutrition label scanner)
 */
object Migrations {

    /**
     * v3 → v4: No schema change. This migration exists solely to bootstrap the explicit
     * migration infrastructure and allow the Room version bump without wiping user data.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No-op: no schema changes in this version bump.
        }
    }

    /**
     * v4 → v5: Change daily_logs.food_item_id FK from ON DELETE CASCADE to ON DELETE SET NULL.
     *
     * Problem: The CASCADE FK caused tombstone purge of food items to cascade-delete all
     *          associated daily log rows, destroying historical macro data.
     * Fix:     SET NULL preserves the daily log row — food_item_id becomes null when the
     *          referenced food item is hard-deleted. The LEFT JOIN query and mapper fall back
     *          to stored snapshot macros and food name when food_item_id is null.
     *
     * SQLite does not support ALTER TABLE to change FK constraints. The table must be
     * recreated: CREATE new → INSERT → DROP old → RENAME new → recreate indices.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create the new table with SET NULL FK and nullable food_item_id.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS daily_logs_new (
                    id               TEXT NOT NULL PRIMARY KEY,
                    user_id          TEXT NOT NULL,
                    food_item_id     TEXT,
                    food_name        TEXT NOT NULL DEFAULT '',
                    date_timestamp   INTEGER NOT NULL,
                    consumed_qty     REAL NOT NULL,
                    consumed_unit    TEXT NOT NULL,
                    total_calories   REAL NOT NULL,
                    total_protein    REAL NOT NULL,
                    total_carbs      REAL NOT NULL,
                    total_fat        REAL NOT NULL,
                    is_synced        INTEGER NOT NULL,
                    last_modified_at INTEGER NOT NULL,
                    deleted_at       INTEGER,
                    FOREIGN KEY (user_id)      REFERENCES users(id)      ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY (food_item_id) REFERENCES food_items(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )

            // 2. Copy all existing data.
            db.execSQL(
                """
                INSERT INTO daily_logs_new (
                    id, user_id, food_item_id, food_name, date_timestamp,
                    consumed_qty, consumed_unit,
                    total_calories, total_protein, total_carbs, total_fat,
                    is_synced, last_modified_at, deleted_at
                )
                SELECT
                    id, user_id, food_item_id, food_name, date_timestamp,
                    consumed_qty, consumed_unit,
                    total_calories, total_protein, total_carbs, total_fat,
                    is_synced, last_modified_at, deleted_at
                FROM daily_logs
                """.trimIndent()
            )

            // 3. Drop old table.
            db.execSQL("DROP TABLE daily_logs")

            // 4. Rename new table.
            db.execSQL("ALTER TABLE daily_logs_new RENAME TO daily_logs")

            // 5. Recreate indices (must match @Entity indices exactly).
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_logs_user_id ON daily_logs (user_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_logs_food_item_id ON daily_logs (food_item_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_logs_date_timestamp ON daily_logs (date_timestamp)")
        }
    }

    /**
     * v5 → v6: Add `label_photos` table for nutrition label scanner (Phase 11).
     *
     * Stores metadata for locally-cached label photos: UUID primary key,
     * absolute file path, creation timestamp (for TTL cleanup), and source type.
     * Photos are stored as JPEG files in filesDir/label_photos/.
     * This table is purely local — it is never synced to Supabase.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS label_photos (
                    id          TEXT NOT NULL PRIMARY KEY,
                    file_path   TEXT NOT NULL,
                    created_at  INTEGER NOT NULL,
                    source_type TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    /**
     * v6 → v7: Add `user_preferences` table for macro goals cross-platform sync (Phase 14).
     *
     * Enables bidirectional sync of daily macro goals (calories, protein, carbs, fat)
     * between Android (Room) and the webapp (Supabase `user_preferences` table).
     * Replaces the DataStore-only approach in [UserPreferences].
     *
     * Schema mirrors the Supabase table (migration 006) with additional local-only
     * columns: `is_synced` (dirty flag) and `last_modified_at` (LWW resolution).
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_preferences (
                    user_id          TEXT NOT NULL PRIMARY KEY,
                    calorie_goal     REAL NOT NULL DEFAULT 2000,
                    protein_goal     REAL NOT NULL DEFAULT 150,
                    carbs_goal       REAL NOT NULL DEFAULT 250,
                    fat_goal         REAL NOT NULL DEFAULT 65,
                    is_synced        INTEGER NOT NULL DEFAULT 1,
                    last_modified_at INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
    }

    /**
     * All migrations in ascending order. Pass as: `.addMigrations(*Migrations.ALL)`
     */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7
    )
}
