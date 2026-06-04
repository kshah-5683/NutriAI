/**
 * Meal progression logic for Edge Functions (Deno runtime).
 *
 * Duplicated from webapp/lib/utils/meal-progression.ts — keep in sync.
 * Cannot share directly because Edge Functions run on Deno with URL imports
 * while the webapp uses Node/Next.js module resolution.
 *
 * Key rule: Snacks are REPEATABLE. Unlike breakfast/lunch/dinner (one per day),
 * snack never "fills up" and is always available as a fallback.
 */

type MealType = "breakfast" | "snack" | "lunch" | "dinner";

/**
 * Determines the next meal slot to prefetch based on logged meals and current hour.
 * Returns null during late night (10pm–6am).
 */
export function determineNextMealSlot(
  loggedMeals: MealType[],
  hour: number
): MealType | null {
  if (hour >= 22 || hour < 6) return null;

  if (hour >= 6 && hour < 11) {
    if (!loggedMeals.includes("breakfast")) return "breakfast";
    return "snack";
  }
  if (hour >= 11 && hour < 14) {
    if (!loggedMeals.includes("lunch")) return "lunch";
    return "snack";
  }
  if (hour >= 14 && hour < 17) {
    if (!loggedMeals.includes("lunch")) return "lunch";
    return "snack";
  }
  if (hour >= 17 && hour < 22) {
    if (!loggedMeals.includes("dinner")) return "dinner";
    return "snack";
  }
  return null;
}
