"use client";

import { useLogFormStore } from "@/lib/stores/log-form-store";
import { useDateStore } from "@/lib/stores/date-store";
import { useLogFood } from "@/lib/hooks/use-log-food";
import { MacroPreviewCard } from "@/components/macro-preview-card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { startOfDayMs } from "@/lib/utils/format";

/** Unit options for the dropdown — matches Android unit picker. */
const UNIT_OPTIONS = [
  { value: "serving", label: "Serving" },
  { value: "g", label: "Grams (g)" },
  { value: "ml", label: "Milliliters (ml)" },
  { value: "cup", label: "Cup" },
  { value: "tbsp", label: "Tablespoon" },
  { value: "tsp", label: "Teaspoon" },
  { value: "piece", label: "Piece" },
  { value: "slice", label: "Slice" },
  { value: "bowl", label: "Bowl" },
] as const;

/**
 * Manual entry form — food name, brand, qty, unit (dropdown), macros.
 * Live MacroPreviewCard shows computed totals.
 */
export function ManualInputSection() {
  const foodName = useLogFormStore((s) => s.foodName);
  const brand = useLogFormStore((s) => s.brand);
  const quantity = useLogFormStore((s) => s.quantity);
  const unit = useLogFormStore((s) => s.unit);
  const calories = useLogFormStore((s) => s.calories);
  const protein = useLogFormStore((s) => s.protein);
  const carbs = useLogFormStore((s) => s.carbs);
  const fat = useLogFormStore((s) => s.fat);
  const setField = useLogFormStore((s) => s.setManualField);

  const selectedDate = useDateStore((s) => s.selectedDate);
  const logFood = useLogFood();

  const numQty = parseFloat(quantity) || 0;
  const numCal = parseFloat(calories) || 0;
  const numProtein = parseFloat(protein) || 0;
  const numCarbs = parseFloat(carbs) || 0;
  const numFat = parseFloat(fat) || 0;

  const isValid = foodName.trim().length > 0 && numQty > 0;

  const handleSave = () => {
    if (!isValid) return;

    logFood.mutate({
      foodName: foodName.trim(),
      brand: brand.trim() || undefined,
      baseServingG: 100, // All macros are per 100g
      baseCalories: numCal,
      baseProtein: numProtein,
      baseCarbs: numCarbs,
      baseFat: numFat,
      consumedQty: numQty,
      consumedUnit: unit,
      dateTimestamp: startOfDayMs(selectedDate),
    });
  };

  return (
    <div className="space-y-4">
      {/* Food name */}
      <Input
        label="Food name *"
        value={foodName}
        onChange={(e) => setField("foodName", e.target.value)}
        placeholder="e.g. Scrambled eggs"
      />

      {/* Brand (optional) */}
      <Input
        label="Brand (optional)"
        value={brand}
        onChange={(e) => setField("brand", e.target.value)}
        placeholder="e.g. Amul"
      />

      {/* Quantity + Unit row */}
      <div className="grid grid-cols-2 gap-3">
        <Input
          label="Quantity *"
          type="number"
          value={quantity}
          onChange={(e) => setField("quantity", e.target.value)}
          min="0"
          step="0.1"
        />
        <div>
          <label
            className="mb-1 block text-xs font-medium"
            style={{ color: "var(--text-secondary)" }}
          >
            Unit
          </label>
          <select
            value={unit}
            onChange={(e) => setField("unit", e.target.value)}
            className="h-10 w-full rounded-lg border px-3 text-sm outline-none transition-colors focus:ring-2"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-outline)",
              color: "var(--text-primary)",
            }}
          >
            {UNIT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Macros — label adapts to unit type */}
      <div>
        <div className="mb-2 text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
          {["piece", "slice", "bowl"].includes(unit.toLowerCase())
            ? `Nutrition per ${unit}`
            : "Nutrition per 100g"}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Calories (kcal)"
            type="number"
            value={calories}
            onChange={(e) => setField("calories", e.target.value)}
            min="0"
          />
          <Input
            label="Protein (g)"
            type="number"
            value={protein}
            onChange={(e) => setField("protein", e.target.value)}
            min="0"
            step="0.1"
          />
          <Input
            label="Carbs (g)"
            type="number"
            value={carbs}
            onChange={(e) => setField("carbs", e.target.value)}
            min="0"
            step="0.1"
          />
          <Input
            label="Fat (g)"
            type="number"
            value={fat}
            onChange={(e) => setField("fat", e.target.value)}
            min="0"
            step="0.1"
          />
        </div>
      </div>

      {/* Live macro preview */}
      <MacroPreviewCard
        calories={numCal}
        protein={numProtein}
        carbs={numCarbs}
        fat={numFat}
        quantity={numQty}
        unit={unit}
      />

      {/* Error banner */}
      {logFood.isError && (
        <div
          className="rounded-lg border px-3 py-2 text-sm"
          style={{
            backgroundColor: "#FFF0EE",
            borderColor: "var(--color-error-red, #BA1A1A)",
            color: "var(--color-error-red, #BA1A1A)",
          }}
        >
          {logFood.error?.message ?? "Failed to save"}
        </div>
      )}

      {/* Save button */}
      <Button
        onClick={handleSave}
        disabled={!isValid}
        loading={logFood.isPending}
        className="w-full"
      >
        Save
      </Button>
    </div>
  );
}
