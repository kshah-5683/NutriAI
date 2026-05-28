package com.app.nutriai.domain.model

/**
 * Domain model representing nutritional data for a food item, sourced from
 * an external nutrition database.
 *
 * All macro values are expressed **per 100g** of the product. The ViewModel
 * calculates actual intake amounts using:
 *   `actual = (per100g / 100) * gramsConsumed`
 *
 * Phase 5: Used to auto-fill calorie/macro fields after AI food parsing.
 * Phase 5.5: Source is now USDA FDC (online) or IFCT 2017 (offline fallback).
 *            The [externalId] stores either the FDC food ID or the IFCT food code.
 *
 * @property productName The matched product name from the nutrition database
 * @property brand Brand or manufacturer name (null for generic / IFCT items)
 * @property caloriesPer100g Calories (kcal) per 100g
 * @property proteinPer100g Protein (g) per 100g
 * @property carbsPer100g Total carbohydrates (g) per 100g
 * @property fatPer100g Total fat (g) per 100g
 * @property fiberPer100g Dietary fiber (g) per 100g; null if not reported
 * @property source Attribution string — "USDA FoodData Central" or "IFCT 2017 (Offline)"
 * @property externalId FDC food ID (integer string) or IFCT food code (e.g. "G001")
 * @property servingWeightG Weight in grams of a single discrete serving unit (piece, slice, bowl).
 *   Sourced from FDC `servingSize` when `servingSizeUnit` is "g". Used by [UnitConverter] to
 *   compute the correct multiplier for discrete units (e.g. 1 piece egg = 50g → 0.5 × per-100g).
 *   Null for volumetric units or when FDC does not report a gram-based serving size.
 * @property matchType Indicates the quality of the nutrition match:
 *   - "branded" — exact brand match from FDC Branded database (high confidence)
 *   - "generic" — generic/unbranded match from FDC Foundation/SR Legacy or IFCT (moderate confidence)
 *   - null — not yet determined or not applicable (e.g. catalog-sourced data)
 */
data class NutritionInfo(
    val productName: String,
    val brand: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    val source: String = "USDA FoodData Central",
    val externalId: String? = null,
    val servingWeightG: Double? = null,
    val matchType: String? = null
)
