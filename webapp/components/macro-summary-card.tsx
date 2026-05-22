"use client";

import { Card } from "@/components/ui/card";
import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";
import type { DailyMacroSummary, MacroGoals } from "@/lib/types/domain";

interface MacroSummaryCardProps {
  totals: DailyMacroSummary;
  goals: MacroGoals;
}

interface RingProps {
  value: number;
  goal: number;
  color: string;
  label: string;
  unit: string;
  size: number;
  strokeWidth: number;
}

function MacroRing({ value, goal, color, label, unit, size, strokeWidth }: RingProps) {
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const progress = goal > 0 ? Math.min(value / goal, 1) : 0;
  const dashOffset = circumference * (1 - progress);
  const center = size / 2;

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          {/* Background track */}
          <circle
            cx={center}
            cy={center}
            r={radius}
            fill="none"
            stroke="var(--border-variant)"
            strokeWidth={strokeWidth}
          />
          {/* Progress arc */}
          <circle
            cx={center}
            cy={center}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={dashOffset}
            className="transition-[stroke-dashoffset] duration-600 ease-out"
          />
        </svg>
        {/* Center label */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-sm font-semibold" style={{ color }}>
            {formatMacro(value)}
          </span>
        </div>
      </div>
      <span className="text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
        {label}
      </span>
      <span className="text-xs" style={{ color: "var(--text-secondary)" }}>
        / {formatMacro(goal)}{unit}
      </span>
    </div>
  );
}

/**
 * Macro summary card — 4 circular SVG arcs showing daily progress.
 * Large calorie ring on the left, 3 smaller macro rings on the right.
 * Mirrors Android's MacroSummaryCard component (Section 9.1 of arch plan).
 */
export function MacroSummaryCard({ totals, goals }: MacroSummaryCardProps) {
  return (
    <Card className="mx-4">
      <div className="flex items-center justify-around gap-2">
        {/* Calories — large ring */}
        <MacroRing
          value={totals.totalCalories}
          goal={goals.calorieGoal}
          color={MACRO_COLORS.calories}
          label="Cal"
          unit=""
          size={100}
          strokeWidth={8}
        />

        {/* Protein, Carbs, Fat — smaller rings */}
        <div className="flex gap-4">
          <MacroRing
            value={totals.totalProtein}
            goal={goals.proteinGoal}
            color={MACRO_COLORS.protein}
            label="Protein"
            unit="g"
            size={72}
            strokeWidth={6}
          />
          <MacroRing
            value={totals.totalCarbs}
            goal={goals.carbsGoal}
            color={MACRO_COLORS.carbs}
            label="Carbs"
            unit="g"
            size={72}
            strokeWidth={6}
          />
          <MacroRing
            value={totals.totalFat}
            goal={goals.fatGoal}
            color={MACRO_COLORS.fat}
            label="Fat"
            unit="g"
            size={72}
            strokeWidth={6}
          />
        </div>
      </div>
    </Card>
  );
}
