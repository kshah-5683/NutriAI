package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fetches food items for a specific catalog.
 *
 * Used by [CatalogViewModel] to load items for each tab:
 * - Recipes tab: `invoke(Constants.RECIPE_CATALOG_ID)`
 * - Ingredients tab: `invoke(Constants.INGREDIENT_CATALOG_ID)`
 */
class GetCatalogsUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    /**
     * Returns a reactive Flow of food items for the given catalog.
     *
     * @param catalogId The catalog to fetch items from
     * @return Flow emitting food items on any change
     */
    operator fun invoke(catalogId: String): Flow<List<FoodItem>> {
        return foodRepository.getFoodsByCatalogId(catalogId)
    }
}
