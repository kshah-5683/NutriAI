/**
 * Types for the AI meal recommendation system.
 * Mirrors the Edge Function `recommend-meals` response shape
 * and Android Recommendation.kt domain model.
 */

export interface Recommendation {
  name: string;
  description: string;
  reason: string;
  suggested_quantity: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  source: "catalog" | "internet";
  food_item_id: string | null;
  recipe_text: string | null;
  search_query: string | null;
  cuisine_tag: string | null;
}

export interface RecommendMealsResponse {
  recommendations: Recommendation[];
  error?: string;
}

export type TimeOfDay = "morning" | "afternoon" | "evening" | "night";
