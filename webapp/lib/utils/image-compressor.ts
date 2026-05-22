/**
 * Client-side image compression utility for label scanning.
 * Port of ImageCompressor.kt — same parameters: max 1024px, JPEG 80% quality.
 *
 * Used before sending label photos to the scan-label Edge Function.
 * Browser-only (uses Canvas API and FileReader).
 */

import { IMAGE_COMPRESSION } from "./constants";

const { MAX_DIMENSION, JPEG_QUALITY } = IMAGE_COMPRESSION;

export interface CompressedImage {
  /** Raw base64 string — no data URI prefix. Ready for Edge Function payload. */
  base64: string;
  mimeType: "image/jpeg";
}

/**
 * Compresses a label image to max 1024px (longest side), JPEG at 80% quality.
 * Strips the data URI prefix — returns raw base64 for the Edge Function.
 *
 * Direct port of ImageCompressor.kt:
 *   MAX_DIMENSION = 1024, Bitmap.CompressFormat.JPEG, quality = 80
 */
export async function compressLabelImage(file: File): Promise<CompressedImage> {
  return new Promise((resolve, reject) => {
    const img = new Image();

    img.onload = () => {
      // 1. Calculate scaled dimensions — preserve aspect ratio
      let { width, height } = img;
      if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
        const scale = MAX_DIMENSION / Math.max(width, height);
        width = Math.round(width * scale);
        height = Math.round(height * scale);
      }

      // 2. Draw to canvas at target size
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      if (!ctx) return reject(new Error("Canvas 2D context unavailable"));
      ctx.drawImage(img, 0, 0, width, height);

      // 3. Export as JPEG at 80% quality
      canvas.toBlob(
        (blob) => {
          if (!blob) return reject(new Error("Canvas compression failed"));
          const reader = new FileReader();
          reader.onloadend = () => {
            const dataUrl = reader.result as string;
            // Strip "data:image/jpeg;base64," prefix — Edge Function wants raw base64
            const base64 = dataUrl.split(",")[1];
            resolve({ base64, mimeType: "image/jpeg" });
          };
          reader.onerror = () => reject(new Error("FileReader failed"));
          reader.readAsDataURL(blob);
        },
        "image/jpeg",
        JPEG_QUALITY
      );
    };

    img.onerror = () => reject(new Error("Failed to load image for compression"));
    img.src = URL.createObjectURL(file);
  });
}
