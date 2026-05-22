-- Migration 007: Add INSERT RLS policy for food_items table.
--
-- The existing RLS policies allow SELECT/UPDATE/DELETE for food items
-- belonging to catalogs owned by the authenticated user, but INSERT
-- was missing — the Android app syncs via a different mechanism.
-- The web app inserts directly via Edge Functions using the user's JWT.

-- Allow authenticated users to insert food items into catalogs they own.
CREATE POLICY "Users can insert food items into their catalogs"
  ON public.food_items
  FOR INSERT
  TO authenticated
  WITH CHECK (
    catalog_id IN (
      SELECT id FROM public.catalogs WHERE user_id = auth.uid()
    )
  );

-- Also ensure catalogs INSERT policy exists (needed for auto-creation).
-- Use DO block to avoid error if policy already exists.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE tablename = 'catalogs'
      AND policyname = 'Users can insert their own catalogs'
  ) THEN
    EXECUTE $policy$
      CREATE POLICY "Users can insert their own catalogs"
        ON public.catalogs
        FOR INSERT
        TO authenticated
        WITH CHECK (user_id = auth.uid())
    $policy$;
  END IF;
END
$$;
