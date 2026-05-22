package com.app.nutriai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for the Gemini API `generateContent` endpoint.
 *
 * Used with Gemma 4 model hosted on the Gemini API.
 * Includes thinking config to disable Gemma 4's built-in thinking mode,
 * which can interfere with structured JSON output.
 *
 * @see <a href="https://ai.google.dev/gemma/docs/core/gemma_on_gemini_api">Gemma on Gemini API</a>
 */
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null,
    @SerialName("systemInstruction")
    val systemInstruction: GeminiContent? = null,
    @SerialName("toolConfig")
    val toolConfig: GeminiToolConfig? = null
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inlineData")
    val inlineData: GeminiInlineData? = null
)

/**
 * Inline image data for multimodal Gemma 4 requests via the Gemini API.
 *
 * Gemma 4 is natively multimodal — text and image parts can be mixed within
 * the same `contents` entry using the `inlineData` field. Images are sent as
 * raw base64-encoded bytes (no data-URI prefix required).
 *
 * @param mimeType Image MIME type (e.g., "image/jpeg", "image/png")
 * @param data Base64-encoded image bytes
 *
 * @see <a href="https://ai.google.dev/gemma/docs/core/gemma_on_gemini_api">Gemma 4 multimodal</a>
 */
@Serializable
data class GeminiInlineData(
    @SerialName("mimeType")
    val mimeType: String,
    val data: String
)

/**
 * Generation configuration for deterministic food parsing with Gemma 4.
 *
 * - Low temperature (0.1) for consistent structured output
 * - `responseMimeType = "application/json"` enforces JSON output at the API level
 * - `thinkingConfig` with `thinkingBudget = 0` disables Gemma 4's built-in
 *   thinking mode, which can interfere with structured output (known issue)
 */
@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.1,
    val topP: Double = 0.8,
    val topK: Int = 40,
    val maxOutputTokens: Int = 1024,
    @SerialName("responseMimeType")
    val responseMimeType: String = "application/json",
    @SerialName("thinkingConfig")
    val thinkingConfig: GeminiThinkingConfig? = null
)

/**
 * Controls Gemma 4's built-in thinking/reasoning mode.
 *
 * Gemma 4 uses `thinkingLevel` (not `thinkingBudget` which is for Gemini 2.5).
 * Supported levels for Gemma 4: "MINIMAL" and "HIGH" only.
 * Setting `thinkingLevel = "MINIMAL"` minimizes thinking overhead for faster,
 * more predictable structured JSON output.
 *
 * Note: Using `thinkingBudget` with Gemma 4 returns a 400 error.
 *
 * @see <a href="https://ai.google.dev/gemma/docs/core/gemma_on_gemini_api">Gemma 4 on Gemini API</a>
 */
@Serializable
data class GeminiThinkingConfig(
    @SerialName("thinkingLevel")
    val thinkingLevel: String = "MINIMAL"
)

/**
 * Tool configuration — reserved for future use (function calling, etc.)
 */
@Serializable
data class GeminiToolConfig(
    @SerialName("functionCallingConfig")
    val functionCallingConfig: GeminiFunctionCallingConfig? = null
)

@Serializable
data class GeminiFunctionCallingConfig(
    val mode: String = "NONE"
)

/**
 * Response DTO for Gemma 4 nutrition label extraction.
 *
 * All fields are nullable — the model may not find every value on a label
 * (e.g., a label might not list serving weight in grams). The domain layer
 * handles nulls by defaulting unknown macros to 0.0.
 *
 * Field names use snake_case to match the JSON schema enforced in the prompt.
 * `@SerialName` annotations ensure deserialization works even if the JSON key
 * differs from the Kotlin property name.
 */
@Serializable
data class GeminiLabelExtractionDto(
    @SerialName("calories_per_serving")
    val caloriesPerServing: Double? = null,
    @SerialName("protein_g")
    val proteinG: Double? = null,
    @SerialName("carbs_g")
    val carbsG: Double? = null,
    @SerialName("fat_g")
    val fatG: Double? = null,
    @SerialName("serving_size_text")
    val servingSizeText: String? = null,
    @SerialName("serving_weight_g")
    val servingWeightG: Double? = null
)
