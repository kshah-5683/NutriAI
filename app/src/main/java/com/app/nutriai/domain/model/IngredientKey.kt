package com.app.nutriai.domain.model

/**
 * Type-safe composite key identifying a specific ingredient within a parsed recipe card.
 *
 * Replaces the previous string-based key pattern ("$foodIndex:$ingredientIndex") that
 * required manual string splitting and integer parsing in swap/remove operations.
 *
 * @param foodIndex       Index of the recipe in [LogUiState.parsedFoods].
 * @param ingredientIndex Index of the ingredient within the recipe's ingredients list.
 */
data class IngredientKey(val foodIndex: Int, val ingredientIndex: Int)
