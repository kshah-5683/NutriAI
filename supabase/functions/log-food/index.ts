/**
 * log-food Edge Function — Canonical macro scaling + catalog auto-creation.
 * Port of LogFoodUseCase.invoke().
 *
 * CRITICAL: All base macros are per 100g.
 *   scaleFactor = computeServingMultiplier(quantity, unit)
 *   total = base_macro_per_100g * scaleFactor
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { computeServingMultiplier } from "../_shared/macro-calculator.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const {
      foodName,
      brand,
      servingG,
      calories,
      protein,
      carbs,
      fat,
      quantity,
      unit,
      dateTimestamp,
      catalogId,
      externalApiId,
      existingFoodItemId,
      skipDailyLog,
    } = await req.json();

    // Validation
    if (!foodName?.trim()) {
      return new Response(
        JSON.stringify({ error: "Food name cannot be blank" }),
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
    if (!servingG || servingG <= 0) {
      return new Response(
        JSON.stringify({ error: "Serving size must be greater than zero" }),
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

    // Auto-create catalog if it doesn't exist (prevents FK violation for new web users).
    const resolvedCatalogId =
      catalogId || `${userId}_local_user_ingredients`;

    const { data: existingCatalog } = await supabase
      .from("catalogs")
      .select("id")
      .eq("id", resolvedCatalogId)
      .maybeSingle();

    if (!existingCatalog) {
      const isRecipe = resolvedCatalogId.endsWith("_local_user_recipes");
      const { error: catalogError } = await supabase.from("catalogs").insert({
        id: resolvedCatalogId,
        user_id: userId,
        name: isRecipe ? "My Recipes" : "My Ingredients",
        last_modified_at: now,
        is_synced: true,
      });
      if (catalogError) {
        console.error("Catalog creation failed:", catalogError);
        throw new Error(`Catalog creation failed: ${catalogError.message}`);
      }
    }

    let foodItemId: string;

    // Check for existing catalog item (reuse, don't duplicate)
    if (existingFoodItemId) {
      const { data: existing } = await supabase
        .from("food_items")
        .select("id")
        .eq("id", existingFoodItemId)
        .is("deleted_at", null)
        .maybeSingle();

      if (existing) {
        foodItemId = existing.id;
      } else {
        // Item was purged between parse and log — create new
        foodItemId = crypto.randomUUID();
        await insertFoodItem(supabase, {
          foodItemId,
          catalogId: resolvedCatalogId,
          userId,
          foodName,
          brand,
          servingG,
          calories,
          protein,
          carbs,
          fat,
          externalApiId,
          now,
        });
      }
    } else {
      foodItemId = crypto.randomUUID();
      await insertFoodItem(supabase, {
        foodItemId,
        catalogId: resolvedCatalogId,
        userId,
        foodName,
        brand,
        servingG,
        calories,
        protein,
        carbs,
        fat,
        externalApiId,
        now,
      });
    }

    if (!skipDailyLog) {
      // CRITICAL: All base macros are per 100g.
      // computeServingMultiplier converts quantity+unit into a 100g-relative multiplier.
      // e.g., "200g" → 2.0, "2 cups" → 4.8, "1 tbsp" → 0.15, "2 servings" → 2.0
      const normalizedUnit = (unit?.trim() || "serving").toLowerCase();
      const scaleFactor = computeServingMultiplier(quantity, normalizedUnit);

      // Android parity: consumed_qty for gram/ml units is stored as a multiplier
      // (200g → 2.0) so that toDisplayQty(2.0, "g") = 200 on all clients.
      const isGramUnit = ["g", "gram", "grams", "ml", "milliliter"].includes(normalizedUnit);
      const storedQty = isGramUnit ? quantity / 100 : quantity;

      const { error: logError } = await supabase.from("daily_logs").insert({
        id: crypto.randomUUID(),
        user_id: userId,
        food_item_id: foodItemId,
        food_name: foodName.trim(),
        date_timestamp: dateTimestamp,
        consumed_qty: storedQty,
        consumed_unit: unit?.trim() || "serving",
        total_calories: calories * scaleFactor,
        total_protein: protein * scaleFactor,
        total_carbs: carbs * scaleFactor,
        total_fat: fat * scaleFactor,
        last_modified_at: now,
        is_synced: true,
        deleted_at: null,
      });

      if (logError) {
        console.error("daily_logs insert error:", logError);
        return new Response(JSON.stringify({ error: logError.message }), {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    return new Response(JSON.stringify({ foodItemId }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("log-food error:", err);
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
async function insertFoodItem(supabase: any, params: any) {
  const { error } = await supabase.from("food_items").insert({
    id: params.foodItemId,
    catalog_id: params.catalogId,
    name: params.foodName.trim(),
    brand: params.brand?.trim() || null,
    base_serving_g: params.servingG,
    base_calories: params.calories ?? 0,
    base_protein: params.protein ?? 0,
    base_carbs: params.carbs ?? 0,
    base_fat: params.fat ?? 0,
    external_api_id: params.externalApiId ?? null,
    last_modified_at: params.now,
    is_synced: true,
    deleted_at: null,
  });

  if (error) {
    throw new Error(`food_items insert failed: ${error.message}`);
  }
}
