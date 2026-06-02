-- 009: Add user profile columns to user_preferences table
-- Phase R1: AI Recommendations prerequisite
--
-- Extends the existing macro goals table with dietary profile fields
-- for personalized meal recommendations. All profile columns are nullable
-- so existing rows are unaffected.
--
-- These columns are synced cross-platform via the existing user_preferences
-- bidirectional sync infrastructure (Phase 14).

ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS age INTEGER;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS gender TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS weight_kg DOUBLE PRECISION;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS weight_goal TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS diet_type TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS cuisine_preferences TEXT[] DEFAULT '{}';
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS allergies TEXT[] DEFAULT '{}';
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS recommendations_enabled BOOLEAN NOT NULL DEFAULT false;
