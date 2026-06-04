"use client";

import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useDateStore } from "@/lib/stores/date-store";
import { startOfDayMs, toLocalDateString } from "@/lib/utils/format";
import type { DailyLog, MealType } from "@/lib/types/domain";

/**
 * Returns the start-of-day timestamp for the day AFTER the given date.
 * Used as the upper bound in daily log queries (exclusive).
 */
function nextDayStartMs(date: Date): number {
  const next = new Date(date);
  next.setDate(next.getDate() + 1);
  return startOfDayMs(next);
}

/**
 * Maps a Supabase daily_logs row (snake_case) to a DailyLog domain object (camelCase).
 */
function mapRow(row: Record<string, unknown>): DailyLog {
  return {
    id: row.id as string,
    userId: row.user_id as string,
    foodItemId: (row.food_item_id as string) ?? null,
    foodName: row.food_name as string,
    dateTimestamp: row.date_timestamp as number,
    consumedQty: row.consumed_qty as number,
    consumedUnit: row.consumed_unit as string,
    totalCalories: row.total_calories as number,
    totalProtein: row.total_protein as number,
    totalCarbs: row.total_carbs as number,
    totalFat: row.total_fat as number,
    mealType: (row.meal_type as MealType) ?? null,
    lastModifiedAt: row.last_modified_at as number,
    deletedAt: (row.deleted_at as number) ?? null,
  };
}

/**
 * TanStack Query hook — fetches daily logs for the currently selected date.
 *
 * RLS on daily_logs filters by auth.uid() automatically, so no explicit
 * .eq("user_id", userId) is needed.
 *
 * CRITICAL: Every query includes .is("deleted_at", null) to exclude tombstones.
 */
export function useDailyLogs() {
  const supabase = useSupabase();
  const selectedDate = useDateStore((s) => s.selectedDate);
  const dateKey = toLocalDateString(selectedDate);

  return useQuery<DailyLog[]>({
    queryKey: ["daily-logs", dateKey],
    queryFn: async () => {
      const dayStart = startOfDayMs(selectedDate);
      const dayEnd = nextDayStartMs(selectedDate);

      const { data, error } = await supabase
        .from("daily_logs")
        .select("*")
        .gte("date_timestamp", dayStart)
        .lt("date_timestamp", dayEnd)
        .is("deleted_at", null)
        .order("date_timestamp", { ascending: false });

      if (error) throw error;
      return (data ?? []).map(mapRow);
    },
  });
}
