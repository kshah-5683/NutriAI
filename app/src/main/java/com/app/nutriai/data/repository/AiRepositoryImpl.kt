package com.app.nutriai.data.repository

import com.app.nutriai.data.remote.api.SupabaseEdgeFunctionService
import com.app.nutriai.data.remote.dto.CatalogMatchDto
import com.app.nutriai.data.remote.dto.FoodItemDto
import com.app.nutriai.data.remote.dto.ParseFoodRequest
import com.app.nutriai.data.remote.dto.ParsedFoodDto
import com.app.nutriai.data.remote.dto.ClarificationDto
import com.app.nutriai.data.remote.dto.ScanLabelRequest
import com.app.nutriai.domain.model.EdgeCatalogMatch
import com.app.nutriai.domain.model.ExtractedLabelData
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.domain.model.Clarification
import com.app.nutriai.domain.repository.AiRepository
import com.app.nutriai.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * Implementation of [AiRepository] using Supabase Edge Functions for food parsing
 * and label scanning.
 *
 * Replaces the previous implementation that called the Gemini API directly from the device.
 * Now calls the same `parse-food` and `scan-label` Edge Functions used by the webapp,
 * achieving full parity:
 *
 * - **Single-sourced prompts** — prompts live in `supabase/functions/_shared/prompts.ts`
 * - **No API key in APK** — Gemini key stays server-side only
 * - **Server-side catalog resolution** — Edge Function resolves catalog matches against Supabase
 * - **Server-side per-100g conversion** — `scan-label` converts per-serving → per-100g
 *
 * Auth: Edge Functions require a valid Supabase JWT (already injected by the OkHttp
 * interceptor on the `@Named("supabase")` Retrofit instance).
 *
 * Error handling: same user-friendly messages as before, but HTTP error codes now come
 * from the Edge Function (which proxies Gemini errors as 502).
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    private val edgeFunctionService: SupabaseEdgeFunctionService
) : AiRepository {

    override suspend fun parseFood(
        input: String,
        clarificationAnswers: Map<String, String>?
    ): Resource<List<ParsedFood>> {
        return try {
            val response = edgeFunctionService.parseFoodViaEdge(
                ParseFoodRequest(foodDescription = input, clarificationAnswers = clarificationAnswers)
            )

            if (!response.isSuccessful) {
                val message = mapHttpError(response.code(), isLabel = false)
                return Resource.Error(message)
            }

            val body = response.body()
                ?: return Resource.Error("AI returned an empty response. Please try again or enter food manually.")

            if (body.error != null) {
                return Resource.Error(body.error)
            }

            if (body.foods.isEmpty()) {
                return Resource.Error("No food items detected in your input. Please describe what you ate.")
            }

            val parsedFoods = body.foods.map { it.toDomain() }
            Resource.Success(parsedFoods)
        } catch (e: java.net.UnknownHostException) {
            Resource.Error("No internet connection. Please check your network and try again, or enter food manually.", e)
        } catch (e: java.net.ConnectException) {
            Resource.Error("Cannot reach the AI service. Please check your connection and try again, or enter food manually.", e)
        } catch (e: SSLException) {
            Resource.Error("Secure connection to AI service failed. Please try again or enter food manually.", e)
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("AI request timed out. Please try again or enter food manually.", e)
        } catch (e: java.io.IOException) {
            Resource.Error("Network error while connecting to AI service. Please try again or enter food manually.", e)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "An unexpected error occurred while parsing food. Please enter food manually.",
                e
            )
        }
    }

    override suspend fun extractLabelFromImage(
        imageBase64: String,
        mimeType: String
    ): Resource<ExtractedLabelData> {
        return try {
            val response = edgeFunctionService.scanLabelViaEdge(
                ScanLabelRequest(base64 = imageBase64, mimeType = mimeType)
            )

            if (!response.isSuccessful) {
                val message = mapHttpError(response.code(), isLabel = true)
                return Resource.Error(message)
            }

            val body = response.body()
                ?: return Resource.Error("Could not read the label. Please try a clearer photo or enter values manually.")

            if (body.error != null) {
                return Resource.Error(body.error)
            }

            Resource.Success(
                ExtractedLabelData(
                    // Raw per-serving values
                    caloriesPerServing = body.rawCaloriesPerServing,
                    proteinG = body.rawProteinG,
                    carbsG = body.rawCarbsG,
                    fatG = body.rawFatG,
                    servingSizeText = body.servingSizeText,
                    servingWeightG = body.servingWeightG,
                    // Pre-computed per-100g values from Edge Function
                    calories = body.calories,
                    protein = body.protein,
                    carbs = body.carbs,
                    fat = body.fat,
                    // Form pre-fill hints
                    suggestedQuantity = body.suggestedQuantity,
                    suggestedUnit = body.suggestedUnit
                )
            )
        } catch (e: java.net.UnknownHostException) {
            Resource.Error("No internet connection. Please check your network and try again, or enter values manually.", e)
        } catch (e: java.net.ConnectException) {
            Resource.Error("Cannot reach the AI service. Please check your connection and try again, or enter values manually.", e)
        } catch (e: SSLException) {
            Resource.Error("Secure connection to AI service failed. Please try again or enter values manually.", e)
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("AI request timed out. Please try a smaller/clearer photo or enter values manually.", e)
        } catch (e: java.io.IOException) {
            Resource.Error("Network error while reading the label. Please try again or enter values manually.", e)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "An unexpected error occurred. Please enter values manually.",
                e
            )
        }
    }

    /**
     * Maps HTTP error codes to user-friendly messages.
     * Edge Functions return 502 when the upstream Gemini API fails.
     */
    private fun mapHttpError(code: Int, isLabel: Boolean): String {
        val suffix = if (isLabel) "enter values manually" else "enter food manually"
        return when (code) {
            400 -> if (isLabel) "Invalid image request. Please try a different photo."
                   else "Invalid request. Please try a simpler food description."
            401, 403 -> "Authentication error. Please sign in again."
            404 -> "AI service endpoint not found. Please update the app."
            429 -> "AI rate limit reached. Please wait a moment and try again."
            502 -> "AI service is temporarily unavailable. Please try again later."
            500, 503 -> "Server error. Please try again later."
            else -> "Service error ($code). Please try again or $suffix."
        }
    }
}

