# Supabase Edge Functions (`supabase/functions/`)

This directory houses the serverless backend functions for the NutriAI platform, powered by the Deno runtime.

## 🎯 Major Function & Purpose

The functions in this directory serve as the single source of truth for the project's core business logic (e.g., nutrition scaling, meal slotting, LLM prompting, and recipe compilation). Placing this logic in centralized Edge Functions prevents logic drift and behavior discrepancies between the Kotlin-based Android application and the TypeScript Next.js web application.

---

## 📂 Subdirectories

* **[`_shared/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/_shared)**: Contains shared utilities, types, CORS helpers, macro calculations, and prompting instructions.
* **[`log-food/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/log-food)**: Normalizes incoming serving macros to a 100g database baseline, creates catalog items if they do not exist, and logs consumed food entries.
* **[`log-recipe/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/log-recipe)**: Compiles multi-ingredient custom recipes and logs them to the daily food diary.
* **[`lookup-nutrition/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/lookup-nutrition)**: Searches and aggregates nutrition data across local databases, USDA FoodData Central, and the Indian Food Composition Tables (IFCT).
* **[`parse-food/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/parse-food)**: Uses Gemini to extract structured food items, quantities, and units from natural language logs.
* **[`prefetch-recommendations/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/prefetch-recommendations)**: Optimistically prefetches and caches next-meal recommendations ahead of time.
* **[`recommend-meals/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/recommend-meals)**: Generates macro-balanced, diet-compliant, and catalog-personalized meal suggestions.
* **[`scan-label/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/scan-label)**: Decodes and extracts nutritional fact panels from food label photos using multimodal vision capabilities.
* **[`update-daily-log/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/update-daily-log)**: Updates the portion sizes, serving units, or individual macros of logged entries.
* **[`update-food/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions/update-food)**: Edits base food definitions in the user's custom catalog and automatically recalculates historical logged occurrences.

---

## 🔌 External Dependencies

Since these functions run in the serverless Deno environment, dependencies are imported directly via URL:
* **Deno HTTP Server** (`https://deno.land/std/http/server.ts`): Powers incoming request handlers.
* **Supabase Client SDK** (`https://esm.sh/@supabase/supabase-js`): Connects to the database and handles backend authentication verification.
