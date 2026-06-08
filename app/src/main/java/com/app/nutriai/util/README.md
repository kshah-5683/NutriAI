# Utility Helpers (`com/app/nutriai/util/`)

This directory houses core Kotlin helper classes, static variables, asset loaders, and client-side calculators.

## 🎯 Major Function & Purpose

The `util` package contains standalone, stateless helper functions and utility observers. They support UI formatting (rounding macro values), platform integrations (monitoring network connectivity, compressing images), system conversion algorithms (portion scaling), and initial database loading operations (parsing raw CSV seeds).

---

## 📂 Utility Files

* **[`ConnectivityObserver.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/ConnectivityObserver.kt)**: Observes active internet connections to trigger or hold cloud data sync.
* **[`Constants.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/Constants.kt)**: Stores global limits, thresholds, and configuration variables.
* **[`IfctCsvLoader.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/IfctCsvLoader.kt)**: Reads and parses the Indian Food Composition Tables (IFCT) raw CSV asset file to seed Room tables.
* **[`ImageCompressor.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/ImageCompressor.kt)**: Compresses food label photos in memory before uploading them to the vision scanner api.
* **[`MacroFormatUtils.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/MacroFormatUtils.kt)**: Helper utilities for decimal number values rounding.
* **[`Resource.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/Resource.kt)**: Standard sealed wrapper class defining async resource states (`Success`, `Error`, `Loading`).
* **[`SyncTriggerResult.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/SyncTriggerResult.kt)**: Result state wrapper for sync actions.
* **[`UnitConverter.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/UnitConverter.kt)**: Handles converting portion quantities between grams, standard measurements (teaspoon, tablespoon, cup), and discrete units.
* **[`GeminiPrompts.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/GeminiPrompts.kt)** & **[`GeminiLabelPrompts.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/util/GeminiLabelPrompts.kt)**: Local reference strings for LLM instructions.

---

## 🔌 External Dependencies

* **None**: Reuses standard Kotlin utilities and Android system platform APIs (e.g., `ConnectivityManager`).
