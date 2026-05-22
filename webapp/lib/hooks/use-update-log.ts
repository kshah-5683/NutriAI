"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";

interface UpdateLogParams {
  logId: string;
  quantity: number;
  unit: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
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
    }: UpdateLogParams) => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.UPDATE_DAILY_LOG,
        {
          body: { logId, quantity, unit, calories, protein, carbs, fat },
        }
      );

      if (error) throw new Error(error.message ?? "Failed to update log");
      if (data?.error) throw new Error(data.error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["daily-logs"] });
    },
  });
}
