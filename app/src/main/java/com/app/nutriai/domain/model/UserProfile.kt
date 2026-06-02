package com.app.nutriai.domain.model

/**
 * User's dietary profile — drives internet recommendation personalization.
 *
 * All fields are optional. When null/empty, the recommendation prompt omits
 * that constraint entirely (no defaults assumed).
 *
 * Phase R4: Part of the AI Recommendations infrastructure.
 */
data class UserProfile(
    val age: Int? = null,
    val gender: String? = null,
    val weightKg: Double? = null,
    val weightGoal: String? = null,        // "lose" | "maintain" | "gain"
    val dietType: String? = null,          // "vegetarian" | "veg_eggs" | "non_veg" | "pescatarian" | "vegan"
    val cuisinePreferences: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val recommendationsEnabled: Boolean = false
) {
    /** True if the user has filled out at least the required fields for internet recs. */
    val isComplete: Boolean
        get() = dietType != null && recommendationsEnabled
}
