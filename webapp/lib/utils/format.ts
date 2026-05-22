/**
 * Display formatting utilities.
 * Port of MacroFormatUtils.kt and DateNavigationHeader formatting from Android.
 */

/**
 * Formats a macro value for display.
 * Whole numbers drop the decimal: 52.0 → "52"
 * Others show one decimal place: 5.3 → "5.3"
 *
 * Direct port of Double.formatMacro() from MacroFormatUtils.kt.
 */
export function formatMacro(value: number): string {
  return value % 1 === 0 ? value.toFixed(0) : value.toFixed(1);
}

/**
 * Formats a date for the date navigation header.
 * E.g. "May 14, 2026" — matches Android's DateNavigationHeader display.
 */
export function formatHeaderDate(date: Date): string {
  return date.toLocaleDateString("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
  });
}

/**
 * Formats a date as a short string for chart x-axis labels.
 * E.g. "May 14"
 */
export function formatShortDate(date: Date): string {
  return date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

/**
 * Returns the ISO date string (YYYY-MM-DD) for a given date in local time.
 * Used as a stable cache key for daily log queries.
 */
export function toLocalDateString(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/**
 * Returns start-of-day timestamp (ms) for a given date in local time.
 */
export function startOfDayMs(date: Date): number {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

/**
 * Returns end-of-day timestamp (ms) for a given date in local time.
 */
export function endOfDayMs(date: Date): number {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d.getTime();
}
