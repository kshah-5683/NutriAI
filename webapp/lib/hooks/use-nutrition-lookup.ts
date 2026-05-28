"use client";

import { useCallback } from "react";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import type { NutritionInfo } from "@/lib/types/ai";

/**
 * Hook for progressive per-item nutrition lookups with optional brand support.
 *
 * Fires an independent Edge Function call per food item (not batched).
 * Each resolves its own loading/found/not-found state in the Zustand store.
 * This preserves the progressive UX from Android where each badge
 * flips independently as lookups complete.
 *
 * When a brand is provided, the Edge Function attempts a brand-specific
 * FDC Branded lookup first, then falls back to generic all-types search.
 * The response includes `matchType` ("branded" | "generic" | null).
 */
export function useNutritionLookup() {
  const supabase = useSupabase();
  const setNutritionResult = useLogFormStore((s) => s.setNutritionResult);
  const setNutritionLoading = useLogFormStore((s) => s.setNutritionLoading);

  /**
   * Look up nutrition for a single food name, optionally with a brand.
   * @param foodName The food name to search for
   * @param brand Optional brand name for brand-specific FDC lookup
   */
  const lookupNutrition = useCallback(
    async (foodName: string, brand?: string) => {
      setNutritionLoading(foodName, true);

      try {
        const body: { foodNames: string[]; brands?: (string | null)[] } = {
          foodNames: [foodName],
        };
        if (brand) {
          body.brands = [brand];
        }

        const { data, error } = await supabase.functions.invoke(
          EDGE_FUNCTIONS.LOOKUP_NUTRITION,
          { body }
        );

        if (error) {
          console.warn(`Nutrition lookup failed for "${foodName}":`, error);
          setNutritionResult(foodName, null);
          return;
        }

        const result = data?.results?.[0];
        const nutrition: NutritionInfo | null = result?.nutrition ?? null;
        setNutritionResult(foodName, nutrition);
      } catch (err) {
        console.warn(`Nutrition lookup error for "${foodName}":`, err);
        setNutritionResult(foodName, null);
      }
    },
    [supabase, setNutritionResult, setNutritionLoading]
  );

  /**
   * Fires parallel lookups for all food names that don't have a catalog match
   * AND don't need clarification (needsClarification items are paused until
   * the user resolves the ambiguity).
   */
  const lookupAll = useCallback(
    (
      foods: Array<{
        name: string;
        catalogMatch: { isFromCatalog: boolean } | null;
        needsClarification?: boolean;
      }>
    ) => {
      for (const food of foods) {
        if (!food.catalogMatch?.isFromCatalog && !food.needsClarification) {
          lookupNutrition(food.name);
        }
      }
    },
    [lookupNutrition]
  );

  return { lookupNutrition, lookupAll };
}
