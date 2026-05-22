package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.util.Resource

/**
 * Repository interface for nutrition data lookup operations.
 *
 * Phase 5: Backs AI-parsed food entities with verified nutritional data from
 * USDA FoodData Central. The data layer implementation is responsible for all
 * network calls, DTO mapping, and error handling.
 */
interface NutritionRepository {

    /**
     * Search for nutrition data by food name.
     *
     * Returns a ranked list of [NutritionInfo] matches (best match first).
     * The caller ([LookupNutritionUseCase]) picks the most relevant result.
     *
     * @param foodName The food name to search for (AI-extracted or user-typed)
     * @return [Resource.Success] with a list of matches (possibly empty),
     *         or [Resource.Error] on network/parsing failure
     */
    suspend fun searchNutrition(foodName: String): Resource<List<NutritionInfo>>
}
