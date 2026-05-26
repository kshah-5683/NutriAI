/**
 * AI-related type definitions for NutriAI web app.
 * Ports of Android AI domain models + Edge Function response shapes.
 */

import type { FoodItem } from "./domain";

/**
 * A food item parsed from natural language by the AI (parse-food Edge Function).
 * Contains entity extraction results only — no calorie/macro data.
 * Macros come from nutrition lookup (lookup-nutrition Edge Function).
 * Port of ParsedFood.kt
 */
export interface ParsedFood {
  name: string;
  quantity: number;
  unit: string;
  confidence: number;
  isRecipe: boolean;
  ingredients: ParsedFood[];
  /** Resolved catalog match — null if this is a new food */
  catalogMatch: CatalogMatch | null;
}

/**
 * Result of resolving a ParsedFood against the user's catalog.
 * Port of CatalogMatch.kt
 */
export interface CatalogMatch {
  isFromCatalog: boolean;
  foodItem: FoodItem;
}

/**
 * Nutritional data for a food item from an external source.
 * All macro values are per 100g of product.
 * Port of NutritionInfo.kt
 */
export interface NutritionInfo {
  productName: string;
  brand: string | null;
  caloriesPer100g: number;
  proteinPer100g: number;
  carbsPer100g: number;
  fatPer100g: number;
  fiberPer100g: number | null;
  source: "USDA FoodData Central" | "IFCT 2017" | string;
  externalId: string | null;
  /**
   * Gram weight of a single discrete serving unit (piece, slice, bowl).
   * Extracted from FDC servingSize when servingSizeUnit is "g".
   * Used to pre-scale per-100g macros to per-unit values for discrete units.
   * Null when FDC does not report a gram-based serving size or for IFCT foods.
   */
  servingWeightG: number | null;
}

/**
 * Nutrition data extracted from a food label photo (scan-label Edge Function).
 * All macro values default to 0.0 when not found on the label.
 * Port of ExtractedLabelData.kt
 */
export interface ExtractedLabelData {
  caloriesPerServing: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  /** Raw serving size text from the label, e.g., "1 cup (240ml)". Null if not found. */
  servingSizeText: string | null;
  /** Serving weight in grams if explicitly printed on the label. Null if not found. */
  servingWeightG: number | null;
}

/**
 * Response shape from the parse-food Edge Function.
 */
export interface ParseFoodResponse {
  foods: ParsedFood[];
}

/**
 * Response shape from the lookup-nutrition Edge Function.
 */
export interface LookupNutritionResponse {
  results: Array<{
    name: string;
    nutrition: NutritionInfo | null;
    source: string | null;
  }>;
}

/**
 * Request body for the log-food Edge Function.
 */
export interface LogFoodRequest {
  foodName: string;
  consumedQty: number;
  consumedUnit: string;
  baseServingG: number;
  baseCalories: number;
  baseProtein: number;
  baseCarbs: number;
  baseFat: number;
  externalApiId?: string | null;
  dateTimestamp: number;
  /** If set, reuses an existing food_items row instead of creating a new one */
  existingFoodItemId?: string;
  /** Catalog ID — e.g. "{uuid}_local_user_ingredients". Omit for default. */
  catalogId?: string;
  /** When true, creates the food_items row but skips the daily_logs insert */
  skipDailyLog?: boolean;
  /** Brand name (optional) — stored on the food_items row */
  brand?: string;
}

/**
 * Request body for the log-recipe Edge Function.
 */
export interface LogRecipeIngredientMatch {
  isFromCatalog: boolean;
  parsedName: string;
  matchedFoodItem?: Partial<FoodItem> | null;
  foodItem?: FoodItem | null;
}

export interface LogRecipeRequest {
  recipeName: string;
  ingredientMatches: LogRecipeIngredientMatch[];
  quantity: number;
  unit: string;
  dateTimestamp: number;
  skipDailyLog?: boolean;
}
