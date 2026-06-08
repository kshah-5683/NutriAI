# Domain Data Models (`com/app/nutriai/domain/model/`)

This directory houses the pure Kotlin business data model classes representing application entities.

## 🎯 Major Function & Purpose

The `model` package declares the core data schemas representing the application's domain objects (e.g., users, daily log entries, catalogs, goals, and recommendations). These classes are declared as plain Kotlin data structures with zero framework dependencies (e.g., no Room annotations or JSON serialization tags) to keep them decoupled from specific database or API schemas.

---

## 📂 Data Model Files

* **[`AuthState.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/AuthState.kt)**: Represents the user login session validation status.
* **[`Catalog.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/Catalog.kt)**: Model detailing catalog headers and properties.
* **[`CatalogMatch.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/CatalogMatch.kt)**: Structure mapping AI search names to catalog database items.
* **[`DailyLog.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/DailyLog.kt)**: Represents a single logged entry in the food diary.
* **[`DailyMacroSummary.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/DailyMacroSummary.kt)**: Aggregated macro summaries representing daily calorie totals.
* **[`ExtractedLabelData.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/ExtractedLabelData.kt)**: Output structures for OCR vision scanning of food labels.
* **[`FoodItem.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/FoodItem.kt)**: Base food database catalog model.
* **[`IngredientKey.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/IngredientKey.kt)**: Maps name/quantity keys for multi-ingredient recipe lists.
* **[`MacroGoals.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/MacroGoals.kt)**: User target macronutrient budgets.
* **[`MealType.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/MealType.kt)**: Sealed class mapping meal categories.
* **[`MonthlyMacroSummary.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/MonthlyMacroSummary.kt)**: Aggregated average macros for monthly dashboard indexes.
* **[`NutritionInfo.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/NutritionInfo.kt)**: Decoupled mapped structures referencing nutrition lookups.
* **[`ParsedFood.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/ParsedFood.kt)**: Model representing AI parsed food descriptions.
* **[`Recommendation.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/Recommendation.kt)**: Structure representing recommended meals.
* **[`User.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/User.kt)**: Mapped profile representation.
* **[`UserProfile.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model/UserProfile.kt)**: User dietary and target weights parameters.

---

## 🔌 External Dependencies

* **None**: Pure Kotlin language structures.
