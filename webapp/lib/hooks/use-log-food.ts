"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { triggerPrefetch } from "@/lib/utils/prefetch-trigger";
import type { LogFoodRequest } from "@/lib/types/ai";

/**
 * TanStack Query mutation — calls the log-food Edge Function.
 * On success: invalidates daily-logs, navigates home, resets form.
 */
export function useLogFood() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();
  const router = useRouter();
  const resetForm = useLogFormStore((s) => s.resetForm);

  return useMutation({
    mutationFn: async (request: LogFoodRequest) => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.LOG_FOOD,
        {
          body: {
            foodName: request.foodName,
            brand: request.brand ?? null,
            servingG: request.baseServingG,
            calories: request.baseCalories,
            protein: request.baseProtein,
            carbs: request.baseCarbs,
            fat: request.baseFat,
            quantity: request.consumedQty,
            unit: request.consumedUnit,
            dateTimestamp: request.dateTimestamp,
            catalogId: request.catalogId ?? null,
            externalApiId: request.externalApiId ?? null,
            existingFoodItemId: request.existingFoodItemId ?? null,
            skipDailyLog: request.skipDailyLog ?? false,
            mealType: request.mealType ?? null,
          },
        }
      );

      if (error) throw new Error(error.message ?? "Failed to log food");
      if (data?.error) throw new Error(data.error);

      return data as { foodItemId: string };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["daily-logs"] });
      queryClient.invalidateQueries({ queryKey: ["recommendation-cache"] });
      triggerPrefetch(supabase);
      resetForm();
      router.push("/");
    },
  });
}
