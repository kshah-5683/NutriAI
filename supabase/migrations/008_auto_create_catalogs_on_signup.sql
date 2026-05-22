-- Migration 008: Auto-create public.users row + catalogs on signup.
--
-- Mirrors Android's InitializeUserUseCase + upsertUser() at the DB level.
-- Fires after every auth.users INSERT so all clients (web, Android, future)
-- get a public.users profile and both default catalogs automatically.
--
-- Key details:
--   1. Inserts public.users FIRST (catalogs.user_id FK references it)
--   2. ::bigint casts (last_modified_at / created_at are BIGINT columns)
--   3. ON CONFLICT DO NOTHING (safe for re-created users, idempotent)
--   4. SECURITY DEFINER bypasses RLS (safe — only fires on auth trigger)

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  -- 1. Create public.users row (FK target for catalogs.user_id)
  INSERT INTO public.users (id, email, created_at)
  VALUES (
    NEW.id,
    COALESCE(NEW.email, ''),
    (extract(epoch from now()) * 1000)::bigint
  )
  ON CONFLICT (id) DO NOTHING;

  -- 2. Create default catalogs
  INSERT INTO public.catalogs (id, user_id, name, last_modified_at, is_synced)
  VALUES
    (NEW.id || '_local_user_ingredients', NEW.id, 'My Ingredients', (extract(epoch from now()) * 1000)::bigint, true),
    (NEW.id || '_local_user_recipes',     NEW.id, 'My Recipes',     (extract(epoch from now()) * 1000)::bigint, true)
  ON CONFLICT (id) DO NOTHING;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop first to make this migration re-runnable.
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
