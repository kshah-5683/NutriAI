"use client";

import { useState } from "react";
import { DashboardShell } from "@/components/dashboard-shell";
import { PeriodSelector } from "@/components/insights/period-selector";
import { MacroBarChart } from "@/components/insights/macro-bar-chart";
import { MacroLineChart } from "@/components/insights/macro-line-chart";
import { MacroYearChart } from "@/components/insights/macro-year-chart";
import { DailyAverageCard } from "@/components/insights/daily-average-card";
import { useInsightsData } from "@/lib/hooks/use-insights-data";
import { PERIOD_CONFIG, type InsightsPeriod } from "@/lib/types/insights";

/**
 * Insights page — weekly/monthly macro trend charts and period averages.
 * Port of Android's InsightsScreen.
 *
 * Layout (scrollable):
 *  1. "Insights" title
 *  2. Average calorie headline
 *  3. PeriodSelector — Week / Month / Year toggle
 *  4. Chart card (bar/line/year — switches on period)
 *  5. DailyAverageCard — period averages vs goals
 */
export default function InsightsPage() {
  const [period, setPeriod] = useState<InsightsPeriod>("week");
  const { data, isLoading } = useInsightsData(period);

  const periodLabel =
    period === "week" ? "This Week" : period === "month" ? "This Month" : "This Year";

  return (
    <DashboardShell>
      <div className="mx-auto max-w-lg space-y-4 px-4 py-4">
        {/* Title */}
        <h1
          className="text-2xl font-bold"
          style={{ color: "var(--text-branded)" }}
        >
          Insights
        </h1>

        {/* Average headline */}
        {data && (
          <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
            Avg {Math.round(data.averageCalories)} kcal/day {periodLabel.toLowerCase()}
          </p>
        )}

        {/* Period selector */}
        <PeriodSelector selected={period} onSelect={setPeriod} />

        {/* Chart */}
        {isLoading ? (
          <div className="flex items-center justify-center py-16">
            <div
              className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
              style={{ borderColor: "var(--color-primary)", borderTopColor: "transparent" }}
            />
          </div>
        ) : data && hasChartData(data, period) ? (
          <div
            className="rounded-md border p-3"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-variant)",
            }}
          >
            {period === "week" && (
              <MacroBarChart
                summaries={data.dailySummaries}
                macroGoals={data.macroGoals}
              />
            )}
            {period === "month" && (
              <MacroLineChart
                summaries={data.dailySummaries}
                macroGoals={data.macroGoals}
              />
            )}
            {period === "year" && (
              <MacroYearChart
                summaries={data.monthlySummaries}
                macroGoals={data.macroGoals}
              />
            )}
          </div>
        ) : (
          <EmptyChartState />
        )}

        {/* Daily average card */}
        {data && (
          <DailyAverageCard
            averageCalories={data.averageCalories}
            averageProtein={data.averageProtein}
            averageCarbs={data.averageCarbs}
            averageFat={data.averageFat}
            macroGoals={data.macroGoals}
            periodLabel={periodLabel}
          />
        )}
      </div>
    </DashboardShell>
  );
}

/**
 * Checks if there is any non-zero data to display in a chart.
 */
function hasChartData(
  data: { dailySummaries: { totalCalories: number }[]; monthlySummaries: { totalCalories: number }[] },
  period: InsightsPeriod
): boolean {
  if (period === "year") {
    return data.monthlySummaries.some((m) => m.totalCalories > 0);
  }
  return data.dailySummaries.some((d) => d.totalCalories > 0);
}

function EmptyChartState() {
  return (
    <div
      className="flex flex-col items-center justify-center gap-3 rounded-md border py-12"
      style={{
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border-variant)",
      }}
    >
      <span className="text-4xl">📊</span>
      <h3
        className="text-base font-medium"
        style={{ color: "var(--text-secondary)" }}
      >
        No data yet
      </h3>
      <p
        className="text-center text-xs"
        style={{ color: "var(--text-secondary)", opacity: 0.7 }}
      >
        Start logging food on the Home screen
        <br />
        to see your trends here.
      </p>
    </div>
  );
}
