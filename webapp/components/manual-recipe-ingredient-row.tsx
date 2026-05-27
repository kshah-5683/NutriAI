"use client";

import { useEffect, useRef, useState } from "react";
import { useLogFormStore, type ManualRecipeIngredient } from "@/lib/stores/log-form-store";
import { useCatalogItems } from "@/lib/hooks/use-catalog-items";
import { scaleIngredientMacros } from "@/lib/utils/macro-calculator";
import { isGramUnit } from "@/lib/utils/unit-converter";
import { formatMacro } from "@/lib/utils/format";
import { MACRO_COLORS } from "@/lib/utils/constants";
import { useSupabase } from "@/components/providers/supabase-provider";
import { getIngredientCatalogId } from "@/lib/utils/constants";
import type { FoodItem } from "@/lib/types/domain";

/** Unit options for the per-ingredient unit dropdown. */
const UNIT_OPTIONS = [
  { value: "g", label: "g" },
  { value: "ml", label: "ml" },
  { value: "serving", label: "Serving" },
  { value: "tsp", label: "tsp" },
  { value: "tbsp", label: "tbsp" },
  { value: "cup", label: "Cup" },
  { value: "piece", label: "Piece" },
  { value: "slice", label: "Slice" },
  { value: "bowl", label: "Bowl" },
] as const;

interface ManualRecipeIngredientRowProps {
  ingredient: ManualRecipeIngredient;
  /** Hide the remove button when this is the only row. */
  isOnly: boolean;
  userId: string | null;
}

/**
 * A single ingredient row in the manual recipe builder.
 *
 * Layout:
 *  Desktop (sm+): [Ingredient combobox ──────] [Qty] [Unit ▾] [×]
 *  Mobile (<sm):  [Ingredient combobox ───────────────────────] [×]
 *                 [Qty ──────────────] [Unit ▾ ───────────────]
 *
 * Per-row macro badge:  "200g: 690 kcal · 15.8g P · …"
 * Custom ingredient:   Inline macro inputs appear below the main row.
 */
