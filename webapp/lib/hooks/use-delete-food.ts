"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { nowMs } from "@/lib/utils/constants";

/**
 * TanStack Query mutation — soft-deletes a catalog food item.
 *
 * CRITICAL PARITY RULES:
 * 1. Sets deleted_at AND last_modified_at so LWW resolves correctly with Android
 * 2. Sets is_synced = true (web writes go directly to Supabase)
 * 3. Never hard-deletes — let pg_cron handle tombstone purge after 15 days
 *
 * Port of Android's DeleteFoodUseCase.
 */
export function useDeleteFood() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (foodItemId: string) => {
      const now = nowMs();

      const { error } = await supabase
        .from("food_items")
        .update({
          deleted_at: now,
          last_modified_at: now,
          is_synced: true,
        })
        .eq("id", foodItemId)
        .is("deleted_at", null);

      if (error) throw error;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalog-items"] });
    },
  });
}
