"use client";

import { useState, useEffect } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useUpdateFood } from "@/lib/hooks/use-update-food";
import { formatMacro } from "@/lib/utils/format";
import type { FoodItem } from "@/lib/types/domain";

interface EditFoodSheetProps {
  food: FoodItem | null;
  onClose: () => void;
}

/**
 * Dialog modal for editing a catalog food item.
 * Port of Android's EditFoodSheetContent composable.
 *
 * Fields: name*, brand, serving size (g)*, calories, protein, carbs, fat.
 * Calls the update-food Edge Function via useUpdateFood mutation.
 * The Postgres trigger auto-recalculates daily_logs when base macros change.
 */
export function EditFoodSheet({ food, onClose }: EditFoodSheetProps) {
  const updateFood = useUpdateFood();

  const [name, setName] = useState("");
  const [brand, setBrand] = useState("");
  const [servingG, setServingG] = useState("");
  const [calories, setCalories] = useState("");
  const [protein, setProtein] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fat, setFat] = useState("");
  const [error, setError] = useState("");

  // Pre-fill fields when food changes
  useEffect(() => {
    if (food) {
      setName(food.name);
      setBrand(food.brand ?? "");
      setServingG(formatMacro(food.baseServingG));
      setCalories(formatMacro(food.baseCalories));
      setProtein(formatMacro(food.baseProtein));
      setCarbs(formatMacro(food.baseCarbs));
      setFat(formatMacro(food.baseFat));
      setError("");
    }
  }, [food]);

  const handleSave = async () => {
    setError("");

    if (!name.trim()) {
      setError("Name cannot be empty.");
      return;
    }

    const sg = parseFloat(servingG);
    if (!sg || sg <= 0) {
      setError("Serving size must be a positive number.");
      return;
    }

    try {
      await updateFood.mutateAsync({
        foodItemId: food!.id,
        name: name.trim(),
        brand: brand.trim() || null,
        servingG: sg,
        calories: parseFloat(calories) || 0,
        protein: parseFloat(protein) || 0,
        carbs: parseFloat(carbs) || 0,
        fat: parseFloat(fat) || 0,
      });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save changes.");
    }
  };

  return (
    <Dialog open={!!food} onClose={onClose} title="Edit Food Item">
      {food && (
        <div className="flex flex-col gap-4">
          <Input
            label="Name *"
            type="text"
            value={name}
            onChange={(e) => { setName(e.target.value); setError(""); }}
          />
          <Input
            label="Brand (optional)"
            type="text"
            value={brand}
            onChange={(e) => setBrand(e.target.value)}
          />
          <Input
            label="Serving size (g) *"
            type="number"
            min="0.01"
            step="any"
            value={servingG}
            onChange={(e) => { setServingG(e.target.value); setError(""); }}
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Calories (kcal)"
              type="number"
              min="0"
              step="any"
              value={calories}
              onChange={(e) => setCalories(e.target.value)}
            />
            <Input
              label="Protein (g)"
              type="number"
              min="0"
              step="any"
              value={protein}
              onChange={(e) => setProtein(e.target.value)}
            />
            <Input
              label="Carbs (g)"
              type="number"
              min="0"
              step="any"
              value={carbs}
              onChange={(e) => setCarbs(e.target.value)}
            />
            <Input
              label="Fat (g)"
              type="number"
              min="0"
              step="any"
              value={fat}
              onChange={(e) => setFat(e.target.value)}
            />
          </div>

          {/* Error banner */}
          {error && (
            <p className="rounded-xs px-3 py-2 text-sm text-error-red" style={{ backgroundColor: "#FFB4AB20" }}>
              {error}
            </p>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="ghost" onClick={onClose} disabled={updateFood.isPending}>
              Cancel
            </Button>
            <Button loading={updateFood.isPending} onClick={handleSave}>
              Save Changes
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
