/**
 * update-daily-log Edge Function — Edit log entry.
 * Port of UpdateDailyLogUseCase + HomeViewModel.saveEditedLog().
 *
 * Updates an existing daily log's quantity, unit, and macro totals.
 * The user directly edits the total macro values (not recalculated
 * from base macros), matching Android's EditLogSheet behavior.
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { logId, quantity, unit, calories, protein, carbs, fat, mealType } =
      await req.json();

    // Validation
    if (!quantity || quantity <= 0) {
      return new Response(
        JSON.stringify({ error: "Quantity must be positive" }),
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

    // Build update payload — only include mealType if explicitly provided
    // (undefined means the caller didn't send it, null means clear it)
    const updatePayload: Record<string, unknown> = {
      consumed_qty: quantity,
      consumed_unit: unit?.trim() || "serving",
      total_calories: calories ?? 0,
      total_protein: protein ?? 0,
      total_carbs: carbs ?? 0,
      total_fat: fat ?? 0,
      last_modified_at: now,
      is_synced: true,
    };
    if (mealType !== undefined) {
      updatePayload.meal_type = mealType;
    }

    const { error } = await supabase
      .from("daily_logs")
      .update(updatePayload)
      .eq("id", logId)
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
    console.error("update-daily-log error:", err);
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
