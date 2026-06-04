-- ─────────────────────────────────────────────────────────────────────────────
--  Migration 011: recommendation_cache table (Phase P1 — Smart Prefetching)
--
--  Stores prefetched AI meal recommendations keyed by user + meal_type + day.
--  The home screen reads from this table for instant display; the
--  prefetch-recommendations Edge Function writes to it after each food log.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS recommendation_cache (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  meal_type TEXT NOT NULL,           -- 'breakfast' | 'snack' | 'lunch' | 'dinner'
  date_timestamp BIGINT NOT NULL,    -- start-of-day epoch ms (same convention as daily_logs)
  recommendations JSONB NOT NULL,    -- the full recommendations array from AI
  remaining_macros JSONB NOT NULL,   -- macro snapshot when generated (for staleness check)
  created_at TIMESTAMPTZ DEFAULT now(),

  -- One cache entry per user + meal_type + day
  UNIQUE(user_id, meal_type, date_timestamp)
);

-- ─── RLS ─────────────────────────────────────────────────────────────────────

ALTER TABLE recommendation_cache ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own cache"
  ON recommendation_cache FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own cache"
  ON recommendation_cache FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own cache"
  ON recommendation_cache FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

-- ─── Index for fast lookups ──────────────────────────────────────────────────

CREATE INDEX idx_rec_cache_user_date
  ON recommendation_cache(user_id, date_timestamp);

-- ─── Auto-refresh created_at on every write (INSERT or UPDATE via upsert) ───
-- Without this, DEFAULT now() only applies on INSERT — upserts would keep
-- stale created_at, breaking the 5-minute cooldown check.

CREATE OR REPLACE FUNCTION refresh_rec_cache_created_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.created_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_rec_cache_created_at
  BEFORE INSERT OR UPDATE ON recommendation_cache
  FOR EACH ROW EXECUTE FUNCTION refresh_rec_cache_created_at();

-- ─── Daily cleanup: purge cache entries older than 2 days ────────────────────
-- Matches the tombstone purge pattern (migration 002). Runs at 3am daily.

SELECT cron.schedule(
  'purge-old-recommendation-cache',
  '0 3 * * *',
  $$DELETE FROM recommendation_cache WHERE date_timestamp < (EXTRACT(EPOCH FROM now() - INTERVAL '2 days') * 1000)$$
);
