package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.CatalogMatch
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.domain.repository.FoodRepository
import com.app.nutriai.util.Constants
import javax.inject.Inject

/**
 * Resolves parsed food items against the local catalog cache.
 *
 * Phase 4.5: After AI parsing, each ingredient is checked against the catalog
 * (case-insensitive exact name match). If a match is found, the cached [FoodItem]
 * is attached so the UI can show "Found in catalog" and reuse macros.
 *
 * Checks BOTH catalogs:
 * - Recipes are resolved against [Constants.RECIPE_CATALOG_ID]
 * - Ingredients (flat items or recipe ingredients) are resolved against [Constants.INGREDIENT_CATALOG_ID]
 *
 * Usage from ViewModel:
 * ```
 * val matches = resolveCatalogCacheUseCase(parsedFoods)
 * // Each CatalogMatch tells you if the ingredient was found in the catalog
 * ```
 */
class ResolveCatalogCacheUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    /**
     * Check each parsed food against the appropriate local catalog.
     *
     * For recipe items: resolves the recipe name against the Recipes catalog,
     * and each ingredient against the Ingredients catalog.
     * For flat items: resolves the item against the Ingredients catalog.
     *
     * @param parsedFoods List of AI-extracted food items (may include recipes)
     * @return List of [CatalogMatch] with resolved catalog entries
     */
    suspend operator fun invoke(
        parsedFoods: List<ParsedFood>
    ): List<CatalogMatch> {
        return parsedFoods.map { parsed ->
            if (parsed.isRecipe) {
                // For recipes: resolve each ingredient against the Ingredients catalog
                val resolvedIngredients = parsed.ingredients.map { ingredient ->
                    val match = foodRepository.searchFoodByNameExact(
                        ingredient.name, Constants.INGREDIENT_CATALOG_ID
                    )
                    CatalogMatch(
                        parsedFood = ingredient,
                        matchedFoodItem = match
                    )
                }
                // Also check if the recipe itself exists in the Recipes catalog
                val recipeMatch = foodRepository.searchFoodByNameExact(
                    parsed.name, Constants.RECIPE_CATALOG_ID
                )
                CatalogMatch(
                    parsedFood = parsed.copy(
                        // Preserve recipe structure with ingredients
                        ingredients = resolvedIngredients.map { it.parsedFood }
                    ),
                    matchedFoodItem = recipeMatch
                )
            } else {
                // Flat item: resolve against the Ingredients catalog first,
                // then fall back to Recipes catalog — Gemini sometimes marks
                // a saved recipe as isRecipe=false, which would otherwise miss
                // the catalog hit and trigger a redundant USDA lookup.
                val ingredientMatch = foodRepository.searchFoodByNameExact(
                    parsed.name, Constants.INGREDIENT_CATALOG_ID
                )
                val match = ingredientMatch ?: foodRepository.searchFoodByNameExact(
                    parsed.name, Constants.RECIPE_CATALOG_ID
                )
                CatalogMatch(
                    parsedFood = parsed,
                    matchedFoodItem = match
                )
            }
        }
    }

    /**
     * Resolve a flat list of ingredients (not wrapped in recipes) against the Ingredients catalog.
     * Used when resolving ingredients within a recipe for detailed cache badges.
     */
    suspend fun resolveIngredients(
        ingredients: List<ParsedFood>
    ): List<CatalogMatch> {
        return ingredients.map { ingredient ->
            val match = foodRepository.searchFoodByNameExact(
                ingredient.name, Constants.INGREDIENT_CATALOG_ID
            )
            CatalogMatch(
                parsedFood = ingredient,
                matchedFoodItem = match
            )
        }
    }
}
