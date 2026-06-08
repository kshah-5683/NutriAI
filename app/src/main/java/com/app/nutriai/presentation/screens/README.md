# Application Page Screens (`com/app/nutriai/presentation/screens/`)

This directory houses the page-level visual screens and UI ViewModels coordinating user interactions.

## 🎯 Major Function & Purpose

The `screens` package represents the core interactive views of the Android application. Each subfolder acts as a distinct user feature module, housing Compose screen layouts and corresponding ViewModels. ViewModels fetch and store screen state flows (via Kotlin StateFlows) to handle device rotation updates and map UI events back to domain use cases.

---

## 📂 Feature Screens

* **[`auth/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/screens/auth)**: Screens managing user login credentials validation and account registration sheets.
* **[`catalog/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/screens/catalog)**: Search dashboards to review, edit, or insert custom entries into the user's catalog.
* **[`home/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/screens/home)**: Core dashboard displaying remaining calorie limits, macro progress, and lists of logged meals.
* **[`insights/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/screens/insights)**: Visual layouts rendering weekly bar graphs, monthly lines, and yearly averages.
* **[`log/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/screens/log)**: Multi-modal logging pages supporting natural text inputs, label scan triggers, and manual recipe compilers.

---

## 🔌 External Dependencies

* **Android Jetpack Compose**: Renders declarations.
* **Android ViewModel Lifecycle**: Caches screen states.
