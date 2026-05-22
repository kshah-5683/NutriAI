package com.app.nutriai.domain.model

/**
 * Result of resolving a [ParsedFood] against the local catalog cache.
 *
 * Phase 4.5: After AI parsing, each ingredient is checked against the catalog.
 * If a match is found (e.g., "besan" was logged before with macros), the cached
 * [FoodItem] is attached so the UI can show "Found in catalog" and reuse macros.
 *
 * @property parsedFood The AI-extracted food entity
 * @property matchedFoodItem The cached catalog entry if found; null if new ingredient
 * @property isFromCatalog True when macros come from the local catalog cache
 */
data class CatalogMatch(
    val parsedFood: ParsedFood,
    val matchedFoodItem: FoodItem? = null,
    val isFromCatalog: Boolean = matchedFoodItem != null
)
