# NutriAI Web App — Development Log & Change Tracker

> **Project:** NutriAI Web Platform (companion to the Android app)
> **Tech Stack:** Next.js 14 (App Router) · TypeScript · Tailwind CSS v4 · Supabase (Auth + PostgreSQL + Edge Functions) · TanStack Query v5 · Zustand · Recharts · Gemma 4 (via Gemini API)
> **Architecture:** Online-first, server-centric business logic (Supabase Edge Functions), shared PostgreSQL backend with Android app
> **Architecture Plan:** [`docs/WEB_ARCHITECTURE_PLAN.md`](docs/WEB_ARCHITECTURE_PLAN.md)
> **Android Dev Log:** [`APP_DEVLOG.md`](APP_DEVLOG.md)
> **Started:** May 14, 2026

---

## Table of Contents

- [Phase W1: Foundation](#phase-w1-foundation)
- [Phase W2: Auth + Home Dashboard](#phase-w2-auth--home-dashboard)
- [Phase W3: AI Pipeline — Edge Functions](#phase-w3-ai-pipeline--edge-functions)
- [Phase W3.5: Auth, RLS & Calculation Bugfixes](#phase-w35-auth-rls--calculation-bugfixes)
- [Phase W4: Features + Polish](#phase-w4-features--polish)
- [Phase W5: Cross-Platform Testing](#phase-w5-cross-platform-testing)
- [Phase W6: Production Deployment](#phase-w6-production-deployment)
- [Phase W7: Sign-Up Flow — Confirmation Before Sign-In](#phase-w7-sign-up-flow--confirmation-before-sign-in)
- [Phase W7.5: Responsive Insight Charts](#phase-w75-responsive-insight-charts)
- [Phase W8: Discrete Unit Macro Fix + Preview Card Correction](#phase-w8-discrete-unit-macro-fix--preview-card-correction)
- [Phase W9: Manual Recipe Builder + Catalog Navigation Fix + Catalog Miss Bug](#phase-w9-manual-recipe-builder--catalog-navigation-fix--catalog-miss-bug)
- [Phase W10: Serving Size Clarification — Brand-Aware Nutrition Lookup](#phase-w10-serving-size-clarification--brand-aware-nutrition-lookup)
- [Phase W10.1: Clarification Robustness — Prompt Expansion, Brand Validation & Dark Mode Fix](#phase-w101-clarification-robustness--prompt-expansion-brand-validation--dark-mode-fix)
- [Phase W11: Edit Log Macro Scaling Guard — Gram ↔ Non-Gram Boundary](#phase-w11-edit-log-macro-scaling-guard--gram--non-gram-boundary)
- [Phase W12: Catalog Re-Log Macro Fix — Per-100g De-Normalization](#phase-w12-catalog-re-log-macro-fix--per-100g-de-normalization)
- [Phase W13: AI Parse Fixes — Recipe Nutrition, Serving Size Pre-Scaling & Button Loading State](#phase-w13-ai-parse-fixes--recipe-nutrition-serving-size-pre-scaling--button-loading-state)
- [Phase W14: Recommendation Cache Type Safety](#phase-w14-recommendation-cache-type-safety)
- [Phase W15: AI Parse + Recommendation Quality Improvements](#phase-w15-ai-parse--recommendation-quality-improvements)
- [Phase W16: Recommendation Macro Accuracy — Server-Side Recalculation](#phase-w16-recommendation-macro-accuracy--server-side-recalculation)
- [Phase W17: Ingredient Inline Edit — AI Parsed Mode](#phase-w17-ingredient-inline-edit--ai-parsed-mode)
- [Phase W18: Recipe Card Nutrition Display + Edit Selected Recipe Fix](#phase-w18-recipe-card-nutrition-display--edit-selected-recipe-fix)
- [Architecture Decisions](#architecture-decisions)
- [Known Issues & Tech Debt](#known-issues--tech-debt)

---

## Pre-Implementation: Architecture Planning

**Status:** Completed
**Date:** May 14, 2026

### Summary
Created a comprehensive Technical Architecture & Implementation Plan covering all 7 areas
required for the web platform: tech stack selection, logic centralization, data consolidation,
AI pipeline mirroring, multimodal label scanning, UI/UX design system translation, and
cross-platform sync/conflict resolution.

### Deliverables
- [`docs/WEB_ARCHITECTURE_PLAN.md`](docs/WEB_ARCHITECTURE_PLAN.md) — Full plan with code samples
- 3 review passes completed — 12 gaps identified and fixed (4 critical, 6 medium, 2 low)

### Key Architecture Decisions (Pre-Implementation)

| # | Decision | Rationale |
|---|----------|-----------|
| W1 | Next.js 14 App Router over Remix | Native `@supabase/ssr` support, RSC for zero-JS dashboard data fetching, Vercel deployment |
| W2 | Supabase Edge Functions for all business logic | Single execution point for both platforms; prevents logic drift between Android and web |
| W3 | No `local_user` mapping on web | Web uses Supabase UUID directly — the `"local_user"` → UUID translation is Android-only (offline mode) |
| W4 | Online-first (no IndexedDB, no local-first) | Web has no offline requirement; Supabase is the single source of truth |
| W5 | TanStack Query for client-side caching | Optimistic updates, background refetch, stale-while-revalidate — replaces Room's Flow-based reactivity |
| W6 | Zustand for UI state (not Redux/Jotai) | Lightweight, no boilerplate, sufficient for date navigation + form state |
| W7 | Tailwind CSS v4 with CSS variables for theming | Maps directly from Android's Material3 color scheme; `class` strategy for dark mode |
| W8 | Recharts for data visualization | Mirrors Android's `MacroLineChart`, `MacroBarChart`, `MacroYearChart` components |
| W9 | Vercel free tier for hosting | Frontend-only host (all logic in Supabase Edge Functions); free Hobby plan sufficient for 5 users |
| W10 | `user_preferences` Supabase table for macro goals | Replaces Android's DataStore; enables cross-platform goal sync |

### Supabase Migrations Required (Before Phase W1)

| Migration | File | Purpose |
|-----------|------|---------|
| 001 | `supabase/migrations/001_sync_infrastructure.sql` | `updated_at` triggers, LWW guard, indexes (already deployed for Android) |
| 002 | `supabase/migrations/002_tombstone_purge_cron.sql` | Weekly pg_cron tombstone cleanup (already deployed) |
| 003 | `supabase/migrations/003_ifct_foods_table.sql` | IFCT 2017 reference table + pg_trgm index |
| 004 | `supabase/migrations/004_ifct_foods_seed.sql` | 120 Indian food entries from CSV |
| 005 | `supabase/migrations/005_recalculate_on_food_update.sql` | Auto-recalculate daily logs when food macros change |
| 006 | `supabase/migrations/006_user_preferences_table.sql` | User macro goals (cross-platform) |

### Edge Functions Inventory

| Function | Port Of (Android) | Purpose |
|----------|-------------------|---------|
| `parse-food` | `AiRepositoryImpl.parseFood()` + `GeminiPrompts` | AI entity extraction with catalog context |
| `lookup-nutrition` | `NutritionRepositoryImpl` | USDA FDC + IFCT two-tier nutrition grounding |
| `log-food` | `LogFoodUseCase.invoke()` | Canonical macro scaling + catalog auto-creation |
| `log-recipe` | `LogFoodUseCase.logRecipe()` | 3-case ingredient aggregation |
| `scan-label` | `AiRepositoryImpl.extractLabel()` + `LabelScannerDelegate` | Multimodal label reading + per-100g conversion |
| `update-food` | `UpdateFoodUseCase` | Catalog item editing (triggers Postgres recalc) |
| `update-daily-log` | `UpdateDailyLogUseCase` | Log entry editing (qty, unit, macros) |

### External Services

| Service | Purpose | Cost |
|---------|---------|------|
| Supabase | DB, Auth, Edge Functions, RLS | Free tier (5 users) |
| Google Gemini API (Gemma 4) | AI food parsing + label scanning | Free tier |
| USDA FoodData Central | Nutrition lookup (Tier 1) | Free (government API) |
| Vercel | Next.js frontend hosting | Free Hobby plan |

---

## Phase W1: Foundation

**Status:** ✅ Complete
**Date:** May 14, 2026

### Completed Work
1. ✅ Initialized Next.js project with TypeScript + Tailwind CSS v4
2. ✅ Configured Forest & Cream theme via CSS variables in `globals.css`
3. ✅ Set up `@supabase/ssr` (browser + server clients)
4. ✅ Configured `proxy.ts` for auth session refresh (Next.js 16 renamed `middleware.ts` → `proxy.ts`)
5. ✅ Generated Supabase types
6. ✅ Ran migrations in Supabase Dashboard
7. ✅ Created shared utilities: `format.ts`, `constants.ts`

---

## Phase W2: Auth + Home Dashboard

**Status:** ✅ Complete
**Date:** May 14, 2026

### Completed Work

8. ✅ **Auth pages** — Sign In / Sign Up / Email Confirm using `@supabase/ssr`
   - `proxy.ts` — Session refresh + route guards (Next.js 16 proxy convention)
   - `app/auth/actions.ts` — Server Actions: `signIn`, `signUp`, `signOut`
   - `app/auth/layout.tsx` — Centered auth layout with NutriAI branding
   - `app/auth/sign-in/page.tsx` — Email/password sign-in with `useActionState`
   - `app/auth/sign-up/page.tsx` — Registration with "check email" success state
   - `app/auth/confirm/route.ts` — PKCE code exchange route handler

9. ✅ **Home page** — Client-side daily logs via TanStack Query (not RSC — hydration complexity)
   - `app/page.tsx` — Dashboard assembling all components inside `DashboardShell`
   - `components/dashboard-shell.tsx` — Header bar (logo + Sign Out) + BottomNav wrapper
   - `components/bottom-nav.tsx` — 3-tab navigation: Home, Insights, Catalog

10. ✅ **MacroSummaryCard** — SVG circular arcs with `stroke-dashoffset` CSS transitions
    - Large calorie ring (100px) + 3 smaller macro rings (72px)
    - Uses `MACRO_COLORS` constants matching Android Color.kt

11. ✅ **FoodLogList** — List with edit/delete actions + loading skeleton + empty state
    - `components/food-log-list.tsx` — 3-state rendering (loading/empty/list)
    - `components/food-log-item.tsx` — Row with food name, qty, MacroChip badges

12. ✅ **EditLogSheet** — Dialog modal for editing log entries (qty, unit, macros)
    - Pre-fills from selected `DailyLog`, validates qty > 0 and macros >= 0
    - Uses `useUpdateLog` mutation (direct Supabase update, not Edge Function)

13. ✅ **ConfirmDeleteDialog** — Soft-delete with `deleted_at` + `last_modified_at` + `is_synced`
    - Uses `useDeleteLog` mutation with `.is("deleted_at", null)` guard
    - LWW-compatible: sets both timestamps so sync resolves correctly

14. ✅ **DateNavigationHeader** — Zustand store for date state
    - `lib/stores/date-store.ts` — `selectedDate`, prev/next/today actions
    - `components/date-navigation-header.tsx` — Arrows + formatted date + "Today" badge

### Supporting Infrastructure Created
- `components/providers/query-provider.tsx` — TanStack Query with `staleTime: 60s`
- `components/providers/supabase-provider.tsx` — React Context for browser Supabase client
- `components/ui/button.tsx` — Variants: primary, secondary, ghost, destructive + loading state
- `components/ui/card.tsx` — Surface card with optional `noPadding`
- `components/ui/input.tsx` — ForwardRef input with label + error display
- `components/ui/dialog.tsx` — Native `<dialog>` with backdrop blur + ESC/click-outside
- `lib/hooks/use-daily-logs.ts` — TanStack Query hook, RLS-filtered, tombstone-excluded
- `lib/hooks/use-macro-goals.ts` — TanStack Query hook with `DEFAULT_MACRO_GOALS` fallback
- `lib/hooks/use-delete-log.ts` — Soft-delete mutation (deleted_at + last_modified_at + is_synced)
- `lib/hooks/use-update-log.ts` — Direct Supabase update mutation
- `lib/utils/compute-daily-totals.ts` — Aggregates `DailyLog[]` → `DailyMacroSummary`
- `lib/types/domain.ts` — All domain model types ported from Android Kotlin
- `app/insights/page.tsx` — Placeholder page for Insights tab
- `app/catalog/page.tsx` — Placeholder page for Catalog tab

### Architecture Decisions (Phase W2)
| # | Decision | Rationale |
|---|----------|-----------|
| W11 | Client-side data fetching (not RSC) for dashboard | Avoids complex RSC→client hydration for interactive date navigation; TanStack Query handles caching |
| W12 | Direct Supabase `.update()` for edit log | Avoids Phase W3 Edge Function dependency; sets `is_synced: true` + `last_modified_at` for LWW |
| W13 | Flat route structure (no route groups) | Prevents Next.js 16 route conflicts; `DashboardShell` component provides shared layout |
| W14 | `proxy.ts` instead of `middleware.ts` | Next.js 16 breaking change: renamed with `proxy` named export |

---

## Phase W3: AI Pipeline — Edge Functions

**Status:** ✅ Complete
**Date:** May 14, 2026

### Completed Work

#### Edge Functions — Shared Utilities
- `supabase/functions/_shared/cors.ts` — CORS headers + `handleCors()` OPTIONS handler
- `supabase/functions/_shared/json-utils.ts` — `extractJson()` with 3 fallback strategies (direct parse, markdown fence strip, brace extraction)
- `supabase/functions/_shared/prompts.ts` — Verbatim port of `SYSTEM_INSTRUCTION`, `buildUserPrompt()`, `LABEL_SYSTEM_INSTRUCTION`, `LABEL_USER_PROMPT` from Android
- `supabase/functions/_shared/macro-calculator.ts` — `computeServingMultiplier()`, `scaleNutrition()`, `computeDailyLogTotals()` with correct formula
- `supabase/functions/_shared/fdc-mapper.ts` — `mapFdcToNutritionInfo()` + `macroScore()` for USDA nutrient ID mapping (1008=kcal, 1003=protein, 1005=carbs, 1004=fat, 1079=fiber)

#### Edge Functions — 7 Functions
15. ✅ **`parse-food`** — AI entity extraction via Gemma 4 (`gemma-4-26b-a4b-it`) with `thinkingLevel: "MINIMAL"`, `responseMimeType: "application/json"`. Includes catalog context resolution and thought-part filtering (`findLast(p => p.thought !== true && p.text?.trim())`)
16. ✅ **`lookup-nutrition`** — Two-tier nutrition grounding: USDA FDC API (primary) → IFCT 2017 Supabase table (fallback). Uses `fdc-mapper.ts` for nutrient ID mapping
17. ✅ **`log-food`** — Canonical macro scaling + catalog auto-creation. Correct formula: `total = base_macro * consumed_qty`. Sets `is_synced: true` on all inserts
18. ✅ **`log-recipe`** — 3-case ingredient aggregation (catalog match / nutrition-enriched / zero-macro placeholder). Creates recipe food item + daily log in one transaction
19. ✅ **`update-food`** — Catalog item editing. Postgres trigger (`005_recalculate_on_food_update`) handles daily log recalculation
20. ✅ **`update-daily-log`** — Log entry editing (qty, unit, macros). Recalculates totals using corrected formula
21. ✅ **`scan-label`** — Multimodal label reading via Gemma 4 Vision + per-100g conversion (port of `LabelScannerDelegate.kt`)

#### Web App — State Management & Hooks
- `webapp/lib/stores/log-form-store.ts` — Zustand store: inputMode, AI parse state, nutritionResults/nutritionLoading maps, manual form fields, `acceptParsedFood` action
- `webapp/lib/hooks/use-parse-food.ts` — TanStack mutation calling parse-food EF, updates Zustand store on success/error
- `webapp/lib/hooks/use-nutrition-lookup.ts` — Progressive per-item lookups with `lookupAll()` helper (parallel mutations, not batched)
- `webapp/lib/hooks/use-log-food.ts` — TanStack mutation calling log-food EF, invalidates `daily-logs` queries, navigates home, resets form
- `webapp/lib/hooks/use-log-recipe.ts` — TanStack mutation calling log-recipe EF with `LogRecipeRequest` type (re-exported from `ai.ts`)
- `webapp/lib/hooks/use-update-log.ts` — **Migrated** from direct Supabase `.update()` to `supabase.functions.invoke(EDGE_FUNCTIONS.UPDATE_DAILY_LOG)`. Resolves Tech Debt #3

#### Web App — Log Page UI
- `webapp/components/input-mode-tabs.tsx` — 3-tab switcher (AI Parse / Manual / Scan) with Zustand-driven state
- `webapp/components/parsed-food-card.tsx` — Selectable card with catalog badge ("In catalog ✅" / "New"), nutrition status (loading/found/not found), recipe ingredient list
- `webapp/components/macro-preview-card.tsx` — Live-computed macro totals with color-coded `MacroBadge` components using `MACRO_COLORS`
- `webapp/components/ai-input-section.tsx` — Textarea input (500 char limit), "✨ Parse with AI" button, parsed results list, "Edit Selected" / "Log All" / "Clear & Try Again" actions
- `webapp/components/manual-input-section.tsx` — Full form: food name, brand, serving (g), qty, unit, macros (kcal/P/C/F), live MacroPreviewCard, Save button
- `webapp/app/log/page.tsx` — Standalone full-screen page (no DashboardShell/BottomNav), inline header with back arrow, `handleLogAll` batch-logging with recipe/ingredient branching

#### Web App — Utilities & Types
- `webapp/lib/utils/macro-calculator.ts` — Client-side `computeServingMultiplier()` + `scaleNutrition()` for preview only (mirrors server-side source of truth)
- `webapp/lib/types/ai.ts` — Extended `LogFoodRequest` with `existingFoodItemId`, `catalogId`, `skipDailyLog`, `brand` fields; added `LogRecipeRequest` type

### Architecture Decisions (Phase W3)
| # | Decision | Rationale |
|---|----------|-----------|
| W15 | Gemma 4 with `thinkingLevel: "MINIMAL"` | Faster inference for food parsing; thought parts filtered before JSON extraction |
| W16 | `extractJson()` with 3-strategy fallback | Handles Gemma 4 responses that may be raw JSON, markdown-fenced, or embedded in text |
| W17 | Progressive nutrition lookup (parallel per-item) | Each food resolves independently — matches Android UX where items appear one by one |
| W18 | Standalone Log page (no DashboardShell) | Full-screen form experience without bottom nav; back arrow navigates to home |
| W19 | `is_synced: true` on all Edge Function inserts | Web is always online; prevents Android sync from overwriting server-created rows |
| W20 | Client-side macro-calculator mirrors server | Enables live preview without round-trip; server remains source of truth for persisted data |

---

## Phase W3.5: Auth, RLS & Calculation Bugfixes

**Status:** ✅ Completed
**Date:** May 15, 2026

### Summary

Post-W3 bugfixes surfaced during end-to-end testing, addressing four categories:
1. **Corporate proxy blocks Supabase from Server Actions** — converted auth flows to client-side calls
2. **Supabase RLS INSERT violation on `food_items`** — missing migration 007
3. **Android signup crash from migration 008** — trigger FK ordering bug fixed
4. **Serving size unit parity** — `fdc-mapper.ts` / `lookup-nutrition` now return `servingWeightG` matching Android

> **Note:** The gram quantity double-conversion bug (`fromDisplayQty` applied twice → 0 macros / 20000g display) and the catalog-match snake_case→camelCase mapping bug were discovered and fixed in **Phase W4** (bugfixes B2 and B3), not here.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/app/auth/sign-in/page.tsx` | Rewritten | Removed `useActionState` + Server Action. Now uses `useState` + `createBrowserClient().signInWithPassword()` + `useRouter().push("/")`. Auth request goes browser→Supabase directly, bypassing corporate proxy. ⚠️ **Superseded in W4**: browser cookies invisible to `proxy.ts` → redirect loop → reverted to `useActionState(signIn, undefined)` server action. |
| 2 | `webapp/app/auth/sign-up/page.tsx` | Rewritten | Same pattern as sign-in: `createBrowserClient().signUp()` client-side. |
| 3 | `webapp/components/dashboard-shell.tsx` | Updated | Replaced `<form action={signOut}>` Server Action with `handleSignOut()` function using `createClient().auth.signOut()` + `router.push("/auth/sign-in")`. |
| 4 | `supabase/migrations/007_food_items_insert_policy.sql` | Created | INSERT RLS policies for `food_items` and `catalogs`. Apply in Supabase SQL Editor. |
| 5 | `supabase/migrations/008_auto_create_catalogs_on_signup.sql` | Updated | Fixed `handle_new_user()` trigger: inserts `public.users` row before `catalogs` (FK fix); `::bigint` casts on `last_modified_at`; `ON CONFLICT DO NOTHING` for idempotency. |
| 6 | `supabase/functions/log-food/index.ts` | Updated | Catalog `INSERT` now throws on error instead of silently continuing — surfaces root cause to caller. |
| 7 | `supabase/functions/log-recipe/index.ts` | Updated | `ensureCatalog()` now throws on error — surfaces root cause to caller. |
| 8 | `supabase/functions/_shared/fdc-mapper.ts` | Updated | `NutritionInfo` interface: added `servingWeightG: number \| null`. `mapFdcToNutritionInfo()` extracts `food.servingSize` when `food.servingSizeUnit === "g"`. |
| 9 | `supabase/functions/lookup-nutrition/index.ts` | Updated | `mapIfctToNutritionInfo()` returns `servingWeightG: null` to satisfy updated interface (IFCT has no serving size data). |

### Root Cause Notes

**Sign-in "fetch failed"**: Next.js Server Actions run on the Next.js server process. On Walmart's corporate network, the server process sits behind `proxy-intlho.wal-mart.com:8080` which blocks non-whitelisted outbound domains, including Supabase. Browser requests bypass the proxy. Converting to `createBrowserClient()` routes auth requests browser→Supabase directly.

**Signup HTTP 500**: `catalogs.user_id` has a FK constraint → `public.users(id)`, NOT `auth.users`. At signup time `auth.users` is created but `public.users` does not yet exist. Trigger was inserting `catalogs` first → FK violation → full transaction rollback → 500. Fix: insert `public.users` first inside the trigger.

**`food_items` RLS**: `log-food` and `log-recipe` Edge Functions insert into `food_items` after first inserting a catalog row. The FK is satisfied but the RLS INSERT policy on `food_items` (`catalog_id IN (SELECT id FROM catalogs WHERE user_id = auth.uid())`) was missing. Migration 007 adds it.

**`servingWeightG` parity**: Android's `NutritionRepositoryImpl` extracts `servingSize` from FDC responses to enable accurate per-serving display. The web `fdc-mapper.ts` was discarding this field entirely. Added `servingWeightG` to the `NutritionInfo` shape and extracted it from `food.servingSize` when `food.servingSizeUnit === "g"`. IFCT rows have no serving size data so `servingWeightG: null` is returned for those.

### Edge Function Deployment Required

| Function | Needs Redeploy | Reason |
|----------|---------------|--------|
| `log-food` | ✅ Yes | Error handling added to catalog INSERT |
| `log-recipe` | ✅ Yes | Error handling added to `ensureCatalog()` |
| `lookup-nutrition` | ✅ Yes | `servingWeightG` added to IFCT mapper response |

Deploy commands:
```bash
supabase functions deploy log-food
supabase functions deploy log-recipe
supabase functions deploy lookup-nutrition
```

The `_shared/fdc-mapper.ts` changes are picked up automatically on next deploy of any function that imports it — `lookup-nutrition` is the only one.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W21 | Client-side auth over Server Actions | Corporate proxy blocks server→Supabase calls. `createBrowserClient()` routes auth browser-direct. Pattern consistent with `@supabase/ssr` browser client docs. |
| W22 | Throw on catalog creation failure in Edge Functions | Silent failure masked root cause for hours. Throwing lets the calling client (webapp, Android sync) surface a meaningful error immediately. |

---

## Phase W3.5b: Android Display Fixes — Cross-Platform Impact Notes

**Status:** ✅ Completed (Android-side changes; no webapp code changes required)
**Date:** May 15, 2026

### Summary

A set of Android display and calculation bugs were identified and fixed in **Phase 13** of the Android app. These bugs do not affect the webapp directly (the webapp reads `consumedQty` from the shared Supabase DB) but the fixes are documented here because:
- Newly logged entries from the Android app (post-fix) are stored correctly and display correctly on the webapp.
- Pre-fix entries already in the DB with wrong `consumedQty` values (e.g. `200` instead of `2.0` for 200g) will display incorrectly on the webapp's `FoodLogItem` until those logs are re-entered or manually corrected.
- The webapp's `EditLogSheet` does **not** apply `toDisplayQty()` / `fromDisplayQty()` conversion — it was built against direct Supabase values. If a legacy wrong-stored entry (e.g. `consumedQty=200`, `consumed_unit="g"`) is edited on the webapp, it will save `qty=200` as-is into `consumedQty`. This is tracked as **Tech Debt #9**.

### Android Phase 13 Fixes (for reference)

| Fix | Android files changed | DB impact |
|-----|-----------------------|-----------|
| `toDisplayQty` / `fromDisplayQty` in `UnitConverter` — gram display at every render/edit-pre-fill site | `UnitConverter.kt`, `FoodLogItem.kt`, `HomeViewModel.kt` | None — display-only |
| `isFromCatalog` + `isGramsUnit` split in `acceptAndLogAllParsed()` — catalog-hit grams now stores multiplier (2.0) not raw qty (200) | `LogViewModel.kt` | New logs stored correctly; old wrong-stored logs unaffected |
| Non-gram FDC Log-All path stores per-unit base + raw qty — "0.3 tbsp" → "2 tbsp" | `LogViewModel.kt` | New logs stored correctly |

### Tech Debt Added

| # | Issue | Severity | Notes |
|---|-------|----------|-------|
| 9 | Webapp `EditLogSheet` lacks `toDisplayQty`/`fromDisplayQty` round-trip | Medium | Pre-fix Android entries with `consumedQty=200` (raw grams) will appear as "200 g" in the webapp edit form and save back as `consumedQty=200`. Add conversion in Phase W4 when polishing `EditLogSheet`. |

---

## Phase W4: Features + Polish

**Status:** ✅ Complete
**Date:** May 16, 2026

### Completed Work

22. ✅ **Catalog page** — Dual-tab (Recipes / Ingredients) with search, EditFoodSheet, and soft-delete
    - `app/catalog/page.tsx` — Live dual-tab with Zustand-backed filter + search
    - `lib/stores/catalog-store.ts` — `activeTab`, `searchQuery` state
    - `lib/hooks/use-catalog-items.ts` — TanStack Query: fetches `food_items` filtered by catalogId + `deleted_at IS NULL`
    - `lib/hooks/use-delete-food.ts` — Soft-delete mutation (sets `deleted_at`, `last_modified_at`, `is_synced`)
    - `lib/hooks/use-update-food.ts` — TanStack mutation calling `update-food` EF
    - `components/catalog-item-card.tsx` — Row with food name, brand, macros per 100g, edit/delete actions
    - `components/edit-food-sheet.tsx` — Modal for editing catalog item macros

23. ✅ **Insights page** — Week/Month/Year macro trend charts with dashed goal reference lines on all tabs
    - `app/insights/page.tsx` — `PeriodSelector` + chart card + `DailyAverageCard`
    - `lib/hooks/use-insights-data.ts` — TanStack Query: date-range query on `daily_logs`, zero-fills missing days, computes averages excluding zero-data days (Android parity)
    - `components/insights/period-selector.tsx` — 3-segment toggle (Week / Month / Year)
    - `components/insights/macro-bar-chart.tsx` — Weekly grouped bars (Recharts `BarChart`); dashed `ReferenceLine` per goal; dynamic Y-axis domain = max(data, goals, 50) × 1.1; legend shows goal values
    - `components/insights/macro-line-chart.tsx` — Monthly smooth lines (Recharts `LineChart`); dashed `ReferenceLine` per goal; same Y-axis logic
    - `components/insights/macro-year-chart.tsx` — Yearly grouped bars (Recharts `BarChart`); dashed `ReferenceLine` per goal; same Y-axis logic
    - `components/insights/daily-average-card.tsx` — Period averages vs goals for each macro
    - `lib/types/insights.ts` — `InsightsPeriod`, `DailyChartSummary`, `InsightsData`, `PERIOD_CONFIG`

24. ✅ **`scan-label` Edge Function deployed** — Gemma 4 Vision (`gemma-4-26b-a4b-it`) + per-100g conversion, port of `LabelScannerDelegate.kt`

25. ✅ **Scan tab** — Client-side image compression + label extraction flow
    - `components/scan-input-section.tsx` — File picker / camera capture, image preview, "Scan Label" trigger, result display, auto-populate to Manual tab
    - `lib/hooks/use-scan-label.ts` — TanStack mutation: compresses to JPEG (quality 0.85, max 1024px), base64-encodes, calls `scan-label` EF, calls `acceptScanResult()` on success
    - `useLogFormStore.acceptScanResult()` — Pre-fills manual form from `ExtractedLabelData`, switches to Manual tab

26. ✅ **Settings page** — Macro goals read/write + dark mode selector
    - `app/settings/page.tsx` — 4 macro goal inputs (Calories / Protein / Carbs / Fat with color dots) + `ThemeSelector`
    - `lib/hooks/use-update-goals.ts` — TanStack mutation upserting to `user_preferences` (Postgres trigger handles `updated_at`)
    - `lib/hooks/use-macro-goals.ts` — Hardened: catches errors, returns `DEFAULT_MACRO_GOALS`, logs warning instead of throwing
    - **SQL required**: `GRANT SELECT, INSERT, UPDATE ON public.user_preferences TO authenticated;`

27. ✅ **EditLogSheet unit converter fix** (Tech Debt #9 resolved)
    - `components/edit-log-sheet.tsx` — `toDisplayQty(log.consumedQty, log.consumedUnit)` on pre-fill; `fromDisplayQty(qty, unit)` on save — gram entries show "200 g" in the edit modal, not the internal multiplier "2"

28. ✅ **Dark mode toggle** — `class` strategy with Tailwind CSS v4
    - `lib/stores/theme-store.ts` — Zustand store: `light | dark | system`, persists to `localStorage`, `applyTheme()` toggles `.dark` class on `<html>`
    - `components/theme-init-script.tsx` — Blocking inline `<script>` prevents FOUC; listens for system preference changes
    - `components/theme-selector.tsx` — 3-segment toggle (Light / Dark / System) on Settings page
    - `components/theme-toggle-button.tsx` — Quick-cycle button (☀️→🌙→💻) in `DashboardShell` header
    - `app/layout.tsx` — Added `ThemeInitScript`, `suppressHydrationWarning` on `<html>`

29. ✅ **Bottom Nav extended to 4 tabs**
    - `components/bottom-nav.tsx` — Added "Settings" tab with gear SVG icon linking to `/settings`

### Post-Testing Bugfixes (W4)

| # | Bug | Root Cause | Fix |
|---|-----|-----------|-----|
| B1 | Sign-in redirect loop after W3.5 fix | `createBrowserClient()` sets cookies on the browser store; `proxy.ts` uses a server store client → `getUser()` returns null → redirect loop | Reverted to `useActionState(signIn, undefined)` server action — server action and `proxy.ts` share the same server cookie context |
| B2 | 20000g rice display + 0 macros (double unit conversion) | Frontend applied `fromDisplayQty(200, "g")` = 2.0; EF then called `computeServingMultiplier(2.0, "g")` = 0.02 → macros ≈ 0; `consumed_qty` stored as 200 raw → `toDisplayQty(200, "g")` = 20000 | Removed `fromDisplayQty` from frontend (send raw 200); `log-food` EF now computes `storedQty = quantity / 100` for gram units separately before writing `consumed_qty` |
| B3 | 0 kcal / 0g macros for catalog-matched items | `parse-food` EF returned catalog matches with Postgres snake_case column names (`base_calories`, etc.); frontend `FoodItem` type uses camelCase (`baseCalories`) → all reads returned `undefined` → `?? 0` | Added `mapFoodItem()` helper in `parse-food` EF that maps snake_case DB columns to camelCase for both food and recipe-ingredient catalog matches |
| B4 | Settings save button silently failing | `@ts-expect-error` in `use-update-goals.ts` was unused (`user_preferences` IS fully typed in `database.ts`) → TypeScript strict mode raised TS2578 | Removed the directive |
| B5 | "Could not load your goals" error banner on Settings page | `useMacroGoals` query threw on error instead of catching gracefully | Changed to return `DEFAULT_MACRO_GOALS` with `console.warn` on error |
| B6 | `user_preferences` permission denied (42501) | Table existed but lacked grants for the `authenticated` role | `GRANT SELECT, INSERT, UPDATE ON public.user_preferences TO authenticated;` in Supabase SQL Editor |
| B7 | Insights goal lines missing from Weekly and Yearly tabs | `MacroBarChart` and `MacroYearChart` had no `macroGoals` prop and no `ReferenceLine` — only `MacroLineChart` had them | Added `macroGoals: MacroGoals` prop + 3 dashed `ReferenceLine` components to both bar charts; `insights/page.tsx` now passes `macroGoals={data.macroGoals}` to all three chart variants |

### Root Cause Notes (Calculation Bugs)

**B2 — Double gram conversion (20000g / 0 macros):**
Two separate layers both divided by 100 for gram units:
1. Frontend: `fromDisplayQty(200, "g")` → sent `quantity: 2.0` to `log-food` EF
2. EF: `computeServingMultiplier(2.0, "g")` → `scaleFactor = 2.0 / 100 = 0.02`
3. Result: `total_calories = cal_per_100g × 0.02` ≈ 0; `consumed_qty` stored as raw 200 → `toDisplayQty(200, "g")` = 20000

Fix: frontend sends raw display quantity (200). EF uses `computeServingMultiplier(200, "g")` = 2.0 for macro scaling, and separately computes `storedQty = 200 / 100 = 2.0` for `consumed_qty`. Both are now correct.

**B3 — Catalog item macros always 0 (snake_case / camelCase mismatch):**
`parse-food` EF did `select("*")` on `food_items` — Supabase returns raw Postgres column names verbatim (`base_calories`, `base_protein`, etc.). The frontend `FoodItem` type and all property accesses in `log/page.tsx` used camelCase (`baseCalories`, `baseProtein`). Every catalog item property read returned `undefined`, fell through to `?? 0`.

Fix: `mapFoodItem()` helper added to `parse-food` EF converts all snake_case DB columns to camelCase before returning the `catalogMatch` payload. Applied to both single-food and recipe-ingredient matches.

### Edge Function Deployments (Phase W4)

```bash
supabase functions deploy log-food      # gram multiplier storage fix (storedQty = quantity / 100)
supabase functions deploy parse-food    # mapFoodItem() snake_case → camelCase mapper
```

### Architecture Decisions (Phase W4)

| # | Decision | Rationale |
|---|----------|-----------|
| W23 | Reverted sign-in to server action (`useActionState`) | W3.5 switched to `createBrowserClient()` to bypass proxy; however browser-client cookies are invisible to `proxy.ts` server client → redirect loop. Server action runs in the same server context as `proxy.ts` so cookies are shared. |
| W24 | `log-food` EF owns the gram → multiplier conversion for `consumed_qty` | Frontend sends raw display quantity (200g); EF stores Android-parity multiplier (2.0). Keeps display ↔ storage round-trip consistent across all clients without double-converting. |
| W25 | `mapFoodItem()` in `parse-food` EF normalises DB snake_case to camelCase | Supabase returns Postgres column names verbatim (`base_calories`). All frontend types use camelCase (`baseCalories`). Mapper centralises the translation at the API boundary rather than spreading raw column reads through the UI. |
| W26 | Goal reference lines on all three Insights chart variants | Android parity — all chart tabs (Week/Month/Year) show dashed goal lines. Y-axis domain expands to always accommodate goal lines even on low-data days. |

### Tech Debt Resolved in W4

| # | Issue | Resolution |
|---|-------|-----------|
| 2 | Insights + Catalog pages were placeholders | Full implementations delivered (items 22–23) |
| 9 | `EditLogSheet` lacked `toDisplayQty`/`fromDisplayQty` round-trip | Implemented in item 27 — gram entries now round-trip correctly |

---

## Phase W5: Cross-Platform Testing

**Status:** Not Started
**Target:** Week 5-6

### Planned Work
29. Conflict testing: Log food on Android (offline) -> edit same on web -> sync -> verify LWW
30. Delete testing: Delete on web -> sync Android -> verify delete-wins
31. Macro recalculation: Edit food macros on web -> verify daily logs updated via trigger
32. Label scanner parity: Same label on Android + web -> verify identical per-100g results
33. User preferences sync: Set goals on web -> verify Android reads from `user_preferences`
34. Tombstone testing: Delete on both platforms -> verify pg_cron purge after 15 days
35. Auth testing: Simultaneous sign-in on web + Android -> verify session isolation

---

## Architecture Decisions

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| W1 | Next.js 14 App Router | Native `@supabase/ssr`, RSC, Vercel deployment | May 14, 2026 |
| W2 | Edge Functions for all writes | Single execution point, prevents logic drift | May 14, 2026 |
| W3 | No `local_user` mapping | Web uses UUID directly from Supabase Auth | May 14, 2026 |
| W4 | Online-first, no IndexedDB | Supabase is single source of truth for web | May 14, 2026 |
| W5 | TanStack Query v5 | Optimistic updates, SWR caching, per-query loading states | May 14, 2026 |
| W6 | Zustand for UI state | Lightweight store for date navigation + form state | May 14, 2026 |
| W7 | Tailwind CSS v4 with CSS vars | Direct mapping from Material3 color scheme | May 14, 2026 |
| W8 | Parallel client calls for nutrition lookup | Preserves progressive UX (each item resolves independently) | May 14, 2026 |
| W9 | `extractJson()` shared utility | Handles Gemma 4 markdown-fenced responses robustly | May 14, 2026 |
| W10 | CORS headers on all Edge Functions | Required for cross-origin browser requests to Supabase | May 14, 2026 |

---

## Known Issues & Tech Debt

| # | Issue | Severity | Phase | Status | Notes |
|---|-------|----------|-------|--------|-------|
| 1 | `/log` page not yet created — FAB on dashboard links to it | Low | W3 | ✅ Resolved | Built in Phase W3 with AI Parse, Manual, and Scan tabs |
| 2 | ~~Insights + Catalog pages are placeholders~~ | ~~Low~~ | W4 | ✅ Resolved | Full implementations delivered in Phase W4 (items 22–23) |
| 3 | `useUpdateLog` uses direct Supabase update, not Edge Function | Medium | W3 | ✅ Resolved | Migrated to `supabase.functions.invoke(EDGE_FUNCTIONS.UPDATE_DAILY_LOG)` in Phase W3 |
| 4 | ~~Sign-in / Sign-up "fetch failed" on corporate network~~ | ~~Critical~~ | W3.5 | ✅ Resolved | W3.5 converted to `createBrowserClient()`. W4 re-introduced redirect loop (browser cookies invisible to `proxy.ts` server client) → reverted to server action `useActionState(signIn, undefined)`. |
| 5 | ~~`food_items` INSERT RLS violation~~ | ~~Critical~~ | W3.5 | ✅ Resolved | Missing INSERT policy. Apply migration 007 in Supabase SQL Editor. |
| 6 | ~~Signup crash (Android) after migration 008~~ | ~~Critical~~ | W3.5 | ✅ Resolved | `handle_new_user()` trigger FK ordering fix — `public.users` inserted before `catalogs`. |
| 7 | ~~`fdc-mapper.ts` discards FDC `servingSize`~~ | ~~High~~ | W3.5 | ✅ Resolved | `servingWeightG` now extracted and returned in `NutritionInfo` shape. `lookup-nutrition` redeployment required. |
| 8 | IFCT discrete units always assume 100g/unit | Low | W3.5 | Open | `servingWeightG: null` for all IFCT results — no serving size data in IFCT 2017. Workaround: use `"g"` unit. |
| 9 | ~~`EditLogSheet` lacks `toDisplayQty`/`fromDisplayQty` round-trip for grams~~ | ~~Medium~~ | W4 | ✅ Resolved | Implemented in Phase W4 item 27 — gram entries now show "200 g" in edit modal and save back as multiplier `2.0`. |
| 10 | ~~`parse-food` EF returned catalog matches with snake_case keys~~ | ~~High~~ | W4 | ✅ Resolved | Added `mapFoodItem()` mapper in `parse-food` EF (W4 B3 bugfix). `parse-food` redeploy required. |
| 11 | ~~Double unit conversion for gram-unit log-all / manual log paths~~ | ~~High~~ | W4 | ✅ Resolved | Removed `fromDisplayQty` from frontend; `log-food` EF now owns the `consumed_qty` gram→multiplier conversion (W4 B2 bugfix). `log-food` redeploy required. |
| 12 | `parse-food` EF has no retry logic for transient Gemini API 500 errors | Low | W5 | Open | Gemini free tier returns occasional `500 INTERNAL` errors under load or infrastructure churn. These surface to the user as "Edge Function returned a non-2xx status code". Fix: add 1–2 retries with exponential backoff in `parse-food/index.ts` before returning 502 to the client. Same pattern should be applied to `scan-label` and `lookup-nutrition` EFs. |
| 13 | ~~Android batch upsert PGRST102 blocked TC-30 Android→Web sync~~ | ~~Critical~~ | W5 | ✅ Fixed (Android) | `kotlinx.serialization` `explicitNulls = false` stripped nullable fields from some objects, making PostgREST batch key-set check fail. Fixed on Android via `@Named("supabase") Json` with `explicitNulls = true`. No webapp changes needed — server schema and RLS were correct. |
| 14 | ~~Android cross-user Room contamination caused 403 RLS on food_items upsert~~ | ~~Critical~~ | W5 | ✅ Fixed (Android) | Stale Room rows from a previous user were pushed under a new user's token, failing RLS UPDATE check. Fixed on Android: `signOut()` now wipes all user-specific Room tables and resets the sync cursor. No webapp changes needed. |
| 15 | ~~Android EditLogSheet pushes stale macros when only qty changes~~ | ~~High~~ | W5 | ✅ Fixed (Android) | Edit sheet didn't recalculate totals on qty change — webapp showed updated qty but old macros. Fixed: `updateEditQty()`/`updateEditUnit()` now auto-recalculate from per-serving base macros. No webapp changes needed. |
| 16 | Per-serving vs per-100g macro mismatch — cross-platform | Critical | W11 | 🔲 Planned | `food_items.base_calories` stores per-serving macros but `computeServingMultiplier()` assumes per-100g. Editing log from "1 serving" to "150g" produces 481 kcal instead of 241. Phase W11 guard is symptomatic. Architectural fix: normalize all `base_*` to per-100g at write time in Edge Functions (`log-food`, `log-recipe`), update `_shared/macro-calculator.ts`, migrate existing data. Plan: `~/.wibey/plans/per100g_normalization_b0f402b8.plan.md`. |
| 17 | `log-recipe` EF stores raw gram qty without `/100` conversion | High | W11 | 🔲 Planned | `log-recipe/index.ts` line 154: `consumed_qty: quantity` — unlike `log-food` which does `quantity / 100` for gram units. Results in inflated macro totals for gram-based recipe logs. To be fixed in per-100g normalization. |

---

## Phase W5: Cross-Platform Testing

**Status:** ✅ Complete (6/6 PASSED)
**Date:** May 22, 2026

### Summary

End-to-end validation of bidirectional sync between the Android app and the webapp. Both clients share the same Supabase PostgreSQL backend. The web app is online-first (no local DB); the Android app is offline-first (Room → Supabase push/pull). Tests verify that data written on one platform correctly appears on the other after sync.

### Test Case Tracker

| TC | Description | Status |
|----|-------------|--------|
| TC-30 | Android → Web: catalog item + daily log created on Android syncs to Supabase and appears in webapp | ✅ PASSED |
| TC-31 | Web → Android: catalog item + daily log created on webapp syncs to Android via pull | ✅ PASSED |
| TC-32 | Edit on Android syncs to webapp (LWW resolution) | ✅ PASSED |
| TC-33 | Macro goals set on webapp sync to Android | ✅ PASSED |
| TC-34 | Soft-delete on Android removes item from webapp | ✅ PASSED |
| TC-35 | Soft-delete on webapp removes item from Android on next pull | ✅ PASSED |

### TC-30: Android → Web Sync

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Android-Side Bugs Fixed

The Supabase server-side setup (RLS policies, schema, indexes) was already correct. Both bugs were in the Android client.

**Bug W5-1 (Android): PGRST102 "All object keys must match"**

The Android sync push uses batch POST arrays to Supabase PostgREST. `kotlinx.serialization` with `explicitNulls = false` (required by the Gemini API client) was silently omitting nullable fields (`brand`, `deleted_at`, `external_api_id`) from some objects in the batch. PostgREST enforces that every object in a batch upsert must have identical JSON key sets — objects with different key counts triggered PGRST102 and aborted the entire batch.

Fix: Android `SupabaseModule` was given a separate `@Named("supabase") Json` instance with `explicitNulls = true`. This instance is used exclusively by the Supabase Retrofit client; the Gemini client continues using the app-wide `Json` with `explicitNulls = false`. No changes required on the Supabase / web side.

**Bug W5-2 (Android): 403 RLS "USING expression" from cross-user Room contamination**

Room DB was not cleared on sign-out. Food items belonging to a previous user (catalog IDs containing that user's UUID prefix) persisted locally. When a different user signed in on the same device and triggered a sync, those stale rows were pushed under the new user's auth token. The Supabase RLS UPDATE policy checked the existing row's `catalog_id` ownership — found it belonged to a different user — and rejected with 403. Daily logs referencing those food items then cascaded to a 409 FK violation.

Fix: (1) Orphaned rows manually deleted from Supabase. (2) `AuthRepositoryImpl.signOut()` now wipes all user-specific Room tables (`daily_logs` → `food_items` → `catalogs` → `users`) on every sign-out, and clears the `last_sync_at` cursor so the next sign-in performs a full pull. No changes required on the Supabase / web side.

#### Result
After both Android fixes, all three tables synced successfully. Webapp displayed the correct catalog entry and daily log with accurate macros. TC-30 **PASSED**.

### TC-32: Edit on Android → Web Sync

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Android-Side Bug Fixed

**Bug W5-3 (Android): EditLogSheet does not auto-recalculate macros on quantity change**

The Android edit sheet treated quantity and macro text fields as independent inputs. When the user changed only the quantity (1→2 servings), the calorie/protein/carbs/fat fields retained their old 1-serving totals. The stale totals were pushed to Supabase. Since the webapp reads `total_*` columns directly, it showed the updated quantity but macros for the previous quantity.

Fix: `EditLogSheet` now stores per-serving base macros (derived at pre-fill time as `total / consumedQty`). `updateEditQty()` and `updateEditUnit()` auto-recalculate totals as `base × newStoredQty`. No changes required on the Supabase / web side.

#### Result
Catalog item name and daily log quantity updated correctly on webapp with correctly recalculated macros. TC-32 **PASSED**.

### TC-34: Soft-Delete on Android → Web

**Status:** ✅ PASSED
**Date:** May 22, 2026

No bugs found. Soft-delete tombstone synced from Android; webapp's `deleted_at IS NULL` filter correctly hid the item. TC-34 **PASSED**.

### TC-35: Soft-Delete on Web → Android

**Status:** ✅ PASSED
**Date:** May 22, 2026

No bugs found. Webapp soft-delete propagated to Android via pull sync. Delete-wins conflict rule applied correctly. TC-35 **PASSED**.

### TC-33: Macro Goals Cross-Platform Sync

**Status:** ✅ PASSED
**Date:** May 22, 2026

**Pre-requisite:** Phase 14 implemented on Android — migrated macro goals from local DataStore to Room + Supabase bidirectional sync.

**Bug W5-4 (Android):** Push DTO included `"updated_at": null` which violated the `NOT NULL` constraint on `user_preferences.updated_at`. Fixed with a separate push DTO excluding the server-managed column. No webapp changes needed.

Goals set on webapp synced to Android via pull. Goals set on Android synced to webapp via push. Bidirectional confirmed. TC-33 **PASSED**.

---

## Phase W6: Production Deployment

**Status:** ✅ Complete
**Date:** May 23, 2026
**Live URL:** https://nutri-ai-git-main-khushi-s-s-projects.vercel.app

### Summary

Pushed the NutriAI monorepo to GitHub (`github.com/kshah-5683/NutriAI`) and deployed the `webapp/` Next.js app to Vercel. Required resolving a chain of dependency, type, build, and Vercel configuration issues before the app was live.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/.npmrc` | Created | `legacy-peer-deps=true` — suppresses npm peer dep conflicts (React 19 + older package declarations) |
| 2 | `webapp/package.json` | Updated | Added `"engines": { "node": "20.x" }` to pin Node version on Vercel (was defaulting to Node 24 which caused silent npm crashes) |
| 3 | `webapp/package.json` | Updated | Added `react-is: ^19.2.6` as a direct dependency — `recharts@3.8.1` declares `react-is` as a peer dep but does not install it automatically; webpack build failed with `Module not found: Can't resolve 'react-is'` |
| 4 | `webapp/package.json` | Updated | Changed build script from `next build` to `next build --webpack` — Turbopack production output is not recognized by Vercel's edge routing layer (all routes return `NOT_FOUND`); webpack output deploys correctly |
| 5 | `webapp/package-lock.json` | Committed | Committed lockfile to ensure Vercel uses `npm ci` for deterministic installs rather than bare `npm install` (which was silently OOM-crashing on Node 20.x) |
| 6 | `webapp/lib/types/database.ts` | Updated | Added `Relationships: []` to every table definition and `CompositeTypes: Record<string, never>` to the public schema — required by `supabase-js` v2's internal `GenericSchema` type. Without these fields, supabase-js silently fell back to an untyped client and `.from("user_preferences")` returned `never`, breaking `use-macro-goals.ts` and `use-update-goals.ts` at TypeScript compile time |
| 7 | `webapp/lib/hooks/use-delete-food.ts` | Updated | Removed `// @ts-expect-error` suppression — now unnecessary after `Relationships`/`CompositeTypes` fix; keeping it raised `TS2578: Unused '@ts-expect-error' directive` |
| 8 | `webapp/lib/hooks/use-delete-log.ts` | Updated | Same as above |
| 9 | `webapp/next.config.ts` | Updated | Added `serverExternalPackages: ["undici", "https-proxy-agent"]` — these packages use `node:` URI imports (e.g. `node:crypto`, `node:dns`, `node:console`) that webpack cannot resolve. Marking them as server externals tells webpack to leave them as native `require()` calls instead of bundling them. Without this, webpack produced `UnhandledSchemeError` for every `node:*` import in `undici` |
| 10 | `webapp/proxy.ts` (diagnostic) | Temporary | Replaced auth guard with bare passthrough (`NextResponse.next()`) to isolate the edge 404 cause — confirmed the NOT_FOUND was independent of proxy code |
| 11 | `webapp/proxy.ts` | Restored | Restored full auth guard (session refresh + unauthenticated redirect + auth-route redirect) after Vercel config was fixed |

### Deployment Issue Chain (RCA)

#### D1 — `npm install` silent crash on Vercel
**Symptom:** Build fails at "Installing dependencies" with `npm error Exit handler never called!` after ~70s. No dependency error shown.
**Root cause:** Vercel was defaulting to Node 24 which ships npm 10.x; a combination of React 19 peer dep conflicts + npm 10.x's dependency resolution caused npm's internal event loop to die before exit handlers ran.
**Fix:** Pinned `engines.node = "20.x"` in `package.json`. Vercel respects this and switches its build container to Node 20 (where the same install completes in 23s locally).

#### D2 — `react-is` missing at build time
**Symptom:** Turbopack build error: `Module not found: Can't resolve 'react-is'` from `recharts/es6/util/ReactUtils.js`.
**Root cause:** `recharts@3.8.1` lists `react-is` as a peer dependency but does not list it as a direct dependency. With `--legacy-peer-deps` the missing peer was ignored during install but the import was still expected at build time.
**Fix:** Added `react-is: ^19.2.6` as a direct dependency.

#### D3 — Turbopack production output gives `NOT_FOUND` on Vercel edge
**Symptom:** Build succeeds (all routes present in build log); Vercel reports "Deployment completed"; but every URL returns Vercel's `404: NOT_FOUND` with no runtime logs generated.
**Root cause:** Next.js 16 Turbopack production builds generate a routing manifest in a format that Vercel's edge routing layer does not yet fully support. The edge layer can't map the hostname to any function/static asset and returns `NOT_FOUND` before the request reaches the app.
**Fix:** Switched build to `next build --webpack`. Webpack produces the established output format that Vercel's routing layer correctly processes.

#### D4 — `undici` breaks webpack build
**Symptom:** `next build --webpack` fails with multiple `UnhandledSchemeError: Reading from "node:crypto" is not handled by plugins` and similar errors for `node:console`, `node:dns`, `node:diagnostics_channel`, `string_decoder`.
**Root cause:** `undici` (used by `instrumentation.ts` for Walmart corporate proxy support) uses Node.js built-in module references with the `node:` URI prefix. Webpack's resolver handles `data:` and `file:` URIs by default but not `node:`.
**Fix:** Added `serverExternalPackages: ["undici", "https-proxy-agent"]` to `next.config.ts`. These packages are server-only and never need to be bundled; marking them as externals tells webpack to emit `require("undici")` calls instead of attempting to bundle the source.

#### D5 — Supabase TypeScript `never` type errors blocking build
**Symptom:** `npx tsc --noEmit` produces 5 errors: `Property 'calorie_goal' does not exist on type 'never'` in `use-macro-goals.ts` and `Object literal may only specify known properties, and 'user_id' does not exist in type 'never[]'` in `use-update-goals.ts`.
**Root cause:** `supabase-js` v2's internal `GenericSchema` type requires every table to have a `Relationships` field and the public schema to have a `CompositeTypes` field. The stub `database.ts` was missing both. When supabase-js validates the `Database` generic against `GenericSchema` and finds a mismatch, it silently falls back to an untyped client — all `.from()` calls return `never`.
**Fix:** Added `Relationships: []` to all five tables in `database.ts` and `CompositeTypes: Record<string, never>` to the public schema. This also fixed the pre-existing `@ts-expect-error` suppressions in `use-delete-food.ts` and `use-delete-log.ts` (those tables were affected by the same issue and are now correctly typed).

#### D6 — Vercel Framework Preset set to "Other"
**Symptom:** Despite correct root directory and successful build, Vercel's "Source" tab showed empty deployment; all URLs returned `NOT_FOUND`.
**Root cause:** Vercel's Framework Preset was set to "Other" instead of "Next.js". With the wrong preset, Vercel does not set up Next.js-specific output routing, serverless function wiring, or CDN configuration — the build output is created but not wired to any serving infrastructure.
**Fix:** Changed Framework Preset to "Next.js" in Vercel → Settings → Build and Deployment. Triggered redeploy. App became live immediately.

### Vercel Final Configuration

| Setting | Value |
|---------|-------|
| **Framework Preset** | Next.js |
| **Root Directory** | `webapp` |
| **Install Command** | `yarn install` |
| **Build Command** | `next build --webpack` (overridden) |
| **Node.js Version** | 20.x |
| **`NEXT_PUBLIC_SUPABASE_URL`** | Supabase project URL (Production + Preview) |
| **`NEXT_PUBLIC_SUPABASE_ANON_KEY`** | Supabase anon key (Production + Preview) |

### Post-Deployment Supabase Auth Configuration

- **Site URL:** `https://nutri-ai-git-main-khushi-s-s-projects.vercel.app`
- **Redirect URLs:** `https://nutri-ai-git-main-khushi-s-s-projects.vercel.app/**`

### Architecture Decisions (Phase W6)

| # | Decision | Rationale |
|---|----------|-----------|
| W27 | `next build --webpack` for production | Turbopack production output is not recognized by Vercel's current edge routing layer. Webpack output is the established format Vercel supports. Dev server still uses Turbopack via `next dev`. |
| W28 | `serverExternalPackages` for undici + https-proxy-agent | These packages are server-only and use `node:` URI imports webpack cannot resolve. Externals emit native `require()` calls; no bundling needed. |
| W29 | `Relationships: []` + `CompositeTypes` in database.ts | Required by supabase-js v2's `GenericSchema` constraint. Without them, `Database` generic is silently rejected and all `.from()` calls become untyped (`never`). |
| W30 | yarn install on Vercel | npm silently crashes during dependency install on Node 20.x for this project's dependency tree. yarn resolves all packages successfully in ~28s. |

### Known Issues Added

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 16 | `instrumentation.ts` Walmart proxy setup is dead code on Vercel — `HTTPS_PROXY` is never set in Vercel's environment | Low | Open — no impact on functionality; clean up when refactoring instrumentation |
| 17 | `proxy.ts` naming is Next.js 16 convention; `next build --webpack` raises a deprecation warning ("middleware file convention is deprecated, use proxy instead") — both coexist but the warning is misleading | Low | Open — will resolve when Vercel supports Turbopack production output and `--webpack` flag is removed |

---

## Phase W7: Sign-Up Flow — Confirmation Before Sign-In

**Status:** ✅ Completed
**Date:** May 23, 2026

### Summary

Changed the sign-up success flow so users land on a confirmation screen before being redirected to sign-in, rather than going straight to the home dashboard. After `supabase.auth.signUp()` resolves without error, the form is replaced by a confirmation screen. A 2-second timer auto-redirects to `/auth/sign-in`; a manual "Sign in now →" link is available as an immediate fallback.

Previously: Sign up → `router.push("/")` → Home (bypassed sign-in entirely)
Now: Sign up → "Account created!" confirmation screen (2s) → `/auth/sign-in` → user signs in → Home

Also deleted `webapp/app/auth/confirm/route.ts` — the PKCE email confirmation route handler is no longer needed now that email confirmation is disabled in Supabase (Authentication → Providers → Email → Confirm email: off).

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/app/auth/sign-up/page.tsx` | Updated | Added `useEffect` import. Added `success: boolean` state. On `signUp()` success: `setSuccess(true)` instead of `router.push("/")`. `useEffect` watches `success` — fires `router.push("/auth/sign-in")` after 2000 ms with cleanup to cancel if the component unmounts. Added `success` conditional render: ✅ icon + "Account created!" heading + "Redirecting you to sign in…" subtitle + "Sign in now →" manual link. |
| 2 | `webapp/app/auth/confirm/route.ts` | Deleted | PKCE email confirmation callback route — no longer needed. Supabase "Confirm email" is disabled; no confirmation emails are sent and no code exchange is required. |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W31 | 2-second auto-redirect on sign-up success | Gives the user clear visual feedback that the account was created before moving on. Short enough not to feel like a delay; long enough to be readable. Manual "Sign in now →" link allows immediate navigation without waiting. |
| W32 | Redirect to `/auth/sign-in`, not `/` | The sign-up action creates the account but does not establish a session (with email confirmation disabled, Supabase may return a session-less user). Redirecting to the sign-in form ensures the user explicitly authenticates and that `proxy.ts` finds a valid JWT before allowing access to the dashboard. |

---

## Phase W7.5: Responsive Insight Charts

**Status:** ✅ Completed
**Date:** May 26, 2026

### Summary

All three Recharts-based Insights page charts used hardcoded `width={500}` which caused horizontal scrolling on viewports narrower than 500px. Replaced with Recharts' `ResponsiveContainer` so charts scale to fill their parent container width. Android's Canvas-based charts were already fully responsive.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `components/insights/macro-bar-chart.tsx` | Updated | Added `ResponsiveContainer` import. Wrapped `<BarChart>` in `<ResponsiveContainer width="100%" height={250}>`. Removed `width={500}` from `BarChart`. Changed outer `<div>` from `overflow-x-auto` to `w-full`. |
| 2 | `components/insights/macro-line-chart.tsx` | Updated | Same pattern — `<LineChart width={500} height={260}>` → `<ResponsiveContainer width="100%" height={260}>` wrapping `<LineChart>`. Removed `overflow-x-auto`. |
| 3 | `components/insights/macro-year-chart.tsx` | Updated | Same pattern — `<BarChart width={500} height={250}>` → `<ResponsiveContainer width="100%" height={250}>` wrapping `<BarChart>`. Removed `overflow-x-auto`. |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W33 | `ResponsiveContainer` over fixed `width` for all Recharts charts | Recharts SVG charts do not auto-size. `ResponsiveContainer` measures the parent DOM element's width on mount and resize, then passes it to the chart. Fixed heights (250–260px) are kept because the parent has no explicit height for the container to inherit. This matches Android's `Canvas(Modifier.fillMaxWidth().height(200.dp))` pattern. |

---

## Phase W8: Discrete Unit Macro Fix + Preview Card Correction

**Status:** ✅ Completed
**Date:** May 26, 2026

### Summary

Two related macro calculation bugs on the webapp Log page:

1. **Discrete unit inflation** — "1 piece boiled egg" logged 155 kcal (full per-100g) instead of ~78 kcal. The `lookup-nutrition` Edge Function (via `fdc-mapper.ts`) returned `servingWeightG: 50` from USDA FDC, but the webapp type didn't declare the field and no code used it. Fixed by adding `servingWeightG` to `NutritionInfo` and pre-scaling macros from per-100g to per-unit in `acceptParsedFood()` when the unit is discrete (piece/slice/bowl) and `servingWeightG` is known. Port of Android Phase 12 fix.

2. **Preview card wrong for all non-serving units** — `MacroPreviewCard` did raw `base × quantity` multiplication. For gram units (200g chicken, 155 kcal/100g) this showed `155 × 200 = 31,000 kcal` instead of `155 × 2.0 = 310 kcal`. Same bug for cup/tbsp/tsp units. The saved data was always correct (Edge Function uses `computeServingMultiplier()`), but the live preview was wrong. Fixed by using `computeServingMultiplier()` in the preview card.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `lib/types/ai.ts` | Updated | Added `servingWeightG: number \| null` to `NutritionInfo` interface with JSDoc. Already returned by `lookup-nutrition` Edge Function — was silently dropped because the type didn't declare it. |
| 2 | `lib/stores/log-form-store.ts` | Updated | `acceptParsedFood()`: detects discrete units (piece/slice/bowl); when `nutrition.servingWeightG` is available, pre-scales macros from per-100g to per-unit (`per100g × servingWeightG / 100`). Updates `servingG` to match. Non-discrete units unchanged. |
| 3 | `components/macro-preview-card.tsx` | Updated | Added `unit` prop (defaults to `"serving"`). Replaced `base × quantity` with `base × computeServingMultiplier(quantity, unit)`. Updated label from static `"Total for X serving(s)"` to context-aware `"Total for 200g"` / `"Total for 2 cups"` / `"Total for 1 piece"`. Added `computeServingMultiplier` and `isGramUnit` imports. |
| 4 | `components/manual-input-section.tsx` | Updated | Passes `unit` prop to `<MacroPreviewCard>`. Changed static `"Nutrition per 100g"` label to dynamic: `"Nutrition per piece/slice/bowl"` for discrete units, `"Nutrition per 100g"` for all others. |

### Calculation Correctness — Before / After

| Scenario | Preview Before | Preview After | Saved (unchanged) |
|----------|---------------|--------------|-------------------|
| "1 piece boiled egg" (FDC `servingWeightG=50`) | 155 kcal ❌ | ~78 kcal ✅ | ~78 kcal ✅ (after pre-scale) |
| "200g chicken" (155 kcal/100g) | 31,000 kcal ❌ | 310 kcal ✅ | 310 kcal ✅ |
| "2 cups oats" (379 kcal/100g) | 758 kcal ❌ | 1,818 kcal ✅ | 1,818 kcal ✅ |
| "2 servings rice" (130 kcal/100g) | 260 kcal ✅ | 260 kcal ✅ | 260 kcal ✅ |
| "1 tbsp olive oil" (884 kcal/100g) | 884 kcal ❌ | 133 kcal ✅ | 133 kcal ✅ |

### Known Limitation

IFCT-sourced foods never report `servingWeightG` (always null). Discrete units for IFCT foods still use the 100g-per-unit assumption. Same known limitation as Android (Known Issue #40). Workaround: switch unit to "g" and enter actual gram weight.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W34 | Pre-scale macros in `acceptParsedFood()`, not in Edge Function | Matches Android Phase 12 pattern. Pre-scaling converts per-100g to per-unit at accept time so the form, preview, and save all work with the same base values. No Edge Function API change needed — `computeServingMultiplier("piece")` returns raw quantity (1.0), and `total = perUnit × 1 = perUnit` is correct. |
| W35 | `computeServingMultiplier()` in `MacroPreviewCard` (not raw multiplication) | The preview must mirror the server-side calculation exactly. Raw `base × quantity` was only correct for serving units. Using the same multiplier function ensures the preview matches what gets stored. |
| W36 | Dynamic "Nutrition per X" label based on unit | After pre-scaling, discrete unit form fields hold per-unit values (not per-100g). The label must reflect this so users know what the macro numbers represent. For non-discrete units, macros remain per-100g. |

---

## Phase W9: Manual Recipe Builder + Catalog Navigation Fix + Catalog Miss Bug

**Status:** ✅ Completed
**Date:** May 27, 2026

### Summary

Three related improvements delivered together — webapp parity with Android Phase 16:

1. **Manual recipe builder mode** — `ManualInputSection` now branches between flat ingredient mode and recipe builder mode via `isRecipeMode` from the Zustand store. The recipe builder shows a dynamic ingredient list (`ManualRecipeIngredientRow`) with per-ingredient catalog search, qty/unit fields, macro fields, and a live aggregated macro preview card (`ManualRecipeIngredientsSection`).

2. **Catalog page FAB opens recipe builder mode** — Clicking `+` on the Recipes catalog tab previously opened the flat ingredient form. Fixed: the FAB now pre-sets the Zustand store (`setInputMode("manual")` + `toggleRecipeMode(true)`) before calling `router.push("/log")`, so the Log page mounts directly in recipe builder mode.

3. **Redundant USDA lookup for catalog-saved recipes** — The `parse-food` Edge Function only queried `ingredientCatalogId` for items where `is_recipe=false`. Gemini sometimes emits `is_recipe=false` for recipes already in the user's recipe catalog (e.g. "banana bread"). The catalog miss returned `catalogMatch: null`, causing `use-nutrition-lookup.ts` to fire a USDA FDC lookup even though macros were already known. Fixed by adding a recipe catalog fallback inside the Edge Function.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `components/manual-recipe-ingredient-row.tsx` | Created | Per-ingredient row component with catalog chip (clears on tap), search dropdown (debounced), qty + unit inputs, macro fields (kcal/P/C/F), remove button, and `IngredientMacroBadge` for live per-row macro preview. Fixed `as const` array shape error: added `unit: ""` to the kcal badge object so all items have identical shapes. |
| 2 | `components/manual-recipe-ingredients-section.tsx` | Created | Recipe builder section: header, `forEach` ingredient row, `+ Add Ingredient` button, recipe-level serving qty/unit row. |
| 3 | `components/manual-input-section.tsx` | Updated | Branches on `isRecipeMode` from Zustand store: `true` → `ManualRecipeIngredientsSection` + aggregated macro preview card + Save Recipe button; `false` → unchanged flat form. Fixed `??` / `||` operator precedence: wrapped `|| 0` in parentheses (`?? (parseFloat(...) || 0)`) to resolve `ts(5076)`. |
| 4 | `lib/utils/macro-calculator.ts` | Updated | Same `??` / `||` precedence fix in `computeRecipeMacros()`. |
| 5 | `app/catalog/page.tsx` | Updated | Added `useRouter` + `useLogFormStore` imports. FAB `<a href="/log">` replaced with `<button onClick={handleAddClick}>`. `handleAddClick` pre-sets Zustand store (`setInputMode("manual")` + `toggleRecipeMode(true)` for Recipes tab, `toggleRecipeMode(false)` for Ingredients tab) before calling `router.push("/log")`. |
| 6 | `supabase/functions/parse-food/index.ts` | Updated | For `is_recipe=false` items: if `ingredientCatalogId` query returns no match, a second query against `recipeCatalogId` is made before returning `catalogMatch: null`. Prevents catalog misses when Gemini mis-classifies a saved recipe as a non-recipe item. |

### Key Implementation Details

**Catalog FAB pre-sets store before navigation (`catalog/page.tsx`):**
```tsx
const handleAddClick = () => {
  if (selectedTab === "recipes") {
    setInputMode("manual");
    toggleRecipeMode(true);
  } else {
    toggleRecipeMode(false);
  }
  router.push("/log");
};
```
Plain `<a href>` navigation bypasses the Zustand store mutation (store is not serialised into the URL). The button + `router.push` pattern ensures the store is updated before the Log page reads it on mount.

**Edge Function recipe catalog fallback (`parse-food/index.ts`):**
```ts
const primaryCatalogId = food.is_recipe ? recipeCatalogId : ingredientCatalogId;
// ... primary query ...
let match = primaryMatch;
if (!match && !food.is_recipe) {
  const { data: recipeMatch } = await supabase
    .from("food_items")
    .select("*")
    .eq("catalog_id", recipeCatalogId)
    .ilike("name", food.name)
    .is("deleted_at", null)
    .limit(1)
    .maybeSingle();
  match = recipeMatch ?? null;
}
```

### TypeScript Fixes

| Error | File | Root Cause | Fix |
|-------|------|-----------|-----|
| `ts(5076)` `??` and `\|\|` mixed | `macro-calculator.ts` | TypeScript disallows mixing `??` and `\|\|` without explicit parentheses | `?? (parseFloat(...) \|\| 0)` |
| `ts(5076)` (×2) | `manual-input-section.tsx` | Same | Same fix in two places |
| `ts(2339)` missing `unit` | `manual-recipe-ingredient-row.tsx` | `as const` array: first object had no `unit` field, breaking destructured type inference | Added `unit: ""` to the kcal macro badge object |

### Edge Function Deployment Required

```bash
supabase functions deploy parse-food   # recipe catalog fallback for is_recipe=false items
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W37 | Pre-mutate Zustand store before `router.push("/log")` from catalog FAB | Next.js `router.push` does not serialise component state into the URL. The Log page reads `isRecipeMode` from the Zustand store on mount — store must be set before navigation, not after. |
| W38 | Recipe catalog fallback in `parse-food` Edge Function (not client-side) | Catalog resolution is server-side for the webapp (unlike Android where `ResolveCatalogCacheUseCase` runs client-side). The fallback must live in the Edge Function so the returned `catalogMatch` is accurate before `use-nutrition-lookup.ts` reads it. |
| W39 | Two-query fallback rather than OR query | `ilike("name", ...).eq("catalog_id", ...)` is already indexed. Running two sequential queries (ingredient → recipe) is simpler and equally fast at this scale. An OR across two `catalog_id` values would require query restructuring and would complicate the "which catalog did it come from?" tracking if ever needed. |

---

## Phase W10: Serving Size Clarification — Brand-Aware Nutrition Lookup

**Status:** ✅ Completed
**Date:** May 27, 2026

### Summary

Foods with variable serving sizes (bread slices, cheese slices, tortillas, protein bars) now trigger an interactive clarification flow. The AI prompt detects serving-size ambiguity and sets `needs_clarification: true` with a helpful hint. The nutrition lookup is paused until the user resolves the ambiguity by:

1. **"Use generic"** — accept the standard USDA/IFCT estimate
2. **Brand name** — trigger a brand-specific FDC Branded lookup (e.g. "Nature's Own" → FDC branded wheat bread)
3. **Weight override** — provide an explicit gram weight per serving unit (e.g. "40g")

The `lookup-nutrition` Edge Function was rewritten with a tiered fallback strategy: FDC Branded (when brand provided) → FDC All Types → IFCT full phrase → IFCT word-by-word → null. Each result includes a `matchType` field ("branded" / "generic" / null) for transparent match quality badges in the UI.

Cross-platform parity with Android Phase 17.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `supabase/functions/_shared/prompts.ts` | Updated | Added SERVING SIZE AMBIGUITY DETECTION section to system prompt with variable-size food examples. Updated JSON schema with `needs_clarification` (boolean) and `clarification_hint` (string/null) fields. |
| 2 | `supabase/functions/_shared/fdc-mapper.ts` | Updated | Added `matchType: "branded" \| "generic" \| null` to `NutritionInfo` interface. Set `matchType: null` in `mapFdcToNutritionInfo()` (caller overrides). |
| 3 | `supabase/functions/parse-food/index.ts` | Updated | Pass through `needsClarification` and `clarificationHint` in response. Catalog match overrides: `needsClarification: match ? false : (food.needs_clarification ?? false)`. |
| 4 | `supabase/functions/lookup-nutrition/index.ts` | Rewritten | Brand-aware tiered fallback. New request shape: `{ foodNames, brands? }`. Extracted `searchFdc()` helper. Tier 1a: FDC Branded (matchType: "branded"), Tier 1b: FDC All Types (matchType: "generic"), Tier 2–3: IFCT (matchType: "generic"), Tier 4: null. |
| 5 | `webapp/lib/types/ai.ts` | Updated | Added `needsClarification`, `clarificationHint` to `ParsedFood`. Added `matchType` to `NutritionInfo` and `LookupNutritionResponse.results[]`. |
| 6 | `webapp/lib/stores/log-form-store.ts` | Updated | Added `ClarificationType`, `ClarificationResolution` types. Added `clarificationResolutions` state and `resolveClarification()` action. Updated `acceptParsedFood()` to use weight override from clarification. |
| 7 | `webapp/lib/hooks/use-nutrition-lookup.ts` | Updated | `lookupNutrition()` accepts optional `brand` parameter. `lookupAll()` skips `needsClarification` items. |
| 8 | `webapp/components/clarification-input.tsx` | Created | `ClarificationInput` banner: hint text, input field, "Use generic" / "Update & Lookup" buttons. `MatchTypeBadge`: green "Exact brand match" / amber "Generic estimate" / amber "Brand not found, using generic". |
| 9 | `webapp/components/parsed-food-card.tsx` | Updated | New props: `clarificationResolution`, `onUseGeneric`, `onSubmitClarification`. Shows `ClarificationBanner` when `needsClarification && !resolved`. Shows `MatchTypeBadge` after nutrition resolves. Hides nutrition status during active clarification. |
| 10 | `webapp/components/ai-input-section.tsx` | Updated | Added `handleUseGeneric(index)` and `handleSubmitClarification(index, input)` handlers. Weight vs brand detection via regex. Passes clarification props to ParsedFoodCard. |

### Key Implementation Details

**Tiered fallback in `lookup-nutrition/index.ts`:**
```ts
// Tier 1a: Brand-specific FDC (when brand provided)
if (brand) {
  const branded = await searchFdc(`${brand} ${name}`, "Branded");
  if (branded) return { ...branded, matchType: "branded" };
}
// Tier 1b: FDC All Types (generic)
const generic = await searchFdc(name);
if (generic) return { ...generic, matchType: "generic" };
// Tier 2–3: IFCT fallback → Tier 4: null
```

**Weight override priority in `log-form-store.ts`:**
```ts
const clarification = state.clarificationResolutions[index];
const weightOverride = clarification?.weightOverrideG;
const swg = weightOverride ?? nutrition?.servingWeightG;
```

**Clarification detection in `ai-input-section.tsx`:**
```ts
const weightMatch = input.match(/^(\d+\.?\d*)\s*g?$/i);
if (weightMatch) {
  resolveClarification(index, "weight", String(weightG));
} else {
  resolveClarification(index, "brand", input);
}
```

### Edge Function Deployment Required

```bash
supabase functions deploy parse-food        # needs_clarification pass-through
supabase functions deploy lookup-nutrition   # brand-aware tiered fallback + matchType
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W40 | AI-level ambiguity detection over client-side heuristics | The Gemini model has food knowledge to determine which items have genuinely variable serving sizes. Client heuristics (e.g. "if unit is slice") would be brittle and miss edge cases. |
| W41 | Tiered FDC fallback: Branded → All Types → IFCT | Brand-specific data is most accurate for variable-size products. Graceful degradation ensures the user always gets some estimate rather than nothing. |
| W42 | `matchType` field for transparent match quality | Users need to know whether "120 kcal per slice" came from the exact brand they specified or a generic USDA average. Badge color (green vs amber) communicates confidence without requiring nutrition literacy. |
| W43 | Weight override applied client-side, not server-side | When the user says "my bread slices are 40g each", the per-100g macros from USDA are unchanged — only the serving multiplier changes. Applying the override in `acceptParsedFood()` keeps the lookup pipeline clean and avoids re-fetching data. |
| W44 | Nutrition lookup paused until clarification resolved | Firing a generic lookup immediately and then overwriting it wastes an API call and causes a visual flicker (loading → found → loading → found). Pausing until the user acts is cleaner UX. |

---

## Phase W10.1: Clarification Robustness — Prompt Expansion, Brand Validation & Dark Mode Fix

**Status:** ✅ Completed
**Date:** May 28, 2026

### Summary

Three post-implementation fixes based on real-world testing of Phase W10:

1. **AI prompt expanded to three ambiguity cases** — The original prompt only flagged variable-size discrete units (Case A: "1 slice bread"). Now also flags bare generic food names without type (Case B: "cheese" → cheddar? mozzarella? paneer?) and specific foods without concrete amounts (Case C: "cheddar cheese" → how much?). Hints guide users to specify weight in grams.

2. **FDC Branded result brand validation** — Searching FDC Branded for "Amul cheese" returned "Food Lion cheese" (wrong brand) because FDC's search is keyword-based. Now validates that the returned result's `brand` field contains the user's requested brand (case-insensitive). Mismatches fall through to generic tier → UI correctly shows "Brand not found, using generic" badge.

3. **Dark mode readability fix** — Clarification banner, match badges, and selected parsed food card used hardcoded light-mode colors (`#FFF8E1`, `#D4E8C8`) that didn't adapt in dark mode. Added dark-mode-aware semantic CSS variables (`--bg-warning`, `--bg-branded`, `--bg-primary-container`, `--text-on-primary-container`) that swap via `.dark` class — same pattern as Android's `MaterialTheme.colorScheme`.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `supabase/functions/_shared/prompts.ts` | Updated | Expanded ambiguity detection: Case A (variable discrete units), Case B (bare generic names), Case C (specific food without concrete amount). Weight-focused hints with gram reference points. |
| 2 | `supabase/functions/lookup-nutrition/index.ts` | Updated | Tier 1a brand validation: checks `brandedResult.brand` contains requested brand (case-insensitive word match). Mismatches log warning and fall through to generic. |
| 3 | `webapp/app/globals.css` | Updated | Added 8 semantic dark-mode-aware variables: `--bg-warning`/`--border-warning`/`--text-warning`, `--bg-branded`/`--text-branded`, `--bg-primary-container`/`--text-on-primary-container`/`--color-on-primary` with light and dark values. |
| 4 | `webapp/components/clarification-input.tsx` | Updated | Banner and badge colors switched from hardcoded hex to `var(--bg-warning)`, `var(--text-warning)`, `var(--bg-branded)`, `var(--text-branded)`. |
| 5 | `webapp/components/parsed-food-card.tsx` | Updated | Selected card background → `var(--bg-primary-container)`. All text elements use `--text-on-primary-container` when selected, `--text-primary`/`--text-secondary` otherwise. Catalog badge uses `--bg-branded`/`--text-branded`. |

### Edge Function Deployment Required

```bash
supabase functions deploy parse-food        # expanded ambiguity prompt
supabase functions deploy lookup-nutrition   # brand validation fix
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W45 | Three-case ambiguity detection (A/B/C) over single-case | Case A alone missed two common scenarios: bare generic names ("cheese") and specific foods without amounts ("cheddar cheese"). Both produce meaningless default `1 serving` lookups. Case B+C catch these with weight-focused hints. |
| W46 | Brand validation via result field matching, not query restructuring | FDC's keyword search cannot be constrained to an exact brand. Post-hoc validation (checking the returned `brand` field) is simple, reliable, and degrades gracefully to generic when the brand isn't in FDC. |
| W47 | Semantic dark-mode CSS variables over hardcoded colors | Same pattern as Android's `MaterialTheme.colorScheme` — define role-based tokens (`--bg-warning`, `--bg-primary-container`) that swap values in `.dark`. Components reference one name; the theme handles the rest. |

---

## Phase W11: Edit Log Macro Scaling Guard — Gram ↔ Non-Gram Boundary

**Status:** ✅ Completed (symptomatic fix — architectural fix planned in per-100g normalization)
**Date:** June 2, 2026

### Summary

Added a guard to the `EditLogSheet` proportional scaling logic that skips auto-recalculation when the unit type crosses the gram ↔ non-gram boundary. Port of Android Phase 18 fix.

The webapp's `edit-log-sheet.tsx` already had proportional scaling (ratio-based: `newMultiplier / orig.consumedQty`), but suffered the same fundamental issue as Android: the original log's `consumedQty` and `total_*` values are in the original unit system. Switching from "1 serving" to "150g" would compute `fromDisplayQty(150, "g") = 1.5`, then `ratio = 1.5 / 1.0 = 1.5`, producing `321 × 1.5 = 481 kcal` instead of the expected 241 kcal (150g of a 321 kcal per 200g item).

**Fix:** Skip scaling when `isGramUnit(newUnit) !== isGramUnit(orig.consumedUnit)`. The user must adjust macros manually after a unit-type change. This guard will be removed after per-100g macro normalization.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/components/edit-log-sheet.tsx` | Updated | `scaleMacros()` function: Added `isGramUnit(resolvedUnit) !== isGramUnit(orig.consumedUnit)` guard at line 79. When unit types differ, returns early — macro fields remain at their current values. User can manually adjust. |

### Key Implementation Details

**Guard in `scaleMacros()`:**
```tsx
const scaleMacros = (displayQty: number, resolvedUnit: string) => {
  const orig = originalLogRef.current;
  if (!orig || orig.consumedQty <= 0 || displayQty <= 0) return;

  // Don't auto-scale when switching between gram ↔ non-gram unit types.
  if (isGramUnit(resolvedUnit) !== isGramUnit(orig.consumedUnit)) return;

  const newMultiplier = fromDisplayQty(displayQty, resolvedUnit);
  const ratio = newMultiplier / orig.consumedQty;
  // ... scale all macros by ratio
};
```

### Cross-Platform Parity

| Platform | File | Guard |
|----------|------|-------|
| Android | `HomeViewModel.kt` `updateEditQty()` / `updateEditUnit()` | `UnitConverter.isGramsUnit(unit) != UnitConverter.isGramsUnit(originalUnit)` |
| Webapp | `edit-log-sheet.tsx` `scaleMacros()` | `isGramUnit(resolvedUnit) !== isGramUnit(orig.consumedUnit)` |

Both guards are symptomatic fixes that will be removed after per-100g macro normalization.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W52 | Skip auto-scaling on gram ↔ non-gram unit type change | Same as Android Phase 18 decision #77. Ratio-based scaling breaks when the original log was in a different unit system because `consumedQty` semantics change (servings vs 100g-relative multiplier). Skipping is safe — manual macro editing is still available. |

---

## Feature 1: Internet Recommendations Infrastructure

### Phase W-R4: User Profile Setup — Webapp

**Status:** ✅ Completed
**Date:** June 2, 2026

### Summary

Full user profile setup for AI-powered meal recommendations on the webapp. Users configure dietary preferences (diet type, cuisines, allergies, weight goal, age, gender, weight) via a settings card. Profile is persisted to Supabase `user_preferences` table and read by the `recommend-meals` edge function when `includeInternet=true`.

Includes a critical race condition fix: saving profile data caused it to visually disappear due to TanStack Query cache invalidation interleaving with React state re-seeding.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/lib/types/domain.ts` | Updated | Added `UserProfile` interface with 8 profile fields. Extended `UserPreferences` interface with same fields. |
| 2 | `webapp/lib/types/database.ts` | Updated | Extended `user_preferences` Row/Insert/Update types with `age`, `gender`, `weight_kg`, `weight_goal`, `diet_type`, `cuisine_preferences: string[]`, `allergies: string[]`, `recommendations_enabled`. |
| 3 | `webapp/lib/hooks/use-user-profile.ts` | **New** | `useUserProfile()` — TanStack Query hook fetching profile columns from `user_preferences`. `useUpdateProfile()` — mutation using `.upsert()` with `onConflict: "user_id"`, invalidates `user-profile` and `recommendations` query keys on success. |
| 4 | `webapp/app/settings/profile-section.tsx` | **New** | Profile settings card with: enable toggle, age/weight inputs, gender/diet type selects, weight goal radio chips, cuisine/allergy multi-select chip grids with custom text input. Client-side `sanitizeEntry()` for prompt injection defense. Fingerprint-based re-seed guard to prevent race conditions. |
| 5 | `webapp/app/settings/page.tsx` | Updated | Added `<ProfileSection />` between Daily Goals card and Appearance card. |
| 6 | `supabase/functions/recommend-meals/index.ts` | Updated | Added `sanitizeProfileEntry()` — server-side authoritative sanitization applied to cuisine and allergy arrays before prompt injection. |

### Key Implementation Details

**Race condition fix — fingerprint-based re-seed guard:**

The original implementation used a boolean `initialized` state that was set to `false` in `onSuccess` to trigger re-seeding from the server after save. The problem: `invalidateQueries` is async, so the `useEffect` ran immediately with the stale cached `profile`, overwriting the just-saved values.

Fix: Replace the boolean with a `useRef` fingerprint (JSON.stringify of the profile). On save, optimistically set the fingerprint to match the saved data. When the async refetch returns, the effect sees a matching fingerprint and skips re-seeding.

```tsx
const seededFromRef = useRef<string | null>(null);

useEffect(() => {
  if (!profile) return;
  const fingerprint = JSON.stringify(profile);
  if (seededFromRef.current === fingerprint) return;
  seededFromRef.current = fingerprint;
  // ... seed all form fields from profile
}, [profile]);

const handleSave = () => {
  const updatedProfile: UserProfile = { /* ... build from local state */ };
  // Optimistically set fingerprint to prevent stale re-seed
  seededFromRef.current = JSON.stringify(updatedProfile);
  updateProfile.mutate(updatedProfile);
};
```

**Custom cuisine/allergy input with sanitization:**
```tsx
function sanitizeEntry(raw: string): string {
  return raw
    .replace(/<[^>]*>/g, "")
    .replace(/[#*_~`\[\]{}()|\\]/g, "")
    .replace(/[\x00-\x1F\x7F]/g, "")
    .trim()
    .slice(0, 40);
}
```

### Prompt Injection Defense (3-Layer)

Custom entries are user-provided free text that flows into the AI recommendation prompt:

| Layer | Location | Function | Purpose |
|-------|----------|----------|---------|
| 1 (Authoritative) | `supabase/functions/recommend-meals/index.ts` | `sanitizeProfileEntry()` | Server-side — strips HTML, markdown, control chars, truncates to 40 chars. |
| 2 | `webapp/app/settings/profile-section.tsx` | `sanitizeEntry()` + `maxLength={40}` | Webapp client — identical logic, pre-save. |
| 3 | Android `ProfileSetupSheet.kt` | `sanitizeEntry()` + `MAX_ENTRY_LENGTH = 40` | Android client — identical logic, pre-save. |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W48 | Fingerprint-based re-seed guard over boolean `initialized` flag | A boolean reset in `onSuccess` triggers the `useEffect` immediately with stale TanStack Query cache data before the async refetch completes. A JSON.stringify fingerprint stored in a `useRef` prevents re-seeding when the profile hasn't actually changed from the server's perspective. Optimistic fingerprint update on save blocks the stale interleave window. |
| W49 | `useRef` over `useState` for seed fingerprint | `useRef` doesn't trigger re-renders. The fingerprint is an internal guard, not a rendered value — using `useState` would cause unnecessary re-renders on every profile fetch. |
| W50 | 3-layer sanitization for custom profile entries | Server-side sanitization is authoritative. Client-side sanitization provides immediate UX feedback (no round-trip to discover truncation). Identical regex across all three layers ensures consistent behavior regardless of entry point. |
| W51 | `onConflict: "user_id"` upsert for profile save | The webapp doesn't have a local database — it writes directly to Supabase. Upsert with conflict on `user_id` ensures idempotent saves whether the row exists or not. Combined with column-level update (only profile fields, not macro goal fields) to prevent wiping goals. |

### Edge Function Deployment Required

```bash
supabase functions deploy recommend-meals  # sanitizeProfileEntry + profile-aware prompts
```

---

## Critical Parity Notes (from Android)

These three issues from the Android DEVLOG must be addressed from day one:

### 1. Macro Recalculation Formula
**Correct:** `total = base_macro * consumed_qty`
**Wrong:** `total = base_macro * consumed_qty / base_serving_g`
Base macros are per-serving. `consumed_qty` is number of servings. No division.

### 2. No `local_user` Mapping
Web always uses `(await supabase.auth.getUser()).data.user.id` — the real UUID.
Catalog IDs: `{uuid}_local_user_ingredients`, `{uuid}_local_user_recipes`.

### 3. Tombstone / Soft-Delete Handling
Every query must include `.is("deleted_at", null)`.
Soft-delete: set `deleted_at = Date.now()` + `last_modified_at = Date.now()`.
Never hard-delete — let pg_cron handle it after 15 days.

---

## Phase W12: Catalog Re-Log Macro Fix — Per-100g De-Normalization

**Status:** ✅ Completed
**Date:** June 4, 2026

### Summary

Fixed incorrect daily log totals when re-logging a catalog item whose `base_serving_g ≠ 100` (e.g., recipes created via the manual recipe builder). The `log-food` Edge Function expects **per-serving** macros, but catalog items store **per-100g** macros. Both webapp save paths were sending per-100g values without de-normalizing, causing the Edge Function to double-normalize.

**Symptom:** A recipe with 365 kcal per 400g serving (stored as `base_calories = 91.25` per-100g, `base_serving_g = 400`) would log as 91 kcal instead of 365 kcal for 1 serving.

**Root cause:** The Edge Function normalizes incoming macros via `normMacro = rawMacro × (100 / servingG)`. When per-100g values are sent with `servingG = 400`, double-normalization occurs: `91.25 × (100/400) = 22.8`, then `22.8 × scaleFactor(4.0) = 91.25` — producing the per-100g value instead of the per-serving total.

**Fix:** De-normalize catalog macros before sending to the Edge Function: `perServing = per100g × (servingG / 100)`. This mirrors the existing correct logic in Android's `acceptAndLogAllParsed()` (lines 818-821).

### Bug Reproduction

1. Create recipe "Mango Smoothie" via manual recipe builder with ingredients totaling 400g and 365 kcal
2. Recipe stored: `base_serving_g = 400`, `base_calories = 91.25` (per-100g)
3. Later, log "mango smoothie" via AI → catalog match found
4. **Before fix:** Daily log shows 91 kcal (per-100g value, not per-serving)
5. **After fix:** Daily log shows 365 kcal (correct per-serving total)

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/app/log/page.tsx` | Updated | `handleLogAll()`: Added `deNorm = servingG / 100` multiplier for catalog items. Catalog macros (per-100g) are now de-normalized to per-serving before calling the `log-food` Edge Function. Non-catalog items (`nutrition?.caloriesPer100g` with `servingG = 100`) are unaffected (`deNorm = 1.0`). |
| 2 | `webapp/components/manual-input-section.tsx` | Updated | `handleSaveFlat()`: Added `servingG` subscription from the Zustand store. Replaced hardcoded `baseServingG: 100` with `numServingG` from the store. Applied `deNorm` multiplier to convert per-100g form macros to per-serving. For pure manual entry, `servingG` defaults to `"100"` → `deNorm = 1.0` → no change. |

### Key Implementation Details

**De-normalization in `handleLogAll()`:**
```tsx
const servingG = catalogItem?.baseServingG ?? 100;
const deNorm = servingG / 100;

await logFood.mutateAsync({
  baseServingG: servingG,
  baseCalories: (catalogItem.baseCalories ?? 0) * deNorm,
  baseProtein:  (catalogItem.baseProtein ?? 0) * deNorm,
  baseCarbs:    (catalogItem.baseCarbs ?? 0) * deNorm,
  baseFat:      (catalogItem.baseFat ?? 0) * deNorm,
  ...
});
```

**De-normalization in `handleSaveFlat()`:**
```tsx
const numServingG = parseFloat(servingG) || 100;
const deNorm = numServingG / 100;

logFood.mutate({
  baseServingG: numServingG,
  baseCalories: numCal * deNorm,
  baseProtein:  numProtein * deNorm,
  baseCarbs:    numCarbs * deNorm,
  baseFat:      numFat * deNorm,
  ...
});
```

**Affected paths and status:**

| Path | Before | After |
|------|--------|-------|
| `handleLogAll()` (Log All button) | ❌ Sent per-100g to EF expecting per-serving | ✅ Fixed — de-normalizes with `deNorm` |
| `handleSaveFlat()` (Accept → Manual → Log) | ❌ Hardcoded `baseServingG: 100` | ✅ Fixed — uses store's `servingG` + `deNorm` |

**Scope of impact:** Any catalog item where `base_serving_g ≠ 100`, including:
- All recipes created via the recipe builder (`base_serving_g = N × 100`)
- Any food edited in the Catalog screen to change its serving size

### Cross-Platform Parity

| Platform | Path | Fix |
|----------|------|-----|
| Android | `acceptAndLogAllParsed()` (Log All) | Already correct — had de-normalization at lines 818-821 |
| Android | `saveLog()` (Accept → Log) | Fixed in Phase 19 — added `servingG` to `LogUiState`, used `deNorm` |
| Webapp | `handleLogAll()` | Fixed — added `deNorm = servingG / 100` multiplier |
| Webapp | `handleSaveFlat()` | Fixed — reads `servingG` from Zustand store, applies `deNorm` |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W53 | De-normalize catalog macros at the call site, not in the store | The `acceptParsedFood()` Zustand action already sets `servingG` from the catalog item. De-normalizing at the save call site (`handleSaveFlat` / `handleLogAll`) keeps the form fields displaying per-100g values (consistent with the UI labels) while sending per-serving values to the Edge Function. |
| W54 | Use Zustand store's `servingG` in `handleSaveFlat()` instead of hardcoding 100 | The store's `servingG` defaults to `"100"` for manual entry and is only changed by `acceptParsedFood()` from a catalog item. This makes the fix zero-impact for pure manual entry while correctly handling catalog re-logs. |

---

## Phase W13: AI Parse Fixes — Recipe Nutrition, Serving Size Pre-Scaling & Button Loading State

**Status:** ✅ Completed
**Date:** June 4, 2026

### Summary

Three related fixes for the AI Parse flow, addressing recipe nutrition lookups returning 0 kcal, recipe serving sizes being double-counted, and the "Parse with AI" button showing no visual feedback (spinner/disabled state) when clicked.

### Bug 1: Recipe Ingredients Logging 0 kcal

**Symptom:** Parsing "Strawberry Milkshake" via AI showed 165 kcal in the parsed card, but logged as 0 kcal in the daily log.

**Root cause:** `lookupAll()` in `use-nutrition-lookup.ts` only fired nutrition lookups for top-level parsed foods, not for recipe ingredients. When a food had `isRecipe: true`, its ingredients were never looked up, so `nutritionResults[ingredientName]` was always `undefined` at log time.

**Fix:** Added recipe ingredient iteration in `lookupAll()` — when `food.isRecipe && food.ingredients`, loop through each ingredient and trigger individual lookups.

### Bug 2: Recipe Serving Size 200g Instead of 150g

**Symptom:** A recipe with 50g blueberry + 100g yogurt yielded `base_serving_g = 200` instead of the correct 150g. Macros were similarly inflated.

**Root cause:** `handleLogAll()` in `app/log/page.tsx` sent raw `base_serving_g: PER_100G_BASE` (100) for each non-catalog ingredient without pre-scaling by the parsed quantity. The `log-recipe` Edge Function performs a direct sum of `base_*` values, so 100+100=200 instead of the correct 50+100=150.

**Fix:** Applied `computeServingMultiplier()` pre-scaling in `handleLogAll()` for both catalog and nutrition-enriched ingredients. This mirrors Android's `acceptAndLogAllParsed()` (lines 767-778) which already pre-scaled correctly.

### Bug 3: Parse AI Button Not Showing Loading State

**Symptom:** Clicking "Parse with AI" showed no spinner or disabled state — the button appeared unchanged while the AI call was in-flight.

**Root cause (multi-layer):**
1. `setParsedFoods()` in Zustand didn't reset `isParsing: false`, leaving the flag stuck after first successful parse
2. `setIsParsing(true)` was inside the async `mutationFn`, not in a synchronous React event handler batch — the state update could be deferred past the next render
3. Even after moving to `onMutate`, Zustand's `useSyncExternalStore` subscription didn't consistently trigger an immediate re-render

**Fix (3-part):**
1. Added `isParsing: false` to `setParsedFoods()` state update in `log-form-store.ts`
2. Moved `setIsParsing(true)` and `setAiError(null)` from `mutationFn` to `onMutate` callback in `use-parse-food.ts`
3. Used TanStack Query's built-in `parseMutation.isPending` directly in `ai-input-section.tsx` — combined with Zustand's `isParsing` as `isParseLoading = parseMutation.isPending || isParsing`. TanStack Query manages `isPending` via React state internally, guaranteeing synchronous re-render on `mutate()`.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `lib/hooks/use-nutrition-lookup.ts` | Updated | `lookupAll()` now recurses into recipe ingredients: added `isRecipe` and `ingredients` to type signature, inner loop triggers lookup for each ingredient |
| 2 | `app/log/page.tsx` | Updated | `handleLogAll()`: Pre-scales ingredient macros and `base_serving_g` using `computeServingMultiplier()` for both catalog-matched and nutrition-enriched ingredients before sending to `log-recipe` Edge Function |
| 3 | `lib/stores/log-form-store.ts` | Updated | `setParsedFoods()` now includes `isParsing: false` in the state update to ensure parsing state resets on success |
| 4 | `lib/hooks/use-parse-food.ts` | Updated | Moved `setIsParsing(true)` and `setAiError(null)` from inside `mutationFn` to `onMutate` callback for synchronous execution within React event handler batch |
| 5 | `components/ai-input-section.tsx` | Updated | Button uses `isParseLoading = parseMutation.isPending \|\| isParsing` for both `disabled` and `loading` props. "Enter manually instead" link also uses `isParseLoading`. |

### Key Implementation Details

**Recipe ingredient nutrition lookup:**
```typescript
// use-nutrition-lookup.ts — lookupAll()
for (const food of foods) {
  if (food.isRecipe && food.ingredients) {
    for (const ing of food.ingredients) {
      lookupNutrition(ing.name);
    }
  } else {
    lookupNutrition(food.name);
  }
}
```

**Recipe ingredient pre-scaling in handleLogAll():**
```typescript
// Catalog-matched ingredient
const multiplier = computeServingMultiplier(
  ing.quantity, ing.unit, catalogMatch.foodItem.baseServingG
);
return {
  isFromCatalog: true,
  parsedName: ing.name,
  foodItem: {
    ...catalogMatch.foodItem,
    baseServingG: (catalogMatch.foodItem.baseServingG ?? PER_100G_BASE) * multiplier,
    baseCalories: (catalogMatch.foodItem.baseCalories ?? 0) * multiplier,
    // ... protein, carbs, fat similarly scaled
  },
};
```

**Button loading state using TanStack Query:**
```typescript
const parseMutation = useParseFood();
const isParseLoading = parseMutation.isPending || isParsing;

<Button
  onClick={handleParse}
  disabled={isParseLoading || !aiInput.trim() || aiInput.trim().length < 2}
  loading={isParseLoading}
>
  ✨ Parse with AI
</Button>
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W55 | Use `parseMutation.isPending` from TanStack Query for button loading state instead of Zustand `isParsing` | TanStack Query manages `isPending` via React's `useState` internally, guaranteeing synchronous re-render when `mutate()` is called. Zustand's `useSyncExternalStore` subscription doesn't always batch with React's rendering cycle for immediate visual feedback. The Zustand `isParsing` is kept as a fallback via OR (`\|\|`) for other consumers. |
| W56 | Pre-scale ingredient macros client-side before sending to `log-recipe` Edge Function | The Edge Function performs a direct sum of `base_*` values from ingredient matches. Each ingredient must represent its actual contribution (quantity-adjusted), not raw per-100g values. This mirrors Android's `acceptAndLogAllParsed()` lines 767-778. |

---

## Phase W14: Recommendation Cache Type Safety

**Status:** ✅ Completed
**Date:** June 4, 2026

### Summary

Fixed webapp build failure caused by the `recommendation_cache` table missing from the generated Supabase types. Added proper type definitions and fixed type casting for JSONB columns.

**Symptom:** `npm run build` failed with: `Property 'recommendations' does not exist on type 'never'` in `use-cached-recommendations.ts`.

**Root cause:** The `recommendation_cache` table (created by migration `011_recommendation_cache.sql`) was not present in the hand-maintained `database.ts` type file. TypeScript inferred the table access as `never`, causing all property accesses to fail.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `lib/types/database.ts` | Updated | Added full `recommendation_cache` table definition with Row/Insert/Update types matching migration `011_recommendation_cache.sql` |
| 2 | `lib/hooks/use-cached-recommendations.ts` | Updated | Added `Json` import, changed `cached.recommendations as Recommendation[]` to `as unknown as Recommendation[]`, used `as unknown as Json` for JSONB columns in upsert |

### Key Implementation Details

**Type definition added to `database.ts`:**
```typescript
recommendation_cache: {
  Row: {
    user_id: string;
    recommendations: Json;
    generated_at: string;
    expires_at: string;
    model_version: string | null;
    created_at: string;
  };
  Insert: { /* ... */ };
  Update: { /* ... */ };
};
```

**JSONB type casting pattern:**
```typescript
// Reading: Json → domain type (double cast needed because Json doesn't overlap)
const recs = cached.recommendations as unknown as Recommendation[];

// Writing: domain type → Json (same pattern in reverse)
{ recommendations: recs as unknown as Json }
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W57 | Use `as unknown as T` double-cast for Supabase JSONB columns | Supabase's `Json` type (`string \| number \| boolean \| null \| { [key: string]: Json } \| Json[]`) doesn't structurally overlap with domain types like `Recommendation[]`. TypeScript correctly rejects a direct `as` cast. The double-cast via `unknown` is the standard pattern for JSONB columns with known runtime shapes. |

---

## Phase W15: AI Parse + Recommendation Quality Improvements

**Status:** ✅ Completed
**Date:** June 5, 2026

### Summary

Two independent improvements: (1) corrected the nutrition lookup strategy for AI-parsed recipes so only ingredients are looked up (not the recipe name), and (2) overhauled the `recommend-meals` Edge Function to prioritize saved recipes first, then prefer internet suggestions that use existing catalog ingredients.

> ⚠️ **Requires Edge Function redeployment** — changes to `recommend-meals/index.ts` and `_shared/prompts.ts` are server-side and take effect only after `supabase functions deploy recommend-meals`.

---

### Change 1: Recipe Ingredient-Only Nutrition Lookup

**File:** `webapp/lib/hooks/use-nutrition-lookup.ts`

**Problem:** `lookupAll()` was firing a nutrition lookup for the recipe name itself (e.g. "Strawberry Milkshake") in addition to each ingredient. The recipe-name lookup was wasted — `nutritionResults["strawberry milkshake"]` is never used since the `log-recipe` Edge Function computes totals from per-ingredient macros. It could also pollute `nutritionResults` with unrelated data.

**Fix:** The recipe path now exclusively loops over ingredients. The top-level name lookup is skipped when `food.isRecipe` is true.

```typescript
// Before: looked up BOTH recipe name AND each ingredient
for (const food of foods) {
  if (!food.catalogMatch?.isFromCatalog && !food.needsClarification) {
    lookupNutrition(food.name);  // ← fired even for recipes
  }
  if (food.isRecipe && food.ingredients) {
    for (const ing of food.ingredients) { lookupNutrition(ing.name); }
  }
}

// After: recipes go to ingredient loop only, non-recipes look up by name
for (const food of foods) {
  if (food.isRecipe && food.ingredients) {
    for (const ing of food.ingredients) {
      if (!ing.catalogMatch?.isFromCatalog) { lookupNutrition(ing.name); }
    }
  } else if (!food.catalogMatch?.isFromCatalog && !food.needsClarification) {
    lookupNutrition(food.name);
  }
}
```

---

### Change 2: Recommendation Prioritization — Saved Recipes + Ingredient Overlap

**Files:** `supabase/functions/recommend-meals/index.ts`, `supabase/functions/_shared/prompts.ts`

**Problem:** The recommendation engine treated saved recipes and raw ingredients as a single mixed pool. Frequently-logged ingredients (e.g. "oats", "egg") could crowd out complete saved recipes. Internet suggestions also had no preference for recipes that used ingredients the user already had.

#### 2a. Edge Function — Separate Recipe vs Ingredient Ranking

`recommend-meals/index.ts` now:
- Fetches `catalog_id` alongside food item fields
- Splits items into `recipeItems` (recipe catalog) and `ingredientItems` (ingredient catalog)
- Ranks and shuffles each group independently:
  - Top 8 recipes by frequency → shuffle → take 5 for prompt (`savedRecipesForPrompt`)
  - Top 15 ingredients by frequency → shuffle → take 10 for prompt (`availableIngredientsForPrompt`)

```typescript
const recipeItems = allItems.filter(item => item.catalog_id === recipeCatalogId);
const ingredientItems = allItems.filter(item => item.catalog_id === ingredientCatalogId);

const savedRecipesForPrompt = shuffle(
  recipeItems.map(withFreq).sort(byFreqDesc).slice(0, 8)
).slice(0, 5).map(toPromptItem);

const availableIngredientsForPrompt = shuffle(
  ingredientItems.map(withFreq).sort(byFreqDesc).slice(0, 15)
).slice(0, 10).map(toPromptItem);
```

#### 2b. Prompt Builder — Two Labeled Catalog Sections

`buildRecommendationPrompt()` now accepts `savedRecipes` and `availableIngredients` (replacing the single `catalogItems` parameter) and emits them as two distinct labeled sections in the prompt:

```
Saved recipes (user's own catalog — HIGHEST PRIORITY, prefer these first):
[...savedRecipes...]

Available ingredients in user's catalog (prefer internet recipes that use these):
[...availableIngredients...]
```

#### 2c. System Instruction — 4-Tier Ranking

Updated `RECOMMENDATION_SYSTEM_INSTRUCTION` with an explicit priority ordering:

| Tier | Source | Description |
|------|--------|-------------|
| 1 | `catalog` | Saved recipes — complete meals, highest priority |
| 2 | `catalog` | Individual ingredients usable as standalone components |
| 3 | `internet` | New recipes that use available ingredients |
| 4 | `internet` | New recipes with no catalog overlap |

Rule 7 explicitly instructs the model to mention which available ingredients an internet recipe uses in the `reason` field.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/lib/hooks/use-nutrition-lookup.ts` | Updated | `lookupAll()`: recipes skip top-level name lookup; only ingredients are looked up |
| 2 | `supabase/functions/recommend-meals/index.ts` | Updated | Fetches `catalog_id`, splits items into recipes vs ingredients, ranks each group separately, passes both to prompt builder |
| 3 | `supabase/functions/_shared/prompts.ts` | Updated | `buildRecommendationPrompt()`: replaced `catalogItems` param with `savedRecipes` + `availableIngredients`; emits two labeled prompt sections |
| 4 | `supabase/functions/_shared/prompts.ts` | Updated | `RECOMMENDATION_SYSTEM_INSTRUCTION`: 4-tier ranking rules; internet suggestions biased toward available ingredients |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W58 | Skip recipe-name nutrition lookup in `lookupAll()` | The `log-recipe` Edge Function sums `base_*` values from per-ingredient matches — it never reads a recipe-level `nutritionResults` entry. Looking up the recipe name wastes an Edge Function call and can produce a misleading result in the store. |
| W59 | Rank saved recipes and ingredients in separate quota pools | A single merged pool sorted by log frequency would bury infrequently-used saved recipes beneath commonly-logged ingredients (e.g. "oats" logged daily outranks a saved smoothie recipe logged weekly). Separate pools guarantee saved recipes always get representation in the prompt regardless of ingredient frequency. |
| W60 | Pass saved recipes and available ingredients as distinct labeled sections to the LLM | A single unlabeled "catalog" list gave the model no signal about which items are complete meals vs raw ingredients. Separate labeled sections let the system instruction express a clear preference hierarchy: suggest a complete saved recipe before suggesting an internet recipe that happens to use the same ingredients. |

---

## Phase W16: Recommendation Macro Accuracy — Server-Side Recalculation

**Status:** ✅ Completed
**Date:** June 5, 2026

### Summary

Removed AI arithmetic from the recommendation pipeline. For `source="catalog"` items, the AI was multiplying per-serving macros by `suggested_quantity` — floating-point math that belongs in code, not in the LLM. The Edge Function now intercepts the AI's JSON output and recomputes catalog macros deterministically from the DB data already in memory.

> ⚠️ **Requires Edge Function redeployment**: `supabase functions deploy recommend-meals`

### Root Cause Analysis

The `recommend-meals` Edge Function fetched catalog items from the DB (with their stored `base_calories`, `base_protein`, etc.), sent them to the AI, and trusted the AI's returned `calories`/`protein`/`carbs`/`fat` fields verbatim. The system instruction told the AI:

> *"The macros returned MUST reflect suggested_quantity × per-serving macros"*

This means:
- The AI received `{ kcal: 91.25, p: 2.1, c: 18.4, f: 1.2 }` for a saved recipe
- The AI returned `{ calories: 182.5, protein: 4.2, ... }` for `suggested_quantity: 2`
- The client used those AI-computed numbers directly

LLMs are unreliable at arithmetic — minor floating-point deviations, hallucinated values, or rounding errors could silently produce wrong macro totals in the recommendation cards.

**Crucially, all the correct data was already in memory**: `allItems` was fetched from the DB in step 3 of the same request. The fix costs zero extra DB calls.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `supabase/functions/recommend-meals/index.ts` | Updated | Added step 9: build `itemMap` from `allItems`, iterate recommendations, replace AI-provided macros for `source="catalog"` items with `base_* × suggested_quantity` computed in Deno/JS |
| 2 | `supabase/functions/_shared/prompts.ts` | Updated | Rule 5 in `RECOMMENDATION_SYSTEM_INSTRUCTION`: removed "MUST reflect suggested_quantity × per-serving macros" instruction — AI no longer needs to do the math, server handles it |

### Key Implementation

```typescript
// Step 9 — recompute catalog macros from DB data, don't trust AI arithmetic
const itemMap = new Map(allItems.map((item: any) => [item.id, item]));

const recommendations = (parsed.recommendations ?? []).map((rec: any) => {
  if (rec.source === "catalog" && rec.food_item_id) {
    const item = itemMap.get(rec.food_item_id) as any;
    if (item) {
      const qty = (typeof rec.suggested_quantity === "number" && rec.suggested_quantity > 0)
        ? rec.suggested_quantity : 1;
      return {
        ...rec,
        calories: Math.round(item.base_calories * qty * 10) / 10,
        protein:  Math.round(item.base_protein  * qty * 10) / 10,
        carbs:    Math.round(item.base_carbs     * qty * 10) / 10,
        fat:      Math.round(item.base_fat       * qty * 10) / 10,
      };
    }
  }
  return rec; // internet items: keep AI estimate (no DB source of truth)
});
```

### Scope

| Recommendation type | Before | After |
|--------------------|--------|-------|
| `source="catalog"` | AI multiplied `base_* × suggested_quantity` | Code multiplies, rounded to 1dp |
| `source="internet"` | AI estimated from recipe ingredients | Unchanged — no DB lookup possible |

### Cross-Platform Context

Android's AI parse flow was audited and confirmed correct — it never asks the AI for macros:
1. AI returns food names + quantities only
2. `LookupNutritionUseCase` → USDA FDC / IFCT → per-100g values
3. `UnitConverter.computeServingMultiplier()` → Kotlin scales to consumed portion
4. `LogFoodUseCase` normalizes for storage

The recommendation flow had no equivalent server-side interception — this phase adds it.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W61 | Recompute catalog recommendation macros in Edge Function code, not in LLM | The DB-fetched `base_*` values are already in memory (from step 3). Trusting the LLM to multiply them correctly is unnecessary risk — LLMs can produce floating-point errors, hallucinate, or round inconsistently. Code multiplication is deterministic and free (zero extra DB calls). |
| W62 | Keep AI macro estimates for `source="internet"` items | There is no database source of truth for internet recipe suggestions. The AI's estimate based on the recipe_text ingredient list is the only available signal. The estimate is approximate by nature and clearly displayed as such in the UI. |

---

## Phase W17: Ingredient Inline Edit — AI Parsed Mode

**Status:** ✅ Completed
**Date:** June 6, 2026

### Summary

Users can now edit an ingredient's quantity and unit directly inside a recipe card in AI parsed mode, without breaking it out of the recipe. Previously the only option was "Edit Selected" which opened the full manual form, removing the ingredient from its recipe context and preventing it from being logged as part of the dish.

A pencil icon on each ingredient row opens a small `<Dialog>` modal with a quantity number input and a unit `<select>`. Saving patches the ingredient in `parsedFoods` via a new `updateParsedIngredient` Zustand action. When "Log All" fires, the corrected values flow through the existing `handleLogAll` / `acceptAndLogAllParsed` paths automatically — both already read `parsedFoods[i].ingredients[j].quantity` and `.unit`.

Cross-platform parity with Android Phase 21.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/lib/stores/log-form-store.ts` | Updated | Added `parsedFoodsFromParse: boolean` state flag. `setParsedFoods()` sets it `true` (fresh parse). New `updateParsedIngredient(foodIndex, ingIndex, patch)` action patches ingredients immutably and sets flag to `false`. |
| 2 | `webapp/components/parsed-food-card.tsx` | Updated | Added `onEditIngredient?: (ingIndex, current) => void` prop. Each ingredient row gains a `✏️` pencil button that calls the prop with `e.stopPropagation()` to prevent card selection. |
| 3 | `webapp/components/ai-input-section.tsx` | Updated | Added imports for `useState`, `Dialog`, `Input`. Added `editingIngredient` local state. Guarded the `parsedFoods` `useEffect` with `parsedFoodsFromParse` to prevent redundant nutrition lookups on every edit. Added `handleEditIngredient`, `handleSaveEdit`, and cancel handler. Renders `<Dialog>` with qty number input and unit `<select>` when an ingredient is being edited. |

### Key Implementation Details

**`parsedFoodsFromParse` guard prevents re-firing nutrition lookups on edit:**

```typescript
// log-form-store.ts
setParsedFoods: (foods) => set({ parsedFoods: foods, parsedFoodsFromParse: true, ... }),
updateParsedIngredient: (foodIndex, ingIndex, patch) =>
  set((state) => {
    // ... immutable patch ...
    return { parsedFoods: foods, parsedFoodsFromParse: false };
  }),

// ai-input-section.tsx
useEffect(() => {
  if (parsedFoods.length > 0 && parsedFoodsFromParse) {
    lookupAll(parsedFoods);  // only fires on fresh parse, not on edits
  }
}, [parsedFoods]);
```

**Dialog with qty and unit fields:**

```tsx
<Dialog open={true} onClose={() => setEditingIngredient(null)} title={`Edit: ${editingIngredient.name}`}>
  <Input label="Quantity" type="number" value={editingIngredient.quantity} onChange={...} />
  <select value={editingIngredient.unit} onChange={...}>
    {["g", "serving", "tsp", "tbsp", "cup", "ml", "piece", "slice", "bowl"].map(...)}
  </select>
  <button onClick={handleSaveEdit} disabled={!qty || qty <= 0}>Save</button>
  <button onClick={() => setEditingIngredient(null)}>Cancel</button>
</Dialog>
```

### Dependency Risk Addressed

| Risk | Mitigation |
|------|-----------|
| `useEffect` re-fires nutrition lookups on every edit (new `parsedFoods` reference from Zustand immutable update) | `parsedFoodsFromParse` flag — set `true` only by `setParsedFoods`, set `false` by `updateParsedIngredient`. Effect guards on `parsedFoodsFromParse && parsedFoods.length > 0`. |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W63 | `parsedFoodsFromParse` flag over separate `parsedFoodsForEdit` copy | A separate copy would require keeping two arrays in sync. A boolean flag adds minimal state and cleanly separates "parse-triggered" vs "edit-triggered" reference changes without duplicating data. |
| W64 | Edit dialog local state (not Zustand) | The edit dialog is ephemeral — transient form text that doesn't need to survive tab switches or be read by any other component. Local `useState` is simpler and avoids Zustand actions for every keypress. Android uses ViewModel state because the dialog must survive configuration changes; the webapp does not have that constraint. |
| W65 | `e.stopPropagation()` on pencil button click | The ingredient row lives inside the recipe card's `onClick` handler. Without stopping propagation, clicking the pencil would simultaneously select the card, which is distracting. Stopping propagation keeps the edit intent isolated. |

---

## Phase W18: Recipe Card Nutrition Display + Edit Selected Recipe Fix

**Status:** ✅ Completed
**Date:** June 6, 2026

### Summary

Fixed two bugs affecting recipe cards in AI parsed mode:

1. **Recipe card showed "No nutrition data found"** — The card received `nutritionResults["banana smoothie"]` which is always `null` because `lookupAll()` correctly skips recipe-level name lookups (Phase W15). The fix computes a recipe total from per-ingredient nutrition results and displays `~N kcal · Xg P · Yg C · Zg F` below the ingredient list. While ingredient lookups are in-flight it shows "Looking up ingredients…".

2. **"Edit Selected" opened flat Manual form for recipes** — `acceptParsedFood()` had no recipe branch. Accepting a recipe card always opened Manual > Ingredient mode with the recipe title as `foodName`, discarding the ingredient structure. The fix adds a recipe branch: detects `food.isRecipe`, builds `recipeIngredients` from `food.ingredients` pre-filled with nutrition results and catalog matches, and opens Manual > Recipe mode.

Cross-platform parity with Android Phase 22.

### Bug Reproduction

**Bug 1:**
1. Type "banana smoothie (50g banana, 100g milk, 50g yogurt)" → Parse
2. Ingredient lookups complete — per-ingredient badges show data
3. **Before fix:** Recipe card shows "⚪ No nutrition data found"
4. **After fix:** Card shows "~245 kcal · 8.2g P · 42.1g C · 4.3g F"

**Bug 2:**
1. Parse "banana smoothie" → recipe card appears → select card → "✏️ Edit Selected"
2. **Before fix:** Manual form opens flat with "banana smoothie" as food name, Ingredient mode
3. **After fix:** Manual form opens in Recipe mode with all ingredients pre-filled

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/components/parsed-food-card.tsx` | Updated | Added `ingredientNutritionResults?: Record<string, NutritionInfo \| null \| undefined>` and `ingredientNutritionLoading?: Record<string, boolean>` props. Added recipe total computation using `computeServingMultiplier` over catalog items (priority 1) and `NutritionInfo` results (priority 2). Replaced unconditional "No nutrition data found" (which was always shown for recipes) with recipe-aware display: "Looking up ingredients…" → computed total → "No ingredient nutrition found". |
| 2 | `webapp/lib/stores/log-form-store.ts` | Updated | `acceptParsedFood()`: Added recipe branch at the top — when `food.isRecipe`, maps `food.ingredients` to `ManualRecipeIngredient` objects pre-filled from `nutritionResults` (per-100g) or catalog items. Appends a trailing empty row. Sets `inputMode: "manual"`, `isRecipeMode: true`, `recipeName`, `recipeQuantity`, `recipeUnit`, `recipeIngredients` and returns early. Non-recipe path unchanged. |
| 3 | `webapp/components/ai-input-section.tsx` | Updated | Passes `ingredientNutritionResults={food.isRecipe ? nutritionResults : undefined}` and `ingredientNutritionLoading={food.isRecipe ? nutritionLoading : undefined}` to `ParsedFoodCard`. Non-recipe cards receive `undefined` — no change to their existing behaviour. |

### Key Implementation Details

**Recipe total computation in `ParsedFoodCard`:**
```tsx
const anyIngredientLoading = food.isRecipe && food.ingredients.some(
  (ing) => !ing.catalogMatch?.isFromCatalog && (ingredientNutritionLoading?.[ing.name] ?? false)
);
let recipeTotal: { calories: number; protein: number; carbs: number; fat: number } | null = null;
if (food.isRecipe && ingredientNutritionResults) {
  let totalCal = 0, totalProt = 0, totalCarb = 0, totalFat = 0, hasData = false;
  for (const ing of food.ingredients) {
    const cat = ing.catalogMatch?.isFromCatalog ? ing.catalogMatch.foodItem : null;
    const nut = ingredientNutritionResults[ing.name] ?? null;
    if (cat) {
      const m = computeServingMultiplier(ing.quantity, ing.unit, cat.baseServingG);
      totalCal += cat.baseCalories * m; totalProt += cat.baseProtein * m;
      totalCarb += cat.baseCarbs * m; totalFat += cat.baseFat * m;
      hasData = true;
    } else if (nut) {
      const m = computeServingMultiplier(ing.quantity, ing.unit, nut.servingWeightG ?? undefined);
      totalCal += nut.caloriesPer100g * m; totalProt += nut.proteinPer100g * m;
      totalCarb += nut.carbsPer100g * m; totalFat += nut.fatPer100g * m;
      hasData = true;
    }
  }
  if (hasData) recipeTotal = { calories: Math.round(totalCal), protein: Math.round(totalProt * 10) / 10, carbs: Math.round(totalCarb * 10) / 10, fat: Math.round(totalFat * 10) / 10 };
}
```

**Recipe branch in `acceptParsedFood()` (`log-form-store.ts`):**
```tsx
if (food.isRecipe) {
  const recipeIngredients: ManualRecipeIngredient[] = food.ingredients.map((ing) => {
    const nutrition = state.nutritionResults[ing.name] ?? null;
    const catalogItem = ing.catalogMatch?.isFromCatalog ? ing.catalogMatch.foodItem : null;
    return {
      id: Math.random().toString(36).slice(2),
      catalogItem: catalogItem ?? null,
      customName: catalogItem ? catalogItem.name : ing.name,
      quantity: String(ing.quantity),
      unit: ing.unit,
      calories: catalogItem ? String(catalogItem.baseCalories)
        : nutrition ? String(Math.round(nutrition.caloriesPer100g * 10) / 10) : "",
      protein:  catalogItem ? String(catalogItem.baseProtein)
        : nutrition ? String(Math.round(nutrition.proteinPer100g * 10) / 10) : "",
      carbs:    catalogItem ? String(catalogItem.baseCarbs)
        : nutrition ? String(Math.round(nutrition.carbsPer100g * 10) / 10) : "",
      fat:      catalogItem ? String(catalogItem.baseFat)
        : nutrition ? String(Math.round(nutrition.fatPer100g * 10) / 10) : "",
    };
  });
  recipeIngredients.push(createEmptyIngredient()); // trailing empty row for adding more
  set({ inputMode: "manual", isRecipeMode: true, recipeName: food.name,
        recipeQuantity: String(food.quantity), recipeUnit: food.unit, recipeIngredients });
  return;
}
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| W66 | Compute recipe total client-side in `ParsedFoodCard` from `ingredientNutritionResults` | The `log-recipe` Edge Function is the authoritative source for final totals at save time. The card-level display is a preview. Computing it from the same `nutritionResults` store that already holds ingredient data costs zero extra API calls and gives immediate feedback as lookups complete progressively. |
| W67 | Pass `nutritionResults` via props rather than subscribing in `ParsedFoodCard` | `ParsedFoodCard` is a pure display component. Subscribing to Zustand directly inside would couple the card to the store, making it harder to test. `AiInputSection` already owns the subscription and passes the full map as a prop — no architectural change required. |
| W68 | Recipe branch in `acceptParsedFood()` sets `isRecipeMode: true` and pre-fills ingredients | Opening the flat Manual form for a recipe discards the AI-detected ingredient structure. Pre-filling the Recipe builder preserves the breakdown, lets the user adjust per-ingredient quantities before logging, and correctly routes the save through the `log-recipe` Edge Function path. |

---

## Phase W19: Unit Scaling Parity Revisions

**Status:** ✅ Completed
**Date:** June 7, 2026

### Summary

Aligned the case-matching, pluralization, and spelling mappings of the unit multiplier calculations in the Web client and the Supabase Edge Functions with the Android application's `UnitConverter` logic. This resolves calculation discrepancies that occurred when logging pluralized units (e.g., `teaspoons`, `milliliters`) from Android.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/lib/utils/macro-calculator.ts` | Updated | Expanded `computeServingMultiplier` to support plurals and variant spellings for ml, tsp, tbsp, cups, and discrete serving sizes. |
| 2 | `supabase/functions/_shared/macro-calculator.ts` | Updated | Mirrored the exact same updates to the backend shared utility function. |

---

## Phase W20: Environment Configuration Setup Template

**Status:** ✅ Completed
**Date:** June 8, 2026

### Summary

Created `.env.example` and `.env.local` in the `webapp` directory to document and specify the required Supabase environment variables needed to build and run the webapp companion application.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `webapp/.env.example` | Created | Added template defining `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`. |
| 2 | `webapp/.env.local` | Created | Added local environment file with placeholders for Supabase URL and anon API key. |

---

## Phase W21: Developer Tooling — Logs & READMEs Skill

**Status:** ✅ Completed
**Date:** June 8, 2026

### Summary

Created a new agent skill `update-logs-readme` to define a playbook for systematically maintaining, formatting, and keeping development logs and directory READMEs up-to-date across the workspace.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `.agents/skills/update-logs-readme/SKILL.md` | Created | Defined the skill playbook outlining templates, entry structures, and verification check-lists. |

---

*End of Web App Development Log*

