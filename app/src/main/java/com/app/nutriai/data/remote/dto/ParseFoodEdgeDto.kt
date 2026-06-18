package com.app.nutriai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request/response DTOs for the `parse-food` Supabase Edge Function.
 *
 * The Edge Function handles:
 * 1. Fetching catalog names for name standardization (server-side)
 * 2. Calling Gemma 4 with shared prompts from `_shared/prompts.ts`
 * 3. Parsing JSON and resolving catalog cache matches (server-side)
 *
 * This replaces direct Gemini API calls via [GeminiApiService] + [GeminiRequest].
 * Prompts are now single-sourced in `supabase/functions/_shared/prompts.ts`.
 */
@Serializable
data class ParseFoodRequest(
    val foodDescription: String,
    val clarificationAnswers: Map<String, String>? = null
)

/**
 * Response from the `parse-food` Edge Function.
 *
 * On success: [foods] contains parsed items with pre-resolved catalog matches.
 * On error: [error] has a user-facing message; [foods] is empty.
 */
@Serializable
data class ParseFoodEdgeResponse(
    val foods: List<ParsedFoodDto> = emptyList(),
    val error: String? = null
)

/**
 * A food item parsed by the Edge Function.
 *
 * Includes [catalogMatch] pre-resolved server-side — the Edge Function queries
 * Supabase `food_items` by name, so Android no longer needs to do this locally
 * (though [ResolveCatalogCacheUseCase] remains as fallback for unsynced local data).
 */
@Serializable
data class ParsedFoodDto(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "serving",
    val confidence: Double = 0.0,
    @SerialName("isRecipe")
    val isRecipe: Boolean = false,
    val ingredients: List<ParsedFoodDto> = emptyList(),
    val catalogMatch: CatalogMatchDto? = null,
    @SerialName("needsClarification")
    val needsClarification: Boolean = false,
    @SerialName("clarificationHint")
    val clarificationHint: String? = null,
    @SerialName("clarifications")
    val clarifications: List<ClarificationDto>? = null
)

@Serializable
data class ClarificationDto(
    val id: String,
    val question: String,
    val options: List<String> = emptyList()
)

/**
 * Pre-resolved catalog match from the Edge Function.
 *
 * When [isFromCatalog] is true, [foodItem] contains the full food_items row
 * with macros — the UI can show "Found in catalog" and skip nutrition lookup.
 */
@Serializable
data class CatalogMatchDto(
    @SerialName("isFromCatalog")
    val isFromCatalog: Boolean,
    val foodItem: FoodItemDto? = null
)

/**
 * food_items row returned by the Edge Function's catalog resolution.
 * Maps the camelCase keys produced by the Edge Function's `mapFoodItem()`.
 */
@Serializable
data class FoodItemDto(
    val id: String,
    @SerialName("catalogId")
    val catalogId: String,
    val name: String,
    val brand: String? = null,
    @SerialName("baseServingG")
    val baseServingG: Double = 100.0,
    @SerialName("baseCalories")
    val baseCalories: Double = 0.0,
    @SerialName("baseProtein")
    val baseProtein: Double = 0.0,
    @SerialName("baseCarbs")
    val baseCarbs: Double = 0.0,
    @SerialName("baseFat")
    val baseFat: Double = 0.0,
    @SerialName("externalApiId")
    val externalApiId: String? = null,
    @SerialName("lastModifiedAt")
    val lastModifiedAt: Long = 0,
    @SerialName("deletedAt")
    val deletedAt: Long? = null
)
