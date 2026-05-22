package com.app.nutriai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response DTO for the Gemini `generateContent` endpoint.
 *
 * The actual text content is nested at:
 *   candidates[0].content.parts[0].text
 *
 * For food parsing, this text will be a JSON string matching
 * the schema defined in our prompt (see [com.app.nutriai.util.GeminiPrompts]).
 */
@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiCandidateContent? = null,
    val finishReason: String? = null,
    val safetyRatings: List<GeminiSafetyRating>? = null
)

@Serializable
data class GeminiCandidateContent(
    val parts: List<GeminiResponsePart>? = null,
    val role: String? = null
)

/**
 * A single part in the response content.
 *
 * When thinking mode is enabled (even MINIMAL), Gemma 4 returns multiple parts:
 * - Parts with `thought = true` contain internal reasoning (may be empty)
 * - Parts with `thought = false` (or null) contain the actual answer
 *
 * We must filter by [thought] to extract the food JSON from the correct part.
 */
@Serializable
data class GeminiResponsePart(
    val text: String? = null,
    val thought: Boolean? = null
)

@Serializable
data class GeminiPromptFeedback(
    val safetyRatings: List<GeminiSafetyRating>? = null,
    val blockReason: String? = null
)

@Serializable
data class GeminiSafetyRating(
    val category: String? = null,
    val probability: String? = null
)

/**
 * Parsed food item returned within the Gemini JSON response body.
 * This is the schema we instruct Gemini to output.
 *
 * Note: No macro/calorie fields here — those come from USDA FDC lookup in Phase 5.
 * Gemini only extracts entity information (name, quantity, unit).
 *
 * Phase 4.5: Added [isRecipe] and [ingredients] for recipe-aware parsing.
 * When [isRecipe] is true, [ingredients] contains the component foods.
 * Backward compatible — non-recipe items have `is_recipe = false` and empty `ingredients`.
 */
@Serializable
data class GeminiParsedFoodDto(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "serving",
    val confidence: Double = 0.0,
    @SerialName("is_recipe")
    val isRecipe: Boolean = false,
    val ingredients: List<GeminiParsedFoodDto> = emptyList()
)

/**
 * Wrapper for the array of parsed foods in Gemini's JSON response.
 */
@Serializable
data class GeminiParsedFoodsWrapper(
    val foods: List<GeminiParsedFoodDto>
)
