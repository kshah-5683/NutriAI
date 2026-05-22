/**
 * Canonical macro calculation utilities.
 * Port of LogViewModel.computeServingMultiplier() + LogFoodUseCase scaling from Android.
 *
 * These functions are the SOURCE OF TRUTH for macro calculations on the server.
 * The client-side macro-calculator.ts in webapp/lib/utils/ is for preview only.
 */

/** Per-100g base constant (food-label convention) */
export const PER_100G_BASE = 100.0;

/**
 * Converts quantity + unit to a multiplier against 100g baseline.
 * Direct port of LogViewModel.computeServingMultiplier() from Android.
 */
export function computeServingMultiplier(
  quantity: number,
  unit: string
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
      return quantity; // serving, piece, slice, bowl — treated as 1:1
  }
}

/**
 * Scales per-100g nutrition values to the consumed portion.
 * Used when grounding AI-parsed items with USDA FDC / IFCT data.
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

/**
 * Computes daily log totals from base macros and consumed quantity.
 *
 * CRITICAL: base macros are PER-SERVING (not per-gram).
 * scaleFactor = consumedQty (number of servings).
 *
 * This is the corrected formula — the Android app had a bug where
 * sync recalculation used base_macro * consumed_qty / base_serving_g
 * which is WRONG. The fix (Phase 8 Pre-work II bugfix #1) confirmed:
 *   total = base_macro * consumed_qty
 */
export function computeDailyLogTotals(
  baseMacros: {
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
  },
  consumedQty: number
) {
  return {
    totalCalories: baseMacros.calories * consumedQty,
    totalProtein: baseMacros.protein * consumedQty,
    totalCarbs: baseMacros.carbs * consumedQty,
    totalFat: baseMacros.fat * consumedQty,
  };
}
