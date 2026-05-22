"use client";

import { useState, useEffect, useRef } from "react";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useUpdateLog } from "@/lib/hooks/use-update-log";
import { toDisplayQty, fromDisplayQty } from "@/lib/utils/unit-converter";
import type { DailyLog } from "@/lib/types/domain";

interface EditLogSheetProps {
  log: DailyLog | null;
  onClose: () => void;
}

/**
 * Modal form for editing a daily log entry.
 * Port of Android's HomeViewModel.EditLogSheet behavior:
 * the user edits total macros directly (not recalculated from base macros).
 *
 * Tech Debt #9 fix: Applies toDisplayQty on pre-fill and fromDisplayQty on save
 * so gram-unit entries show actual grams (e.g. 200 g) not the internal multiplier (2.0).
 *
 * Proportional scaling: when quantity or unit changes, macro totals are
 * automatically scaled relative to the original log values opened in the sheet.
 * e.g. 300g → 250g scales all macros by (2.5 / 3.0) = 0.833.
 * The user can still override individual macro fields after scaling.
 */
export function EditLogSheet({ log, onClose }: EditLogSheetProps) {
  const updateLog = useUpdateLog();

  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState("");
  const [calories, setCalories] = useState("");
  const [protein, setProtein] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fat, setFat] = useState("");
  const [error, setError] = useState("");

  // Original log stored as stable reference for proportional scaling.
  // Always scales from the original totals — not from intermediate typed values —
  // so typing "25" (2 → 5) doesn't cascade through two scale operations.
  const originalLogRef = useRef<DailyLog | null>(null);

  // Pre-fill fields when log changes — convert multiplier → display grams for gram units
  useEffect(() => {
    if (log) {
      originalLogRef.current = log;
      setQuantity(String(toDisplayQty(log.consumedQty, log.consumedUnit)));
      setUnit(log.consumedUnit);
      setCalories(String(log.totalCalories));
      setProtein(String(log.totalProtein));
      setCarbs(String(log.totalCarbs));
      setFat(String(log.totalFat));
      setError("");
    }
  }, [log]);

  /** Round to 1 decimal place, stripping unnecessary trailing zeros. */
  const fmtMacro = (n: number) => String(Math.round(n * 10) / 10);

  /**
   * Recompute all 4 macro fields proportionally from the original log values.
   * ratio = newConsumedQtyMultiplier / originalConsumedQtyMultiplier
   */
  const scaleMacros = (displayQty: number, resolvedUnit: string) => {
    const orig = originalLogRef.current;
    if (!orig || orig.consumedQty <= 0 || displayQty <= 0) return;
    const newMultiplier = fromDisplayQty(displayQty, resolvedUnit);
    const ratio = newMultiplier / orig.consumedQty;
    setCalories(fmtMacro(orig.totalCalories * ratio));
    setProtein(fmtMacro(orig.totalProtein * ratio));
    setCarbs(fmtMacro(orig.totalCarbs * ratio));
    setFat(fmtMacro(orig.totalFat * ratio));
  };

  const handleQuantityChange = (newQtyStr: string) => {
    setQuantity(newQtyStr);
    const newQty = parseFloat(newQtyStr);
    if (newQty > 0) scaleMacros(newQty, unit);
  };

  const handleUnitChange = (newUnit: string) => {
    setUnit(newUnit);
    const qty = parseFloat(quantity);
    if (qty > 0) scaleMacros(qty, newUnit);
  };

  const handleSave = async () => {
    setError("");

    const qty = parseFloat(quantity);
    const cal = parseFloat(calories) || 0;
    const pro = parseFloat(protein) || 0;
    const crb = parseFloat(carbs) || 0;
    const ft = parseFloat(fat) || 0;

    if (!qty || qty <= 0) {
      setError("Quantity must be a positive number.");
      return;
    }
    if (cal < 0 || pro < 0 || crb < 0 || ft < 0) {
      setError("Macro values cannot be negative.");
      return;
    }

    try {
      // Convert display grams back to multiplier for DB storage
      const storedQty = fromDisplayQty(qty, unit || "serving");
      await updateLog.mutateAsync({
        logId: log!.id,
        quantity: storedQty,
        unit: unit || "serving",
        calories: cal,
        protein: pro,
        carbs: crb,
        fat: ft,
      });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update log.");
    }
  };

  return (
    <Dialog open={!!log} onClose={onClose} title="Edit Log Entry">
      {log && (
        <div className="flex flex-col gap-4">
          {/* Read-only food name */}
          <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
            {log.foodName}
          </p>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Quantity"
              type="number"
              min="0.01"
              step="any"
              value={quantity}
              onChange={(e) => handleQuantityChange(e.target.value)}
            />
            <Input
              label="Unit"
              type="text"
              value={unit}
              onChange={(e) => handleUnitChange(e.target.value)}
              placeholder="serving"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Calories"
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
            <Button variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button loading={updateLog.isPending} onClick={handleSave}>
              Save
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
