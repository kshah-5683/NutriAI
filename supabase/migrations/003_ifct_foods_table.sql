-- Migration: 003_ifct_foods_table.sql
-- Purpose: Create IFCT 2017 reference table for nutrition lookup (Tier 2 fallback).
-- Run in Supabase Dashboard → SQL Editor before deploying Edge Functions.

-- Enable pg_trgm for fuzzy name matching (must be before index creation)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS public.ifct_foods (
    code        TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    energy_kcal REAL NOT NULL DEFAULT 0,
    protein_g   REAL NOT NULL DEFAULT 0,
    fat_g       REAL NOT NULL DEFAULT 0,
    carbs_g     REAL NOT NULL DEFAULT 0,
    fiber_g     REAL NOT NULL DEFAULT 0
);

-- No user-owned rows — this is a public reference table.
-- RLS is enabled but with a permissive read policy for all authenticated users.
ALTER TABLE public.ifct_foods ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can read IFCT data"
    ON public.ifct_foods
    FOR SELECT
    TO authenticated
    USING (true);

-- GIN trigram index for fast ILIKE name searches used in lookup-nutrition Edge Function.
-- Supports: .ilike("name", `%${query}%`) queries.
CREATE INDEX IF NOT EXISTS idx_ifct_foods_name_trgm
    ON public.ifct_foods USING gin (name gin_trgm_ops);
