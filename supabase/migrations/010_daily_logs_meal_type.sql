Th -- ============================================================
-- NutriAI — Add meal_type to daily_logs
-- Date: Phase M1 (Meal Categories + Smart Prefetching)
-- ============================================================
-- Adds a meal_type column to categorize each food log entry as
-- breakfast, snack, lunch, or dinner.
--
-- Nullable — existing rows remain NULL (backward compatible).
-- Valid values enforced at application layer:
--   'breakfast', 'snack', 'lunch', 'dinner'
-- ============================================================

ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS meal_type TEXT;
