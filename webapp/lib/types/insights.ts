/**
 * Types for the Insights page.
 * Port of Android's InsightsPeriod enum and InsightsUiState.
 */

import type { MacroGoals, MonthlyMacroSummary } from "./domain";

/** Selectable time window for charts. Matches Android InsightsPeriod enum. */
export type InsightsPeriod = "week" | "month" | "year";

export const PERIOD_CONFIG: Record<InsightsPeriod, { days: number; label: string }> = {
  week: { days: 7, label: "Week" },
  month: { days: 30, label: "Month" },
  year: { days: 365, label: "Year" },
};

/** Per-day summary used by bar and line charts. */
export interface DailyChartSummary {
  /** Date string "YYYY-MM-DD" */
  date: string;
  /** Short label for x-axis: "Mon", "14 May", etc. */
  label: string;
  totalCalories: number;
  totalProtein: number;
  totalCarbs: number;
  totalFat: number;
}

/** Aggregated insights data returned by the hook. */
export interface InsightsData {
  dailySummaries: DailyChartSummary[];
  monthlySummaries: MonthlyMacroSummary[];
  averageCalories: number;
  averageProtein: number;
  averageCarbs: number;
  averageFat: number;
  macroGoals: MacroGoals;
}
