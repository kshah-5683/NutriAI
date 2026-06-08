# Helper Utilities (`webapp/lib/utils/`)

This directory contains standalone helper modules, formatting utilities, and client-side calculators.

## 🎯 Major Function & Purpose

The functions in this folder provide standard algorithms and utilities for string formatting, mathematical conversions, and media compression. These modules are stateless and side-effect free, promoting code reusability across components and pages.

---

## 📂 Utility Files

* **[`compute-daily-totals.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/compute-daily-totals.ts)**: Summarizes individual daily log entries into a single aggregated caloric and macronutrient daily total.
* **[`constants.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/constants.ts)**: Hosts static configuration parameters, validation limits, and navigation paths.
* **[`format.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/format.ts)**: Handles localization formatting for dates, decimal macros, and unit weights.
* **[`image-compressor.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/image-compressor.ts)**: Shrinks and optimizes food label pictures in the browser before sending them to the label-scanning vision pipeline.
* **[`macro-calculator.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/macro-calculator.ts)**: Companion client-side utility for computing serving multipliers and scaling macros for UI previews.
* **[`meal-progression.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/meal-progression.ts)**: Companion client-side utility that determines the user's active meal period (Breakfast, Lunch, Dinner, Snack) based on the local time.
* **[`prefetch-trigger.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/prefetch-trigger.ts)**: Triggers background preload API fetches for upcoming recommendations.
* **[`unit-converter.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils/unit-converter.ts)**: Maps portion quantities between discrete serving and metric systems.

---

## 🔌 External Dependencies

* **None**: Employs only standard Web/DOM APIs (e.g., HTMLCanvasElement for image compression) and native TypeScript utilities.
