# Repository Interfaces (`com/app/nutriai/domain/repository/`)

This directory contains abstract repository interfaces defining the data access contracts for the application.

## 🎯 Major Function & Purpose

The `repository` package implements boundary boundaries. By declaring abstract interfaces here, the domain layer specifies exactly what data operations the application supports (such as querying local databases, registering users, calling Gemini API parsers, or synchronizing offline updates) without knowing how those actions are implemented. The concrete implementations are isolated in the data layer, allowing for clean decoupling and simplified mock testing.

---

## 📂 Repository Interface Files

* **[`AiRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/AiRepository.kt)**: Contract for natural language food item parsing and vision-based label extraction.
* **[`AuthRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/AuthRepository.kt)**: Contract coordinating user registration, sign-in, and sign-out sessions.
* **[`CatalogRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/CatalogRepository.kt)**: Contract governing database operations for the user's custom food catalog.
* **[`DailyLogRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/DailyLogRepository.kt)**: Contract managing local and remote food diary log inputs.
* **[`FoodRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/FoodRepository.kt)**: Contract querying custom database food details and handling macro scaling conversions.
* **[`NutritionRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/NutritionRepository.kt)**: Contract defining nutrition lookups against USDA FDC and IFCT reference catalogs.
* **[`RecommendationRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/RecommendationRepository.kt)**: Contract querying cache or live AI recommendations.
* **[`SyncRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/SyncRepository.kt)**: Contract managing local-to-cloud data reconciliation routines.
* **[`UserRepository.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository/UserRepository.kt)**: Contract managing profile settings and target budgets.

---

## 🔌 External Dependencies

* **None**: Pure Kotlin interface definitions.
