package com.app.nutriai.data.repository

import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.data.remote.api.SupabaseDbApiService
import com.app.nutriai.data.remote.api.SupabaseEdgeFunctionService
import com.app.nutriai.data.remote.dto.PrefetchRequest
import com.app.nutriai.data.remote.dto.RecommendMealsRequest
import com.app.nutriai.data.remote.dto.RecommendationCacheUpsertDto
import com.app.nutriai.data.remote.dto.RecommendationDto
import com.app.nutriai.data.remote.dto.RemainingMacrosDto
import com.app.nutriai.domain.model.Recommendation
import com.app.nutriai.domain.model.RecommendationSource
import com.app.nutriai.domain.repository.RecommendationRepository
import com.app.nutriai.domain.repository.RemainingMacros
import com.app.nutriai.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [RecommendationRepository] — cache-first architecture matching
 * the webapp pattern.
 *
 * Three capabilities:
 * 1. **Cache read** — `recommendation_cache` Supabase table via PostgREST (instant).
 * 2. **Live fallback** — `recommend-meals` Edge Function via Retrofit (~2-4s AI call).
 *    On success, writes the result back to `recommendation_cache` (fire-and-forget)
 *    so subsequent loads are instant cache hits.
 * 3. **Prefetch trigger** — `prefetch-recommendations` Edge Function (fire-and-forget).
 *
 * After every food log/edit/delete, [triggerPrefetch] fires the
 * `prefetch-recommendations` Edge Function to proactively refresh the cache.
 *
 * Error handling follows the [AiRepositoryImpl] pattern:
 * - HTTP 429 → rate limit message
 * - HTTP 502 → AI service unavailable
 * - Other HTTP errors → generic message with status code
 * - Network exceptions → user-friendly fallback
 *
 * Phase R2: Android Home Screen Recommendations.
 * Phase R2.1: Cache-first architecture + write-back.
 */
@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val edgeFunctionService: SupabaseEdgeFunctionService,
    private val dbApiService: SupabaseDbApiService,
    private val authPreferences: AuthPreferences
) : RecommendationRepository {

    /** Scope for fire-and-forget cache write-backs — survives caller cancellation. */
    private val writeBackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Cache read (instant, no AI call) ─────────────────────────────────

    override suspend fun getCachedRecommendations(
        mealType: String,
        dateTimestamp: Long
    ): List<Recommendation>? {
        return try {
            val response = dbApiService.getRecommendationCache(
                mealTypeFilter = "eq.$mealType",
                dateFilter = "eq.$dateTimestamp"
            )

            if (!response.isSuccessful) return null

            val rows = response.body()
            if (rows.isNullOrEmpty()) return null

            // PostgREST returns a list (filtered by RLS to current user).
            // Expect 0 or 1 rows due to the UNIQUE constraint.
            val cached = rows.first()
            if (cached.recommendations.isEmpty()) return null

            cached.recommendations.map { it.toDomain() }
        } catch (_: Exception) {
            // Cache read failure is non-fatal — fall through to live call.
            null
        }
    }

    // ── Live Edge Function call (fallback) ───────────────────────────────

    override suspend fun getRecommendations(
        mode: String,
        timeOfDay: String,
        remainingMacros: RemainingMacros,
        query: String?,
        includeInternet: Boolean,
        targetMeal: String?,
        dateTimestamp: Long?
    ): Resource<List<Recommendation>> {
        return try {
            val macrosDto = RemainingMacrosDto(
                calories = remainingMacros.calories,
                protein = remainingMacros.protein,
                carbs = remainingMacros.carbs,
                fat = remainingMacros.fat
            )

            val request = RecommendMealsRequest(
                mode = mode,
                timeOfDay = timeOfDay,
                remainingMacros = macrosDto,
                query = query,
                includeInternet = includeInternet,
                targetMeal = targetMeal
            )

            val response = edgeFunctionService.recommendMeals(request)

            if (!response.isSuccessful) {
                val errorMsg = when (response.code()) {
                    429 -> "Too many requests — please wait a moment and try again."
                    502 -> "AI service is temporarily unavailable. Please try again later."
                    else -> "Failed to get recommendations (${response.code()})."
                }
                return Resource.Error(errorMsg)
            }

            val body = response.body()
            if (body == null) {
                return Resource.Error("Empty response from recommendation service.")
            }

            // Edge Function returns an error field for validation/scope errors
            if (body.error != null) {
                return Resource.Error(body.error)
            }

            val recDtos = body.recommendations
            val recommendations = recDtos.map { it.toDomain() }

            // Write live result back to recommendation_cache so subsequent loads
            // are instant (cache hit). Fire-and-forget — don't block the UI.
            // Mirrors webapp: use-cached-recommendations.ts lines 127-143.
            if (recDtos.isNotEmpty() && dateTimestamp != null && targetMeal != null) {
                writeBackScope.launch {
                    writeBackToCache(targetMeal, dateTimestamp, recDtos, macrosDto)
                }
            }

            Resource.Success(recommendations)
        } catch (e: Exception) {
            Resource.Error(
                message = "Couldn't load recommendations. Check your connection and try again.",
                throwable = e
            )
        }
    }

    // ── Cache write-back (fire-and-forget) ───────────────────────────────

    /**
     * Writes live Edge Function results into `recommendation_cache` via PostgREST upsert.
     * Best-effort — errors are silently swallowed.
     */
    private suspend fun writeBackToCache(
        mealType: String,
        dateTimestamp: Long,
        recommendations: List<RecommendationDto>,
        remainingMacros: RemainingMacrosDto
    ) {
        try {
            val userId = authPreferences.getSession()?.userId ?: return
            dbApiService.upsertRecommendationCache(
                body = listOf(
                    RecommendationCacheUpsertDto(
                        userId = userId,
                        mealType = mealType,
                        dateTimestamp = dateTimestamp,
                        recommendations = recommendations,
                        remainingMacros = remainingMacros
                    )
                )
            )
        } catch (_: Exception) {
            // Cache write is best-effort — never block or fail the primary flow.
        }
    }

    // ── Prefetch trigger (fire-and-forget) ───────────────────────────────

    override suspend fun triggerPrefetch(dateTimestamp: Long, currentHour: Int) {
        try {
            edgeFunctionService.triggerPrefetch(
                PrefetchRequest(
                    dateTimestamp = dateTimestamp,
                    currentHour = currentHour
                )
            )
        } catch (_: Exception) {
            // Prefetch is best-effort — silently swallow errors.
        }
    }
}

// ─── DTO → Domain mapping ────────────────────────────────────────────────────

private fun RecommendationDto.toDomain() = Recommendation(
    name = name,
    description = description,
    reason = reason,
    suggestedQuantity = suggestedQuantity,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    source = RecommendationSource.fromString(source),
    foodItemId = foodItemId,
    recipeText = recipeText,
    searchQuery = searchQuery,
    cuisineTag = cuisineTag
)
