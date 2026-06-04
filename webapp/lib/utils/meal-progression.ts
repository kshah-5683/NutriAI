/**
 * Meal progression logic — determines which meal to recommend next
 * and which meals the user has missed today.
 *
 * Shared between the webapp client and Edge Functions (duplicated in
 * supabase/functions/_shared/meal-progression.ts for Deno runtime).
 *
 * Key rule: Snacks are REPEATABLE. Unlike breakfast/lunch/dinner (one per day),
 * a user can log multiple snacks. The system suggests snack as a fallback
 * whenever the primary meal for the current time window is done.
 */

import type { MealType } from "../types/domain";

/**
 * Determines the next meal slot to prefetch/recommend based on
 * which meals have been logged today and the current hour.
 *
 * Returns null only during late night (10pm–6am).
 * During waking hours always returns a meal — falls back to "snack"
 * since snack is repeatable and never "fills up".
 *
 * @param loggedMeals Array of meal types already logged today.
 * @param hour       Current hour (0–23) from the client's local clock.
 */
export function determineNextMealSlot(
  loggedMeals: MealType[],
  hour: number
): MealType | null {
  if (hour >= 22 || hour < 6) return null; // late night — skip

  if (hour >= 6 && hour < 11) {
    if (!loggedMeals.includes("breakfast")) return "breakfast";
    return "snack"; // breakfast done, suggest snack
  }
  if (hour >= 11 && hour < 14) {
    if (!loggedMeals.includes("lunch")) return "lunch";
    return "snack"; // lunch done, suggest snack
  }
  if (hour >= 14 && hour < 17) {
    // Afternoon — snack is primary, but suggest lunch if not yet logged
    if (!loggedMeals.includes("lunch")) return "lunch";
    return "snack"; // snack is always valid here
  }
  if (hour >= 17 && hour < 22) {
    if (!loggedMeals.includes("dinner")) return "dinner";
    return "snack"; // dinner done, suggest snack
  }
  return null;
}

/**
 * Derives which standard (non-repeatable) meals the user has missed today.
 * Only includes meals whose primary time window has passed.
 * Snack is excluded — it's repeatable and always available.
 *
 * Examples:
 * - 7pm with nothing logged → ["breakfast", "lunch"]
 * - 2pm with only breakfast → [] (lunch window still active)
 * - 7pm with breakfast + lunch → [] (all non-repeatable meals covered before dinner window)
 * - 10am with breakfast logged → []
 *
 * @param loggedMeals Array of meal types already logged today.
 * @param hour       Current hour (0–23).
 */
export function deriveMissedMeals(
  loggedMeals: MealType[],
  hour: number
): MealType[] {
  const missed: MealType[] = [];

  // Breakfast window: 6am–11am. Missed if hour >= 11 and not logged.
  if (hour >= 11 && !loggedMeals.includes("breakfast")) {
    missed.push("breakfast");
  }

  // Lunch window: 11am–5pm. Missed if hour >= 17 and not logged.
  if (hour >= 17 && !loggedMeals.includes("lunch")) {
    missed.push("lunch");
  }

  // Dinner window: 5pm–10pm. Missed if hour >= 22 and not logged.
  // (Only relevant at late night — but the user likely won't see this
  //  since recommendations are disabled after 10pm.)
  if (hour >= 22 && !loggedMeals.includes("dinner")) {
    missed.push("dinner");
  }

  return missed;
}
