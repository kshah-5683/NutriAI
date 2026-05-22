package com.app.nutriai.util

/**
 * Formats a Double macro value for display in text fields.
 * Whole numbers drop the decimal (52.0 → "52"); others show one decimal (5.3 → "5.3").
 */
fun Double.formatMacro(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else "%.1f".format(this)

/**
 * Formats a Double quantity for display.
 * Whole numbers drop the decimal (2.0 → "2"); fractional values use full precision (2.5 → "2.5").
 */
fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
