package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.CatalogMatch
import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.repository.CatalogRepository
import com.app.nutriai.domain.repository.DailyLogRepository
import com.app.nutriai.domain.repository.FoodRepository
import com.app.nutriai.util.Constants
import java.util.UUID
import javax.inject.Inject

/**
 * Validates input, creates a [FoodItem] (if new) and a [DailyLog] entry,
 * and inserts both into their respective repositories.
 *
 * Phase 3: Manual entry with user-provided name and macros.
 * Phase 4: AI-parsed food items plug into [invoke].
 * Phase 4.5: [logRecipe] handles recipe logging with aggregated ingredient macros.
 * Phase 5: [invoke] accepts [externalApiId] to persist the nutrition database product ID.
 *          [logRecipe] handles nutrition-enriched ingredients (inserts them to DB).
 *
 * Catalog routing:
 * - Single food items (ingredients) → [Constants.INGREDIENT_CATALOG_ID]
 * - Recipes → [Constants.RECIPE_CATALOG_ID]
 * - New ingredients within recipes → [Constants.INGREDIENT_CATALOG_ID]
 */
class LogFoodUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val catalogRepository: CatalogRepository
) {
    /**
     * Log a single food item (ingredient) to the Ingredients catalog.
     *
     * @param foodName display name for the food (e.g., "Oatmeal with honey")
     * @param brand optional brand name (sourced from nutrition database in Phase 5)
     * @param servingG serving size in grams
     * @param calories total calories for the serving
     * @param protein grams of protein for the serving
     * @param carbs grams of carbohydrates for the serving
     * @param fat grams of fat for the serving
     * @param quantity number of servings consumed
     * @param unit unit label (e.g., "serving", "bowl", "piece")
     * @param dateTimestamp epoch millis for the log date
     * @param catalogId target catalog ID — defaults to [Constants.INGREDIENT_CATALOG_ID]
     * @param skipDailyLog when true, only creates the [FoodItem] without a [DailyLog] entry.
     *        Used when adding items directly to a catalog (from Catalog screen FAB)
     *        without affecting daily macro totals.
     * @param externalApiId Phase 5: USDA FDC ID to store in [FoodItem.externalApiId]
     *        for future cache re-lookups. Pass null when not sourced from a nutrition database.
     * @param existingFoodItemId When non-null, reuses an existing [FoodItem] from the catalog
     *        instead of inserting a new one. The [DailyLog] will reference this ID directly.
     *        Used to prevent duplicate catalog entries when logging a food that is already
     *        in the catalog (catalog cache hit). Pass null to create a new [FoodItem] as before.
     * @throws IllegalArgumentException if name is blank or quantity is not positive
     */
    suspend operator fun invoke(
        foodName: String,
        brand: String? = null,
        servingG: Double,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        quantity: Double,
        unit: String,
        dateTimestamp: Long,
        catalogId: String = Constants.INGREDIENT_CATALOG_ID,
        skipDailyLog: Boolean = false,
        externalApiId: String? = null,
        existingFoodItemId: String? = null
    ) {
        require(foodName.isNotBlank()) { "Food name cannot be blank" }
        require(quantity > 0) { "Quantity must be greater than zero" }
        require(servingG > 0) { "Serving size must be greater than zero" }

        val now = System.currentTimeMillis()

        val foodItemId: String
        // Verify the existing food item still exists in the DB before reusing its ID.
        // A background sync may have purged it (tombstone cleanup) or cascade-deleted it
        // between when the catalog cache was resolved and when the user tapped "Log All".
        // If the item is gone, fall through to creating a new FoodItem so the DailyLog
        // insert never receives a dangling food_item_id FK reference.
        val verifiedExistingId = existingFoodItemId?.let { foodRepository.getFoodById(it)?.id }
        if (verifiedExistingId != null) {
            // Reuse the existing catalog item — do NOT insert a new FoodItem.
            // This prevents duplicate catalog entries when the food is already known.
            foodItemId = verifiedExistingId
        } else {
            // New item (or previously-existing item that was purged): create and insert.
            val resolvedCatalogId = resolveCatalogId(catalogId, now)
            foodItemId = UUID.randomUUID().toString()
            val foodItem = FoodItem(
                id = foodItemId,
                catalogId = resolvedCatalogId,
                name = foodName.trim(),
                brand = brand?.trim()?.ifBlank { null },
                baseServingG = servingG,
                baseCalories = calories,
                baseProtein = protein,
                baseCarbs = carbs,
                baseFat = fat,
                externalApiId = externalApiId,
                lastModifiedAt = now
            )
            foodRepository.insertFood(foodItem)
        }

        // Create the daily log with computed macro totals (skip when adding to catalog only)
        if (!skipDailyLog) {
            val scaleFactor = quantity // each "quantity" = 1 serving
            val dailyLog = DailyLog(
                id = UUID.randomUUID().toString(),
                userId = Constants.LOCAL_USER_ID,
                foodItemId = foodItemId,
                foodName = foodName.trim(),
                dateTimestamp = dateTimestamp,
                consumedQty = quantity,
                consumedUnit = unit.trim().ifBlank { "serving" },
                totalCalories = calories * scaleFactor,
                totalProtein = protein * scaleFactor,
                totalCarbs = carbs * scaleFactor,
                totalFat = fat * scaleFactor,
                isSynced = false,
                lastModifiedAt = now
            )
            dailyLogRepository.insertLog(dailyLog)
        }
    }

    /**
     * Phase 4.5: Log a recipe with its ingredients.
     *
     * Creates:
     * 1. A [FoodItem] for each NEW ingredient → Ingredients catalog
     *    - Phase 5: If the ingredient was enriched with USDA FDC nutrition data
     *      ([CatalogMatch.isFromCatalog] = false, [CatalogMatch.matchedFoodItem] != null),
     *      the enriched [FoodItem] (with real macros) is inserted to the catalog.
     *    - Otherwise a 0-macro placeholder is created.
     * 2. A [FoodItem] for the recipe with aggregated macros → Recipes catalog
     * 3. A [DailyLog] entry for the recipe (unless [skipDailyLog] is true)
     *
     * @param recipeName The recipe name (e.g., "Besan Chila")
     * @param ingredientMatches Resolved ingredient matches from catalog cache.
     *        For Phase 5: may include nutrition-enriched matches where
     *        [CatalogMatch.isFromCatalog] = false but [CatalogMatch.matchedFoodItem] != null.
     * @param quantity Number of recipe servings consumed
     * @param unit Unit label for the recipe (e.g., "serving")
     * @param dateTimestamp Epoch millis for the log date
     * @param skipDailyLog when true, only creates [FoodItem] entries without a [DailyLog].
     */
    suspend fun logRecipe(
        recipeName: String,
        ingredientMatches: List<CatalogMatch>,
        quantity: Double,
        unit: String,
        dateTimestamp: Long,
        skipDailyLog: Boolean = false
    ) {
        require(recipeName.isNotBlank()) { "Recipe name cannot be blank" }
        require(quantity > 0) { "Quantity must be greater than zero" }

        val now = System.currentTimeMillis()
        val ingredientCatalogId = resolveCatalogId(Constants.INGREDIENT_CATALOG_ID, now)
        val recipeCatalogId = resolveCatalogId(Constants.RECIPE_CATALOG_ID, now)

        // Aggregate macros from all ingredients
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalServingG = 0.0

        for (match in ingredientMatches) {
            val cachedFood = match.matchedFoodItem

            when {
                // Case 1: Catalog hit — item already in DB, just accumulate macros
                match.isFromCatalog && cachedFood != null -> {
                    totalCalories += cachedFood.baseCalories
                    totalProtein += cachedFood.baseProtein
                    totalCarbs += cachedFood.baseCarbs
                    totalFat += cachedFood.baseFat
                    totalServingG += cachedFood.baseServingG
                }

                // Case 2: Phase 5 — not in catalog, but enriched with USDA FDC nutrition.
                // Insert to catalog so it's available for future sessions.
                !match.isFromCatalog && cachedFood != null -> {
                    val ingredientToInsert = cachedFood.copy(
                        catalogId = ingredientCatalogId,
                        lastModifiedAt = now
                    )
                    foodRepository.insertFood(ingredientToInsert)
                    totalCalories += ingredientToInsert.baseCalories
                    totalProtein += ingredientToInsert.baseProtein
                    totalCarbs += ingredientToInsert.baseCarbs
                    totalFat += ingredientToInsert.baseFat
                    totalServingG += ingredientToInsert.baseServingG
                }

                // Case 3: No data at all — create 0-macro placeholder in the Ingredients catalog
                else -> {
                    val ingredientId = UUID.randomUUID().toString()
                    val ingredientItem = FoodItem(
                        id = ingredientId,
                        catalogId = ingredientCatalogId,
                        name = match.parsedFood.name.trim(),
                        baseServingG = Constants.PER_100G_BASE,
                        baseCalories = 0.0,
                        baseProtein = 0.0,
                        baseCarbs = 0.0,
                        baseFat = 0.0,
                        lastModifiedAt = now
                    )
                    foodRepository.insertFood(ingredientItem)
                    totalServingG += Constants.PER_100G_BASE
                }
            }
        }

        // Create the recipe FoodItem in the Recipes catalog with aggregated macros
        val recipeItemId = UUID.randomUUID().toString()
        val recipeFoodItem = FoodItem(
            id = recipeItemId,
            catalogId = recipeCatalogId,
            name = recipeName.trim(),
            baseServingG = if (totalServingG > 0) totalServingG else Constants.PER_100G_BASE,
            baseCalories = totalCalories,
            baseProtein = totalProtein,
            baseCarbs = totalCarbs,
            baseFat = totalFat,
            lastModifiedAt = now
        )
        foodRepository.insertFood(recipeFoodItem)

        // Create the daily log entry for the recipe (skip when adding to catalog only)
        if (!skipDailyLog) {
            val scaleFactor = quantity
            val dailyLog = DailyLog(
                id = UUID.randomUUID().toString(),
                userId = Constants.LOCAL_USER_ID,
                foodItemId = recipeItemId,
                foodName = recipeName.trim(),
                dateTimestamp = dateTimestamp,
                consumedQty = quantity,
                consumedUnit = unit.trim().ifBlank { "serving" },
                totalCalories = totalCalories * scaleFactor,
                totalProtein = totalProtein * scaleFactor,
                totalCarbs = totalCarbs * scaleFactor,
                totalFat = totalFat * scaleFactor,
                isSynced = false,
                lastModifiedAt = now
            )
            dailyLogRepository.insertLog(dailyLog)
        }
    }

    /**
     * Resolves a catalog ID, creating the catalog on-the-fly if it doesn't exist.
     * Handles both recipe and ingredient catalogs.
     */
    private suspend fun resolveCatalogId(catalogId: String, now: Long): String {
        val existing = catalogRepository.getCatalogById(catalogId)
        if (existing != null) return existing.id

        val name = when (catalogId) {
            Constants.RECIPE_CATALOG_ID -> Constants.RECIPE_CATALOG_NAME
            Constants.INGREDIENT_CATALOG_ID -> Constants.INGREDIENT_CATALOG_NAME
            else -> "Catalog"
        }
        catalogRepository.insertCatalog(
            com.app.nutriai.domain.model.Catalog(
                id = catalogId,
                userId = Constants.LOCAL_USER_ID,
                name = name,
                lastModifiedAt = now
            )
        )
        return catalogId
    }
}
