package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.repository.FoodRepository
import javax.inject.Inject

/**
 * Validates and updates an existing [FoodItem] in the catalog.
 *
 * Enforces business rules (non-blank name, positive serving size) that were
 * previously checked only in the ViewModel's [saveEditedFood] method.
 */
class UpdateFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(food: FoodItem) {
        require(food.name.isNotBlank()) { "Food name cannot be blank" }
        require(food.baseServingG > 0) { "Serving size must be positive" }
        foodRepository.updateFood(food)
    }
}
