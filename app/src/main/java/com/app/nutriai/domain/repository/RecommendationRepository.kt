package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.Recommendation
import com.app.nutriai.util.Resource

/**
 * Repository interface for AI meal recommendations.
 *
 * Abstracts two data sources:
 * 1. `recommendation_cache` Supabase table (PostgREST read — instant, no AI call).
 * 2. `recommend-meals` Edge Function (live Gemma 4 inference — ~2-4s).
 *
 * Phase R2: Android Home Screen Recommendations.
 * Phase R2.1: Cache-first architecture matching the webapp pattern.
 */
interface RecommendationRepository {

    /**
     * Read cached recommendations from the `recommendation_cache` Supabase table.
     *
     * Returns null if no cache entry exists for this meal/date combo.
     * This is the instant primary path — no AI call involved.
     *
     * @param mealType  "breakfast" | "snack" | "lunch" | "dinner".
     * @param dateTimestamp Start-of-day epoch ms (same convention as daily_logs).
     */
    suspend fun getCachedRecommendations(
        mealType: String,
        dateTimestamp: Long
    ): List<Recommendation>?

    /**
     * Fetch AI-generated meal recommendations via live Edge Function call.
     *
     * This is the fallback path when the cache is empty (cold start).
     * On success, writes the result back to `recommendation_cache` (fire-and-forget)
     * so subsequent loads are instant cache hits.
     *
     * @param mode           "time_based" for Home screen auto-recs, "query" for on-demand search.
     * @param timeOfDay      "morning" | "afternoon" | "evening" | "night".
     * @param remainingMacros Current remaining macro budget for the day.
     * @param query          Free-text query (required when [mode] = "query").
     * @param includeInternet True to include internet-sourced suggestions (requires profile setup).
     * @param targetMeal     Optional meal type hint for the prompt (e.g., "breakfast", "dinner").
     * @param dateTimestamp   Start-of-day epoch ms — used as cache key for write-back. Null skips write-back.
     */
    suspend fun getRecommendations(
        mode: String,
        timeOfDay: String,
        remainingMacros: RemainingMacros,
        query: String? = null,
        includeInternet: Boolean = false,
        targetMeal: String? = null,
        dateTimestamp: Long? = null
    ): Resource<List<Recommendation>>

    /**
     * Fire-and-forget trigger for the `prefetch-recommendations` Edge Function.
     *
     * Called after every food log, edit, or delete to proactively refresh the
     * recommendation cache for the next meal slot. Errors are silently swallowed —
     * prefetch is best-effort and must never block or fail the primary mutation flow.
     *
     * @param dateTimestamp Start-of-day epoch ms (client-local midnight).
     * @param currentHour   Current hour in client's local timezone (0–23).
     */
    suspend fun triggerPrefetch(dateTimestamp: Long, currentHour: Int)
}

/**
 * Remaining daily macro budget — passed to the Edge Function so the AI can
 * recommend meals that fit within the user's remaining targets.
 */
data class RemainingMacros(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