export function ManualRecipeIngredientRow({
  ingredient,
  isOnly,
  userId,
}: ManualRecipeIngredientRowProps) {
  const updateIngredient = useLogFormStore((s) => s.updateIngredient);
  const removeIngredient = useLogFormStore((s) => s.removeIngredient);
  const selectCatalogIngredient = useLogFormStore((s) => s.selectCatalogIngredient);
  const clearCatalogIngredient = useLogFormStore((s) => s.clearCatalogIngredient);

  const supabase = useSupabase();
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 300ms debounce on search input
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(searchQuery), 300);
    return () => clearTimeout(t);
  }, [searchQuery]);

  const catalogId = userId ? getIngredientCatalogId(userId) : null;
  const { data: catalogResults = [] } = useCatalogItems(catalogId, debouncedQuery);

  // Sync the input display value with ingredient state
  const displayName = ingredient.customName;

  // Close dropdown on outside click
  useEffect(() => {
    function handleOutside(e: MouseEvent) {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node) &&
        !inputRef.current?.contains(e.target as Node)
      ) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleOutside);
    return () => document.removeEventListener("mousedown", handleOutside);
  }, []);

  const handleNameInputChange = (value: string) => {
    setSearchQuery(value);
    updateIngredient(ingredient.id, "customName", value);
    // Clear any previous catalog selection when user edits name manually
    if (ingredient.catalogItem) {
      clearCatalogIngredient(ingredient.id);
    }
    setDropdownOpen(true);
  };

  const handleSelectCatalogItem = (item: FoodItem) => {
    setSearchQuery(item.name);
    selectCatalogIngredient(ingredient.id, item);
    setDropdownOpen(false);
  };

  const handleAddCustom = () => {
    const name = searchQuery.trim();
    if (!name) return;
    updateIngredient(ingredient.id, "customName", name);
    clearCatalogIngredient(ingredient.id);
    setDropdownOpen(false);
  };

  const handleInputBlur = () => {
    // Small delay so click on dropdown item fires first
    setTimeout(() => {
      setDropdownOpen(false);
      // If text was typed but no catalog item selected, commit as custom ingredient
      if (!ingredient.catalogItem && searchQuery.trim()) {
        updateIngredient(ingredient.id, "customName", searchQuery.trim());
      }
    }, 150);
  };

  const isCustom = ingredient.catalogItem === null;
  const hasName = displayName.trim().length > 0;

  // Per-row scaled macros for badge display
  const scaled = scaleIngredientMacros(ingredient);
  const qty = parseFloat(ingredient.quantity) || 0;
  const showBadge = hasName && qty > 0;

  // Adaptive macro label based on unit
  const macroLabel = isGramUnit(ingredient.unit)
    ? "Nutrition per 100g"
    : ["piece", "pieces", "slice", "slices", "bowl", "bowls"].includes(
        ingredient.unit.toLowerCase()
      )
    ? `Nutrition per ${ingredient.unit}`
    : "Nutrition per serving";

  return (
    <div
      className="rounded-lg border p-3 space-y-2"
      style={{
        borderColor: "var(--border-variant)",
        backgroundColor: "var(--bg-surface)",
      }}
    >
      {/* ── Main row ── */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        {/* Ingredient combobox */}
        <div className="relative flex-1">
          {ingredient.catalogItem ? (
            /* Catalog chip — shows selected item with clear button */
            <div
              className="flex h-10 items-center gap-2 rounded-lg border px-3 text-sm"
              style={{
                backgroundColor: "var(--bg-surface-variant)",
                borderColor: "var(--border-outline)",
              }}
            >
              <span
                className="inline-block h-2 w-2 flex-shrink-0 rounded-full"
                style={{ backgroundColor: "var(--color-primary, #2D5A27)" }}
              />
              <span className="flex-1 truncate">{ingredient.catalogItem.name}</span>
              <button
                type="button"
                onClick={() => {
                  clearCatalogIngredient(ingredient.id);
                  setSearchQuery("");
                  setTimeout(() => inputRef.current?.focus(), 0);
                }}
                className="ml-1 text-xs opacity-60 hover:opacity-100"
                aria-label="Clear ingredient selection"
              >
                ×
              </button>
            </div>
          ) : (
            /* Search input */
            <div className="relative">
              <input
                ref={inputRef}
                type="text"
                value={searchQuery || displayName}
                onChange={(e) => handleNameInputChange(e.target.value)}
                onFocus={() => setDropdownOpen(true)}
                onBlur={handleInputBlur}
                placeholder="🔍 Search or add ingredient…"
                className="h-10 w-full rounded-lg border px-3 text-sm outline-none transition-colors focus:ring-1 focus:ring-primary"
                style={{
                  backgroundColor: "var(--bg-surface)",
                  borderColor: "var(--border-outline)",
                  color: "var(--text-primary)",
                }}
              />
            </div>
          )}

          {/* Dropdown */}
          {dropdownOpen && !ingredient.catalogItem && (
            <div
              ref={dropdownRef}
              className="absolute left-0 right-0 top-full z-50 mt-1 max-h-56 overflow-y-auto rounded-lg border shadow-lg"
              style={{
                backgroundColor: "var(--bg-surface)",
                borderColor: "var(--border-outline)",
              }}
            >
              {/* "Add new" option — always at top when text is typed */}
              {searchQuery.trim() && (
                <button
                  type="button"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    handleAddCustom();
                  }}
                  className="flex w-full items-center gap-2 px-3 py-2.5 text-sm transition-colors hover:bg-sage-gray"
                  style={{ color: "var(--color-primary, #2D5A27)" }}
                >
                  <span className="font-medium">+</span>
                  <span>
                    Add &ldquo;{searchQuery.trim()}&rdquo; as new ingredient
                  </span>
                </button>
              )}

              {/* Catalog results */}
              {catalogResults.length > 0 ? (
                catalogResults.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      handleSelectCatalogItem(item);
                    }}
                    className="flex w-full items-center justify-between px-3 py-2.5 text-sm transition-colors hover:bg-sage-gray"
                  >
                    <span className="truncate">{item.name}</span>
                    <span
                      className="ml-2 flex-shrink-0 text-xs"
                      style={{ color: "var(--text-secondary)" }}
                    >
                      {formatMacro(item.baseCalories)} kcal/100g
                    </span>
                  </button>
                ))
              ) : (
                <p
                  className="px-3 py-2.5 text-sm"
                  style={{ color: "var(--text-secondary)" }}
                >
                  {searchQuery.trim()
                    ? "No matches in catalog"
                    : "No ingredients in catalog yet — type to add"}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Quantity + Unit */}
        <div className="flex gap-2 sm:w-auto">
          <input
            type="number"
            value={ingredient.quantity}
            onChange={(e) =>
              updateIngredient(ingredient.id, "quantity", e.target.value)
            }
            min="0"
            step="0.1"
            placeholder="Qty"
            className="h-10 w-20 rounded-lg border px-2 text-sm outline-none focus:ring-1 focus:ring-primary"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-outline)",
              color: "var(--text-primary)",
            }}
          />
          <select
            value={ingredient.unit}
            onChange={(e) =>
              updateIngredient(ingredient.id, "unit", e.target.value)
            }
            className="h-10 rounded-lg border px-2 text-sm outline-none focus:ring-1 focus:ring-primary"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-outline)",
              color: "var(--text-primary)",
            }}
          >
            {UNIT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Remove button */}
        {!isOnly && (
          <button
            type="button"
            onClick={() => removeIngredient(ingredient.id)}
            className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg border text-sm transition-colors hover:border-error-red hover:text-error-red"
            style={{
              borderColor: "var(--border-outline)",
              color: "var(--text-secondary)",
            }}
            aria-label="Remove ingredient"
          >
            ×
          </button>
        )}
      </div>

      {/* ── Custom macro inputs ── */}
      {isCustom && hasName && (
        <div className="space-y-1.5 pt-1">
          <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
            {macroLabel}
          </p>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {(
              [
                { field: "calories" as const, label: "Cal (kcal)", color: MACRO_COLORS.calories },
                { field: "protein" as const, label: "Protein (g)", color: MACRO_COLORS.protein },
                { field: "carbs" as const, label: "Carbs (g)", color: MACRO_COLORS.carbs },
                { field: "fat" as const, label: "Fat (g)", color: MACRO_COLORS.fat },
              ] as const
            ).map(({ field, label, color }) => (
              <div key={field} className="flex flex-col gap-1">
                <label
                  className="flex items-center gap-1 text-xs font-medium"
                  style={{ color: "var(--text-secondary)" }}
                >
                  <span
                    className="inline-block h-2 w-2 rounded-full"
                    style={{ backgroundColor: color }}
                  />
                  {label}
                </label>
                <input
                  type="number"
                  value={ingredient[field]}
                  onChange={(e) =>
                    updateIngredient(ingredient.id, field, e.target.value)
                  }
                  min="0"
                  step="0.1"
                  placeholder="0"
                  className="h-9 rounded-lg border px-2 text-sm outline-none focus:ring-1 focus:ring-primary"
                  style={{
                    backgroundColor: "var(--bg-surface)",
                    borderColor: "var(--border-outline)",
                    color: "var(--text-primary)",
                  }}
                />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Per-row scaled macro badges ── */}
      {showBadge && (
        <div
          className="flex flex-wrap items-center gap-x-3 gap-y-1 rounded-md px-2 py-1.5"
          style={{ backgroundColor: "var(--bg-surface-variant)" }}
        >
          <span className="text-xs font-medium" style={{ color: "var(--text-secondary)" }}>
            {formatMacro(qty)}
            {ingredient.unit}:
          </span>
          {(
            [
              { label: "kcal", value: scaled.calories, color: MACRO_COLORS.calories, unit: "" },
              { label: "P", value: scaled.protein, color: MACRO_COLORS.protein, unit: "g" },
              { label: "C", value: scaled.carbs, color: MACRO_COLORS.carbs, unit: "g" },
              { label: "F", value: scaled.fat, color: MACRO_COLORS.fat, unit: "g" },
            ] as const
          ).map(({ label, value, color, unit: u }) => (
            <span key={label} className="flex items-center gap-1">
              <span
                className="inline-block h-2 w-2 rounded-full"
                style={{ backgroundColor: color }}
              />
              <span className="text-xs font-medium">
                {formatMacro(value)}
                {u ?? ""}
              </span>
              <span className="text-xs" style={{ color: "var(--text-secondary)" }}>
                {label}
              </span>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
