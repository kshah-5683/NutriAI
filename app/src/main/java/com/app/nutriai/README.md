# NutriAI Android Application Package (`com.app.nutriai/`)

This directory houses the core Kotlin codebase for the NutriAI Android application, organized using **Clean Architecture** conventions.

## 🎯 Major Function & Purpose

This package namespace coordinates all database management, API lookups, offline caching, background synchronization, and UI screens for the Android client. By separating operations into distinct architectural layers (`domain`, `data`, and `presentation`), the project enforces clear boundaries, guarantees testability, and isolates framework-specific concerns.

---

## 📂 Subdirectories

* **[`domain/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain)**: Core business layer (pure Kotlin, zero framework dependencies). Contains:
  * `model/`: Domain data schemas (e.g., `DailyLog`, `FoodItem`, `MacroGoals`).
  * `repository/`: Interfaces specifying operations implemented by the data layer.
  * `usecase/`: Single-responsibility classes coordinating actions (e.g., logging a food, recalculating macros, triggering sync).
* **[`data/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/data)**: Data access layer. Handles:
  * `local/`: Local SQLite caching via the Room database library.
  * `remote/`: API clients (Retrofit configuration and Supabase calls).
  * `repository/`: Concrete implementations of domain repositories.
  * `sync/`: Algorithms coordinating database updates, conflict resolution, and changes upload.
* **[`presentation/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation)**: User interface layer built with Jetpack Compose. Contains:
  * `screens/`: Composite layouts for pages (Home, Catalog, Log, Insights, Auth).
  * `navigation/`: Routes host and tab bars routing logic.
  * `theme/`: Material3 color profiles, light/dark configurations, and typography styles.
  * `components/`: UI helpers like macro progress circles.
* **[`di/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/di)**: Hilt modules providing dependency injection bindings (e.g., databases, API services, repositories, use cases).
* **[`work/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/work)**: Custom WorkManager jobs running database cloud synchronization background workers.
* **[`util/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util)**: Shared constants, status envelopes, and network checking helpers.

---

## 🔌 Core Dependencies

* **Android Jetpack Suite**: Compose UI, Compose Navigation, Room Database, LiveData/StateFlow, Hilt Dependency Injection, and WorkManager.
* **Kotlin Coroutines**: Supports asynchronous programming and reactive flows.
* **Retrofit & OkHttp**: Networking libraries for external API integrations (OpenFoodFacts, USDA, Supabase).
* **Gemini API SDK**: Integrates on-device and remote AI functionalities.
