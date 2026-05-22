package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.nutriai.domain.model.MacroGoals

/**
 * Room entity for user macro goal preferences.
 *
 * Replaces the DataStore-only approach with Room + Supabase bidirectional sync,
 * enabling cross-platform goal sharing with the webapp.
 *
 * PK is [userId] — locally always [Constants.LOCAL_USER_ID] ("local_user").
 * Defaults match [MacroGoals] and the Supabase `user_preferences` table defaults.
 *
 * Phase 14: Part of the macro goals cross-platform sync migration.
 */
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "calorie_goal")
    val calorieGoal: Double = 2000.0,

    @ColumnInfo(name = "protein_goal")
    val proteinGoal: Double = 150.0,

    @ColumnInfo(name = "carbs_goal")
    val carbsGoal: Double = 250.0,

    @ColumnInfo(name = "fat_goal")
    val fatGoal: Double = 65.0,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true,

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long = System.currentTimeMillis()
) {
    /** Converts to the domain model consumed by ViewModels. */
    fun toMacroGoals(): MacroGoals = MacroGoals(
        calorieGoal = calorieGoal,
        proteinGoal = proteinGoal,
        carbsGoal = carbsGoal,
        fatGoal = fatGoal
    )
}
