/**
 * scan-label Edge Function — Multimodal label reading + per-100g conversion.
 * Port of AiRepositoryImpl.extractLabel() + LabelScannerDelegate conversion logic.
 *
 * CRITICAL: Per-serving → Per-100g conversion.
 * When serving_weight_g is available, converts per-serving values to per-100g
 * so they match the storage convention used by food_items.base_* columns.
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import {
  LABEL_SYSTEM_INSTRUCTION,
  LABEL_USER_PROMPT,
} from "../_shared/prompts.ts";
import { extractJson } from "../_shared/json-utils.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

const PER_100G_BASE = 100.0;

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { base64, mimeType } = await req.json();

    if (!base64) {
      return new Response(
        JSON.stringify({ error: "base64 image data is required" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // 1. Call Gemma 4 Vision endpoint
    const geminiKey = Deno.env.get("GEMINI_API_KEY")!;
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemma-4-26b-a4b-it:generateContent?key=${geminiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [
            {
              role: "user",
              parts: [
                {
                  inlineData: {
                    mimeType: mimeType || "image/jpeg",
                    data: base64,
                  },
                },
                { text: LABEL_USER_PROMPT },
              ],
            },
          ],
          systemInstruction: {
            role: "user",
            parts: [{ text: LABEL_SYSTEM_INSTRUCTION }],
          },
          generationConfig: {
            temperature: 0.1,
            topP: 0.8,
            topK: 40,
            maxOutputTokens: 1024,
            responseMimeType: "application/json",
            thinkingConfig: { thinkingLevel: "MINIMAL" },
          },
        }),
      }
    );

    if (!response.ok) {
      const errText = await response.text();
      console.error(`Gemini API error ${response.status}: ${errText}`);
      return new Response(
        JSON.stringify({ error: `AI service error (${response.status})` }),
        {
          status: 502,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    const data = await response.json();
    const parts = data.candidates?.[0]?.content?.parts ?? [];
    const contentPart = parts.findLast(
      // deno-lint-ignore no-explicit-any
      (p: any) => p.thought !== true && p.text?.trim()
    );
    const raw = extractJson(contentPart?.text ?? "{}");

    // 2. CRITICAL: Per-serving → Per-100g conversion
    // Port of LabelScannerDelegate.kt lines 55-77.
    const hasServingWeight =
      raw.serving_weight_g != null && raw.serving_weight_g > 0;
    const w = raw.serving_weight_g ?? 1.0;

    const result = {
      // Raw extraction (always returned for display)
      raw_calories_per_serving: raw.calories_per_serving ?? 0,
      raw_protein_g: raw.protein_g ?? 0,
      raw_carbs_g: raw.carbs_g ?? 0,
      raw_fat_g: raw.fat_g ?? 0,
      serving_size_text: raw.serving_size_text ?? null,
      serving_weight_g: raw.serving_weight_g ?? null,

      // Converted values for form pre-fill (what log-food expects)
      calories: hasServingWeight
        ? (raw.calories_per_serving / w) * PER_100G_BASE
        : raw.calories_per_serving ?? 0,
      protein: hasServingWeight
        ? (raw.protein_g / w) * PER_100G_BASE
        : raw.protein_g ?? 0,
      carbs: hasServingWeight
        ? (raw.carbs_g / w) * PER_100G_BASE
        : raw.carbs_g ?? 0,
      fat: hasServingWeight
        ? (raw.fat_g / w) * PER_100G_BASE
        : raw.fat_g ?? 0,

      // Pre-fill hints for the manual form
      suggested_quantity: hasServingWeight ? w : 1,
      suggested_unit: hasServingWeight ? "grams" : "serving",
    };

    return new Response(JSON.stringify(result), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("scan-label error:", err);
    return new Response(
      JSON.stringify({
        error: err instanceof Error ? err.message : "Internal server error",
      }),
      {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }
});
