"use client";

import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useDailyLogs } from "@/lib/hooks/use-daily-logs";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { useUserProfile } from "@/lib/hooks/use-user-profile";
import { useDateStore } from "@/lib/stores/date-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { computeDailyTotals } from "@/lib/utils/compute-daily-totals";
import { toLocalDateString } from "@/lib/utils/format";
import type { RecommendMealsResponse, TimeOfDay } from "@/lib/types/recommendation";

/**
 * Returns a time-of-day bucket from the client's local clock.
 * Returns null during late night (10pm–6am) — skip recs.
 *
 * CRITICAL: Uses new Date() which is always the client's local time.
 * This hook is "use client" — it never runs on the server. No hydration mismatch risk.
 */
function getTimeOfDay(): TimeOfDay | null {
  const hour = new Date().getHours();
  if (hour >= 6 && hour <= 10) return "morning";
  if (hour >= 11 && hour <= 14) return "afternoon";
  if (hour >= 15 && hour <= 18) return "evening";
  if (hour >= 19 && hour <= 21) return "night";
  return null; // late night — skip recs
}

/**
 * Checks if a Date object represents today in local time.
 */
function isToday(date: Date): boolean {
  return toLocalDateString(date) === toLocalDateString(new Date());
}

/**
 * TanStack Query hook — fetches time-based meal recommendations from the
 * `recommend-meals` Edge Function.
 *
 * Only fetches when:
 * - The selected date is today (recs are irrelevant for past dates)
 * - It's not late night (10pm–6am)
 *
 * Uses a 30-min staleTime to prevent refetch on every tab switch / navigation.
 *
 * Adding `includeInternet` to the queryKey ensures TanStack Query refetches
 * when the user toggles recommendations on/off in Settings.
 */
export function useRecommendations() {
  const supabase = useSupabase();
  const selectedDate = useDateStore((s) => s.selectedDate);
  const { data: logs = [] } = useDailyLogs();
  const { data: goals } = useMacroGoals();
  const { data: profile } = useUserProfile();

  const timeOfDay = getTimeOfDay();
  const todaySelected = isToday(selectedDate);
  const includeInternet = profile?.recommendationsEnabled ?? false;

  // Compute remaining macros from today's logged totals
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

  return useQuery<RecommendMealsResponse>({
    queryKey: ["recommendations", "time_based", timeOfDay, includeInternet],
    queryFn: async () => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.RECOMMEND_MEALS,
        {
          body: {
            mode: "time_based",
            timeOfDay,
            remainingMacros,
            includeInternet,
          },
        }
      );

      if (error) throw new Error(error.message ?? "Failed to get recommendations");
      if (data?.error) throw new Error(data.error);
      return data as RecommendMealsResponse;
    },
    // Only fetch if it's today AND not late night
    enabled: todaySelected && timeOfDay !== null,
    staleTime: 30 * 60_000, // 30 min — don't refetch on every navigation
    gcTime: 60 * 60_000, // 1 hour garbage collection
    retry: 1, // Retry once on failure
    refetchOnWindowFocus: false, // Don't refetch when user tabs back
  });
}
