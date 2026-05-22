-- ============================================================
-- NutriAI — Sync Infrastructure Migration
-- Run in: Supabase Dashboard → SQL Editor
-- Date: Phase 8 Pre-work II
-- ============================================================
-- This migration adds:
--   1. server-controlled updated_at timestamp columns (eliminates client clock skew)
--   2. auto-update triggers (set updated_at on every INSERT or UPDATE)
--   3. Last-Write-Wins (LWW) guard triggers (reject stale pushes)
--   4. Performance indexes for incremental pull queries
-- ============================================================


-- ─────────────────────────────────────────────────────────────
-- 1. Add server-controlled updated_at columns
--    DEFAULT now() backfills all existing rows immediately.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE catalogs    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();
ALTER TABLE food_items  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();
ALTER TABLE daily_logs  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();


-- ─────────────────────────────────────────────────────────────
-- 2. Auto-update function — sets updated_at on every write
-- ─────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Catalogs
DROP TRIGGER IF EXISTS trg_catalogs_updated_at ON catalogs;
CREATE TRIGGER trg_catalogs_updated_at
    BEFORE INSERT OR UPDATE ON catalogs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Food items
DROP TRIGGER IF EXISTS trg_food_items_updated_at ON food_items;
CREATE TRIGGER trg_food_items_updated_at
    BEFORE INSERT OR UPDATE ON food_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Daily logs
DROP TRIGGER IF EXISTS trg_daily_logs_updated_at ON daily_logs;
CREATE TRIGGER trg_daily_logs_updated_at
    BEFORE INSERT OR UPDATE ON daily_logs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ─────────────────────────────────────────────────────────────
-- 3. Last-Write-Wins (LWW) guard triggers
--    On UPDATE: if the incoming last_modified_at is OLDER than
--    the stored value, silently keep the existing row.
--    This prevents a stale push from overwriting newer data.
--
--    Note: RETURN OLD means PostgREST still returns 200 OK —
--    the app marks the row as synced, which is correct because
--    the server already holds the newer version.
-- ─────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION guard_lww()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.last_modified_at > NEW.last_modified_at THEN
        RETURN OLD;  -- silently keep the newer row; discard the stale push
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Catalogs
DROP TRIGGER IF EXISTS trg_catalogs_lww ON catalogs;
CREATE TRIGGER trg_catalogs_lww
    BEFORE UPDATE ON catalogs
    FOR EACH ROW EXECUTE FUNCTION guard_lww();

-- Food items
DROP TRIGGER IF EXISTS trg_food_items_lww ON food_items;
CREATE TRIGGER trg_food_items_lww
    BEFORE UPDATE ON food_items
    FOR EACH ROW EXECUTE FUNCTION guard_lww();

-- Daily logs
DROP TRIGGER IF EXISTS trg_daily_logs_lww ON daily_logs;
CREATE TRIGGER trg_daily_logs_lww
    BEFORE UPDATE ON daily_logs
    FOR EACH ROW EXECUTE FUNCTION guard_lww();


-- ─────────────────────────────────────────────────────────────
-- 4. Performance indexes for incremental pull queries
--    These allow Postgres to satisfy WHERE updated_at > $cursor
--    with an index scan instead of a full table scan.
-- ─────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_catalogs_user_updated
    ON catalogs (user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_food_items_catalog_updated
    ON food_items (catalog_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_daily_logs_user_updated
    ON daily_logs (user_id, updated_at);


-- ─────────────────────────────────────────────────────────────
-- Done. Verify by checking:
--   SELECT column_name FROM information_schema.columns
--   WHERE table_name = 'catalogs' AND column_name = 'updated_at';
-- ─────────────────────────────────────────────────────────────
