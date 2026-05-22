"use client";

import { FoodLogItem } from "@/components/food-log-item";
import { Card } from "@/components/ui/card";
import type { DailyLog } from "@/lib/types/domain";

interface FoodLogListProps {
  logs: DailyLog[];
  isLoading: boolean;
  onEdit: (log: DailyLog) => void;
  onDelete: (log: DailyLog) => void;
}

/**
 * Renders the list of food log entries for the selected day.
 * Shows loading skeleton, empty state, or the actual list.
 */
export function FoodLogList({ logs, isLoading, onEdit, onDelete }: FoodLogListProps) {
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
    <Card noPadding className="mx-4 overflow-hidden">
      {logs.map((log) => (
        <FoodLogItem
          key={log.id}
          log={log}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </Card>
  );
}
