"use client";

import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";
import { computeServingMultiplier } from "@/lib/utils/macro-calculator";
import { isGramUnit } from "@/lib/utils/unit-converter";

interface MacroPreviewCardProps {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  quantity: number;
  /** Unit string — used for correct multiplier calculation. Defaults to "serving". */
  unit?: string;
}

/**
 * Live-computed macro totals preview card.
 * Uses computeServingMultiplier() for correct scaling across all unit types.
 * Color-coded badges matching MACRO_COLORS.
 */
export function MacroPreviewCard({
  calories,
  protein,
  carbs,
  fat,
  quantity,
  unit = "serving",
}: MacroPreviewCardProps) {
  const multiplier = computeServingMultiplier(quantity, unit);
  const total = {
    calories: calories * multiplier,
    protein: protein * multiplier,
    carbs: carbs * multiplier,
    fat: fat * multiplier,
  };

  // Context-aware label: "Total for 200g" / "Total for 2 cups" / "Total for 1 piece"
  const qtyLabel = isGramUnit(unit)
    ? `${formatMacro(quantity)}${unit}`
    : `${formatMacro(quantity)} ${unit}${quantity !== 1 ? "s" : ""}`;

  return (
    <div
      className="rounded-lg border p-3"
      style={{
        backgroundColor: "var(--bg-surface-variant)",
        borderColor: "var(--border-variant)",
      }}
    >
      <div className="mb-2 text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
        Total for {qtyLabel}
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
