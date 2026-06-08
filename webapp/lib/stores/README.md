# Zustand State Stores (`webapp/lib/stores/`)

This directory houses the global client-side state management stores for the web application, built using **Zustand**.

## 🎯 Major Function & Purpose

The stores in this folder manage shared client-side application states that need to be accessed or updated across disparate components (e.g., current date selection, theme preferences, and active food logging form states). This keeps components clean and decoupled, bypassing React's prop-drilling pattern.

---

## 📂 Store Files

* **[`catalog-store.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/stores/catalog-store.ts)**: Manages UI state and search options for the food catalog drawer.
* **[`date-store.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/stores/date-store.ts)**: Coordinates the active date selected by the user for logging and dashboard stats.
* **[`log-form-store.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/stores/log-form-store.ts)**: Manages complex state for the multi-mode food logger (AI input text, parsed items, manual adjustments, and nutrition lookup results).
* **[`theme-store.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/stores/theme-store.ts)**: Coordinates color theme settings (light, dark, system), syncing preferences to local storage and updating stylesheet bindings.

---

## 🔌 External Dependencies

* **`zustand`**: Lightweight, store-centric state management library.
