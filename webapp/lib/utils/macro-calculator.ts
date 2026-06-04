/**
 * Client-side macro calculation utilities — FOR PREVIEW ONLY.
 *
 * The server-side source of truth is in supabase/functions/_shared/macro-calculator.ts.
 * This file mirrors computeServingMultiplier() so the Log page can show
 * live macro previews without a round-trip. Never use this for persisted data.
 *
 * CRITICAL: All base macros in food_items are stored PER-100g (normalized at write time).
 * Port of UnitConverter.computeServingMultiplier() from Android.
 */

import type { ManualRecipeIngredient } from "@/lib/stores/log-form-store";

/** Per-100g base constant (food-label convention) */
export const PER_100G_BASE = 100.0;

/**
 * Converts quantity + unit to a multiplier against 100g baseline.
 * Must stay in sync with supabase/functions/_shared/macro-calculator.ts.
 *
 * @param servingWeightG Optional gram-weight of one serving/discrete unit.
 *   When provided for "serving", discrete units, or unknown units,
 *   the multiplier is `quantity * servingWeightG / 100`.
 */
export function computeServingMultiplier(
  quantity: number,
  unit: string,
  servingWeightG?: number
): number {
  const normalizedUnit = unit.toLowerCase().trim();
  switch (normalizedUnit) {
    case "g":
    case "gram":
    case "grams":
    case "ml":
    case "milliliter":
      return quantity / PER_100G_BASE;
    case "tsp":
    case "teaspoon":
      return (quantity * 5) / PER_100G_BASE;
    case "tbsp":
    case "tablespoon":
      return (quantity * 15) / PER_100G_BASE;
    case "cup":
    case "cups":
      return (quantity * 240) / PER_100G_BASE;
    default:
      // serving, piece, slice, bowl, unknown — use actual serving weight when available
      if (servingWeightG != null && servingWeightG > 0) {
        return (quantity * servingWeightG) / PER_100G_BASE;
      }
      return quantity;
  }
}

/**
 * Returns the scaled macros for a single ingredient row.
 * Multiplies base macros (per-100g or per-unit) by computeServingMultiplier(qty, unit).
 * Used to display per-row macro badges with quantity prefix.
 *
 * Skips rows with no name (catalog item and no customName) — returns all zeros.
 */
export function scaleIngredientMacros(ingredient: ManualRecipeIngredient): {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
} {
  const hasName =
    ingredient.catalogItem !== null || ingredient.customName.trim().length > 0;
  if (!hasName) return { calories: 0, protein: 0, carbs: 0, fat: 0 };

  const baseCal =
    ingredient.catalogItem?.baseCalories ?? (parseFloat(ingredient.calories) || 0);
  const baseProt =
    ingredient.catalogItem?.baseProtein ?? (parseFloat(ingredient.protein) || 0);
  const baseCarb =
    ingredient.catalogItem?.baseCarbs ?? (parseFloat(ingredient.carbs) || 0);
  const baseFat =
    ingredient.catalogItem?.baseFat ?? (parseFloat(ingredient.fat) || 0);

  const qty = parseFloat(ingredient.quantity) || 0;
  const servingG = ingredient.catalogItem?.baseServingG;
  const multiplier = computeServingMultiplier(qty, ingredient.unit, servingG);

  return {
    calories: Math.round(baseCal * multiplier * 10) / 10,
    protein: Math.round(baseProt * multiplier * 10) / 10,
    carbs: Math.round(baseCarb * multiplier * 10) / 10,
    fat: Math.round(baseFat * multiplier * 10) / 10,
  };
}

/**
 * Aggregates macros across all ingredient rows, scaling each by its quantity/unit.
 * Returns the total macros representing 1 serving of the recipe.
 *
 * Rows with no name (empty rows) are skipped.
 * This result is the per-serving total passed to MacroPreviewCard (which further
 * scales by recipe-level quantity for the daily log total preview).
 *
 * CRITICAL: These same pre-scaled per-ingredient values must be sent to the
 * log-recipe Edge Function in matchedFoodItem.base_* so the server-side direct
 * sum produces the correct recipe total. See plan section 6 for details.
 */
export function aggregateIngredientMacros(
  ingredients: ManualRecipeIngredient[]
): { calories: number; protein: number; carbs: number; fat: number } {
  return ingredients.reduce(
    (acc, ing) => {
      const scaled = scaleIngredientMacros(ing);
      return {
        calories: acc.calories + scaled.calories,
        protein: acc.protein + scaled.protein,
        carbs: acc.carbs + scaled.carbs,
        fat: acc.fat + scaled.fat,
      };
    },
    { calories: 0, protein: 0, carbs: 0, fat: 0 }
  );
}

/**
 * Scales per-100g nutrition values to the consumed portion.
 * Used in MacroPreviewCard for live preview.
 */
export function scaleNutrition(
  per100g: {
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
  },
  multiplier: number
): { calories: number; protein: number; carbs: number; fat: number } {
  return {
    calories: Math.round(per100g.calories * multiplier * 10) / 10,
    protein: Math.round(per100g.protein * multiplier * 10) / 10,
    carbs: Math.round(per100g.carbs * multiplier * 10) / 10,
    fat: Math.round(per100g.fat * multiplier * 10) / 10,
  };
}
