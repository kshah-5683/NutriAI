package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.UserProfile

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
 * Phase R4: Extended with dietary profile columns for AI recommendations.
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
    val lastModifiedAt: Long = System.currentTimeMillis(),

    // ── Profile columns (Phase R4: AI Recommendations) ──────────────────

    @ColumnInfo(name = "age")
    val age: Int? = null,

    @ColumnInfo(name = "gender")
    val gender: String? = null,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,

    @ColumnInfo(name = "weight_goal")
    val weightGoal: String? = null,

    @ColumnInfo(name = "diet_type")
    val dietType: String? = null,

    /** Stored as comma-separated string in Room (e.g., "Indian,Italian,Japanese").
     *  Room has no native array type — CSV string with split/join in mappers.
     *  Supabase uses TEXT[] (Postgres array). */
    @ColumnInfo(name = "cuisine_preferences")
    val cuisinePreferences: String? = null,

    /** Stored as comma-separated string in Room (e.g., "Gluten,Dairy,Nuts").
     *  Same CSV approach as cuisinePreferences. */
    @ColumnInfo(name = "allergies")
    val allergies: String? = null,

    @ColumnInfo(name = "recommendations_enabled")
    val recommendationsEnabled: Boolean = false
) {
    /** Converts to the domain model consumed by ViewModels. */
    fun toMacroGoals(): MacroGoals = MacroGoals(
        calorieGoal = calorieGoal,
        proteinGoal = proteinGoal,
        carbsGoal = carbsGoal,
        fatGoal = fatGoal
    )

    /** Converts profile columns to the domain model consumed by ViewModels. */
    fun toUserProfile(): UserProfile = UserProfile(
        age = age,
        gender = gender,
        weightKg = weightKg,
        weightGoal = weightGoal,
        dietType = dietType,
        cuisinePreferences = cuisinePreferences
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        allergies = allergies
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        recommendationsEnabled = recommendationsEnabled
    )
}
