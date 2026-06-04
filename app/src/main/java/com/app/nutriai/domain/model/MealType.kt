package com.app.nutriai.domain.model

/**
 * Meal category for daily log entries.
 * Matches webapp MealType = "breakfast" | "snack" | "lunch" | "dinner".
 * Stored as TEXT in Room and Supabase for cross-platform sync compatibility.
 */
enum class MealType(val value: String, val emoji: String, val label: String) {
    BREAKFAST("breakfast", "🌅", "Breakfast"),
    LUNCH("lunch", "☀️", "Lunch"),
    SNACK("snack", "🍎", "Snack"),
    DINNER("dinner", "🌙", "Dinner");

    companion object {
        fun fromString(value: String?): MealType? =
            entries.find { it.value == value?.lowercase()?.trim() }

        /**
         * Auto-detect the most likely meal type from the current hour.
         * Matches webapp inferMealType() exactly.
         */
        fun inferFromCurrentTime(): MealType {
            val hour = java.time.LocalTime.now().hour
            return when {
                hour in 6..10 -> BREAKFAST
                hour in 11..13 -> LUNCH
                hour in 14..16 -> SNACK
                else -> DINNER
            }
        }
    }
}
