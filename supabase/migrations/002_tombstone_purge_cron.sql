-- ============================================================
-- NutriAI — Tombstone Purge Cron Job
-- Run in: Supabase Dashboard → SQL Editor
-- TTL: 15 days
-- ============================================================
-- Schedules a weekly pg_cron job that hard-deletes soft-deleted
-- rows (tombstones) older than 15 days from all synced tables.
--
-- deleted_at is stored as epoch milliseconds (Long), so the
-- 15-day threshold is: EXTRACT(EPOCH FROM now() - interval '15 days') * 1000
--
-- Deletion order: daily_logs → food_items → catalogs
-- (children before parents to avoid unnecessary FK nullification)
-- ============================================================


-- ─────────────────────────────────────────────────────────────
-- 1. Enable pg_cron extension
--    Supabase supports pg_cron but it must be explicitly enabled.
-- ─────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Grant usage to the postgres role (required by Supabase)
GRANT USAGE ON SCHEMA cron TO postgres;


-- ─────────────────────────────────────────────────────────────
-- 2. Schedule the purge job
--    Runs every Sunday at 03:00 UTC.
--    Unschedules any previous version of the job first to make
--    this migration idempotent (safe to re-run).
-- ─────────────────────────────────────────────────────────────

SELECT cron.unschedule('purge-old-tombstones') WHERE EXISTS (
    SELECT 1 FROM cron.job WHERE jobname = 'purge-old-tombstones'
);

SELECT cron.schedule(
    'purge-old-tombstones',  -- job name
    '0 3 * * 0',             -- every Sunday at 03:00 UTC
    $$
        -- Children first: avoids unnecessary FK nullification on rows being purged anyway
        DELETE FROM daily_logs
        WHERE deleted_at IS NOT NULL
          AND deleted_at < EXTRACT(EPOCH FROM now() - interval '15 days') * 1000;

        DELETE FROM food_items
        WHERE deleted_at IS NOT NULL
          AND deleted_at < EXTRACT(EPOCH FROM now() - interval '15 days') * 1000;

        DELETE FROM catalogs
        WHERE deleted_at IS NOT NULL
          AND deleted_at < EXTRACT(EPOCH FROM now() - interval '15 days') * 1000;
    $$
);


-- ─────────────────────────────────────────────────────────────
-- Verify: confirm the job was registered
--   SELECT jobid, jobname, schedule, command FROM cron.job;
-- ─────────────────────────────────────────────────────────────
