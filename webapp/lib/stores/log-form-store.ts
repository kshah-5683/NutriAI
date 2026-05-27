"use client";

import { create } from "zustand";
import type { ParsedFood, NutritionInfo } from "@/lib/types/ai";
import type { FoodItem } from "@/lib/types/domain";

// ─── Types ────────────────────────────────────────────────────────────────────

export type InputMode = "ai" | "manual" | "scan";

/**
 * A single ingredient row in the manual recipe builder.
 * Macros are stored as per-100g strings (same convention as the flat manual form).
 * For discrete units (piece, slice, bowl), macros represent per-unit values.
 */
export interface ManualRecipeIngredient {
  /** Stable React list key */
  id: string;
  /** Non-null when user selected an item from their ingredient catalog */
  catalogItem: FoodItem | null;
  /** Name entered manually when not from catalog */
  customName: string;
  quantity: string;
  unit: string;
  /** Base macros per 100g (or per-unit for discrete). Pre-filled from catalog or user-entered. */
  calories: string;
  protein: string;
  carbs: string;
  fat: string;
}

/** Returns a blank ingredient row with a unique id. */
function createEmptyIngredient(): ManualRecipeIngredient {
  return {
    id: Math.random().toString(36).slice(2),
    catalogItem: null,
    customName: "",
    quantity: "1",
    unit: "g",
    calories: "",
    protein: "",
    carbs: "",
    fat: "",
  };
}

interface LogFormState {
  // Tab state
  inputMode: InputMode;

  // AI parse state
  aiInput: string;
  isParsing: boolean;
  parsedFoods: ParsedFood[];
  selectedIndex: number | null;
  aiError: string | null;

  // Nutrition lookup results — keyed by food name
  nutritionResults: Record<string, NutritionInfo | null>;
  nutritionLoading: Record<string, boolean>;

  // Manual form state — flat ingredient mode
  foodName: string;
  brand: string;
  servingG: string;
  quantity: string;
  unit: string;
  calories: string;
  protein: string;
  carbs: string;
  fat: string;

  // Manual form state — recipe mode
  /** True when the manual form is building a recipe with an ingredient list */
  isRecipeMode: boolean;
  /** Recipe name (separate from flat-mode foodName) */
  recipeName: string;
  /** How many servings of the recipe the user ate (recipe-level quantity) */
  recipeQuantity: string;
  recipeUnit: string;
  /** Dynamic ingredient list — preserved across Recipe↔Ingredient mode toggles */
  recipeIngredients: ManualRecipeIngredient[];

  // Actions
  setInputMode: (mode: InputMode) => void;
  setAiInput: (text: string) => void;
  setIsParsing: (loading: boolean) => void;
  setParsedFoods: (foods: ParsedFood[]) => void;
  selectFood: (index: number | null) => void;
  setAiError: (error: string | null) => void;
  setNutritionResult: (name: string, info: NutritionInfo | null) => void;
  setNutritionLoading: (name: string, loading: boolean) => void;
  acceptParsedFood: (index: number) => void;
  acceptScanResult: (result: {
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
    suggested_quantity: number;
    suggested_unit: string;
  }) => void;
  setManualField: (field: string, value: string) => void;

  // Recipe mode actions
  /** Toggle Recipe/Ingredient mode. Ingredient list is preserved across toggles. */
  toggleRecipeMode: (isRecipe: boolean) => void;
  /** Append a new empty ingredient row. */
  addIngredient: () => void;
  /** Remove ingredient row by id. No-op when only one row remains. */
  removeIngredient: (id: string) => void;
  /** Update a single field on an ingredient row. */
  updateIngredient: (id: string, field: keyof ManualRecipeIngredient, value: string) => void;
  /**
   * Select a catalog FoodItem for an ingredient row.
   * Pre-fills macros from catalogItem.base* and switches customName to item name.
   * Auto-appends a new empty row if this was the last row.
   */
  selectCatalogIngredient: (id: string, foodItem: FoodItem) => void;
  /**
   * Clear the catalog selection for a row, keeping the custom name.
   * Macros are NOT cleared — user keeps whatever was pre-filled.
   */
  clearCatalogIngredient: (id: string) => void;

  resetForm: () => void;
}

// ─── Initial State ────────────────────────────────────────────────────────────

const initialManualState = {
  foodName: "",
  brand: "",
  servingG: "100",
  quantity: "1",
  unit: "serving",
  calories: "",
  protein: "",
  carbs: "",
  fat: "",
  // Recipe mode
  isRecipeMode: false,
  recipeName: "",
  recipeQuantity: "1",
  recipeUnit: "serving",
  recipeIngredients: [createEmptyIngredient()] as ManualRecipeIngredient[],
};

const initialAiState = {
  aiInput: "",
  isParsing: false,
  parsedFoods: [] as ParsedFood[],
  selectedIndex: null as number | null,
  aiError: null as string | null,
  nutritionResults: {} as Record<string, NutritionInfo | null>,
  nutritionLoading: {} as Record<string, boolean>,
};

// ─── Store ────────────────────────────────────────────────────────────────────

