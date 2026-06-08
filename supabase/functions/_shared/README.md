# Shared Utilities for Supabase Edge Functions (`_shared/`)

This directory contains reusable utility modules, types, and prompting templates shared across all backend Supabase Edge Functions.

## 🎯 Major Function & Purpose

The `_shared` folder serves as the central backend library. Since all core business logic (e.g., nutrition scaling, meal slotting, LLM prompting) is centralized in Supabase Edge Functions to prevent behavior drift between the Android and Next.js Web applications, this directory ensures that helper functions, data mapping utilities, and Gemini prompts are written, maintained, and updated in exactly one place.

---

## 📂 Files

* **[`cors.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared/cors.ts)**: Configures Cross-Origin Resource Sharing (CORS) headers and handles pre-flight requests for the edge functions.
* **[`fdc-mapper.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared/fdc-mapper.ts)**: Maps USDA FoodData Central (FDC) API search responses to the project's internal nutrition information structures.
* **[`json-utils.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared/json-utils.ts)**: Extracts and parses JSON payloads from LLM response texts.
* **[`macro-calculator.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared/macro-calculator.ts)**: Acts as the canonical source of truth for portion scaling, macro calculations, and daily log totals on the backend.
* **[`meal-progression.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared/meal-progression.ts)**: Predicts the next logical meal slot to suggest based on logged meals and the current hour.
* **[`prompts.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared/prompts.ts)**: Serves as the central repository for LLM system instructions and prompts (food parsing, label scanning, meal recommendations).


---

## 🔌 External Dependencies

This folder contains pure TypeScript modules designed to run in Deno Deploy's serverless runtime.
* **No external third-party library imports** (e.g., npm or deno.land imports) are used within these files.
* Uses the native serverless environment global (`Deno.env.get`).
