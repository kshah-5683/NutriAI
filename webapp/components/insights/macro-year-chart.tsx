"use client";

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ReferenceLine } from "recharts";
import { MACRO_COLORS } from "@/lib/utils/constants";
import type { MonthlyMacroSummary, MacroGoals } from "@/lib/types/domain";

interface MacroYearChartProps {
  summaries: MonthlyMacroSummary[];
  macroGoals: MacroGoals;
}

/**
 * Short month label from "YYYY-MM" string.
 */
function shortMonthLabel(yearMonth: string): string {
  const [y, m] = yearMonth.split("-");
  const d = new Date(Number(y), Number(m) - 1, 1);
  return d.toLocaleDateString("en-US", { month: "short" });
}

/**
 * Yearly macro overview — 3 grouped bars per month (Protein, Carbs, Fat).
 * Port of Android's MacroYearChart (Canvas-based grouped bars).
 *
 * Same visual structure as the weekly bar chart but with 12 months.
 */
export function MacroYearChart({ summaries, macroGoals }: MacroYearChartProps) {
  if (summaries.length === 0) return null;

  const data = summaries.map((s) => ({
    ...s,
    label: shortMonthLabel(s.yearMonth),
  }));

  // Y-axis domain: max of data, goals, or 50
  const dataMax = Math.max(
    ...data.map((d) => Math.max(d.totalProtein, d.totalCarbs, d.totalFat))
  );
  const goalMax = Math.max(macroGoals.proteinGoal, macroGoals.carbsGoal, macroGoals.fatGoal);
  const yMax = Math.ceil(Math.max(dataMax, goalMax, 50) * 1.1);

  return (
    <div className="w-full overflow-x-auto">
      <BarChart width={500} height={250} data={data} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border-variant)" />
        <XAxis
          dataKey="label"
          tick={{ fontSize: 9, fill: "var(--text-secondary)" }}
          tickLine={false}
          axisLine={{ stroke: "var(--border-variant)" }}
        />
        <YAxis
          tick={{ fontSize: 10, fill: "var(--text-secondary)" }}
          tickLine={false}
          axisLine={false}
          width={40}
          domain={[0, yMax]}
          unit="g"
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "var(--bg-surface)",
            border: "1px solid var(--border-variant)",
            borderRadius: 8,
            fontSize: 12,
          }}
          labelStyle={{ color: "var(--text-primary)", fontWeight: 600 }}
        />
        <Legend
          wrapperStyle={{ fontSize: 11 }}
          iconType="square"
          iconSize={10}
          formatter={(value: string) => {
            if (value === "Protein") return `Protein (goal: ${Math.round(macroGoals.proteinGoal)}g)`;
            if (value === "Carbs") return `Carbs (${Math.round(macroGoals.carbsGoal)}g)`;
            if (value === "Fat") return `Fat (${Math.round(macroGoals.fatGoal)}g)`;
            return value;
          }}
        />

        {/* Goal reference lines (dashed) — matches MacroLineChart */}
        <ReferenceLine
          y={macroGoals.proteinGoal}
          stroke={MACRO_COLORS.protein}
          strokeDasharray="8 6"
          strokeOpacity={0.45}
        />
        <ReferenceLine
          y={macroGoals.carbsGoal}
          stroke={MACRO_COLORS.carbs}
          strokeDasharray="8 6"
          strokeOpacity={0.45}
        />
        <ReferenceLine
          y={macroGoals.fatGoal}
          stroke={MACRO_COLORS.fat}
          strokeDasharray="8 6"
          strokeOpacity={0.45}
        />

        <Bar dataKey="totalProtein" name="Protein" fill={MACRO_COLORS.protein} radius={[2, 2, 0, 0]} barSize={8} />
        <Bar dataKey="totalCarbs" name="Carbs" fill={MACRO_COLORS.carbs} radius={[2, 2, 0, 0]} barSize={8} />
        <Bar dataKey="totalFat" name="Fat" fill={MACRO_COLORS.fat} radius={[2, 2, 0, 0]} barSize={8} />
      </BarChart>
    </div>
  );
}
