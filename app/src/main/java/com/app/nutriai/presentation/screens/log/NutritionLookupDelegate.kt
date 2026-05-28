package com.app.nutriai.presentation.screens.log

import com.app.nutriai.domain.model.CatalogMatch
import com.app.nutriai.domain.model.IngredientKey
import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.domain.usecase.LookupNutritionUseCase
import com.app.nutriai.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles nutrition database lookups (USDA FDC / IFCT 2017) for parsed food items.
 *
 * Extracted from [LogViewModel] to isolate the parallel lookup orchestration:
 * - Flat items not in catalog → lookup by name
 * - Recipe ingredients not in catalog → lookup each individually
 * - Items already in catalog → skip (macros already known)
 *
 * The delegate does NOT own the [MutableStateFlow] — it receives a reference
 * from the ViewModel and updates it in-place.
 */
class NutritionLookupDelegate(
    private val lookupNutritionUseCase: LookupNutritionUseCase,
    private val uiState: MutableStateFlow<LogUiState>,
    private val coroutineScope: CoroutineScope
) {

    /**
     * Looks up nutrition for all parsed foods in parallel.
     *
     * Strategy:
     * - Flat items in catalog → skip (macros already known from cache)
     * - Flat items NOT in catalog → lookup by AI-extracted name
     * - Recipes → lookup each ingredient individually
     *
     * All lookups run concurrently via [async]/[awaitAll] — typically 200–800ms total.
     */
    suspend fun lookupNutrition(
        parsedFoods: List<ParsedFood>,
        catalogMatches: List<CatalogMatch>,
        ingredientCatalogMatches: Map<Int, List<CatalogMatch>>
    ) {
        // Mark all non-catalog items as loading
        val initialLookups = mutableMapOf<Int, NutritionLookupState>()
        val initialIngredientLookups = mutableMapOf<IngredientKey, NutritionLookupState>()

        parsedFoods.forEachIndexed { index, food ->
            val catalogMatch = catalogMatches.getOrNull(index)
            if (food.isRecipe) {
                food.ingredients.forEachIndexed { ingIndex, _ ->
                    val ingMatch = ingredientCatalogMatches[index]?.getOrNull(ingIndex)
                    if (ingMatch?.isFromCatalog != true) {
                        initialIngredientLookups[IngredientKey(index, ingIndex)] = NutritionLookupState.Loading
                    }
                }
            } else {
                // Phase 17: Skip items needing clarification — they'll be looked up
                // after the user resolves the ambiguity via resolveClarification*()
                if (catalogMatch?.isFromCatalog != true && !food.needsClarification) {
                    initialLookups[index] = NutritionLookupState.Loading
                }
            }
        }

        uiState.update {
            it.copy(
                nutritionLookups = initialLookups.toMap(),
                ingredientNutritionLookups = initialIngredientLookups.toMap()
            )
        }

        // No items need lookup (all from catalog)
        if (initialLookups.isEmpty() && initialIngredientLookups.isEmpty()) return

        coroutineScope.launch {
            val flatFoodJobs = parsedFoods.mapIndexedNotNull { index, food ->
                val catalogMatch = catalogMatches.getOrNull(index)
                if (!food.isRecipe && catalogMatch?.isFromCatalog != true && !food.needsClarification) {
                    async {
                        val state = performNutritionLookup(food.name)
                        uiState.update { current ->
                            current.copy(
                                nutritionLookups = current.nutritionLookups + (index to state)
                            )
                        }
                    }
                } else null
            }

            val ingredientJobs = parsedFoods.mapIndexedNotNull { recipeIndex, food ->
                if (food.isRecipe) {
                    food.ingredients.mapIndexedNotNull { ingIndex, ingredient ->
                        val ingMatch = ingredientCatalogMatches[recipeIndex]?.getOrNull(ingIndex)
                        if (ingMatch?.isFromCatalog != true) {
                            async {
                                val state = performNutritionLookup(ingredient.name)
                                val key = IngredientKey(recipeIndex, ingIndex)
                                uiState.update { current ->
                                    current.copy(
                                        ingredientNutritionLookups = current.ingredientNutritionLookups + (key to state)
                                    )
                                }
                            }
                        } else null
                    }
                } else null
            }.flatten()

            (flatFoodJobs + ingredientJobs).awaitAll()
        }
    }

    /**
     * Performs a single nutrition lookup for the given food name.
     * Returns the resulting [NutritionLookupState].
     */
    suspend fun performNutritionLookup(foodName: String, brand: String? = null): NutritionLookupState {
        return when (val result = lookupNutritionUseCase(foodName, brand = brand)) {
            is Resource.Success -> {
                if (result.data != null) {
                    NutritionLookupState.Found(result.data)
                } else {
                    NutritionLookupState.NotFound
                }
            }
            is Resource.Error -> NutritionLookupState.Error(
                result.message ?: "Nutrition lookup failed"
            )
            is Resource.Loading -> NutritionLookupState.NotFound
        }
    }

    /**
     * Phase 17: Performs a single nutrition lookup and updates the UI state.
     * Used by clarification actions (brand-aware re-lookup, generic resolve, weight override).
     *
     * @param index The parsed food index
     * @param foodName The food name to search
     * @param brand Optional brand name for brand-specific FDC lookup
     */
    suspend fun performAndUpdateLookup(index: Int, foodName: String, brand: String? = null) {
        uiState.update { current ->
            current.copy(nutritionLookups = current.nutritionLookups + (index to NutritionLookupState.Loading))
        }
        val state = performNutritionLookup(foodName, brand)
        uiState.update { current ->
            current.copy(nutritionLookups = current.nutritionLookups + (index to state))
        }
    }
}
