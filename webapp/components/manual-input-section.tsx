"use client";

import { useEffect, useState } from "react";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { useDateStore } from "@/lib/stores/date-store";
import { useLogFood } from "@/lib/hooks/use-log-food";
import { useLogRecipe } from "@/lib/hooks/use-log-recipe";
import { MacroPreviewCard } from "@/components/macro-preview-card";
import { MealTypeSelector } from "@/components/meal-type-selector";
import { ManualRecipeIngredientRow } from "@/components/manual-recipe-ingredient-row";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { startOfDayMs } from "@/lib/utils/format";
import { aggregateIngredientMacros, computeServingMultiplier, PER_100G_BASE } from "@/lib/utils/macro-calculator";
import { getIngredientCatalogId, getRecipeCatalogId } from "@/lib/utils/constants";
import { useSupabase } from "@/components/providers/supabase-provider";
import type { LogRecipeRequest } from "@/lib/hooks/use-log-recipe";

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
 * Manual entry form — supports two modes:
 *
 * Ingredient mode (default): flat form — food name, brand, qty, unit, macros.
 * Recipe mode: ingredient builder — recipe name, dynamic ingredient rows, recipe qty.
 *
 * A Recipe / Ingredient segmented toggle at the top switches between modes.
 * Ingredient list is preserved across mode toggles — only resetForm() clears it.
 */
