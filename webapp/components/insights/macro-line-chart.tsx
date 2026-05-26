"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine,
  ResponsiveContainer,
} from "recharts";
import { MACRO_COLORS } from "@/lib/utils/constants";
import type { DailyChartSummary } from "@/lib/types/insights";
import type { MacroGoals } from "@/lib/types/domain";

interface MacroLineChartProps {
  summaries: DailyChartSummary[];
  macroGoals: MacroGoals;
}

/**
 * Monthly macro trend line chart — 3 smooth lines (Protein, Carbs, Fat)
 * with dashed goal reference lines.
 *
 * Port of Android's MacroLineChart (Canvas cubic Bézier lines).
 *
 * Does NOT plot calories — only P/C/F, matching Android exactly.
 * Y-axis max = max(dataMax, goalMax, 50).
 * X-axis labels shown every 5 days.
 */
export function MacroLineChart({ summaries, macroGoals }: MacroLineChartProps) {
  if (summaries.length === 0) return null;

  // Y-axis domain: max of data, goals, or 50 — matches Android
  const dataMax = Math.max(
    ...summaries.map((d) => Math.max(d.totalProtein, d.totalCarbs, d.totalFat))
  );
  const goalMax = Math.max(macroGoals.proteinGoal, macroGoals.carbsGoal, macroGoals.fatGoal);
  const yMax = Math.ceil(Math.max(dataMax, goalMax, 50) * 1.1);

  // Show x-axis labels every 5 days + last day
  const filteredData = summaries.map((d, i) => ({
    ...d,
    xLabel: i % 5 === 0 || i === summaries.length - 1 ? d.label : "",
  }));

  return (
    <div className="w-full">
      <ResponsiveContainer width="100%" height={260}>
      <LineChart data={filteredData} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border-variant)" />
        <XAxis
          dataKey="xLabel"
          tick={{ fontSize: 9, fill: "var(--text-secondary)" }}
          tickLine={false}
          axisLine={{ stroke: "var(--border-variant)" }}
          interval={0}
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
          labelFormatter={(_label, payload) => {
            if (payload?.[0]?.payload?.label) return payload[0].payload.label;
            return _label;
          }}
        />
        <Legend
          wrapperStyle={{ fontSize: 11 }}
          formatter={(value: string) => {
            if (value === "Protein") return `Protein (goal: ${Math.round(macroGoals.proteinGoal)}g)`;
            if (value === "Carbs") return `Carbs (${Math.round(macroGoals.carbsGoal)}g)`;
            if (value === "Fat") return `Fat (${Math.round(macroGoals.fatGoal)}g)`;
            return value;
          }}
        />

        {/* Goal reference lines (dashed) */}
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

        {/* Data lines */}
        <Line
          type="monotone"
          dataKey="totalProtein"
          name="Protein"
          stroke={MACRO_COLORS.protein}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4 }}
        />
        <Line
          type="monotone"
          dataKey="totalCarbs"
          name="Carbs"
          stroke={MACRO_COLORS.carbs}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4 }}
        />
        <Line
          type="monotone"
          dataKey="totalFat"
          name="Fat"
          stroke={MACRO_COLORS.fat}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4 }}
        />
      </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
