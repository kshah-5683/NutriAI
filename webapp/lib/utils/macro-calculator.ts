/**
 * Client-side macro calculation utilities — FOR PREVIEW ONLY.
 *
 * The server-side source of truth is in supabase/functions/_shared/macro-calculator.ts.
 * This file mirrors computeServingMultiplier() so the Log page can show
 * live macro previews without a round-trip. Never use this for persisted data.
 *
 * Port of LogViewModel.computeServingMultiplier() from Android.
 */

/** Per-100g base constant (food-label convention) */
export const PER_100G_BASE = 100.0;

/**
 * Converts quantity + unit to a multiplier against 100g baseline.
 * Must stay in sync with supabase/functions/_shared/macro-calculator.ts.
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
