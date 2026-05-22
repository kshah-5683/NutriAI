"use client";

import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { DEFAULT_MACRO_GOALS } from "@/lib/utils/constants";
import type { MacroGoals } from "@/lib/types/domain";

/**
 * TanStack Query hook — fetches user macro goals from user_preferences table.
 * Falls back to DEFAULT_MACRO_GOALS if no row exists for the user.
 *
 * RLS on user_preferences filters by auth.uid() automatically.
 */
export function useMacroGoals() {
  const supabase = useSupabase();

  return useQuery<MacroGoals>({
    queryKey: ["macro-goals"],
    queryFn: async () => {
      const { data, error } = await supabase
        .from("user_preferences")
        .select("calorie_goal, protein_goal, carbs_goal, fat_goal")
        .maybeSingle();

      // If table doesn't exist or RLS blocks access, fall back to defaults.
      // This lets the app work even before migration 006 is applied.
      if (error) {
        console.warn("[useMacroGoals] Query failed, using defaults:", error.message);
        return DEFAULT_MACRO_GOALS;
      }

      if (!data) {
        return DEFAULT_MACRO_GOALS;
      }

      return {
        calorieGoal: data.calorie_goal,
        proteinGoal: data.protein_goal,
        carbsGoal: data.carbs_goal,
        fatGoal: data.fat_goal,
      };
    },
    // Goals change rarely — cache for 5 minutes
    staleTime: 5 * 60_000,
  });
}
