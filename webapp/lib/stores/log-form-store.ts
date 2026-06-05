"use client";

import { create } from "zustand";
import type { ParsedFood, NutritionInfo } from "@/lib/types/ai";
import type { FoodItem, MealType } from "@/lib/types/domain";

// ─── Types ────────────────────────────────────────────────────────────────────

export type InputMode = "ai" | "manual" | "scan";

/**
 * How a serving-size ambiguity was resolved for a specific parsed food item.
 * - "generic" — user accepted the generic USDA estimate
 * - "brand" — user provided a brand name for brand-specific lookup
 * - "weight" — user provided an explicit gram weight per serving unit
 */
export type ClarificationType = "generic" | "brand" | "weight";

export interface ClarificationResolution {
  type: ClarificationType;
  /** Brand name provided by the user (only when type === "brand") */
  brand?: string;
  /** Gram weight per unit provided by the user (only when type === "weight") */
  weightOverrideG?: number;
}

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

/**
 * Auto-detect the most likely meal type from the current hour.
 * Used as the default selection when the log form opens.
 */
function inferMealType(): MealType {
  const hour = new Date().getHours();
  if (hour >= 6 && hour < 11) return "breakfast";
  if (hour >= 11 && hour < 14) return "lunch";
  if (hour >= 14 && hour < 17) return "snack";
  return "dinner";
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

  // Meal type — shared across all input modes
  mealType: MealType;

  // AI parse state
  aiInput: string;
  isParsing: boolean;
  parsedFoods: ParsedFood[];
  /**
   * True immediately after a fresh AI parse sets parsedFoods.
   * Set to false by updateParsedIngredient so the nutrition lookup
   * useEffect does not re-fire on every inline ingredient edit.
   */
  parsedFoodsFromParse: boolean;
  selectedIndex: number | null;
  aiError: string | null;

  // Nutrition lookup results — keyed by food name
  nutritionResults: Record<string, NutritionInfo | null>;
  nutritionLoading: Record<string, boolean>;

  // Serving size clarification state — keyed by parsed food index
  // Absent key = not yet resolved (clarification banner is showing)
  clarificationResolutions: Record<number, ClarificationResolution>;

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
  setMealType: (type: MealType) => void;
  setInputMode: (mode: InputMode) => void;
  setAiInput: (text: string) => void;
  setIsParsing: (loading: boolean) => void;
  setParsedFoods: (foods: ParsedFood[]) => void;
  selectFood: (index: number | null) => void;
  setAiError: (error: string | null) => void;
  /**
   * Patch quantity and unit for a single ingredient inside a parsed recipe card.
   * Sets parsedFoodsFromParse to false so the nutrition lookup useEffect
   * does not re-fire for every edit.
   */
  updateParsedIngredient: (foodIndex: number, ingIndex: number, patch: { quantity: number; unit: string }) => void;
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

  // Clarification actions
  /**
   * Resolve a serving-size clarification for a parsed food item.
   * - "generic": accept the generic USDA estimate, proceed with standard lookup
   * - "brand": re-lookup with brand-specific FDC query
   * - "weight": override servingWeightG client-side (no re-lookup needed)
   */
  resolveClarification: (
    index: number,
    type: ClarificationType,
    value?: string
  ) => void;

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
  parsedFoodsFromParse: false as boolean,
  selectedIndex: null as number | null,
  aiError: null as string | null,
  nutritionResults: {} as Record<string, NutritionInfo | null>,
  nutritionLoading: {} as Record<string, boolean>,
  clarificationResolutions: {} as Record<number, ClarificationResolution>,
};

// ─── Store ────────────────────────────────────────────────────────────────────

