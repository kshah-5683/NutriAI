package com.app.nutriai.domain.model

/**
 * User-configured daily nutrition goals displayed as targets on the Home screen
 * macro summary card.
 *
 * Defaults match typical adult TDEE guidelines and are used on first launch
 * before the user configures their own targets.
 *
 * @param calorieGoal Daily calorie target in kcal.
 * @param proteinGoal Daily protein target in grams.
 * @param carbsGoal   Daily carbohydrate target in grams.
 * @param fatGoal     Daily fat target in grams.
 */
data class MacroGoals(
    val calorieGoal: Double = 2000.0,
    val proteinGoal: Double = 150.0,
    val carbsGoal: Double = 250.0,
    val fatGoal: Double = 65.0
)
