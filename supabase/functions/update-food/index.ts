/**
 * update-food Edge Function — Edit catalog item.
 * Port of UpdateFoodUseCase + CatalogViewModel.saveEditedFood().
 *
 * The Postgres trigger `trg_food_items_recalculate` automatically
 * recalculates all daily_logs referencing this food item when base
 * macros change — no application-level recalculation needed.
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { foodItemId, name, brand, servingG, calories, protein, carbs, fat } =
      await req.json();

    // Validation
    if (!name?.trim()) {
      return new Response(
        JSON.stringify({ error: "Food name cannot be blank" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }
    if (!servingG || servingG <= 0) {
      return new Response(
        JSON.stringify({ error: "Serving size must be positive" }),
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

    const { data: { user } } = await supabase.auth.getUser();
    if (!user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const now = Date.now();

    const { error } = await supabase
      .from("food_items")
      .update({
        name: name.trim(),
        brand: brand?.trim() || null,
        base_serving_g: servingG,
        base_calories: calories ?? 0,
        base_protein: protein ?? 0,
        base_carbs: carbs ?? 0,
        base_fat: fat ?? 0,
        last_modified_at: now,
        is_synced: true,
      })
      .eq("id", foodItemId)
      .is("deleted_at", null);

    if (error) {
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ success: true }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("update-food error:", err);
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
