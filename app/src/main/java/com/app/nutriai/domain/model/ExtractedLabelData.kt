package com.app.nutriai.domain.model

/**
 * Domain model for nutrition data extracted from a food label photo.
 *
 * All macro values default to 0.0 when not found on the label, so the manual
 * form always has a valid (if zero) pre-fill. The user reviews and corrects values
 * before saving — accuracy is their responsibility, not the AI's.
 *
 * [servingSizeText] and [servingWeightG] are informational — they help the user
 * fill in the "serving size" field on the manual form but are not stored downstream.
 *
 * Phase 11: Nutrition Label Scanner.
 *
 * Edge Function migration: The `scan-label` Edge Function now returns BOTH raw
 * per-serving values AND converted per-100g values. The per-100g fields ([calories],
 * [protein], [carbs], [fat]) and form hints ([suggestedQuantity], [suggestedUnit])
 * are pre-computed server-side, eliminating the need for client-side conversion.
 */
data class ExtractedLabelData(
    // Raw per-serving values (from AI extraction)
    val caloriesPerServing: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    /** Raw serving size text from the label, e.g., "1 cup (240ml)" or "30g". Null if not found. */
    val servingSizeText: String?,
    /** Serving weight in grams if explicitly printed on the label. Null if not found. */
    val servingWeightG: Double?,

    // Converted per-100g values (pre-computed by Edge Function)
    /** Calories per 100g — pre-computed by Edge Function. Falls back to [caloriesPerServing] when no serving weight. */
    val calories: Double = caloriesPerServing,
    /** Protein per 100g — pre-computed by Edge Function. */
    val protein: Double = proteinG,
    /** Carbs per 100g — pre-computed by Edge Function. */
    val carbs: Double = carbsG,
    /** Fat per 100g — pre-computed by Edge Function. */
    val fat: Double = fatG,

    // Form pre-fill hints (from Edge Function)
    /** Suggested quantity for form pre-fill (serving weight in grams, or 1 if no weight). */
    val suggestedQuantity: Double = 1.0,
    /** Suggested unit for form pre-fill ("grams" when weight available, "serving" otherwise). */
    val suggestedUnit: String = "serving"
)
