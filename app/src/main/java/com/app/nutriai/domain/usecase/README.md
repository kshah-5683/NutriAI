# Domain Use Cases (`com/app/nutriai/domain/usecase/`)

This directory houses the single-responsibility use case classes orchestrating the application's business logic.

## 🎯 Major Function & Purpose

The `usecase` package coordinates the workflows of the application. Each class represents a single, testable, and reusable user action (e.g., logging a food item, synchronizing local edits, or requesting meal suggestions). By wrapping these workflows in distinct classes, the UI presentation layer (ViewModels) can execute operations without knowing the details of repositories, database triggers, or external integrations.

---

## 📂 Use Case Files

* **[`ParseFoodWithAiUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/ParseFoodWithAiUseCase.kt)**: Coordinates AI-driven natural language food logging.
* **[`ExtractLabelUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/ExtractLabelUseCase.kt)** & **[`CleanupLabelPhotosUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/CleanupLabelPhotosUseCase.kt)**: Coordinate nutrition facts label photo scanning and clean up temporary layout photos.
* **[`LookupNutritionUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/LookupNutritionUseCase.kt)**: Grounds parsed food items against USDA and IFCT databases.
* **[`ResolveCatalogCacheUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/ResolveCatalogCacheUseCase.kt)**: Maps parsed food item names to existing database catalog definitions.
* **[`LogFoodUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/LogFoodUseCase.kt)**: Normalizes portion sizes to 100g database standards and logs food entries.
* **[`GetDailyLogsUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/GetDailyLogsUseCase.kt)** & **[`UpdateDailyLogUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/UpdateDailyLogUseCase.kt)**: Manage querying and editing daily logs.
* **[`SearchCatalogUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/SearchCatalogUseCase.kt)**, **[`GetCatalogsUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/GetCatalogsUseCase.kt)**, **[`UpdateFoodUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/UpdateFoodUseCase.kt)** & **[`DeleteFoodUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/DeleteFoodUseCase.kt)**: Manage user custom food catalog entries.
* **[`GetTimeBasedRecsUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/GetTimeBasedRecsUseCase.kt)**: Formulates calorie budgets and target targets to fetch compliant meal suggestions.
* **[`SyncDataUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/SyncDataUseCase.kt)**: Reconciles local changes with cloud servers.
* **[`SignInUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/SignInUseCase.kt)**, **[`SignInWithGoogleUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/SignInWithGoogleUseCase.kt)**, **[`SignUpUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/SignUpUseCase.kt)**, **[`SignOutUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/SignOutUseCase.kt)** & **[`GetAuthStateUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/GetAuthStateUseCase.kt)**: Manage authentication sessions and login status checks.
* **[`InitializeUserUseCase.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase/InitializeUserUseCase.kt)**: Handles provisioning default settings when a user signs up.

---

## 🔌 External Dependencies

* **None**: Pure Kotlin business workflows.
