# Presentation Layer (`com/app/nutriai/presentation/`)

This directory houses the user interface components, Compose screen layouts, navigation routing, ViewModels, and visual themes.

## 🎯 Major Function & Purpose

The `presentation` layer renders screens and coordinates user interactions. Built entirely using **Jetpack Compose** (declarative UI), it coordinates screen layouts, hooks up navigation structures, translates backend flows into reactive ViewModel UI States (using StateFlows), and defines the Material3 visual theme (colors, fonts, and dark mode controls).

---

## 📂 Subdirectories & Files

### Subdirectories
* **[`components/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/components)**: Reusable, stateless Compose components (e.g., custom macro circles, charts, item cards).
* **[`navigation/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/navigation)**: Configures routes definitions, bottom tab bars, and the main `NavHost` binding.
* **[`screens/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/screens)**: Page-level Compose screen modules (Home, Log, Catalog, Insights, Auth) and their associated ViewModel coordinators.
* **[`theme/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/theme)**: Configures Material3 Color Schemes, font definitions, and the global `Theme` wrapper.

### Root Files
* **[`MainActivity.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/MainActivity.kt)**: The Android app entrypoint Activity that enables edge-to-edge rendering and loads the root Compose context.
* **[`ForegroundSyncViewModel.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/ForegroundSyncViewModel.kt)**: ViewModel coordinating foreground sync triggers, network state status overlays, and loading indicators.

---

## 🔌 Core UI Dependencies

* **Jetpack Compose**: Declarative UI engine (Foundation, Layout, Material3).
* **Compose Navigation**: Handles routing and animations between screens.
* **Lifecycle ViewModel & Compose Lifecycle**: Manages screen state caches surviving configuration changes.
