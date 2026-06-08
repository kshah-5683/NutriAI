# Android Assets (`app/src/main/assets/`)

This directory contains static, read-only assets bundled with the Android application package (APK) that are accessible via the `AssetManager` at runtime.

## 🎯 Major Function & Purpose

The `assets` folder hosts static data files that need to be read programmatically by the application. In this project, it is primarily used to store the raw food database seed files, which are parsed and loaded into the local SQLite database via Room when the application is initialized for the first time.

---

## 📂 Files

* **[`ifct2017.csv`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/assets/ifct2017.csv)**: Contains raw food entry rows and baseline macro values (energy, protein, carbs, fat) from the Indian Food Composition Tables 2017, used to seed reference database catalog items.

---

## 🔌 External Dependencies

* **None**: Statically served via the native Android `AssetManager` API.
