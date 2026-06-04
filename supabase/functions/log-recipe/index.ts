/**
 * log-recipe Edge Function — Ingredient aggregation + recipe logging.
 * Port of LogFoodUseCase.logRecipe().
 *
 * Three-case ingredient handling:
 *   Case 1: Catalog hit — accumulate macros, don't insert
 *   Case 2: Nutrition-enriched but not in catalog — insert + accumulate
 *   Case 3: No data — 0-macro placeholder in ingredients catalog
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { PER_100G_BASE, computeServingMultiplier, isGramUnit } from "../_shared/macro-calculator.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { recipeName, ingredientMatches, quantity, unit, dateTimestamp, skipDailyLog, mealType } =
      await req.json();

    // Validation
    if (!recipeName?.trim()) {
      return new Response(
        JSON.stringify({ error: "Recipe name cannot be blank" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }
    if (!quantity || quantity <= 0) {
      return new Response(
        JSON.stringify({ error: "Quantity must be greater than zero" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // Authenticated client — user's JWT for both auth and DB operations (respects RLS)
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      {
        global: {
          headers: { Authorization: req.headers.get("Authorization")! },
        },
      }
    );

    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }
    const userId = user.id;
    const now = Date.now();

    const ingredientCatalogId = `${userId}_local_user_ingredients`;
    const recipeCatalogId = `${userId}_local_user_recipes`;

    // Ensure both catalogs exist
    await ensureCatalog(supabase, ingredientCatalogId, userId, "My Ingredients", now);
    await ensureCatalog(supabase, recipeCatalogId, userId, "My Recipes", now);

    // Aggregate macros from all ingredients
    let totalCalories = 0;
    let totalProtein = 0;
    let totalCarbs = 0;
    let totalFat = 0;
    let totalServingG = 0;

    for (const match of ingredientMatches ?? []) {
      const cachedFood = match.matchedFoodItem ?? match.foodItem ?? null;
      const isFromCatalog = match.isFromCatalog ?? false;

      if (isFromCatalog && cachedFood) {
        // Case 1: Catalog hit — item already in DB, just accumulate macros
        totalCalories += cachedFood.base_calories ?? cachedFood.baseCalories ?? 0;
        totalProtein += cachedFood.base_protein ?? cachedFood.baseProtein ?? 0;
        totalCarbs += cachedFood.base_carbs ?? cachedFood.baseCarbs ?? 0;
        totalFat += cachedFood.base_fat ?? cachedFood.baseFat ?? 0;
        totalServingG += cachedFood.base_serving_g ?? cachedFood.baseServingG ?? 0;
      } else if (!isFromCatalog && cachedFood) {
        // Case 2: Nutrition-enriched but not in catalog — insert + accumulate
        const ingId = cachedFood.id ?? crypto.randomUUID();
        await supabase.from("food_items").insert({
          id: ingId,
          catalog_id: ingredientCatalogId,
          name: (cachedFood.name ?? match.parsedName ?? "Unknown").trim(),
          brand: cachedFood.brand ?? null,
          base_serving_g: cachedFood.base_serving_g ?? cachedFood.baseServingG ?? PER_100G_BASE,
          base_calories: cachedFood.base_calories ?? cachedFood.baseCalories ?? 0,
          base_protein: cachedFood.base_protein ?? cachedFood.baseProtein ?? 0,
          base_carbs: cachedFood.base_carbs ?? cachedFood.baseCarbs ?? 0,
          base_fat: cachedFood.base_fat ?? cachedFood.baseFat ?? 0,
          external_api_id: cachedFood.external_api_id ?? cachedFood.externalApiId ?? null,
          last_modified_at: now,
          is_synced: true,
          deleted_at: null,
        });
        totalCalories += cachedFood.base_calories ?? cachedFood.baseCalories ?? 0;
        totalProtein += cachedFood.base_protein ?? cachedFood.baseProtein ?? 0;
        totalCarbs += cachedFood.base_carbs ?? cachedFood.baseCarbs ?? 0;
        totalFat += cachedFood.base_fat ?? cachedFood.baseFat ?? 0;
        totalServingG += cachedFood.base_serving_g ?? cachedFood.baseServingG ?? PER_100G_BASE;
      } else {
        // Case 3: No data — 0-macro placeholder in the Ingredients catalog
        await supabase.from("food_items").insert({
          id: crypto.randomUUID(),
          catalog_id: ingredientCatalogId,
          name: (match.parsedName ?? match.name ?? "Unknown ingredient").trim(),
          base_serving_g: PER_100G_BASE,
          base_calories: 0,
          base_protein: 0,
          base_carbs: 0,
          base_fat: 0,
          last_modified_at: now,
          is_synced: true,
          deleted_at: null,
        });
        totalServingG += PER_100G_BASE;
      }
    }

    // Create the recipe FoodItem in the Recipes catalog
    // Normalize aggregated totals to per-100g before storing.
    const recipeServingG = totalServingG > 0 ? totalServingG : PER_100G_BASE;
    const normFactor = PER_100G_BASE / recipeServingG;
    const normCalories = totalCalories * normFactor;
    const normProtein  = totalProtein  * normFactor;
    const normCarbs    = totalCarbs    * normFactor;
    const normFat      = totalFat      * normFactor;

    const recipeItemId = crypto.randomUUID();
    const { error: recipeError } = await supabase.from("food_items").insert({
      id: recipeItemId,
      catalog_id: recipeCatalogId,
      name: recipeName.trim(),
      base_serving_g: recipeServingG,
      base_calories: normCalories,
      base_protein: normProtein,
      base_carbs: normCarbs,
      base_fat: normFat,
      last_modified_at: now,
      is_synced: true,
      deleted_at: null,
    });

    if (recipeError) {
      throw new Error(`Recipe food_items insert failed: ${recipeError.message}`);
    }

    // Create the daily log entry
    if (!skipDailyLog) {
      const normalizedUnit = (unit?.trim() || "serving").toLowerCase();
      const scaleFactor = computeServingMultiplier(quantity, normalizedUnit, recipeServingG);

      // For gram/ml units: store as 100g-relative multiplier (200g → 2.0)
      const storedQty = isGramUnit(normalizedUnit) ? quantity / PER_100G_BASE : quantity;

      const { error: logError } = await supabase.from("daily_logs").insert({
        id: crypto.randomUUID(),
        user_id: userId,
        food_item_id: recipeItemId,
        food_name: recipeName.trim(),
        date_timestamp: dateTimestamp,
        consumed_qty: storedQty,
        consumed_unit: unit?.trim() || "serving",
        total_calories: normCalories * scaleFactor,
        total_protein: normProtein * scaleFactor,
        total_carbs: normCarbs * scaleFactor,
        total_fat: normFat * scaleFactor,
        meal_type: mealType ?? null,
        last_modified_at: now,
        is_synced: true,
        deleted_at: null,
      });

      if (logError) {
        throw new Error(`daily_logs insert failed: ${logError.message}`);
      }
    }

    return new Response(JSON.stringify({ recipeItemId }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("log-recipe error:", err);
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

// deno-lint-ignore no-explicit-any
async function ensureCatalog(supabase: any, catalogId: string, userId: string, name: string, now: number) {
  const { data } = await supabase
    .from("catalogs")
    .select("id")
    .eq("id", catalogId)
    .maybeSingle();

  if (!data) {
    const { error } = await supabase.from("catalogs").insert({
      id: catalogId,
      user_id: userId,
      name,
      last_modified_at: now,
      is_synced: true,
    });
    if (error) {
      console.error("ensureCatalog failed:", error);
      throw new Error(`Catalog creation failed (${name}): ${error.message}`);
    }
  }
}
