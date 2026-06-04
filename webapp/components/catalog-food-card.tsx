"use client";

import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";
import type { FoodItem } from "@/lib/types/domain";

interface CatalogFoodCardProps {
  food: FoodItem;
  isRecipe: boolean;
  onEdit: (food: FoodItem) => void;
  onDelete: (food: FoodItem) => void;
}

/**
 * Card displaying a single food item in the catalog.
 * Port of Android's CatalogFoodCard composable.
 *
 * Shows type icon (recipe/ingredient), name, brand, macro badges,
 * serving size label, and edit/delete icon buttons.
 */
export function CatalogFoodCard({ food, isRecipe, onEdit, onDelete }: CatalogFoodCardProps) {
  return (
    <div
      className="flex items-center gap-3 rounded-md border p-3"
      style={{
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border-variant)",
      }}
    >
      {/* Type icon */}
      <span
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md text-lg"
        style={{ backgroundColor: "var(--color-primary-container)" }}
      >
        {isRecipe ? "🍲" : "🥚"}
      </span>

      {/* Food info */}
      <div className="flex-1 min-w-0">
        <p
          className="truncate text-sm font-medium"
          style={{ color: "var(--text-primary)" }}
        >
          {food.name}
        </p>
        {food.brand && (
          <p
            className="truncate text-xs"
            style={{ color: "var(--text-secondary)" }}
          >
            {food.brand}
          </p>
        )}
        {/* Denormalize per-100g base macros to per-serving for display */}
        <div className="mt-1 flex flex-wrap gap-1.5">
          <MacroBadge value={food.baseCalories * food.baseServingG / 100} label=" kcal" color={MACRO_COLORS.calories} />
          <MacroBadge value={food.baseProtein * food.baseServingG / 100} label="g P" color={MACRO_COLORS.protein} />
          <MacroBadge value={food.baseCarbs * food.baseServingG / 100} label="g C" color={MACRO_COLORS.carbs} />
          <MacroBadge value={food.baseFat * food.baseServingG / 100} label="g F" color={MACRO_COLORS.fat} />
        </div>
        <p
          className="mt-0.5 text-xs"
          style={{ color: "var(--text-secondary)", opacity: 0.7 }}
        >
          per {Math.round(food.baseServingG)}g serving
        </p>
      </div>

      {/* Actions */}
      <div className="flex shrink-0 gap-0.5">
        <button
          onClick={() => onEdit(food)}
          className="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
          aria-label={`Edit ${food.name}`}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path
              d="M11.333 2.00004C11.51 1.82274 11.7209 1.68283 11.9529 1.58874C12.185 1.49465 12.4337 1.44824 12.6843 1.45199C12.9348 1.45573 13.182 1.50955 13.4112 1.61057C13.6404 1.71159 13.847 1.85779 14.019 2.04038C14.191 2.22296 14.325 2.43832 14.4131 2.67356C14.5011 2.9088 14.5413 3.15927 14.5312 3.41018C14.5211 3.66109 14.461 3.90748 14.3545 4.13481C14.2479 4.36214 14.0972 4.56593 13.911 4.73404L5.244 13.401L1.333 14.334L2.266 10.423L11.333 2.00004Z"
              stroke="var(--color-primary)"
              strokeWidth="1.2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
        <button
          onClick={() => onDelete(food)}
          className="flex h-9 w-9 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
          aria-label={`Delete ${food.name}`}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path
              d="M2 4H14M12.667 4V13.333C12.667 14 12 14.667 11.333 14.667H4.667C4 14.667 3.333 14 3.333 13.333V4M5.333 4V2.667C5.333 2 6 1.333 6.667 1.333H9.333C10 1.333 10.667 2 10.667 2.667V4"
              stroke="var(--color-error-red)"
              strokeWidth="1.2"
              strokeLinecap="round"
              strokeLinejoin="round"
              opacity="0.7"
            />
          </svg>
        </button>
      </div>
    </div>
  );
}

function MacroBadge({ value, label, color }: { value: number; label: string; color: string }) {
  return (
    <span
      className="text-xs font-semibold"
      style={{ color }}
    >
      {formatMacro(Math.round(value))}{label}
    </span>
  );
}
