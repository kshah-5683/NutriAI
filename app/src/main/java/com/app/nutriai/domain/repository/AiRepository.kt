package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.ExtractedLabelData
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.util.Resource

/**
 * Repository interface for AI-powered food operations.
 *
 * Abstracts the AI provider (currently Gemini) behind a domain-level interface.
 * Implementations live in the data layer.
 *
 * Phase 4:  [parseFood] — text-based food entity extraction
 * Phase 11: [extractLabelFromImage] — vision-based nutrition label reading
 */
interface AiRepository {

    /**
     * Parse a natural language food description into structured food entities.
     *
     * Examples:
     * - "2 slices of whole wheat toast with peanut butter"
     *   → [ParsedFood("whole wheat toast", 2.0, "slice"), ParsedFood("peanut butter", 1.0, "serving")]
     * - "a bowl of oatmeal with honey"
     *   → [ParsedFood("oatmeal", 1.0, "bowl"), ParsedFood("honey", 1.0, "serving")]
     *
     * @param input Natural language description of food consumed
     * @param clarificationAnswers Optional map of previously answered clarification IDs and selected options
     * @return [Resource] wrapping a list of [ParsedFood] entities, or an error
     */
    suspend fun parseFood(
        input: String,
        clarificationAnswers: Map<String, String>? = null
    ): Resource<List<ParsedFood>>

    /**
     * Extract per-serving nutrition data from a food label photo using Gemma 4's vision.
     *
     * The image is sent as a base64-encoded inline data part alongside a text prompt.
     * Gemma 4 reads the nutrition facts panel and returns structured macro data.
     *
     * Phase 11: Nutrition Label Scanner.
     *
     * @param imageBase64 Base64-encoded image bytes (no data-URI prefix)
     * @param mimeType    Image MIME type (e.g., "image/jpeg", "image/png")
     * @return [Resource] wrapping [ExtractedLabelData] on success, or an error message
     */
    suspend fun extractLabelFromImage(
        imageBase64: String,
        mimeType: String
    ): Resource<ExtractedLabelData>
}
