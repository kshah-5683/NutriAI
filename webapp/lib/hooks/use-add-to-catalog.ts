"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { getRecipeCatalogId, nowMs } from "@/lib/utils/constants";
import type { Recommendation } from "@/lib/types/recommendation";

/**
 * TanStack Query mutation — adds an internet recommendation to the user's
 * recipes catalog as a new food item.
 *
 * CRITICAL — per-serving conversion: The recommendation's macros are
 * `total for suggested_quantity servings`. When adding to the catalog,
 * we store per-serving macros: rec.calories / rec.suggested_quantity.
 * This matches the catalog convention (base_calories = macros per base_serving_g).
 *
 * Uses direct Supabase insert (NOT the log-food Edge Function, which has
 * its own macro scaling logic and would be a contract misuse here).
 */
export function useAddToCatalog() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (rec: Recommendation) => {
      const {
        data: { user },
      } = await supabase.auth.getUser();
      if (!user) throw new Error("Not authenticated");

      const tempId = crypto.randomUUID();
      const catalogId = getRecipeCatalogId(user.id);
      const qty = rec.suggested_quantity || 1;

      const { error } = await supabase.from("food_items").insert({
        id: tempId,
        catalog_id: catalogId,
        name: rec.name,
        base_serving_g: 100, // Default — editable later
        base_calories: rec.calories / qty,
        base_protein: rec.protein / qty,
        base_carbs: rec.carbs / qty,
        base_fat: rec.fat / qty,
        is_synced: true,
        last_modified_at: nowMs(),
      });

      if (error) throw error;
      return tempId;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalog-items"] });
    },
  });
}
