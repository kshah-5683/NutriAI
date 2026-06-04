/**
 * Domain model types for NutriAI web app.
 * Ported from Android domain/model/*.kt — field names converted to camelCase.
 *
 * CRITICAL RULES (from DEVLOG parity notes):
 * 1. Macro formula: total = base_macro * consumed_qty  (no division by serving size)
 * 2. No "local_user": Always use real Supabase UUID
 *    Catalog IDs: `{uuid}_local_user_ingredients` and `{uuid}_local_user_recipes`
 * 3. Soft-delete: Every query MUST include .is("deleted_at", null). Never hard-delete.
 */

/**
 * Meal category for daily log entries.
 * Fixed enum — one of four categories the user selects when logging food.
 * Snack is repeatable (user can log multiple snacks per day).
 */
export type MealType = "breakfast" | "snack" | "lunch" | "dinner";

/**
 * A user's food catalog — groups related food items.
 * Port of Catalog.kt
 */
export interface Catalog {
  id: string;
  userId: string;
  name: string;
  lastModifiedAt: number;
  deletedAt: number | null;
}

/**
 * A food item with baseline nutritional data.
 * Macros are stored per baseServingG grams (the reference serving size).
 * Port of FoodItem.kt
 */
export interface FoodItem {
  id: string;
  catalogId: string;
  name: string;
  brand: string | null;
  /** Reference serving size in grams */
  baseServingG: number;
  /** Calories per serving (not per 100g) */
  baseCalories: number;
  /** Protein (g) per serving */
  baseProtein: number;
  /** Carbs (g) per serving */
  baseCarbs: number;
  /** Fat (g) per serving */
  baseFat: number;
  externalApiId: string | null;
  lastModifiedAt: number;
  deletedAt: number | null;
}

/**
 * A single food log entry for a day.
 * totalCalories = baseCalories * consumedQty  (CORRECT formula)
 * Port of DailyLog.kt
 */
export interface DailyLog {
  id: string;
  userId: string;
  /** Null when the referenced food item has been tombstone-purged */
  foodItemId: string | null;
  /** Snapshot of food name at log-creation time — survives food rename/delete */
  foodName: string;
  dateTimestamp: number;
  consumedQty: number;
  consumedUnit: string;
  totalCalories: number;
  totalProtein: number;
  totalCarbs: number;
  totalFat: number;
  /** Meal category: breakfast, snack, lunch, or dinner. Null for legacy uncategorized rows. */
  mealType: MealType | null;
  lastModifiedAt: number;
  deletedAt: number | null;
}

/**
 * Aggregated macro totals for a day's logs.
 */
export interface DailyMacroSummary {
  date: Date;
  totalCalories: number;
  totalProtein: number;
  totalCarbs: number;
  totalFat: number;
  logCount: number;
}

/**
 * Aggregated macro totals for a calendar month.
 * Port of MonthlyMacroSummary.kt — used by Insights Year view.
 */
export interface MonthlyMacroSummary {
  /** Year and month string: "2026-05" */
  yearMonth: string;
  totalCalories: number;
  totalProtein: number;
  totalCarbs: number;
  totalFat: number;
  /** Number of days in this month that had at least one log entry. */
  daysWithData: number;
}

/**
 * User-configured daily nutrition goals.
 * Stored in the `user_preferences` Supabase table for cross-platform sync.
 * Port of MacroGoals.kt — defaults match typical adult TDEE guidelines.
 */
export interface MacroGoals {
  calorieGoal: number;
  proteinGoal: number;
  carbsGoal: number;
  fatGoal: number;
}

/**
 * User preferences record from Supabase (includes primary key + timestamps).
 */
export interface UserPreferences {
  id: string;
  userId: string;
  calorieGoal: number;
  proteinGoal: number;
  carbsGoal: number;
  fatGoal: number;
  lastModifiedAt: number;
  // Profile fields (Phase R4: AI Recommendations)
  age: number | null;
  gender: string | null;
  weightKg: number | null;
  weightGoal: string | null;
  dietType: string | null;
  cuisinePreferences: string[];
  allergies: string[];
  recommendationsEnabled: boolean;
}

/**
 * User's dietary profile — drives internet recommendation personalization.
 * Port of UserProfile.kt
 *
 * All fields are optional. When null/empty, the recommendation prompt omits
 * that constraint entirely (no defaults assumed).
 */
export interface UserProfile {
  age: number | null;
  gender: string | null;
  weightKg: number | null;
  weightGoal: string | null;       // "lose" | "maintain" | "gain"
  dietType: string | null;         // "vegetarian" | "veg_eggs" | "non_veg" | "pescatarian" | "vegan"
  cuisinePreferences: string[];
  allergies: string[];
  recommendationsEnabled: boolean;
}
