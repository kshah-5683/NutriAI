"use client";

import Link from "next/link";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { useDateStore } from "@/lib/stores/date-store";
import { useLogFood } from "@/lib/hooks/use-log-food";
import { useLogRecipe, type LogRecipeRequest } from "@/lib/hooks/use-log-recipe";
import { InputModeTabs } from "@/components/input-mode-tabs";
import { AiInputSection } from "@/components/ai-input-section";
import { ManualInputSection } from "@/components/manual-input-section";
import { ScanInputSection } from "@/components/scan-input-section";
import { startOfDayMs } from "@/lib/utils/format";
import { getIngredientCatalogId } from "@/lib/utils/constants";
import { computeServingMultiplier, PER_100G_BASE } from "@/lib/utils/macro-calculator";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useEffect, useState } from "react";

/**
 * Log page — AI Parse / Manual / Scan tabs.
 * Does NOT use DashboardShell — standalone full-screen form.
 * No bottom nav (it's only rendered inside DashboardShell).
 */
export default function LogPage() {
  const inputMode = useLogFormStore((s) => s.inputMode);
  const mealType = useLogFormStore((s) => s.mealType);
  const parsedFoods = useLogFormStore((s) => s.parsedFoods);
  const nutritionResults = useLogFormStore((s) => s.nutritionResults);
  const selectedDate = useDateStore((s) => s.selectedDate);

  const logFood = useLogFood();
  const logRecipe = useLogRecipe();
  const supabase = useSupabase();
  const [userId, setUserId] = useState<string | null>(null);

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      setUserId(data.user?.id ?? null);
    });
  }, [supabase]);

  /**
   * "Log All" — batch-log all parsed foods.
   * Recipes go through log-recipe; ingredients go through log-food.
   */
  const handleLogAll = async () => {
    if (parsedFoods.length === 0 || !userId) return;

    const dateTimestamp = startOfDayMs(selectedDate);

    for (const food of parsedFoods) {
      if (food.isRecipe) {
        // Build ingredient matches for the recipe Edge Function
        // Pre-scale each ingredient's macros by its parsed quantity/unit.
        // Mirrors Android LogViewModel lines 767-778: the log-recipe Edge Function
        // does a direct sum of base_* values, so each ingredient must represent
        // its actual contribution (not raw per-100g).
        const ingredientMatches = food.ingredients.map((ing) => {
          const ingNutrition = nutritionResults[ing.name] ?? null;
          const catalogMatch = ing.catalogMatch;

          if (catalogMatch?.isFromCatalog && catalogMatch.foodItem) {
            const multiplier = computeServingMultiplier(
              ing.quantity, ing.unit, catalogMatch.foodItem.baseServingG
            );
            return {
              isFromCatalog: true,
              parsedName: ing.name,
              foodItem: {
                ...catalogMatch.foodItem,
                baseServingG: (catalogMatch.foodItem.baseServingG ?? PER_100G_BASE) * multiplier,
                baseCalories: (catalogMatch.foodItem.baseCalories ?? 0) * multiplier,
                baseProtein: (catalogMatch.foodItem.baseProtein ?? 0) * multiplier,
                baseCarbs: (catalogMatch.foodItem.baseCarbs ?? 0) * multiplier,
                baseFat: (catalogMatch.foodItem.baseFat ?? 0) * multiplier,
              },
            };
          } else if (ingNutrition) {
            // Pre-scale nutrition macros by ingredient's parsed quantity/unit
            const multiplier = computeServingMultiplier(
              ing.quantity, ing.unit, ingNutrition.servingWeightG ?? undefined
            );
            return {
              isFromCatalog: false,
              parsedName: ing.name,
              matchedFoodItem: {
                id: crypto.randomUUID(),
                name: ing.name,
                base_serving_g: PER_100G_BASE * multiplier,
                base_calories: ingNutrition.caloriesPer100g * multiplier,
                base_protein: ingNutrition.proteinPer100g * multiplier,
                base_carbs: ingNutrition.carbsPer100g * multiplier,
                base_fat: ingNutrition.fatPer100g * multiplier,
                brand: ingNutrition.brand,
                external_api_id: ingNutrition.externalId,
              },
            };
          } else {
            // No data — 0-macro placeholder
            return {
              isFromCatalog: false,
              parsedName: ing.name,
              matchedFoodItem: null,
            };
          }
        });

        const request: LogRecipeRequest = {
          recipeName: food.name,
          ingredientMatches,
          quantity: food.quantity,
          unit: food.unit,
          dateTimestamp,
          mealType,
        };
        await logRecipe.mutateAsync(request);
      } else {
        // Single food item
        const nutrition = nutritionResults[food.name] ?? null;
        const catalogItem = food.catalogMatch?.foodItem;

        // Catalog macros are per-100g; the log-food Edge Function expects per-serving.
        // De-normalize: perServing = per100g × (servingG / 100).
        // Mirrors Android acceptAndLogAllParsed() lines 818-821.
        const servingG = catalogItem?.baseServingG ?? 100;
        const deNorm = servingG / 100;

        await logFood.mutateAsync({
          foodName: food.name,
          brand: nutrition?.brand ?? undefined,
          baseServingG: servingG,
          baseCalories: catalogItem
            ? (catalogItem.baseCalories ?? 0) * deNorm
            : (nutrition?.caloriesPer100g ?? 0),
          baseProtein: catalogItem
            ? (catalogItem.baseProtein ?? 0) * deNorm
            : (nutrition?.proteinPer100g ?? 0),
          baseCarbs: catalogItem
            ? (catalogItem.baseCarbs ?? 0) * deNorm
            : (nutrition?.carbsPer100g ?? 0),
          baseFat: catalogItem
            ? (catalogItem.baseFat ?? 0) * deNorm
            : (nutrition?.fatPer100g ?? 0),
          consumedQty: food.quantity,
          consumedUnit: food.unit,
          dateTimestamp,
          existingFoodItemId: food.catalogMatch?.isFromCatalog
            ? (catalogItem?.id ?? undefined)
            : undefined,
          externalApiId: nutrition?.externalId ?? undefined,
          catalogId: getIngredientCatalogId(userId),
          mealType,
        });
      }
    }
  };

  const logAllLoading = logFood.isPending || logRecipe.isPending;

  return (
    <div className="flex min-h-screen flex-col" style={{ backgroundColor: "var(--bg-app)" }}>
      {/* Header */}
      <header
        className="flex items-center gap-3 border-b px-4 py-3"
        style={{
          backgroundColor: "var(--bg-surface)",
          borderColor: "var(--border-variant)",
        }}
      >
        <Link
          href="/"
          className="flex h-8 w-8 items-center justify-center rounded-md transition-colors hover:bg-sage-gray"
          aria-label="Back to home"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path
              d="M19 12H5M5 12L12 19M5 12L12 5"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </Link>
        <h1 className="text-base font-semibold">Log Food</h1>
      </header>

      {/* Content */}
      <main className="flex-1 px-4 py-4">
        <div className="mx-auto max-w-lg space-y-4">
          <InputModeTabs />

          {inputMode === "ai" && (
            <AiInputSection
              onLogAll={handleLogAll}
              onLogAllLoading={logAllLoading}
            />
          )}

          {inputMode === "manual" && <ManualInputSection />}

          {inputMode === "scan" && <ScanInputSection />}
        </div>
      </main>
    </div>
  );
}
