# TypeScript Type Definitions (`webapp/lib/types/`)

This directory houses the TypeScript interfaces, types, and schemas used throughout the web application to guarantee compile-time type safety.

## 🎯 Major Function & Purpose

The types in this folder define the data structures for all application states, database tables, and AI services. Keeping interfaces structured and centralized prevents code discrepancies, guarantees autocomplete and validation compile-time safety, and defines the structural boundary models that convert raw database shapes into clean domain schemas.

---

## 📂 Type Files

* **[`ai.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types/ai.ts)**: Defines data schemas for LLM inputs, parsed food entries, and label detection structures.
* **[`database.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types/database.ts)**: Contains the database schema types mapping directly to the Supabase PostgreSQL tables.
* **[`domain.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types/domain.ts)**: Defines camelCase application domain models (such as `FoodItem`, `DailyLog`, and `UserProfile`) to decouple UI components from snake_case database rows.
* **[`insights.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types/insights.ts)**: Types representing historical nutrition trends, charting indexes, and macro splits.
* **[`recommendation.ts`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/webapp/lib/types/recommendation.ts)**: Outlines interfaces for AI-driven recipe and ingredient recommendation items.

---

## 🔌 External Dependencies

* **None**: Pure TypeScript type and interface declarations.
