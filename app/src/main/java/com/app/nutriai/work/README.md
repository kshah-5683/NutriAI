# Background Tasks & Workers (`com/app/nutriai/work/`)

This directory houses the background workers and task schedulers running database synchronization routines for the application.

## 🎯 Major Function & Purpose

The `work` package manages background scheduling using **Android WorkManager**. It schedules and executes data synchronization workers to sync offline logs, updates, and custom catalogs with the remote Supabase database without interrupting the foreground user experience.

---

## 📂 Background Task Modules

* **[`SyncScheduler.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/work/SyncScheduler.kt)**: Configures and manages task scheduling boundaries (such as network requirements and retry policies) to schedule one-off or periodic sync worker operations.
* **[`SyncWorker.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/work/SyncWorker.kt)**: The Hilt-injected WorkManager background execution class that runs the data synchronization pipeline.

---

## 🔌 External Dependencies

* **Android Jetpack WorkManager**: The standard background task library for running deferred, guaranteed tasks.
* **Hilt WorkManager**: Integration libraries allowing Hilt dependency injection directly into custom `ListenableWorker` constructors.
