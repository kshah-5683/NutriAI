"use client";

import { useCallback } from "react";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import type { NutritionInfo } from "@/lib/types/ai";

/**
 * Hook for progressive per-item nutrition lookups.
 *
 * Fires an independent Edge Function call per food item (not batched).
 * Each resolves its own loading/found/not-found state in the Zustand store.
 * This preserves the progressive UX from Android where each badge
 * flips independently as lookups complete.
 */
export function useNutritionLookup() {
  const supabase = useSupabase();
  const setNutritionResult = useLogFormStore((s) => s.setNutritionResult);
  const setNutritionLoading = useLogFormStore((s) => s.setNutritionLoading);

  const lookupNutrition = useCallback(
    async (foodName: string) => {
      setNutritionLoading(foodName, true);

      try {
        const { data, error } = await supabase.functions.invoke(
          EDGE_FUNCTIONS.LOOKUP_NUTRITION,
          { body: { foodNames: [foodName] } }
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
   * Fires parallel lookups for all food names that don't have a catalog match.
   */
  const lookupAll = useCallback(
    (
      foods: Array<{
        name: string;
        catalogMatch: { isFromCatalog: boolean } | null;
      }>
    ) => {
      for (const food of foods) {
        if (!food.catalogMatch?.isFromCatalog) {
          lookupNutrition(food.name);
        }
      }
    },
    [lookupNutrition]
  );

  return { lookupNutrition, lookupAll };
}
