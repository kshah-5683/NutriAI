import type { DailyLog, DailyMacroSummary } from "@/lib/types/domain";

/**
 * Aggregates an array of DailyLog entries into a single DailyMacroSummary.
 * Used by the dashboard page to derive totals for MacroSummaryCard.
 */
export function computeDailyTotals(
  logs: DailyLog[],
  date: Date
): DailyMacroSummary {
  return logs.reduce<DailyMacroSummary>(
    (acc, log) => ({
      ...acc,
      totalCalories: acc.totalCalories + log.totalCalories,
      totalProtein: acc.totalProtein + log.totalProtein,
      totalCarbs: acc.totalCarbs + log.totalCarbs,
      totalFat: acc.totalFat + log.totalFat,
      logCount: acc.logCount + 1,
    }),
    {
      date,
      totalCalories: 0,
      totalProtein: 0,
      totalCarbs: 0,
      totalFat: 0,
      logCount: 0,
    }
  );
}
