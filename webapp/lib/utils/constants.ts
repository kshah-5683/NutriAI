/**
 * Shared constants for NutriAI web app.
 * Values aligned with Android app constants.
 */

import type { MacroGoals } from "../types/domain";

// ─── Macro visualization colors ──────────────────────────────────────────────
// Must match Android Color.kt values exactly.
export const MACRO_COLORS = {
  calories: "#E8673C", // CalorieColor — Tomato-orange
  protein: "#2E8B7A",  // ProteinColor — Teal-green
  carbs: "#D4A017",    // CarbsColor   — Honey-amber
  fat: "#8E5BA2",      // FatColor     — Fig-purple
} as const;

// ─── Default macro goals ──────────────────────────────────────────────────────
// Matches MacroGoals.kt defaults — typical adult TDEE guidelines.
export const DEFAULT_MACRO_GOALS: MacroGoals = {
  calorieGoal: 2000,
  proteinGoal: 150,
  carbsGoal: 250,
  fatGoal: 65,
};

// ─── Catalog ID helpers ───────────────────────────────────────────────────────
// Web always uses the real Supabase UUID — no "local_user" mapping needed.
// These helpers mirror the catalog ID format used in SyncRepositoryImpl.kt.

/**
 * Returns the ingredient catalog ID for a given user UUID.
 * e.g. "abc123_local_user_ingredients"
 */
export function getIngredientCatalogId(userId: string): string {
  return `${userId}_local_user_ingredients`;
}

/**
 * Returns the recipe catalog ID for a given user UUID.
 * e.g. "abc123_local_user_recipes"
 */
export function getRecipeCatalogId(userId: string): string {
  return `${userId}_local_user_recipes`;
}

// ─── Supabase Edge Function names ─────────────────────────────────────────────
export const EDGE_FUNCTIONS = {
  PARSE_FOOD: "parse-food",
  LOOKUP_NUTRITION: "lookup-nutrition",
  LOG_FOOD: "log-food",
  LOG_RECIPE: "log-recipe",
  SCAN_LABEL: "scan-label",
  UPDATE_FOOD: "update-food",
  UPDATE_DAILY_LOG: "update-daily-log",
  RECOMMEND_MEALS: "recommend-meals",
  PREFETCH_RECOMMENDATIONS: "prefetch-recommendations",
} as const;

// ─── Soft-delete sentinel ─────────────────────────────────────────────────────
/**
 * Returns the current epoch ms timestamp to use for deleted_at and last_modified_at
 * on soft-delete operations.
 * NEVER hard-delete — let pg_cron handle tombstone purge after 15 days.
 */
export function nowMs(): number {
  return Date.now();
}

// ─── Image compression settings ──────────────────────────────────────────────
// Must match Android ImageCompressor.kt: MAX_DIMENSION = 1024, JPEG_QUALITY = 80
export const IMAGE_COMPRESSION = {
  MAX_DIMENSION: 1024,
  JPEG_QUALITY: 0.8,
} as const;