export function ManualInputSection() {
  // ─── Flat ingredient mode fields ───────────────────────────────────────────
  const foodName = useLogFormStore((s) => s.foodName);
  const brand = useLogFormStore((s) => s.brand);
  const servingG = useLogFormStore((s) => s.servingG);
  const quantity = useLogFormStore((s) => s.quantity);
  const unit = useLogFormStore((s) => s.unit);
  const calories = useLogFormStore((s) => s.calories);
  const protein = useLogFormStore((s) => s.protein);
  const carbs = useLogFormStore((s) => s.carbs);
  const fat = useLogFormStore((s) => s.fat);
  const setField = useLogFormStore((s) => s.setManualField);

  // ─── Meal type (shared across modes) ─────────────────────────────────────────
  const mealType = useLogFormStore((s) => s.mealType);

  // ─── Recipe mode fields ─────────────────────────────────────────────────────
  const isRecipeMode = useLogFormStore((s) => s.isRecipeMode);
  const recipeName = useLogFormStore((s) => s.recipeName);
  const recipeQuantity = useLogFormStore((s) => s.recipeQuantity);
  const recipeUnit = useLogFormStore((s) => s.recipeUnit);
  const recipeIngredients = useLogFormStore((s) => s.recipeIngredients);
  const toggleRecipeMode = useLogFormStore((s) => s.toggleRecipeMode);
  const addIngredient = useLogFormStore((s) => s.addIngredient);

  const selectedDate = useDateStore((s) => s.selectedDate);
  const logFood = useLogFood();
  const logRecipe = useLogRecipe();

  const supabase = useSupabase();
  const [userId, setUserId] = useState<string | null>(null);
  const [zeroMacroWarnings, setZeroMacroWarnings] = useState<string[]>([]);

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      setUserId(data.user?.id ?? null);
    });
  }, [supabase]);

  // ─── Flat mode computed values ──────────────────────────────────────────────
  const numQty = parseFloat(quantity) || 0;
  const numCal = parseFloat(calories) || 0;
  const numProtein = parseFloat(protein) || 0;
  const numCarbs = parseFloat(carbs) || 0;
  const numFat = parseFloat(fat) || 0;

  const isFlatValid = foodName.trim().length > 0 && numQty > 0;

  // ─── Recipe mode computed values ────────────────────────────────────────────
  const validIngredients = recipeIngredients.filter(
    (r) => (r.catalogItem !== null || r.customName.trim().length > 0) &&
            (parseFloat(r.quantity) || 0) > 0
  );
  const isRecipeValid = recipeName.trim().length > 0 && validIngredients.length > 0;

  const aggregatedMacros = aggregateIngredientMacros(recipeIngredients);
  const recipeQty = parseFloat(recipeQuantity) || 1;

  // ─── Handlers ───────────────────────────────────────────────────────────────

  const handleSaveFlat = () => {
    if (!isFlatValid) return;
    // servingG is set by acceptParsedFood from catalog item; defaults to "100"
    // for manual entry. Form macros are per-100g; the log-food Edge Function
    // expects per-serving. De-normalize: perServing = per100g × (servingG / 100).
    const numServingG = parseFloat(servingG) || 100;
    const deNorm = numServingG / 100;
    logFood.mutate({
      foodName: foodName.trim(),
      brand: brand.trim() || undefined,
      baseServingG: numServingG,
      baseCalories: numCal * deNorm,
      baseProtein: numProtein * deNorm,
      baseCarbs: numCarbs * deNorm,
      baseFat: numFat * deNorm,
      consumedQty: numQty,
      consumedUnit: unit,
      dateTimestamp: startOfDayMs(selectedDate),
      mealType,
    });
  };

  const handleSaveRecipe = () => {
    if (!isRecipeValid || !userId) return;

    // Soft-warning: collect ingredient names with all-zero macros
    const warnings = validIngredients
      .filter((r) => {
        const cal = r.catalogItem?.baseCalories ?? (parseFloat(r.calories) || 0);
        const prot = r.catalogItem?.baseProtein ?? (parseFloat(r.protein) || 0);
        const carb = r.catalogItem?.baseCarbs ?? (parseFloat(r.carbs) || 0);
        const f = r.catalogItem?.baseFat ?? (parseFloat(r.fat) || 0);
        return cal === 0 && prot === 0 && carb === 0 && f === 0;
      })
      .map((r) => r.catalogItem?.name ?? r.customName);

    setZeroMacroWarnings(warnings);

    const dateTimestamp = startOfDayMs(selectedDate);

    // Build ingredientMatches with PRE-SCALED macros.
    // Each ingredient's base_* is scaled by computeServingMultiplier(qty, unit)
    // so the Edge Function's direct sum produces the correct per-serving recipe total.
    const ingredientMatches = validIngredients.map((r) => {
      const baseCal = r.catalogItem?.baseCalories ?? (parseFloat(r.calories) || 0);
      const baseProt = r.catalogItem?.baseProtein ?? (parseFloat(r.protein) || 0);
      const baseCarb = r.catalogItem?.baseCarbs ?? (parseFloat(r.carbs) || 0);
      const baseFat = r.catalogItem?.baseFat ?? (parseFloat(r.fat) || 0);

      const qty = parseFloat(r.quantity) || 0;
      const multiplier = computeServingMultiplier(qty, r.unit);

      const scaledCal = Math.round(baseCal * multiplier * 10) / 10;
      const scaledProt = Math.round(baseProt * multiplier * 10) / 10;
      const scaledCarb = Math.round(baseCarb * multiplier * 10) / 10;
      const scaledFat = Math.round(baseFat * multiplier * 10) / 10;

      const parsedName = r.catalogItem?.name ?? r.customName.trim();

      if (r.catalogItem) {
        return {
          isFromCatalog: true,
          parsedName,
          foodItem: {
            ...r.catalogItem,
            // Override base_* with pre-scaled values (actual contribution to recipe)
            base_calories: scaledCal,
            base_protein: scaledProt,
            base_carbs: scaledCarb,
            base_fat: scaledFat,
            base_serving_g: PER_100G_BASE,
            baseCalories: scaledCal,
            baseProtein: scaledProt,
            baseCarbs: scaledCarb,
            baseFat: scaledFat,
            baseServingG: PER_100G_BASE,
          },
        };
      } else {
        return {
          isFromCatalog: false,
          parsedName,
          matchedFoodItem: {
            id: Math.random().toString(36).slice(2),
            name: parsedName,
            base_calories: scaledCal,
            base_protein: scaledProt,
            base_carbs: scaledCarb,
            base_fat: scaledFat,
            base_serving_g: PER_100G_BASE,
            baseCalories: scaledCal,
            baseProtein: scaledProt,
            baseCarbs: scaledCarb,
            baseFat: scaledFat,
            baseServingG: PER_100G_BASE,
            brand: null,
            external_api_id: null,
            externalApiId: null,
          },
        };
      }
    });

    const request: LogRecipeRequest = {
      recipeName: recipeName.trim(),
      ingredientMatches,
      quantity: recipeQty,
      unit: recipeUnit,
      dateTimestamp,
      mealType,
    };

    logRecipe.mutate(request);
  };

  const isSaving = isRecipeMode ? logRecipe.isPending : logFood.isPending;
  const saveError = isRecipeMode ? logRecipe.error : logFood.error;

  return (
    <div className="space-y-4">
      {/* ── Meal type selector ── */}
      <MealTypeSelector />

      {/* ── Recipe / Ingredient toggle ── */}
      <div
        className="flex rounded-lg p-1"
        style={{ backgroundColor: "var(--bg-surface-variant)" }}
      >
        {(
          [
            { isRecipe: false, icon: "🥚", label: "Ingredient" },
            { isRecipe: true, icon: "🍳", label: "Recipe" },
          ] as const
        ).map(({ isRecipe, icon, label }) => {
          const isActive = isRecipeMode === isRecipe;
          return (
            <button
              key={label}
              type="button"
              onClick={() => toggleRecipeMode(isRecipe)}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors"
              style={{
                backgroundColor: isActive ? "var(--bg-surface)" : "transparent",
                color: isActive ? "var(--color-primary, #2D5A27)" : "var(--text-secondary)",
                boxShadow: isActive ? "0 1px 3px rgba(0,0,0,0.1)" : "none",
              }}
            >
              <span>{icon}</span>
              <span>{label}</span>
            </button>
          );
        })}
      </div>

      {/* ═══════════════════════════════════════════════════════════ */}
      {/* INGREDIENT MODE                                            */}
      {/* ═══════════════════════════════════════════════════════════ */}
      {!isRecipeMode && (
        <>
          <Input
            label="Food name *"
            value={foodName}
            onChange={(e) => setField("foodName", e.target.value)}
            placeholder="e.g. Scrambled eggs"
          />

          <Input
            label="Brand (optional)"
            value={brand}
            onChange={(e) => setField("brand", e.target.value)}
            placeholder="e.g. Amul"
          />

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

          <MacroPreviewCard
            calories={numCal}
            protein={numProtein}
            carbs={numCarbs}
            fat={numFat}
            quantity={numQty}
            unit={unit}
          />

          {logFood.isError && (
            <ErrorBanner message={logFood.error?.message ?? "Failed to save"} />
          )}

          <Button
            onClick={handleSaveFlat}
            disabled={!isFlatValid}
            loading={logFood.isPending}
            className="w-full"
          >
            Save
          </Button>
        </>
      )}

      {/* ═══════════════════════════════════════════════════════════ */}
      {/* RECIPE MODE                                                */}
      {/* ═══════════════════════════════════════════════════════════ */}
      {isRecipeMode && (
        <>
          <Input
            label="Recipe name *"
            value={recipeName}
            onChange={(e) => setField("recipeName", e.target.value)}
            placeholder="e.g. Besan Chila"
          />

          {/* Ingredients section */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold uppercase tracking-wide" style={{ color: "var(--text-secondary)" }}>
                Ingredients
                <span className="ml-1 font-normal normal-case opacity-70">
                  (defines 1 serving)
                </span>
              </p>
            </div>

            {recipeIngredients.map((ingredient, idx) => (
              <ManualRecipeIngredientRow
                key={ingredient.id}
                ingredient={ingredient}
                isOnly={recipeIngredients.length === 1}
                userId={userId}
              />
            ))}

            <button
              type="button"
              onClick={addIngredient}
              className="flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed py-2.5 text-sm font-medium transition-colors hover:border-primary hover:text-primary"
              style={{
                borderColor: "var(--border-outline)",
                color: "var(--text-secondary)",
              }}
            >
              <span>+</span>
              <span>Add Ingredient</span>
            </button>
          </div>

          {/* Recipe-level quantity */}
          <div className="space-y-1">
            <p className="text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
              How many servings did you eat?
            </p>
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Servings *"
                type="number"
                value={recipeQuantity}
                onChange={(e) => setField("recipeQuantity", e.target.value)}
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
                  value={recipeUnit}
                  onChange={(e) => setField("recipeUnit", e.target.value)}
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
          </div>

          {/* Aggregated macro preview */}
          <MacroPreviewCard
            calories={aggregatedMacros.calories}
            protein={aggregatedMacros.protein}
            carbs={aggregatedMacros.carbs}
            fat={aggregatedMacros.fat}
            quantity={recipeQty}
            unit={recipeUnit}
          />

          {/* Zero-macro soft warnings */}
          {zeroMacroWarnings.length > 0 && (
            <div
              className="rounded-lg border px-3 py-2 text-sm"
              style={{
                backgroundColor: "#FFFBEB",
                borderColor: "#F59E0B",
                color: "#92400E",
              }}
            >
              {zeroMacroWarnings.map((name) => (
                <p key={name}>
                  ⚠️ <strong>{name}</strong> has no macros — will be logged as 0 calories
                </p>
              ))}
            </div>
          )}

          {saveError && (
            <ErrorBanner message={saveError.message ?? "Failed to save recipe"} />
          )}

          <Button
            onClick={handleSaveRecipe}
            disabled={!isRecipeValid || !userId}
            loading={logRecipe.isPending}
            className="w-full"
          >
            Save Recipe
          </Button>
        </>
      )}
    </div>
  );
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div
      className="rounded-lg border px-3 py-2 text-sm"
      style={{
        backgroundColor: "#FFF0EE",
        borderColor: "var(--color-error-red, #BA1A1A)",
        color: "var(--color-error-red, #BA1A1A)",
      }}
    >
      {message}
    </div>
  );
}
