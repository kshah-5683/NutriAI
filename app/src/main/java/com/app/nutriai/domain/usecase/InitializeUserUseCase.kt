package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.Catalog
import com.app.nutriai.domain.model.User
import com.app.nutriai.domain.repository.CatalogRepository
import com.app.nutriai.domain.repository.UserRepository
import com.app.nutriai.util.Constants
import javax.inject.Inject

/**
 * Ensures a local user and both catalogs (Recipes + Ingredients) exist in Room on first launch.
 * Idempotent — safe to call on every app start.
 */
class InitializeUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke() {
        val now = System.currentTimeMillis()

        // Ensure the local user exists
        val existingUser = userRepository.getUserById(Constants.LOCAL_USER_ID)
        if (existingUser == null) {
            userRepository.insertUser(
                User(
                    id = Constants.LOCAL_USER_ID,
                    email = "local@nutriai.app",
                    createdAt = now
                )
            )
        }

        // Ensure the Recipe catalog exists
        if (catalogRepository.getCatalogById(Constants.RECIPE_CATALOG_ID) == null) {
            catalogRepository.insertCatalog(
                Catalog(
                    id = Constants.RECIPE_CATALOG_ID,
                    userId = Constants.LOCAL_USER_ID,
                    name = Constants.RECIPE_CATALOG_NAME,
                    lastModifiedAt = now
                )
            )
        }

        // Ensure the Ingredient catalog exists
        if (catalogRepository.getCatalogById(Constants.INGREDIENT_CATALOG_ID) == null) {
            catalogRepository.insertCatalog(
                Catalog(
                    id = Constants.INGREDIENT_CATALOG_ID,
                    userId = Constants.LOCAL_USER_ID,
                    name = Constants.INGREDIENT_CATALOG_NAME,
                    lastModifiedAt = now
                )
            )
        }
    }
}
