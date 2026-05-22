package com.app.nutriai.util

/**
 * Converts food quantity + unit into a multiplier relative to [Constants.PER_100G_BASE].
 * Used to scale per-100g nutrition values to the actual serving amount.
 *
 * | Unit              | Assumed weight        | Multiplier formula                        |
 * |-------------------|-----------------------|-------------------------------------------|
 * | g / gram(s)       | 1g per unit           | quantity / 100                            |
 * | ml / milliliter(s)| ~1g per ml            | quantity / 100                            |
 * | tsp               | ~5g per tsp           | (quantity * 5) / 100                      |
 * | tbsp              | ~15g per tbsp         | (quantity * 15) / 100                     |
 * | cup(s)            | ~240g per cup         | (quantity * 240) / 100                    |
 * | piece/slice/bowl  | servingWeightG if set | quantity * servingWeightG / 100           |
 * |                   | 100g fallback         | quantity × 1.0                            |
 * | serving           | 100g assumed          | quantity × 1.0                            |
 */
object UnitConverter {

    private val BASE = Constants.PER_100G_BASE

    /**
     * @param quantity User-entered quantity value.
     * @param unit Unit string (case-insensitive).
     * @param servingWeightG Optional gram-weight of one discrete unit (piece/slice/bowl).
     *   When provided for discrete units, the multiplier is `quantity * servingWeightG / 100`
     *   instead of the default `quantity × 1.0` (which assumes 100g per unit).
     *   Sourced from [com.app.nutriai.domain.model.NutritionInfo.servingWeightG].
     */
    fun computeServingMultiplier(
        quantity: Double,
        unit: String,
        servingWeightG: Double? = null
    ): Double {
        val u = unit.trim().lowercase()
        return when {
            u in listOf("g", "gram", "grams") -> quantity / BASE
            u in listOf("ml", "milliliter", "millilitre", "milliliters", "millilitres") -> quantity / BASE
            u in listOf("tsp", "teaspoon", "teaspoons") -> (quantity * 5.0) / BASE
            u in listOf("tbsp", "tablespoon", "tablespoons") -> (quantity * 15.0) / BASE
            u in listOf("cup", "cups") -> (quantity * 240.0) / BASE
            // Discrete units — use actual gram weight when available, else 100g per unit
            u in listOf("piece", "pieces", "slice", "slices", "bowl", "bowls") ->
                if (servingWeightG != null && servingWeightG > 0)
                    (quantity * servingWeightG) / BASE
                else
                    quantity
            u == "serving" -> quantity
            else -> quantity // Unknown unit: treat as serving-based
        }
    }

    /** Returns true if the unit represents grams. */
    fun isGramsUnit(unit: String): Boolean =
        unit.trim().lowercase() in listOf("g", "gram", "grams")

    /**
     * Converts a stored [consumedQty] (multiplier) back to the human-readable display quantity.
     *
     * For gram units: `consumedQty × 100` (e.g. 2.0 → 200 g).
     * For all other units: unchanged (e.g. 2.0 tbsp → 2.0 tbsp).
     *
     * Result is rounded to 2 decimal places to eliminate IEEE 754 double-precision
     * artifacts from the × 100 multiplication.
     * e.g. stored 2.5499999... × 100 = 254.9999997 → displayed as 255.0 after rounding.
     *
     * Use this at every display / edit-pre-fill site so users always see
     * their original entered quantity rather than the internal multiplier.
     */
    fun toDisplayQty(consumedQty: Double, unit: String): Double {
        val raw = if (isGramsUnit(unit)) consumedQty * BASE else consumedQty
        return Math.round(raw * 100.0) / 100.0
    }

    /**
     * Converts a display / user-entered quantity back to the stored multiplier form.
     *
     * For gram units: `displayQty ÷ 100` (e.g. 200 g → 2.0).
     * For all other units: unchanged.
     *
     * Use this in every save path before writing [consumedQty] to the database.
     */
    fun fromDisplayQty(displayQty: Double, unit: String): Double =
        if (isGramsUnit(unit)) displayQty / BASE else displayQty
}
