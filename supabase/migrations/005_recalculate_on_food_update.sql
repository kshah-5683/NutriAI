-- Migration: 005_recalculate_on_food_update.sql
-- Purpose: Postgres trigger — when a food_item's base macros change, automatically
--          recalculate all daily_logs that reference that food item.
--
-- This ensures web edits via the update-food Edge Function keep daily logs in sync
-- without requiring a separate recalculation step.
--
-- CRITICAL FORMULA (from DEVLOG parity notes):
--   total = base_macro * consumed_qty
--   Base macros are PER-SERVING. consumed_qty is number of servings.
--   NO division by base_serving_g — that bug was fixed in Android Phase 8 Pre-work II.

CREATE OR REPLACE FUNCTION public.recalculate_logs_on_food_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Only recalculate if base macros actually changed (avoid unnecessary writes)
    IF OLD.base_calories IS DISTINCT FROM NEW.base_calories
       OR OLD.base_protein  IS DISTINCT FROM NEW.base_protein
       OR OLD.base_carbs    IS DISTINCT FROM NEW.base_carbs
       OR OLD.base_fat      IS DISTINCT FROM NEW.base_fat THEN

        -- CORRECT FORMULA: total = base_macro × consumed_qty
        -- base macros are already PER-SERVING; consumed_qty is number of servings.
        -- DO NOT divide by base_serving_g.
        UPDATE public.daily_logs
        SET
            total_calories   = NEW.base_calories * consumed_qty,
            total_protein    = NEW.base_protein  * consumed_qty,
            total_carbs      = NEW.base_carbs    * consumed_qty,
            total_fat        = NEW.base_fat      * consumed_qty,
            last_modified_at = EXTRACT(EPOCH FROM now()) * 1000
        WHERE food_item_id = NEW.id
          AND deleted_at IS NULL;

    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop existing trigger before recreating (idempotent)
DROP TRIGGER IF EXISTS trg_food_items_recalculate ON public.food_items;

CREATE TRIGGER trg_food_items_recalculate
    AFTER UPDATE ON public.food_items
    FOR EACH ROW EXECUTE FUNCTION public.recalculate_logs_on_food_update();
