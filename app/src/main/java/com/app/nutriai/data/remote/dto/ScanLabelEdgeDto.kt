package com.app.nutriai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request/response DTOs for the `scan-label` Supabase Edge Function.
 *
 * The Edge Function handles:
 * 1. Calling Gemma 4 Vision with shared prompts from `_shared/prompts.ts`
 * 2. Per-serving → per-100g conversion (server-side)
 * 3. Suggested quantity/unit for form pre-fill
 *
 * This replaces direct Gemini API calls for label scanning.
 * The per-100g conversion that was in [LabelScannerDelegate] is now server-side.
 */
@Serializable
data class ScanLabelRequest(
    val base64: String,
    val mimeType: String
)

/**
 * Response from the `scan-label` Edge Function.
 *
 * Contains BOTH raw per-serving values (for display) AND converted per-100g values
 * (for storage/form pre-fill). Also includes suggested quantity/unit hints.
 *
 * When [error] is non-null, the extraction failed and all numeric fields are 0.
 */
@Serializable
data class ScanLabelEdgeResponse(
    // Raw extraction (always returned for display)
    @SerialName("raw_calories_per_serving")
    val rawCaloriesPerServing: Double = 0.0,
    @SerialName("raw_protein_g")
    val rawProteinG: Double = 0.0,
    @SerialName("raw_carbs_g")
    val rawCarbsG: Double = 0.0,
    @SerialName("raw_fat_g")
    val rawFatG: Double = 0.0,
    @SerialName("serving_size_text")
    val servingSizeText: String? = null,
    @SerialName("serving_weight_g")
    val servingWeightG: Double? = null,

    // Converted per-100g values for form pre-fill (what log-food expects)
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,

    // Pre-fill hints for the manual form
    @SerialName("suggested_quantity")
    val suggestedQuantity: Double = 1.0,
    @SerialName("suggested_unit")
    val suggestedUnit: String = "serving",

    // Error message if extraction failed
    val error: String? = null
)
