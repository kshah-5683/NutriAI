# Gradle Build Wrapper & Catalog (`gradle/`)

This directory houses the Gradle Wrapper binaries, daemon properties, and the centralized dependency version catalog for the Android project.

## 🎯 Major Function & Purpose

The `gradle` folder configures the build system and dependency versions for the Android compilation process. It ensures developer environment consistency by locking build tool versions (via Gradle Wrapper properties) and consolidates package definitions in a single central repository (via Version Catalogs), simplifying updates and imports.

---

## 📂 Subdirectories & Files

* **[`libs.versions.toml`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/gradle/libs.versions.toml)**: Centralized Version Catalog declaring all external Android library versions, dependencies, bundles, and build plugins (e.g., Jetpack Compose, Room, Hilt, Retrofit).
* **[`gradle-daemon-jvm.properties`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/gradle/gradle-daemon-jvm.properties)**: Configures the Java Virtual Machine (JVM) execution runtime parameters for Gradle build daemons.
* **[`wrapper/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/gradle/wrapper)**: Contins the bootstrap binaries and configuration properties to download and execute the exact locked version of the Gradle build runner (`gradle-wrapper.jar` and `gradle-wrapper.properties`).

---

## 🔌 External Dependencies

* **Gradle**: Build execution system tool.
* **Java Development Kit (JDK)**: Execution environment required to build the Kotlin/Android application (configured for JVM targets).
