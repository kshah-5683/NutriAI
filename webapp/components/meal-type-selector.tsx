"use client";

import { useLogFormStore } from "@/lib/stores/log-form-store";
import type { MealType } from "@/lib/types/domain";

const MEAL_OPTIONS: { value: MealType; label: string }[] = [
  { value: "breakfast", label: "Breakfast" },
  { value: "lunch", label: "Lunch" },
  { value: "snack", label: "Snack" },
  { value: "dinner", label: "Dinner" },
];

/**
 * Dropdown selector for meal type — used across all log input modes
 * (AI, Manual, Scan) and the EditLogSheet.
 *
 * When `value`/`onChange` props are provided, operates as a controlled component
 * (used by EditLogSheet). Otherwise reads/writes from the Zustand log form store.
 */
export function MealTypeSelector({
  value,
  onChange,
}: {
  value?: MealType;
  onChange?: (type: MealType) => void;
} = {}) {
  const storeMealType = useLogFormStore((s) => s.mealType);
  const storeSetMealType = useLogFormStore((s) => s.setMealType);

  const selected = value ?? storeMealType;
  const handleChange = onChange ?? storeSetMealType;

  return (
    <div>
      <label
        className="mb-1 block text-xs font-medium"
        style={{ color: "var(--text-secondary)" }}
      >
        Meal
      </label>
      <select
        value={selected}
        onChange={(e) => handleChange(e.target.value as MealType)}
        className="h-10 w-full rounded-lg border px-3 text-sm outline-none transition-colors focus:ring-2"
        style={{
          backgroundColor: "var(--bg-surface)",
          borderColor: "var(--border-outline)",
          color: "var(--text-primary)",
        }}
      >
        {MEAL_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
}
