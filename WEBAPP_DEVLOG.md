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

*End of Web App Development Log*
