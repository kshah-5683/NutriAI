"use client";

import { useEffect } from "react";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { useParseFood } from "@/lib/hooks/use-parse-food";
import { useNutritionLookup } from "@/lib/hooks/use-nutrition-lookup";
import { ParsedFoodCard } from "@/components/parsed-food-card";
import { MealTypeSelector } from "@/components/meal-type-selector";
import { Button } from "@/components/ui/button";

interface AiInputSectionProps {
  onLogAll: () => void;
  onLogAllLoading: boolean;
}

/**
 * AI Parse tab — text input, Parse with AI button, parsed food cards,
 * action buttons (Edit Selected, Log All, Clear & Retry).
 */
export function AiInputSection({ onLogAll, onLogAllLoading }: AiInputSectionProps) {
  const aiInput = useLogFormStore((s) => s.aiInput);
  const setAiInput = useLogFormStore((s) => s.setAiInput);
  const isParsing = useLogFormStore((s) => s.isParsing);
  const parsedFoods = useLogFormStore((s) => s.parsedFoods);
  const selectedIndex = useLogFormStore((s) => s.selectedIndex);
  const selectFood = useLogFormStore((s) => s.selectFood);
  const aiError = useLogFormStore((s) => s.aiError);
  const setAiError = useLogFormStore((s) => s.setAiError);
  const acceptParsedFood = useLogFormStore((s) => s.acceptParsedFood);
  const setParsedFoods = useLogFormStore((s) => s.setParsedFoods);
  const nutritionResults = useLogFormStore((s) => s.nutritionResults);
  const nutritionLoading = useLogFormStore((s) => s.nutritionLoading);
  const clarificationResolutions = useLogFormStore((s) => s.clarificationResolutions);
  const resolveClarification = useLogFormStore((s) => s.resolveClarification);

  const parseMutation = useParseFood();
  const { lookupNutrition, lookupAll } = useNutritionLookup();

  // After parse succeeds, fire nutrition lookups for new items
  useEffect(() => {
    if (parsedFoods.length > 0) {
      lookupAll(parsedFoods);
    }
    // Only run when parsedFoods changes identity (after a fresh parse)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [parsedFoods]);

  const handleParse = () => {
    if (!aiInput.trim() || aiInput.trim().length < 2) return;
    parseMutation.mutate(aiInput.trim());
  };

  const handleClear = () => {
    setParsedFoods([]);
    setAiInput("");
    setAiError(null);
  };

  /**
   * User clicked "Use generic" on a clarification banner.
   * Resolve as generic and trigger standard nutrition lookup.
   */
  const handleUseGeneric = (index: number) => {
    resolveClarification(index, "generic");
    const food = parsedFoods[index];
    if (food && !food.catalogMatch?.isFromCatalog) {
      lookupNutrition(food.name);
    }
  };

  /**
   * User submitted a brand or weight in the clarification input.
   * Detect whether it's a weight (e.g. "40g", "35") or brand text.
   */
  const handleSubmitClarification = (index: number, input: string) => {
    const food = parsedFoods[index];
    if (!food) return;

    // Detect weight input: "40g", "40 g", "40", "40.5g"
    const weightMatch = input.match(/^(\d+\.?\d*)\s*g?$/i);
    if (weightMatch) {
      const weightG = parseFloat(weightMatch[1]);
      resolveClarification(index, "weight", String(weightG));
      // For weight overrides, trigger a standard lookup (no brand)
      // The weight override will be applied client-side in acceptParsedFood
      if (!food.catalogMatch?.isFromCatalog) {
        lookupNutrition(food.name);
      }
    } else {
      // Treat as brand name — trigger brand-aware lookup
      resolveClarification(index, "brand", input);
      if (!food.catalogMatch?.isFromCatalog) {
        lookupNutrition(food.name, input);
      }
    }
  };

  const hasParsedFoods = parsedFoods.length > 0;

  return (
    <div className="space-y-4">
      {/* Text input */}
      <div>
        <textarea
          value={aiInput}
          onChange={(e) => setAiInput(e.target.value)}
          placeholder='Describe what you ate, e.g. "2 eggs, toast with butter, glass of milk"'
          rows={3}
          maxLength={500}
          className="w-full resize-none rounded-lg border px-3 py-2 text-sm outline-none transition-colors focus:ring-2"
          style={{
            backgroundColor: "var(--bg-surface)",
            borderColor: "var(--border-outline)",
            color: "var(--text-primary)",
          }}
        />
        <div className="mt-1 text-right text-xs" style={{ color: "var(--text-secondary)" }}>
          {aiInput.length}/500
        </div>
      </div>

      {/* Parse button */}
      <Button
        onClick={handleParse}
        disabled={isParsing || !aiInput.trim() || aiInput.trim().length < 2}
        loading={isParsing}
        className="w-full"
      >
        ✨ Parse with AI
      </Button>

      {/* Error banner */}
      {aiError && (
        <div
          className="flex items-start gap-2 rounded-lg border px-3 py-2 text-sm"
          style={{
            backgroundColor: "#FFF0EE",
            borderColor: "var(--color-error-red, #BA1A1A)",
            color: "var(--color-error-red, #BA1A1A)",
          }}
        >
          <span className="flex-1">{aiError}</span>
          <button
            onClick={() => setAiError(null)}
            className="text-xs font-medium underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Parsed results */}
      {hasParsedFoods && (
        <div className="space-y-2">
          {/* Meal type selector — shown after parse, before Log All */}
          <MealTypeSelector />

          <div className="text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
            {parsedFoods.length} item{parsedFoods.length !== 1 ? "s" : ""} found
          </div>

          {parsedFoods.map((food, i) => (
            <ParsedFoodCard
              key={`${food.name}-${i}`}
              food={food}
              index={i}
              isSelected={selectedIndex === i}
              nutritionInfo={nutritionResults[food.name]}
              nutritionLoading={nutritionLoading[food.name] ?? false}
              onSelect={selectFood}
              clarificationResolution={clarificationResolutions[i]}
              onUseGeneric={handleUseGeneric}
              onSubmitClarification={handleSubmitClarification}
            />
          ))}

          {/* Action buttons */}
          <div className="flex gap-2 pt-2">
            {selectedIndex !== null && (
              <Button
                variant="secondary"
                onClick={() => acceptParsedFood(selectedIndex)}
                className="flex-1"
              >
                ✏️ Edit Selected
              </Button>
            )}
            <Button
              onClick={onLogAll}
              loading={onLogAllLoading}
              className="flex-1"
            >
              Log All
            </Button>
          </div>
          <button
            onClick={handleClear}
            className="w-full py-1 text-center text-xs font-medium underline"
            style={{ color: "var(--text-secondary)" }}
          >
            Clear & Try Again
          </button>
        </div>
      )}

      {/* Fallback to manual */}
      {!hasParsedFoods && !isParsing && (
        <div className="text-center">
          <button
            onClick={() => useLogFormStore.getState().setInputMode("manual")}
            className="text-xs font-medium underline"
            style={{ color: "var(--text-secondary)" }}
          >
            Enter manually instead
          </button>
        </div>
      )}
    </div>
  );
}
