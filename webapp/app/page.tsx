"use client";

import { useState, useMemo } from "react";
import { DashboardShell } from "@/components/dashboard-shell";
import { DateNavigationHeader } from "@/components/date-navigation-header";
import { MacroSummaryCard } from "@/components/macro-summary-card";
import { FoodLogList } from "@/components/food-log-list";
import { RecommendationCard } from "@/components/recommendation-card";
import { EditLogSheet } from "@/components/edit-log-sheet";
import { ConfirmDeleteDialog } from "@/components/confirm-delete-dialog";
import { useDailyLogs } from "@/lib/hooks/use-daily-logs";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { useCachedRecommendations } from "@/lib/hooks/use-cached-recommendations";
import { computeDailyTotals } from "@/lib/utils/compute-daily-totals";
import { useDateStore } from "@/lib/stores/date-store";
import type { DailyLog } from "@/lib/types/domain";

/**
 * Home dashboard page — daily macro summary + food log list.
 * Assembles DateNavigationHeader, MacroSummaryCard, FoodLogList,
 * EditLogSheet, and ConfirmDeleteDialog inside a DashboardShell.
 */
export default function HomePage() {
  const selectedDate = useDateStore((s) => s.selectedDate);
  const { data: logs = [], isLoading: logsLoading } = useDailyLogs();
  const { data: goals } = useMacroGoals();

  const {
    data: recommendations = [],
    isLoading: recsLoading,
    error: recsError,
    nextMeal,
    missedMeals,
  } = useCachedRecommendations();

  const [editingLog, setEditingLog] = useState<DailyLog | null>(null);
  const [deletingLog, setDeletingLog] = useState<DailyLog | null>(null);

  const totals = useMemo(
    () => computeDailyTotals(logs, selectedDate),
    [logs, selectedDate]
  );

  // Use defaults while goals are loading (useMacroGoals already falls back,
  // but this guards the brief undefined window on first render).
  const safeGoals = goals ?? {
    calorieGoal: 2000,
    proteinGoal: 150,
    carbsGoal: 250,
    fatGoal: 65,
  };

  return (
    <DashboardShell>
      <DateNavigationHeader />
      <MacroSummaryCard totals={totals} goals={safeGoals} />

      <RecommendationCard
        recommendations={recommendations}
        isLoading={recsLoading}
        error={recsError?.message ?? null}
        nextMeal={nextMeal}
        missedMeals={missedMeals}
      />

      <div className="mt-4">
        <FoodLogList
          logs={logs}
          isLoading={logsLoading}
          onEdit={setEditingLog}
          onDelete={setDeletingLog}
        />
      </div>

      {/* Log Food FAB — links to Phase W3 food logging flow */}
      <div className="fixed bottom-20 right-4 z-10">
        <a
          href="/log"
          className="flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform hover:scale-105 active:scale-95"
          style={{ backgroundColor: "var(--color-primary)", color: "#FFFFFF" }}
          aria-label="Log food"
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path
              d="M12 5V19M5 12H19"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </a>
      </div>

      {/* Edit + Delete modals */}
      <EditLogSheet log={editingLog} onClose={() => setEditingLog(null)} />
      <ConfirmDeleteDialog log={deletingLog} onClose={() => setDeletingLog(null)} />
    </DashboardShell>
  );
}
