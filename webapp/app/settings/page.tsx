"use client";

import { useState, useEffect } from "react";
import { DashboardShell } from "@/components/dashboard-shell";
import { Button } from "@/components/ui/button";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { useUpdateGoals } from "@/lib/hooks/use-update-goals";
import { MACRO_COLORS } from "@/lib/utils/constants";
import { ThemeSelector } from "@/components/theme-selector";
import { ProfileSection } from "./profile-section";
import { useSupabase } from "@/components/providers/supabase-provider";

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
  const supabase = useSupabase();
  const [linkError, setLinkError] = useState<string | null>(null);

  const handleLinkGoogle = async () => {
    setLinkError(null);
    const { data, error } = await supabase.auth.linkIdentity({
      provider: "google",
      options: {
        redirectTo: `${window.location.origin}/auth/confirm`,
      },
    });

    if (error) {
      setLinkError(error.message);
    } else if (data?.url) {
      window.location.href = data.url;
    }
  };

  // Local form state — initialized from fetched goals
  const [calories, setCalories] = useState("");
  const [protein, setProtein] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fat, setFat] = useState("");
  const [initialized, setInitialized] = useState(false);
  const [isGoalsExpanded, setIsGoalsExpanded] = useState(false);

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
          style={{ color: "var(--text-branded)" }}
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
        ) : !isGoalsExpanded ? (
          <div
            className="rounded-md border p-4 space-y-3"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-variant)",
            }}
          >
            <div className="flex items-center justify-between">
              <div>
                <h2
                  className="text-sm font-semibold"
                  style={{ color: "var(--text-primary)" }}
                >
                  Nutrition Goals
                </h2>
                <p className="text-xs mt-1" style={{ color: "var(--text-secondary)" }}>
                  Set your daily calorie & macro targets
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsGoalsExpanded(true)}
                className="cursor-pointer font-semibold"
                style={{ color: "var(--text-branded)" }}
              >
                Edit
              </Button>
            </div>
          </div>
        ) : (
          <div
            className="rounded-md border p-4 space-y-4 animate-in fade-in slide-in-from-top-2 duration-200"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-variant)",
            }}
          >
            <h2
              className="text-sm font-semibold"
              style={{ color: "var(--text-primary)" }}
            >
              Nutrition Goals
            </h2>

            <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
              Set your daily nutrition targets. These are used to calculate progress rings and insight percentages.
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

            {/* Save & Cancel buttons */}
            <div className="flex gap-3">
              <Button
                type="button"
                onClick={() => {
                  if (goals) {
                    setCalories(String(Math.round(goals.calorieGoal)));
                    setProtein(String(Math.round(goals.proteinGoal)));
                    setCarbs(String(Math.round(goals.carbsGoal)));
                    setFat(String(Math.round(goals.fatGoal)));
                  }
                  setIsGoalsExpanded(false);
                }}
                variant="ghost"
                className="flex-1 cursor-pointer border"
                style={{ borderColor: "var(--border-variant)", color: "var(--text-secondary)" }}
              >
                Cancel
              </Button>
              <Button
                onClick={() => {
                  handleSave();
                  setIsGoalsExpanded(false);
                }}
                disabled={!hasChanges || updateGoals.isPending}
                className="flex-1"
              >
                {updateGoals.isPending ? "Saving..." : "Save Goals"}
              </Button>
            </div>

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
                style={{ color: "var(--text-branded)" }}
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

        {/* Account Connections Card */}
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
            Account Connections
          </h2>
          <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
            Connect external OAuth providers to easily sign in with your favorite accounts.
          </p>

          <Button
            type="button"
            onClick={handleLinkGoogle}
            className="w-full flex items-center justify-center gap-2 border cursor-pointer hover:bg-neutral-50 dark:hover:bg-neutral-900"
            variant="ghost"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-variant)",
              color: "var(--text-primary)",
            }}
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22-.19-.63z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                fill="#EA4335"
              />
            </svg>
            Link Google Account
          </Button>

          {linkError && (
            <p className="text-xs text-center" style={{ color: "var(--color-error-red)" }}>
              {linkError}
            </p>
          )}
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
