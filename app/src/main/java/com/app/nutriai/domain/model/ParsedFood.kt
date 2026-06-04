package com.app.nutriai.domain.model

/**
 * Domain model representing a food item parsed from natural language by the AI.
 *
 * Contains entity extraction results only — food name, quantity, and unit.
 * Does NOT contain calorie/macro data; those come from nutrition database lookup (Phase 5).
 *
 * Phase 4.5 adds recipe support:
 * - When [isRecipe] is true, [name] is the recipe name (e.g., "Besan Chila")
 *   and [ingredients] contains the component foods.
 * - When [isRecipe] is false (default), this is a flat food item (backward compatible).
 *
 * @property name The extracted food name (e.g., "whole wheat toast") or recipe name
 * @property quantity The extracted quantity (e.g., 2.0) — for recipes, this is the serving count
 * @property unit The extracted unit (e.g., "slice", "bowl", "serving")
 * @property confidence AI confidence score (0.0–1.0) for the extraction
 * @property isRecipe Whether this is a recipe with nested ingredients
 * @property ingredients Component foods when [isRecipe] is true; empty for flat items
 * @property needsClarification True when the food has a variable serving size (e.g. bread slices,
 *   cheese slices) and the user did not specify a brand, weight, or size qualifier. The UI should
 *   prompt the user for more detail before proceeding with nutrition lookup.
 * @property clarificationHint A short user-facing prompt explaining why clarification is needed
 *   (e.g. "Bread slice sizes vary widely (20–60g). Can you specify the brand or weight?").
 *   Null when [needsClarification] is false.
 */
data class ParsedFood(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "serving",
    val confidence: Double = 0.0,
    val isRecipe: Boolean = false,
    val ingredients: List<ParsedFood> = emptyList(),
    val needsClarification: Boolean = false,
    val clarificationHint: String? = null,
    /**
     * Pre-resolved catalog match from the Edge Function.
     *
     * When non-null, the Edge Function already resolved this food against the user's
     * Supabase catalog — [ResolveCatalogCacheUseCase] can skip local Room lookup for
     * this item. Null means the item needs local resolution (fallback for unsynced data).
     */
    val edgeCatalogMatch: EdgeCatalogMatch? = null
)

/**
 * Lightweight catalog match data embedded in [ParsedFood] by the Edge Function.
 *
 * Separate from [CatalogMatch] which wraps the full parsedFood + matchedFoodItem
 * pair for the ViewModel layer. This only carries the server-side resolution result.
 *
 * @property isFromCatalog True when the Edge Function found this food in the user's catalog
 * @property foodItem The matched food_items row with macros; null if not found
 */
data class EdgeCatalogMatch(
    val isFromCatalog: Boolean,
    val foodItem: FoodItem? = null
)
