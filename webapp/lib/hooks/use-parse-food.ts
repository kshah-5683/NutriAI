"use client";

import { useMutation } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import type { ParseFoodResponse } from "@/lib/types/ai";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";

/**
 * TanStack Query mutation — calls the parse-food Edge Function.
 * On success, updates the Zustand store with parsed foods.
 * On error, sets aiError in the store.
 */
export function useParseFood() {
  const supabase = useSupabase();
  const setParsedFoods = useLogFormStore((s) => s.setParsedFoods);
  const setAiError = useLogFormStore((s) => s.setAiError);
  const setIsParsing = useLogFormStore((s) => s.setIsParsing);

  return useMutation({
    mutationFn: async (foodDescription: string) => {
      setIsParsing(true);
      setAiError(null);

      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.PARSE_FOOD,
        { body: { foodDescription } }
      );

      if (error) throw new Error(error.message ?? "Failed to parse food");
      if (data?.error) throw new Error(data.error);

      return data as ParseFoodResponse;
    },
    onSuccess: (data) => {
      setParsedFoods(data.foods ?? []);
    },
    onError: (err: Error) => {
      setAiError(err.message);
    },
  });
}
