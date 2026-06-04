package com.app.nutriai.domain.model

/**
 * Domain model for a single AI meal recommendation.
 *
 * Returned by the `recommend-meals` Edge Function with two possible sources:
 * - [RecommendationSource.CATALOG] — an item from the user's food catalog.
 * - [RecommendationSource.INTERNET] — an AI-generated suggestion with estimated macros.
 *
 * Macros reflect the total for [suggestedQuantity] servings (not per-serving).
 *
 * Phase R2: Android Home Screen Recommendations.
 */
data class Recommendation(
    val name: String,
    val description: String,
    val reason: String,
    val suggestedQuantity: Double = 1.0,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val source: RecommendationSource,
    val foodItemId: String? = null,
    val recipeText: String? = null,
    val searchQuery: String? = null,
    val cuisineTag: String? = null
)

enum class RecommendationSource {
    CATALOG, INTERNET;

    companion object {
        fun fromString(value: String): RecommendationSource =
            if (value.equals("catalog", ignoreCase = true)) CATALOG else INTERNET
    }
}
