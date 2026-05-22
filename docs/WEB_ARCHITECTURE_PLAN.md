# NutriAI Web App — Technical Architecture & Implementation Plan

> **Author:** Solutions Architect
> **Date:** May 14, 2026
> **Status:** Draft — Ready for Review
> **Scope:** Full web platform for NutriAI, sharing Supabase backend with the existing Android app

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Tech Stack Recommendation](#2-tech-stack-recommendation)
3. [Logic Centralization — Supabase Edge Functions](#3-logic-centralization--supabase-edge-functions)
4. [Data Consolidation — IFCT 2017 Migration](#4-data-consolidation--ifct-2017-migration)
5. [AI Pipeline Mirroring](#5-ai-pipeline-mirroring)
6. [Multimodal Implementation — Label Scanner](#6-multimodal-implementation--label-scanner)
7. [UI/UX Translation — Design System](#7-uiux-translation--design-system)
8. [Sync & Conflict Resolution](#8-sync--conflict-resolution)
9. [Component Map](#9-component-map)
10. [Shared vs. Duplicated Logic Matrix](#10-shared-vs-duplicated-logic-matrix)
11. [Migration Guide — Step by Step](#11-migration-guide--step-by-step)
12. [Critical Bug Parity Notes](#12-critical-bug-parity-notes)

---

## 1. Executive Summary

The NutriAI web app is an **online-first** platform that shares the same Supabase PostgreSQL backend as the existing **offline-first** Android app. The key architectural shift is **logic centralization**: business rules currently embedded in Kotlin on-device (macro scaling, unit conversion, AI prompt construction, nutrition lookup) move into **Supabase Edge Functions (TypeScript/Deno)** so both platforms execute identical logic against the same database.

**Core principles:**
- **Single source of truth:** Supabase PostgreSQL is the authoritative data store for web. No client-side database (no IndexedDB, no local-first).
- **Shared business logic:** Edge Functions handle all writes (log-food, log-recipe, AI parsing) so Android can eventually call the same endpoints, eliminating logic drift.
- **Direct UUID auth:** The web app uses the Supabase user UUID directly — no `"local_user"` mapping layer needed (that's an Android-only concern for offline mode).
- **Conflict safety:** The existing `updated_at` triggers + LWW guard triggers + `deleted_at` tombstones protect against cross-platform overwrites with zero changes needed.

---

## 2. Tech Stack Recommendation

### Framework: **Next.js 14 (App Router)**

| Criteria | Next.js 14 | Remix | Rationale |
|----------|-----------|-------|-----------|
| Supabase Auth | Native via `@supabase/ssr` | Manual cookie management | `@supabase/ssr` provides `createServerClient` + `createBrowserClient` with automatic cookie-based session management for Next.js middleware, Server Components, and Route Handlers |
| Gemini SDK | `@google/generative-ai` works in both Edge Runtime and Node.js | Same | No difference |
| SSR + RSC | React Server Components for dashboard data fetching | Loader pattern | RSC enables zero-JS data fetching for the Home dashboard (macro summary, food logs) — better initial load |
| Edge Functions | Supabase Edge Functions (Deno) handle all business logic | Same | Framework-agnostic — both call the same Edge Functions |
| Ecosystem | Vercel deployment, built-in Image optimization, middleware | Cloudflare/Fly.io | Vercel's Edge Middleware handles auth redirects before page render |
| Community | Larger ecosystem, more Supabase examples | Growing | More battle-tested patterns for Supabase + Next.js |

### Full Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Framework** | Next.js 14 (App Router) | SSR, RSC, API routes, middleware |
| **Language** | TypeScript (strict mode) | Type safety across client + server |
| **Auth** | `@supabase/ssr` + Supabase GoTrue | Cookie-based PKCE auth, middleware session refresh |
| **Data Fetching** | TanStack Query v5 | Client-side cache, optimistic updates, background refetch |
| **Client State** | Zustand | Date navigation, form state, UI mode (AI/Manual) |
| **Styling** | Tailwind CSS v4 | Utility-first, CSS variable theming, dark mode |
| **Font** | `next/font/google` (Outfit) | Self-hosted, zero layout shift, subset optimization |
| **Charts** | Recharts | Macro line/bar/year charts (mirrors Android `MacroLineChart`, `MacroBarChart`, `MacroYearChart`) |
| **Image Processing** | Canvas API (browser) | Client-side label photo compression (1024px, JPEG 80%) |
| **Deployment** | Vercel | Edge-optimized, automatic preview deployments |
| **Business Logic** | Supabase Edge Functions (Deno) | Centralized: macro scaling, AI pipeline, nutrition lookup |

### Project Structure

```
nutriai-web/
├── app/                          # Next.js App Router
│   ├── layout.tsx                # Root layout — theme, font, providers
│   ├── page.tsx                  # Home dashboard (RSC)
│   ├── log/
│   │   └── page.tsx              # Food logging (AI + Manual)
│   ├── catalog/
│   │   └── page.tsx              # Food catalog browser
│   ├── insights/
│   │   └── page.tsx              # Charts + analytics
│   ├── scan/
│   │   └── page.tsx              # Label scanner
│   ├── auth/
│   │   ├── sign-in/page.tsx
│   │   └── sign-up/page.tsx
│   ├── settings/
│   │   └── page.tsx              # Macro goals, preferences
│   └── api/                      # Next.js Route Handlers (thin proxies if needed)
├── components/
│   ├── ui/                       # Primitives: Button, Card, Input, Dialog
│   ├── macro-summary-card.tsx    # Circular progress arcs
│   ├── food-log-item.tsx         # Swipe-to-delete row
│   ├── parsed-food-card.tsx      # AI parse result card
│   ├── nutrition-facts-card.tsx  # Nutrition label display
│   ├── macro-line-chart.tsx      # Weekly/monthly line chart
│   ├── macro-bar-chart.tsx       # Weekly bar chart
│   ├── macro-year-chart.tsx      # Yearly summary chart
│   ├── offline-banner.tsx        # Connectivity status
│   ├── confirm-delete-dialog.tsx # Delete confirmation
│   └── period-selector.tsx       # Date range selector
├── lib/
│   ├── supabase/
│   │   ├── client.ts             # Browser client factory
│   │   ├── server.ts             # Server client factory (cookies)
│   │   └── middleware.ts         # Auth session refresh
│   ├── types/
│   │   ├── database.ts           # Supabase generated types
│   │   ├── domain.ts             # Domain models (DailyLog, FoodItem, etc.)
│   │   └── ai.ts                 # ParsedFood, NutritionInfo, ExtractedLabelData
│   ├── hooks/
│   │   ├── use-daily-logs.ts     # TanStack Query hook
│   │   ├── use-catalog.ts
│   │   ├── use-macro-goals.ts
│   │   └── use-ai-parse.ts
│   ├── stores/
│   │   ├── date-store.ts         # Zustand: selected date, navigation
│   │   └── log-form-store.ts     # Zustand: AI/Manual mode, form fields
│   └── utils/
│       ├── image-compressor.ts   # Canvas-based compression
│       ├── macro-calculator.ts   # Client-side preview only (Edge Function is source of truth)
│       ├── format.ts             # formatMacro(), formatDate() — display utilities
│       └── constants.ts
├── supabase/
│   └── functions/
│       ├── parse-food/index.ts
│       ├── scan-label/index.ts
│       ├── lookup-nutrition/index.ts
│       ├── log-food/index.ts
│       ├── log-recipe/index.ts
│       ├── update-food/index.ts      # Edit catalog item (triggers macro recalc)
│       ├── update-daily-log/index.ts  # Edit log entry (qty, unit, macros)
│       └── _shared/
│           ├── prompts.ts        # GeminiPrompts + GeminiLabelPrompts (ported from Kotlin)
│           ├── macro-calculator.ts  # Canonical macro scaling + unit conversion
│           ├── nutrition-lookup.ts  # USDA FDC + IFCT two-tier chain
│           ├── json-utils.ts     # extractJson() — robust Gemini response parsing
│           └── cors.ts           # CORS headers + OPTIONS handler
├── tailwind.config.ts            # Forest & Cream theme
├── middleware.ts                 # Next.js middleware (auth guard)
└── next.config.ts
```

---

## 3. Logic Centralization — Supabase Edge Functions

### Why Edge Functions?

The Android app has business logic spread across 6 UseCases and 4 Repository implementations. On the web, this logic **must not live in the browser** (it would be duplicated, driftable, and bypassable). Supabase Edge Functions (Deno runtime, TypeScript) provide:

- **Single execution point:** Both web and Android can call the same function
- **Direct DB access:** Uses `supabase.from('table')` with the service role key — no PostgREST round-trip
- **Secrets management:** Gemini API key and USDA FDC key are stored in Supabase Vault, not shipped to the client
- **Row-level context:** The function receives the authenticated user's JWT and can enforce RLS

### Edge Function Inventory

#### 3.1 `parse-food` — AI Entity Extraction

**Port of:** `AiRepositoryImpl.parseFood()` + `GeminiPrompts` + `ResolveCatalogCacheUseCase`

```typescript
// supabase/functions/parse-food/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { SYSTEM_INSTRUCTION, buildUserPrompt } from "../_shared/prompts.ts";
import { extractJson } from "../_shared/json-utils.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  const { foodDescription } = await req.json();
  if (!foodDescription?.trim() || foodDescription.length > 500) {
    return new Response(
      JSON.stringify({ error: "Food description must be 1-500 characters" }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
  const authHeader = req.headers.get("Authorization")!;
  
  // 1. Create authenticated Supabase client
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );
  
  // 2. Get user ID from JWT
  const { data: { user } } = await supabase.auth.getUser();
  const userId = user!.id;
  
  // 3. Fetch existing catalog names for name standardization (Phase 4.6)
  const ingredientCatalogId = `${userId}_local_user_ingredients`;
  const recipeCatalogId = `${userId}_local_user_recipes`;
  
  const [{ data: ingredients }, { data: recipes }] = await Promise.all([
    supabase.from("food_items")
      .select("name")
      .eq("catalog_id", ingredientCatalogId)
      .is("deleted_at", null),
    supabase.from("food_items")
      .select("name")
      .eq("catalog_id", recipeCatalogId)
      .is("deleted_at", null),
  ]);
  
  const existingIngredients = (ingredients ?? []).map(r => r.name);
  const existingRecipes = (recipes ?? []).map(r => r.name);
  
  // 4. Build prompt with catalog context
  const userPrompt = buildUserPrompt(foodDescription, existingIngredients, existingRecipes);
  
  // 5. Call Gemini API (Gemma 4)
  const geminiKey = Deno.env.get("GEMINI_API_KEY")!;
  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/gemma-4-26b-a4b-it:generateContent?key=${geminiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: userPrompt }] }],
        systemInstruction: { role: "user", parts: [{ text: SYSTEM_INSTRUCTION }] },
        generationConfig: {
          temperature: 0.1,
          topP: 0.8,
          topK: 40,
          maxOutputTokens: 1024,
          responseMimeType: "application/json",
          thinkingConfig: { thinkingLevel: "MINIMAL" },
        },
      }),
    }
  );
  
  // 6. Extract JSON from response (filter thought parts)
  const data = await response.json();
  const parts = data.candidates?.[0]?.content?.parts ?? [];
  const contentPart = parts.findLast(
    (p: any) => p.thought !== true && p.text?.trim()
  );
  
  const parsed = extractJson(contentPart?.text ?? '{"foods": []}');
  
  // 7. Resolve catalog cache matches
  const resolvedFoods = await Promise.all(
    parsed.foods.map(async (food: any) => {
      const catalogId = food.is_recipe ? recipeCatalogId : ingredientCatalogId;
      const { data: match } = await supabase
        .from("food_items")
        .select("*")
        .eq("catalog_id", catalogId)
        .ilike("name", food.name)
        .is("deleted_at", null)
        .limit(1)
        .maybeSingle();
      
      return {
        ...food,
        catalogMatch: match ? { isFromCatalog: true, foodItem: match } : null,
        // Resolve ingredients for recipes too
        ingredients: food.is_recipe
          ? await Promise.all(
              (food.ingredients ?? []).map(async (ing: any) => {
                const { data: ingMatch } = await supabase
                  .from("food_items")
                  .select("*")
                  .eq("catalog_id", ingredientCatalogId)
                  .ilike("name", ing.name)
                  .is("deleted_at", null)
                  .limit(1)
                  .maybeSingle();
                return { ...ing, catalogMatch: ingMatch ? { isFromCatalog: true, foodItem: ingMatch } : null };
              })
            )
          : [],
      };
    })
  );
  
  return new Response(JSON.stringify({ foods: resolvedFoods }), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
```

#### 3.2 `lookup-nutrition` — Two-Tier Nutrition Grounding

**Port of:** `NutritionRepositoryImpl` (USDA FDC + IFCT fallback)

```typescript
// supabase/functions/lookup-nutrition/index.ts
// Tier 1: USDA FDC API
// Tier 2: SELECT from ifct_foods table (seeded from CSV migration)
// Tier 3: null (user fills manually)

serve(async (req) => {
  const { foodNames } = await req.json(); // string[]
  const fdcApiKey = Deno.env.get("USDA_FDC_API_KEY") ?? "";
  
  const supabase = createClient(/* ... */);
  
  const results = await Promise.all(
    foodNames.map(async (name: string) => {
      // Tier 1: USDA FDC
      if (fdcApiKey) {
        try {
          const fdcRes = await fetch(
            `https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(name)}&api_key=${fdcApiKey}&dataType=Foundation,SR%20Legacy,Branded&pageSize=5`
          );
          if (fdcRes.ok) {
            const fdcData = await fdcRes.json();
            const ranked = fdcData.foods
              .filter((f: any) => f.foodNutrients?.length > 0)
              .map((f: any) => mapFdcToNutritionInfo(f))
              .filter((n: any) => n.caloriesPer100g > 0)
              .sort((a: any, b: any) => macroScore(b) - macroScore(a));
            if (ranked.length > 0) return { name, nutrition: ranked[0], source: "USDA FDC" };
          }
        } catch { /* fall through to IFCT */ }
      }
      
      // Tier 2: IFCT 2017 (Supabase table)
      const { data: ifctRows } = await supabase
        .from("ifct_foods")
        .select("*")
        .ilike("name", `%${name}%`)
        .limit(5);
      
      if (ifctRows?.length) {
        return { name, nutrition: mapIfctToNutritionInfo(ifctRows[0]), source: "IFCT 2017" };
      }
      
      // Tier 3: word-by-word IFCT fallback
      const words = name.split(/\s+/).filter(w => w.length > 2);
      for (const word of words) {
        const { data: wordRows } = await supabase
          .from("ifct_foods")
          .select("*")
          .ilike("name", `%${word}%`)
          .limit(3);
        if (wordRows?.length) {
          return { name, nutrition: mapIfctToNutritionInfo(wordRows[0]), source: "IFCT 2017" };
        }
      }
      
      return { name, nutrition: null, source: null };
    })
  );
  
  return new Response(JSON.stringify({ results }));
});
```

#### 3.3 `log-food` — Canonical Macro Scaling

**Port of:** `LogFoodUseCase.invoke()`

This is the **critical** function that prevents the macro recalculation bug. The corrected formula from the Android app is:

```
totalCalories = baseCalories * scaleFactor
totalProtein  = baseProtein  * scaleFactor
totalCarbs    = baseCarbs    * scaleFactor
totalFat      = baseFat      * scaleFactor

where scaleFactor = consumedQty (each quantity = 1 serving)
```

**NOT** `base_macro × consumed_qty / base_serving_g` — the base macros are already per-serving, not per-gram.

```typescript
// supabase/functions/log-food/index.ts
serve(async (req) => {
  const {
    foodName, brand, servingG, calories, protein, carbs, fat,
    quantity, unit, dateTimestamp, catalogId, externalApiId,
    existingFoodItemId, skipDailyLog
  } = await req.json();
  
  // Validation
  if (!foodName?.trim()) throw new Error("Food name cannot be blank");
  if (quantity <= 0) throw new Error("Quantity must be greater than zero");
  if (servingG <= 0) throw new Error("Serving size must be greater than zero");
  
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: req.headers.get("Authorization")! } } }
  );
  const { data: { user } } = await supabase.auth.getUser();
  const userId = user!.id;
  const now = Date.now();
  
  // Auto-create catalogs if they don't exist (fresh web sign-ups).
  // Port of InitializeUserUseCase + LogFoodUseCase.resolveCatalogId().
  // Without this, first food log from a new web user hits FK violation
  // on food_items.catalog_id → catalogs.id.
  const resolvedCatalogId = catalogId || `${userId}_local_user_ingredients`;
  const { data: existingCatalog } = await supabase
    .from("catalogs")
    .select("id")
    .eq("id", resolvedCatalogId)
    .maybeSingle();

  if (!existingCatalog) {
    const isRecipe = resolvedCatalogId.endsWith("_local_user_recipes");
    await supabase.from("catalogs").insert({
      id: resolvedCatalogId,
      user_id: userId,
      name: isRecipe ? "My Recipes" : "My Ingredients",
      last_modified_at: now,
      is_synced: true,
    });
  }
  
  let foodItemId: string;
  
  // Check for existing catalog item (reuse, don't duplicate)
  if (existingFoodItemId) {
    const { data: existing } = await supabase
      .from("food_items")
      .select("id")
      .eq("id", existingFoodItemId)
      .is("deleted_at", null)
      .maybeSingle();
    
    if (existing) {
      foodItemId = existing.id;
    } else {
      // Item was purged between parse and log — create new
      foodItemId = crypto.randomUUID();
      await insertFoodItem(supabase, { foodItemId, catalogId, userId, foodName, brand, servingG, calories, protein, carbs, fat, externalApiId, now });
    }
  } else {
    foodItemId = crypto.randomUUID();
    await insertFoodItem(supabase, { foodItemId, catalogId, userId, foodName, brand, servingG, calories, protein, carbs, fat, externalApiId, now });
  }
  
  if (!skipDailyLog) {
    // CRITICAL: Correct macro scaling formula
    // baseCalories is per-serving, scaleFactor = quantity (number of servings)
    const scaleFactor = quantity;
    
    await supabase.from("daily_logs").insert({
      id: crypto.randomUUID(),
      user_id: userId,
      food_item_id: foodItemId,
      food_name: foodName.trim(),
      date_timestamp: dateTimestamp,
      consumed_qty: quantity,
      consumed_unit: unit?.trim() || "serving",
      total_calories: calories * scaleFactor,
      total_protein: protein * scaleFactor,
      total_carbs: carbs * scaleFactor,
      total_fat: fat * scaleFactor,
      last_modified_at: now,
      is_synced: true,        // Web writes are immediately in Supabase
      deleted_at: null,
    });
  }
  
  return new Response(JSON.stringify({ foodItemId }));
});
```

#### 3.4 `log-recipe` — Ingredient Aggregation

**Port of:** `LogFoodUseCase.logRecipe()`

```typescript
// supabase/functions/log-recipe/index.ts
// Aggregates ingredient macros, creates recipe FoodItem in recipes catalog,
// inserts new ingredients to ingredients catalog, creates DailyLog with
// aggregated totals × quantity.
// Same 3-case logic as Kotlin:
//   Case 1: Catalog hit → accumulate macros, don't insert
//   Case 2: Nutrition-enriched but not in catalog → insert to catalog + accumulate
//   Case 3: No data → 0-macro placeholder in catalog
```

#### 3.5 `scan-label` — Multimodal Label Extraction

**Port of:** `AiRepositoryImpl.extractLabel()` + `GeminiLabelPrompts` + `LabelScannerDelegate` conversion logic

```typescript
// supabase/functions/scan-label/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { LABEL_SYSTEM_INSTRUCTION, LABEL_USER_PROMPT } from "../_shared/prompts.ts";
import { extractJson } from "../_shared/json-utils.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

const PER_100G_BASE = 100.0;

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  const { base64, mimeType } = await req.json();

  // 1. Call Gemma 4 Vision endpoint
  const geminiKey = Deno.env.get("GEMINI_API_KEY")!;
  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/gemma-4-26b-a4b-it:generateContent?key=${geminiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{
          role: "user",
          parts: [
            { inlineData: { mimeType, data: base64 } },
            { text: LABEL_USER_PROMPT },
          ],
        }],
        systemInstruction: { role: "user", parts: [{ text: LABEL_SYSTEM_INSTRUCTION }] },
        generationConfig: {
          temperature: 0.1,
          topP: 0.8,
          topK: 40,
          maxOutputTokens: 1024,
          responseMimeType: "application/json",
          thinkingConfig: { thinkingLevel: "MINIMAL" },
        },
      }),
    }
  );

  const data = await response.json();
  const parts = data.candidates?.[0]?.content?.parts ?? [];
  const contentPart = parts.findLast(
    (p: any) => p.thought !== true && p.text?.trim()
  );
  const raw = extractJson(contentPart?.text ?? "{}");

  // 2. CRITICAL: Per-serving → Per-100g conversion
  // Port of LabelScannerDelegate.kt lines 55-77.
  // When serving_weight_g is available, convert per-serving values to per-100g
  // so they match the storage convention used by food_items.base_* columns.
  // Without this conversion, log-food would receive per-serving values where
  // it expects per-100g, producing wrong totals when consumed_qty != 1.
  const hasServingWeight = raw.serving_weight_g != null && raw.serving_weight_g > 0;
  const w = raw.serving_weight_g ?? 1.0;

  const result = {
    // Raw extraction (always returned for display)
    raw_calories_per_serving: raw.calories_per_serving ?? 0,
    raw_protein_g: raw.protein_g ?? 0,
    raw_carbs_g: raw.carbs_g ?? 0,
    raw_fat_g: raw.fat_g ?? 0,
    serving_size_text: raw.serving_size_text ?? null,
    serving_weight_g: raw.serving_weight_g ?? null,

    // Converted values for form pre-fill (what log-food expects)
    calories: hasServingWeight
      ? (raw.calories_per_serving / w) * PER_100G_BASE
      : raw.calories_per_serving ?? 0,
    protein: hasServingWeight
      ? (raw.protein_g / w) * PER_100G_BASE
      : raw.protein_g ?? 0,
    carbs: hasServingWeight
      ? (raw.carbs_g / w) * PER_100G_BASE
      : raw.carbs_g ?? 0,
    fat: hasServingWeight
      ? (raw.fat_g / w) * PER_100G_BASE
      : raw.fat_g ?? 0,

    // Pre-fill hints for the manual form
    suggested_quantity: hasServingWeight ? w : 1,
    suggested_unit: hasServingWeight ? "grams" : "serving",
  };

  return new Response(JSON.stringify(result), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
```

> **Why the conversion matters:** The Android `LabelScannerDelegate` converts
> per-serving label values to per-100g before pre-filling the form. The form's
> macro fields represent `base_calories` etc. which are per-serving values stored
> in `food_items`. When `serving_weight_g` is known (e.g. 55g), the delegate sets
> `quantity = 55, unit = "grams"` and scales macros to per-100g so that
> `computeServingMultiplier(55, "grams")` produces the correct total. Without
> this, a 55g serving with 280 kcal would be stored as 280 kcal base and the
> user eating 55g would get `280 × 55/100 = 154 kcal` — wrong.

#### 3.6 `update-food` — Edit Catalog Item

**Port of:** `UpdateFoodUseCase` + `CatalogViewModel.saveEditedFood()`

The web needs to update existing food items from the Catalog edit sheet.
When base macros change, the `recalculate_logs_on_food_update()` Postgres
trigger (Section 8.6) automatically recalculates all referencing daily logs.

```typescript
// supabase/functions/update-food/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  const { foodItemId, name, brand, servingG, calories, protein, carbs, fat } =
    await req.json();
  const authHeader = req.headers.get("Authorization")!;

  // Validation (mirrors UpdateFoodUseCase)
  if (!name?.trim()) {
    return new Response(
      JSON.stringify({ error: "Food name cannot be blank" }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
  if (!servingG || servingG <= 0) {
    return new Response(
      JSON.stringify({ error: "Serving size must be positive" }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );

  const now = Date.now();

  const { error } = await supabase
    .from("food_items")
    .update({
      name: name.trim(),
      brand: brand?.trim() || null,
      base_serving_g: servingG,
      base_calories: calories ?? 0,
      base_protein: protein ?? 0,
      base_carbs: carbs ?? 0,
      base_fat: fat ?? 0,
      last_modified_at: now,
      is_synced: true,
    })
    .eq("id", foodItemId)
    .is("deleted_at", null);

  if (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  // The Postgres trigger `trg_food_items_recalculate` automatically
  // recalculates all daily_logs referencing this food item when base
  // macros change — no application-level recalculation needed on web.

  return new Response(JSON.stringify({ success: true }), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
```

#### 3.7 `update-daily-log` — Edit Log Entry

**Port of:** `UpdateDailyLogUseCase` + `HomeViewModel.saveEditedLog()`

Updates an existing daily log's quantity, unit, and macro totals from the
Home screen's edit sheet. The user directly edits the total macro values
(not recalculated from base macros), matching Android's `EditLogSheet` behavior.

```typescript
// supabase/functions/update-daily-log/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { corsHeaders, handleCors } from "../_shared/cors.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return handleCors();

  const { logId, quantity, unit, calories, protein, carbs, fat } =
    await req.json();
  const authHeader = req.headers.get("Authorization")!;

  // Validation (mirrors UpdateDailyLogUseCase)
  if (!quantity || quantity <= 0) {
    return new Response(
      JSON.stringify({ error: "Quantity must be positive" }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );

  const now = Date.now();

  const { error } = await supabase
    .from("daily_logs")
    .update({
      consumed_qty: quantity,
      consumed_unit: unit?.trim() || "serving",
      total_calories: calories ?? 0,
      total_protein: protein ?? 0,
      total_carbs: carbs ?? 0,
      total_fat: fat ?? 0,
      last_modified_at: now,
      is_synced: true,
    })
    .eq("id", logId)
    .is("deleted_at", null);

  if (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }

  return new Response(JSON.stringify({ success: true }), {
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
});
```

### Shared Module: `_shared/prompts.ts`

Direct TypeScript port of `GeminiPrompts.kt` and `GeminiLabelPrompts.kt`:

```typescript
// supabase/functions/_shared/prompts.ts
export const SYSTEM_INSTRUCTION = `
You are a food entry parser for a nutrition tracking app.
// ... exact copy of GeminiPrompts.SYSTEM_INSTRUCTION ...
`;

export function buildUserPrompt(
  foodDescription: string,
  existingIngredients: string[] = [],
  existingRecipes: string[] = [],
): string {
  const ingredientsSection = existingIngredients.length > 0
    ? `\nEXISTING INGREDIENTS (use exact name if semantically matched):\n${existingIngredients.join(", ")}\n`
    : "";
  const recipesSection = existingRecipes.length > 0
    ? `\nEXISTING RECIPES (use exact name if semantically matched):\n${existingRecipes.join(", ")}\n`
    : "";

  return `Parse the following food entry and extract each food item.
${ingredientsSection}${recipesSection}
Respond with ONLY a JSON object in this exact schema:
{
  "foods": [
    {
      "name": "string",
      "quantity": number,
      "unit": "string",
      "confidence": number,
      "is_recipe": boolean,
      "ingredients": [
        {"name": "string", "quantity": number, "unit": "string", "confidence": number}
      ]
    }
  ]
}

When is_recipe is false, set ingredients to an empty array [].
When is_recipe is true, list all component ingredients in the ingredients array.

Food entry: "${foodDescription}"`;
}

export const LABEL_SYSTEM_INSTRUCTION = `
You are a nutrition label reader for a nutrition tracking app.
// ... exact copy of GeminiLabelPrompts.LABEL_SYSTEM_INSTRUCTION ...
`;

export const LABEL_USER_PROMPT = `
Please extract the nutrition facts from this label image.
// ... exact copy of GeminiLabelPrompts.LABEL_USER_PROMPT ...
`;
```

### Shared Module: `_shared/macro-calculator.ts`

```typescript
// supabase/functions/_shared/macro-calculator.ts

/** Per-100g base constant (food-label convention) */
export const PER_100G_BASE = 100.0;

/**
 * Converts quantity + unit to a multiplier against 100g baseline.
 * Direct port of LogViewModel.computeServingMultiplier() from Android.
 */
export function computeServingMultiplier(quantity: number, unit: string): number {
  const normalizedUnit = unit.toLowerCase().trim();
  switch (normalizedUnit) {
    case "g":
    case "gram":
    case "grams":
    case "ml":
    case "milliliter":
      return quantity / PER_100G_BASE;
    case "tsp":
    case "teaspoon":
      return (quantity * 5) / PER_100G_BASE;
    case "tbsp":
    case "tablespoon":
      return (quantity * 15) / PER_100G_BASE;
    case "cup":
    case "cups":
      return (quantity * 240) / PER_100G_BASE;
    default:
      return quantity; // serving, piece, slice, bowl — treated as 1:1
  }
}

/**
 * Scales per-100g nutrition values to the consumed portion.
 * Used when grounding AI-parsed items with USDA FDC / IFCT data.
 */
export function scaleNutrition(
  per100g: { calories: number; protein: number; carbs: number; fat: number },
  multiplier: number
): { calories: number; protein: number; carbs: number; fat: number } {
  return {
    calories: Math.round(per100g.calories * multiplier * 10) / 10,
    protein: Math.round(per100g.protein * multiplier * 10) / 10,
    carbs: Math.round(per100g.carbs * multiplier * 10) / 10,
    fat: Math.round(per100g.fat * multiplier * 10) / 10,
  };
}

/**
 * Computes daily log totals from base macros and consumed quantity.
 *
 * CRITICAL: base macros are PER-SERVING (not per-gram).
 * scaleFactor = consumedQty (number of servings).
 *
 * This is the corrected formula — the Android app had a bug where
 * sync recalculation used base_macro × consumed_qty / base_serving_g
 * which is WRONG. The fix (Phase 8 Pre-work II bugfix #1) confirmed:
 *   total = base_macro × consumed_qty
 */
export function computeDailyLogTotals(
  baseMacros: { calories: number; protein: number; carbs: number; fat: number },
  consumedQty: number
) {
  return {
    totalCalories: baseMacros.calories * consumedQty,
    totalProtein: baseMacros.protein * consumedQty,
    totalCarbs: baseMacros.carbs * consumedQty,
    totalFat: baseMacros.fat * consumedQty,
  };
}
```

### Client Utility: `lib/utils/format.ts`

Port of `MacroFormatUtils.kt` — used extensively in both edit sheets and
display components for consistent number formatting.

```typescript
// lib/utils/format.ts

/**
 * Formats a macro value for display.
 * Whole numbers drop the decimal: 52.0 → "52"
 * Others show one decimal place: 5.3 → "5.3"
 *
 * Direct port of Double.formatMacro() from MacroFormatUtils.kt.
 */
export function formatMacro(value: number): string {
  return value % 1 === 0 ? value.toFixed(0) : value.toFixed(1);
}

/**
 * Formats a date for the date navigation header.
 * E.g. "May 14, 2026" — matches Android's DateNavigationHeader display.
 */
export function formatHeaderDate(date: Date): string {
  return date.toLocaleDateString("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
  });
}
```

### Shared Module: `_shared/json-utils.ts`

Port of `AiRepositoryImpl.extractJson()` — handles markdown-fenced responses
from Gemma 4, which sometimes wraps JSON in ` ```json ... ``` ` even when
`responseMimeType: "application/json"` is set.

```typescript
// supabase/functions/_shared/json-utils.ts

/**
 * Robustly extracts a JSON object from a Gemini response text.
 *
 * Handles three common response formats:
 *   1. Clean JSON: `{"foods": [...]}`
 *   2. Markdown-fenced: ` ```json\n{"foods": [...]}\n``` `
 *   3. Text with embedded JSON: `Here is the result: {"foods": [...]}`
 *
 * Falls back to an empty object on parse failure.
 * Direct port of AiRepositoryImpl.extractJson() from Kotlin.
 */
export function extractJson(text: string): any {
  const trimmed = text.trim();

  // 1. Try direct parse first (clean JSON)
  try {
    return JSON.parse(trimmed);
  } catch {
    // continue to fallbacks
  }

  // 2. Strip markdown fences: ```json ... ``` or ``` ... ```
  const fenceMatch = trimmed.match(/```(?:json)?\s*\n?([\s\S]*?)\n?\s*```/);
  if (fenceMatch) {
    try {
      return JSON.parse(fenceMatch[1].trim());
    } catch {
      // continue
    }
  }

  // 3. Find first { to last } (embedded JSON in prose)
  const firstBrace = trimmed.indexOf("{");
  const lastBrace = trimmed.lastIndexOf("}");
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    try {
      return JSON.parse(trimmed.substring(firstBrace, lastBrace + 1));
    } catch {
      // continue
    }
  }

  // 4. Fallback — return empty object
  console.warn("[extractJson] Failed to parse response:", trimmed.substring(0, 200));
  return {};
}
```

### Shared Module: `_shared/cors.ts`

```typescript
// supabase/functions/_shared/cors.ts

/**
 * Standard CORS headers for all Edge Functions.
 * The origin should match your deployed web app domain.
 */
export const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": process.env.ALLOWED_ORIGIN ?? "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

/** Pre-flight OPTIONS handler. */
export function handleCors(): Response {
  return new Response("ok", { headers: corsHeaders });
}
```

> **CORS note:** Every Edge Function must return `corsHeaders` in its response
> headers and handle `OPTIONS` preflight requests. The `scan-label`, `update-food`,
> and `update-daily-log` Edge Functions above already include this. The `parse-food`,
> `lookup-nutrition`, `log-food`, and `log-recipe` Edge Functions from earlier
> sections must also be updated to import and use `corsHeaders` + `handleCors`.

---

## 4. Data Consolidation — IFCT 2017 Migration

### Step-by-Step: CSV to Supabase Table

The Android app bundles `ifct2017.csv` (120 rows) as an asset and seeds it into Room's `ifct_foods` table on first launch. For the web, this data must live in Supabase so Edge Functions and direct queries can access it.

#### Step 1: Create the `ifct_foods` table in Supabase

```sql
-- supabase/migrations/003_ifct_foods_table.sql
-- Run in Supabase Dashboard → SQL Editor

CREATE TABLE IF NOT EXISTS public.ifct_foods (
    code    TEXT PRIMARY KEY,
    name    TEXT NOT NULL,
    energy_kcal REAL NOT NULL DEFAULT 0,
    protein_g   REAL NOT NULL DEFAULT 0,
    fat_g       REAL NOT NULL DEFAULT 0,
    carbs_g     REAL NOT NULL DEFAULT 0,
    fiber_g     REAL NOT NULL DEFAULT 0
);

-- No RLS needed — this is a public reference table (read-only for all authenticated users)
ALTER TABLE public.ifct_foods ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can read IFCT data"
    ON public.ifct_foods
    FOR SELECT
    TO authenticated
    USING (true);

-- Full-text search index for name lookups
CREATE INDEX IF NOT EXISTS idx_ifct_foods_name_trgm
    ON public.ifct_foods USING gin (name gin_trgm_ops);

-- Requires pg_trgm extension for fuzzy matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

#### Step 2: Seed the data from CSV

**Option A — SQL INSERT (recommended for 120 rows):**

Generate from the existing `app/src/main/assets/ifct2017.csv`:

```sql
-- supabase/migrations/004_ifct_foods_seed.sql
-- Auto-generated from ifct2017.csv

INSERT INTO public.ifct_foods (code, name, energy_kcal, protein_g, fat_g, carbs_g, fiber_g)
VALUES
    ('G001', 'Rice raw', 345, 7.9, 1.0, 78.2, 0.6),
    ('G002', 'Rice cooked', 130, 2.7, 0.3, 28.1, 0.3),
    ('G003', 'Wheat flour atta', 341, 11.8, 1.5, 69.4, 1.9),
    ('G004', 'Roti chapati', 297, 9.5, 3.7, 54.0, 2.3),
    -- ... (all 120 rows)
ON CONFLICT (code) DO NOTHING;
```

**Option B — Supabase CLI seed script:**

```bash
# Convert CSV to SQL seed file
cd nutriai-web
node scripts/csv-to-sql.js ../app/src/main/assets/ifct2017.csv > supabase/seed.sql
supabase db push
```

#### Step 3: Create `user_preferences` table for cross-platform macro goals

The Android app stores macro goals in DataStore (`UserPreferences.kt`):
`calorie_goal`, `protein_goal`, `carbs_goal`, `fat_goal`. These need a
Supabase table so the web can read/write them and goals sync across platforms.

```sql
-- supabase/migrations/006_user_preferences_table.sql

CREATE TABLE IF NOT EXISTS public.user_preferences (
    user_id      UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    calorie_goal REAL NOT NULL DEFAULT 2000,
    protein_goal REAL NOT NULL DEFAULT 150,
    carbs_goal   REAL NOT NULL DEFAULT 250,
    fat_goal     REAL NOT NULL DEFAULT 65,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auto-update timestamp on changes
CREATE TRIGGER set_user_preferences_updated_at
    BEFORE UPDATE ON public.user_preferences
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- RLS: users can only read/write their own row
ALTER TABLE public.user_preferences ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage their own preferences"
    ON public.user_preferences
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
```

**Web usage:**

```typescript
// lib/hooks/use-macro-goals.ts
// Read: supabase.from("user_preferences").select("*").eq("user_id", userId).maybeSingle()
// Falls back to MacroGoals defaults (2000/150/250/65) if no row exists.
// Write: supabase.from("user_preferences").upsert({ user_id, ...goals })
```

**Android migration (Phase 2):** On first sync after this migration, push
DataStore values to the `user_preferences` table if no row exists. Subsequent
reads can observe the Supabase row via Realtime or periodic pull, keeping
DataStore as a local cache.

#### Step 4: Update Android to use the shared IFCT table (future)

Once the Supabase `ifct_foods` table exists, the Android app can optionally query it via PostgREST instead of the local Room table — but this is **not required** for web launch. The local Room `ifct_foods` table continues to work for offline mode.

#### Step 4: Verify parity

```sql
-- Should return 120
SELECT COUNT(*) FROM public.ifct_foods;

-- Spot check: rice should be ~345 kcal/100g raw, ~130 cooked
SELECT name, energy_kcal FROM public.ifct_foods WHERE name ILIKE '%rice%';
```

---

## 5. AI Pipeline Mirroring

### 5.1 Name Standardization Context Fetch

**Android (Phase 4.6):** `AiRepositoryImpl` calls `foodRepository.getAllFoodNames(INGREDIENT_CATALOG_ID)` and `getAllFoodNames(RECIPE_CATALOG_ID)` before every parse.

**Web:** The `parse-food` Edge Function does the same via Supabase client:

```typescript
// In parse-food Edge Function (see Section 3.1)
const userId = user.id;
const ingredientCatalogId = `${userId}_local_user_ingredients`;
const recipeCatalogId = `${userId}_local_user_recipes`;

const [{ data: ingredients }, { data: recipes }] = await Promise.all([
  supabase.from("food_items").select("name")
    .eq("catalog_id", ingredientCatalogId)
    .is("deleted_at", null),
  supabase.from("food_items").select("name")
    .eq("catalog_id", recipeCatalogId)
    .is("deleted_at", null),
]);
```

**Key difference:** The web uses the **prefixed** catalog IDs (`{uuid}_local_user_ingredients`) because it talks to Supabase directly. The Android app uses the local IDs and translates during sync.

### 5.2 Recipe Detection Flow

Identical to Android — the Gemma 4 prompt handles recipe detection server-side. The web flow is:

```
User types "Dosa and Chutney: ingredients - 100g sooji, 50g yogurt"
    │
    ▼
[Client] POST /functions/v1/parse-food { foodDescription: "..." }
    │
    ▼
[Edge Function: parse-food]
    ├── Fetch existing catalog names (name standardization)
    ├── Build prompt with RECIPE DETECTION RULES
    ├── Call Gemma 4 API
    ├── Parse JSON response (filter thought parts)
    ├── Resolve catalog cache for each food + ingredient
    └── Return: { foods: [{ name: "Dosa and Chutney", is_recipe: true, ingredients: [...], catalogMatch: ... }] }
    │
    ▼
[Client] Display recipe card with ingredient rows + catalog badges
    │
    ▼
User taps "Log All"
    │
    ▼
[Client] POST /functions/v1/log-recipe { recipeName, ingredientMatches, quantity, unit, dateTimestamp }
    │
    ▼
[Edge Function: log-recipe]
    ├── Aggregate ingredient macros (3-case: catalog hit / enriched / placeholder)
    ├── Insert new ingredients to ingredients catalog
    ├── Insert recipe FoodItem to recipes catalog
    └── Insert DailyLog with aggregated totals × quantity
```

### 5.3 Nutrition Grounding Flow

```
After parse-food returns parsed items
    │
    ▼
[Client] POST /functions/v1/lookup-nutrition { foodNames: ["sooji", "yogurt", "chutney powder"] }
    │  (skip items with catalogMatch.isFromCatalog === true)
    │
    ▼
[Edge Function: lookup-nutrition]
    ├── For each name, concurrently:
    │   ├── Tier 1: USDA FDC API search → rank by macro completeness
    │   ├── Tier 2: Supabase ifct_foods ILIKE search → word-by-word fallback
    │   └── Tier 3: null
    └── Return: { results: [{ name, nutrition: { caloriesPer100g, ... }, source }] }
    │
    ▼
[Client] Display nutrition badges (loading → found/not found)
         Scale per-100g values using computeServingMultiplier()
         Pre-fill macros in edit form
```

#### 5.3.1 Progressive Nutrition Lookup UX Decision

The Android `NutritionLookupDelegate` updates each food item's nutrition
status independently — items resolve visually one by one (Loading → Found /
Not Found) as each parallel `async` lookup completes. The `lookup-nutrition`
Edge Function returns all results in a single response, which means a naive
web implementation would show a single loading spinner for all items.

**Recommended approach: Parallel client calls (Option C)**

```typescript
// lib/hooks/use-nutrition-lookup.ts
// For each parsed food item that has no catalog match, fire an independent
// TanStack Query mutation. Each lookup resolves its own loading state.
const lookups = parsedFoods
  .filter((f) => !f.catalogMatch)
  .map((f) =>
    useMutation({
      mutationKey: ["nutrition-lookup", f.name],
      mutationFn: () =>
        supabase.functions.invoke("lookup-nutrition", {
          body: { foodNames: [f.name] },
        }),
    })
  );
```

This preserves the progressive UX (each badge flips independently), uses
TanStack Query's built-in loading/error states per item, and keeps the
Edge Function unchanged. The overhead is minimal (3-5 parallel HTTP calls
vs. 1 batched call) and the perceived performance is better because faster
lookups render immediately.

**Alternative (Option A):** Keep the single batched call and accept a
single loading spinner. Simpler implementation, slightly worse UX.
Choose this if network round-trip overhead is a concern on high-latency
connections.

---

## 6. Multimodal Implementation — Label Scanner

### Client-Side Image Compression

The Android app uses `ImageCompressor.kt` with `MAX_DIMENSION = 1024` and `JPEG_QUALITY = 80`. The web replicates this exactly using the Canvas API:

```typescript
// lib/utils/image-compressor.ts

const MAX_DIMENSION = 1024;
const JPEG_QUALITY = 0.80;

export async function compressLabelImage(file: File): Promise<{
  base64: string;
  mimeType: "image/jpeg";
}> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      // 1. Calculate scaled dimensions (preserve aspect ratio)
      let { width, height } = img;
      if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
        const scale = MAX_DIMENSION / Math.max(width, height);
        width = Math.round(width * scale);
        height = Math.round(height * scale);
      }

      // 2. Draw to canvas at target size
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d")!;
      ctx.drawImage(img, 0, 0, width, height);

      // 3. Export as JPEG at 80% quality
      canvas.toBlob(
        (blob) => {
          if (!blob) return reject(new Error("Canvas compression failed"));
          const reader = new FileReader();
          reader.onloadend = () => {
            // Strip data URI prefix to get raw base64
            const base64 = (reader.result as string).split(",")[1];
            resolve({ base64, mimeType: "image/jpeg" });
          };
          reader.readAsDataURL(blob);
        },
        "image/jpeg",
        JPEG_QUALITY
      );
    };
    img.onerror = () => reject(new Error("Failed to load image"));
    img.src = URL.createObjectURL(file);
  });
}
```

### End-to-End Label Scanner Flow

```
User selects/captures photo via <input type="file" accept="image/*" capture="environment">
    │
    ▼
[Client: image-compressor.ts]
    ├── Load into <img>
    ├── Scale to max 1024px (preserve aspect ratio)
    ├── Canvas → JPEG blob at 80% quality (~100-200KB)
    └── FileReader → base64 string (no data URI prefix)
    │
    ▼
[Client] POST /functions/v1/scan-label { base64, mimeType: "image/jpeg" }
    │
    ▼
[Edge Function: scan-label]
    ├── Build multimodal Gemini request:
    │   contents: [{
    │     role: "user",
    │     parts: [
    │       { inlineData: { mimeType: "image/jpeg", data: base64 } },
    │       { text: LABEL_USER_PROMPT }
    │     ]
    │   }]
    │   systemInstruction: LABEL_SYSTEM_INSTRUCTION
    │   generationConfig: { responseMimeType: "application/json", ... }
    │
    ├── Call Gemma 4 Vision endpoint
    ├── Extract JSON (filter thought parts)
    └── Return: { calories_per_serving, protein_g, carbs_g, fat_g, serving_size_text, serving_weight_g }
    │
    ▼
[Client] Pre-fill manual form with extracted values
         User reviews/edits → submits via log-food Edge Function
```

### Web-Specific Considerations

| Aspect | Android | Web |
|--------|---------|-----|
| Image source | CameraX capture or gallery picker | `<input type="file" accept="image/*" capture="environment">` |
| Compression | `BitmapFactory` + `Bitmap.compress()` | Canvas API + `toBlob()` |
| Photo persistence | Room `label_photos` table + filesystem | Not needed — no offline mode; photo lives in memory until submitted |
| TTL cleanup | `CleanupLabelPhotosUseCase` (90-day purge) | Not applicable |

---

## 7. UI/UX Translation — Design System

### 7.1 Tailwind CSS Configuration

```typescript
// tailwind.config.ts
import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      // ─── Colors: Forest & Cream palette ────────────────────────
      colors: {
        // Primary — Forest Green
        primary: {
          DEFAULT: "#2D5A27",
          light: "#A8D99C",
          dark: "#1B3D17",
          container: "#D4E8C8",
          "container-dark": "#3A4F34",
        },
        // Secondary — Lime
        secondary: {
          DEFAULT: "#6B9B37",
          light: "#C0E17E",
          dark: "#3D6B1F",
          container: "#E8F5E1",
          "container-dark": "#2A3D24",
        },
        // Tertiary — Amber
        tertiary: {
          DEFAULT: "#F9A825",
          light: "#FFD95A",
          dark: "#B27A00",
          container: "#FFF3D6",
          "container-dark": "#3D3420",
        },
        // Neutrals — Warm Cream
        cream: "#FEFBF3",
        "cream-surface": "#FFFFFF",
        charcoal: "#1A1C18",
        "mid-green": "#3C4A37",
        "sage-gray": "#EEF2E6",
        "warm-gray": "#C8C8BC",
        // Dark theme surfaces
        "dark-forest": "#0F1A0D",
        "dark-surface": "#1E2A1C",
        "dark-variant": "#2A3828",
        "on-dark": "#E0E8DC",
        // Semantic
        "error-red": "#BA1A1A",
        "error-red-light": "#FFB4AB",
        // Macro colors (data visualization — not theme tokens)
        calorie: "#E8673C",
        protein: "#2E8B7A",
        carbs: "#D4A017",
        fat: "#8E5BA2",
      },

      // ─── Background shortcuts ──────────────────────────────────
      backgroundColor: {
        app: "var(--bg-app)",
        surface: "var(--bg-surface)",
        "surface-variant": "var(--bg-surface-variant)",
      },

      // ─── Border radius: organic rounded corners ────────────────
      borderRadius: {
        xs: "6px",
        sm: "10px",
        md: "16px",
        lg: "24px",
        xl: "32px",
      },

      // ─── Font: Outfit ──────────────────────────────────────────
      fontFamily: {
        outfit: ["var(--font-outfit)", "system-ui", "sans-serif"],
      },

      // ─── Typography scale (matching Android Type.kt) ──────────
      fontSize: {
        "display-lg": ["3.5625rem", { lineHeight: "4rem", letterSpacing: "-0.015625em" }],
        "headline-lg": ["2rem", { lineHeight: "2.5rem", letterSpacing: "-0.03125em" }],
        "headline-md": ["1.75rem", { lineHeight: "2.25rem", letterSpacing: "-0.015625em" }],
        "headline-sm": ["1.5rem", { lineHeight: "2rem", letterSpacing: "0" }],
        "title-lg": ["1.375rem", { lineHeight: "1.75rem", letterSpacing: "0" }],
        "title-md": ["1rem", { lineHeight: "1.5rem", letterSpacing: "0.00625em" }],
        "title-sm": ["0.875rem", { lineHeight: "1.25rem", letterSpacing: "0.00625em" }],
        "body-lg": ["1rem", { lineHeight: "1.5rem", letterSpacing: "0.009375em" }],
        "body-md": ["0.875rem", { lineHeight: "1.25rem", letterSpacing: "0.00625em" }],
        "body-sm": ["0.75rem", { lineHeight: "1rem", letterSpacing: "0.00625em" }],
        "label-lg": ["0.875rem", { lineHeight: "1.25rem", letterSpacing: "0.00625em" }],
        "label-md": ["0.75rem", { lineHeight: "1rem", letterSpacing: "0.01875em" }],
        "label-sm": ["0.6875rem", { lineHeight: "1rem", letterSpacing: "0.01875em" }],
      },
    },
  },
  plugins: [],
};

export default config;
```

### 7.2 CSS Variables for Light/Dark Mode

```css
/* app/globals.css */
@import "tailwindcss";
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600&display=swap');

@layer base {
  :root {
    /* Light theme — Forest & Cream */
    --bg-app: #FEFBF3;
    --bg-surface: #FFFFFF;
    --bg-surface-variant: #EEF2E6;
    --text-primary: #1A1C18;
    --text-secondary: #3C4A37;
    --border-outline: #C8C8BC;
    --border-variant: #EEF2E6;
  }

  .dark {
    /* Dark theme — Dark Forest */
    --bg-app: #0F1A0D;
    --bg-surface: #1E2A1C;
    --bg-surface-variant: #2A3828;
    --text-primary: #E0E8DC;
    --text-secondary: #A8D99C;
    --border-outline: #3C4A37;
    --border-variant: #2A3828;
  }

  body {
    @apply bg-app text-[var(--text-primary)] font-outfit;
  }
}
```

### 7.3 Next.js Font Setup

```typescript
// app/layout.tsx
import { Outfit } from "next/font/google";

const outfit = Outfit({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-outfit",
  display: "swap",
});

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={outfit.variable}>
      <body className="font-outfit bg-cream dark:bg-dark-forest">
        {children}
      </body>
    </html>
  );
}
```

### 7.4 Macro Color Usage in Components

```tsx
// components/macro-summary-card.tsx
// Circular progress arcs matching Android MacroSummaryCard.kt

const MACRO_RING_COLORS = {
  calories: { stroke: "stroke-calorie", text: "text-calorie" },   // #E8673C
  protein:  { stroke: "stroke-protein", text: "text-protein" },    // #2E8B7A
  carbs:    { stroke: "stroke-carbs",   text: "text-carbs" },      // #D4A017
  fat:      { stroke: "stroke-fat",     text: "text-fat" },        // #8E5BA2
} as const;
```

---

## 8. Sync & Conflict Resolution

### 8.1 The Web is Online-First — No `local_user` Mapping

| Concern | Android | Web |
|---------|---------|-----|
| User ID in DB | `"local_user"` → translated to UUID during sync | UUID directly (from Supabase Auth) |
| Catalog IDs | `"local_user_recipes"` (local) → `"{uuid}_local_user_recipes"` (remote) | `"{uuid}_local_user_recipes"` always (direct Supabase writes) |
| `is_synced` flag | `false` on local write → `true` after push | Always `true` (writes go directly to Supabase) |
| Offline queue | Room stores unsynced rows; push-on-write + periodic WorkManager | Not applicable — all writes are online |

### 8.2 How Web Writes Interact with Existing Triggers

The web app writes **directly** to Supabase via Edge Functions (or the Supabase JS client). All existing infrastructure still protects against conflicts:

1. **`updated_at` trigger** — Fires on every INSERT/UPDATE. Sets `updated_at = now()`. The Android app uses this as its incremental pull cursor. Web writes automatically trigger this, so the Android app sees the changes on next sync.

2. **LWW guard trigger (`guard_lww`)** — On UPDATE: if `NEW.last_modified_at < OLD.last_modified_at`, silently keeps the existing row (`RETURN OLD`). The web must set `last_modified_at = Date.now()` on every write. Since the web is online, its clock is accurate — no skew risk.

3. **RLS policies** — All tables have `auth.uid()` scoped policies. The web's Supabase client sends the JWT automatically. No changes needed.

4. **Tombstone purge cron** — Weekly pg_cron job hard-deletes rows where `deleted_at` is > 15 days old. Works identically for web-created tombstones.

### 8.3 Web-Side Delete Handling (Delete-Wins Policy)

When the web app deletes a food item or daily log:

```typescript
// Soft-delete (same as Android)
const now = Date.now();
await supabase
  .from("daily_logs")
  .update({
    deleted_at: now,
    last_modified_at: now,
    is_synced: true,  // already in Supabase
  })
  .eq("id", logId);
```

The Android app's conflict resolution handles this correctly:
- **Pull phase:** Sees `deleted_at != null` → applies tombstone locally
- **Delete-wins rule:** If local row is already deleted, skip remote edit (prevent "zombie un-delete")

### 8.4 Web Reads — Filtering Soft-Deleted Rows

Every web query must exclude tombstones:

```typescript
// lib/supabase/queries.ts
export async function getDailyLogs(supabase: SupabaseClient, date: Date) {
  const startOfDay = new Date(date); startOfDay.setHours(0, 0, 0, 0);
  const endOfDay = new Date(date); endOfDay.setHours(23, 59, 59, 999);

  return supabase
    .from("daily_logs")
    .select("*")
    .eq("user_id", (await supabase.auth.getUser()).data.user!.id)
    .gte("date_timestamp", startOfDay.getTime())
    .lt("date_timestamp", endOfDay.getTime())
    .is("deleted_at", null)  // CRITICAL: exclude soft-deleted rows
    .order("date_timestamp", { ascending: false });
}
```

### 8.5 Preventing Overwrite of Newer Android Data

**Scenario:** User logs food on Android (offline). Before the Android pushes, the web edits the same food item.

**Protection layers:**
1. The Android push carries `last_modified_at` from device clock (set at write time)
2. The web edit also sets `last_modified_at = Date.now()`
3. The LWW guard trigger compares timestamps — the later write wins
4. If the Android push arrives after the web edit, `guard_lww` silently rejects it (returns OLD)
5. Android marks the row as synced (correct — server has the newer version)

**Scenario:** Web deletes a food item. Android has an unsynced edit to the same item.

**Protection:**
1. Web writes `deleted_at = <timestamp>`, `last_modified_at = <timestamp>`
2. Android pushes with `deleted_at = null` and its own `last_modified_at`
3. If Android's `last_modified_at > web's last_modified_at`, the push "wins" and un-deletes
4. This is the **one edge case** where delete-wins is violated by LWW
5. Acceptable: the user explicitly edited on Android after the web delete — their intent was to keep the item

### 8.6 Macro Recalculation on Food Item Update

The `recalculate_logs_for_food()` SQL function (from Phase 8 Pre-work II bugfix) must also be callable from the web. Since the Android app already calls it during pull, and the web writes directly to Supabase, we need a Postgres trigger:

```sql
-- supabase/migrations/005_recalculate_on_food_update.sql
-- Trigger: when a food_item's base macros change, recalculate all daily_logs

CREATE OR REPLACE FUNCTION recalculate_logs_on_food_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Only recalculate if base macros actually changed
    IF OLD.base_calories != NEW.base_calories
       OR OLD.base_protein != NEW.base_protein
       OR OLD.base_carbs != NEW.base_carbs
       OR OLD.base_fat != NEW.base_fat THEN
        
        -- CORRECT FORMULA: total = base_macro × consumed_qty
        -- base macros are PER-SERVING, consumed_qty is number of servings
        UPDATE daily_logs
        SET
            total_calories = NEW.base_calories * consumed_qty,
            total_protein  = NEW.base_protein  * consumed_qty,
            total_carbs    = NEW.base_carbs    * consumed_qty,
            total_fat      = NEW.base_fat      * consumed_qty,
            last_modified_at = EXTRACT(EPOCH FROM now()) * 1000
        WHERE food_item_id = NEW.id
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_food_items_recalculate ON food_items;
CREATE TRIGGER trg_food_items_recalculate
    AFTER UPDATE ON food_items
    FOR EACH ROW EXECUTE FUNCTION recalculate_logs_on_food_update();
```

---

## 9. Component Map

### 9.1 Web Dashboard (Home Screen)

```
┌─────────────────────────────────────────────────┐
│  TopNav: NutriAI logo · Date · Settings icon    │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │  DateNavigationHeader                     │  │
│  │  ← May 14, 2026 →                        │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │  MacroSummaryCard                         │  │
│  │  ┌─────┐  ┌───┐ ┌───┐ ┌───┐             │  │
│  │  │ CAL │  │ P │ │ C │ │ F │  (SVG arcs)  │  │
│  │  │1450 │  │75g│ │180│ │45g│              │  │
│  │  │/2000│  │/  │ │/  │ │/  │              │  │
│  │  └─────┘  └───┘ └───┘ └───┘             │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │  FoodLogList (virtualized)                │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │ FoodLogItem                         │  │  │
│  │  │ 🍳 Scrambled Eggs (2 serving)       │  │  │
│  │  │ 280 kcal · 24g P · 2g C · 20g F    │  │  │
│  │  │                        ✏️ 🗑️         │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │ FoodLogItem                         │  │  │
│  │  │ ...                                 │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  [+ Log Food]  (primary FAB → /log)             │
│                                                 │
├─────────────────────────────────────────────────┤
│  BottomNav: 🏠 Home · 📊 Insights · 📋 Catalog │
└─────────────────────────────────────────────────┘
```

### 9.2 Log Screen

```
┌─────────────────────────────────────────────────┐
│  ← Back to Home                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌───────────────┬───────────────┐              │
│  │ ✨ AI Parse   │ ✏️ Manual     │  (tabs)      │
│  └───────────────┴───────────────┘              │
│                                                 │
│  ── AI Parse Tab ──                             │
│  ┌───────────────────────────────────────────┐  │
│  │  Multi-line text input                    │  │
│  │  "2 eggs, toast with butter, glass of     │  │
│  │   milk"                                   │  │
│  │                                           │  │
│  │  [Parse with AI ✨]  (loading spinner)    │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │  ParsedFoodCard (selectable)              │  │
│  │  ✅ scrambled eggs · 2 serving · 95%      │  │
│  │     In catalog ✅  │  🟢 280 kcal/100g   │  │
│  ├───────────────────────────────────────────┤  │
│  │  ParsedFoodCard                           │  │
│  │  ○ whole wheat toast · 2 slice · 90%      │  │
│  │     New  │  🔍 Loading nutrition...        │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  [Edit Selected] [Log All] [Clear & Retry]      │
│                                                 │
│  ── Manual Tab ──                               │
│  Food name, brand, serving size, qty, unit,     │
│  calories, protein, carbs, fat fields           │
│  MacroPreviewCard (live computation)            │
│  [Save]                                         │
│                                                 │
├─────────────────────────────────────────────────┤
│  ── Scan Tab (Phase 11) ──                      │
│  [📸 Take Photo] [🖼️ Upload Image]             │
│  LabelPreviewCard (extracted values)            │
│  [Accept & Log]                                 │
└─────────────────────────────────────────────────┘
```

### 9.3 Component Hierarchy

```
app/layout.tsx
├── ThemeProvider (dark mode toggle)
├── QueryClientProvider (TanStack Query)
├── SupabaseProvider (auth context)
└── app/page.tsx (Home)
    ├── DateNavigationHeader
    ├── MacroSummaryCard
    │   └── CircularProgressArc (×4: cal, protein, carbs, fat)
    ├── FoodLogList
    │   └── FoodLogItem (×N)
    │       ├── MacroChips (4 colored badges)
    │       ├── EditButton → opens EditLogSheet
    │       └── DeleteButton → opens ConfirmDeleteDialog
    ├── EditLogSheet (dialog/slide-over)              ← port of HomeViewModel.EditLogSheet
    │   ├── FoodNameLabel (read-only, from DailyLog.foodName)
    │   ├── QuantityField (required, positive number validation)
    │   ├── UnitField (text input, default "serving")
    │   ├── CaloriesField, ProteinField, CarbsField, FatField (total macros, editable)
    │   ├── ErrorBanner (validation messages)
    │   ├── SaveButton (loading state) → POST /functions/v1/update-daily-log
    │   └── CancelButton
    ├── ConfirmDeleteDialog (soft-delete)
    ├── OfflineBanner (conditional)
    └── FloatingActionButton → /log

app/log/page.tsx
├── InputModeTabs (AI / Manual / Scan)
├── AiInputSection
│   ├── TextArea
│   ├── ParseButton (with loading spinner)
│   └── ParsedFoodCard (×N)
│       ├── CatalogBadge ("In catalog ✅" / "New")
│       ├── NutritionStatusRow (loading / found / not found)
│       └── IngredientRow (×N, for recipes)
├── ManualInputSection
│   ├── FormFields (name, brand, serving, macros)
│   └── MacroPreviewCard
└── ScanSection (Phase 11)
    ├── ImageUploader (file input + camera)
    ├── ImagePreview (compressed)
    └── LabelResultCard

app/insights/page.tsx
├── PeriodSelector (Week / Month / Year)
├── DailyAverageCard
├── MacroLineChart (Recharts)
├── MacroBarChart (Recharts)
└── MacroYearChart (Recharts)

app/catalog/page.tsx
├── CatalogTabSelector (Recipes / Ingredients)  ← mirrors CatalogTab enum
│   ├── Tab: "Recipes" (catalogId = "{uuid}_local_user_recipes")
│   └── Tab: "Ingredients" (catalogId = "{uuid}_local_user_ingredients")
├── SearchBar (scoped to active tab; cleared on tab switch)
├── CatalogFoodCard (×N)
│   ├── MacroLabels (calories, protein, carbs, fat)
│   ├── BrandLabel (optional)
│   ├── ServingSizeLabel (e.g. "per 100g")
│   ├── EditButton → opens EditFoodSheet
│   └── DeleteButton → opens ConfirmDeleteDialog
├── EditFoodSheet (dialog/slide-over)            ← port of CatalogViewModel.EditFoodSheet
│   ├── NameField (required, non-blank validation)
│   ├── BrandField (optional)
│   ├── ServingGField (required, positive number validation)
│   ├── CaloriesField, ProteinField, CarbsField, FatField
│   ├── ErrorBanner (validation messages)
│   ├── SaveButton (loading state) → POST /functions/v1/update-food
│   └── CancelButton
├── ConfirmDeleteDialog (soft-delete with deleted_at)
└── EmptyCatalogState (per-tab: "No recipes yet" / "No ingredients yet")
```

---

## 10. Shared vs. Duplicated Logic Matrix

| Logic | Location | Shared? | Notes |
|-------|----------|---------|-------|
| **Macro scaling formula** | `supabase/functions/_shared/macro-calculator.ts` | **Shared** (Edge Function) | Both platforms call `log-food` / `log-recipe` Edge Functions. Android continues using local `LogFoodUseCase` for offline writes — **must keep in sync manually** until Android migrates to Edge Functions |
| **Unit conversion** (`computeServingMultiplier`) | `supabase/functions/_shared/macro-calculator.ts` | **Shared** | Same as above |
| **AI prompt templates** | `supabase/functions/_shared/prompts.ts` | **Shared** | Single source of truth. Android can eventually call `parse-food` Edge Function instead of direct Gemini API |
| **Name standardization** | `parse-food` Edge Function | **Shared** | Catalog name fetch + prompt injection happens server-side |
| **Nutrition lookup** (USDA FDC + IFCT) | `lookup-nutrition` Edge Function | **Shared** | Android can also call this instead of local `NutritionRepositoryImpl` |
| **Label extraction prompt** | `supabase/functions/_shared/prompts.ts` | **Shared** | `LABEL_SYSTEM_INSTRUCTION` + `LABEL_USER_PROMPT` |
| **Image compression** | `lib/utils/image-compressor.ts` (web) / `ImageCompressor.kt` (Android) | **Duplicated** | Platform-specific APIs (Canvas vs Bitmap). Same parameters: 1024px, JPEG 80% |
| **Conflict resolution** (LWW, delete-wins) | Postgres triggers (`guard_lww`, `set_updated_at`) | **Shared** | Server-side. Both platforms benefit automatically |
| **Tombstone purge** | pg_cron job (`002_tombstone_purge_cron.sql`) | **Shared** | Runs weekly. Purges both Android and web tombstones |
| **Auth flow** | Supabase GoTrue | **Shared** | Same auth backend. Android uses Retrofit; web uses `@supabase/ssr` |
| **Macro goals storage** | Supabase (new: `user_preferences` table) | **Should centralize** | Currently in Android DataStore. Migrate to Supabase table so goals sync across platforms |
| **`local_user` ID mapping** | `SyncRepositoryImpl.kt` (Android only) | **Android-only** | Web uses UUID directly. This code stays in Android and is never ported |
| **Room database + DAOs** | Android only | **Android-only** | Web has no local database |
| **WorkManager sync** | Android only | **Android-only** | Web is always online |
| **`is_synced` flag management** | Android only | **Android-only** | Web writes are immediately synced (flag = always true) |
| **`recalculate_logs_for_food`** | Postgres trigger (new: `005_recalculate_on_food_update.sql`) | **Shared** | Server-side trigger handles both web edits and Android-pushed edits |
| **`extractJson` response parsing** | `_shared/json-utils.ts` (Edge Functions) / `AiRepositoryImpl.extractJson()` (Android) | **Duplicated** | Platform-specific implementations of the same fallback logic. Must handle identical edge cases (markdown fences, embedded JSON) |
| **`formatMacro` display utility** | `lib/utils/format.ts` (web) / `MacroFormatUtils.kt` (Android) | **Duplicated** | Simple formatting: `52.0 → "52"`, `5.3 → "5.3"`. Same logic, different languages |
| **Update food item** | `update-food` Edge Function (web) / `UpdateFoodUseCase` (Android) | **Partially shared** | Both write to the same `food_items` table. Postgres trigger handles recalculation. Android still writes locally first |
| **Update daily log** | `update-daily-log` Edge Function (web) / `UpdateDailyLogUseCase` (Android) | **Partially shared** | Both write to `daily_logs`. Android writes locally; web writes directly to Supabase |
| **User preferences (macro goals)** | `user_preferences` Supabase table (new: `006_user_preferences_table.sql`) | **Should centralize** | Web reads/writes directly. Android to migrate from DataStore in Phase 2 |

### Migration Path for Android

Phase 1 (web launch): Android continues with on-device logic. Web uses Edge Functions.
Phase 2 (post-web): Android calls Edge Functions when online, falls back to local logic offline.
Phase 3 (long-term): Local logic becomes a thin cache/queue; Edge Functions are the canonical execution path.

---

## 11. Migration Guide — Step by Step

### Phase W1: Foundation (Week 1-2)

1. **Initialize Next.js project**
   ```bash
   npx create-next-app@latest nutriai-web --typescript --tailwind --app --src-dir=false
   ```

2. **Configure Tailwind** with Forest & Cream palette (Section 7.1)

3. **Set up Supabase client**
   - Install `@supabase/ssr`
   - Create `lib/supabase/client.ts` (browser) and `lib/supabase/server.ts` (server)
   - Configure `middleware.ts` for auth session refresh

4. **Generate Supabase types**
   ```bash
   npx supabase gen types typescript --project-id YOUR_PROJECT_ID > lib/types/database.ts
   ```

5. **Run IFCT migration** (Section 4)
   - Execute `003_ifct_foods_table.sql` and `004_ifct_foods_seed.sql` in Supabase Dashboard

6. **Run macro recalculation trigger** (Section 8.6)
   - Execute `005_recalculate_on_food_update.sql`

7. **Run user preferences migration** (Section 4, Step 3)
   - Execute `006_user_preferences_table.sql`

### Phase W2: Auth + Home Dashboard (Week 2-3)

7. **Auth pages** — Sign In / Sign Up using `@supabase/ssr`
8. **Home page** — RSC fetching daily logs for selected date
9. **MacroSummaryCard** — SVG circular arcs with animated CSS transitions
10. **FoodLogList** — Virtualized list with edit/delete actions
11. **ConfirmDeleteDialog** — Soft-delete with `deleted_at` timestamp
12. **DateNavigationHeader** — Zustand store for date state

### Phase W3: AI Pipeline (Week 3-4)

13. **Deploy `parse-food` Edge Function** — Port prompts, extractJson, catalog context fetch
14. **Deploy `lookup-nutrition` Edge Function** — USDA FDC + IFCT chain
15. **Deploy `log-food` Edge Function** — With corrected macro formula + catalog auto-creation
16. **Deploy `log-recipe` Edge Function** — 3-case ingredient handling
17. **Deploy `update-food` Edge Function** — Catalog item editing (triggers macro recalc)
18. **Deploy `update-daily-log` Edge Function** — Log entry editing
19. **Log page** — AI Parse tab + Manual tab + parsed food cards

### Phase W4: Features + Polish (Week 4-5)

20. **Catalog page** — Dual-tab (Recipes/Ingredients), search, browse, EditFoodSheet, delete
21. **Insights page** — Recharts line/bar/year charts
22. **Deploy `scan-label` Edge Function** — Gemma 4 Vision + per-100g conversion
23. **Scan tab** — Client-side image compression + label extraction
24. **Settings page** — Macro goals (read/write `user_preferences` Supabase table)
25. **EditLogSheet** — Home screen log editing modal (qty, unit, macros)
26. **Dark mode toggle** — `class` strategy with Tailwind

### Phase W5: Cross-Platform Testing (Week 5-6)

27. **Conflict testing:** Log food on Android (offline) → edit same item on web → sync Android → verify LWW
28. **Delete testing:** Delete on web → sync Android → verify item stays deleted (delete-wins)
29. **Macro recalculation:** Edit food macros on web (via `update-food`) → verify all daily logs recalculated via Postgres trigger
30. **Label scanner parity:** Scan same label on Android + web → verify identical per-100g conversion results
31. **User preferences sync:** Set macro goals on web → verify Android pulls them from `user_preferences` table (Phase 2)
32. **Tombstone testing:** Delete on both platforms → verify pg_cron purges after 15 days
33. **Auth testing:** Sign in on web + Android simultaneously → verify session isolation

---

## 12. Critical Bug Parity Notes

These three issues from the DEVLOG must be addressed in the web implementation from day one:

### 12.1 Macro Recalculation Formula

**Bug (Android Phase 8 Pre-work II, fix #1):** The sync recalculation SQL used `base_calories * consumed_qty / base_serving_g` which divides by serving size — **wrong**. Base macros are already per-serving.

**Correct formula:**
```
total_calories = base_calories * consumed_qty
total_protein  = base_protein  * consumed_qty
total_carbs    = base_carbs    * consumed_qty
total_fat      = base_fat      * consumed_qty
```

**Web implementation:** The `log-food` Edge Function and the `recalculate_logs_on_food_update()` Postgres trigger both use the corrected formula. The `_shared/macro-calculator.ts` module documents this prominently.

### 12.2 No `local_user` Mapping

The Android app uses `LOCAL_USER_ID = "local_user"` everywhere (DAOs, UseCases, ViewModels) and translates to the real UUID only in `SyncRepositoryImpl`. The web **must not** replicate this pattern.

**Web rule:** Always use `(await supabase.auth.getUser()).data.user.id` — the real UUID. Catalog IDs are always prefixed: `{uuid}_local_user_ingredients`.

### 12.3 Tombstone / Soft-Delete Handling

Every query in the web app must include `.is("deleted_at", null)` to exclude tombstones. The web must also:

- Set `deleted_at = Date.now()` (epoch ms) on soft-delete — matching the Android convention
- Set `last_modified_at = Date.now()` alongside `deleted_at` so LWW resolves correctly
- Never hard-delete rows — let the pg_cron job handle that after 15 days
- On read: if a row has `deleted_at != null`, treat it as if it doesn't exist

---

*End of Technical Architecture Plan*
