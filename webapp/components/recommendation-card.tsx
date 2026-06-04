"use client";

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";
import { useAddToCatalog } from "@/lib/hooks/use-add-to-catalog";
import type { Recommendation } from "@/lib/types/recommendation";
import type { MealType } from "@/lib/types/domain";

// ─── Meal label from nextMeal prop ──────────────────────────────────────────

const MEAL_LABELS: Record<MealType, string> = {
  breakfast: "Breakfast",
  lunch: "Lunch",
  snack: "Snack",
  dinner: "Dinner",
};

// ─── Macro pill ──────────────────────────────────────────────────────────────

interface MacroPillProps {
  value: number;
  label: string;
  color: string;
}

function MacroPill({ value, label, color }: MacroPillProps) {
  return (
    <span
      className="inline-flex items-center gap-0.5 rounded-xs px-1.5 py-0.5 text-xs font-medium"
      style={{ backgroundColor: `${color}18`, color }}
    >
      {formatMacro(value)}{label}
    </span>
  );
}

// ─── Shimmer loading placeholder ─────────────────────────────────────────────

function ShimmerPlaceholder() {
  return (
    <div className="space-y-3 animate-pulse">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="space-y-1.5">
          <div className="h-4 w-36 rounded bg-sage-gray" />
          <div className="h-3 w-48 rounded bg-sage-gray" />
          <div className="flex gap-1">
            {Array.from({ length: 4 }).map((_, j) => (
              <div key={j} className="h-5 w-14 rounded bg-sage-gray" />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── Formatted recipe display ────────────────────────────────────────────────

/**
 * Parses AI recipe_text (e.g. "Ingredients: 200g tofu, 1 cup spinach. Method: Cook...")
 * into structured bulleted lists for both ingredients and method steps.
 * Scrollable container with max-height to prevent the card from growing too tall.
 */
function FormattedRecipe({ text }: { text: string }) {
  const methodMatch = text.match(/method\s*:/i);
  const ingredientsMatch = text.match(/ingredients\s*:/i);

  let ingredientsRaw = "";
  let methodRaw = "";

  if (ingredientsMatch && methodMatch) {
    const ingStart = ingredientsMatch.index! + ingredientsMatch[0].length;
    ingredientsRaw = text.slice(ingStart, methodMatch.index!).trim();
    methodRaw = text.slice(methodMatch.index! + methodMatch[0].length).trim();
  } else if (ingredientsMatch) {
    ingredientsRaw = text.slice(ingredientsMatch.index! + ingredientsMatch[0].length).trim();
  } else {
    // No structured format — fall back to plain bulleted display
    const lines = text.split(/[.,]/).map((s) => s.trim()).filter(Boolean);
    return (
      <div
        className="mt-1 max-h-48 overflow-y-auto rounded-md p-2 text-xs leading-relaxed"
        style={{ color: "var(--text-primary)", backgroundColor: "var(--bg-base)" }}
      >
        <ul className="list-disc list-inside space-y-0.5">
          {lines.map((line, i) => (
            <li key={i}>{line}</li>
          ))}
        </ul>
      </div>
    );
  }

  // Split ingredients by comma, trimming trailing period
  const ingredients = ingredientsRaw
    .replace(/\.\s*$/, "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);

  // Split method into individual steps by sentence-ending periods
  const steps = methodRaw
    .split(/\.(?:\s|$)/)
    .map((s) => s.trim())
    .filter(Boolean);

  return (
    <div
      className="mt-1 max-h-48 overflow-y-auto rounded-md p-2 text-xs leading-relaxed"
      style={{ color: "var(--text-primary)", backgroundColor: "var(--bg-base)" }}
    >
      {ingredients.length > 0 && (
        <div>
          <p className="font-semibold mb-1">Ingredients</p>
          <ul className="list-disc list-inside space-y-0.5">
            {ingredients.map((item, i) => (
              <li key={i}>{item}</li>
            ))}
          </ul>
        </div>
      )}

      {steps.length > 0 && (
        <div className={ingredients.length > 0 ? "mt-2" : ""}>
          <p className="font-semibold mb-1">Method</p>
          <ul className="list-disc list-inside space-y-0.5">
            {steps.map((step, i) => (
              <li key={i}>{step}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

// ─── "Add to Food Catalog" button ────────────────────────────────────────────────

interface AddToCatalogButtonProps {
  rec: Recommendation;
  isAdded: boolean;
  onAdded: () => void;
}

function AddToCatalogButton({ rec, isAdded, onAdded }: AddToCatalogButtonProps) {
  const addToCatalog = useAddToCatalog();
  const [loading, setLoading] = useState(false);

  const handleAdd = async () => {
    if (loading || isAdded) return;
    setLoading(true);
    try {
      await addToCatalog.mutateAsync(rec);
      onAdded();
    } catch {
      // Error — revert to idle so user can retry
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleAdd}
      disabled={loading || isAdded}
      className="inline-flex items-center gap-1 rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors disabled:opacity-60"
      style={{
        backgroundColor: isAdded ? "var(--bg-surface)" : "var(--color-primary)",
        color: isAdded ? "var(--text-secondary)" : "#FFFFFF",
        border: isAdded ? "1px solid var(--border-variant)" : "none",
      }}
    >
      {isAdded ? (
        <>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path
              d="M20 6L9 17L4 12"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          Added
        </>
      ) : loading ? (
        <>
          <svg
            className="animate-spin"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="2"
              strokeOpacity="0.3"
            />
            <path
              d="M12 2a10 10 0 0 1 10 10"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
          </svg>
          Adding...
        </>
      ) : (
        <>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path
              d="M12 5V19M5 12H19"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          Add to Food Catalog
        </>
      )}
    </button>
  );
}

// ─── Single recommendation item ──────────────────────────────────────────────

interface RecommendationItemProps {
  rec: Recommendation;
  featured: boolean;
  isAdded: boolean;
  onAdded: () => void;
  onDismiss?: () => void;
}

function RecommendationItem({
  rec,
  featured,
  isAdded,
  onAdded,
  onDismiss,
}: RecommendationItemProps) {
  const [recipeExpanded, setRecipeExpanded] = useState(false);

  const qtyLabel =
    rec.suggested_quantity > 1 ? ` (x${Math.round(rec.suggested_quantity)})` : "";

  // Client-side URL construction — no AI-generated URLs
  const searchTerm = rec.search_query ?? `${rec.name} recipe`;
  const youtubeUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(searchTerm)}`;
  const googleUrl = `https://www.google.com/search?q=${encodeURIComponent(searchTerm)}`;

  return (
    <div className="space-y-2">
      {/* Name + quantity + dismiss */}
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
          {rec.name}{qtyLabel}
        </p>
        {onDismiss && (
          <button
            onClick={onDismiss}
            className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
            aria-label={`Dismiss ${rec.name}`}
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path
                d="M9 3L3 9M3 3L9 9"
                stroke="var(--text-secondary)"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        )}
      </div>

      {/* Reason (featured items only) */}
      {featured && rec.reason && (
        <p className="text-xs italic" style={{ color: "var(--text-secondary)" }}>
          {rec.reason}
        </p>
      )}

      {/* Macro pills */}
      <div className="flex flex-wrap gap-1">
        <MacroPill value={rec.calories} label=" kcal" color={MACRO_COLORS.calories} />
        <MacroPill value={rec.protein} label="g P" color={MACRO_COLORS.protein} />
        <MacroPill value={rec.carbs} label="g C" color={MACRO_COLORS.carbs} />
        <MacroPill value={rec.fat} label="g F" color={MACRO_COLORS.fat} />
      </div>

      {/* Source badge + internet-specific content */}
      {rec.source === "catalog" ? (
        <p className="text-xs font-medium" style={{ color: "var(--color-primary)" }}>
          From: Your Catalog
        </p>
      ) : (
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span
              className="text-xs font-medium"
              style={{ color: "var(--text-secondary)" }}
            >
              AI Suggestion
            </span>
            <span
              className="text-xs italic"
              style={{ color: "var(--text-secondary)", opacity: 0.7 }}
            >
              ~Estimated macros~
            </span>
          </div>

          {/* Expandable recipe text */}
          {rec.recipe_text && (
            <div>
              <button
                onClick={() => setRecipeExpanded(!recipeExpanded)}
                className="text-xs font-medium underline"
                style={{ color: "var(--color-primary)" }}
              >
                {recipeExpanded ? "Hide Recipe" : "View Recipe"}
              </button>
              {recipeExpanded && (
                <FormattedRecipe text={rec.recipe_text} />
              )}
            </div>
          )}

          {/* Action buttons: YouTube, Google, Add to Food Catalog */}
          <div className="flex flex-wrap items-center gap-2">
            <a
              href={youtubeUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors hover:opacity-80"
              style={{
                color: "var(--text-primary)",
                borderColor: "var(--border-variant)",
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path
                  d="M22.54 6.42a2.78 2.78 0 0 0-1.94-2C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 0 0-1.94 2A29 29 0 0 0 1 11.75a29 29 0 0 0 .46 5.33A2.78 2.78 0 0 0 3.4 19.13C5.12 19.56 12 19.56 12 19.56s6.88 0 8.6-.46a2.78 2.78 0 0 0 1.94-1.94 29 29 0 0 0 .46-5.25 29.3 29.3 0 0 0-.46-5.49z"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M9.75 15.02L15.5 11.75L9.75 8.48V15.02Z"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              YouTube
            </a>
            <a
              href={googleUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors hover:opacity-80"
              style={{
                color: "var(--text-primary)",
                borderColor: "var(--border-variant)",
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <circle
                  cx="11"
                  cy="11"
                  r="8"
                  stroke="currentColor"
                  strokeWidth="1.5"
                />
                <path
                  d="M21 21L16.65 16.65"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                />
              </svg>
              Google
            </a>
            <AddToCatalogButton rec={rec} isAdded={isAdded} onAdded={onAdded} />
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Main RecommendationCard ─────────────────────────────────────────────────

interface RecommendationCardProps {
  recommendations: Recommendation[];
  isLoading: boolean;
  error: string | null;
  /** The next meal slot being recommended for. */
  nextMeal: MealType | null;
  /** Standard meals the user has missed today (excludes snack). */
  missedMeals: MealType[];
}

/**
 * Card showing AI meal recommendations on the Home screen.
 *
 * Shows up to 3 visible recommendations from a buffer of 5.
 * Dismiss X button removes one and slides in the next from the buffer.
 * Handles both catalog and internet sources.
 *
 * Renders only when there's data, loading, or an error to display.
 * Returns null for empty state (no recs, no loading, no error).
 */
export function RecommendationCard({
  recommendations,
  isLoading,
  error,
  nextMeal,
  missedMeals,
}: RecommendationCardProps) {
  const [addedNames, setAddedNames] = useState<Set<string>>(new Set());
  const [dismissedIndices, setDismissedIndices] = useState<Set<number>>(
    new Set()
  );

  // Reset dismissed state when nextMeal changes (new meal slot)
  useEffect(() => {
    setDismissedIndices(new Set());
  }, [nextMeal]);

  const mealLabel = nextMeal ? MEAL_LABELS[nextMeal] : null;

  // Visible recommendations: filter dismissed, show max 3
  const visibleRecs = recommendations
    .map((rec, i) => ({ rec, index: i }))
    .filter(({ index }) => !dismissedIndices.has(index))
    .slice(0, 3);

  // Don't render anything if empty + not loading + no error
  if (!isLoading && !error && recommendations.length === 0 && missedMeals.length === 0) {
    return null;
  }

  const handleAdded = (name: string) => {
    setAddedNames((prev) => new Set(prev).add(name));
  };

  const handleDismiss = (index: number) => {
    setDismissedIndices((prev) => new Set(prev).add(index));
  };

  return (
    <Card className="mx-4 mt-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <span className="text-base">✨</span>
          <span
            className="text-sm font-semibold"
            style={{ color: "var(--text-primary)" }}
          >
            {mealLabel ? `${mealLabel} Suggestions` : "Suggested for you"}
          </span>
        </div>
        {mealLabel && (
          <span
            className="rounded-full px-2 py-0.5 text-xs font-medium"
            style={{
              backgroundColor: "var(--color-primary-light, rgba(46, 139, 122, 0.12))",
              color: "var(--color-primary)",
            }}
          >
            {mealLabel}
          </span>
        )}
      </div>

      {/* Missed meals banner */}
      {missedMeals.length > 0 && (
        <p
          className="mt-2 text-xs"
          style={{ color: "var(--text-secondary)" }}
        >
          You haven&apos;t logged{" "}
          {missedMeals.map((m) => MEAL_LABELS[m].toLowerCase()).join(" or ")}{" "}
          today
        </p>
      )}

      {/* Loading state */}
      {isLoading && (
        <div className="mt-3">
          <ShimmerPlaceholder />
        </div>
      )}

      {/* Error state — non-blocking inline message */}
      {error && !isLoading && (
        <p className="mt-3 text-xs" style={{ color: "var(--color-error, #DC3545)" }}>
          Couldn&apos;t load suggestions. Pull to refresh to try again.
        </p>
      )}

      {/* Recommendations — show up to 3 visible from the 5-item buffer */}
      {!isLoading && visibleRecs.length > 0 && (
        <div className="mt-3 space-y-4">
          {visibleRecs.map(({ rec, index }, i) => (
            <div
              key={rec.name}
              className={i > 0 ? "border-t pt-3" : ""}
              style={i > 0 ? { borderColor: "var(--border-variant)" } : undefined}
            >
              <RecommendationItem
                rec={rec}
                featured={i === 0}
                isAdded={addedNames.has(rec.name)}
                onAdded={() => handleAdded(rec.name)}
                onDismiss={() => handleDismiss(index)}
              />
            </div>
          ))}
        </div>
      )}

      {/* All dismissed — subtle empty message */}
      {!isLoading &&
        recommendations.length > 0 &&
        visibleRecs.length === 0 && (
          <p
            className="mt-3 text-xs italic"
            style={{ color: "var(--text-secondary)" }}
          >
            No more suggestions for now.
          </p>
        )}
    </Card>
  );
}
