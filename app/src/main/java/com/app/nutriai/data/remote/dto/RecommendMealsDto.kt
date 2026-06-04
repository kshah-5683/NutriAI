package com.app.nutriai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for the `recommend-meals` Supabase Edge Function.
 *
 * The Edge Function handles catalog pre-filtering, frequency ranking,
 * and profile fetching server-side — the Android client only needs to
 * provide mode, time context, and remaining macros.
 */
@Serializable
data class RecommendMealsRequest(
    val mode: String,
    @SerialName("timeOfDay")
    val timeOfDay: String? = null,
    @SerialName("remainingMacros")
    val remainingMacros: RemainingMacrosDto,
    val query: String? = null,
    @SerialName("includeInternet")
    val includeInternet: Boolean = false,
    @SerialName("targetMeal")
    val targetMeal: String? = null
)

@Serializable
data class RemainingMacrosDto(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * Response DTO from the `recommend-meals` Edge Function.
 *
 * On success: [recommendations] contains the list; [error] is null.
 * On validation/scope error: [recommendations] may be empty; [error] has a message.
 */
@Serializable
data class RecommendMealsResponse(
    val recommendations: List<RecommendationDto> = emptyList(),
    val error: String? = null
)

/**
 * Response DTO when reading from the `recommendation_cache` Supabase table via PostgREST.
 *
 * The table stores AI-generated recommendations keyed by (user_id, meal_type, date_timestamp).
 * Only the `recommendations` JSONB column is selected — other columns are used server-side
 * for staleness/cooldown checks.
 *
 * Phase R2.1: Cache-first Android recommendations.
 */
@Serializable
data class RecommendationCacheDto(
    val recommendations: List<RecommendationDto> = emptyList()
)

/**
 * Push DTO for upserting into the `recommendation_cache` Supabase table via PostgREST.
 *
 * Used to write back live Edge Function results so subsequent Home screen loads
 * are instant cache hits. Mirrors the webapp's cold-start write-back pattern.
 *
 * Excludes `created_at` — Supabase trigger auto-refreshes it on every write.
 *
 * Phase R2.1: Cache write-back after live fallback.
 */
@Serializable
data class RecommendationCacheUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("meal_type") val mealType: String,
    @SerialName("date_timestamp") val dateTimestamp: Long,
    val recommendations: List<RecommendationDto>,
    @SerialName("remaining_macros") val remainingMacros: RemainingMacrosDto
)

/**
 * Request body for the `prefetch-recommendations` Edge Function.
 * Passes client-local timestamps so the server uses the correct timezone
 * for cache keys and meal progression (not the Deno server's UTC clock).
 */
@Serializable
data class PrefetchRequest(
    @SerialName("dateTimestamp")
    val dateTimestamp: Long,
    @SerialName("currentHour")
    val currentHour: Int
)

@Serializable
data class RecommendationDto(
    val name: String = "",
    val description: String = "",
    val reason: String = "",
    @SerialName("suggested_quantity")
    val suggestedQuantity: Double = 1.0,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val source: String = "catalog",
    @SerialName("food_item_id")
    val foodItemId: String? = null,
    @SerialName("recipe_text")
    val recipeText: String? = null,
    @SerialName("search_query")
    val searchQuery: String? = null,
    @SerialName("cuisine_tag")
    val cuisineTag: String? = null
)
