/**
 * Canonical macro calculation utilities.
 * Port of UnitConverter.computeServingMultiplier() from Android.
 *
 * CRITICAL: All base macros in food_items are stored PER-100g (normalized at write time).
 * These functions are the SOURCE OF TRUTH for macro calculations on the server.
 * The client-side macro-calculator.ts in webapp/lib/utils/ is for preview only.
 */

/** Per-100g base constant (food-label convention) */
export const PER_100G_BASE = 100.0;

/**
 * Converts quantity + unit to a multiplier against 100g baseline.
 * Direct port of UnitConverter.computeServingMultiplier() from Android.
 *
 * @param servingWeightG Optional gram-weight of one serving/discrete unit.
 *   When provided for "serving", discrete units (piece/slice/bowl), or unknown
 *   units, the multiplier is `quantity * servingWeightG / 100` instead of
 *   `quantity × 1.0` (which assumes 100g per unit).
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

/** Returns true if the unit represents grams. */
export function isGramUnit(unit: string): boolean {
  const u = unit.toLowerCase().trim();
  return u === "g" || u === "gram" || u === "grams";
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
 * CRITICAL: base macros are PER-100g (normalized at write time).
 * consumedQty is a 100g-relative multiplier (e.g. 200g → 2.0, 1 serving of 200g item → 2.0).
 *   total = base_macro_per_100g * consumed_qty_multiplier
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
