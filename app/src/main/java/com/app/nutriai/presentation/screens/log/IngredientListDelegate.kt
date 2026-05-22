package com.app.nutriai.presentation.screens.log

import com.app.nutriai.domain.model.IngredientKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles ingredient list mutations within parsed recipe cards.
 *
 * Extracted from [LogViewModel] to isolate the complex index-management logic
 * for removing, reordering, and swapping ingredients while keeping three parallel
 * data structures ([parsedFoods], [ingredientCatalogMatches],
 * [ingredientNutritionLookups]) in sync.
 *
 * The delegate does NOT own the [MutableStateFlow] — it receives a reference
 * from the ViewModel and updates it in-place.
 */
class IngredientListDelegate(
    private val uiState: MutableStateFlow<LogUiState>
) {

    /**
     * Remove a single ingredient from a parsed recipe card.
     *
     * Keeps [LogUiState.ingredientCatalogMatches] and [LogUiState.ingredientNutritionLookups]
     * in sync by removing the corresponding entries and re-keying indices above the removed one.
     * Also adjusts [LogUiState.selectedIngredientIndex] if it pointed at or past the removed item.
     */
    fun removeIngredient(foodIndex: Int, ingredientIndex: Int) {
        uiState.update { state ->
            val foods = state.parsedFoods.toMutableList()
            val food = foods.getOrNull(foodIndex) ?: return@update state
            if (ingredientIndex !in food.ingredients.indices) return@update state

            // Remove from ingredients list
            val newIngredients = food.ingredients.toMutableList().also { it.removeAt(ingredientIndex) }
            foods[foodIndex] = food.copy(ingredients = newIngredients)

            // Remove from catalog matches
            val newCatalogMatches = state.ingredientCatalogMatches.toMutableMap()
            val matches = newCatalogMatches[foodIndex]?.toMutableList()
            if (matches != null && ingredientIndex in matches.indices) {
                matches.removeAt(ingredientIndex)
                newCatalogMatches[foodIndex] = matches
            }

            // Re-key nutrition lookups: drop removed entry, shift indices above it down by 1
            val newLookups = mutableMapOf<IngredientKey, NutritionLookupState>()
            state.ingredientNutritionLookups.forEach { (key, value) ->
                when {
                    key.foodIndex != foodIndex -> newLookups[key] = value
                    key.ingredientIndex < ingredientIndex -> newLookups[key] = value
                    key.ingredientIndex == ingredientIndex -> { /* drop */ }
                    else -> newLookups[key.copy(ingredientIndex = key.ingredientIndex - 1)] = value
                }
            }

            // Adjust selectedIngredientIndex
            val newSelectedIngIdx = if (state.selectedParsedFoodIndex == foodIndex) {
                state.selectedIngredientIndex?.let { sel ->
                    when {
                        sel < ingredientIndex -> sel
                        sel == ingredientIndex -> null
                        else -> sel - 1
                    }
                }
            } else {
                state.selectedIngredientIndex
            }

            state.copy(
                parsedFoods = foods,
                ingredientCatalogMatches = newCatalogMatches,
                ingredientNutritionLookups = newLookups,
                selectedIngredientIndex = newSelectedIngIdx
            )
        }
    }

    /**
     * Move an ingredient one position up (towards index 0) within its recipe card.
     * No-op if already the first ingredient.
     */
    fun moveIngredientUp(foodIndex: Int, ingredientIndex: Int) {
        if (ingredientIndex <= 0) return
        swapIngredients(foodIndex, ingredientIndex, ingredientIndex - 1)
    }

    /**
     * Move an ingredient one position down within its recipe card.
     * No-op if already the last ingredient.
     */
    fun moveIngredientDown(foodIndex: Int, ingredientIndex: Int) {
        val food = uiState.value.parsedFoods.getOrNull(foodIndex) ?: return
        if (ingredientIndex >= food.ingredients.lastIndex) return
        swapIngredients(foodIndex, ingredientIndex, ingredientIndex + 1)
    }

    /** Swaps two ingredients at [indexA] and [indexB] within the given recipe card. */
    private fun swapIngredients(foodIndex: Int, indexA: Int, indexB: Int) {
        uiState.update { state ->
            val foods = state.parsedFoods.toMutableList()
            val food = foods.getOrNull(foodIndex) ?: return@update state

            val newIngredients = food.ingredients.toMutableList()
            val temp = newIngredients[indexA]
            newIngredients[indexA] = newIngredients[indexB]
            newIngredients[indexB] = temp
            foods[foodIndex] = food.copy(ingredients = newIngredients)

            // Swap catalog matches
            val newCatalogMatches = state.ingredientCatalogMatches.toMutableMap()
            val matches = newCatalogMatches[foodIndex]?.toMutableList()
            if (matches != null && indexA in matches.indices && indexB in matches.indices) {
                val tempMatch = matches[indexA]
                matches[indexA] = matches[indexB]
                matches[indexB] = tempMatch
                newCatalogMatches[foodIndex] = matches
            }

            // Swap nutrition lookups
            val newLookups = state.ingredientNutritionLookups.toMutableMap()
            val keyA = IngredientKey(foodIndex, indexA)
            val keyB = IngredientKey(foodIndex, indexB)
            val valA = newLookups[keyA]
            val valB = newLookups[keyB]
            if (valA != null) newLookups[keyB] = valA else newLookups.remove(keyB)
            if (valB != null) newLookups[keyA] = valB else newLookups.remove(keyA)

            // Adjust selectedIngredientIndex to follow the moved item
            val newSelectedIngIdx = if (state.selectedParsedFoodIndex == foodIndex) {
                when (state.selectedIngredientIndex) {
                    indexA -> indexB
                    indexB -> indexA
                    else -> state.selectedIngredientIndex
                }
            } else state.selectedIngredientIndex

            state.copy(
                parsedFoods = foods,
                ingredientCatalogMatches = newCatalogMatches,
                ingredientNutritionLookups = newLookups,
                selectedIngredientIndex = newSelectedIngIdx
            )
        }
    }
}
