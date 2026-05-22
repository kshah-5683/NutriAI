package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Wraps [FoodRepository.searchFoodsByNameInCatalog] with input sanitization.
 * Returns an empty Flow if the query is blank.
 * Search is scoped to a specific catalog (Recipes or Ingredients).
 */
class SearchCatalogUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    /**
     * @param query search text (partial name match)
     * @param catalogId catalog to search within
     */
    operator fun invoke(query: String, catalogId: String): Flow<List<FoodItem>> {
        val sanitized = query.trim()
        if (sanitized.isBlank()) {
            return flowOf(emptyList())
        }
        return foodRepository.searchFoodsByNameInCatalog(sanitized, catalogId)
    }
}
