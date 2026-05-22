package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.repository.FoodRepository
import javax.inject.Inject

/**
 * Soft-deletes a food item from the catalog.
 *
 * Validates the food ID before delegating to [FoodRepository.softDeleteFood].
 */
class DeleteFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(foodId: String) {
        require(foodId.isNotBlank()) { "Food ID cannot be blank" }
        foodRepository.softDeleteFood(foodId)
    }
}
