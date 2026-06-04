"use client";

import { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { MACRO_COLORS } from "@/lib/utils/constants";
import { formatMacro } from "@/lib/utils/format";
import { useScanLabel, type ScanLabelResult } from "@/lib/hooks/use-scan-label";
import { MealTypeSelector } from "@/components/meal-type-selector";
import { useLogFormStore } from "@/lib/stores/log-form-store";

/**
 * Scan tab content — image selection/capture, label scanning, result display.
 * Port of Android's LabelScannerDelegate flow:
 *  1. Select/capture image
 *  2. Compress and send to scan-label Edge Function
 *  3. Display extracted macros
 *  4. "Use These Values" → pre-fills Manual tab
 */
export function ScanInputSection() {
  const fileRef = useRef<HTMLInputElement>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [result, setResult] = useState<ScanLabelResult | null>(null);
  const scanLabel = useScanLabel();
  const acceptScanResult = useLogFormStore((s) => s.acceptScanResult);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Preview thumbnail
    setPreviewUrl(URL.createObjectURL(file));
    setResult(null);

    // Scan
    scanLabel.mutate(file, {
      onSuccess: (data) => setResult(data),
    });
  };

  const handleUseValues = () => {
    if (!result) return;
    acceptScanResult(result);
  };

  const handleClear = () => {
    setPreviewUrl(null);
    setResult(null);
    scanLabel.reset();
    if (fileRef.current) fileRef.current.value = "";
  };

  return (
    <div className="space-y-4">
      {/* File input */}
      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileSelect}
        className="hidden"
      />

      {!previewUrl ? (
        <button
          onClick={() => fileRef.current?.click()}
          className="flex w-full flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed py-12 transition-colors hover:border-[var(--color-primary)]"
          style={{ borderColor: "var(--border-variant)" }}
        >
          <span className="text-4xl">📸</span>
          <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
            Tap to scan a nutrition label
          </p>
          <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
            Take a photo or select from gallery
          </p>
        </button>
      ) : (
        <div className="space-y-3">
          {/* Preview */}
          <div className="relative overflow-hidden rounded-lg">
            <img
              src={previewUrl}
              alt="Label preview"
              className="h-48 w-full object-cover"
            />
            {scanLabel.isPending && (
              <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                <div className="flex flex-col items-center gap-2">
                  <div
                    className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
                    style={{ borderColor: "#fff", borderTopColor: "transparent" }}
                  />
                  <span className="text-sm font-medium text-white">Scanning label...</span>
                </div>
              </div>
            )}
          </div>

          {/* Error */}
          {scanLabel.isError && (
            <div
              className="rounded-md px-3 py-2 text-sm"
              style={{ backgroundColor: "#FFB4AB20", color: "var(--color-error-red)" }}
            >
              {scanLabel.error instanceof Error
                ? scanLabel.error.message
                : "Could not read the label. Please try again or enter values manually."}
            </div>
          )}

          {/* Results */}
          {result && (
            <div
              className="rounded-md border p-3 space-y-3"
              style={{
                backgroundColor: "var(--bg-surface)",
                borderColor: "var(--border-variant)",
              }}
            >
              <h3 className="text-sm font-semibold" style={{ color: "var(--text-primary)" }}>
                Extracted Values (per 100g)
              </h3>

              <div className="grid grid-cols-2 gap-2 text-sm">
                <MacroRow label="Calories" value={`${Math.round(result.calories)} kcal`} color={MACRO_COLORS.calories} />
                <MacroRow label="Protein" value={`${formatMacro(result.protein)}g`} color={MACRO_COLORS.protein} />
                <MacroRow label="Carbs" value={`${formatMacro(result.carbs)}g`} color={MACRO_COLORS.carbs} />
                <MacroRow label="Fat" value={`${formatMacro(result.fat)}g`} color={MACRO_COLORS.fat} />
              </div>

              {result.serving_size_text && (
                <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
                  Original serving: {result.serving_size_text}
                  {result.serving_weight_g ? ` (${result.serving_weight_g}g)` : ""}
                </p>
              )}

              {/* Meal type — set before accepting values into manual tab */}
              <MealTypeSelector />

              <Button onClick={handleUseValues} className="w-full">
                Use These Values
              </Button>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-2">
            <Button variant="ghost" onClick={handleClear} className="flex-1">
              Clear &amp; Try Again
            </Button>
            <Button
              variant="secondary"
              onClick={() => fileRef.current?.click()}
              className="flex-1"
            >
              Scan Another
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function MacroRow({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className="flex items-center justify-between">
      <span style={{ color: "var(--text-secondary)" }}>{label}</span>
      <span className="font-semibold" style={{ color }}>{value}</span>
    </div>
  );
}
