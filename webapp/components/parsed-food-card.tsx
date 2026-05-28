"use client";

import type { ParsedFood, NutritionInfo } from "@/lib/types/ai";
import type { ClarificationResolution } from "@/lib/stores/log-form-store";
import { ClarificationInput, MatchTypeBadge } from "./clarification-input";

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
}: ParsedFoodCardProps) {
  const hasCatalogMatch = food.catalogMatch?.isFromCatalog === true;
  const needsClarification = food.needsClarification && !hasCatalogMatch;
  const isResolved = clarificationResolution !== undefined;
  const showClarificationBanner = needsClarification && !isResolved;

  // Determine if user provided a brand that wasn't found (generic fallback)
  const brandNotFound =
    clarificationResolution?.type === "brand" &&
    nutritionInfo?.matchType === "generic";

  return (
    <button
      onClick={() => onSelect(index)}
      className="w-full rounded-lg border p-3 text-left transition-colors"
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

      {/* Nutrition status */}
      {!hasCatalogMatch && !showClarificationBanner && (
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
                <span>
                  {ing.name} — {ing.quantity} {ing.unit}
                </span>
                {ing.catalogMatch?.isFromCatalog && (
                  <span className="text-xs" style={{ color: "var(--color-primary)" }}>✅</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </button>
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
