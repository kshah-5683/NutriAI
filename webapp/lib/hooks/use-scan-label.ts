"use client";

import { useMutation } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { compressLabelImage } from "@/lib/utils/image-compressor";

/**
 * Shape of the scan-label Edge Function response.
 */
export interface ScanLabelResult {
  /** Raw per-serving values (for display) */
  raw_calories_per_serving: number;
  raw_protein_g: number;
  raw_carbs_g: number;
  raw_fat_g: number;
  serving_size_text: string | null;
  serving_weight_g: number | null;

  /** Converted per-100g values (for form pre-fill) */
  calories: number;
  protein: number;
  carbs: number;
  fat: number;

  /** Pre-fill hints */
  suggested_quantity: number;
  suggested_unit: string;
}

/**
 * TanStack Query mutation — compresses an image and calls the scan-label Edge Function.
 *
 * Port of Android's LabelScannerDelegate.onLabelPhotoSelected():
 * 1. Compress image (max 1024px, JPEG 80%)
 * 2. Send base64 to scan-label EF
 * 3. Return extracted + converted macro values
 *
 * The EF does per-serving → per-100g conversion server-side (same math as Android client-side).
 */
export function useScanLabel() {
  const supabase = useSupabase();

  return useMutation({
    mutationFn: async (file: File): Promise<ScanLabelResult> => {
      // 1. Compress image
      const compressed = await compressLabelImage(file);

      // 2. Call scan-label Edge Function
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.SCAN_LABEL,
        {
          body: {
            base64: compressed.base64,
            mimeType: compressed.mimeType,
          },
        }
      );

      if (error) throw new Error(error.message ?? "Failed to scan label");
      if (data?.error) throw new Error(data.error);

      return data as ScanLabelResult;
    },
  });
}
