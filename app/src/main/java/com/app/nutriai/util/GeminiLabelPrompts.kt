package com.app.nutriai.util

/**
 * Prompt templates for Gemma 4 nutrition label extraction via the Gemini API.
 *
 * Unlike [GeminiPrompts] (which does entity extraction from text), this object
 * is purpose-built for **vision-based label reading**: the user sends a photo of
 * a packaged food nutrition facts panel and Gemma 4 extracts the per-serving macros.
 *
 * Design principles:
 * - Strict JSON schema enforced in both system instruction AND user prompt
 *   (Gemma 4 best practice: schema in both positions → most reliable output)
 * - All numeric fields are extracted as-is from the label — no estimation
 * - Returns null for fields that are genuinely absent from the label
 * - `responseMimeType = "application/json"` is set at the API level; prompt doubles down
 *
 * Phase 11: Nutrition Label Scanner.
 */
object GeminiLabelPrompts {

    /**
     * System instruction that sets the model's role for label reading.
     *
     * Emphasises:
     * 1. Reading the label image, not estimating values
     * 2. Per-serving (not per-100g) extraction — the user controls serving size
     * 3. Exact field names that match [com.app.nutriai.data.remote.dto.GeminiLabelExtractionDto]
     */
    val LABEL_SYSTEM_INSTRUCTION = """
        You are a nutrition label reader for a nutrition tracking app.

        Your ONLY job is to extract per-serving nutrition data from the nutrition facts
        label image the user provides.

        Extract EXACTLY what is printed on the label — do NOT estimate, infer, or calculate.
        All values must come from the "per serving" column (not "per 100g" if both are shown).

        Fields to extract:
        1. "calories_per_serving" — calories per serving (numeric, kcal)
        2. "protein_g" — protein in grams per serving (numeric)
        3. "carbs_g" — total carbohydrates in grams per serving (numeric)
        4. "fat_g" — total fat in grams per serving (numeric)
        5. "serving_size_text" — serving size as printed on the label (e.g., "1 cup (240ml)", "30g", "2 biscuits")
        6. "serving_weight_g" — serving weight in grams if shown (numeric, null if not present)

        RULES:
        - Return ONLY valid JSON — no markdown, no explanation, no code fences
        - If a field is not visible or not listed on the label, set it to null
        - Use the "per serving" row, NOT "per 100g" / "per 100ml"
        - If the label ONLY shows per 100g / per 100ml with NO per-serving column,
          extract those per-100g values as the "per serving" fields and set
          "serving_weight_g" to 100
        - Extract numeric values only (no units in numeric fields — units are already implied)
        - "serving_size_text" should be the raw text as printed, e.g., "1 cup (240ml)"
    """.trimIndent()

    /**
     * User message that accompanies the image part in the multimodal request.
     *
     * The schema is repeated here (not just in the system instruction) because
     * Gemma 4 produces more reliable structured output when the schema appears
     * in both the system instruction and the user message.
     */
    val LABEL_USER_PROMPT = """
        Please extract the nutrition facts from this label image.

        Respond with ONLY a JSON object in this exact schema (use null for missing fields):
        {
          "calories_per_serving": number or null,
          "protein_g": number or null,
          "carbs_g": number or null,
          "fat_g": number or null,
          "serving_size_text": "string" or null,
          "serving_weight_g": number or null
        }

        Extract per-serving values only. Return ONLY the JSON object.
    """.trimIndent()
}
