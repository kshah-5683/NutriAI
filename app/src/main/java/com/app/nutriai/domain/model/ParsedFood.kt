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
 */
data class ParsedFood(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "serving",
    val confidence: Double = 0.0,
    val isRecipe: Boolean = false,
    val ingredients: List<ParsedFood> = emptyList()
)
