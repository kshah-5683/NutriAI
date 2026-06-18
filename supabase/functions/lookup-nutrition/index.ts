/**
 * lookup-nutrition Edge Function — Tiered nutrition grounding with brand awareness.
 * Port of NutritionRepositoryImpl (USDA FDC + IFCT fallback).
 *
 * Tiers (when brand IS provided):
 *   1a. USDA FDC Branded — query = "{brand} {name}", dataType = Branded
 *   1b. USDA FDC All types — query = "{name}", dataType = Foundation,SR Legacy,Branded
 *   2.  Supabase ifct_foods table — ILIKE search + word-by-word fallback
 *   3.  null — user fills manually
 *
 * Tiers (when brand is NOT provided — original behavior):
 *   1. USDA FDC All types — rank by macro completeness via macroScore()
 *   2. Supabase ifct_foods — ILIKE search + word-by-word fallback
 *   3. null — user fills manually
 *
 * Response includes `matchType` per item:
 *   - "branded" — exact brand match from FDC Branded database
 *   - "generic" — generic match from FDC Foundation/SR Legacy or IFCT
 *   - null — no match found
 */
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import {
  mapFdcToNutritionInfo,
  macroScore,
} from "../_shared/fdc-mapper.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

/**
 * Searches USDA FDC with the given query and dataType filter.
 * Returns the top-ranked NutritionInfo or null if no usable results.
 */
async function searchFdc(
  query: string,
  dataType: string,
  fdcApiKey: string
): Promise<ReturnType<typeof mapFdcToNutritionInfo> | null> {
  try {
    const fdcRes = await fetch(
      `https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(query)}&api_key=${fdcApiKey}&dataType=${encodeURIComponent(dataType)}&pageSize=5`
    );
    if (!fdcRes.ok) return null;

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
        ) => {
          const queryLower = query.toLowerCase().trim();
          
          // Priority 1: Exact matches first (case-insensitive)
          const aExact = a.productName.toLowerCase().trim() === queryLower;
          const bExact = b.productName.toLowerCase().trim() === queryLower;
          if (aExact !== bExact) return aExact ? -1 : 1;

          // Priority 2: Starts-with matches next
          const aStarts = a.productName.toLowerCase().trim().startsWith(queryLower);
          const bStarts = b.productName.toLowerCase().trim().startsWith(queryLower);
          if (aStarts !== bStarts) return aStarts ? -1 : 1;

          // Priority 3: Macro completeness score (descending)
          return macroScore(b) - macroScore(a);
        }
      );

    return ranked.length > 0 ? ranked[0] : null;
  } catch (err) {
    console.warn(`FDC lookup failed for "${query}":`, err);
    return null;
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  try {
    const { foodNames, brands } = await req.json();

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
      foodNames.map(async (name: string, i: number) => {
        const brand: string | null = Array.isArray(brands) ? (brands[i] ?? null) : null;

        // ── Tier 1a: FDC Branded (only when brand is provided) ──────
        if (brand && fdcApiKey) {
          const brandedResult = await searchFdc(
            `${brand} ${name}`,
            "Branded",
            fdcApiKey
          );
          if (brandedResult) {
            // Validate the FDC result actually belongs to the requested brand.
            // FDC search is keyword-based and may return a different brand
            // (e.g. "Amul cheese" → "Food Lion cheese" if Amul isn't in FDC).
            const resultBrand = (brandedResult.brand ?? "").toLowerCase();
            const requestedBrand = brand.toLowerCase().trim();
            const brandMatches =
              resultBrand.includes(requestedBrand) ||
              requestedBrand
                .split(/\s+/)
                .some((word) => word.length > 2 && resultBrand.includes(word));

            if (brandMatches) {
              return {
                name,
                nutrition: { ...brandedResult, matchType: "branded" as const },
                source: "USDA FDC",
                matchType: "branded" as const,
              };
            }
            // Brand mismatch — fall through to generic tier.
            // UI will show "Brand not found, using generic" via matchType + clarification type.
            console.warn(
              `FDC Branded result brand "${brandedResult.brand}" does not match requested "${brand}" — skipping to generic`
            );
          }
        }

        // ── Tier 1b: FDC All types (generic) ────────────────────────
        if (fdcApiKey) {
          const genericResult = await searchFdc(
            name,
            "Foundation,SR Legacy,Branded",
            fdcApiKey
          );
          if (genericResult) {
            return {
              name,
              nutrition: { ...genericResult, matchType: "generic" as const },
              source: "USDA FDC",
              matchType: "generic" as const,
            };
          }
        }

        // ── Tier 2: IFCT 2017 (Supabase table) — full phrase ────────
        const { data: ifctRows } = await supabase
          .from("ifct_foods")
          .select("*")
          .ilike("name", `%${name}%`)
          .limit(5);

        if (ifctRows?.length) {
          return {
            name,
            nutrition: { ...mapIfctToNutritionInfo(ifctRows[0]), matchType: "generic" as const },
            source: "IFCT 2017",
            matchType: "generic" as const,
          };
        }

        // ── Tier 3: IFCT word-by-word fallback ──────────────────────
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
              nutrition: { ...mapIfctToNutritionInfo(wordRows[0]), matchType: "generic" as const },
              source: "IFCT 2017",
              matchType: "generic" as const,
            };
          }
        }

        // ── Tier 4: Nothing found ───────────────────────────────────
        return { name, nutrition: null, source: null, matchType: null };
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
