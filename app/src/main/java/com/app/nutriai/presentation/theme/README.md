# Material3 Design System Theme (`com/app/nutriai/presentation/theme/`)

This directory houses the Material Design 3 (M3) design system tokens, color palettes, typographies, and theme configurations.

## 🎯 Major Function & Purpose

The `theme` package enforces the app's visual identity. It defines the light and dark color schemes, maps custom macro branding colors (calories, protein, carbs, fat), maps font files to type scales, and configures dynamic color profiles on Android 12+ devices.

---

## 📂 Theme Configuration Files

* **[`Color.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/theme/Color.kt)**: Declares hex color constants, light/dark color mappings, and macro specific tracking colors.
* **[`Type.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/theme/Type.kt)**: Configures the application's Material3 typography scales, mapping Custom font weights (Regular, Medium, Semibold) to display, title, body, and label text sizes.
* **[`Theme.kt`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/app/src/main/java/com/app/nutriai/presentation/theme/Theme.kt)**: Defines standard `LightColorScheme` and `DarkColorScheme` builders, coordinates dynamic coloring, and exposes the global `NutriAiTheme` Composable wrapper.

---

## 🔌 External Dependencies

* **Android Jetpack Compose Material3**: The standard Material Design 3 component configurations.
