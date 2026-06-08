# Web Application Library Root (`webapp/lib/`)

This directory serves as the core library and state module for the Next.js Web companion application.

## 🎯 Major Function & Purpose

The `lib` folder organizes all the frontend business logic, state stores, database initializers, type contracts, and helper utilities. It decouples the UI layout components (`webapp/components/` and `webapp/app/`) from the raw data models and remote services, providing a single source of truth for front-end operations.

---

## 📂 Subdirectories

* **[`hooks/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/hooks)**: Houses custom React hooks wrapped in TanStack Query (React Query) to fetch, cache, and mutate database rows and execute Edge Functions.
* **[`stores/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/stores)**: Contains global Zustand state stores for managing cross-component UI state (e.g., date selections, drawer toggles, theme configurations, logging states).
* **[`supabase/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/supabase)**: Instantiates database client instances configured for either client-side (browser) or server-side (server components, route handlers) Next.js environments.
* **[`types/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types)**: Defines interfaces and contracts, aligning database rows and API models to decoupled application domain objects.
* **[`utils/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/utils)**: Statless helper scripts managing text localization, image compression, units scaling, and meal progression lookups.

---

## 🔌 External Dependencies

* **`@supabase/supabase-js`** & **`@supabase/ssr`**: Supabase authentication and database connection layers.
* **`@tanstack/react-query`**: Core state cache and remote data syncing engine.
* **`zustand`**: Global client UI state controller.
* **`react`**: Core rendering library.
