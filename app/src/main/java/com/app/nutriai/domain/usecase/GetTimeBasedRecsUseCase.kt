package com.app.nutriai.domain.usecase

import com.app.nutriai.data.local.preferences.UserPreferences
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.MealType
import com.app.nutriai.domain.model.Recommendation
import com.app.nutriai.domain.repository.RecommendationRepository
import com.app.nutriai.domain.repository.RemainingMacros
import com.app.nutriai.util.Resource
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Use case for fetching time-based meal recommendations for the Home screen.
 *
 * Cache-first architecture (matching the webapp):
 * 1. Read from `recommendation_cache` Supabase table (instant, no AI call).
 * 2. If cache miss → fall back to live `recommend-meals` Edge Function (~2-4s).
 *
 * Also orchestrates:
 * - Remaining macro computation (goals − consumed).
 * - Time-of-day bucket detection from the device clock.
 * - Next meal slot detection based on logged meals today.
 * - Profile check to determine if internet recs are enabled.
 *
 * Returns [Resource.Success] with an empty list during late night (22:00–05:59)
 * — no recommendations are fetched.
 *
 * Phase R2: Android Home Screen Recommendations.
 * Phase R2.1: Cache-first architecture.
 */
class GetTimeBasedRecsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val userPreferences: UserPreferences
) {
    /**
     * @param totalCalories Today's consumed calories so far.
     * @param totalProtein  Today's consumed protein so far.
     * @param totalCarbs    Today's consumed carbs so far.
     * @param totalFat      Today's consumed fat so far.
     * @param goals         User's daily macro targets.
     * @param loggedMealTypes Meal types already logged today (for next-meal detection).
     */
    suspend operator fun invoke(
        totalCalories: Double,
        totalProtein: Double,
        totalCarbs: Double,
        totalFat: Double,
        goals: MacroGoals,
        loggedMealTypes: List<String?>
    ): Resource<List<Recommendation>> {
        // 1. Calculate remaining macros
        val remaining = RemainingMacros(
            calories = goals.calorieGoal - totalCalories,
            protein = goals.proteinGoal - totalProtein,
            carbs = goals.carbsGoal - totalCarbs,
            fat = goals.fatGoal - totalFat
        )

        // 2. Determine time of day from device clock
        val hour = LocalTime.now().hour
        val timeOfDay = when {
            hour in 6..10  -> "morning"
            hour in 11..14 -> "afternoon"
            hour in 15..18 -> "evening"
            hour in 19..21 -> "night"
            else -> return Resource.Success(emptyList())  // late night — skip recs
        }

        // 3. Determine next meal slot based on what's been logged today
        val loggedMeals = loggedMealTypes.mapNotNull { MealType.fromString(it) }
        val nextMeal = determineNextMealSlot(loggedMeals, hour)
            ?: return Resource.Success(emptyList())

        // 4. Try reading from recommendation_cache first (instant path)
        val startOfDayMs = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val cached = recommendationRepository.getCachedRecommendations(
            mealType = nextMeal.value,
            dateTimestamp = startOfDayMs
        )

        if (!cached.isNullOrEmpty()) {
            return Resource.Success(cached)
        }

        // 5. Cache miss — fall back to live Edge Function call.
        //    Pass startOfDayMs so the repository can write the result back to
        //    recommendation_cache (fire-and-forget) for instant subsequent loads.
        val profile = userPreferences.profileFlow.first()
        val includeInternet = profile.recommendationsEnabled

        return recommendationRepository.getRecommendations(
            mode = "time_based",
            timeOfDay = timeOfDay,
            remainingMacros = remaining,
            includeInternet = includeInternet,
            targetMeal = nextMeal.value,
            dateTimestamp = startOfDayMs
        )
    }

    /**
     * Fire-and-forget: trigger the `prefetch-recommendations` Edge Function
     * to proactively refresh the recommendation cache for the next meal slot.
     *
     * Called after every food log, edit, or delete.
     */
    suspend fun triggerPrefetch() {
        val startOfDayMs = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val currentHour = LocalTime.now().hour
        recommendationRepository.triggerPrefetch(startOfDayMs, currentHour)
    }

    /**
     * The next meal type returned from this use case — exposed so the ViewModel
     * can display meal-aware headers ("Breakfast Suggestions", etc.).
     *
     * Call this with the same parameters to determine the current next meal slot
     * without making an Edge Function call.
     */
    fun determineNextMealSlot(loggedMeals: List<MealType>, hour: Int): MealType? {
        if (hour >= 22 || hour < 6) return null

        return when {
            hour in 6..10 -> {
                if (MealType.BREAKFAST !in loggedMeals) MealType.BREAKFAST
                else MealType.SNACK
            }
            hour in 11..13 -> {
                if (MealType.LUNCH !in loggedMeals) MealType.LUNCH
                else MealType.SNACK
            }
            hour in 14..16 -> {
                if (MealType.LUNCH !in loggedMeals) MealType.LUNCH
                else MealType.SNACK
            }
            hour in 17..21 -> {
                if (MealType.DINNER !in loggedMeals) MealType.DINNER
                else MealType.SNACK
            }
            else -> null
        }
    }

    /**
     * Returns standard meals (breakfast, lunch, dinner — NOT snack) whose time
     * window has passed and haven't been logged today. Used for the "missed meals"
     * banner on the Home screen.
     */
    fun deriveMissedMeals(loggedMeals: List<MealType>, hour: Int): List<MealType> {
        val missed = mutableListOf<MealType>()
        // Breakfast window ends at 11
        if (hour >= 11 && MealType.BREAKFAST !in loggedMeals) missed.add(MealType.BREAKFAST)
        // Lunch window ends at 17
        if (hour >= 17 && MealType.LUNCH !in loggedMeals) missed.add(MealType.LUNCH)
        // Dinner window ends at 22
        if (hour >= 22 && MealType.DINNER !in loggedMeals) missed.add(MealType.DINNER)
        return missed
    }
}
