"use client";

import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { startOfDayMs } from "@/lib/utils/format";
import { PERIOD_CONFIG, type InsightsPeriod, type DailyChartSummary, type InsightsData } from "@/lib/types/insights";
import type { MonthlyMacroSummary } from "@/lib/types/domain";

/**
 * Formats a Date as "YYYY-MM-DD" in local time.
 */
function toDateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/**
 * Formats a Date as "YYYY-MM" in local time.
 */
function toMonthKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

/**
 * Short day label for week view: "Mon", "Tue", etc.
 */
function shortDayLabel(date: Date): string {
  return date.toLocaleDateString("en-US", { weekday: "short" });
}

/**
 * Short date label for month view: "14 May", etc.
 */
function shortDateLabel(date: Date): string {
  return date.toLocaleDateString("en-US", { day: "numeric", month: "short" });
}

/**
 * Short month label for year view: "Jan", "Feb", etc.
 */
function shortMonthLabel(yearMonth: string): string {
  const [y, m] = yearMonth.split("-");
  const d = new Date(Number(y), Number(m) - 1, 1);
  return d.toLocaleDateString("en-US", { month: "short" });
}

interface DailyLogRow {
  date_timestamp: number;
  total_calories: number;
  total_protein: number;
  total_carbs: number;
  total_fat: number;
}

/**
 * TanStack Query hook — fetches daily logs for the selected period,
 * aggregates into daily/monthly summaries, and computes averages.
 *
 * Port of Android's InsightsViewModel data flow:
 *   selectedPeriod → date range → query → group by date → zero-fill → averages
 *
 * Averages exclude zero-data days (matches Android).
 */
export function useInsightsData(period: InsightsPeriod) {
  const supabase = useSupabase();
  const { data: goals } = useMacroGoals();

  const macroGoals = goals ?? {
    calorieGoal: 2000,
    proteinGoal: 150,
    carbsGoal: 250,
    fatGoal: 65,
  };

  const config = PERIOD_CONFIG[period];
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const startDate = new Date(today);
  startDate.setDate(startDate.getDate() - (config.days - 1));

  const endDate = new Date(today);
  endDate.setDate(endDate.getDate() + 1);

  const queryResult = useQuery<InsightsData>({
    queryKey: ["insights-data", period, toDateKey(today)],
    queryFn: async () => {
      const startMs = startOfDayMs(startDate);
      const endMs = startOfDayMs(endDate);

      const { data, error } = await supabase
        .from("daily_logs")
        .select("date_timestamp, total_calories, total_protein, total_carbs, total_fat")
        .gte("date_timestamp", startMs)
        .lt("date_timestamp", endMs)
        .is("deleted_at", null);

      if (error) throw error;

      const logs = (data ?? []) as unknown as DailyLogRow[];

      // Group logs by date
      const byDate = new Map<string, { cal: number; pro: number; carb: number; fat: number }>();
      for (const log of logs) {
        const d = new Date(log.date_timestamp);
        const key = toDateKey(d);
        const existing = byDate.get(key) ?? { cal: 0, pro: 0, carb: 0, fat: 0 };
        existing.cal += log.total_calories;
        existing.pro += log.total_protein;
        existing.carb += log.total_carbs;
        existing.fat += log.total_fat;
        byDate.set(key, existing);
      }

      // Build daily summaries (zero-fill empty days)
      const dailySummaries: DailyChartSummary[] = [];
      for (let i = 0; i < config.days; i++) {
        const d = new Date(startDate);
        d.setDate(d.getDate() + i);
        const key = toDateKey(d);
        const agg = byDate.get(key);
        dailySummaries.push({
          date: key,
          label: period === "week" ? shortDayLabel(d) : shortDateLabel(d),
          totalCalories: agg?.cal ?? 0,
          totalProtein: agg?.pro ?? 0,
          totalCarbs: agg?.carb ?? 0,
          totalFat: agg?.fat ?? 0,
        });
      }

      // Build monthly summaries for year view
      let monthlySummaries: MonthlyMacroSummary[] = [];
      if (period === "year") {
        const currentMonth = toMonthKey(today);
        const startMonth = new Date(today);
        startMonth.setMonth(startMonth.getMonth() - 11);
        startMonth.setDate(1);

        // Group by month
        const byMonth = new Map<string, { cal: number; pro: number; carb: number; fat: number; daysSet: Set<string> }>();
        for (const [dateKey, agg] of byDate.entries()) {
          const mKey = dateKey.slice(0, 7); // "YYYY-MM"
          const existing = byMonth.get(mKey) ?? { cal: 0, pro: 0, carb: 0, fat: 0, daysSet: new Set() };
          existing.cal += agg.cal;
          existing.pro += agg.pro;
          existing.carb += agg.carb;
          existing.fat += agg.fat;
          if (agg.cal > 0 || agg.pro > 0 || agg.carb > 0 || agg.fat > 0) {
            existing.daysSet.add(dateKey);
          }
          byMonth.set(mKey, existing);
        }

        // Zero-fill 12 months
        for (let i = 0; i < 12; i++) {
          const d = new Date(startMonth);
          d.setMonth(d.getMonth() + i);
          const mKey = toMonthKey(d);
          const agg = byMonth.get(mKey);
          monthlySummaries.push({
            yearMonth: mKey,
            totalCalories: agg?.cal ?? 0,
            totalProtein: agg?.pro ?? 0,
            totalCarbs: agg?.carb ?? 0,
            totalFat: agg?.fat ?? 0,
            daysWithData: agg?.daysSet.size ?? 0,
          });
        }
      }

      // Compute averages (exclude zero-data days — matches Android)
      let avgCal = 0, avgPro = 0, avgCarb = 0, avgFat = 0;

      if (period === "year") {
        const totalDays = Math.max(1, monthlySummaries.reduce((sum, m) => sum + m.daysWithData, 0));
        avgCal = monthlySummaries.reduce((s, m) => s + m.totalCalories, 0) / totalDays;
        avgPro = monthlySummaries.reduce((s, m) => s + m.totalProtein, 0) / totalDays;
        avgCarb = monthlySummaries.reduce((s, m) => s + m.totalCarbs, 0) / totalDays;
        avgFat = monthlySummaries.reduce((s, m) => s + m.totalFat, 0) / totalDays;
      } else {
        const daysWithData = dailySummaries.filter((d) => d.totalCalories > 0);
        const count = Math.max(1, daysWithData.length);
        avgCal = daysWithData.reduce((s, d) => s + d.totalCalories, 0) / count;
        avgPro = daysWithData.reduce((s, d) => s + d.totalProtein, 0) / count;
        avgCarb = daysWithData.reduce((s, d) => s + d.totalCarbs, 0) / count;
        avgFat = daysWithData.reduce((s, d) => s + d.totalFat, 0) / count;
      }

      return {
        dailySummaries,
        monthlySummaries,
        averageCalories: avgCal,
        averageProtein: avgPro,
        averageCarbs: avgCarb,
        averageFat: avgFat,
        macroGoals,
      };
    },
    staleTime: 2 * 60_000, // 2 minutes
  });

  return queryResult;
}
