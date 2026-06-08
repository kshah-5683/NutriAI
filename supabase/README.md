# Supabase Backend Configuration (`supabase/`)

This directory serves as the root folder for the backend database configuration, schema migrations, and serverless edge functions for NutriAI.

## 🎯 Major Function & Purpose

The `supabase` folder contains the infrastructure-as-code configuration for the backend of the platform. It handles the local development lifecycle, manages database versioning via PostgreSQL migrations, provisions row-level security (RLS) policies, and runs server-side logic in serverless edge functions to align the data flow and calculations between the Android app and the Next.js web companion.

---

## 📂 Subdirectories & Files

* **[`functions/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/functions)**: Houses serverless edge functions built on the Deno runtime to execute centralized, cross-platform business logic.
* **[`migrations/`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations)**: Contains PostgreSQL SQL scripts that version and provision the database tables, views, RLS policies, indexes, and triggers.
* **[`config.toml`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/config.toml)**: Defines Supabase CLI parameters, local port mappings, API routes, local authentication thresholds, and environment configurations.
* **[`.gitignore`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/.gitignore)**: Standard file specifying directories and temporary data to exclude from version control.

---

## 🔌 External Dependencies

* **Supabase CLI**: Command-line tool used to manage the backend lifecycle, start local containers, and deploy migrations or edge functions.
* **Docker**: Required by the Supabase CLI to run local containers (e.g., PostgreSQL, Kong gateway, GoTrue auth engine, and Deno runtime).
