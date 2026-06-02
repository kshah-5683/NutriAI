"use client";

import { useState, useEffect } from "react";
import { DashboardShell } from "@/components/dashboard-shell";
import { Button } from "@/components/ui/button";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { useUpdateGoals } from "@/lib/hooks/use-update-goals";
import { MACRO_COLORS } from "@/lib/utils/constants";
import { ThemeSelector } from "@/components/theme-selector";
import { ProfileSection } from "./profile-section";

/**
 * Settings page — macro goals CRUD + account actions.
 *
 * Web divergence from Android:
 * - Android manages goals in a bottom-sheet from the Auth/Profile screen
 * - Web has a dedicated Settings page in the bottom nav (4th tab)
 * - Goals are persisted to Supabase user_preferences for cross-platform sync
 *
 * Layout:
 *  1. "Settings" title
 *  2. Daily Goals card — 4 numeric inputs (calories, protein, carbs, fat)
 *  3. Save button (disabled when unchanged or saving)
 *  4. Dark mode toggle (added in W4-6)
 */
export default function SettingsPage() {
  const { data: goals, isLoading, isError } = useMacroGoals();
  const updateGoals = useUpdateGoals();

  // Local form state — initialized from fetched goals
  const [calories, setCalories] = useState("");
  const [protein, setProtein] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fat, setFat] = useState("");
  const [initialized, setInitialized] = useState(false);

  // Seed form when goals load
  useEffect(() => {
    if (goals && !initialized) {
      setCalories(String(Math.round(goals.calorieGoal)));
      setProtein(String(Math.round(goals.proteinGoal)));
      setCarbs(String(Math.round(goals.carbsGoal)));
      setFat(String(Math.round(goals.fatGoal)));
      setInitialized(true);
    }
  }, [goals, initialized]);

  const hasChanges =
    initialized &&
    goals &&
    (String(Math.round(goals.calorieGoal)) !== calories ||
      String(Math.round(goals.proteinGoal)) !== protein ||
      String(Math.round(goals.carbsGoal)) !== carbs ||
      String(Math.round(goals.fatGoal)) !== fat);

  const handleSave = () => {
    const cal = parseFloat(calories) || 0;
    const pro = parseFloat(protein) || 0;
    const carb = parseFloat(carbs) || 0;
    const f = parseFloat(fat) || 0;

    if (cal <= 0) return;

    updateGoals.mutate(
      {
        calorieGoal: cal,
        proteinGoal: pro,
        carbsGoal: carb,
        fatGoal: f,
      },
      {
        onSuccess: () => {
          // Reset "initialized" so next load re-seeds with saved values
          setInitialized(false);
        },
      }
    );
  };

  return (
    <DashboardShell>
      <div className="mx-auto max-w-lg space-y-4 px-4 py-4">
        {/* Title */}
        <h1
          className="text-2xl font-bold"
          style={{ color: "var(--color-primary)" }}
        >
          Settings
        </h1>

        {/* Goals card */}
        {isLoading ? (
          <div className="flex items-center justify-center py-16">
            <div
              className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
              style={{
                borderColor: "var(--color-primary)",
                borderTopColor: "transparent",
              }}
            />
          </div>
        ) : isError ? (
          <div
            className="rounded-md border p-4 text-center space-y-2"
            style={{
              backgroundColor: "#FFB4AB20",
              borderColor: "var(--color-error-red)",
            }}
          >
            <p className="text-sm font-medium" style={{ color: "var(--color-error-red)" }}>
              Could not load your goals
            </p>
            <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
              Please check your connection and reload the page.
            </p>
          </div>
        ) : (
          <div
            className="rounded-md border p-4 space-y-4"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-variant)",
            }}
          >
            <h2
              className="text-sm font-semibold"
              style={{ color: "var(--text-primary)" }}
            >
              Daily Macro Goals
            </h2>

            <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
              Set your daily nutrition targets. These are used to calculate
              progress rings and insight percentages.
            </p>

            <div className="space-y-3">
              <GoalInput
                label="Calories"
                unit="kcal"
                value={calories}
                onChange={setCalories}
                color={MACRO_COLORS.calories}
              />
              <GoalInput
                label="Protein"
                unit="g"
                value={protein}
                onChange={setProtein}
                color={MACRO_COLORS.protein}
              />
              <GoalInput
                label="Carbs"
                unit="g"
                value={carbs}
                onChange={setCarbs}
                color={MACRO_COLORS.carbs}
              />
              <GoalInput
                label="Fat"
                unit="g"
                value={fat}
                onChange={setFat}
                color={MACRO_COLORS.fat}
              />
            </div>

            {/* Save button */}
            <Button
              onClick={handleSave}
              disabled={!hasChanges || updateGoals.isPending}
              className="w-full"
            >
              {updateGoals.isPending ? "Saving..." : "Save Goals"}
            </Button>

            {updateGoals.isError && (
              <p
                className="text-xs text-center"
                style={{ color: "var(--color-error-red)" }}
              >
                {updateGoals.error instanceof Error
                  ? updateGoals.error.message
                  : "Failed to save goals. Please try again."}
              </p>
            )}

            {updateGoals.isSuccess && !hasChanges && (
              <p
                className="text-xs text-center"
                style={{ color: "var(--color-primary)" }}
              >
                Goals saved successfully!
              </p>
            )}
          </div>
        )}

        {/* AI Recommendations Profile */}
        <ProfileSection />

        {/* Appearance — Dark mode toggle */}
        <div
          className="rounded-md border p-4 space-y-3"
          style={{
            backgroundColor: "var(--bg-surface)",
            borderColor: "var(--border-variant)",
          }}
        >
          <h2
            className="text-sm font-semibold"
            style={{ color: "var(--text-primary)" }}
          >
            Appearance
          </h2>
          <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
            Choose your preferred theme. System follows your device settings.
          </p>
          <ThemeSelector />
        </div>
      </div>
    </DashboardShell>
  );
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function GoalInput({
  label,
  unit,
  value,
  onChange,
  color,
}: {
  label: string;
  unit: string;
  value: string;
  onChange: (v: string) => void;
  color: string;
}) {
  return (
    <div className="flex items-center gap-3">
      {/* Color dot */}
      <div
        className="h-3 w-3 shrink-0 rounded-full"
        style={{ backgroundColor: color }}
      />

      {/* Label */}
      <span
        className="w-16 text-sm font-medium"
        style={{ color: "var(--text-primary)" }}
      >
        {label}
      </span>

      {/* Input */}
      <div className="flex flex-1 items-center gap-1">
        <input
          type="number"
          inputMode="numeric"
          min="0"
          step="1"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full rounded-md border px-3 py-2 text-sm outline-none transition-colors focus:border-[var(--color-primary)]"
          style={{
            backgroundColor: "var(--bg-app)",
            borderColor: "var(--border-variant)",
            color: "var(--text-primary)",
          }}
        />
        <span
          className="shrink-0 text-xs"
          style={{ color: "var(--text-secondary)" }}
        >
          {unit}
        </span>
      </div>
    </div>
  );
}
