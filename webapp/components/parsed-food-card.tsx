"use client";

import type { ParsedFood, NutritionInfo } from "@/lib/types/ai";
import type { ClarificationResolution } from "@/lib/stores/log-form-store";
import { ClarificationInput, MatchTypeBadge } from "./clarification-input";
import { computeServingMultiplier } from "@/lib/utils/macro-calculator";

interface ParsedFoodCardProps {
  food: ParsedFood;
  index: number;
  isSelected: boolean;
  nutritionInfo: NutritionInfo | null | undefined;
  nutritionLoading: boolean;
  onSelect: (index: number) => void;
  /** Current clarification resolution for this item (undefined = not yet resolved) */
  clarificationResolution?: ClarificationResolution;
  /** Called when user accepts generic estimate */
  onUseGeneric: (index: number) => void;
  /** Called when user submits brand or weight clarification */
  onSubmitClarification: (index: number, input: string) => void;
  /**
   * Called when the user taps the pencil icon on an ingredient row.
   * Only relevant for recipe cards (food.isRecipe === true).
   */
  onEditIngredient?: (ingIndex: number, current: { quantity: number; unit: string }) => void;
  /** Full nutrition results map — used to compute recipe totals from ingredients. */
  ingredientNutritionResults?: Record<string, NutritionInfo | null | undefined>;
  /** Nutrition loading states — used to show "looking up" while ingredient lookups are in-flight. */
  ingredientNutritionLoading?: Record<string, boolean>;
}

/**
 * Selectable card showing a parsed food item with catalog badge, nutrition status,
 * and serving-size clarification prompt when needed.
 *
 * For recipes: shows expandable ingredient rows.
 */
