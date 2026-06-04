"use client";

import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";
import { toDisplayQty } from "@/lib/utils/unit-converter";
import type { DailyLog } from "@/lib/types/domain";

interface FoodLogItemProps {
  log: DailyLog;
  onEdit: (log: DailyLog) => void;
  onDelete: (log: DailyLog) => void;
}

interface MacroChipProps {
  value: number;
  label: string;
  color: string;
}

function MacroChip({ value, label, color }: MacroChipProps) {
  return (
    <span
      className="inline-flex items-center gap-0.5 rounded-xs px-1.5 py-0.5 text-xs font-medium"
      style={{ backgroundColor: `${color}18`, color }}
    >
      {formatMacro(value)}{label}
    </span>
  );
}

/**
 * A single food log row — name, quantity, macro chips, edit/delete buttons.
 */
export function FoodLogItem({ log, onEdit, onDelete }: FoodLogItemProps) {
  return (
    <div
      className="flex items-center gap-3 border-b px-4 py-3"
      style={{ borderColor: "var(--border-variant)" }}
    >
      {/* Food info */}
      <div className="flex-1 min-w-0">
        <p
          className="truncate text-sm font-medium"
          style={{ color: "var(--text-primary)" }}
        >
          {log.foodName}
        </p>
        <div className="mt-0.5 flex items-center gap-1.5">
          <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
            {formatMacro(toDisplayQty(log.consumedQty, log.consumedUnit))} {log.consumedUnit}
          </p>
          {log.mealType && (
            <span
              className="rounded-sm px-1.5 py-0.5 text-[10px] font-medium capitalize"
              style={{
                backgroundColor: "var(--bg-surface-variant)",
                color: "var(--text-secondary)",
              }}
            >
              {log.mealType}
            </span>
          )}
        </div>
        <div className="mt-1.5 flex flex-wrap gap-1">
          <MacroChip value={log.totalCalories} label=" kcal" color={MACRO_COLORS.calories} />
          <MacroChip value={log.totalProtein} label="g P" color={MACRO_COLORS.protein} />
          <MacroChip value={log.totalCarbs} label="g C" color={MACRO_COLORS.carbs} />
          <MacroChip value={log.totalFat} label="g F" color={MACRO_COLORS.fat} />
        </div>
      </div>

      {/* Actions */}
      <div className="flex shrink-0 gap-1">
        <button
          onClick={() => onEdit(log)}
          className="flex h-8 w-8 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
          aria-label={`Edit ${log.foodName}`}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path
              d="M11.333 2.00004C11.51 1.82274 11.7209 1.68283 11.9529 1.58874C12.185 1.49465 12.4337 1.44824 12.6843 1.45199C12.9348 1.45573 13.182 1.50955 13.4112 1.61057C13.6404 1.71159 13.847 1.85779 14.019 2.04038C14.191 2.22296 14.325 2.43832 14.4131 2.67356C14.5011 2.9088 14.5413 3.15927 14.5312 3.41018C14.5211 3.66109 14.461 3.90748 14.3545 4.13481C14.2479 4.36214 14.0972 4.56593 13.911 4.73404L5.244 13.401L1.333 14.334L2.266 10.423L11.333 2.00004Z"
              stroke="var(--text-secondary)"
              strokeWidth="1.2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
        <button
          onClick={() => onDelete(log)}
          className="flex h-8 w-8 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
          aria-label={`Delete ${log.foodName}`}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path
              d="M2 4H14M12.667 4V13.333C12.667 14 12 14.667 11.333 14.667H4.667C4 14.667 3.333 14 3.333 13.333V4M5.333 4V2.667C5.333 2 6 1.333 6.667 1.333H9.333C10 1.333 10.667 2 10.667 2.667V4"
              stroke="var(--text-secondary)"
              strokeWidth="1.2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      </div>
    </div>
  );
}
