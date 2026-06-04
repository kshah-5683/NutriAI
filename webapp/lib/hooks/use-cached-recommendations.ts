"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useDailyLogs } from "@/lib/hooks/use-daily-logs";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { useUserProfile } from "@/lib/hooks/use-user-profile";
import { useDateStore } from "@/lib/stores/date-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { computeDailyTotals } from "@/lib/utils/compute-daily-totals";
import { toLocalDateString, startOfDayMs } from "@/lib/utils/format";
import {
  determineNextMealSlot,
  deriveMissedMeals,
} from "@/lib/utils/meal-progression";
import type { MealType } from "@/lib/types/domain";
import type { Recommendation } from "@/lib/types/recommendation";

/**
 * Checks if a Date object represents today in local time.
 */
function isToday(date: Date): boolean {
  return toLocalDateString(date) === toLocalDateString(new Date());
}

/**
 * TanStack Query hook — reads meal recommendations from the
 * `recommendation_cache` table for instant display on the home screen.
 *
 * Primary path: reads from cache (instant, no AI call).
 * Cold-start fallback: when no cache exists, fires prefetch in background
 * AND falls back to a live `recommend-meals` Edge Function call so the user
 * sees recommendations on first visit.
 *
 * Also derives `nextMeal` and `missedMeals` for the UI.
 *
 * Replaces `useRecommendations()` for the home screen.
 */
export function useCachedRecommendations() {
  const supabase = useSupabase();
  const selectedDate = useDateStore((s) => s.selectedDate);
  const { data: logs = [] } = useDailyLogs();
  const { data: goals } = useMacroGoals();
  const { data: profile } = useUserProfile();

  const todaySelected = isToday(selectedDate);
  const includeInternet = profile?.recommendationsEnabled ?? false;

  // Derive logged meal types from today's logs
  const loggedMeals = useMemo(
    () =>
      logs
        .map((l) => l.mealType)
        .filter((mt): mt is MealType => mt !== null),
    [logs]
  );

  const hour = new Date().getHours();
  const nextMeal = determineNextMealSlot(loggedMeals, hour);
  const missedMeals = deriveMissedMeals(loggedMeals, hour);

  // Compute remaining macros for live fallback
  const totals = computeDailyTotals(logs, selectedDate);
  const safeGoals = goals ?? {
    calorieGoal: 2000,
    proteinGoal: 150,
    carbsGoal: 250,
    fatGoal: 65,
  };
  const remainingMacros = {
    calories: safeGoals.calorieGoal - totals.totalCalories,
    protein: safeGoals.proteinGoal - totals.totalProtein,
    carbs: safeGoals.carbsGoal - totals.totalCarbs,
    fat: safeGoals.fatGoal - totals.totalFat,
  };

  const query = useQuery<Recommendation[]>({
    queryKey: [
      "recommendation-cache",
      toLocalDateString(selectedDate),
      nextMeal,
      includeInternet,
    ],
    queryFn: async () => {
      if (!nextMeal) return [];

      const dayMs = startOfDayMs(selectedDate);

      // Try reading from cache first (instant path)
      const { data: cached } = await supabase
        .from("recommendation_cache")
        .select("recommendations")
        .eq("meal_type", nextMeal)
        .eq("date_timestamp", dayMs)
        .maybeSingle();

      if (cached?.recommendations) {
        return cached.recommendations as Recommendation[];
      }

      // Cold start — no cache yet.
      // Fall back to live Edge Function call, then write result to cache
      // so the next page load is instant. The background prefetch is unreliable
      // (60s Gemma call may not complete before user refreshes, or the request
      // may be cancelled by browser navigation).
      const { data: live, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.RECOMMEND_MEALS,
        {
          body: {
            mode: "time_based",
            timeOfDay: nextMeal,
            remainingMacros,
            includeInternet,
            targetMeal: nextMeal,
          },
        }
      );

      if (error) throw new Error(error.message ?? "Failed to get recommendations");
      if (live?.error) throw new Error(live.error);

      const recs = (live?.recommendations ?? []) as Recommendation[];

      // Write the live result to recommendation_cache so subsequent loads
      // are instant (cache hit). Fire-and-forget — don't block the UI.
      if (recs.length > 0) {
        supabase
          .from("recommendation_cache")
          .upsert(
            {
              user_id: (await supabase.auth.getUser()).data.user!.id,
              meal_type: nextMeal,
              date_timestamp: dayMs,
              recommendations: recs,
              remaining_macros: remainingMacros,
            },
            { onConflict: "user_id,meal_type,date_timestamp" }
          )
          .then(({ error: cacheErr }) => {
            if (cacheErr) console.warn("Cache write failed:", cacheErr.message);
          });
      }

      return recs;
    },
    enabled: todaySelected && nextMeal !== null,
    staleTime: 5 * 60_000, // 5 min — matches server cooldown
    gcTime: 60 * 60_000, // 1 hour garbage collection
    retry: 1,
    refetchOnWindowFocus: false,
  });

  return { ...query, nextMeal, missedMeals };
}
