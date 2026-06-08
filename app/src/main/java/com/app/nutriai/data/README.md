# Data Layer (`com/app/nutriai/data/`)

This directory houses the data access implementations, local database entities, networking adapters, and synchronization routines for the Android application.

## 🎯 Major Function & Purpose

The `data` layer bridges the app's abstract business domain and the physical storage engines or remote web interfaces. It is responsible for offline caching (storing data in Room SQLite tables), calling external APIs (executing queries to Supabase Edge Functions, USDA FoodData Central, or Gemini), implementing domain repository contracts, and coordinating background synchronization.

---

## 📂 Subdirectories

* **[`local/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data/local)**: Configures the Room database client (`NutriAiDatabase`) and local SQL queries (Data Access Objects / DAOs) for offline caching.
* **[`remote/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data/remote)**: Sets up Retrofit API network clients, request interceptors, and direct endpoints to interface with backend cloud systems.
* **[`repository/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data/repository)**: Contains the concrete implementation classes of the repositories defined in the domain layer, coordinating cached local reads and remote writes.
* **[`sync/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data/sync)**: Implements database sync managers that reconcile local updates with cloud updates, handling conflict resolution.

---

## 🔌 External Dependencies

* **Android Room Persistence Library**: Powers SQLite mapping, caching, and migrations.
* **Retrofit & OkHttp**: Handles HTTP REST networking requests, timeouts, and authorization headers.
* **Supabase Client**: Interfaces with remote database, Auth, and Edge Function endpoints.
