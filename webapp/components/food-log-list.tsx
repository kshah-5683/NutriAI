"use client";

import { useMemo } from "react";
import { FoodLogItem } from "@/components/food-log-item";
import { Card } from "@/components/ui/card";
import type { DailyLog, MealType } from "@/lib/types/domain";

/** Display order and emoji for each meal type section header. */
const MEAL_SECTIONS: { type: MealType; label: string; emoji: string }[] = [
  { type: "breakfast", label: "Breakfast", emoji: "🌅" },
  { type: "lunch", label: "Lunch", emoji: "☀️" },
  { type: "snack", label: "Snack", emoji: "🍎" },
  { type: "dinner", label: "Dinner", emoji: "🌙" },
];

interface FoodLogListProps {
  logs: DailyLog[];
  isLoading: boolean;
  onEdit: (log: DailyLog) => void;
  onDelete: (log: DailyLog) => void;
}

/**
 * Renders the list of food log entries for the selected day,
 * grouped by meal type with section headers.
 * Shows loading skeleton, empty state, or the grouped list.
 */
export function FoodLogList({ logs, isLoading, onEdit, onDelete }: FoodLogListProps) {
  /** Group logs by mealType, preserving MEAL_SECTIONS order. */
  const groupedSections = useMemo(() => {
    const byType = new Map<string, DailyLog[]>();
    for (const log of logs) {
      const key = log.mealType ?? "snack";
      const arr = byType.get(key) ?? [];
      arr.push(log);
      byType.set(key, arr);
    }
    return MEAL_SECTIONS
      .filter((s) => byType.has(s.type))
      .map((s) => ({ ...s, items: byType.get(s.type)! }));
  }, [logs]);
  if (isLoading) {
    return (
      <Card noPadding className="mx-4 overflow-hidden">
        {Array.from({ length: 3 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-3 border-b px-4 py-3 animate-pulse"
            style={{ borderColor: "var(--border-variant)" }}
          >
            <div className="flex-1">
              <div className="h-4 w-32 rounded bg-sage-gray" />
              <div className="mt-1.5 h-3 w-20 rounded bg-sage-gray" />
              <div className="mt-2 flex gap-1">
                {Array.from({ length: 4 }).map((_, j) => (
                  <div key={j} className="h-5 w-14 rounded bg-sage-gray" />
                ))}
              </div>
            </div>
          </div>
        ))}
      </Card>
    );
  }

  if (logs.length === 0) {
    return (
      <div className="mx-4 flex flex-col items-center py-12 text-center">
        <div className="mb-3 text-4xl">🍽️</div>
        <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
          No food logged for this day
        </p>
        <p className="mt-1 text-xs" style={{ color: "var(--text-secondary)" }}>
          Tap the button below to log your meals
        </p>
      </div>
    );
  }

  return (
    <div className="mx-4 flex flex-col gap-3">
      {groupedSections.map((section) => (
        <Card key={section.type} noPadding className="overflow-hidden">
          {/* Section header */}
          <div
            className="flex items-center gap-2 px-4 py-2"
            style={{
              backgroundColor: "var(--bg-surface-variant)",
              borderBottom: "1px solid var(--border-variant)",
            }}
          >
            <span className="text-sm">{section.emoji}</span>
            <span
              className="text-xs font-semibold uppercase tracking-wide"
              style={{ color: "var(--text-secondary)" }}
            >
              {section.label}
            </span>
            <span
              className="ml-auto text-xs tabular-nums"
              style={{ color: "var(--text-secondary)" }}
            >
              {section.items.reduce((sum, l) => sum + l.totalCalories, 0).toFixed(0)} kcal
            </span>
          </div>
          {section.items.map((log) => (
            <FoodLogItem
              key={log.id}
              log={log}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </Card>
      ))}
    </div>
  );
}
