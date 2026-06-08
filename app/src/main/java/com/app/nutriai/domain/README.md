# Domain Layer (`com/app/nutriai/domain/`)

This directory houses the core business rules, repository interfaces, and data models for the Android application.

## 🎯 Major Function & Purpose

The `domain` layer defines the functional framework of the application. As the central core of **Clean Architecture**, it contains no dependencies on external frameworks, databases, or UI libraries (it is a pure Kotlin module). It encapsulates the application's domain logic into reusable Use Cases, defines model structures, and specifies Repository interfaces that downstream layers implementation must fulfill.

---

## 📂 Subdirectories

* **[`model/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/model)**: Declares immutable Kotlin data classes representing key domain concepts (e.g., `FoodItem`, `DailyLog`, `UserProfile`, `MacroGoals`).
* **[`repository/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/repository)**: Defines interface contracts for repositories coordinating logins, logs, catalogs, sync updates, and recommendations.
* **[`usecase/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/domain/usecase)**: Single-responsibility workflow classes implementing individual app actions (e.g., parsing food, logging entries, syncing data, checking auth status).

---

## 🔌 External Dependencies

* **None**: Pure Kotlin language structures with zero external framework dependencies to ensure isolation and testability.
