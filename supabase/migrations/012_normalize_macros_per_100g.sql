-- Migration 012: Normalize all food_items base macros to per-100g
--
-- Previously, base_calories/protein/carbs/fat stored per-serving values
-- (e.g., 321 kcal for a 200g serving). After this migration, they store
-- per-100g values (e.g., 160.5 kcal per 100g for the same item).
-- base_serving_g is preserved unchanged for display denormalization.
--
-- Formula: normalized = raw × (100 / base_serving_g)
-- Items where base_serving_g = 100 are unchanged (normFactor = 1.0).

-- Step 1: Normalize food_items where base_serving_g != 100
UPDATE food_items
SET
  base_calories = base_calories * (100.0 / base_serving_g),
  base_protein  = base_protein  * (100.0 / base_serving_g),
  base_carbs    = base_carbs    * (100.0 / base_serving_g),
  base_fat      = base_fat      * (100.0 / base_serving_g)
WHERE base_serving_g != 100
  AND base_serving_g > 0
  AND deleted_at IS NULL;

-- Step 2: Recalculate all daily_logs from their linked (now-normalized) food_items.
-- total_macro = normalized_base_macro × consumed_qty
-- This is safe because consumed_qty is already a 100g-relative multiplier
-- for gram units (200g stored as 2.0) and a raw count for serving units
-- (which previously had servingG=100 default, so those items didn't change).
UPDATE daily_logs dl
SET
  total_calories = fi.base_calories * dl.consumed_qty,
  total_protein  = fi.base_protein  * dl.consumed_qty,
  total_carbs    = fi.base_carbs    * dl.consumed_qty,
  total_fat      = fi.base_fat      * dl.consumed_qty
FROM food_items fi
WHERE dl.food_item_id = fi.id
  AND fi.deleted_at IS NULL
  AND dl.deleted_at IS NULL;
