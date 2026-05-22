/**
 * Unit conversion utilities for display ↔ stored quantity.
 * Direct port of Android's UnitConverter.kt (toDisplayQty / fromDisplayQty).
 *
 * In the database, gram-unit entries store a multiplier relative to PER_100G_BASE:
 *   consumedQty = 2.0 means 200g (2.0 × 100g base).
 *
 * These helpers convert between the stored multiplier and the human-readable
 * gram display so users see "200 g" instead of "2".
 *
 * Non-gram units (serving, tbsp, cup, piece, etc.) are unchanged.
 */

const PER_100G_BASE = 100;

/**
 * Returns true if the unit represents grams.
 * Matches Android UnitConverter.isGramsUnit().
 */
export function isGramUnit(unit: string): boolean {
  const u = unit.toLowerCase().trim();
  return u === "g" || u === "grams" || u === "gram";
}

/**
 * Converts a stored consumedQty (multiplier) to the display quantity.
 *
 * For gram units: consumedQty × 100  (e.g. 2.0 → 200 g)
 * For all other units: unchanged     (e.g. 2.0 tbsp → 2.0 tbsp)
 *
 * Result is rounded to 2 decimal places to eliminate IEEE 754 double-precision
 * artifacts from the × 100 multiplication.
 * e.g. stored 2.5499999... × 100 = 254.9999997 → displayed as 255 after rounding.
 *
 * Use at every display / edit-pre-fill site.
 */
export function toDisplayQty(consumedQty: number, unit: string): number {
  const raw = isGramUnit(unit) ? consumedQty * PER_100G_BASE : consumedQty;
  return Math.round(raw * 100) / 100;
}

/**
 * Converts a display / user-entered quantity back to the stored multiplier.
 *
 * For gram units: displayQty ÷ 100  (e.g. 200 g → 2.0)
 * For all other units: unchanged
 *
 * Use in every save path before writing consumedQty to the database.
 */
export function fromDisplayQty(displayQty: number, unit: string): number {
  return isGramUnit(unit) ? displayQty / PER_100G_BASE : displayQty;
}
