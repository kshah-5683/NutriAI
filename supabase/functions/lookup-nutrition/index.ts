/**
 * lookup-nutrition Edge Function — Two-tier nutrition grounding.
 * Port of NutritionRepositoryImpl (USDA FDC + IFCT fallback).
 *
 * Tiers:
 *   1. USDA FDC API — rank by macro completeness via macroScore()
 *   2. Supabase ifct_foods table — ILIKE search + word-by-word fallback
 *   3. null — user fills manually
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import {
  mapFdcToNutritionInfo,
  macroScore,
} from "../_shared/fdc-mapper.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { foodNames } = await req.json();

    if (!Array.isArray(foodNames) || foodNames.length === 0) {
      return new Response(
        JSON.stringify({ error: "foodNames must be a non-empty array" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    const fdcApiKey = Deno.env.get("USDA_FDC_API_KEY") ?? "";
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      {
        global: {
          headers: { Authorization: req.headers.get("Authorization")! },
        },
      }
    );

    const results = await Promise.all(
      foodNames.map(async (name: string) => {
        // ── Tier 1: USDA FDC ──────────────────────────────────────────
        if (fdcApiKey) {
          try {
            const fdcRes = await fetch(
              `https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(name)}&api_key=${fdcApiKey}&dataType=Foundation,SR%20Legacy,Branded&pageSize=5`
            );
            if (fdcRes.ok) {
              const fdcData = await fdcRes.json();
              const ranked = (fdcData.foods ?? [])
                // deno-lint-ignore no-explicit-any
                .filter((f: any) => f.foodNutrients?.length > 0)
                // deno-lint-ignore no-explicit-any
                .map((f: any) => mapFdcToNutritionInfo(f))
                .filter(
                  (n: ReturnType<typeof mapFdcToNutritionInfo>) =>
                    n != null && n.caloriesPer100g > 0
                )
                .sort(
                  (
                    a: NonNullable<ReturnType<typeof mapFdcToNutritionInfo>>,
                    b: NonNullable<ReturnType<typeof mapFdcToNutritionInfo>>
                  ) => macroScore(b) - macroScore(a)
                );

              if (ranked.length > 0) {
                return { name, nutrition: ranked[0], source: "USDA FDC" };
              }
            }
          } catch (err) {
            // Non-fatal — fall through to IFCT
            console.warn(`FDC lookup failed for "${name}":`, err);
          }
        }

        // ── Tier 2: IFCT 2017 (Supabase table) — full phrase ─────────
        const { data: ifctRows } = await supabase
          .from("ifct_foods")
          .select("*")
          .ilike("name", `%${name}%`)
          .limit(5);

        if (ifctRows?.length) {
          return {
            name,
            nutrition: mapIfctToNutritionInfo(ifctRows[0]),
            source: "IFCT 2017",
          };
        }

        // ── Tier 3: IFCT word-by-word fallback ────────────────────────
        const words = name.split(/\s+/).filter((w: string) => w.length > 2);
        for (const word of words) {
          const { data: wordRows } = await supabase
            .from("ifct_foods")
            .select("*")
            .ilike("name", `%${word}%`)
            .limit(3);
          if (wordRows?.length) {
            return {
              name,
              nutrition: mapIfctToNutritionInfo(wordRows[0]),
              source: "IFCT 2017",
            };
          }
        }

        // ── Tier 4: Nothing found ─────────────────────────────────────
        return { name, nutrition: null, source: null };
      })
    );

    return new Response(JSON.stringify({ results }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    console.error("lookup-nutrition error:", err);
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
 * Maps an IFCT table row to NutritionInfo shape.
 * IFCT columns: code, name, energy_kcal, protein_g, fat_g, carbs_g, fiber_g
 */
// deno-lint-ignore no-explicit-any
function mapIfctToNutritionInfo(row: any) {
  return {
    productName: row.name ?? "Unknown",
    brand: null,
    caloriesPer100g: row.energy_kcal ?? 0,
    proteinPer100g: row.protein_g ?? 0,
    carbsPer100g: row.carbs_g ?? 0,
    fatPer100g: row.fat_g ?? 0,
    fiberPer100g: row.fiber_g ?? null,
    source: "IFCT 2017",
    externalId: row.code ?? null,
    servingWeightG: null, // IFCT does not report serving sizes
  };
}
