"use client";

import { create } from "zustand";
import type { ParsedFood, NutritionInfo } from "@/lib/types/ai";

// ─── Types ────────────────────────────────────────────────────────────────────

export type InputMode = "ai" | "manual" | "scan";

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

  // Manual form state
  foodName: string;
  brand: string;
  servingG: string;
  quantity: string;
  unit: string;
  calories: string;
  protein: string;
  carbs: string;
  fat: string;

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

  resetForm: () =>
    set({
      inputMode: "ai",
      ...initialAiState,
      ...initialManualState,
    }),
}));
