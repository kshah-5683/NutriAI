"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";

interface UpdateFoodParams {
  foodItemId: string;
  name: string;
  brand: string | null;
  servingG: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
}

/**
 * TanStack Query mutation — updates a catalog food item via the update-food Edge Function.
 *
 * The Postgres trigger `trg_food_items_recalculate` automatically recalculates
 * all daily_logs referencing this food item when base macros change.
 */
export function useUpdateFood() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (params: UpdateFoodParams) => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.UPDATE_FOOD,
        { body: params }
      );

      if (error) throw new Error(error.message ?? "Failed to update food item");
      if (data?.error) throw new Error(data.error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["catalog-items"] });
      // Also invalidate daily-logs since Postgres trigger may have recalculated them
      queryClient.invalidateQueries({ queryKey: ["daily-logs"] });
    },
  });
}
