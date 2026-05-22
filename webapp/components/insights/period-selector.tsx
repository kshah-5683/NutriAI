"use client";

import { PERIOD_CONFIG, type InsightsPeriod } from "@/lib/types/insights";

interface PeriodSelectorProps {
  selected: InsightsPeriod;
  onSelect: (period: InsightsPeriod) => void;
}

const PERIODS: InsightsPeriod[] = ["week", "month", "year"];

/**
 * 3-segment toggle: Week / Month / Year.
 * Port of Android's PeriodSelector (SingleChoiceSegmentedButtonRow).
 */
export function PeriodSelector({ selected, onSelect }: PeriodSelectorProps) {
  return (
    <div
      className="flex rounded-md border"
      style={{ borderColor: "var(--border-variant)" }}
    >
      {PERIODS.map((period) => (
        <button
          key={period}
          onClick={() => onSelect(period)}
          className="flex-1 py-2 text-sm font-medium transition-colors"
          style={{
            backgroundColor:
              selected === period ? "var(--color-primary-container)" : "transparent",
            color:
              selected === period ? "var(--color-primary)" : "var(--text-secondary)",
          }}
        >
          {PERIOD_CONFIG[period].label}
        </button>
      ))}
    </div>
  );
}