export const useLogFormStore = create<LogFormState>((set, get) => ({
  inputMode: "ai",
  ...initialAiState,
  ...initialManualState,

  setInputMode: (mode) => set({ inputMode: mode }),

  setAiInput: (text) => set({ aiInput: text }),

  setIsParsing: (loading) => set({ isParsing: loading }),

  setParsedFoods: (foods) =>
    set({
      parsedFoods: foods,
      selectedIndex: null,
      aiError: null,
    }),

  selectFood: (index) => set({ selectedIndex: index }),

  setAiError: (error) => set({ aiError: error, isParsing: false }),

  setNutritionResult: (name, info) =>
    set((state) => ({
      nutritionResults: { ...state.nutritionResults, [name]: info },
      nutritionLoading: { ...state.nutritionLoading, [name]: false },
    })),

  setNutritionLoading: (name, loading) =>
    set((state) => ({
      nutritionLoading: { ...state.nutritionLoading, [name]: loading },
    })),

  /**
   * Copies a parsed food into the manual form and switches to Manual tab.
   * If nutrition was resolved, pre-fills macros. Otherwise leaves them empty.
   *
   * For discrete units (piece, slice, bowl) with a known servingWeightG,
   * macros are pre-scaled from per-100g to per-unit so that the preview
   * and save paths produce correct totals. Matches Android Phase 12 fix.
   */
  acceptParsedFood: (index) => {
    const state = get();
    const food = state.parsedFoods[index];
    if (!food) return;

    const nutrition = state.nutritionResults[food.name] ?? null;

    // Discrete units where servingWeightG enables per-unit pre-scaling
    const discreteUnits = ["piece", "pieces", "slice", "slices", "bowl", "bowls"];
    const isDiscrete = discreteUnits.includes(food.unit.toLowerCase().trim());
    const swg = nutrition?.servingWeightG;
    const shouldPreScale = isDiscrete && swg != null && swg > 0;

    // Pre-scale factor: converts per-100g → per-unit (e.g. 50g egg → 0.5)
    const scale = shouldPreScale ? swg / 100 : 1;

    set({
      inputMode: "manual",
      foodName: food.name,
      brand: "",
      quantity: String(food.quantity),
      unit: food.unit,
      servingG: shouldPreScale ? String(Math.round(swg)) : "100",
      calories: nutrition ? String(Math.round(nutrition.caloriesPer100g * scale * 10) / 10) : "",
      protein: nutrition ? String(Math.round(nutrition.proteinPer100g * scale * 10) / 10) : "",
      carbs: nutrition ? String(Math.round(nutrition.carbsPer100g * scale * 10) / 10) : "",
      fat: nutrition ? String(Math.round(nutrition.fatPer100g * scale * 10) / 10) : "",
    });
  },

  /**
   * Bulk pre-fills manual form fields from a scan-label result.
   * Switches to Manual tab. Matches Android LabelScannerDelegate field mapping.
   * foodName left empty — label scan doesn't extract product name.
   */
  acceptScanResult: (result) => {
    set({
      inputMode: "manual",
      foodName: "",
      brand: "",
      servingG: "100",
      quantity: String(Math.round(result.suggested_quantity * 10) / 10),
      unit: result.suggested_unit === "grams" ? "g" : result.suggested_unit,
      calories: String(Math.round(result.calories)),
      protein: String(Math.round(result.protein * 10) / 10),
      carbs: String(Math.round(result.carbs * 10) / 10),
      fat: String(Math.round(result.fat * 10) / 10),
    });
  },

  setManualField: (field, value) => set({ [field]: value }),

  // ─── Recipe mode actions ────────────────────────────────────────────────────

  toggleRecipeMode: (isRecipe) =>
    set({ isRecipeMode: isRecipe }),

  addIngredient: () =>
    set((state) => ({
      recipeIngredients: [...state.recipeIngredients, createEmptyIngredient()],
    })),

  removeIngredient: (id) =>
    set((state) => {
      if (state.recipeIngredients.length <= 1) return state;
      return {
        recipeIngredients: state.recipeIngredients.filter((r) => r.id !== id),
      };
    }),

  updateIngredient: (id, field, value) =>
    set((state) => {
      const updated = state.recipeIngredients.map((r) =>
        r.id === id ? { ...r, [field]: value } : r
      );

      // Auto-grow: if the last row now has a customName, append a new empty row.
      // Triggered only by the 'customName' field — catalog selection uses selectCatalogIngredient.
      const last = updated[updated.length - 1];
      if (
        field === "customName" &&
        last?.id === id &&
        last.customName.trim().length > 0 &&
        last.catalogItem === null
      ) {
        return { recipeIngredients: [...updated, createEmptyIngredient()] };
      }

      return { recipeIngredients: updated };
    }),

  selectCatalogIngredient: (id, foodItem) =>
    set((state) => {
      const updated = state.recipeIngredients.map((r) =>
        r.id === id
          ? {
              ...r,
              catalogItem: foodItem,
              customName: foodItem.name,
              calories: String(foodItem.baseCalories),
              protein: String(foodItem.baseProtein),
              carbs: String(foodItem.baseCarbs),
              fat: String(foodItem.baseFat),
            }
          : r
      );

      // Auto-grow: if the updated row was the last one, append a new empty row.
      const lastIdx = updated.length - 1;
      const wasLast = updated[lastIdx]?.id === id;
      return {
        recipeIngredients: wasLast
          ? [...updated, createEmptyIngredient()]
          : updated,
      };
    }),

  clearCatalogIngredient: (id) =>
    set((state) => ({
      recipeIngredients: state.recipeIngredients.map((r) =>
        r.id === id ? { ...r, catalogItem: null } : r
      ),
    })),

  resetForm: () =>
    set({
      inputMode: "ai",
      ...initialAiState,
      ...initialManualState,
      // Ensure recipe ingredient list is re-initialised with a fresh empty row
      recipeIngredients: [createEmptyIngredient()],
    }),
}));
