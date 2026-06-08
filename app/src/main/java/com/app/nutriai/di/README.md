# Dependency Injection Module (`com/app/nutriai/di/`)

This directory houses the Hilt Dependency Injection (DI) module configurations for the application.

## 🎯 Major Function & Purpose

This package configures dependency lifetimes, scope bounds, and builder injection wrappers using **Dagger Hilt**. Centralizing dependencies in these modules prevents manual instantiation, handles scoping (e.g., Singletons for databases and clients), and simplifies mocking or replacing components during testing.

---

## 📂 Dependency Modules

* **[`AppModule.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/di/AppModule.kt)**: Configures global app scope singletons, including OkHttp network clients, JSON serializers, and Retrofit adapters.
* **[`DatabaseModule.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/di/DatabaseModule.kt)**: Instantiates the Room Database singleton (`NutriAiDatabase`) and binds local Data Access Object (DAO) instances (User, FoodItem, DailyLog, Catalog).
* **[`SupabaseModule.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/di/SupabaseModule.kt)**: Initializes the remote client SDK configurations for Supabase database queries, Auth sessions, and remote Edge Function execution.

---

## 🔌 External Dependencies

* **Dagger Hilt & Hilt Android**: Annotation processing compiler and framework bindings automating dependency injection graph resolution.
