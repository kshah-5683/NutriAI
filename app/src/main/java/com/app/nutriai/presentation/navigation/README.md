# Application Navigation Routing (`com/app/nutriai/presentation/navigation/`)

This directory coordinates the screen navigation architecture, route maps, and deep-linking hooks for the application.

## 🎯 Major Function & Purpose

The `navigation` package binds UI views to navigation paths. Using **Compose Navigation**, it defines a sealed route catalog, instantiates the main application `NavHost` containing all screen graphs (Home, Log, Catalog, Insights, Auth), and links bottom bar click actions to corresponding page views.

---

## 📂 Navigation Files

* **[`Screen.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/navigation/Screen.kt)**: Sealed class declaring screen route paths and visual bar tags (icons, string labels) for bottom navigation tabs.
* **[`NutriAiNavHost.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/navigation/NutriAiNavHost.kt)**: The main routing graph wrapping page screens, injecting ViewModels, and coordinating transitional animations.

---

## 🔌 External Dependencies

* **Android Jetpack Compose Navigation**: Powers routing engines, navigation controller handlers, and page transition frameworks.
