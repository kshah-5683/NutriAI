package com.app.nutriai.data.remote.dto

import com.app.nutriai.domain.model.NutritionInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the USDA FoodData Central /foods/search endpoint.
 *
 * Nutrient ID reference (USDA standard nutrient numbers):
 * - 1008 = Energy (kcal)
 * - 1003 = Protein (g)
 * - 1005 = Carbohydrate, by difference (g)
 * - 1004 = Total lipid (fat) (g)
 * - 1079 = Fiber, total dietary (g)
 * - 2000 = Sugars, total (g)
 *
 * All values are per 100g unless unitName indicates otherwise.
 */
@Serializable
data class FdcSearchResponse(
    val totalHits: Int? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val foods: List<FdcFood> = emptyList()
)

@Serializable
data class FdcFood(
    val fdcId: Int? = null,
    val description: String? = null,
    val dataType: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val foodNutrients: List<FdcFoodNutrient> = emptyList(),
    /** Weight of a single described serving (e.g. 50 for "1 large egg = 50g"). */
    val servingSize: Double? = null,
    /** Unit for [servingSize] — usually "g" or "ml" for FDC entries. */
    @SerialName("servingSizeUnit")
    val servingSizeUnit: String? = null
) {
    /**
     * Resolves the best available brand string.
     * brandOwner is the manufacturer name; brandName is the product line name.
     * For Foundation/SR Legacy foods these are typically null (generic foods).
     */
    val resolvedBrand: String?
        get() = (brandName?.trim()?.ifBlank { null } ?: brandOwner?.trim()?.ifBlank { null })

    /** Returns the kcal per 100g value, null if not reported. */
    val caloriesPer100g: Double?
        get() = nutrientValue(NUTRIENT_ENERGY_KCAL)

    val proteinPer100g: Double?
        get() = nutrientValue(NUTRIENT_PROTEIN)

    val carbsPer100g: Double?
        get() = nutrientValue(NUTRIENT_CARBS)

    val fatPer100g: Double?
        get() = nutrientValue(NUTRIENT_FAT)

    val fiberPer100g: Double?
        get() = nutrientValue(NUTRIENT_FIBER)

    /** True if at least calories are present (minimum viable data). */
    val hasUsableData: Boolean
        get() = caloriesPer100g != null

    private fun nutrientValue(nutrientId: Int): Double? =
        foodNutrients.firstOrNull { it.nutrientId == nutrientId }?.value

    companion object {
        private const val NUTRIENT_ENERGY_KCAL = 1008
        private const val NUTRIENT_PROTEIN = 1003
        private const val NUTRIENT_CARBS = 1005
        private const val NUTRIENT_FAT = 1004
        private const val NUTRIENT_FIBER = 1079
    }
}

@Serializable
data class FdcFoodNutrient(
    val nutrientId: Int? = null,
    val nutrientName: String? = null,
    val value: Double? = null,
    val unitName: String? = null
)

// ─── DTO → Domain mapper ────────────────────────────────────────────────────

fun FdcFood.toNutritionInfo(): NutritionInfo? {
    val kcal = caloriesPer100g ?: return null  // Must have calories
    // Extract gram-based serving weight for discrete units (piece, slice, bowl).
    // Only used when servingSizeUnit is "g"; volumetric units (ml, oz) are ignored.
    val servingWeightG: Double? = if (
        servingSize != null &&
        servingSize > 0 &&
        servingSizeUnit?.trim()?.lowercase() == "g"
    ) servingSize else null

    return NutritionInfo(
        productName = description?.trim()?.ifBlank { null } ?: "Unknown",
        brand = resolvedBrand,
        caloriesPer100g = kcal.coerceAtLeast(0.0),
        proteinPer100g = (proteinPer100g ?: 0.0).coerceAtLeast(0.0),
        carbsPer100g = (carbsPer100g ?: 0.0).coerceAtLeast(0.0),
        fatPer100g = (fatPer100g ?: 0.0).coerceAtLeast(0.0),
        fiberPer100g = fiberPer100g?.coerceAtLeast(0.0),
        source = "USDA FoodData Central",
        externalId = fdcId?.toString(),
        servingWeightG = servingWeightG
    )
}