export const useLogFormStore = create<LogFormState>((set, get) => ({
  inputMode: "ai",
  mealType: inferMealType(),
  ...initialAiState,
  ...initialManualState,

  setMealType: (type) => set({ mealType: type }),

  setInputMode: (mode) => set({ inputMode: mode }),

  setAiInput: (text) => set({ aiInput: text }),

  setIsParsing: (loading) => set({ isParsing: loading }),

  setParsedFoods: (foods) =>
    set({
      parsedFoods: foods,
      parsedFoodsFromParse: true,
      selectedIndex: null,
      aiError: null,
      isParsing: false,
    }),

  selectFood: (index) => set({ selectedIndex: index }),

  setAiError: (error) => set({ aiError: error, isParsing: false }),

  updateParsedIngredient: (foodIndex, ingIndex, patch) =>
    set((state) => {
      const foods = [...state.parsedFoods];
      const food = foods[foodIndex];
      if (!food) return state;
      const ingredients = [...food.ingredients];
      ingredients[ingIndex] = { ...ingredients[ingIndex], ...patch };
      foods[foodIndex] = { ...food, ingredients };
      return { parsedFoods: foods, parsedFoodsFromParse: false };
    }),

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
    const catalogItem = food.catalogMatch?.isFromCatalog ? food.catalogMatch.foodItem : null;

    // Check for weight override from clarification resolution
    const clarification = state.clarificationResolutions[index];
    const weightOverride = clarification?.weightOverrideG;

    // Discrete units where servingWeightG enables per-unit pre-scaling
    const discreteUnits = ["piece", "pieces", "slice", "slices", "bowl", "bowls"];
    const isDiscrete = discreteUnits.includes(food.unit.toLowerCase().trim());
    // Weight override from clarification takes priority over FDC servingWeightG
    const swg = weightOverride ?? nutrition?.servingWeightG;
    const shouldPreScale = isDiscrete && swg != null && swg > 0;

    // Pre-scale factor: converts per-100g → per-unit (e.g. 50g egg → 0.5)
    const scale = shouldPreScale ? swg / 100 : 1;

    // Resolve macros: prefer nutrition lookup, fall back to catalog item's base macros.
    // Catalog items skip the nutrition lookup (already in user's DB), so their macros
    // must come from catalogItem.base* fields. Both sources are per 100g.
    const cal = nutrition ? nutrition.caloriesPer100g : (catalogItem?.baseCalories ?? 0);
    const pro = nutrition ? nutrition.proteinPer100g : (catalogItem?.baseProtein ?? 0);
    const crb = nutrition ? nutrition.carbsPer100g : (catalogItem?.baseCarbs ?? 0);
    const ft = nutrition ? nutrition.fatPer100g : (catalogItem?.baseFat ?? 0);
    const hasMacros = nutrition != null || catalogItem != null;

    set({
      inputMode: "manual",
      foodName: food.name,
      brand: catalogItem?.brand ?? "",
      quantity: String(food.quantity),
      unit: food.unit,
      servingG: shouldPreScale ? String(Math.round(swg)) : (catalogItem ? String(catalogItem.baseServingG) : "100"),
      calories: hasMacros ? String(Math.round(cal * scale * 10) / 10) : "",
      protein: hasMacros ? String(Math.round(pro * scale * 10) / 10) : "",
      carbs: hasMacros ? String(Math.round(crb * scale * 10) / 10) : "",
      fat: hasMacros ? String(Math.round(ft * scale * 10) / 10) : "",
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

  // ─── Clarification actions ─────────────────────────────────────────────────

  resolveClarification: (index, type, value) =>
    set((state) => {
      const resolution: ClarificationResolution = { type };

      if (type === "brand" && value) {
        resolution.brand = value;
      } else if (type === "weight" && value) {
        resolution.weightOverrideG = parseFloat(value) || undefined;
      }

      return {
        clarificationResolutions: {
          ...state.clarificationResolutions,
          [index]: resolution,
        },
      };
    }),

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
      mealType: inferMealType(),
      ...initialAiState,
      ...initialManualState,
      // Ensure recipe ingredient list is re-initialised with a fresh empty row
      recipeIngredients: [createEmptyIngredient()],
    }),
}));
