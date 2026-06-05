/**
 * recommend-meals Edge Function — AI meal recommendations with catalog + internet support.
 *
 * Flow:
 *   1. Validate input (mode, remainingMacros, optional query)
 *   2. Get user ID from JWT
 *   3. Fetch catalog food items + log frequencies for pre-filtering
 *   4. Rank by usage frequency, shuffle top 20, take 15 for prompt
 *   5. Optionally fetch user profile (when includeInternet=true)
 *   6. Build recommendation prompt
 *   7. Call Gemma 4 (gemma-4-26b-a4b-it)
 *   8. Extract JSON, return recommendations
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import {
  RECOMMENDATION_SYSTEM_INSTRUCTION,
  buildRecommendationPrompt,
} from "../_shared/prompts.ts";
import { extractJson } from "../_shared/json-utils.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    // 1. Parse + validate request body
    const body = await req.json();
    const { mode, query, timeOfDay, remainingMacros, includeInternet, targetMeal } = body;

    if (!["time_based", "query"].includes(mode)) {
      return jsonError("Invalid mode — must be 'time_based' or 'query'", 400);
    }

    let sanitizedQuery: string | undefined;
    if (mode === "query") {
      if (!query?.trim() || query.length > 200) {
        return jsonError(
          "Query must be 1-200 characters for mode='query'",
          400
        );
      }
      // Strip HTML/markdown tags
      sanitizedQuery = query
        .replace(/<[^>]*>/g, "")
        .replace(/[#*_~`]/g, "")
        .trim();
    }

    if (
      !remainingMacros ||
      typeof remainingMacros.calories !== "number" ||
      typeof remainingMacros.protein !== "number" ||
      typeof remainingMacros.carbs !== "number" ||
      typeof remainingMacros.fat !== "number"
    ) {
      return jsonError(
        "remainingMacros must include calories, protein, carbs, fat (numbers)",
        400
      );
    }

    // 2. Auth — get user from JWT
    const authHeader = req.headers.get("Authorization")!;
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } }
    );

    const {
      data: { user },
    } = await supabase.auth.getUser();
    const userId = user!.id;

    // 3. Fetch catalog food items + log frequencies in parallel
    const ingredientCatalogId = `${userId}_local_user_ingredients`;
    const recipeCatalogId = `${userId}_local_user_recipes`;

    const [{ data: foodItems }, { data: logRows }] = await Promise.all([
      supabase
        .from("food_items")
        .select("id, name, catalog_id, base_calories, base_protein, base_carbs, base_fat")
        .in("catalog_id", [ingredientCatalogId, recipeCatalogId])
        .is("deleted_at", null),
      supabase
        .from("daily_logs")
        .select("food_item_id")
        .eq("user_id", userId)
        .is("deleted_at", null)
        .not("food_item_id", "is", null),
    ]);

    // 4. Count frequencies, then rank recipes and ingredients separately.
    // Saved recipes get their own top-N slice so they aren't crowded out by
    // frequently-logged raw ingredients.
    const freqMap = new Map<string, number>();
    for (const row of logRows ?? []) {
      freqMap.set(
        row.food_item_id,
        (freqMap.get(row.food_item_id) ?? 0) + 1
      );
    }

    const allItems = foodItems ?? [];

    // deno-lint-ignore no-explicit-any
    const recipeItems = allItems.filter((item: any) => item.catalog_id === recipeCatalogId);
    // deno-lint-ignore no-explicit-any
    const ingredientItems = allItems.filter((item: any) => item.catalog_id === ingredientCatalogId);

    // deno-lint-ignore no-explicit-any
    const withFreq = (item: any) => ({ ...item, freq: freqMap.get(item.id) ?? 0 });
    // deno-lint-ignore no-explicit-any
    const byFreqDesc = (a: any, b: any) => b.freq - a.freq;
    // deno-lint-ignore no-explicit-any
    const toPromptItem = (item: any) => ({
      id: item.id,
      name: item.name,
      kcal: item.base_calories,
      p: item.base_protein,
      c: item.base_carbs,
      f: item.base_fat,
    });

    // Top 8 recipes by frequency (shuffled to add variety), up to 5 in prompt
    const savedRecipesForPrompt = shuffle(
      recipeItems.map(withFreq).sort(byFreqDesc).slice(0, 8)
    ).slice(0, 5).map(toPromptItem);

    // Top 15 ingredients by frequency (shuffled), up to 10 in prompt
    const availableIngredientsForPrompt = shuffle(
      ingredientItems.map(withFreq).sort(byFreqDesc).slice(0, 15)
    ).slice(0, 10).map(toPromptItem);

    // 5. Optionally fetch profile (only if includeInternet)
    let profile = null;
    if (includeInternet) {
      const { data: prefs } = await supabase
        .from("user_preferences")
        .select(
          "diet_type, cuisine_preferences, allergies, weight_goal, recommendations_enabled"
        )
        .eq("user_id", userId)
        .maybeSingle();

      if (prefs?.recommendations_enabled) {
        profile = {
          dietType: prefs.diet_type,
          cuisines: (prefs.cuisine_preferences ?? [])
            .map(sanitizeProfileEntry)
            .filter(Boolean),
          allergies: (prefs.allergies ?? [])
            .map(sanitizeProfileEntry)
            .filter(Boolean),
          weightGoal: prefs.weight_goal,
        };
      }
    }

    // 6. Build prompt
    const userPrompt = buildRecommendationPrompt({
      mode,
      timeOfDay: timeOfDay ?? inferTimeOfDay(),
      remainingMacros,
      savedRecipes: savedRecipesForPrompt,
      availableIngredients: availableIngredientsForPrompt,
      query: mode === "query" ? sanitizedQuery : undefined,
      profile,
      targetMeal: targetMeal ?? null,
    });

    // 7. Call Gemma 4
    const geminiKey = Deno.env.get("GEMINI_API_KEY")!;
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemma-4-26b-a4b-it:generateContent?key=${geminiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: userPrompt }] }],
          systemInstruction: {
            role: "user",
            parts: [{ text: RECOMMENDATION_SYSTEM_INSTRUCTION }],
          },
          generationConfig: {
            temperature: 0.7,
            topP: 0.9,
            topK: 40,
            maxOutputTokens: 2048,
            responseMimeType: "application/json",
            thinkingConfig: { thinkingLevel: "MINIMAL" },
          },
        }),
      }
    );

    if (!response.ok) {
      const errText = await response.text();
      console.error(`Gemini API error ${response.status}: ${errText}`);
      return jsonError(`AI service error (${response.status})`, 502);
    }

    // 8. Extract JSON from response (filter thought parts)
    const data = await response.json();
    const parts = data.candidates?.[0]?.content?.parts ?? [];
    const contentPart = parts.findLast(
      // deno-lint-ignore no-explicit-any
      (p: any) => p.thought !== true && p.text?.trim()
    );

    const parsed = extractJson(
      contentPart?.text ?? '{"recommendations": []}'
    );

    return new Response(JSON.stringify(parsed), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("recommend-meals error:", err);
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

// ─── Helpers ────────────────────────────────────────────────────────────────

/**
 * Sanitizes a user-provided profile string (cuisine name, allergy name).
 * Defends against prompt injection via custom entries.
 *
 * - Strips HTML tags, markdown/special chars, and control characters
 * - Truncates to 40 chars (no real cuisine/allergy name is longer)
 * - Returns empty string if nothing useful remains (caller filters with .filter(Boolean))
 */
function sanitizeProfileEntry(raw: string): string {
  return raw
    .replace(/<[^>]*>/g, "")              // strip HTML tags
    .replace(/[#*_~`\[\]{}()|\\]/g, "")   // strip markdown/special chars
    .replace(/[\x00-\x1F\x7F]/g, "")      // strip control characters
    .trim()
    .slice(0, 40);
}

/** Returns a JSON error response with CORS headers. */
function jsonError(message: string, status: number): Response {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

/** Fisher-Yates shuffle — returns a new shuffled array. */
function shuffle<T>(arr: T[]): T[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

/** Infer time of day from server clock (UTC fallback if client didn't send it). */
function inferTimeOfDay(): string {
  const hour = new Date().getUTCHours();
  if (hour >= 6 && hour <= 10) return "morning";
  if (hour >= 11 && hour <= 14) return "afternoon";
  if (hour >= 15 && hour <= 18) return "evening";
  if (hour >= 19 && hour <= 21) return "night";
  return "evening"; // safe default for late night
}