/** Map [ParsedFoodDto] → [ParsedFood] domain model, recursively for recipe ingredients. */
private fun ParsedFoodDto.toDomain(): ParsedFood {
    return ParsedFood(
        name = name.trim(),
        quantity = quantity.coerceAtLeast(0.01),
        unit = unit.trim().ifBlank { "serving" },
        confidence = confidence.coerceIn(0.0, 1.0),
        isRecipe = isRecipe,
        ingredients = ingredients.map { it.toDomain() },
        needsClarification = needsClarification,
        clarificationHint = clarificationHint,
        clarifications = clarifications?.map { it.toDomain() },
        edgeCatalogMatch = catalogMatch?.toDomain()
    )
}

private fun ClarificationDto.toDomain(): Clarification {
    return Clarification(
        id = id,
        question = question,
        options = options
    )
}

/** Map [CatalogMatchDto] → [EdgeCatalogMatch] domain model. */
private fun CatalogMatchDto.toDomain(): EdgeCatalogMatch {
    return EdgeCatalogMatch(
        isFromCatalog = isFromCatalog,
        foodItem = foodItem?.toDomain()
    )
}

/** Map [FoodItemDto] → [FoodItem] domain model. */
private fun FoodItemDto.toDomain(): FoodItem {
    return FoodItem(
        id = id,
        catalogId = catalogId,
        name = name,
        brand = brand,
        baseServingG = baseServingG,
        baseCalories = baseCalories,
        baseProtein = baseProtein,
        baseCarbs = baseCarbs,
        baseFat = baseFat,
        externalApiId = externalApiId,
        lastModifiedAt = lastModifiedAt,
        deletedAt = deletedAt
    )
}
