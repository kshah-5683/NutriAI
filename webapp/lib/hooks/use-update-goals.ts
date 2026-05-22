"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import type { MacroGoals } from "@/lib/types/domain";

/**
 * TanStack mutation — upserts macro goals into the user_preferences table.
 *
 * Web divergence from Android:
 * - Android stores goals in DataStore (local only)
 * - Web stores in Supabase user_preferences table for cross-platform sync
 *
 * The table has an `updated_at` column managed by a BEFORE UPDATE trigger
 * (set_updated_at). Do NOT include any timestamp in the upsert payload.
 *
 * On success, invalidates the "macro-goals" query so all consumers
 * (Home ring, Insights averages) pick up the new values immediately.
 */
export function useUpdateGoals() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (goals: MacroGoals) => {
      const {
        data: { user },
      } = await supabase.auth.getUser();
      if (!user) throw new Error("Not authenticated");

      const { error } = await supabase.from("user_preferences").upsert(
        {
          user_id: user.id,
          calorie_goal: goals.calorieGoal,
          protein_goal: goals.proteinGoal,
          carbs_goal: goals.carbsGoal,
          fat_goal: goals.fatGoal,
        },
        { onConflict: "user_id" }
      );

      if (error) throw error;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["macro-goals"] });
    },
  });
}
