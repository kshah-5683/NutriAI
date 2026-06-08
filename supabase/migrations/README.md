# Supabase Database Migrations (`supabase/migrations/`)

This directory contains the PostgreSQL database migration scripts that define the database schema, indexes, Row-Level Security (RLS) policies, and triggers for the NutriAI backend.

## 🎯 Major Function & Purpose

The migrations in this folder define and evolutionize the shared PostgreSQL database structure used by the Android and Next.js Web applications. They ensure database schema versioning, configure performance enhancements like search indexes, secure user data via Postgres RLS policies, and run background server actions such as automatic macro recalculations and cron cleanup jobs.

---

## 📂 Migration Files

* **[`001_sync_infrastructure.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/001_sync_infrastructure.sql)**: Sets up the base tracking tables, sync columns, updated-at triggers, and conflict-handling structures.
* **[`002_tombstone_purge_cron.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/002_tombstone_purge_cron.sql)**: Configures a background database cron schedule to purge soft-deleted record tombstones.
* **[`003_ifct_foods_table.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/003_ifct_foods_table.sql)**: Sets up the Indian Food Composition Tables (IFCT) reference database table with fast search indexes.
* **[`004_ifct_foods_seed.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/004_ifct_foods_seed.sql)**: Seeds standard food items into the IFCT reference table.
* **[`005_recalculate_on_food_update.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/005_recalculate_on_food_update.sql)**: Triggers auto-recalculations of logged daily macros whenever base food macros are edited.
* **[`006_user_preferences_table.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/006_user_preferences_table.sql)**: Creates the schema for managing user preferences and nutritional target goals.
* **[`007_food_items_insert_policy.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/007_food_items_insert_policy.sql)**: Defines Row-Level Security (RLS) insertion rules for custom food items.
* **[`008_auto_create_catalogs_on_signup.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/008_auto_create_catalogs_on_signup.sql)**: Sets up triggers to provision default catalog items automatically when a user signs up.
* **[`009_user_profile_columns.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/009_user_profile_columns.sql)**: Adds demographics, cuisines, and allergy columns to the user profile schema.
* **[`010_daily_logs_meal_type.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/010_daily_logs_meal_type.sql)**: Adjusts logging structures to track the type of meal logged (e.g. Breakfast, Lunch, Dinner, Snack).
* **[`011_recommendation_cache.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/011_recommendation_cache.sql)**: Implements database tables and eviction procedures to cache meal recommendations.
* **[`012_normalize_macros_per_100g.sql`](file:///C:/Users/Khushi%20Shah/Documents/GitHub/NutriAI/supabase/migrations/012_normalize_macros_per_100g.sql)**: Ensures all custom food items normalize and scale base macro values per 100g.

---

## 🔌 External Dependencies

All migrations are standard SQL statements executed inside the Supabase managed PostgreSQL environment.
* Re-uses the following standard PostgreSQL extensions:
  * `pgcrypto` for UUID generation.
  * `pg_cron` for background scheduling.
  * `pg_trgm` (trigram) for fast text similarity searching.