export function ParsedFoodCard({
  food,
  index,
  isSelected,
  nutritionInfo,
  nutritionLoading,
  onSelect,
  clarificationResolution,
  onUseGeneric,
  onSubmitClarification,
  onEditIngredient,
  ingredientNutritionResults,
  ingredientNutritionLoading,
}: ParsedFoodCardProps) {
  const hasCatalogMatch = food.catalogMatch?.isFromCatalog === true;
  const needsClarification = food.needsClarification && !hasCatalogMatch;
  const isResolved = clarificationResolution !== undefined;
  const showClarificationBanner = needsClarification && !isResolved;

  // Determine if user provided a brand that wasn't found (generic fallback)
  const brandNotFound =
    clarificationResolution?.type === "brand" &&
    nutritionInfo?.matchType === "generic";

  // Recipe total — computed from per-ingredient nutrition results
  const anyIngredientLoading = food.isRecipe && food.ingredients.some(
    (ing) => !ing.catalogMatch?.isFromCatalog && (ingredientNutritionLoading?.[ing.name] ?? false)
  );
  let recipeTotal: { calories: number; protein: number; carbs: number; fat: number } | null = null;
  if (food.isRecipe && ingredientNutritionResults) {
    let totalCal = 0, totalProt = 0, totalCarb = 0, totalFat = 0, hasData = false;
    for (const ing of food.ingredients) {
      const cat = ing.catalogMatch?.isFromCatalog ? ing.catalogMatch.foodItem : null;
      const nut = ingredientNutritionResults[ing.name] ?? null;
      if (cat) {
        const m = computeServingMultiplier(ing.quantity, ing.unit, cat.baseServingG);
        totalCal += cat.baseCalories * m; totalProt += cat.baseProtein * m;
        totalCarb += cat.baseCarbs * m; totalFat += cat.baseFat * m;
        hasData = true;
      } else if (nut) {
        const m = computeServingMultiplier(ing.quantity, ing.unit, nut.servingWeightG ?? undefined);
        totalCal += nut.caloriesPer100g * m; totalProt += nut.proteinPer100g * m;
        totalCarb += nut.carbsPer100g * m; totalFat += nut.fatPer100g * m;
        hasData = true;
      }
    }
    if (hasData) {
      recipeTotal = {
        calories: Math.round(totalCal),
        protein: Math.round(totalProt * 10) / 10,
        carbs: Math.round(totalCarb * 10) / 10,
        fat: Math.round(totalFat * 10) / 10,
      };
    }
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => onSelect(index)}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect(index);
        }
      }}
      className="w-full cursor-pointer rounded-lg border p-3 text-left transition-colors"
      style={{
        backgroundColor: isSelected
          ? "var(--bg-primary-container)"
          : "var(--bg-surface)",
        borderColor: isSelected
          ? "var(--color-primary)"
          : "var(--border-variant)",
      }}
    >
      {/* Header row */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1">
          <div className="flex items-center gap-2">
            {isSelected && (
              <span
                className="flex h-5 w-5 items-center justify-center rounded-full text-xs text-white"
                style={{ backgroundColor: "var(--color-primary)" }}
              >
                ✓
              </span>
            )}
            <span className="text-sm font-medium" style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-primary)" }}>{food.name}</span>
          </div>
          <div className="mt-1 flex items-center gap-2 text-xs" style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-secondary)" }}>
            <span>
              {food.quantity} {food.unit}
            </span>
            <span>·</span>
            <span>{Math.round(food.confidence * 100)}%</span>
          </div>
        </div>

        {/* Catalog badge */}
        {hasCatalogMatch ? (
          <span
            className="whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium"
            style={{
              backgroundColor: "var(--bg-branded)",
              color: "var(--text-branded)",
            }}
          >
            In catalog ✅
          </span>
        ) : (
          <span
            className="whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium"
            style={{
              backgroundColor: "var(--bg-surface-variant)",
              color: "var(--text-secondary)",
            }}
          >
            New
          </span>
        )}
      </div>

      {/* Clarification banner — shown when AI flagged ambiguous serving size */}
      {showClarificationBanner && (
        <div onClick={(e) => e.stopPropagation()}>
          <ClarificationInput
            hint={food.clarificationHint ?? "Serving size varies by brand. Specify a brand or weight for better accuracy?"}
            onUseGeneric={() => onUseGeneric(index)}
            onSubmitClarification={(input) => onSubmitClarification(index, input)}
            isLoading={nutritionLoading}
          />
        </div>
      )}

      {/* Nutrition status — non-recipe items only; recipes show totals below ingredient list */}
      {!food.isRecipe && !hasCatalogMatch && !showClarificationBanner && (
        <div className="mt-2 text-xs" style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-secondary)" }}>
          {nutritionLoading ? (
            <span className="flex items-center gap-1">
              <LoadingDot /> Looking up nutrition...
            </span>
          ) : nutritionInfo ? (
            <div className="flex flex-col gap-1">
              <span style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-branded)" }}>
                {Math.round(nutritionInfo.caloriesPer100g)} kcal/100g ({nutritionInfo.source})
              </span>
              {/* Match type badge — shows quality of the match */}
              {nutritionInfo.matchType && (
                <MatchTypeBadge
                  matchType={nutritionInfo.matchType}
                  brandNotFound={brandNotFound}
                />
              )}
            </div>
          ) : needsClarification && !isResolved ? null : (
            <span>⚪ No nutrition data found</span>
          )}
        </div>
      )}

      {/* Recipe ingredients */}
      {food.isRecipe && food.ingredients.length > 0 && (
        <div className="mt-2 border-t pt-2" style={{ borderColor: "var(--border-variant)" }}>
          <div className="text-xs font-medium" style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-secondary)" }}>
            Ingredients:
          </div>
          <div className="mt-1 space-y-1">
            {food.ingredients.map((ing, i) => (
              <div key={i} className="flex items-center gap-2 text-xs" style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-secondary)" }}>
                <span>·</span>
                <span className="flex-1">
                  {ing.name} — {ing.quantity} {ing.unit}
                </span>
                {ing.catalogMatch?.isFromCatalog && (
                  <span className="text-xs" style={{ color: "var(--color-primary)" }}>✅</span>
                )}
                {onEditIngredient && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onEditIngredient(i, { quantity: ing.quantity, unit: ing.unit });
                    }}
                    className="rounded p-0.5 opacity-60 hover:opacity-100 transition-opacity"
                    style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-secondary)" }}
                    title="Edit quantity / unit"
                  >
                    ✏️
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recipe total — summed from ingredient nutrition results */}
      {food.isRecipe && food.ingredients.length > 0 && (
        <div className="mt-2 text-xs" style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-secondary)" }}>
          {anyIngredientLoading ? (
            <span className="flex items-center gap-1">
              <LoadingDot /> Looking up ingredients...
            </span>
          ) : recipeTotal ? (
            <span style={{ color: isSelected ? "var(--text-on-primary-container)" : "var(--text-branded)" }}>
              ~{recipeTotal.calories} kcal · {recipeTotal.protein}g P · {recipeTotal.carbs}g C · {recipeTotal.fat}g F
            </span>
          ) : (
            <span>⚪ No ingredient nutrition found</span>
          )}
        </div>
      )}
    </div>
  );
}

function LoadingDot() {
  return (
    <span
      className="inline-block h-2 w-2 animate-pulse rounded-full"
      style={{ backgroundColor: "var(--color-primary)" }}
    />
  );
}
