"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { triggerPrefetch } from "@/lib/utils/prefetch-trigger";
import type { LogRecipeRequest } from "@/lib/types/ai";

export type { LogRecipeRequest };

/**
 * TanStack Query mutation — calls the log-recipe Edge Function.
 * On success: invalidates daily-logs, navigates home, resets form.
 */
export function useLogRecipe() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();
  const router = useRouter();
  const resetForm = useLogFormStore((s) => s.resetForm);

  return useMutation({
    mutationFn: async (request: LogRecipeRequest) => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.LOG_RECIPE,
        { body: request }
      );

      if (error) throw new Error(error.message ?? "Failed to log recipe");
      if (data?.error) throw new Error(data.error);

      return data as { recipeItemId: string };
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
