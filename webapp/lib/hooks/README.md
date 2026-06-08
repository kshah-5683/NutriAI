# Custom React Hooks (`webapp/lib/hooks/`)

This directory houses custom React Hooks that manage data fetching, local caching, state synchronization, and backend mutations for the NutriAI web companion application.

## 🎯 Major Function & Purpose

The hooks in this folder serve as the interface layer between the React UI components and the Supabase backend (PostgreSQL database tables and Edge Functions). They wrap query and mutation logic using **TanStack Query (React Query)**, providing components with automatic data caching, optimistic UI updates, loading/error states, and cache invalidation protocols.

---

## 📂 Hook Files

* **[`use-add-to-catalog.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-add-to-catalog.ts)**: Handles inserting new custom food items into the user's catalog.
* **[`use-cached-recommendations.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-cached-recommendations.ts)**: Manages querying, prefetching, and updating local cached AI meal recommendations.
* **[`use-catalog-items.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-catalog-items.ts)**: Fetches the active, non-deleted custom food definitions from the user's catalog.
* **[`use-daily-logs.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-daily-logs.ts)**: Retrieves logged food entries and totals for a selected calendar date.
* **[`use-delete-food.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-delete-food.ts)**: Handles soft-deleting a custom food definition from the catalog.
* **[`use-delete-log.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-delete-log.ts)**: Removes a logged food occurrence from the daily food diary.
* **[`use-insights-data.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-insights-data.ts)**: Gathers historical log summaries to generate progress and macro distribution chart data.
* **[`use-log-food.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-log-food.ts)**: Logs standard food items consumed by the user to the daily log table.
* **[`use-log-recipe.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-log-recipe.ts)**: Compiles and logs multi-ingredient custom recipes to the food diary.
* **[`use-macro-goals.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-macro-goals.ts)**: Retrieves the user's daily macronutrient calorie and weight target preferences.
* **[`use-nutrition-lookup.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-nutrition-lookup.ts)**: Queries USDA FDC and IFCT reference databases for grounding nutrition facts.
* **[`use-parse-food.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-parse-food.ts)**: Parses natural language food input descriptions via AI backend handlers.
* **[`use-recommendations.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-recommendations.ts)**: Fetches dynamically generated meal and diet recommendations from the AI engine.
* **[`use-scan-label.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-scan-label.ts)**: Submits images of nutrition facts panels to the label-scanning vision pipeline.
* **[`use-update-food.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-update-food.ts)**: Modifies details of custom food entries in the catalog.
* **[`use-update-goals.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-update-goals.ts)**: Saves new macronutrient target preferences.
* **[`use-update-log.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-update-log.ts)**: Updates quantities and units of logged food entries.
* **[`use-user-profile.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks/use-user-profile.ts)**: Fetches and updates user profile data, including diet preferences and allergen lists.

---

## 🔌 External Dependencies

* **`@tanstack/react-query`**: State management library that handles asynchronous state queries, caching, automatic polling, and optimistic UI mutations.
* **`react`**: Core React Hooks (`useMemo`, `useCallback`, etc.).
* **`@supabase/supabase-js`** / **`@supabase/ssr`**: Utilized indirectly via context providers to query database tables and execute Edge Functions.
