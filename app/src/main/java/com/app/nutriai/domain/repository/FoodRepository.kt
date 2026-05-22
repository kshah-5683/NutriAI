package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for food item catalog operations.
 * Implementations live in the data layer (data/repository/).
 */
interface FoodRepository {
    fun getFoodsByCatalogId(catalogId: String): Flow<List<FoodItem>>
    fun searchFoodsByName(query: String): Flow<List<FoodItem>>

    /**
     * LIKE-based partial name match scoped to a specific catalog.
     * Used by CatalogScreen search within a single tab (Recipes or Ingredients).
     */
    fun searchFoodsByNameInCatalog(query: String, catalogId: String): Flow<List<FoodItem>>

    suspend fun getFoodById(id: String): FoodItem?
    suspend fun insertFood(food: FoodItem)
    suspend fun updateFood(food: FoodItem)
    suspend fun softDeleteFood(id: String)

    /**
     * Phase 4.5: Case-insensitive exact name match within a catalog.
     * Used for catalog cache resolution — checks if an ingredient already exists.
     * @return The matching [FoodItem] or null if not found.
     */
    suspend fun searchFoodByNameExact(name: String, catalogId: String): FoodItem?

    /**
     * Returns all non-deleted food names for a given catalog.
     * Used to build the "existing ingredients / existing recipes" context
     * injected into the Gemma 4 prompt for name standardization.
     * Pass [Constants.INGREDIENT_CATALOG_ID] or [Constants.RECIPE_CATALOG_ID].
     */
    suspend fun getAllFoodNames(catalogId: String): List<String>
}
