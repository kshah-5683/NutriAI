package com.app.nutriai.domain.usecase

import android.util.Log
import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.domain.repository.NutritionRepository
import com.app.nutriai.util.Resource
import javax.inject.Inject

/**
 * Use case for looking up nutritional data for a food name via USDA FDC.
 *
 * Phase 5: Called after Gemma 4 extracts food entities from user input.
 * The result is used to auto-fill the calorie and macro fields in the Log screen,
 * reducing the need for manual data entry.
 *
 * Picking strategy:
 * - Returns the best-ranked match from [NutritionRepository.searchNutrition]
 *   (ranked by completeness of macro data in the repository).
 * - Returns null ([Resource.Success] with null) when no match is found.
 *
 * Graceful degradation:
 * - Network errors (blocked domain, timeout, SSL, no internet) → [Resource.Success] with null.
 *   The nutrition lookup is an enhancement, not a critical feature. When the USDA FDC
 *   domain is unreachable (e.g., blocked by a corporate web filter), the app shows
 *   "No nutrition data found — fill in manually" instead of a disruptive error.
 * - Only HTTP 429 (rate-limit) is surfaced as [Resource.Error] since it's actionable
 *   (the user should wait and retry).
 * - All failures are still logged to logcat via [NutritionRepository] for debugging.
 *
 * @param foodName The AI-extracted or user-typed food name
 * @return [Resource.Success] with the best [NutritionInfo] match, or null if not found/failed;
 *         [Resource.Error] only for HTTP 429 rate-limit
 */
class LookupNutritionUseCase @Inject constructor(
    private val nutritionRepository: NutritionRepository
) {
    suspend operator fun invoke(foodName: String): Resource<NutritionInfo?> {
        if (foodName.isBlank()) {
            return Resource.Success(null)
        }

        return when (val result = nutritionRepository.searchNutrition(foodName.trim())) {
            is Resource.Success -> Resource.Success(result.data.firstOrNull())

            is Resource.Error -> {
                // Surface rate-limit errors — actionable (user should wait and retry).
                // All other network/domain failures degrade gracefully to NotFound so
                // the UI shows a neutral "fill in manually" prompt rather than an error
                // badge. This handles blocked domains, proxies, SSL errors, timeouts, etc.
                if (result.message.contains("rate-limited", ignoreCase = true)) {
                    Resource.Error(result.message, result.throwable)
                } else {
                    Log.w(TAG, "Nutrition lookup for \"$foodName\" unavailable: ${result.message}")
                    Resource.Success(null) // Degrade to NotFound — non-disruptive
                }
            }

            is Resource.Loading -> Resource.Success(null)
        }
    }

    companion object {
        private const val TAG = "LookupNutrition"
    }
}
