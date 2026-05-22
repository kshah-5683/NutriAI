"use client";

import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";

interface MacroPreviewCardProps {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  quantity: number;
}

/**
 * Live-computed macro totals preview card.
 * Shows total = base_macro * quantity in real-time.
 * Color-coded badges matching MACRO_COLORS.
 */
export function MacroPreviewCard({
  calories,
  protein,
  carbs,
  fat,
  quantity,
}: MacroPreviewCardProps) {
  const total = {
    calories: calories * quantity,
    protein: protein * quantity,
    carbs: carbs * quantity,
    fat: fat * quantity,
  };

  return (
    <div
      className="rounded-lg border p-3"
      style={{
        backgroundColor: "var(--bg-surface-variant)",
        borderColor: "var(--border-variant)",
      }}
    >
      <div className="mb-2 text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
        Total for {formatMacro(quantity)} serving{quantity !== 1 ? "s" : ""}
      </div>
      <div className="flex items-center gap-3">
        <MacroBadge
          label="kcal"
          value={total.calories}
          color={MACRO_COLORS.calories}
        />
        <MacroBadge
          label="P"
          value={total.protein}
          color={MACRO_COLORS.protein}
          unit="g"
        />
        <MacroBadge
          label="C"
          value={total.carbs}
          color={MACRO_COLORS.carbs}
          unit="g"
        />
        <MacroBadge
          label="F"
          value={total.fat}
          color={MACRO_COLORS.fat}
          unit="g"
        />
      </div>
    </div>
  );
}

function MacroBadge({
  label,
  value,
  color,
  unit = "",
}: {
  label: string;
  value: number;
  color: string;
  unit?: string;
}) {
  return (
    <div className="flex items-center gap-1">
      <span
        className="inline-block h-2 w-2 rounded-full"
        style={{ backgroundColor: color }}
      />
      <span className="text-xs font-medium">
        {formatMacro(value)}
        {unit}
      </span>
      <span className="text-xs" style={{ color: "var(--text-secondary)" }}>
        {label}
      </span>
    </div>
  );
}
