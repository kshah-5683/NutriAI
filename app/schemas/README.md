# Room Database Exported Schemas (`app/schemas/`)

This directory contains the exported SQLite database schemas for the Android application's local Room database, organized by package namespace.

## 🎯 Major Function & Purpose

The `schemas` folder serves as the build-time history for local database schemas. The Android **Room Persistence Library** compiles these JSON files during annotation processing. They represent the exact schema structure (tables, indices, columns, datatypes, and queries) for every database version, enabling developers and automated tests to validate migration paths and detect schema regressions.

---

## 📂 Subdirectories

* **[`com.app.nutriai.data.local.NutriAiDatabase/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/schemas/com.app.nutriai.data.local.NutriAiDatabase)**: Houses the versioned JSON files (`1.json` through `10.json`) representing the schema versions of the local SQLite database.

---

## 🔌 External Dependencies

* **Android Jetpack Room**: SQLite object mapping library that outputs these schema definitions.
