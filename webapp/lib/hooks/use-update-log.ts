"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { triggerPrefetch } from "@/lib/utils/prefetch-trigger";
import type { MealType } from "@/lib/types/domain";

interface UpdateLogParams {
  logId: string;
  quantity: number;
  unit: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  /** Meal category — only sent when the user changes it in the edit sheet */
  mealType?: MealType | null;
}

/**
 * TanStack Query mutation — updates a daily log entry via Edge Function.
 *
 * Phase W3: Migrated from direct Supabase .update() to the update-daily-log
 * Edge Function for consistency with all other write operations.
 */
export function useUpdateLog() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      logId,
      quantity,
      unit,
      calories,
      protein,
      carbs,
      fat,
      mealType,
    }: UpdateLogParams) => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.UPDATE_DAILY_LOG,
        {
          body: { logId, quantity, unit, calories, protein, carbs, fat, mealType },
        }
      );

      if (error) throw new Error(error.message ?? "Failed to update log");
      if (data?.error) throw new Error(data.error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["daily-logs"] });
      queryClient.invalidateQueries({ queryKey: ["recommendation-cache"] });
      triggerPrefetch(supabase);
    },
  });
}
