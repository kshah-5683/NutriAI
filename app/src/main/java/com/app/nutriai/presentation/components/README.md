# Reusable UI Components (`com/app/nutriai/presentation/components/`)

This directory houses the low-level, reusable Jetpack Compose UI components, custom visual widgets, data visualization charts, and status indicators.

## 🎯 Major Function & Purpose

The `components` folder coordinates the UI primitives for the Android application. These Composable functions remain stateless and decoupled from specific screen logic. They package styles, draw charts, and assemble card components (e.g., daily macro progress rings, stacked weekly bars, offline flags, and item lookup panels) to enforce visual consistency across all feature views.

---

## 📂 Composable Files

* **[`ConfirmDeleteDialog.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/ConfirmDeleteDialog.kt)**: Confirmation prompt displayed when removing catalog items or logged records.
* **[`DailyAverageCard.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/DailyAverageCard.kt)**: Grid layout showing user daily intake averages.
* **[`FoodLogItem.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/FoodLogItem.kt)**: Layout rendering a single logged food row with scaled macros.
* **[`MacroBarChart.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/MacroBarChart.kt)**, **[`MacroLineChart.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/MacroLineChart.kt)**, **[`MacroYearChart.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/MacroYearChart.kt)**: Drawing components displaying weekly distribution bars, monthly intake lines, and yearly averages.
* **[`MacroSummaryCard.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/MacroSummaryCard.kt)**: Custom rings displaying remaining daily macros on the home screen.
* **[`MealTypeSelector.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/MealTypeSelector.kt)**: Component to select meal categories (e.g., Breakfast, Snack).
* **[`NutritionFactsCard.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/NutritionFactsCard.kt)**: Displays food item description and parsed serving details.
* **[`NutritionMatchCard.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/NutritionMatchCard.kt)**: Overlay sheet showing database items mapped to search requests.
* **[`OfflineBanner.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/OfflineBanner.kt)**: Top status bar overlay indicating lack of connection.
* **[`PeriodSelector.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/PeriodSelector.kt)**: Segmented control toggling chart intervals.
* **[`RecommendationCard.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components/RecommendationCard.kt)**: Displays AI recommended meals with quick-logging buttons.

---

## 🔌 External Dependencies

* **Android Jetpack Compose**: Foundation component layout packages.
