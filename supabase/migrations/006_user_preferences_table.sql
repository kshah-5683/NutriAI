-- Migration: 006_user_preferences_table.sql
-- Purpose: User macro goals table — enables cross-platform sync between web and Android.
--          Replaces Android's DataStore (UserPreferences.kt) with a Supabase-backed store
--          so goals set on the web are visible on Android and vice versa.
--
-- Defaults match MacroGoals.kt: calorie=2000, protein=150, carbs=250, fat=65

CREATE TABLE IF NOT EXISTS public.user_preferences (
    user_id      UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    calorie_goal REAL NOT NULL DEFAULT 2000,
    protein_goal REAL NOT NULL DEFAULT 150,
    carbs_goal   REAL NOT NULL DEFAULT 250,
    fat_goal     REAL NOT NULL DEFAULT 65,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auto-update timestamp whenever a row changes.
-- Depends on set_updated_at() function created in 001_sync_infrastructure.sql.
CREATE TRIGGER set_user_preferences_updated_at
    BEFORE UPDATE ON public.user_preferences
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- RLS: each user can only read and write their own preferences row.
ALTER TABLE public.user_preferences ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage their own preferences"
    ON public.user_preferences
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
