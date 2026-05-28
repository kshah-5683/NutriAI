/**
 * parse-food Edge Function — AI entity extraction with catalog context.
 * Port of AiRepositoryImpl.parseFood() + ResolveCatalogCacheUseCase.
 *
 * Flow:
 *   1. Validate input (1-500 chars)
 *   2. Get user ID from JWT
 *   3. Fetch existing catalog names for name standardization
 *   4. Build prompt with catalog context
 *   5. Call Gemma 4 (gemma-4-26b-a4b-it)
 *   6. Extract JSON, filter thought parts
 *   7. Resolve catalog cache matches
 *   8. Return { foods: ParsedFood[] }
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { SYSTEM_INSTRUCTION, buildUserPrompt } from "../_shared/prompts.ts";
import { extractJson } from "../_shared/json-utils.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { foodDescription } = await req.json();

    // 1. Validate input
    if (!foodDescription?.trim() || foodDescription.length > 500) {
      return new Response(
        JSON.stringify({
          error: "Food description must be 1-500 characters",
        }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    const authHeader = req.headers.get("Authorization")!;

    // 2. Create authenticated Supabase client
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } }
    );

    // 3. Get user ID from JWT
    const {
      data: { user },
    } = await supabase.auth.getUser();
    const userId = user!.id;

    // 4. Fetch existing catalog names for name standardization
    const ingredientCatalogId = `${userId}_local_user_ingredients`;
    const recipeCatalogId = `${userId}_local_user_recipes`;

    const [{ data: ingredients }, { data: recipes }] = await Promise.all([
      supabase
        .from("food_items")
        .select("name")
        .eq("catalog_id", ingredientCatalogId)
        .is("deleted_at", null),
      supabase
        .from("food_items")
        .select("name")
        .eq("catalog_id", recipeCatalogId)
        .is("deleted_at", null),
    ]);

    const existingIngredients = (ingredients ?? []).map(
      (r: { name: string }) => r.name
    );
    const existingRecipes = (recipes ?? []).map(
      (r: { name: string }) => r.name
    );

    // 5. Build prompt with catalog context
    const userPrompt = buildUserPrompt(
      foodDescription,
      existingIngredients,
      existingRecipes
    );

    // 6. Call Gemma 4 API
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
            parts: [{ text: SYSTEM_INSTRUCTION }],
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

    // 7. Extract JSON from response (filter thought parts)
    const data = await response.json();
    const parts = data.candidates?.[0]?.content?.parts ?? [];
    const contentPart = parts.findLast(
      // deno-lint-ignore no-explicit-any
      (p: any) => p.thought !== true && p.text?.trim()
    );

    const parsed = extractJson(contentPart?.text ?? '{"foods": []}');

    // 8. Resolve catalog cache matches
    const resolvedFoods = await Promise.all(
      // deno-lint-ignore no-explicit-any
      (parsed.foods ?? []).map(async (food: any) => {
        const primaryCatalogId = food.is_recipe ? recipeCatalogId : ingredientCatalogId;

        const { data: primaryMatch } = await supabase
          .from("food_items")
          .select("*")
          .eq("catalog_id", primaryCatalogId)
          .ilike("name", food.name)
          .is("deleted_at", null)
          .limit(1)
          .maybeSingle();

        // Fallback: Gemini sometimes marks saved recipes as is_recipe=false.
        // If the ingredient catalog missed it, also check the recipe catalog.
        let match = primaryMatch;
        if (!match && !food.is_recipe) {
          const { data: recipeMatch } = await supabase
            .from("food_items")
            .select("*")
            .eq("catalog_id", recipeCatalogId)
            .ilike("name", food.name)
            .is("deleted_at", null)
            .limit(1)
            .maybeSingle();
          match = recipeMatch ?? null;
        }

        // Resolve ingredients for recipes too
        let resolvedIngredients: unknown[] = [];
        if (food.is_recipe && Array.isArray(food.ingredients)) {
          resolvedIngredients = await Promise.all(
            // deno-lint-ignore no-explicit-any
            food.ingredients.map(async (ing: any) => {
              const { data: ingMatch } = await supabase
                .from("food_items")
                .select("*")
                .eq("catalog_id", ingredientCatalogId)
                .ilike("name", ing.name)
                .is("deleted_at", null)
                .limit(1)
                .maybeSingle();
              return {
                ...ing,
                catalogMatch: ingMatch
                  ? { isFromCatalog: true, foodItem: mapFoodItem(ingMatch) }
                  : null,
              };
            })
          );
        }

        return {
          name: food.name,
          quantity: food.quantity ?? 1,
          unit: food.unit ?? "serving",
          confidence: food.confidence ?? 0.5,
          isRecipe: food.is_recipe ?? false,
          ingredients: resolvedIngredients,
          catalogMatch: match
            ? { isFromCatalog: true, foodItem: mapFoodItem(match) }
            : null,
          // Catalog match overrides clarification — user's own data is trusted
          needsClarification: match ? false : (food.needs_clarification ?? false),
          clarificationHint: match ? null : (food.clarification_hint ?? null),
        };
      })
    );

    return new Response(JSON.stringify({ foods: resolvedFoods }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("parse-food error:", err);
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

/**
 * Maps a raw Supabase food_items row (snake_case) to the FoodItem domain type (camelCase).
 * Supabase returns column names as-is from Postgres — callers expect camelCase keys.
 */
// deno-lint-ignore no-explicit-any
function mapFoodItem(row: any) {
  return {
    id: row.id,
    catalogId: row.catalog_id,
    name: row.name,
    brand: row.brand ?? null,
    baseServingG: row.base_serving_g ?? 100,
    baseCalories: row.base_calories ?? 0,
    baseProtein: row.base_protein ?? 0,
    baseCarbs: row.base_carbs ?? 0,
    baseFat: row.base_fat ?? 0,
    externalApiId: row.external_api_id ?? null,
    lastModifiedAt: row.last_modified_at ?? 0,
    deletedAt: row.deleted_at ?? null,
  };
}
