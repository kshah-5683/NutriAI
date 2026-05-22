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
 */
data class ExtractedLabelData(
    val caloriesPerServing: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    /** Raw serving size text from the label, e.g., "1 cup (240ml)" or "30g". Null if not found. */
    val servingSizeText: String?,
    /** Serving weight in grams if explicitly printed on the label. Null if not found. */
    val servingWeightG: Double?
)
