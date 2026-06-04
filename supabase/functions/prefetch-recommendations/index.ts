/**
 * prefetch-recommendations Edge Function — proactively generates and caches
 * AI meal recommendations for the next meal slot.
 *
 * Called fire-and-forget after every food log, edit, or delete.
 * Also called when the home screen opens with an empty cache (cold-start).
 *
 * Flow:
 *   1. Get user ID from JWT
 *   2. Fetch today's logged meals to determine next meal slot
 *   3. Check cooldown (5-min debounce via created_at on recommendation_cache)
 *   4. Check staleness (15% calorie diff threshold)
 *   5. If stale or missing → compute remaining macros, call Gemma 4
 *   6. Upsert into recommendation_cache
 *
 * Server-side cooldown is more robust than client-side debounce:
 * works across platforms (webapp + Android), survives app close/reopen.
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import {
  RECOMMENDATION_SYSTEM_INSTRUCTION,
  buildRecommendationPrompt,
} from "../_shared/prompts.ts";
import { extractJson } from "../_shared/json-utils.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { determineNextMealSlot } from "../_shared/meal-progression.ts";

/** 5 minutes in milliseconds — cooldown between AI calls. */
const COOLDOWN_MS = 5 * 60 * 1000;

/** 15% calorie difference threshold for staleness check. */
const STALENESS_THRESHOLD = 0.15;

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    // 1. Auth — get user from JWT
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

    // 2. Use client-provided timestamps to avoid UTC vs local timezone mismatch.
    //    The webapp always sends its local midnight epoch ms and current hour.
    //    Fall back to server time only if body is missing (e.g. direct API call).
    let body: { dateTimestamp?: number; currentHour?: number } = {};
    try {
      body = await req.json();
    } catch {
      // No body or invalid JSON — fall back to server time
    }

    const now = new Date();
    const startOfDay =
      body.dateTimestamp ??
      new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const currentHour = body.currentHour ?? now.getHours();

    // 3. Fetch today's logged meals
    const { data: todayLogs } = await supabase
      .from("daily_logs")
      .select("meal_type")
      .eq("user_id", userId)
      .gte("date_timestamp", startOfDay)
      .is("deleted_at", null)
      .not("meal_type", "is", null);

    const loggedMeals = (todayLogs ?? [])
      .map((r: { meal_type: string }) => r.meal_type)
      .filter(Boolean) as Array<
      "breakfast" | "snack" | "lunch" | "dinner"
    >;

    // 4. Determine next meal slot
    const nextMeal = determineNextMealSlot(loggedMeals, currentHour);
    if (!nextMeal) {
      return jsonOk({ skipped: true, reason: "late_night" });
    }

    // 5. Check existing cache — cooldown + staleness
    const { data: existingCache } = await supabase
      .from("recommendation_cache")
      .select("remaining_macros, created_at")
      .eq("user_id", userId)
      .eq("meal_type", nextMeal)
      .eq("date_timestamp", startOfDay)
      .maybeSingle();

    // 6. Compute current remaining macros
    // Fetch goals
    const { data: prefs } = await supabase
      .from("user_preferences")
      .select(
        "calorie_goal, protein_goal, carbs_goal, fat_goal, diet_type, cuisine_preferences, allergies, weight_goal, recommendations_enabled"
      )
      .eq("user_id", userId)
      .maybeSingle();

    const goals = {
      calories: prefs?.calorie_goal ?? 2000,
      protein: prefs?.protein_goal ?? 150,
      carbs: prefs?.carbs_goal ?? 250,
      fat: prefs?.fat_goal ?? 65,
    };

    // Fetch today's totals
    const { data: todayTotals } = await supabase
      .from("daily_logs")
      .select(
        "total_calories, total_protein, total_carbs, total_fat"
      )
      .eq("user_id", userId)
      .gte("date_timestamp", startOfDay)
      .is("deleted_at", null);

    let sumCal = 0,
      sumPro = 0,
      sumCarb = 0,
      sumFat = 0;
    for (const row of todayTotals ?? []) {
      sumCal += row.total_calories ?? 0;
      sumPro += row.total_protein ?? 0;
      sumCarb += row.total_carbs ?? 0;
      sumFat += row.total_fat ?? 0;
    }

    const remainingMacros = {
      calories: goals.calories - sumCal,
      protein: goals.protein - sumPro,
      carbs: goals.carbs - sumCarb,
      fat: goals.fat - sumFat,
    };

    if (existingCache) {
      // Cooldown check: if cache was written less than 5 min ago, skip
      const cacheAge =
        Date.now() - new Date(existingCache.created_at).getTime();
      if (cacheAge < COOLDOWN_MS) {
        return jsonOk({ skipped: true, reason: "cooldown" });
      }

      // Staleness check: if calorie difference < 15%, cache is still relevant
      const cachedCal =
        (existingCache.remaining_macros as { calories?: number })
          ?.calories ?? 0;
      if (
        cachedCal > 0 &&
        remainingMacros.calories > 0 &&
        Math.abs(remainingMacros.calories - cachedCal) / cachedCal <
          STALENESS_THRESHOLD
      ) {
        return jsonOk({ skipped: true, reason: "fresh" });
      }
    }

    // 7. Fetch catalog food items + log frequencies for prompt
    const ingredientCatalogId = `${userId}_local_user_ingredients`;
    const recipeCatalogId = `${userId}_local_user_recipes`;

    const [{ data: foodItems }, { data: logRows }] = await Promise.all([
      supabase
        .from("food_items")
        .select(
          "id, name, base_calories, base_protein, base_carbs, base_fat"
        )
        .in("catalog_id", [ingredientCatalogId, recipeCatalogId])
        .is("deleted_at", null),
      supabase
        .from("daily_logs")
        .select("food_item_id")
        .eq("user_id", userId)
        .is("deleted_at", null)
        .not("food_item_id", "is", null),
    ]);

    // Rank by usage frequency, shuffle top 20, take 15
    const freqMap = new Map<string, number>();
    for (const row of logRows ?? []) {
      freqMap.set(
        row.food_item_id,
        (freqMap.get(row.food_item_id) ?? 0) + 1
      );
    }

    const ranked = (foodItems ?? [])
      // deno-lint-ignore no-explicit-any
      .map((item: any) => ({
        ...item,
        freq: freqMap.get(item.id) ?? 0,
      }))
      // deno-lint-ignore no-explicit-any
      .sort((a: any, b: any) => b.freq - a.freq)
      .slice(0, 20);

    const catalogForPrompt = shuffle(ranked)
      .slice(0, 15)
      // deno-lint-ignore no-explicit-any
      .map((item: any) => ({
        id: item.id,
        name: item.name,
        kcal: item.base_calories,
        p: item.base_protein,
        c: item.base_carbs,
        f: item.base_fat,
      }));

    // Build profile if recommendations are enabled
    let profile = null;
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

    // 8. Build prompt + call Gemma 4
    const timeOfDay = inferTimeOfDay(currentHour);
    const userPrompt = buildRecommendationPrompt({
      mode: "time_based",
      timeOfDay,
      remainingMacros,
      catalogItems: catalogForPrompt,
      profile,
      targetMeal: nextMeal,
    });

    const geminiKey = Deno.env.get("GEMINI_API_KEY")!;
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemma-4-26b-a4b-it:generateContent?key=${geminiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [
            { role: "user", parts: [{ text: userPrompt }] },
          ],
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
      console.error(
        `Gemini API error ${response.status}: ${errText}`
      );
      return jsonError(`AI service error (${response.status})`, 502);
    }

    // 9. Extract JSON from response
    const data = await response.json();
    const parts = data.candidates?.[0]?.content?.parts ?? [];
    const contentPart = parts.findLast(
      // deno-lint-ignore no-explicit-any
      (p: any) => p.thought !== true && p.text?.trim()
    );

    const parsed = extractJson(
      contentPart?.text ?? '{"recommendations": []}'
    );

    // 10. Upsert into recommendation_cache
    const { error: upsertError } = await supabase
      .from("recommendation_cache")
      .upsert(
        {
          user_id: userId,
          meal_type: nextMeal,
          date_timestamp: startOfDay,
          recommendations: parsed.recommendations ?? [],
          remaining_macros: remainingMacros,
        },
        { onConflict: "user_id,meal_type,date_timestamp" }
      );

    if (upsertError) {
      console.error("Cache upsert error:", upsertError);
      return jsonError("Failed to cache recommendations", 500);
    }

    return jsonOk({
      cached: true,
      mealType: nextMeal,
      count: (parsed.recommendations ?? []).length,
    });
  } catch (err) {
    console.error("prefetch-recommendations error:", err);
    return new Response(
      JSON.stringify({
        error:
          err instanceof Error ? err.message : "Internal server error",
      }),
      {
        status: 500,
        headers: {
          ...corsHeaders,
          "Content-Type": "application/json",
        },
      }
    );
  }
});

// ─── Helpers ────────────────────────────────────────────────────────────────

function sanitizeProfileEntry(raw: string): string {
  return raw
    .replace(/<[^>]*>/g, "")
    .replace(/[#*_~`\[\]{}()|\\]/g, "")
    .replace(/[\x00-\x1F\x7F]/g, "")
    .trim()
    .slice(0, 40);
}

function jsonOk(body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function jsonError(message: string, status: number): Response {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function shuffle<T>(arr: T[]): T[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function inferTimeOfDay(hour: number): string {
  if (hour >= 6 && hour <= 10) return "morning";
  if (hour >= 11 && hour <= 14) return "afternoon";
  if (hour >= 15 && hour <= 18) return "evening";
  if (hour >= 19 && hour <= 21) return "night";
  return "evening";
}
