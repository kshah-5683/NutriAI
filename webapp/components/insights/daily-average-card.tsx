"use client";

import { MACRO_COLORS } from "@/lib/utils/constants";
import type { MacroGoals } from "@/lib/types/domain";

interface DailyAverageCardProps {
  averageCalories: number;
  averageProtein: number;
  averageCarbs: number;
  averageFat: number;
  macroGoals: MacroGoals;
  periodLabel: string;
}

/**
 * Card displaying average daily macro intake for the selected period.
 * Exact port of Android's DailyAverageCard composable.
 *
 * Layout:
 *  - Title: "Daily Average — {periodLabel}"
 *  - Calorie row: full-width, value + % in CalorieColor
 *  - Divider
 *  - P/C/F: 3 equal columns, each: value (bold) + "% of goal" (colored) + label
 */
export function DailyAverageCard({
  averageCalories,
  averageProtein,
  averageCarbs,
  averageFat,
  macroGoals,
  periodLabel,
}: DailyAverageCardProps) {
  const calPct = macroGoals.calorieGoal > 0
    ? Math.round((averageCalories / macroGoals.calorieGoal) * 100)
    : 0;
  const proPct = macroGoals.proteinGoal > 0
    ? Math.round((averageProtein / macroGoals.proteinGoal) * 100)
    : 0;
  const carbPct = macroGoals.carbsGoal > 0
    ? Math.round((averageCarbs / macroGoals.carbsGoal) * 100)
    : 0;
  const fatPct = macroGoals.fatGoal > 0
    ? Math.round((averageFat / macroGoals.fatGoal) * 100)
    : 0;

  return (
    <div
      className="rounded-md border p-4"
      style={{
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border-variant)",
      }}
    >
      {/* Title */}
      <h3
        className="text-sm font-semibold"
        style={{ color: "var(--text-primary)" }}
      >
        Daily Average — {periodLabel}
      </h3>

      {/* Calorie row */}
      <div className="mt-3 flex items-center justify-between">
        <span className="text-sm" style={{ color: "var(--text-secondary)" }}>
          Calories
        </span>
        <div className="flex items-center gap-2">
          <span
            className="text-base font-semibold"
            style={{ color: "var(--text-primary)" }}
          >
            {Math.round(averageCalories)} kcal
          </span>
          <span
            className="text-xs font-medium"
            style={{ color: MACRO_COLORS.calories }}
          >
            {calPct}%
          </span>
        </div>
      </div>

      {/* Divider */}
      <hr
        className="my-3"
        style={{ borderColor: "var(--border-variant)", opacity: 0.5 }}
      />

      {/* P/C/F columns */}
      <div className="grid grid-cols-3 text-center">
        <MacroColumn
          value={averageProtein}
          pct={proPct}
          label="Protein"
          color={MACRO_COLORS.protein}
        />
        <MacroColumn
          value={averageCarbs}
          pct={carbPct}
          label="Carbs"
          color={MACRO_COLORS.carbs}
        />
        <MacroColumn
          value={averageFat}
          pct={fatPct}
          label="Fat"
          color={MACRO_COLORS.fat}
        />
      </div>
    </div>
  );
}

function MacroColumn({
  value,
  pct,
  label,
  color,
}: {
  value: number;
  pct: number;
  label: string;
  color: string;
}) {
  return (
    <div className="flex flex-col items-center">
      <span
        className="text-base font-bold"
        style={{ color: "var(--text-primary)" }}
      >
        {Math.round(value)}g
      </span>
      <span className="text-xs font-medium" style={{ color }}>
        {pct}% of goal
      </span>
      <span
        className="text-xs"
        style={{ color: "var(--text-secondary)" }}
      >
        {label}
      </span>
    </div>
  );
}
