package com.app.nutriai.data.repository

import com.app.nutriai.BuildConfig
import com.app.nutriai.data.remote.api.GeminiApiService
import com.app.nutriai.data.remote.dto.GeminiContent
import com.app.nutriai.data.remote.dto.GeminiGenerationConfig
import com.app.nutriai.data.remote.dto.GeminiInlineData
import com.app.nutriai.data.remote.dto.GeminiLabelExtractionDto
import com.app.nutriai.data.remote.dto.GeminiParsedFoodsWrapper
import com.app.nutriai.data.remote.dto.GeminiPart
import com.app.nutriai.data.remote.dto.GeminiRequest
import com.app.nutriai.data.remote.dto.GeminiThinkingConfig
import com.app.nutriai.domain.model.ExtractedLabelData
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.domain.repository.AiRepository
import com.app.nutriai.domain.repository.FoodRepository
import com.app.nutriai.util.Constants
import com.app.nutriai.util.GeminiLabelPrompts
import com.app.nutriai.util.GeminiPrompts
import com.app.nutriai.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * Implementation of [AiRepository] using Google Gemma 4 (hosted on the Gemini API)
 * for food entity extraction.
 *
 * Gemma 4-specific adaptations:
 * - `thinkingLevel = "MINIMAL"` minimizes thinking mode to keep JSON output clean
 * - Schema reinforced in both system instruction and user prompt for reliability
 * - Fallback JSON extraction handles edge cases where model wraps JSON in text
 *
 * Name standardization (Phase 5):
 * - Before each request, fetches existing ingredient and recipe names from [FoodRepository]
 * - Injects both lists into [GeminiPrompts.buildUserPrompt] so the AI can normalize
 *   extracted names against known catalog entries (e.g. "besan flour" → "besan")
 * - Prevents duplicate catalog entries caused by synonym variations
 *
 * Flow:
 * 1. Fetches existing catalog names (ingredients + recipes) from Room
 * 2. Builds request with system instruction + user prompt (schema + catalog context inline)
 * 3. Calls Gemma 4 `generateContent` with low temperature + JSON mime type
 * 4. Extracts and parses JSON response into [ParsedFood] domain models
 * 5. Wraps result in [Resource] for error handling
 *
 * Graceful degradation:
 * - Returns [Resource.Error] on network failure, empty response, or malformed JSON
 * - The caller (UseCase/ViewModel) falls back to manual entry on error
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val json: Json,
    private val foodRepository: FoodRepository
) : AiRepository {

    override suspend fun parseFood(input: String): Resource<List<ParsedFood>> {
        return try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return Resource.Error("API key not configured. Please add GEMINI_API_KEY to local.properties.")
            }

            // Fetch existing catalog names for name standardization.
            // The AI will normalize extracted names against these lists so that
            // e.g. "besan flour" is returned as "besan" if "besan" is already in the catalog.
            // Both queries are lightweight (name column only) and run in parallel via coroutines.
            val existingIngredients = foodRepository.getAllFoodNames(Constants.INGREDIENT_CATALOG_ID)
            val existingRecipes = foodRepository.getAllFoodNames(Constants.RECIPE_CATALOG_ID)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = GeminiPrompts.buildUserPrompt(input, existingIngredients, existingRecipes))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.1,
                    topP = 0.8,
                    topK = 40,
                    maxOutputTokens = 1024,
                    responseMimeType = "application/json",
                    // Minimize Gemma 4's thinking mode — reduces latency and keeps
                    // output cleaner for structured JSON parsing.
                    // Gemma 4 uses thinkingLevel (MINIMAL/HIGH), not thinkingBudget.
                    thinkingConfig = GeminiThinkingConfig(thinkingLevel = "MINIMAL")
                ),
                systemInstruction = GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(text = GeminiPrompts.SYSTEM_INSTRUCTION)
                    )
                )
            )

            val response = geminiApiService.generateContent(
                apiKey = apiKey,
                request = request
            )

            // Check for blocked content
            val blockReason = response.promptFeedback?.blockReason
            if (blockReason != null) {
                return Resource.Error("Content was blocked by safety filters: $blockReason")
            }

            // Extract the generated text from the response.
            // When thinking is enabled (even MINIMAL), Gemma 4 returns multiple parts:
            //   parts[0] = { text: "...", thought: true }  ← thinking (skip this)
            //   parts[1] = { text: "{...}", thought: false } ← actual answer
            // We filter for non-thought parts first, then fall back to any part with text.
            val parts = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts

            val generatedText = parts
                ?.lastOrNull { it.thought != true && !it.text.isNullOrBlank() }
                ?.text
                ?: parts?.lastOrNull { !it.text.isNullOrBlank() }?.text

            if (generatedText.isNullOrBlank()) {
                return Resource.Error("AI returned an empty response. Please try again or enter food manually.")
            }

            // Try to extract clean JSON from the response.
            // Gemma 4 may occasionally wrap JSON in markdown code fences or extra text
            // even with responseMimeType set, especially if thinking leaked through.
            val jsonText = extractJson(generatedText)

            // Parse the JSON response into our DTO
            val parsedWrapper = try {
                json.decodeFromString<GeminiParsedFoodsWrapper>(jsonText)
            } catch (e: Exception) {
                return Resource.Error("AI response was not in the expected format. Please try again or enter food manually.")
            }

            if (parsedWrapper.foods.isEmpty()) {
                return Resource.Error("No food items detected in your input. Please describe what you ate.")
            }

            // Map DTOs to domain models.
            // Phase 4.5: Handles recipe detection — when isRecipe=true,
            // ingredients are mapped recursively into nested ParsedFood items.
            val parsedFoods = parsedWrapper.foods.map { dto ->
                ParsedFood(
                    name = dto.name.trim(),
                    quantity = dto.quantity.coerceAtLeast(0.01),
                    unit = dto.unit.trim().ifBlank { "serving" },
                    confidence = dto.confidence.coerceIn(0.0, 1.0),
                    isRecipe = dto.isRecipe,
                    ingredients = dto.ingredients.map { ingDto ->
                        ParsedFood(
                            name = ingDto.name.trim(),
                            quantity = ingDto.quantity.coerceAtLeast(0.01),
                            unit = ingDto.unit.trim().ifBlank { "serving" },
                            confidence = ingDto.confidence.coerceIn(0.0, 1.0)
                        )
                    }
                )
            }

            Resource.Success(parsedFoods)
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "Invalid request to AI service. Please try a simpler food description."
                401, 403 -> "AI API key is invalid or expired. Please check your Gemini API key."
                404 -> "AI model not found. The model may have been deprecated or renamed."
                429 -> "AI rate limit reached. Please wait a moment and try again."
                500, 503 -> "AI service is temporarily unavailable. Please try again later."
                else -> "AI service error (${e.code()}). Please try again or enter food manually."
            }
            Resource.Error(message, e)
        } catch (e: java.net.UnknownHostException) {
            // DNS resolution failed — device has no internet or DNS is broken.
            Resource.Error("No internet connection. Please check your network and try again, or enter food manually.", e)
        } catch (e: java.net.ConnectException) {
            // TCP connection refused or reset — server unreachable on this network.
            Resource.Error("Cannot reach the AI service. Please check your connection and try again, or enter food manually.", e)
        } catch (e: SSLException) {
            // TLS handshake failed — network may be intercepting HTTPS traffic.
            Resource.Error("Secure connection to AI service failed. Please try again or enter food manually.", e)
        } catch (e: java.net.SocketTimeoutException) {
            Resource.Error("AI request timed out. Please try again or enter food manually.", e)
        } catch (e: java.io.IOException) {
            // Catch-all for remaining I/O network errors (interface change, reset, etc.)
            Resource.Error("Network error while connecting to AI service. Please try again or enter food manually.", e)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "An unexpected error occurred while parsing food. Please enter food manually.",
                e
            )
        }
    }

    /**
     * Phase 11: Extract nutrition data from a food label photo.
     *
     * Sends a multimodal request to Gemma 4: one text part (the extraction prompt)
     * and one inlineData part (the base64-encoded label image). Gemma 4's vision
     * reads the nutrition facts panel and returns structured JSON.
     *
     * Response parsing mirrors [parseFood]: extract non-thought text part, strip
     * any accidental markdown fences, then deserialize into [GeminiLabelExtractionDto].
     *
     * Error handling: same HTTP/network error set as [parseFood], with label-specific
     * messages so the user knows to try again or enter values manually.
     */
    override suspend fun extractLabelFromImage(
        imageBase64: String,
        mimeType: String
    ): Resource<ExtractedLabelData> {
        return try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return Resource.Error("API key not configured. Please add GEMINI_API_KEY to local.properties.")
            }

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            // Text instruction comes first, then the image
                            GeminiPart(text = GeminiLabelPrompts.LABEL_USER_PROMPT),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = imageBase64))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.1,
                    topP = 0.8,
                    topK = 40,
                    maxOutputTokens = 512,
                    responseMimeType = "application/json",
                    thinkingConfig = GeminiThinkingConfig(thinkingLevel = "MINIMAL")
                ),
                systemInstruction = GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(text = GeminiLabelPrompts.LABEL_SYSTEM_INSTRUCTION)
                    )
                )
            )

            val response = geminiApiService.generateContent(
                apiKey = apiKey,
                request = request
            )

            val blockReason = response.promptFeedback?.blockReason
            if (blockReason != null) {
                return Resource.Error("Label image was blocked by safety filters: $blockReason")
            }

            val parts = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts

            val generatedText = parts
                ?.lastOrNull { it.thought != true && !it.text.isNullOrBlank() }
                ?.text
                ?: parts?.lastOrNull { !it.text.isNullOrBlank() }?.text

            if (generatedText.isNullOrBlank()) {
                return Resource.Error("Could not read the label. Please try a clearer photo or enter values manually.")
            }

            val jsonText = extractJson(generatedText)

            val dto = try {
                json.decodeFromString<GeminiLabelExtractionDto>(jsonText)
            } catch (e: Exception) {
                return Resource.Error("Label data was not in the expected format. Please try again or enter values manually.")
            }

            // Map DTO → domain model; default nulls to 0.0 so the form always has a valid prefill
            Resource.Success(
                ExtractedLabelData(
                    caloriesPerServing = dto.caloriesPerServing ?: 0.0,
                    proteinG = dto.proteinG ?: 0.0,
                    carbsG = dto.carbsG ?: 0.0,
                    fatG = dto.fatG ?: 0.0,
                    servingSizeText = dto.servingSizeText,
                    servingWeightG = dto.servingWeightG
                )
            )
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "Invalid image request to AI service. Please try a different photo."
                401, 403 -> "AI API key is invalid or expired. Please check your Gemini API key."
                404 -> "AI model not found. The model may have been deprecated or renamed."
                429 -> "AI rate limit reached. Please wait a moment and try again."
                500, 503 -> "AI service is temporarily unavailable. Please try again later."
                else -> "AI service error (${e.code()}). Please try again or enter values manually."
            }
            Resource.Error(message, e)
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
     * Extracts clean JSON from Gemma 4's response text.
     *
     * Handles edge cases where the model wraps JSON in:
     * - Markdown code fences (```json ... ```)
     * - Extra whitespace or trailing text
     * - Thinking tags that leaked through despite thinkingBudget=0
     *
     * Falls back to the raw text if no JSON object is found.
     */
    private fun extractJson(text: String): String {
        val trimmed = text.trim()

        // Best case: already clean JSON
        if (trimmed.startsWith("{")) {
            return trimmed
        }

        // Strip markdown code fences: ```json ... ``` or ``` ... ```
        val codeFenceRegex = Regex("""```(?:json)?\s*\n?(.*?)\n?\s*```""", RegexOption.DOT_MATCHES_ALL)
        codeFenceRegex.find(trimmed)?.let {
            return it.groupValues[1].trim()
        }

        // Find first JSON object in the text
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1)
        }

        // Fallback: return raw text and let JSON parser report the error
        return trimmed
    }
}
