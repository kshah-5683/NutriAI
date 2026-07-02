# NutriAI — Android App Development Log & Change Tracker

> **File:** Renamed to `APP_DEVLOG.md` — please rename this file: `mv DEVLOG.md APP_DEVLOG.md`
> **Project:** AI Nutrition Android Application
> **Tech Stack:** Jetpack Compose · Room · Gemini API · OpenFoodFacts (Phase 5, to be replaced in Phase 5.5) · USDA FDC + IFCT 2017 (Phase 5.5) · Supabase
> **Architecture:** Clean Architecture (Domain / Data / Presentation) with Hilt DI
> **Started:** April 27, 2026

---

## Table of Contents

- [Phase 1: Project Scaffolding & Core Architecture](#phase-1-project-scaffolding--core-architecture)
- [Phase 2: Room Database & Local Data Layer](#phase-2-room-database--local-data-layer)
- [Phase 3: Presentation Layer — Core UI Screens](#phase-3-presentation-layer--core-ui-screens)
- [Phase 4: AI Pipeline — Gemini API Integration](#phase-4-ai-pipeline--gemini-api-integration)
- [Phase 4.5: Recipe Detection, Catalog Cache & Bug Fixes](#phase-45-recipe-detection-catalog-cache--bug-fixes)
- [Phase 4.6: AI Name Standardization via Catalog Context](#phase-46-ai-name-standardization-via-catalog-context)
- [Phase 5: Nutrition Grounding — OpenFoodFacts API](#phase-5-nutrition-grounding--openfoodfacts-api)
- [Phase 5.5: Food Database Migration — USDA FDC + IFCT 2017](#phase-55-food-database-migration--usda-fdc--ifct-2017)
- [Phase 6: Authentication & Supabase Cloud Sync](#phase-6-authentication--supabase-cloud-sync)
- [Phase 7: Polish, Error Handling & Production Readiness](#phase-7-polish-error-handling--production-readiness)
- [Phase 8 Pre-work: UI / Design System Redesign](#phase-8-pre-work-ui--design-system-redesign)
- [Phase 8 Pre-work II: Sync Optimization & Schema Migrations](#phase-8-pre-work-ii-sync-optimization--schema-migrations)
- [Phase 8 Pre-work III: Pull-to-Refresh on Home Screen](#phase-8-pre-work-iii-pull-to-refresh-on-home-screen)
- [Phase 12: Unit Parity, Macro Calculation Fixes & RLS Hardening](#phase-12-unit-parity-macro-calculation-fixes--rls-hardening)
- [Phase 13: Grams Display Fix & Catalog-Hit Quantity Correction](#phase-13-grams-display-fix--catalog-hit-quantity-correction)
- [Phase 14: Macro Goals Cross-Platform Sync](#phase-14-macro-goals-cross-platform-sync)
- [Phase 16: Manual Recipe Builder + Catalog Navigation Fix + Catalog Miss Bug](#phase-16-manual-recipe-builder--catalog-navigation-fix--catalog-miss-bug)
- [Phase 17: Serving Size Clarification — Brand-Aware Nutrition Lookup](#phase-17-serving-size-clarification--brand-aware-nutrition-lookup)
- [Phase 17.1: Clarification Robustness — Prompt Expansion & Brand Validation](#phase-171-clarification-robustness--prompt-expansion--brand-validation)
- [Phase 18: Edit Log Macro Scaling Guard — Gram ↔ Non-Gram Boundary](#phase-18-edit-log-macro-scaling-guard--gram--non-gram-boundary)
- [Phase 19: Catalog Re-Log Macro Fix — Per-100g De-Normalization](#phase-19-catalog-re-log-macro-fix--per-100g-de-normalization)
- [Phase 20: AI Migration — Direct Gemini → Shared Supabase Edge Functions](#phase-20-ai-migration--direct-gemini--shared-supabase-edge-functions)
- [Phase 21: Ingredient Inline Edit — AI Parsed Mode](#phase-21-ingredient-inline-edit--ai-parsed-mode)
- [Phase 22: Recipe Card Nutrition Display + Edit Selected Recipe Fix](#phase-22-recipe-card-nutrition-display--edit-selected-recipe-fix)
- [Architecture Decisions](#architecture-decisions)
- [Known Issues & Tech Debt](#known-issues--tech-debt)

---

## Phase 1: Project Scaffolding & Core Architecture

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Set up the Android project skeleton with Clean Architecture layers, dependency injection, Material3 theming, and navigation.

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `settings.gradle.kts` | Created | Root Gradle settings with plugin management and dependency resolution |
| 2 | `build.gradle.kts` (root) | Created | Top-level build config with plugin aliases (AGP, Kotlin, Hilt, KSP, Serialization) |
| 3 | `gradle.properties` | Created | JVM args, AndroidX, Kotlin code style |
| 4 | `gradle/libs.versions.toml` | Created | Version catalog with all dependencies (Compose BOM 2024.12.01, Hilt 2.53.1, Room 2.6.1, Retrofit 2.11.0, etc.) |
| 5 | `gradle/wrapper/gradle-wrapper.properties` | Created | Gradle 8.11.1 distribution URL |
| 6 | `app/build.gradle.kts` | Created | App module — compileSdk 35, minSdk 26, Compose, Hilt, Room, Retrofit, WorkManager, testing deps |
| 7 | `app/proguard-rules.pro` | Created | R8 rules for Kotlin Serialization, Retrofit, Room entities, OkHttp |
| 8 | `app/src/main/AndroidManifest.xml` | Created | INTERNET + ACCESS_NETWORK_STATE permissions, Application & MainActivity declarations |
| 9 | `app/src/main/res/values/strings.xml` | Created | App name string resource |
| 10 | `app/src/main/res/values/themes.xml` | Created | Base XML theme (Material Light NoActionBar) |
| 11 | `NutriAiApplication.kt` | Created | `@HiltAndroidApp` Application class — Hilt dependency container entry point |
| 12 | `presentation/MainActivity.kt` | Created | `@AndroidEntryPoint` activity with `enableEdgeToEdge()` and Compose `setContent` |
| 13 | `presentation/theme/Color.kt` | Created | Green/teal/orange health-themed palette + macro-specific colors (cal, protein, carbs, fat) |
| 14 | `presentation/theme/Type.kt` | Created | Full Material3 Typography scale (display → label) |
| 15 | `presentation/theme/Theme.kt` | Created | Material3 light/dark color schemes + Android 12+ dynamic color support |
| 16 | `presentation/navigation/Screen.kt` | Created | Sealed class with route definitions: Home, Log, Catalog, Auth |
| 17 | `presentation/navigation/NutriAiNavHost.kt` | Created | NavHost with bottom navigation bar (Home, Catalog, Profile tabs) |
| 18 | `presentation/screens/home/HomeScreen.kt` | Created | Dashboard placeholder with FAB → Log screen navigation |
| 19 | `presentation/screens/log/LogScreen.kt` | Created | Food input screen with OutlinedTextField, TopAppBar with back nav |
| 20 | `presentation/screens/catalog/CatalogScreen.kt` | Created | Catalog placeholder with empty state messaging |
| 21 | `presentation/screens/auth/AuthScreen.kt` | Created | Auth/Profile placeholder screen |
| 22 | `domain/model/User.kt` | Created | Domain model — pure Kotlin data class |
| 23 | `domain/model/Catalog.kt` | Created | Domain model with soft-delete support (`deletedAt`) |
| 24 | `domain/model/FoodItem.kt` | Created | Domain model with baseline macros per serving |
| 25 | `domain/model/DailyLog.kt` | Created | Domain model with sync flags and computed macro totals |
| 26 | `domain/repository/UserRepository.kt` | Created | Repository interface — `getUserById`, `getUserByEmail`, `insertUser` |
| 27 | `domain/repository/CatalogRepository.kt` | Created | Repository interface — CRUD + soft delete with Flow |
| 28 | `domain/repository/FoodRepository.kt` | Created | Repository interface — search, CRUD, soft delete with Flow |
| 29 | `domain/repository/DailyLogRepository.kt` | Created | Repository interface — date range queries, unsynced queries, CRUD, soft/hard delete |
| 30 | `di/AppModule.kt` | Created | Hilt `@Module` — provides Json, OkHttpClient, named Retrofit instances (Gemini, OpenFoodFacts) |
| 31 | `util/Resource.kt` | Created | Sealed class `Resource<T>` — Success / Error / Loading wrapper |
| 32 | `util/Constants.kt` | Created | App constants — DB name, sync interval, API base URLs, local user ID |
| 33 | `.gitignore` | Created | Ignores .gradle, /build, local.properties, .idea, APKs |
| 34 | `local.properties.example` | Created | Template for API keys (Gemini, Supabase) — safe to commit |

### Key Decisions
- **Min SDK 26 (Android 8.0):** Covers 95%+ of active devices; enables `java.time` APIs without desugaring.
- **Dynamic Colors:** Uses Material You (Android 12+) with fallback to custom green palette.
- **Named Retrofits:** Separate Retrofit instances (`@Named("gemini")`, `@Named("openfoodfacts")`) to avoid URL conflicts.
- **KSP over KAPT:** Faster annotation processing for Hilt and Room.

### Tests
- [ ] Project compiles and runs on emulator
- [ ] Bottom navigation switches between Home, Catalog, and Profile screens
- [ ] FAB on Home screen navigates to Log screen
- [ ] Back arrow on Log screen returns to Home

---

## Phase 2: Room Database & Local Data Layer

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Implemented the complete local data layer: Room entities with sync/soft-delete flags, Flow-based DAOs, entity↔domain mappers, repository implementations wiring DAOs to domain interfaces, and Hilt DI module for database and repository bindings.

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `data/local/entity/UserEntity.kt` | Created | Room entity for `users` table — id (PK), email, created_at |
| 2 | `data/local/entity/CatalogEntity.kt` | Created | Room entity for `catalogs` table — FK to users, soft-delete (`deleted_at`), sync flag (`is_synced`), indexed on `user_id` |
| 3 | `data/local/entity/FoodItemEntity.kt` | Created | Room entity for `food_items` table — FK to catalogs, baseline macros per serving, external API ID, soft-delete, sync flag, indexed on `catalog_id` |
| 4 | `data/local/entity/DailyLogEntity.kt` | Created | Room entity for `daily_logs` table — FKs to users + food_items, consumed qty/unit, computed macro totals, sync flag, soft-delete, indexed on `user_id`, `food_item_id`, `date_timestamp` |
| 5 | `data/local/dao/UserDao.kt` | Created | DAO — `getUserById`, `getUserByEmail`, `insertUser` (REPLACE on conflict for upsert) |
| 6 | `data/local/dao/CatalogDao.kt` | Created | DAO — Flow queries filtering soft-deleted, `getUnsyncedCatalogs`, `softDeleteCatalog` with timestamp, `markAsSynced` batch |
| 7 | `data/local/dao/FoodItemDao.kt` | Created | DAO — Flow queries by catalog, LIKE-based name search, `getUnsyncedFoods`, `softDeleteFood`, `markAsSynced` batch |
| 8 | `data/local/dao/DailyLogDao.kt` | Created | DAO — date range Flow query, `getUnsyncedLogs` Flow (includes tombstones), `softDeleteLog`, `hardDeleteLog`, `markAsSynced` batch |
| 9 | `data/local/NutriAiDatabase.kt` | Created | Room `@Database` class (v1) — registers all 4 entities, exposes abstract DAO accessors, `exportSchema = true` |
| 10 | `data/local/mapper/UserMapper.kt` | Created | `UserEntity.toDomain()` / `User.toEntity()` extension functions |
| 11 | `data/local/mapper/CatalogMapper.kt` | Created | `CatalogEntity.toDomain()` / `Catalog.toEntity(isSynced)` extension functions |
| 12 | `data/local/mapper/FoodItemMapper.kt` | Created | `FoodItemEntity.toDomain()` / `FoodItem.toEntity(isSynced)` extension functions |
| 13 | `data/local/mapper/DailyLogMapper.kt` | Created | `DailyLogEntity.toDomain()` / `DailyLog.toEntity()` extension functions |
| 14 | `data/repository/UserRepositoryImpl.kt` | Created | Implements `UserRepository` — delegates to `UserDao` with mapper conversions |
| 15 | `data/repository/CatalogRepositoryImpl.kt` | Created | Implements `CatalogRepository` — Flow mapping, soft delete delegation |
| 16 | `data/repository/FoodRepositoryImpl.kt` | Created | Implements `FoodRepository` — Flow mapping, LIKE search, soft delete |
| 17 | `data/repository/DailyLogRepositoryImpl.kt` | Created | Implements `DailyLogRepository` — date range Flows, unsynced Flows, soft/hard delete |
| 18 | `di/DatabaseModule.kt` | Created | Hilt `@Module` — `DatabaseProviderModule` (Room DB + DAOs) + `RepositoryBindingsModule` (@Binds interface→impl) |
| 19 | `app/build.gradle.kts` | Modified | Added KSP args for Room schema export (`room.schemaLocation`), incremental processing, Kotlin codegen |

### Key Decisions
- **Destructive migration fallback:** Used `fallbackToDestructiveMigration()` during pre-release; proper migrations will be added post-schema-freeze.
- **Split Hilt modules:** `DatabaseProviderModule` (object, `@Provides`) for concrete instances vs `RepositoryBindingsModule` (abstract, `@Binds`) for interface bindings — cleaner separation.
- **Foreign keys with CASCADE:** User→Catalog→FoodItem and User→DailyLog cascading deletes for data integrity.
- **Indexed foreign key columns:** `user_id`, `catalog_id`, `food_item_id`, `date_timestamp` for query performance.
- **Sync-ready DAOs:** `getUnsyncedX()`, `markAsSynced()`, and `hardDeleteLog()` added proactively for Phase 6 (Supabase sync).
- **Room schema export:** Enabled via KSP args for schema version tracking and migration testing.

### Tests
- [x] Project compiles successfully (`BUILD SUCCESSFUL in 33s`)
- [x] Unit tests pass (`BUILD SUCCESSFUL in 12s` — `testDebugUnitTest`)
- [x] App installs and launches on emulator without Hilt/Room DI crash
- [ ] DAO instrumented tests on emulator
- [ ] Mapper unit tests
- [ ] Repository integration tests

---

## Phase 3: Presentation Layer — Core UI Screens

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Built functional UI screens with ViewModels, UseCases, and real data from Room. Replaced Phase 1 placeholder screens with fully wired Compose UI backed by Room via Kotlin Flows. Added 6 domain UseCases, 3 `@HiltViewModel` classes with `StateFlow`-based state management, 3 reusable Compose components, and enhanced navigation with animated bottom bar visibility.

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `domain/usecase/GetDailyLogsUseCase.kt` | Created | Fetches logs for a date range via `DailyLogRepository`, returns `Flow<List<DailyLog>>` |
| 2 | `domain/usecase/LogFoodUseCase.kt` | Created | Validates input, creates `FoodItem` + `DailyLog`, resolves/creates default catalog, inserts via repositories |
| 3 | `domain/usecase/SearchCatalogUseCase.kt` | Created | Wraps `FoodRepository.searchFoodsByName()` with input sanitization; returns empty Flow for blank queries |
| 4 | `domain/usecase/DeleteLogUseCase.kt` | Created | Soft-deletes a daily log entry via `DailyLogRepository` with ID validation |
| 5 | `domain/usecase/GetCatalogsUseCase.kt` | Created | Fetches user catalogs via `flatMapLatest` + food items from default catalog as `Flow<List<FoodItem>>` |
| 6 | `domain/usecase/InitializeUserUseCase.kt` | Created | Idempotent bootstrap — ensures `LOCAL_USER_ID` user + default catalog exist in Room on first launch |
| 7 | `presentation/screens/home/HomeViewModel.kt` | Created | `@HiltViewModel` — observes daily logs as `StateFlow` via `flatMapLatest`, computes macro totals, date navigation (prev/next/today), triggers user bootstrap in `init` |
| 8 | `presentation/screens/home/HomeScreen.kt` | Replaced | Date picker header with prev/next arrows, `MacroSummaryCard` with animated progress arcs, `LazyColumn` of food logs with `FoodLogItem`, empty state, FAB to Log screen |
| 9 | `presentation/screens/log/LogViewModel.kt` | Created | `@HiltViewModel` — manages form state (`LogUiState`) with computed `isValid` + preview totals, `SharedFlow` events for save success/error, form reset on save |
| 10 | `presentation/screens/log/LogScreen.kt` | Replaced | Manual entry form: food name, brand, serving size/qty/unit, calories/protein/carbs/fat, live macro preview card, snackbar feedback, loading spinner on save |
| 11 | `presentation/screens/catalog/CatalogViewModel.kt` | Created | `@HiltViewModel` — observes catalog foods via `flatMapLatest` (all or search-filtered), handles search query state, soft-delete actions |
| 12 | `presentation/screens/catalog/CatalogScreen.kt` | Replaced | Search bar with clear button, `LazyColumn` of `CatalogFoodCard` items with macro labels + delete, empty state (catalog empty vs. no search results) |
| 13 | `presentation/components/MacroSummaryCard.kt` | Created | Reusable card with animated circular progress arcs (800ms tween) for calories (large ring) + protein/carbs/fat (small rings), goal-based progress, Material3 styling |
| 14 | `presentation/components/FoodLogItem.kt` | Created | Reusable `SwipeToDismissBox` row — food name, quantity label, 4 macro chips (kcal/P/C/F), swipe-to-delete with red background + delete icon |
| 15 | `presentation/components/NutritionFactsCard.kt` | Created | Nutrition label-style card with food name, brand, per-serving header, dividers, colored macro rows |
| 16 | `presentation/navigation/NutriAiNavHost.kt` | Modified | Added `AnimatedVisibility` for bottom bar (slides in/out), bottom bar hidden on Log screen, maintained `hiltViewModel()` injection in composables |
| 17 | `presentation/navigation/Screen.kt` | Modified | Updated KDoc to document Phase 3 static routes and future parameterized route plans |

### Key Decisions
- **StateFlow over LiveData:** Compose-native, lifecycle-aware, better for unidirectional data flow. `WhileSubscribed(5_000)` prevents unnecessary upstream collection.
- **UseCases as single-responsibility classes:** Each UseCase has one `operator fun invoke()` — testable, composable. Hilt injects repository dependencies.
- **Manual food entry first:** AI parsing (Phase 4) plugs into the same `LogViewModel` later; Phase 3 uses manual input fields for name, serving size, and macros.
- **Local user bootstrap:** `InitializeUserUseCase` seeds `LOCAL_USER_ID` + default catalog on first app launch. Called in `HomeViewModel.init {}` — idempotent.
- **Date-centric home screen:** Dashboard pivots on a single date via `_selectedDate` StateFlow. Next day capped at today. Previous day unlimited.
- **SharedFlow for one-time events:** `LogViewModel` uses `MutableSharedFlow<LogEvent>` for save success/error navigation — avoids state re-consumption issues with StateFlow.
- **SwipeToDismissBox:** Material3 native swipe-to-delete for food log items — intuitive mobile UX.
- **Animated bottom bar:** `AnimatedVisibility` with `slideInVertically`/`slideOutVertically` hides bottom nav on the Log screen for a cleaner full-screen form experience.
- **Content/state separation:** Each screen has a public `@Composable` (with ViewModel) + private `Content` composable (pure state) — enables `@Preview` without Hilt injection.

### Tests
- [x] Project compiles successfully (`BUILD SUCCESSFUL in 17s`)
- [x] Unit tests pass (`BUILD SUCCESSFUL in 2s` — `testDebugUnitTest`)
- [x] App installs and runs on emulator — all screens functional
- [x] Log food → appears on Home screen with correct date and macro totals
- [x] Catalog screen shows logged food items with search
- [x] Date navigation (prev/next/today) works correctly
- [x] Delete button removes food log entry
- [ ] `HomeViewModel` state transitions (loading → success with data, empty state)
- [ ] `LogViewModel` validation (reject blank name, zero quantity)
- [ ] `CatalogViewModel` search filtering
- [ ] UseCase unit tests with mocked repositories (MockK + Turbine)
- [ ] Compose UI: `MacroSummaryCard` renders correct values

### Bugs Found & Fixed (Post-Implementation)

| # | Bug | Root Cause | Fix | Files Changed |
|---|-----|-----------|-----|--------------|
| 1 | **Food log showed UUID instead of food name** | `HomeScreen` passed `log.foodItemId` (UUID) as `foodName` to `FoodLogItem` — no name resolution | Added `foodNames: Map<String, String>` to `HomeUiState`; `HomeViewModel` resolves names via `foodRepository.getFoodById()` for each unique `foodItemId`; `HomeScreen` reads from `uiState.foodNames[log.foodItemId]` | `HomeViewModel.kt`, `HomeScreen.kt` |
| 2 | **Catalog screen was empty after logging food** | `InitializeUserUseCase` created catalog with `UUID.randomUUID()` but `LogFoodUseCase.resolveDefaultCatalogId()` looked for `"local_user_default"` — IDs never matched, so food went into orphan catalogs | Added `DEFAULT_CATALOG_ID = "local_user_default"` to `Constants`; both `InitializeUserUseCase` and `LogFoodUseCase` now use this deterministic ID | `Constants.kt`, `InitializeUserUseCase.kt`, `LogFoodUseCase.kt` |
| 3 | **`GetCatalogsUseCase` only read first catalog** | Used `catalogs.firstOrNull()` — missed food items in other catalogs if multiple existed | Changed to `combine()` all catalog food Flows into a single flat list across ALL user catalogs | `GetCatalogsUseCase.kt` |
| 4 | **Food log appeared on wrong date (off by one)** | DAO used `BETWEEN :start AND :end` (inclusive both ends); `LogViewModel` saved `dateTimestamp = startOfDay (midnight)`; viewing previous day's range `[prevDay 00:00, today 00:00]` included today's midnight entry | Changed SQL to `>= :start AND < :end` (exclusive end bound) — half-open interval `[startOfDay, startOfNextDay)` | `DailyLogDao.kt` |
| 5 | **No visible edit/delete actions on food log items** | Only had swipe-to-delete (not discoverable to users) | Added visible ✏️ edit and 🗑️ delete `IconButton`s at bottom-right of each `FoodLogItem` card; swipe-to-delete preserved as secondary gesture | `FoodLogItem.kt` |

### Known Remaining Issues

| # | Issue | Severity | Notes |
|---|-------|----------|-------|
| 1 | **Edit button not wired** | Medium | `FoodLogItem` accepts `onEdit` callback but `HomeScreen` doesn't pass one yet. Needs a pre-populated Log screen or edit dialog. |
| 2 | **Deleting food from catalog shows "Unknown Food" on Home** | Low | If a food item is soft-deleted from Catalog, the Home screen's `getFoodById()` returns null → displays "Unknown Food" for existing logs. Logs should preserve the food name independently. |
| 3 | **Macro goals are hardcoded** | Low | `MacroSummaryCard` uses fixed goals (2000 kcal, 150g protein, 250g carbs, 65g fat). Should be user-configurable in Profile/Settings. |
| 4 | **No confirmation dialog on delete** | Low | Delete happens immediately on button tap or swipe — no undo or "Are you sure?" prompt. |

---

## Phase 3.5: Post-Implementation Bug Fixes

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Fixed 5 bugs discovered during manual testing of Phase 3. Issues spanned data layer (DAO date query, catalog ID mismatch), presentation layer (UUID displayed instead of food name), and UX (no visible delete/edit actions).

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `util/Constants.kt` | Modified | Added `DEFAULT_CATALOG_ID = "local_user_default"` — single source of truth for catalog ID |
| 2 | `domain/usecase/InitializeUserUseCase.kt` | Modified | Uses `Constants.DEFAULT_CATALOG_ID` instead of `UUID.randomUUID()` for default catalog creation |
| 3 | `domain/usecase/LogFoodUseCase.kt` | Modified | `resolveDefaultCatalogId()` now uses `Constants.DEFAULT_CATALOG_ID` — consistent with `InitializeUserUseCase` |
| 4 | `domain/usecase/GetCatalogsUseCase.kt` | Modified | Aggregates food items across ALL user catalogs via `combine()` instead of just `firstOrNull()` |
| 5 | `presentation/screens/home/HomeViewModel.kt` | Modified | Added `FoodRepository` injection; resolves `foodItemId` → food name via `getFoodById()`; added `foodNames` map to `HomeUiState` |
| 6 | `presentation/screens/home/HomeScreen.kt` | Modified | Reads food name from `uiState.foodNames[log.foodItemId]` instead of raw `log.foodItemId`; fixed preview data |
| 7 | `data/local/dao/DailyLogDao.kt` | Modified | Changed date range query from `BETWEEN` (inclusive) to `>= AND <` (half-open interval) — fixes off-by-one day bleed |
| 8 | `presentation/components/FoodLogItem.kt` | Modified | Added visible edit (✏️) and delete (🗑️) `IconButton`s; added optional `onEdit` callback parameter; kept swipe-to-delete as secondary gesture |

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 13s`
- [x] Manually verified: food name displays correctly, date range correct, catalog populated, delete button works

---

## Phase 4: AI Pipeline — Gemma 4 API Integration

**Status:** ✅ Completed (bugs fixed in Phase 4.5)
**Date:** April 27, 2026

### Summary
Implemented the full AI food parsing pipeline using Google Gemma 4 (`gemma-4-26b-a4b-it`) hosted on the Gemini API. The Log screen now has two input modes: **AI Parse** (default) and **Manual Entry**, switchable via tabs. Users type a natural language food description (e.g., "2 eggs, toast with butter, and a glass of milk"), tap "Parse with AI", and the Gemma 4 model extracts structured food entities (name, quantity, unit, confidence). Users can then accept individual parsed items into the manual form for editing, or batch-log all parsed items at once.

**Current state:** Code compiles and builds successfully (`BUILD SUCCESSFUL`), unit tests pass, but **runtime AI parsing has not been verified on-device** — there are likely bugs in the API request/response flow that need debugging with a real Gemini API key.

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `data/remote/dto/GeminiRequest.kt` | Created | Request DTOs — `GeminiRequest`, `GeminiContent`, `GeminiPart`, `GeminiGenerationConfig` with `responseMimeType: "application/json"`, `GeminiThinkingConfig` (disables Gemma 4 thinking mode with `thinkingBudget=0`), `GeminiToolConfig`, `GeminiFunctionCallingConfig` |
| 2 | `data/remote/dto/GeminiResponse.kt` | Created | Response DTOs — `GeminiResponse`, `GeminiCandidate`, `GeminiResponseContent`, `GeminiResponsePart`, `GeminiPromptFeedback` for nested `candidates[0].content.parts[0].text` extraction; `GeminiParsedFoodsWrapper` + `GeminiParsedFoodDto` for deserializing the structured JSON food output |
| 3 | `data/remote/api/GeminiApiService.kt` | Created | Retrofit `@POST("v1beta/models/gemma-4-26b-a4b-it:generateContent")` interface with `@Query("key")` API key auth |
| 4 | `domain/model/ParsedFood.kt` | Created | Domain model — `name: String`, `quantity: Double`, `unit: String`, `confidence: Double`; no macros (Phase 5 via OpenFoodFacts) |
| 5 | `domain/repository/AiRepository.kt` | Created | Repository interface — `suspend fun parseFood(input: String): Resource<List<ParsedFood>>` |
| 6 | `data/repository/AiRepositoryImpl.kt` | Created | Implements `AiRepository` — builds Gemma 4 request with system instruction + user prompt, calls API, extracts JSON with `extractJson()` fallback (strips markdown fences/thinking tags), maps to domain, comprehensive HTTP error handling (400/401/403/429/500/503), network errors, timeouts |
| 7 | `domain/usecase/ParseFoodWithAiUseCase.kt` | Created | Input validation (blank check, min 2 chars, max 500 chars) + delegates to `AiRepository.parseFood()` |
| 8 | `util/GeminiPrompts.kt` | Created | `SYSTEM_INSTRUCTION` (entity extraction rules, JSON schema, no-calorie-estimation constraint) + `buildUserPrompt()` (reinforces JSON schema inline — Gemma 4 best practice for reliable structured output) |
| 9 | `di/AppModule.kt` | Modified | Added `provideGeminiApiService()` — creates `GeminiApiService` from `@Named("gemini")` Retrofit instance |
| 10 | `di/DatabaseModule.kt` | Modified | Added `bindAiRepository()` in `RepositoryBindingsModule` — `AiRepositoryImpl` → `AiRepository` |
| 11 | `presentation/screens/log/LogViewModel.kt` | Modified | Added `LogInputMode` enum (AI_INPUT / MANUAL_INPUT), AI state fields (`aiInput`, `isParsing`, `parsedFoods`, `selectedParsedFoodIndex`, `aiErrorMessage`), `parseWithAi()`, `selectParsedFood()`, `acceptParsedFood()` (copies into manual form), `acceptAndLogAllParsed()` (batch save), `clearParsedFoods()`, mode switching methods |
| 12 | `presentation/screens/log/LogScreen.kt` | Modified | Added `InputModeTabs` (AI Parse ✨ / Manual ✏️), `AiInputSection` (multi-line text input, "Parse with AI" button with loading spinner, parsed results cards with selection, "Edit Selected" / "Log All" / "Clear & Try Again" actions, info card about macros, "Enter Manually Instead" fallback), `ParsedFoodCard` (selectable card with name, quantity, unit, confidence %, checkmark), preserved full `ManualInputSection` from Phase 3 |

### Key Decisions
- **Gemma 4 over Gemini models:** Using `gemma-4-26b-a4b-it` (Apache 2.0 open model) hosted on the Gemini API. Future-proofs against Gemini model deprecations (2.0 shut down June 2026, 2.5 will eventually follow). Same API key, same endpoint format.
- **Thinking mode minimized:** `thinkingLevel = "MINIMAL"` in `GeminiThinkingConfig` — Gemma 4's full thinking mode wraps output in thinking tags that break JSON parsing. Fixed from `thinkingBudget = 0` in Phase 4.5.
- **Schema reinforced in both prompts:** JSON schema described in system instruction AND user prompt. Gemma 4 produces more reliable structured output with dual reinforcement.
- **`responseMimeType: "application/json"`:** API-level JSON enforcement, plus `extractJson()` fallback in `AiRepositoryImpl` to handle edge cases (markdown fences, leaked thinking tags).
- **Entity extraction only:** Gemma 4 extracts food name, quantity, unit, and confidence. It does NOT estimate calories or macros — that's Phase 5 (OpenFoodFacts grounding).
- **Two input modes with tabs:** `AI_INPUT` (default) and `MANUAL_INPUT`. State is preserved when switching. AI mode defaults for discoverability; manual mode always available as fallback.
- **Graceful fallback:** If AI fails (no key, network error, rate limit, bad parse), user sees clear error + "Enter Manually Instead" button. AI is an enhancement, not a dependency.
- **API key via BuildConfig:** `GEMINI_API_KEY` loaded from `local.properties` → `BuildConfig.GEMINI_API_KEY` at build time. Blank key returns `Resource.Error` with setup instructions.
- **Low temperature (0.1):** Deterministic food parsing, not creative generation. Combined with `topP=0.8`, `topK=40` for focused output.
- **Batch log all:** "Log All" saves all parsed foods with 0-value macros. Users can fill macros later, or Phase 5 will auto-fill from OpenFoodFacts.

### Gemma 4 Request Structure
```json
{
  "contents": [{"role": "user", "parts": [{"text": "Parse the following..."}]}],
  "generationConfig": {
    "temperature": 0.1,
    "topP": 0.8,
    "topK": 40,
    "maxOutputTokens": 1024,
    "responseMimeType": "application/json",
    "thinkingConfig": {"thinkingLevel": "MINIMAL"}
  },
  "systemInstruction": {
    "role": "user",
    "parts": [{"text": "You are a food entry parser..."}]
  }
}
```

### Expected AI Response
```json
{
  "foods": [
    {"name": "whole wheat toast", "quantity": 2, "unit": "slice", "confidence": 0.95},
    {"name": "peanut butter", "quantity": 1, "unit": "serving", "confidence": 0.9}
  ]
}
```

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 5s`
- [x] `testDebugUnitTest` — `BUILD SUCCESSFUL in 1s` (no regressions)
- [x] All Phase 3 functionality preserved (manual entry, macro preview, save)

### Tests (Pending)
- [ ] **CRITICAL:** Runtime test with real `GEMINI_API_KEY` — verify Gemma 4 returns valid JSON
- [ ] `GeminiApiService` returns valid response for test prompt
- [ ] `AiRepositoryImpl` maps response to `ParsedFood` correctly
- [ ] `AiRepositoryImpl.extractJson()` handles markdown fences, thinking tags, clean JSON
- [ ] `ParseFoodWithAiUseCase` returns `Resource.Error` on network failure
- [ ] `LogViewModel.parseWithAi()` populates `parsedFoods` in state
- [ ] `LogViewModel.acceptParsedFood()` copies name/qty/unit to manual form
- [ ] `LogViewModel.acceptAndLogAllParsed()` saves all foods to Room
- [ ] Fallback to manual entry when API key is missing or API fails
- [ ] UI: loading spinner during parse, parsed food cards render correctly
- [ ] UI: tab switching preserves state between AI and Manual modes

### Known Issues & Bugs to Investigate

| # | Issue | Severity | Notes |
|---|-------|----------|-------|
| 1 | **AI parsing not runtime-tested** | High | Code compiles but has not been verified with a real API key on-device. Request/response format may have issues with Gemma 4's actual output structure. |
| 2 | **`thinkingConfig` placement may be wrong** | Medium | `thinkingConfig` is nested inside `generationConfig` in our DTOs. Some Gemma 4 docs show it at the top level of the request. Need to verify which placement the API accepts. If wrong, Gemma 4 thinking mode stays active and corrupts JSON output. |
| 3 | **`responseMimeType` may not be supported for Gemma 4** | Medium | Some documentation suggests `responseMimeType: "application/json"` works for Gemma 4 via the Gemini API, but other sources are ambiguous. If unsupported, the API may return a 400 error or ignore the constraint and return free-text. The `extractJson()` fallback should handle the latter case. |
| 4 | **Batch "Log All" saves 0 calories** | Low | `acceptAndLogAllParsed()` logs all foods with `calories=0, protein=0, carbs=0, fat=0`. On the Home screen, these will show as 0-calorie entries with no macro data. Expected behavior for Phase 4 (Phase 5 will auto-fill), but confusing for users. Consider adding a note/badge. |
| 5 | **Phase 3 bug interaction: "Unknown Food" on soft-delete** | Low | AI-logged foods store names in `FoodItem`, resolved via `getFoodById()` in `HomeViewModel`. Soft-deleting a food from Catalog still causes "Unknown Food" for existing logs. The `acceptParsedFood()` flow correctly populates `FoodItem.name`, but the underlying Phase 3 bug remains. |

### API Key Setup
1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey) and create a Gemini API key
2. Add to `local.properties` (never committed): `GEMINI_API_KEY=your_key_here`
3. Rebuild the project — key is injected via `BuildConfig.GEMINI_API_KEY`

---

## Phase 4.5: Recipe Detection, Catalog Cache & Bug Fixes

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Three-part enhancement to Phase 4: (1) fixed critical runtime bugs that prevented AI parsing from working on-device (wrong model name, empty response from thinking mode, wrong thinking config), (2) added recipe-aware AI parsing where compound dishes (e.g., "Dosa and Chutney: ingredients - 100g sooji, 50g yogurt, 20g chutney powder, 1tsp oil") are detected and returned as nested recipes with individual ingredients, (3) implemented local catalog caching so previously-logged ingredients show "In catalog ✅" badge and reuse cached macros. Also added ingredient-level selection and editing within recipe cards.

### Bug Fixes (Phase 4 Runtime Issues)

| # | Bug | Root Cause | Fix | Files Changed |
|---|-----|-----------|-----|--------------|
| 1 | **404 error on AI parse** | Model name was `gemma-4-27b-it` — incorrect for Gemma 4 | Changed to `gemma-4-26b-a4b-it` — correct model identifier | `GeminiApiService.kt` |
| 2 | **Empty AI response (no parsed foods)** | Gemma 4 returns multi-part response with `thought: true` parts; code extracted `parts[0].text` which was the thinking part (empty/null) | Added `thought: Boolean?` field to `GeminiResponsePart`; extraction changed to `lastOrNull { it.thought != true && !it.text.isNullOrBlank() }` | `GeminiResponse.kt`, `AiRepositoryImpl.kt` |
| 3 | **`thinkingConfig` format wrong** | Used `thinkingBudget: Int` but Gemma 4 requires `thinkingLevel: String` | Changed to `thinkingLevel: "MINIMAL"` — string enum instead of integer budget | `GeminiRequest.kt` |

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `domain/model/ParsedFood.kt` | Modified | Added `isRecipe: Boolean = false` and `ingredients: List<ParsedFood> = emptyList()` fields for recipe support |
| 2 | `data/remote/dto/GeminiResponse.kt` | Modified | Added `@SerialName("is_recipe") isRecipe: Boolean = false` and self-referential `ingredients: List<GeminiParsedFoodDto>` to DTO; added `thought: Boolean?` to `GeminiResponsePart` |
| 3 | `domain/model/CatalogMatch.kt` | Created | Domain model linking `ParsedFood` → cached `FoodItem` with `isFromCatalog` flag |
| 4 | `util/GeminiPrompts.kt` | Modified | Added RECIPE DETECTION RULES to system instruction (patterns: "X ingredients: ...", "X made with ..."); updated `buildUserPrompt()` with nested JSON schema including `is_recipe` and `ingredients` |
| 5 | `data/repository/AiRepositoryImpl.kt` | Modified | Updated DTO→domain mapping to handle recursive `ingredients` list; fixed response extraction to filter `thought` parts |
| 6 | `data/local/dao/FoodItemDao.kt` | Modified | Added `searchFoodByNameExact()` — `LOWER(name) = LOWER(:name)` case-insensitive exact match query |
| 7 | `domain/repository/FoodRepository.kt` | Modified | Added `suspend fun searchFoodByNameExact(name: String, catalogId: String): FoodItem?` |
| 8 | `data/repository/FoodRepositoryImpl.kt` | Modified | Implemented `searchFoodByNameExact()` delegating to DAO with mapper |
| 9 | `domain/usecase/ResolveCatalogCacheUseCase.kt` | Created | Resolves parsed foods against local catalog: `invoke()` for top-level (recipes + flat items), `resolveIngredients()` for ingredient-level resolution |
| 10 | `domain/usecase/LogFoodUseCase.kt` | Modified | Added `logRecipe()` method — creates FoodItem for each new ingredient, recipe FoodItem with aggregated macros, DailyLog entry |
| 11 | `presentation/screens/log/LogViewModel.kt` | Modified | Full rewrite: added `ResolveCatalogCacheUseCase` injection, `catalogMatches`/`ingredientCatalogMatches`/`isResolvingCache`/`selectedIngredientIndex` state fields, auto-triggers `resolveCatalogCache()` after parse, recipe-aware batch logging, ingredient-level selection and editing |
| 12 | `presentation/screens/log/LogScreen.kt` | Modified | Recipe cards with restaurant icon + ingredient count, tappable `IngredientRow` with highlight + edit icon, `CatalogBadge` composable ("In catalog" / "New"), hint text for ingredient editing, contextual button labels |
| 13 | `data/remote/api/GeminiApiService.kt` | Modified | Fixed model name from `gemma-4-27b-it` to `gemma-4-26b-a4b-it` |
| 14 | `data/remote/dto/GeminiRequest.kt` | Modified | Changed `thinkingBudget: Int` to `thinkingLevel: String` in `GeminiThinkingConfig` |

### Key Decisions
- **AI-side recipe detection:** Recipe patterns detected by Gemma 4 in the prompt (not client-side heuristics). AI returns `is_recipe: true` with nested `ingredients` array. Enables compound dishes like "Besan Chila" to be parsed as one recipe with individual ingredients.
- **Show indicator + confirm (catalog cache):** When an ingredient is found in the local catalog, show "In catalog ✅" badge. User can still override macros. New ingredients show "New" badge.
- **Local catalog first (no remote lookup for cache):** Catalog cache only checks the local Room database — no network calls. Fast and offline-friendly. Remote catalog sync is deferred to Phase 6.
- **Ingredient-level editing:** Users can tap individual ingredients within a recipe card. The selected ingredient highlights (primary color background + edit icon), and "Edit Ingredient" opens the manual form pre-filled with that ingredient's data (including cached macros if available from catalog).
- **`thinkingLevel: "MINIMAL"` over `thinkingBudget: 0`:** Gemma 4 uses string-enum thinking config, not integer budgets. "MINIMAL" allows lightweight thinking without full chain-of-thought output that corrupts JSON.
- **Multi-part response filtering:** Gemma 4 returns `thought: true` parts alongside content parts. Extraction uses `lastOrNull { it.thought != true }` to skip thinking parts and get the actual JSON response.

### On-Device Verification
- [x] **AI parsing works:** "Dosa and Chutney: ingredients - 100g sooji, 50g yogurt, 20g chutney powder, 1tsp oil" → Recipe card with 4 ingredients, correct names/quantities/units
- [x] **Catalog cache works:** "oil" (previously logged) shows catalog badge ✅; new ingredients show "New" badge
- [x] **Ingredient editing works:** Tapping an ingredient highlights it; "Edit Ingredient" button opens manual form pre-filled with that ingredient's data
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 11s`

---

## Phase 4.6: AI Name Standardization via Catalog Context

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Injected the user's existing ingredient and recipe catalog names into every Gemma 4 parse request so the AI normalizes extracted food names against known entries. This prevents duplicate catalog records caused by synonym variation — e.g., if "besan" is already in the catalog, typing "gram flour" or "chickpea flour" will produce `"name": "besan"` in the AI output rather than creating a new entry. The feature is fully self-contained in the prompt and data layers with no ViewModel, UseCase, or UI changes required.

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `data/local/dao/FoodItemDao.kt` | Modified | Added `getAllFoodNames(catalogId: String): List<String>` — lightweight `SELECT name` query filtered to non-deleted rows; scoped to a specific catalog |
| 2 | `domain/repository/FoodRepository.kt` | Modified | Added `suspend fun getAllFoodNames(catalogId: String): List<String>` to the domain interface |
| 3 | `data/repository/FoodRepositoryImpl.kt` | Modified | Implemented `getAllFoodNames()` delegating to `FoodItemDao.getAllFoodNames()` |
| 4 | `util/GeminiPrompts.kt` | Modified | Added `NAME STANDARDIZATION RULES` section to `SYSTEM_INSTRUCTION`; updated `buildUserPrompt()` signature to accept `existingIngredients: List<String>` and `existingRecipes: List<String>` (both default `emptyList()`); injects both as labelled context sections before the JSON schema when non-empty |
| 5 | `data/repository/AiRepositoryImpl.kt` | Modified | Added `FoodRepository` constructor injection; fetches `getAllFoodNames(INGREDIENT_CATALOG_ID)` and `getAllFoodNames(RECIPE_CATALOG_ID)` before each parse request; passes both lists to `buildUserPrompt()` |

### How It Works

**Request flow:**
1. User types a food description and taps "Parse with AI"
2. `AiRepositoryImpl.parseFood()` fetches two name lists from Room (name column only — lightweight)
3. Both lists are passed to `GeminiPrompts.buildUserPrompt()`, which injects them as context sections:
   ```
   EXISTING INGREDIENTS (use exact name if semantically matched):
   besan, oil, yogurt, cumin seeds, ...

   EXISTING RECIPES (use exact name if semantically matched):
   Besan Chila, Dosa, ...
   ```
4. The Gemma 4 system instruction's `NAME STANDARDIZATION RULES` section tells the model how to apply these lists

**Prompt structure (when catalog is populated):**
```
Parse the following food entry and extract each food item.

EXISTING INGREDIENTS (use exact name if semantically matched):
besan, oil, yogurt

EXISTING RECIPES (use exact name if semantically matched):
Besan Chila

Respond with ONLY a JSON object in this exact schema:
{ ... }

Food entry: "200g gram flour, 1 tsp cumin, 1 tbsp oil"
```

**Expected AI output (with "besan" and "oil" in catalog):**
```json
{
  "foods": [
    {"name": "besan", "quantity": 200, "unit": "gram", "confidence": 0.95, "is_recipe": false, "ingredients": []},
    {"name": "cumin seeds", "quantity": 1, "unit": "tsp", "confidence": 0.90, "is_recipe": false, "ingredients": []},
    {"name": "oil", "quantity": 1, "unit": "tablespoon", "confidence": 0.98, "is_recipe": false, "ingredients": []}
  ]
}
```

### Key Decisions

- **Lists injected in `buildUserPrompt()`, rules in `SYSTEM_INSTRUCTION`:** The system instruction defines static matching behaviour (role of the lists, case rules, confidence threshold). The user prompt carries the per-request dynamic data (the actual names). This keeps the system instruction stable across requests while the context varies.
- **Catalog separation via existing catalog IDs:** Ingredients and recipes are already stored in separate catalogs (`INGREDIENT_CATALOG_ID` / `RECIPE_CATALOG_ID` from Phase 4.5). No schema change or new `isRecipe` column needed — just query by catalog ID.
- **`SELECT name` only:** `getAllFoodNames()` fetches only the `name` column, not full entities. Avoids unnecessary deserialization for what is purely a string list.
- **Both lists default to `emptyList()`:** If the catalog is empty (first launch, or all entries deleted), the prompt sections are omitted entirely — no empty headers sent to the model. Backward-compatible with zero catalog entries.
- **Case-agnostic matching, catalog-casing output:** The `NAME STANDARDIZATION RULES` instruct the model to match case-insensitively but output the name in the same case as the existing catalog entry. The user's established casing is preserved.
- **Token cost is negligible:** For a single-user app with ~100–150 ingredients + ~50 recipes, the lists add approximately 1,500–2,000 tokens to a stateless single-turn request — well within Gemma 4's context window and budget.
- **No ViewModel or UseCase changes:** The feature is entirely contained in `AiRepositoryImpl` (data fetch + prompt building). `ParseFoodWithAiUseCase` and `LogViewModel` signatures are unchanged.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 27 | Catalog names fetched in `AiRepositoryImpl`, not UseCase/ViewModel | Keeps the name standardization concern internal to the AI data layer. UseCase and ViewModel stay agnostic — they just call `parseFood(input)` as before |
| 28 | Per-request catalog fetch (not cached in memory) | Room queries are fast and the catalog is the source of truth. Caching names in memory risks stale data if the user adds items mid-session |
| 29 | Empty lists → sections omitted from prompt | Avoids sending empty/misleading headers to the model on first launch. Cleaner prompt, no wasted tokens |

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL`
- [x] No regressions — all Phase 4.5 functionality preserved
- [x] `FoodRepository` interface change is non-breaking — only `AiRepositoryImpl` calls the new method

### Tests (Pending)
- [ ] `getAllFoodNames()` DAO query returns correct names for each catalog, excludes soft-deleted items
- [ ] `buildUserPrompt()` with non-empty lists includes both sections with correct formatting
- [ ] `buildUserPrompt()` with empty lists omits both sections entirely
- [ ] On-device: "gram flour" normalizes to "besan" when "besan" is in the catalog
- [ ] On-device: new ingredient (not in catalog) keeps the AI-extracted name unchanged

---

## Phase 5: Nutrition Grounding — OpenFoodFacts API

**Status:** ✅ Completed
**Date:** April 27, 2026

### Summary
Grounded AI-parsed food entities against OpenFoodFacts for real nutritional data. After Gemma 4 extracts food names, Phase 5 looks up those names in the OpenFoodFacts database in parallel to retrieve verified per-100g calorie and macro values, then scales them to the user's serving quantity. Macros are auto-filled when the user taps "Edit Selected", and the "Log All" batch path also uses nutrition data when available.

**Macro fill priority (highest → lowest):**
1. Catalog cache hit (Phase 4.5) — user's own historical data
2. OpenFoodFacts match (Phase 5) — scaled per-100g values
3. 0-value — user fills manually

**For recipes:** Each ingredient gets its own nutrition lookup. When an ingredient is resolved with OpenFoodFacts data AND logged via "Log All", the enriched `FoodItem` is inserted to the Ingredients catalog (not just used transiently) so future sessions benefit from the cache.

### Changes Made

| # | File / Directory | Action | Description |
|---|-----------------|--------|-------------|
| 1 | `data/remote/dto/OpenFoodFactsResponse.kt` | Created | DTOs — `OFFSearchResponse`, `OFFProduct`, `OFFNutriments` with `@SerialName("energy-kcal_100g")` (hyphen in field name handled by Kotlinx Serialization); `resolvedKcal100g` computed property falls back from kcal → kJ÷4.184; `hasUsableData` guard filters products with no calorie data |
| 2 | `data/remote/api/OpenFoodFactsApiService.kt` | Created | Retrofit `@GET("api/v2/search")` interface — `search_terms`, `fields`, `page_size=5`, `sort_by=popularity` params; no API key required |
| 3 | `domain/model/NutritionInfo.kt` | Created | Domain model — all fields per 100g, `source = "OpenFoodFacts"`, `externalId` for future cache re-lookups stored in `FoodItem.externalApiId` |
| 4 | `domain/repository/NutritionRepository.kt` | Created | Repository interface — `suspend fun searchNutrition(foodName: String): Resource<List<NutritionInfo>>` |
| 5 | `data/repository/NutritionRepositoryImpl.kt` | Created | Implements `NutritionRepository` — calls OpenFoodFacts, filters `hasUsableData`, ranks results by macro completeness (score: +4 cal, +1 each protein/carbs/fat), maps `OFFProduct` → `NutritionInfo`; handles HTTP 429/5xx, no-internet, timeout errors |
| 6 | `domain/usecase/LookupNutritionUseCase.kt` | Created | Validates non-blank name, delegates to `NutritionRepository.searchNutrition()`, returns first-ranked match or null |
| 7 | `di/AppModule.kt` | Modified | Added `@Named("openfoodfacts")` `OkHttpClient` with `User-Agent: NutriAI - Android - 1.0.0` interceptor (required by OpenFoodFacts); updated `provideOpenFoodFactsRetrofit` to inject the dedicated client; added `provideOpenFoodFactsApiService()` |
| 8 | `di/DatabaseModule.kt` | Modified | Added `bindNutritionRepository(impl: NutritionRepositoryImpl): NutritionRepository` in `RepositoryBindingsModule` |
| 9 | `domain/usecase/LogFoodUseCase.kt` | Modified | `invoke()` accepts new optional `externalApiId: String?` — persisted to `FoodItem.externalApiId`; `logRecipe()` updated with 3-case ingredient handling: catalog hit (reuse macros, no DB insert), nutrition-enriched (insert to catalog + use macros), no-data (0-macro placeholder) |
| 10 | `presentation/screens/log/LogViewModel.kt` | Modified | Added `NutritionLookupState` sealed class (Loading / Found / NotFound / Error); `LogUiState` gains `nutritionLookups: Map<Int, NutritionLookupState>`, `ingredientNutritionLookups: Map<String, NutritionLookupState>`, `isLookingUpNutrition: Boolean`; `lookupNutrition()` method launches all lookups concurrently via `async`/`awaitAll`, skipping catalog-hit items; `acceptParsedFood()` updated with priority-based macro pre-fill; `acceptAndLogAllParsed()` uses nutrition results to log with real macros; `computeServingMultiplier()` converts unit → grams multiplier (g/ml=÷100, tsp=×5÷100, tbsp=×15÷100, cups=×240÷100, other=1.0) |
| 11 | `presentation/components/NutritionMatchCard.kt` | Created | Reusable card displaying matched product name, brand, per-100g macros (color-coded), and "OpenFoodFacts" attribution badge; used in previews |
| 12 | `presentation/screens/log/LogScreen.kt` | Modified | `ParsedFoodCard` accepts `nutritionState` + `ingredientNutritionStates`; flat items show `NutritionStatusRow` (loading spinner / found badge with kcal/100g / not-found / error); ingredient rows show mini nutrition badge (spinner / green check / "New"); status bar consolidates "Checking catalog..." and "Looking up nutrition..." into single animated indicator; info card text updates to "Nutrition data auto-filled ✓" when any lookup completes |

### Key Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 30 | Dedicated `@Named("openfoodfacts")` OkHttpClient | OpenFoodFacts requires a descriptive `User-Agent`. Sharing the Gemini client would add the header to Gemini requests unnecessarily. Separate clients keep concerns clean. |
| 31 | `energy-kcal_100g` with hyphen via `@SerialName` | The OpenFoodFacts API returns `energy-kcal_100g` (hyphen) which is not valid Kotlin identifier syntax. Kotlinx `@SerialName` handles this cleanly. |
| 32 | kJ fallback for kcal | Some products only report `energy_100g` in kJ. `resolvedKcal100g` converts kJ÷4.184 automatically. |
| 33 | `sort_by=popularity` on search | Returns well-known branded products first, which tend to have complete nutrient data. Better than alphabetical or relevance-only ordering. |
| 34 | Concurrent `async`/`awaitAll` for all lookups | With 5 parsed foods, sequential lookups would take 5× the latency. Parallel launch completes as fast as the slowest individual lookup (~200–800ms total). |
| 35 | Skip catalog-hit items in lookup | Items already in the local catalog have known macros — no need to query OpenFoodFacts. Reduces unnecessary API calls. |
| 36 | `computeServingMultiplier()` in ViewModel | Converts quantity+unit to a ×multiplier against 100g so per-100g values scale correctly. Covers g, ml, tsp, tbsp, cup; defaults to 1.0 for unknown units. |
| 37 | `NutritionLookupState` sealed class | Distinguishes Loading / Found / NotFound / Error explicitly — enables correct UI rendering without null-checking ambiguity. |
| 38 | Phase 5 logic isolated to `lookupNutrition()` | Called at the end of `resolveCatalogCache()` — all catalog resolution completes first, so lookup skipping is accurate. |
| 39 | Recipe ingredients persisted to catalog on "Log All" | If an ingredient has OpenFoodFacts data and is new (not in catalog), it's inserted to the Ingredients catalog so future sessions benefit from the cache — not just the current session. |
| 40 | `externalApiId` stored in `FoodItem` | Enables future phases (or UX enhancement) to re-fetch and refresh nutrition data for a food item without repeating the search. |

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 15s`
- [x] `testDebugUnitTest` — `BUILD SUCCESSFUL in 1s` (no regressions)
- [x] All Phase 4.5 functionality preserved

### Post-Implementation Issues Found & Fixed

| # | Issue | Root Cause | Fix |
|---|-------|-----------|-----|
| 1 | **Error badge showed "Nutrition lookup failed" without detail** | `NutritionStatusRow` in `LogScreen.kt` used a hardcoded string instead of `nutritionState.message` | Changed `Text` to display `nutritionState.message` with `maxLines = 2`, `TextOverflow.Ellipsis`; added `Log.e` throughout `NutritionRepositoryImpl` with a separate `IOException` catch for SSL/connect/socket errors |
| 2 | **OpenFoodFacts blocked on Walmart corporate VPN** | `world.openfoodfacts.org` is blocked by the Walmart web filter (`proxy-intlho.wal-mart.com:8080`). VPN is the full-tunnel internet gateway (no VPN = no internet at all). `generativelanguage.googleapis.com` (Gemini) is explicitly allowlisted, but the OpenFoodFacts French non-profit domain is not. | Updated `LookupNutritionUseCase` to degrade network failures gracefully: `Resource.Error` → `Resource.Success(null)` → `NutritionLookupState.NotFound` → "No nutrition data found — fill in manually". Only HTTP 429 (actionable rate-limit) is surfaced as `Resource.Error`. Actual failures still logged via `Log.w`. |

**Graceful degradation logic in `LookupNutritionUseCase.kt`:**
```kotlin
is Resource.Error -> {
    if (result.message.contains("rate-limited", ignoreCase = true)) {
        Resource.Error(result.message, result.throwable)
    } else {
        Log.w(TAG, "Nutrition lookup for \"$foodName\" unavailable: ${result.message}")
        Resource.Success(null) // Degrade to NotFound — non-disruptive
    }
}
```
- [x] `assembleDebug` after graceful degradation fix — `BUILD SUCCESSFUL in 11s`

### Food Database: OpenFoodFacts Limitations & Indian/South Asian Coverage

**Problem:** OpenFoodFacts coverage of Indian/South Asian ingredients and packaged foods is sparse. Additionally, `world.openfoodfacts.org` is blocked by Walmart's corporate proxy, making it non-functional on the VPN.

**Evaluated Alternatives:**

| Database | Domain | Cost | Indian Coverage | Proxy-Friendly | Status |
|----------|--------|------|-----------------|----------------|--------|
| **USDA FoodData Central** | `api.nal.usda.gov` | Free (API key, 1000 req/h) | ✅ Good for raw ingredients (dal, paneer, ghee, basmati, turmeric) | ✅ Likely — `.gov` domain, typically allowlisted in enterprise filters | **Recommended primary** |
| **IFCT 2017 (Indian Food Composition Tables)** | N/A — bundled locally | Free — open dataset | ✅✅ Excellent — ~900 Indian foods from India's National Institute of Nutrition (NIN) | ✅ No network — CSV embedded in app assets | **Recommended offline fallback** |
| **OpenFoodFacts India** | `in.openfoodfacts.org` | Free | ✅ Packaged Indian brands (Maggi, Amul, Haldirams, MDH) | ❌ Same blocked domain as `world.openfoodfacts.org` | Blocked on VPN |
| **Nutritionix** | `trackapi.nutritionix.com` | Free tier (500 req/day) | ✅ Decent | Unknown | Requires account/API key |

**Recommended Architecture for Phase 5.5:**
1. **USDA FoodData Central** — primary network lookup (`.gov` domain most likely to pass corporate filter)
2. **IFCT 2017 bundled CSV** — offline fallback (no network required, ~900 Indian food entries, loaded into Room on first launch)
3. **Manual entry** — always available as final fallback

**IFCT 2017 Data:**
- Publisher: National Institute of Nutrition (NIN), Hyderabad, India
- Dataset: `ifct2017` (available as npm package or raw CSV download)
- ~900 entries: raw + cooked Indian foods, regional dishes, spices, pulses, grains
- Fields map cleanly to `NutritionInfo` per-100g model

**USDA FDC API:**
- Base URL: `https://api.nal.usda.gov/fdc/v1/`
- Endpoint: `GET /foods/search?query=eggs&api_key={key}`
- Free API key: apply at [fdc.nal.usda.gov](https://fdc.nal.usda.gov/api-guide.html)
- Response fields: `fdcId`, `description`, `brandOwner`, `foodNutrients[].nutrientName`, `foodNutrients[].value`
- Nutrient names to map: `"Energy"` (kcal), `"Protein"`, `"Carbohydrate, by difference"`, `"Total lipid (fat)"`, `"Fiber, total dietary"`

**Implementation effort (Phase 5.5):**
- New `FoodDataCentralApiService.kt` (Retrofit interface)
- New `FoodDataCentralResponse.kt` DTO (different field structure from OFFs)
- Update `NutritionRepositoryImpl` or add `FDCNutritionRepositoryImpl`
- Add `USDA_FDC_API_KEY` to `local.properties` + `BuildConfig`
- Bundle IFCT 2017 CSV in `assets/` + `IFCTLoader` to seed a Room table on first launch
- Update `di/AppModule.kt` with `@Named("fdc")` Retrofit + client
- Everything above `NutritionRepository` interface (UseCase, ViewModel, UI) **unchanged**

### Tests (Pending)
- [ ] `NutritionRepositoryImpl` maps OFFs response to `NutritionInfo` correctly
- [ ] `NutritionRepositoryImpl` handles `energy-kcal_100g` hyphen field correctly
- [ ] `NutritionRepositoryImpl` falls back to kJ conversion when kcal field is absent
- [ ] `LookupNutritionUseCase` returns `Resource.Success(null)` for blank input
- [ ] `LookupNutritionUseCase` degrades non-rate-limit errors to `Resource.Success(null)`
- [ ] `computeServingMultiplier()` unit coverage (g, tsp, tbsp, cups, ml, unknown)
- [ ] `acceptParsedFood()` uses catalog cache macros over nutrition when both available
- [ ] `acceptParsedFood()` uses nutrition macros scaled by multiplier when no catalog match
- [ ] `acceptAndLogAllParsed()` logs flat items with OFFs macros when lookup found
- [ ] `logRecipe()` inserts nutrition-enriched ingredients to catalog when `isFromCatalog=false`
- [ ] On-device: "2 eggs scrambled" → nutrition lookup finds egg data → macros auto-filled on Edit
- [ ] On-device: "Dosa and Chutney: ..." recipe → ingredient-level spinners → green checks when found
- [ ] On-device: Unknown food → "No nutrition data found — fill in manually" label displayed

---

## Phase 5.5: Food Database Migration — USDA FDC + IFCT 2017

**Status:** ✅ Completed
**Date:** April 30, 2026

### Summary
Replaced OpenFoodFacts with a two-tier nutrition lookup chain:

1. **USDA FoodData Central (FDC)** — primary online source. Free REST API backed by USDA's authoritative Foundation, SR Legacy, and Branded food databases. API key stored in `local.properties` as `USDA_FDC_API_KEY`. Results ranked by macro completeness (Foundation/SR Legacy preferred over Branded).
2. **IFCT 2017 bundled CSV** — offline fallback with 120 common Indian foods from India's National Institute of Nutrition (NIN, Hyderabad). Loaded into a dedicated Room table (`ifct_foods`) on first launch via an idempotent seeder; no network required.
3. **Manual entry** — always available as final fallback when both tiers return nothing.

The `NutritionRepository` interface, all UseCases, ViewModel, and UI are **completely unchanged** — the migration is fully contained in the data layer.

### Motivation
- `world.openfoodfacts.org` blocked by Walmart's corporate proxy
- OpenFoodFacts had sparse coverage of Indian/South Asian ingredients and dishes
- USDA FDC `.gov` domain passes corporate web filters
- IFCT 2017 eliminates any network dependency for common Indian foods

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `app/src/main/assets/ifct2017.csv` | Created | 120 NIN-verified Indian foods across 8 groups (grains, pulses, vegetables, fruits, dairy, meat/fish, nuts/oils, prepared dishes, sweets, beverages); per-100g macros: code, name, energy_kcal, protein_g, fat_g, carbs_g, fiber_g |
| 2 | `data/remote/api/FoodDataCentralApiService.kt` | Created | Retrofit `GET /fdc/v1/foods/search` — `query`, `api_key`, `dataType="Foundation,SR Legacy,Branded"`, `pageSize=5` params |
| 3 | `data/remote/dto/FoodDataCentralResponse.kt` | Created | DTOs — `FdcSearchResponse`, `FdcFood`, `FdcFoodNutrient`; nutrient ID constants (1008=kcal, 1003=protein, 1005=carbs, 1004=fat, 1079=fiber); `toNutritionInfo()` mapper; `resolvedBrand` prefers `brandName` over `brandOwner` |
| 4 | `data/local/entity/IfctFoodEntity.kt` | Created | Room entity for `ifct_foods` table; `toNutritionInfo()` mapper sets `source = "IFCT 2017 (Offline)"` |
| 5 | `data/local/dao/IfctFoodDao.kt` | Created | `searchByName(query, limit=5)` — SQL `LIKE '%query%'`; `searchByWord(word, limit=3)` — single-word fallback; `insertAll(IGNORE)`; `count()` for idempotency check |
| 6 | `util/IfctCsvLoader.kt` | Created | `@Singleton` seeder — checks `count()` first (no-op if > 0); opens `assets/ifct2017.csv`; skips header; parses each row with `NumberFormatException` guard; bulk inserts via `insertAll()`; runs on `Dispatchers.IO` |
| 7 | `data/repository/NutritionRepositoryImpl.kt` | Rewritten | Three-tier chain: (1) FDC — skipped if key blank; network/timeout failures fall through silently; only HTTP 403 surfaces as error; (2) IFCT — full-phrase then word-by-word fallback; (3) empty list. Seeds IFCT via `runCatching { ifctCsvLoader.seedIfNeeded() }` on every call (no-op after first launch) |
| 8 | `di/AppModule.kt` | Modified | Removed `@Named("openfoodfacts")` client/retrofit/service; added `@Named("fdc")` `OkHttpClient` (25s timeouts, `Proxy.NO_PROXY`), `@Named("fdc")` `Retrofit` (`https://api.nal.usda.gov/`), `FoodDataCentralApiService`, `@Named("fdcApiKey") String` from `BuildConfig.USDA_FDC_API_KEY` |
| 9 | `data/local/NutriAiDatabase.kt` | Modified | Added `IfctFoodEntity` to `@Database` entities; added `abstract fun ifctFoodDao(): IfctFoodDao`; bumped `version = 1 → 2` |
| 10 | `di/DatabaseModule.kt` | Modified | Added `provideIfctFoodDao(database): IfctFoodDao` |
| 11 | `util/Constants.kt` | Modified | Added `USDA_FDC_BASE_URL = "https://api.nal.usda.gov/"`; deprecated `OPEN_FOOD_FACTS_BASE_URL` |
| 12 | `domain/model/NutritionInfo.kt` | Modified | Updated `source` default to `"USDA FoodData Central"`; updated KDoc for Phase 5.5 |
| 13 | `app/build.gradle.kts` | Modified | Added `buildConfigField("String", "USDA_FDC_API_KEY", ...)` — reads from `local.properties` |
| 14 | `local.properties.example` | Modified | Added `USDA_FDC_API_KEY=your_usda_fdc_api_key_here` with signup URL |

**Unchanged (zero modifications needed):**
- `domain/repository/NutritionRepository.kt`
- `domain/usecase/LookupNutritionUseCase.kt`
- All ViewModel and UI files

### Key Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 41 | Lookup chain inside single `NutritionRepositoryImpl` | Cleaner than two separate repository impls with a coordinator; the domain interface has one method so one impl is appropriate |
| 42 | Nutrient IDs instead of nutrient name strings | FDC returns both `nutrientId` (int) and `nutrientName` (string). IDs are stable across API versions; names can change ("Energy" vs "Energy (Atwater General Factors)") |
| 43 | `runCatching` around `seedIfNeeded()` in repository | IFCT seeding failure should never crash a nutrition lookup — app degrades to empty results gracefully |
| 44 | Word-by-word fallback in IFCT search | Multi-word queries like "chicken breast" may not LIKE-match a stored name like "Chicken breast". Splitting and searching individual words finds the best partial match. |
| 45 | FDC HTTP 403 surfaces as error, other failures fall through | 403 = invalid/expired API key — actionable for developer. Timeout/DNS/IO = environment issue — fall through to IFCT silently |
| 46 | `USDA_FDC_API_KEY` blank = FDC skipped gracefully | Allows app to work fully offline (IFCT only) during development without a FDC key configured. No crash, just a logcat warning. |
| 47 | DB version 1 → 2 with `fallbackToDestructiveMigration()` | Pre-release; adding `ifct_foods` table. Destructive migration is acceptable — all user data is recreated by the seeder on next launch. |

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 43s`
- [x] IFCT seeding verified on-device — 120 foods loaded from CSV on first launch
- [x] IFCT search verified — common Indian foods (rice, dal, roti, paneer, chicken) return correct per-100g values

### Pending
- [ ] Add `USDA_FDC_API_KEY` to `local.properties` (free key — https://fdc.nal.usda.gov/api-key-signup.html)
- [ ] On-device: verify FDC returns results for western foods (eggs, oats, banana) when key is present
- [ ] Unit test: `IfctCsvLoader` skips seeding when `count() > 0`
- [ ] Unit test: `NutritionRepositoryImpl` falls through to IFCT on FDC `UnknownHostException`

---

## Phase 6: Authentication & Supabase Cloud Sync

**Status:** ✅ Completed
**Date:** April 30, 2026

### Summary
Implemented Supabase email/password authentication (GoTrue REST API) and offline-first background cloud sync (PostgREST + WorkManager). Auth is **additive** — the app continues to work fully offline without sign-in. The "Profile" tab shows a sign-in/sign-up form when unauthenticated and a profile panel with manual sync and sign-out controls when authenticated.

**Key design decisions:**
- **No Supabase Kotlin SDK** — Supabase is accessed via its public REST APIs using the existing Retrofit + OkHttp + Kotlinx Serialization stack. Avoids adding Ktor as a second HTTP client.
- **DataStore for session persistence** — JWT + refresh token + user info stored in DataStore (`auth_prefs`). Survives process death; loaded on app start to restore auth state.
- **In-memory token cache** — `AuthPreferences.getCachedToken()` provides synchronous token access for the OkHttp interceptor (can't use `suspend` in interceptors).
- **User ID translation** — Local Room uses `"local_user"` as user ID; Supabase uses the real UUID. Translation happens in `SyncRepositoryImpl` (push: `local_user` → UUID; pull: UUID → `local_user`). All upstream code (ViewModels, UseCases) is unchanged.
- **Catalog ID prefixing** — Fixed local catalog IDs (`"local_user_recipes"`, `"local_user_ingredients"`) are prefixed with the Supabase UUID in the remote schema (`"{uuid}_local_user_recipes"`) to prevent PRIMARY KEY collisions across users.
- **Offline-first sync** — Push unsynced rows → Supabase; pull remote rows → Room (LWW). Soft-delete tombstones are pushed so deletions propagate across devices.
- **`@HiltWorker`** — `SyncWorker` is injected by Hilt. Required `NutriAiApplication` to implement `Configuration.Provider` + `HiltWorkerFactory`, and disabling the default WorkManager `InitializationProvider` in the Manifest.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `util/Constants.kt` | Modified | Added `SYNC_WORK_TAG`, `AUTH_PREFS_NAME`, `REMOTE_CATALOG_ID_SEPARATOR` |
| 2 | `domain/model/AuthState.kt` | Created | Sealed class — `Loading`, `Unauthenticated`, `Authenticated(userId, email)` |
| 3 | `data/remote/dto/SupabaseAuthDto.kt` | Created | GoTrue request DTOs (`SignUpRequest`, `SignInRequest`, `RefreshTokenRequest`); response DTOs (`GoTrueResponse`, `GoTrueUser`, `GoTrueError`) |
| 4 | `data/remote/dto/SupabaseSyncDto.kt` | Created | PostgREST DTOs (`RemoteUserDto`, `RemoteCatalogDto`, `RemoteFoodItemDto`, `RemoteDailyLogDto`); entity↔DTO mapper extensions (push + pull directions) |
| 5 | `data/remote/api/SupabaseAuthApiService.kt` | Created | Retrofit interface for GoTrue: `signUp`, `signIn`, `refreshToken`, `signOut` |
| 6 | `data/remote/api/SupabaseDbApiService.kt` | Created | Retrofit interface for PostgREST: upsert + fetch for users/catalogs/food_items/daily_logs |
| 7 | `data/local/preferences/AuthPreferences.kt` | Created | DataStore-backed session storage; `sessionFlow`, `saveSession`, `clearSession`, `updateAccessToken`, `setLastSyncAt`; in-memory `cachedToken` for OkHttp interceptor |
| 8 | `domain/repository/AuthRepository.kt` | Created | Interface: `getAuthStateFlow()`, `signUp`, `signIn`, `signOut`, `refreshSession` |
| 9 | `domain/repository/SyncRepository.kt` | Created | Interface: `pushLocalChanges`, `pullRemoteChanges`, `syncAll` |
| 10 | `data/repository/AuthRepositoryImpl.kt` | Created | Implements `AuthRepository` — GoTrue REST calls, DataStore session persistence, GoTrue error parsing, token refresh |
| 11 | `data/repository/SyncRepositoryImpl.kt` | Created | Implements `SyncRepository` — push unsynced entities → Supabase, pull remote → Room with LWW; ID translation: `local_user` ↔ UUID, local catalog IDs ↔ prefixed remote IDs |
| 12 | `domain/usecase/SignInUseCase.kt` | Created | Input validation + delegates to `AuthRepository.signIn()` |
| 13 | `domain/usecase/SignUpUseCase.kt` | Created | Input validation (email, password length, confirm match) + `AuthRepository.signUp()` |
| 14 | `domain/usecase/SignOutUseCase.kt` | Created | Best-effort final push before sign-out + `AuthRepository.signOut()` |
| 15 | `domain/usecase/GetAuthStateUseCase.kt` | Created | Exposes `AuthRepository.getAuthStateFlow()` to the presentation layer |
| 16 | `domain/usecase/SyncDataUseCase.kt` | Created | Resolves current auth state; calls `SyncRepository.syncAll()` with the UUID; returns error if not authenticated |
| 17 | `work/SyncWorker.kt` | Created | `@HiltWorker` `CoroutineWorker` — calls `SyncDataUseCase`; returns `Result.retry()` on transient errors, `Result.failure()` on auth errors |
| 18 | `work/SyncScheduler.kt` | Created | Object — `schedule()` enqueues periodic sync (12 h, network connected); `cancel()` cancels it on sign-out |
| 19 | `presentation/screens/auth/AuthViewModel.kt` | Created | `@HiltViewModel` — `AuthUiState` + `AuthEvent`; collects `GetAuthStateUseCase` flow; `signIn()`, `signUp()`, `signOut()`, `syncNow()`, form field handlers; schedules sync on sign-in |
| 20 | `presentation/screens/auth/AuthScreen.kt` | Replaced | 3-panel `AnimatedContent`: Loading spinner, Sign-In/Sign-Up form (with mode toggle), Profile card (email, sync button, sign-out). All 3 preview variants included. |
| 21 | `di/SupabaseModule.kt` | Created | Hilt `@Module` — `DataStore<Preferences>`, `@Named("supabase")` `OkHttpClient` (apikey + JWT interceptor, `Proxy.NO_PROXY`), `@Named("supabase")` Retrofit, `SupabaseAuthApiService`, `SupabaseDbApiService`, `WorkManager` |
| 22 | `di/DatabaseModule.kt` | Modified | Added `@Binds` for `AuthRepository → AuthRepositoryImpl` and `SyncRepository → SyncRepositoryImpl` |
| 23 | `presentation/navigation/NutriAiNavHost.kt` | Modified | `AuthScreen` now receives `onNavigateToHome` — navigates to Home on sign-in success; uses `popUpTo` + `launchSingleTop` |
| 24 | `NutriAiApplication.kt` | Modified | Implements `Configuration.Provider`; injects `HiltWorkerFactory`; supplies custom `WorkManager` configuration for `@HiltWorker` support |
| 25 | `app/src/main/AndroidManifest.xml` | Modified | Disables default `WorkManagerInitializer` via `tools:node="remove"` so custom `HiltWorkerFactory` configuration is used |

### Supabase Database Schema (run in Supabase Dashboard → SQL Editor)

```sql
-- Users table (links GoTrue UUID to profile data)
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

-- Catalogs (prefixed IDs prevent PK collision across users)
CREATE TABLE IF NOT EXISTS public.catalogs (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    last_modified_at BIGINT NOT NULL,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at BIGINT
);

-- Food items
CREATE TABLE IF NOT EXISTS public.food_items (
    id TEXT PRIMARY KEY,
    catalog_id TEXT NOT NULL REFERENCES public.catalogs(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    brand TEXT,
    base_serving_g REAL NOT NULL,
    base_calories REAL NOT NULL,
    base_protein REAL NOT NULL,
    base_carbs REAL NOT NULL,
    base_fat REAL NOT NULL,
    external_api_id TEXT,
    last_modified_at BIGINT NOT NULL,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at BIGINT
);

-- Daily logs
CREATE TABLE IF NOT EXISTS public.daily_logs (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    food_item_id TEXT NOT NULL REFERENCES public.food_items(id),
    date_timestamp BIGINT NOT NULL,
    consumed_qty REAL NOT NULL,
    consumed_unit TEXT NOT NULL,
    total_calories REAL NOT NULL,
    total_protein REAL NOT NULL,
    total_carbs REAL NOT NULL,
    total_fat REAL NOT NULL,
    last_modified_at BIGINT NOT NULL,
    is_synced BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at BIGINT
);

-- Row Level Security (restrict each user to their own data)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.catalogs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.food_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users own data" ON public.users USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
CREATE POLICY "Users own catalogs" ON public.catalogs USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY "Users own food items" ON public.food_items
    USING (catalog_id IN (SELECT id FROM public.catalogs WHERE user_id = auth.uid()))
    WITH CHECK (catalog_id IN (SELECT id FROM public.catalogs WHERE user_id = auth.uid()));
CREATE POLICY "Users own logs" ON public.daily_logs USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
```

**Important Supabase dashboard settings:**
- Go to **Authentication → Providers → Email**: disable "Confirm email" for development (or handle email verification in Phase 7).

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 22s`
- [x] `assembleDebug` (after HiltWorker + Manifest fix) — `BUILD SUCCESSFUL in 8s`
- [x] Runtime test: sign up with real Supabase project (email confirmation flow)
- [x] Runtime test: sign in → manual sync → data appears in Supabase Table Editor (2 catalogs, 3 food items, 4 daily logs, 1 user row)
- [x] Runtime test: session persistence — force-close + reopen lands on Profile panel (still signed in)
- [x] Runtime test: offline Sync Now → "No internet connection" error (fixed in `SyncDataUseCase` — added `NET_CAPABILITY_VALIDATED` connectivity check)
- [x] Runtime test: wrong password → inline error message shown, no crash
- [x] WorkManager periodic task `nutriai_sync_work` confirmed in App Inspection → Background Task Inspector
- [x] Fixed HTTP 403 — required manual `GRANT` + `CREATE TABLE` + RLS policies in Supabase SQL Editor; tables were not auto-created because "Expose tables via API" was disabled at project creation
- [x] Fixed false-positive "Sync complete" when offline — `SyncDataUseCase` now checks `ConnectivityManager` before any network call

### Architecture Notes

| # | Decision | Rationale |
|---|----------|-----------|
| 48 | Retrofit + OkHttp for Supabase (no Supabase SDK) | Avoids adding Ktor as a second HTTP client alongside existing OkHttp/Retrofit. Supabase is standard REST — GoTrue + PostgREST are fully accessible via Retrofit interfaces |
| 49 | Auth is additive (no sign-in gate) | App works fully offline without auth. Profile tab drives sign-in. Local data persists through sign-out. |
| 50 | In-memory token cache in `AuthPreferences` | OkHttp interceptors are synchronous — can't call DataStore's `suspend` read. A `@Volatile` in-memory copy updated on `saveSession`/`clearSession` bridges the gap |
| 51 | Catalog ID prefixing in remote schema | Fixed local IDs (`"local_user_recipes"`) would collide in Supabase's TEXT PRIMARY KEY across users. Prefix `{uuid}_` guarantees uniqueness while RLS ensures users can only access their own rows |
| 52 | `LOCAL_USER_ID` translation in `SyncRepositoryImpl` | All UseCases and ViewModels use `LOCAL_USER_ID = "local_user"`. The translation layer is entirely in the sync repo — zero changes needed in the rest of the app |
| 53 | `Configuration.Provider` + `HiltWorkerFactory` | Required for `@HiltWorker` Hilt-injected workers. Default `WorkManagerInitializer` is removed from Manifest so the custom Hilt config is used |
| 54 | `ExistingPeriodicWorkPolicy.KEEP` | Safe to call `SyncScheduler.schedule()` on every app launch — no-op if already enqueued. Prevents timer reset on each launch |

---

## Phase 7: Polish, Error Handling & Production Readiness

**Status:** ✅ Completed
**Date:** May 2, 2026

### Summary
Implemented all high- and medium-priority polish items: JWT pre-emptive token refresh before every sync, delete confirmation dialogs on both Home and Catalog screens, an animated offline connectivity banner on Home and Catalog, a snackbar for sync results (replacing inline card text), and fully user-configurable daily macro goals stored in DataStore (accessible via the Profile tab). Verified all existing functionality preserved — `assembleDebug BUILD SUCCESSFUL in 21s`.

Items confirmed as **already implemented** in earlier phases (no work needed in Phase 7):
- Empty states — `EmptyLogState` (Home) and `EmptyCatalogState` (Catalog) were added in Phase 3
- Date navigation — `DateNavigationHeader` with prev/next arrows already in `HomeScreen` since Phase 3
- Edit log entry — full `EditLogSheet` bottom sheet already wired in `HomeViewModel`/`HomeScreen` since Phase 6
- Accessibility content descriptions — all `IconButton`s already have `contentDescription` from earlier phases

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `domain/repository/AuthRepository.kt` | Modified | Added `refreshSessionIfNeeded(bufferMs)` to interface |
| 2 | `data/repository/AuthRepositoryImpl.kt` | Modified | Implemented `refreshSessionIfNeeded()` — reads `expiresAt` from `AuthPreferences`, calls `refreshSession()` if token expires within `bufferMs` (default 5 min), returns `Resource.Success` immediately if token is fresh or no session exists |
| 3 | `domain/usecase/SyncDataUseCase.kt` | Modified | Calls `authRepository.refreshSessionIfNeeded()` before `syncRepository.syncAll()` — surfaces "Session expired" as an error to stop the sync rather than silently failing with 401 |
| 4 | `presentation/components/ConfirmDeleteDialog.kt` | Created | Reusable Material3 `AlertDialog` with delete icon, configurable title/message, red **Delete** button and **Cancel** button; both close the dialog |
| 5 | `presentation/screens/home/HomeViewModel.kt` | Modified | Added `_pendingDeleteLogId: MutableStateFlow<String?>` + `requestDeleteLog()` / `confirmDeleteLog()` / `cancelDeleteLog()` — staging pattern keeps dialog dismissal separate from the actual soft-delete coroutine |
| 6 | `presentation/screens/home/HomeScreen.kt` | Modified | Uses `requestDeleteLog` instead of direct `deleteLog`; shows `ConfirmDeleteDialog` when `pendingDeleteLogId != null` with food name resolved from `uiState.foodNames` |
| 7 | `presentation/screens/catalog/CatalogViewModel.kt` | Modified | Added `_pendingDeleteFoodId: MutableStateFlow<String?>` + `requestDeleteFood()` / `confirmDeleteFood()` / `cancelDeleteFood()` |
| 8 | `presentation/screens/catalog/CatalogScreen.kt` | Modified | Shows `ConfirmDeleteDialog` when `pendingDeleteFoodId != null`; uses `requestDeleteFood` from onDeleteFood callback |
| 9 | `util/ConnectivityObserver.kt` | Created | `@Singleton` — wraps `ConnectivityManager.NetworkCallback` in a `callbackFlow<Boolean>` that emits validated connectivity state; `distinctUntilChanged()` to avoid redundant recompositions; emits current state immediately on first collection |
| 10 | `presentation/components/OfflineBanner.kt` | Created | Animated (`expandVertically` / `shrinkVertically`) surface using `errorContainer` color; shows WifiOff icon + "No internet connection — changes are saved locally" |
| 11 | `presentation/screens/home/HomeViewModel.kt` | Modified | Injects `ConnectivityObserver`; exposes `isOnline: StateFlow<Boolean>` via `stateIn(WhileSubscribed(5s))` |
| 12 | `presentation/screens/home/HomeScreen.kt` | Modified | Collects `isOnline`; passes to `HomeScreenContent`; renders `OfflineBanner` as first item in `LazyColumn` |
| 13 | `presentation/screens/catalog/CatalogViewModel.kt` | Modified | Injects `ConnectivityObserver`; exposes `isOnline: StateFlow<Boolean>` |
| 14 | `presentation/screens/catalog/CatalogScreen.kt` | Modified | Collects `isOnline`; passes to `CatalogScreenContent`; renders `OfflineBanner` at top of column |
| 15 | `presentation/screens/auth/AuthScreen.kt` | Modified | Added `LaunchedEffect(uiState.lastSyncMessage)` to show sync result in snackbar (same `SnackbarHostState` used for errors); removed inline sync-result card text from `ProfilePanel`; cleaned up `CheckCircle` import |
| 16 | `domain/model/MacroGoals.kt` | Created | Domain model `data class MacroGoals(calorieGoal, proteinGoal, carbsGoal, fatGoal)` with sensible defaults |
| 17 | `data/local/preferences/AuthPreferences.kt` | Modified | Added four `floatPreferencesKey` constants for macro goals; added `macroGoalsFlow: Flow<MacroGoals>` and `suspend fun saveMacroGoals(goals: MacroGoals)` |
| 18 | `presentation/screens/home/HomeViewModel.kt` | Modified | Injects `AuthPreferences`; exposes `macroGoals: StateFlow<MacroGoals>` |
| 19 | `presentation/screens/home/HomeScreen.kt` | Modified | Collects `macroGoals`; passes `calorieGoal/proteinGoal/carbsGoal/fatGoal` to `MacroSummaryCard` — progress arcs now reflect user-configured targets |
| 20 | `presentation/screens/auth/AuthViewModel.kt` | Modified | Injects `AuthPreferences`; exposes `macroGoals: StateFlow<MacroGoals>`; adds `saveMacroGoals(goals)` |
| 21 | `presentation/screens/auth/AuthScreen.kt` | Modified | Added `MacroGoalsSheetContent` composable (editable calorie/protein/carbs/fat fields with validation); added `showGoalsSheet` state; added "Nutrition Goals" card in `ProfilePanel` with "Edit" button; added "Set Nutrition Goals" text button in `AuthFormPanel` (accessible even when not signed in); wires `ModalBottomSheet` + `AuthViewModel.saveMacroGoals()` |

### Key Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 55 | Pre-emptive JWT refresh in `SyncDataUseCase`, not OkHttp Authenticator | Avoids circular DI dependency: `OkHttpClient` → `Authenticator` → `AuthRepository` → `SupabaseAuthApiService` → `Retrofit` → `OkHttpClient`. Pre-emptive refresh in the UseCase is clean, testable, and covers the primary failure case (sync after ~1 hour idle) |
| 56 | Pending-delete staging pattern (`requestDelete` → `confirmDelete`) | Separates UI state (dialog visible) from side-effect (coroutine soft-delete). `_pendingDeleteLogId` is a separate `MutableStateFlow` — not embedded in `uiState` which is recomputed from database Flows and would reset the dialog state on every emission |
| 57 | `ConnectivityObserver` as `@Singleton` | Single `NetworkCallback` registration shared across all consumers (Home + Catalog ViewModels). Both get the same `@Singleton` instance from Hilt so there is exactly one callback registered at a time |
| 58 | Offline banner in `LazyColumn` first item (Home) / Column top (Catalog) | Animates inside the scroll area so it doesn't cause a layout jump on the `Scaffold` content. `expandVertically` / `shrinkVertically` gives a smooth push-down effect on the content below |
| 59 | Macro goals as `Float` in DataStore | DataStore `Preferences` has no `Double` key type. `Float` is sufficient for per-day nutrition targets (sub-gram precision is irrelevant). Values are converted to `Double` on read so `MacroGoals` keeps its `Double` domain type unchanged |
| 60 | Goals accessible from sign-in screen (unauthenticated) | Offline-first design — macro goals are local app settings, not auth-dependent. Users who never sign in should still be able to customize their targets |

### Build Verification
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 21s`
- [x] All Phase 6 functionality preserved
- [x] All Phase 5.5 IFCT/FDC nutrition lookup preserved

### Tests (Pending)
- [ ] `AuthRepositoryImpl.refreshSessionIfNeeded()` — returns `Resource.Success(Unit)` when token has >5 min remaining
- [ ] `AuthRepositoryImpl.refreshSessionIfNeeded()` — calls `refreshSession()` when token expires in <5 min
- [ ] `SyncDataUseCase` — surfaces "Session expired" error when refresh fails
- [ ] `HomeViewModel.requestDeleteLog` → `pendingDeleteLogId` non-null → `confirmDeleteLog` calls `deleteLogUseCase`
- [ ] `ConnectivityObserver` — emits `false` when `NET_CAPABILITY_VALIDATED` is absent
- [ ] `MacroGoals` saved to DataStore persist across ViewModel recreation
- [ ] `MacroSummaryCard` progress arcs reflect updated goals immediately after save

### Post-Phase 7 Fix: Known Issue #7 — "Unknown Food" after soft-delete

**Date:** May 2, 2026  
**Status:** ✅ Fixed

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `domain/model/DailyLog.kt` | Modified | Added `foodName: String = ""` — snapshotted display name embedded at log creation |
| 2 | `data/local/entity/DailyLogEntity.kt` | Modified | Added `food_name TEXT NOT NULL DEFAULT ''` column with `@ColumnInfo(defaultValue = "")` |
| 3 | `data/local/mapper/DailyLogMapper.kt` | Modified | Both `toDomain()` and `toEntity()` now map `foodName` |
| 4 | `domain/usecase/LogFoodUseCase.kt` | Modified | `invoke()` passes `foodName = foodName.trim()`; `logRecipe()` passes `foodName = recipeName.trim()` to every `DailyLog` created |
| 5 | `presentation/screens/home/HomeViewModel.kt` | Modified | Removed `FoodRepository` constructor injection; removed `foodNames: Map` from `uiState` computation; `startEditLog()` reads `log.foodName` directly |
| 6 | `presentation/screens/home/HomeViewModel.kt` | Modified | Removed `foodNames: Map<String, String>` field from `HomeUiState` |
| 7 | `presentation/screens/home/HomeScreen.kt` | Modified | `FoodLogItem` call uses `log.foodName.ifBlank { "Unknown Food" }`; delete dialog uses `log.foodName.ifBlank { null } ?: "this entry"`; preview `DailyLog`s updated with `foodName` |
| 8 | `data/local/NutriAiDatabase.kt` | Modified | Version bumped 2 → 3 |
| 9 | `data/remote/dto/SupabaseSyncDto.kt` | Modified | `RemoteDailyLogDto` + both mapper fns (`toRemoteDto`, `toEntity`) include `food_name` field with `= ""` default for backward-compat pulls |

**Key decision:** Snapshot pattern over join lookup. Food names are display data, not foreign-key-dependent data. Snapshotting `foodName` at write time is the correct approach — the same pattern used by order history in e-commerce systems. The `FoodItem.name` can evolve independently without corrupting historical log display.

**Supabase schema migration required:**
```sql
ALTER TABLE public.daily_logs ADD COLUMN IF NOT EXISTS food_name TEXT NOT NULL DEFAULT '';
```

**Build & Validation:**
- [x] `assembleDebug` — `BUILD SUCCESSFUL in 10s`
- [x] Supabase `food_name` column added via SQL Editor
- [x] `assembleRelease` — R8/ProGuard verified, no crashes
- [x] On-device: log food → soft-delete from Catalog → Home screen still shows correct food name (not "Unknown Food")

---

## Phase 8 Pre-work: UI / Design System Redesign

**Status:** ✅ Completed  
**Date:** May 2, 2026

### Summary

Full visual redesign of NutriAI from a generic Material3 green/teal palette to a premium, organic **"Forest & Cream"** aesthetic inspired by nature-forward health app designs. Done *before* Phase 8 feature work so that all new screens (charts, barcode scanner, widgets) are built on the finalized design system from day one.

**What changed at a glance:**
- **Colors:** Deep forest green primary, lime secondary, warm amber tertiary — all on a warm cream background (not clinical white)
- **Font:** Outfit (Google Fonts, OFL) — a geometric, humanist sans-serif replacing the system default (Roboto)
- **Shapes:** Softer, rounder corners (16 dp medium, 24 dp large) for an organic feel
- **Dynamic color disabled:** Brand palette is now always consistent regardless of wallpaper (can be re-enabled in a future settings screen)
- **Macro colors refreshed:** Tomato-orange calories, teal-green protein, honey-amber carbs, fig-purple fat

### Design Palette — "Forest & Cream"

| Role | Light Theme | Dark Theme |
|------|------------|------------|
| Primary | `#2D5A27` Forest Green | `#A8D99C` Forest Green Light |
| Primary Container | `#D4E8C8` Sage | `#3A4F34` Sage Dark |
| Secondary | `#6B9B37` Lime | `#C0E17E` Lime Light |
| Secondary Container | `#E8F5E1` Pale Lime | `#2A3D24` |
| Tertiary | `#F9A825` Amber | `#FFD95A` Amber Light |
| Background | `#FEFBF3` Warm Cream | `#0F1A0D` Dark Forest |
| Surface | `#FFFDF7` Cream Surface | `#1E2A1C` Dark Surface |
| Surface Variant | `#EEF2E6` Sage Gray | `#2A3828` Dark Variant |
| On-Surface | `#1A1C18` Charcoal | `#E0E8DC` |
| **CalorieColor** | `#E8673C` Tomato-orange | *(same)* |
| **ProteinColor** | `#2E8B7A` Teal-green | *(same)* |
| **CarbsColor** | `#D4A017` Honey-amber | *(same)* |
| **FatColor** | `#8E5BA2` Fig-purple | *(same)* |

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `app/src/main/res/font/outfit_regular.ttf` | Created | Outfit Regular (weight 400) — downloaded from Google Fonts CDN (OFL license) |
| 2 | `app/src/main/res/font/outfit_medium.ttf` | Created | Outfit Medium (weight 500) |
| 3 | `app/src/main/res/font/outfit_semibold.ttf` | Created | Outfit SemiBold (weight 600); also mapped to Bold slot (no separate Bold file needed) |
| 4 | `presentation/theme/Color.kt` | Rewritten | Full "Forest & Cream" palette — `ForestGreen`, `Sage`, `Lime`, `PaleLime`, `Amber`, `PaleAmber`, `Cream`, `CreamSurface`, `SageGray`, dark-theme counterparts, refreshed macro colors |
| 5 | `presentation/theme/Type.kt` | Rewritten | `OutfitFontFamily` defined with 3 font resource references; all 12 Material3 typography roles updated from `FontFamily.Default` → `OutfitFontFamily`; letter-spacing tuned for Outfit's geometry |
| 6 | `presentation/theme/Theme.kt` | Rewritten | `LightColorScheme` and `DarkColorScheme` rewritten with new tokens; dynamic color **removed** (consistent brand identity); custom `NutriAiShapes` added (extraSmall 6dp → extraLarge 32dp); `NutriAiTheme()` signature simplified (no `dynamicColor` param) |
| 7 | `app/src/main/res/values/themes.xml` | Modified | Splash window background → `@color/window_background` (cream); `windowLightStatusBar = true`; transparent status + nav bars for edge-to-edge Compose |
| 8 | `app/src/main/res/values/colors.xml` | Created | Centralised XML color resources: `window_background = #FEFBF3`, `ic_launcher_background = #2D5A27` (forest green) |
| 9 | `app/src/main/res/values/ic_launcher_background.xml` | Deleted | Consolidated into `colors.xml` (was standalone file defining only one color) |

**Zero changes needed in any screen or component file:** all 6 files that import `CalorieColor`, `ProteinColor`, `CarbsColor`, `FatColor` pick up the new hex values automatically. All screens use `MaterialTheme.colorScheme.*` tokens which resolve through the updated schemes.

### Key Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 61 | Theming before Phase 8 features | All new Phase 8 screens (charts, barcode, widgets) will be built on the finalized palette from day one — no retroactive restyling needed |
| 62 | Outfit font (embedded TTF, not downloadable) | Downloadable Fonts API requires Google Play Services; embedded TTF works on all devices including emulators and CI. OFL license permits bundling |
| 63 | Dynamic color disabled by default | Brand identity is consistent regardless of wallpaper. Forest & Cream is the intentional aesthetic — Material You overrides would undermine it. Can be re-exposed as a toggle in Phase 8 settings |
| 64 | Warm cream backgrounds (`#FEFBF3`) instead of pure white | Matches the organic/natural health aesthetic from the inspiration images. Reduces eye strain and aligns with the "natural food" positioning |
| 65 | Macro colors kept as named constants, not theme tokens | `CalorieColor`, `ProteinColor`, etc. are semantic data-visualization colors — not part of the Material3 role system. Keeping them as `val` constants (still in `Color.kt`) is idiomatic for chart/data UI |
| 66 | Bold falls back to SemiBold in OutfitFontFamily | No separate Outfit-Bold.ttf is needed — Android's font synthesis is sufficient for `FontWeight.Bold` headings. Avoids bundling an extra 47KB file. |

### Build & Validation

- [x] `assembleDebug` — `BUILD SUCCESSFUL` — font resources compile, R.font references resolve, no unused import warnings
- [ ] Android Studio Compose Preview — open `HomeScreenPreview`, `MacroSummaryCardPreview`, `AuthScreenSignInPreview` to visually verify new palette
- [ ] On-device light theme — cream backgrounds, forest green buttons, Outfit font renders
- [ ] On-device dark theme — dark forest background, sage cards, lime accents
- [ ] Splash screen shows cream background (no white flash before first Compose frame)
- [ ] `assembleRelease` — R8/ProGuard: font resources survive minification

---

## Phase 8 Pre-work II: Sync Optimization & Schema Migrations

**Status:** ✅ Completed
**Date:** May 2, 2026

### Summary
Overhauled the sync infrastructure and schema migration system before beginning Phase 8 feature work. The previous implementation had a full-fetch pull (all rows every sync), a data-wiping destructive migration strategy, no server-side write guard, and no immediate push after local mutations. This phase fixes all of these gaps as Layer 1 (infrastructure) + Layer 2 (responsive sync + conflict safety).

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `data/local/migrations/Migrations.kt` | Created | Central Room migration registry. `MIGRATION_3_4` is a no-op to bootstrap the explicit migration system. `ALL` array passed to Room builder. |
| 2 | `data/local/NutriAiDatabase.kt` | Updated | Bumped schema version 3 → 4. Updated KDoc with version history. |
| 3 | `di/DatabaseModule.kt` | Updated | Replaced `.fallbackToDestructiveMigration()` with `.fallbackToDestructiveMigrationFrom(1, 2)` + `.addMigrations(*Migrations.ALL)`. Pre-release wipe now limited to v1/v2 only. |
| 4 | `data/remote/api/SupabaseDbApiService.kt` | Updated | Added optional `updatedAtFilter`, `limit`, `offset` query params to all three GET endpoints (catalogs, food_items, daily_logs) for incremental pull + pagination. |
| 5 | `data/remote/dto/SupabaseSyncDto.kt` | Updated | Added `updatedAt: String?` (ISO 8601 TIMESTAMPTZ) field to all three remote DTOs — populated by server-side Postgres trigger. |
| 6 | `data/local/preferences/AuthPreferences.kt` | Updated | Changed `LAST_SYNC_AT` from `Long` (epoch ms) to `String` (ISO 8601). Cursor now uses server clock to prevent clock skew. Updated `setLastSyncAt(isoTimestamp: String)`. |
| 7 | `data/repository/SyncRepositoryImpl.kt` | Updated | Incremental pull with `updated_at` cursor; paginated full pull (500-row pages) on first sync/reset; delete-wins conflict rule; tombstone purge after successful push; pull cursor advanced from max `updatedAt` in server response; `setLastSyncAt` now only fires when both push AND pull succeed. |
| 8 | `data/local/dao/DailyLogDao.kt` | Updated | Added `purgeSyncedTombstones()` and `getLogByIdIncludingDeleted()`. |
| 9 | `data/local/dao/FoodItemDao.kt` | Updated | Added `purgeSyncedTombstones()` and `getFoodByIdIncludingDeleted()`. |
| 10 | `data/local/dao/CatalogDao.kt` | Updated | Added `purgeSyncedTombstones()` and `getCatalogByIdIncludingDeleted()`. |
| 11 | `data/sync/SyncPushManager.kt` | Created | Debounced push-on-write manager (`@Singleton`). `schedulePush()` is fire-and-forget; multiple calls within 500 ms are coalesced. Triggers `pushLocalChanges()` after debounce; silently swallows network failures (dirty flag retries on next sync). |
| 12 | `data/repository/CatalogRepositoryImpl.kt` | Updated | Injected `SyncPushManager`; calls `schedulePush()` after `insertCatalog`, `updateCatalog`, `softDeleteCatalog`. |
| 13 | `data/repository/FoodRepositoryImpl.kt` | Updated | Injected `SyncPushManager`; calls `schedulePush()` after `insertFood`, `updateFood`, `softDeleteFood`. |
| 14 | `data/repository/DailyLogRepositoryImpl.kt` | Updated | Injected `SyncPushManager`; calls `schedulePush()` after `insertLog`, `updateLog`, `softDeleteLog`. |
| 15 | `presentation/ForegroundSyncViewModel.kt` | Created | Throttled foreground pull (max once per 5 min). `pullIfThrottled()` called on `onResume`; uses `SyncDataUseCase` for incremental pull + push of any remaining dirty rows. |
| 16 | `presentation/MainActivity.kt` | Updated | Registered `DefaultLifecycleObserver`; calls `foregroundSyncViewModel.pullIfThrottled()` on `onResume`. |
| 17 | `util/Constants.kt` | Updated | `SYNC_INTERVAL_HOURS` 12 → 24. Push-on-write + foreground pull handle the happy path; periodic job is now a backstop only. |
| 18 | `supabase/migrations/001_sync_infrastructure.sql` | Created | SQL migration for Supabase Dashboard: `updated_at` columns + auto-update triggers, LWW guard triggers (reject stale pushes), composite indexes for incremental pull queries. |

### Supabase SQL — Run in Dashboard before testing
File: `supabase/migrations/001_sync_infrastructure.sql`

Must be executed in Supabase Dashboard → SQL Editor before the `updated_at` incremental pull cursor will work. Until it's run, the app falls back gracefully (no `updated_at` values → cursor not advanced → full pull retried each time — data safe, just not optimized).

### Architecture Decisions Added
| # | Decision | Rationale |
|---|----------|-----------|
| 30 | Explicit Room migrations from v3 onward | Pre-release v1/v2 were allowed to wipe; from v3 (post-release data) all schema changes must migrate cleanly. Central `Migrations.kt` registry makes future migrations obvious. |
| 31 | Server-side `updated_at` as pull cursor | Client `lastModifiedAt` is set by device clock — vulnerable to skew. Server Postgres trigger sets `updated_at = now()` on every write; app uses this as the incremental pull cursor. |
| 32 | Server-side LWW guard trigger | `RETURN OLD` when incoming `last_modified_at < existing.last_modified_at` — silently rejects stale pushes without returning an error. App marks row synced (correct — server already has the newer version). |
| 33 | Delete-wins conflict policy | If remote row is deleted, apply tombstone locally. If local row is already deleted, skip the remote edit. Prevents "zombie un-delete" on multi-device. |
| 34 | `*IncludingDeleted` DAO queries for conflict resolution | Standard `getCatalogById` filters `deleted_at IS NULL`. Conflict resolver needs to see deleted rows to apply delete-wins correctly; dedicated `getCatalogByIdIncludingDeleted` queries expose them. |
| 35 | Debounced push-on-write (500 ms) | AI parse inserts 8+ ingredients in <200 ms — 500 ms debounce batches them into one HTTP call. Falls back silently on network failure (dirty flag retries). Keeps periodic sync as backstop only. |
| 36 | Paginated full pull (500-row pages) | PostgREST default 1,000-row limit silently truncates large result sets. Page size 500 is conservative — well below the limit. Loop stops when a page has fewer rows than the page size. |
| 37 | Foreground pull throttled to 5 min | Prevents redundant pulls on rapid app switching. `lastPullAtMs` lives in ViewModel memory — survives config changes, resets on process death (acceptable; periodic sync covers the gap). |

### Verification Checklist
- [ ] `assembleDebug` — Room KSP generates `4.json` schema snapshot
- [ ] Install v3 build → add data → upgrade to v4 → verify data survives (no wipe)
- [ ] Run `supabase/migrations/001_sync_infrastructure.sql` in Supabase Dashboard
- [ ] Add a food item → check Supabase within 1s → row present without manual sync (push-on-write)
- [ ] Edit a row in Supabase Dashboard → switch apps and back → verify change appears within 5s (foreground pull)
- [ ] Insert 1,500 daily logs in Supabase → fresh install → sign in → verify all 1,500 restored (pagination)
- [ ] Soft-delete a log → sync → verify local DB has no tombstone with `is_synced=1` (tombstone purge)
- [ ] Delete item on device A → edit same item on device B → pull on A → item stays deleted (delete-wins)

---

## Phase 8 Pre-work II: Sync Bug Fixes (Post-Testing)

**Status:** ✅ Completed
**Date:** May 2, 2026

### Summary
Five bugs discovered during post-implementation testing of Phase 8 Pre-work II sync infrastructure. All fixed in-session without a version bump (no schema changes, no Room migration required).

### Bugs Fixed

| # | Bug | Root Cause | Fix |
|---|-----|-----------|-----|
| 1 | Daily logs showed `1 kcal, 0g P/C/F` after editing a food item's macros | `recalculateLogsForFoodItem` SQL divided by `:baseServingG`, but `consumed_qty` is in **servings** (not grams) — `100 × 1 ÷ 100 = 1` instead of `100 × 1 = 100` | Removed `/ :baseServingG` from all four macro columns; dropped `baseServingG` param from function signature and call site |
| 2 | Push-on-write always returned 401 after cold start | `cachedToken` in `AuthPreferences` is in-memory only — reset to `null` on process restart. OkHttp interceptor found no token → sent requests without `Authorization` header → Supabase returned 401 | Added `init` coroutine in `AuthPreferences` that reads `Keys.ACCESS_TOKEN` from DataStore and warms `cachedToken` immediately on singleton creation |
| 3 | App crashed with `FOREIGN KEY constraint failed` during incremental pull | Incremental food items pull filtered by `updated_at > cursor`, skipping old unchanged items. A daily log updated since the cursor (e.g. via `recalculateLogsForFoodItem`) could reference a food item created before the cursor that was absent from the local DB → FK violation on `insertLog` | Changed food items pull to **always full-fetch** (paginated, no cursor filter) — food items are the FK parent and must always be complete before daily logs are inserted |
| 4 | Daily log entries disappeared from app after rebuild / partial-restore | Previous incremental pull had crashed mid-flight after inserting catalogs/foods but before finishing daily logs. Cursor was not advanced (pull failed). Sentinel `catalogCount == 0` didn't trigger because catalogs had been written. Next pull was incremental → old unchanged logs were never re-fetched | Changed daily logs pull to **always full-fetch** (paginated, no cursor filter) — same reasoning as food items; a user's history must always be complete |
| 5 | Pull cursor regressed to an older timestamp | Food items (now always full-fetched) were included in `max(updatedAt)` cursor calculation. Their `updated_at` values may predate the current cursor → cursor moved backwards → next pull re-fetched already-seen data or missed recent changes | Cursor now advances **only from daily log `updated_at` values**, with an explicit "never regress" guard (`if latestLogUpdatedAt > current`) |

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `data/local/dao/DailyLogDao.kt` | Updated | Fixed `recalculateLogsForFoodItem` SQL: removed `/ :baseServingG` from all four macro columns. Dropped `baseServingG` parameter. Added `getNonDeletedLogCount()` for partial-restore sentinel. |
| 2 | `data/repository/FoodRepositoryImpl.kt` | Updated | Removed `baseServingG = food.baseServingG` from `recalculateLogsForFoodItem` call site. Fixed KDoc formula comment. |
| 3 | `data/local/preferences/AuthPreferences.kt` | Updated | Added `init` coroutine block that reads stored `ACCESS_TOKEN` from DataStore and populates `cachedToken` immediately — prevents cold-start 401s on push-on-write. Added missing coroutine imports. |
| 4 | `data/local/dao/CatalogDao.kt` | Updated | Added `getCatalogCount()` for empty-DB sentinel. |
| 5 | `data/repository/SyncRepositoryImpl.kt` | Updated | (a) Empty-DB sentinel: forced full pull when `catalogCount == 0 \|\| logCount == 0`. (b) Food items: removed incremental filter — always full paginated fetch. (c) Daily logs: removed incremental filter — always full paginated fetch. (d) Cursor: only advance from `remoteLogs.updatedAt` max; explicit never-regress guard; dropped `remoteFoods` from cursor calculation. (e) Food item conflict resolver: simplified (no `isIncremental` branch needed). (f) Daily log conflict resolver: replaced `isIncremental` branch with `remote.lastModifiedAt >= local.lastModifiedAt` LWW. |

### Architecture Decisions Added
| # | Decision | Rationale |
|---|----------|-----------|
| 38 | `base_macro × consumed_qty` (no division) | `base_macro` is stored **per serving** (the value shown in the UI form). `consumed_qty` is the number of servings. Total = base × qty. Division by `baseServingG` is wrong because `consumed_qty` is not in grams. |
| 39 | Cold-start token warm-up via `AuthPreferences.init` coroutine | `cachedToken` is in-memory only and resets on process death. DataStore read in `init` block completes in <100 ms — well before the 500 ms push debounce fires. Eliminates cold-start 401 without an OkHttp `Authenticator` (which would create a Retrofit circular dependency). |
| 40 | Food items and daily logs always full-fetched (no cursor filter) | Both entities can silently develop gaps when the local DB is partially wiped or a previous pull crashed. A user's catalog + history is bounded (hundreds of rows max) and cheap to re-sync. Correctness trumps incremental efficiency here. The `updated_at` cursor is retained for future optimisation if needed. |
| 41 | Pull cursor advanced from daily logs only, never regresses | Catalogs and food items are always fully fetched; their `updated_at` values may be older than the current cursor. Including them caused the cursor to move backwards. The cursor represents "last seen incremental change" — only daily logs carry that meaning. Explicit `> current` guard prevents any regression. |

### Verification Checklist
- [x] Edit food item macros → daily log entries show correct recalculated values immediately
- [x] Rebuild app (cold start) → edit food item → check Supabase within 1s → row updated (no 401)
- [x] Fresh install with existing Supabase data → all food items and daily logs restored (no FK crash)
- [x] Partial-restore scenario (crash mid-pull) → next launch restores missing daily logs via full pull
- [x] Pull cursor only advances, never regresses after incremental sync with 0 new logs

---

## Phase 8 Pre-work III: Pull-to-Refresh on Home Screen

**Status:** ✅ Completed (UI polish deferred — see Known Issue #31)
**Date:** May 11, 2026

### Summary
Added swipe-down-to-refresh on the Home screen, wired to the existing `SyncDataUseCase`.
A 5-minute shared throttle prevents redundant syncs. All sync outcomes surface as a snackbar
("Sync complete", "Already up to date", "No internet connection", "Sign in to enable sync",
"Sync failed — will retry later"). A determinate progress bar fills during the pull gesture;
an indeterminate bar shows while sync is in progress.

Implementing PTR without Material3's built-in `PullToRefreshBox` (unavailable in material3 1.3.1
from BOM 2024.12.01) required two failed approaches before settling on a working solution — see
Architecture Decision #42 for the full reasoning.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `data/sync/SyncThrottleManager.kt` | Created | `@Singleton` that owns the 5-minute throttle and calls `SyncDataUseCase`. Returns `SyncTriggerResult` enum. Shared by `ForegroundSyncViewModel` (foreground pull on resume) and `HomeViewModel` (manual PTR). |
| 2 | `presentation/ForegroundSyncViewModel.kt` | Updated | Added `SyncTriggerResult` enum (`SYNCED`, `THROTTLED`, `NOT_SIGNED_IN`, `NO_INTERNET`, `ERROR`). Refactored `pullIfThrottled()` to delegate to `SyncThrottleManager`. |
| 3 | `presentation/screens/home/HomeViewModel.kt` | Updated | Added `isRefreshing: StateFlow<Boolean>`, `refreshMessage: SharedFlow<String>` (capacity 1, DROP_OLDEST), `onPullToRefresh()` coroutine that calls `SyncThrottleManager.triggerSync()` and emits the result message. |
| 4 | `presentation/screens/home/HomeScreen.kt` | Updated | Full PTR implementation: `rememberLazyListState()` for top-of-list detection; `PointerEventPass.Initial` gesture intercept that accumulates raw y-drag when `firstVisibleItemIndex == 0`; 64 dp threshold; determinate `LinearProgressIndicator` while dragging; indeterminate bar while `isRefreshing`; snackbar wired to `viewModel.refreshMessage`. Removed `NestedScrollConnection`, `nestedScroll`, `Velocity`, `Offset` imports (no longer used). |
| 5 | `gradle/gradle-daemon-jvm.properties` | Fixed | Changed `toolchainVendor` from JetBrains to `amazon` and `toolchainVersion` from 21 to 17 — JetBrains JVM 21 was not installed, causing daemon startup failure. |

### Architecture Decisions Added
| # | Decision | Rationale |
|---|----------|-----------|
| 42 | PTR via `PointerEventPass.Initial` + `LazyListState`, not `NestedScrollConnection` | Two prior approaches failed: (1) Material3 `PullToRefreshBox`/`PullToRefreshContainer` — not in material3 1.3.1 (BOM 2024.12.01). (2) Custom `NestedScrollConnection.onPostScroll` — LazyColumn's built-in overscroll effect (rubber-band stretch animation) consumes all downward-pull events before `onPostScroll` fires; `pullProgress` never accumulated. Working solution: `PointerEventPass.Initial` on the parent Box fires before any child processes the event; we observe (never consume) the raw y-delta independently of the scroll system. `LazyListState.firstVisibleItemIndex/ScrollOffset` guards against false positives from mid-list drags. |
| 43 | `SyncThrottleManager` as shared `@Singleton` | `ForegroundSyncViewModel` (foreground pull on resume) and `HomeViewModel` (manual PTR) must share the same 5-minute throttle clock to avoid double-syncs. Extracting to a singleton ensures the throttle is enforced regardless of which entry point triggers sync. |

### Verification Checklist
- [x] `./gradlew assembleDebug` — clean build (only pre-existing FlowPreview warning)
- [x] Slow drag-down on Home → determinate bar fills → release → snackbar appears
- [x] Pull again within 5 min → "Already up to date" snackbar (throttle path)
- [ ] Pull while offline → "No internet connection" snackbar
- [ ] Pull while signed out → "Sign in to enable sync" snackbar

---

## Phase 11: Nutrition Label Scanner

**Status:** ✅ Completed
**Date:** May 12, 2026

### Summary
Added a camera/photo button to the Log Food screen that lets the user pick a nutrition label photo from their gallery. The image is compressed, base64-encoded, and sent as a multimodal request to Gemma 4 (vision). The model reads the nutrition facts panel and returns structured JSON. Extracted macros pre-fill the manual entry form; the user adds the food name and saves normally.

Photos are saved locally to `filesDir/label_photos/` and tracked in a new Room table (`label_photos`). A TTL cleanup job runs on every app launch and deletes photos older than 10 days. Photos are never synced to Supabase.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `data/remote/dto/GeminiRequest.kt` | Updated | Made `GeminiPart.text` nullable; added `inlineData: GeminiInlineData?` field; added `GeminiInlineData` data class (mimeType + base64 data); added `GeminiLabelExtractionDto` response DTO |
| 2 | `di/AppModule.kt` | Updated | Added `explicitNulls = false` to the `Json` instance — prevents `"text": null` / `"inlineData": null` from being serialised (Gemini API rejects explicit null fields) |
| 3 | `util/GeminiLabelPrompts.kt` | Created | System instruction + user prompt for label reading. Enforces per-serving extraction with JSON schema in both positions (Gemma 4 best practice). Includes fallback rule for per-100g-only labels. |
| 4 | `domain/model/ExtractedLabelData.kt` | Created | Domain model: `caloriesPerServing`, `proteinG`, `carbsG`, `fatG`, `servingSizeText?`, `servingWeightG?` |
| 5 | `domain/repository/AiRepository.kt` | Updated | Added `extractLabelFromImage(imageBase64, mimeType): Resource<ExtractedLabelData>` |
| 6 | `data/repository/AiRepositoryImpl.kt` | Updated | Implemented `extractLabelFromImage()` — multimodal request with text + inlineData parts. Same error handling set as `parseFood()`. |
| 7 | `domain/usecase/ExtractLabelUseCase.kt` | Created | Thin wrapper delegating to `AiRepository.extractLabelFromImage()` |
| 8 | `data/local/entity/LabelPhotoEntity.kt` | Created | Room entity: `id`, `file_path`, `created_at` (epoch ms), `source_type` |
| 9 | `data/local/dao/LabelPhotoDao.kt` | Created | `insertPhoto`, `getPhotosOlderThan(cutoff)`, `deletePhoto(id)`, `deletePhotos(ids)` |
| 10 | `data/local/migrations/Migrations.kt` | Updated | Added `MIGRATION_5_6`: creates `label_photos` table. Added to `ALL` array. |
| 11 | `data/local/NutriAiDatabase.kt` | Updated | Bumped to version 6; added `LabelPhotoEntity::class`; added `abstract fun labelPhotoDao(): LabelPhotoDao` |
| 12 | `di/DatabaseModule.kt` | Updated | Added `provideLabelPhotoDao()` |
| 13 | `util/ImageCompressor.kt` | Created | `@Singleton` with `@ApplicationContext` + `LabelPhotoDao`. `compressAndSave(uri, sourceType)`: loads bitmap, scales to max 1024px, JPEG 80%, base64-encodes, saves to `filesDir/label_photos/{uuid}.jpg`, inserts Room row. Returns `CompressedPhoto(base64, mimeType, entity)`. |
| 14 | `domain/usecase/CleanupLabelPhotosUseCase.kt` | Created | Queries photos older than 10 days, deletes files, removes DB rows |
| 15 | `NutriAiApplication.kt` | Updated | Injected `CleanupLabelPhotosUseCase`; fire-and-forget cleanup coroutine in `onCreate()` |
| 16 | `presentation/screens/log/LogViewModel.kt` | Updated | Added `isExtractingLabel`, `labelExtractionError`, `labelPhotoId` to `LogUiState`; added `ExtractLabelUseCase` + `ImageCompressor` to constructor; added `onLabelPhotoSelected(uri)` and `clearLabelExtraction()` |
| 17 | `presentation/screens/log/LogScreen.kt` | Updated | Added `PickVisualMedia` launcher; added `OutlinedIconButton` camera button alongside Parse button; `AnimatedVisibility` loading + error states; label-scanned banner on manual form |
| 18 | `util/Constants.kt` | Updated | `DATABASE_VERSION = 6` |

### Architecture Decisions Added
| # | Decision | Rationale |
|---|----------|-----------|
| 44 | Multimodal request via `inlineData` (base64), not URI | Gemini API's `fileData` URI approach requires a separate file upload call. `inlineData` sends the image in one request — simpler, no extra round-trip, no file lifecycle to manage |
| 45 | `explicitNulls = false` in `Json` config | `GeminiPart` has both `text` and `inlineData` as nullable fields. Without this flag, unused fields serialise as `"text": null` — the Gemini API rejects requests with explicit null fields in the `parts` array |
| 46 | `@ApplicationContext` in `ImageCompressor` | Avoids passing `Context` from ViewModel (which would require the ViewModel to hold a reference to an Activity context). Hilt injects the application context directly into the singleton. |
| 47 | Photos never synced; TTL cleanup on launch | Label photos are large binary files with no value after the scan. Syncing them to Supabase would inflate storage costs and add complexity. 10-day TTL gives enough time to review logged meals without permanent storage growth. |
| 48 | `OutlinedIconButton` for camera button | Material3 `OutlinedButton` enforces `ButtonDefaults.MinWidth = 58dp`, overriding `Modifier.size(48.dp)`. `OutlinedIconButton` has no min-width constraint and renders as a correct 48×48dp square. |

### Verification Checklist
- [x] `./gradlew assembleDebug` — clean build
- [x] Log screen camera button renders as 48×48dp square
- [x] Photo picker opens on camera button tap (no runtime permission required)
- [x] Label photo compressed + saved to `filesDir/label_photos/`
- [x] Gemma 4 vision reads the label and returns structured JSON
- [x] Macros pre-filled in manual form; form switches to MANUAL_INPUT mode
- [x] Label-scanned banner visible when form was populated from a photo
- [x] Extraction error shows retry option

---

## Phase 11 Post-Implementation: Per-100g Nutrition Form

**Status:** ✅ Completed
**Date:** May 12, 2026

### Summary
Updated the manual entry form to align with food-label convention: macros are entered **per 100g** (when unit is grams) rather than per serving. Added "serving" as a first-class unit. Fixed the live preview formula and save path for correct scaling. Also fixed a double-scaling bug in the OpenFoodFacts pre-fill path and an incorrect conversion in the label scanner pre-fill. Updated the label-reading prompt to handle European labels that only show per-100g values.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/log/LogViewModel.kt` | Updated | Added `"serving"` to `UNIT_OPTIONS` (after `"grams"`) |
| 2 | `presentation/screens/log/LogViewModel.kt` | Updated | `previewCalories/Protein/Carbs/Fat`: when `unit == "grams"` use `macro × (qty/100)`, otherwise `macro × qty` |
| 3 | `presentation/screens/log/LogViewModel.kt` | Updated | `saveLog()`: compute `effectiveQty = if (unit == "grams") qty / 100.0 else qty` before calling `LogFoodUseCase` — ensures `DailyLog.totalCalories = per100g × (grams/100)` |
| 4 | `presentation/screens/log/LogViewModel.kt` | Updated | `acceptParsedFood()` OFFs branch (Priority 2): when target unit is grams, store raw `caloriesPer100g` instead of pre-scaling by `servingMultiplier` — prevents double-scaling with the new preview formula |
| 5 | `presentation/screens/log/LogViewModel.kt` | Updated | `onLabelPhotoSelected()`: when `servingWeightG` is known, convert per-serving → per-100g (`value / servingWeightG × 100`) before storing in form fields; unit stays `"grams"`, quantity = `servingWeightG` |
| 6 | `presentation/screens/log/LogScreen.kt` | Updated | Section header: dynamic label — `"Nutrition (per 100g)"` when unit is grams, `"Nutrition (per ${unit})"` otherwise |
| 7 | `presentation/screens/log/LogScreen.kt` | Updated | `MacroPreviewCard` header: `"Total (${qty}g)"` for grams, `"Total ($qty × $unit)"` for all other units |
| 8 | `util/GeminiLabelPrompts.kt` | Updated | Added fallback rule to `LABEL_SYSTEM_INSTRUCTION`: if the label only shows per-100g with no per-serving column, extract those values as per-serving and set `serving_weight_g = 100` — ViewModel conversion then produces the correct per-100g form values |

### Worked Example
Label: serving size 212g, 257 kcal/serving
- Model returns: `calories_per_serving=257`, `serving_weight_g=212`
- ViewModel converts: `257 / 212 × 100 = 121.2 kcal/100g`
- Form: Calories=121.2, Qty=212, Unit=grams
- Preview: `121.2 × (212/100) = 256.9 kcal` ✅
- Save: `effectiveQty = 212/100 = 2.12`, `totalCalories = 121.2 × 2.12 = 257 kcal` ✅

### Verification Checklist
- [x] Manual entry: 200 kcal/100g × 200g → preview shows 400 kcal
- [x] Manual entry: 257 kcal × 1 serving → preview shows 257 kcal
- [x] Label scan (212g serving, 257 kcal): form shows 121.2 kcal/100g, qty=212, preview≈257
- [x] AI parse + accept (OFFs hit, grams unit): preview does not double-scale
- [x] Daily log total on Home screen matches preview value after save

---

## Phase 12: Unit Parity, Macro Calculation Fixes & RLS Hardening

**Status:** ✅ Completed
**Date:** May 15, 2026

### Summary

Addressed a set of calculation correctness bugs discovered through cross-platform audit, along with unit list parity between Android and webapp and Supabase RLS/signup hardening:

1. **Unit list parity** — Android UNIT_OPTIONS brought in line with webapp (`"grams"→"g"`, `"cups"→"cup"`, added `"piece"`, `"slice"`, `"bowl"`). HomeScreen EditLog unit field replaced with a constrained dropdown (was free-text).
2. **Gram unit string normalization** — Two spots in `LogViewModel` used hardcoded `unit == "grams"` check instead of `UnitConverter.isGramsUnit()`. This caused incorrect macro calculations when AI returned `"gram"` or `"g"`.
3. **Gap 3 — FDC accept pre-scale double-count** — `acceptParsedFood()` was pre-multiplying FDC per-100g values by the *total* serving multiplier (e.g. `884 × 0.3 = 265 kcal for 2 tbsp`) and storing that in the form field. `saveLog()` then multiplied again by rawQty `× 2 = 530 kcal`. Fixed by using a *per-unit* multiplier (`computeServingMultiplier(1.0, unit)`) so form fields always hold per-unit values consistent with the `"Nutrition (per ${unit})"` label semantics.
4. **Gap 1 — Serving weight for discrete units** — FDC returns `servingSize`/`servingSizeUnit` for many foods but the DTO and mapper discarded these. "1 piece boiled egg" logged 156 kcal instead of ~78 kcal. Fixed: `servingWeightG` added to `NutritionInfo`, extracted from FDC `servingSize` (when unit = "g"), propagated through `UnitConverter.computeServingMultiplier()`.
5. **RLS hardening** — Missing INSERT policies on `food_items` (migration 007). Webapp `log-food` and `log-recipe` Edge Functions now throw on catalog creation failure instead of silently continuing.
6. **Signup crash fix** — Migration 008 `handle_new_user()` trigger inserted into `catalogs` before `public.users` existed, violating the FK constraint. Fixed: trigger now inserts `public.users` row first with `::bigint` cast and `ON CONFLICT DO NOTHING`.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/log/LogViewModel.kt` | Updated | `UNIT_OPTIONS`: `"grams"→"g"`, `"cups"→"cup"`, added `"piece"`, `"slice"`, `"bowl"` |
| 2 | `presentation/screens/log/LogViewModel.kt` | Updated | Default unit state: `"grams"→"g"` |
| 3 | `presentation/screens/log/LogViewModel.kt` | Updated | `scaleMacro()`: `unit == "grams"` → `UnitConverter.isGramsUnit(unit)` |
| 4 | `presentation/screens/log/LogViewModel.kt` | Updated | `saveLog()`: `unit == "grams"` → `UnitConverter.isGramsUnit(unit)` |
| 5 | `presentation/screens/log/LogViewModel.kt` | Updated | **Gap 3 fix** — `acceptParsedFood()` FDC path: replaced total `servingMultiplier` with `perUnitMultiplier = computeServingMultiplier(1.0, unit, servingWeightG)` to prevent double-scaling |
| 6 | `presentation/screens/log/LogViewModel.kt` | Updated | **Gap 1** — `acceptAndLogAllParsed()` recipe ingredient enrichment: passes `nutritionInfo.servingWeightG` to `computeServingMultiplier()` |
| 7 | `presentation/screens/home/HomeScreen.kt` | Updated | EditLog unit field: free-text `OutlinedTextField` replaced with `DropdownMenu` constrained to `LogUiState.UNIT_OPTIONS` |
| 8 | `presentation/screens/log/LabelScannerDelegate.kt` | Updated | Unit assignment: `"grams"→"g"` |
| 9 | `presentation/screens/log/LogScreen.kt` | Updated | Label string match: `"grams"→"g"` in section header and preview label |
| 10 | `util/UnitConverter.kt` | Updated | Added explicit `"piece"/"pieces"/"slice"/"slices"/"bowl"/"bowls"` cases; `else → quantity`; **Gap 1**: optional `servingWeightG` param — discrete units use `quantity * servingWeightG / 100` when available |
| 11 | `domain/model/NutritionInfo.kt` | Updated | **Gap 1**: added `servingWeightG: Double? = null` |
| 12 | `data/remote/dto/FoodDataCentralResponse.kt` | Updated | **Gap 1**: added `servingSize: Double?` + `servingSizeUnit: String?` to `FdcFood`; `toNutritionInfo()` extracts `servingWeightG` when `servingSizeUnit == "g"` |
| 13 | `supabase/migrations/007_food_items_insert_policy.sql` | Created | INSERT RLS policies for `food_items` and `catalogs` |
| 14 | `supabase/migrations/008_auto_create_catalogs_on_signup.sql` | Updated | Trigger now inserts `public.users` row before `catalogs` (FK fix); `::bigint` casts; `ON CONFLICT DO NOTHING` |

### Calculation Correctness — Before / After

| Scenario | Before | After |
|----------|--------|-------|
| "1 piece boiled egg" (FDC `servingWeightG=50g`) | 156 kcal | ~78 kcal ✅ |
| "2 tbsp olive oil" accept → preview | 530 kcal | ~265 kcal ✅ |
| "1 cup oats" accept → preview | 884 kcal (raw per-100g × qty) | ~212 kcal (240g basis) ✅ |
| Grams unit: "200g chicken" | Correct | Correct (no regression) |
| Serving unit: "2 servings rice" | Correct | Correct (no regression) |

### Known Limitations (not fixed in this phase)

- **IFCT-sourced foods** (offline fallback): `servingWeightG` is always null; discrete units still use 100g/unit assumption. Workaround: use `"g"` unit and enter actual gram weight.
- **Gap 4 / Gap 5**: `acceptAndLogAllParsed()` catalog-hit path uses raw qty as `effectiveQty`; this is correct when `baseCalories` was stored as per-unit (manual/saveLog path) but wrong when the item was originally created from the FDC Log-All path (stores per-100g in baseCalories). Fully fixing this requires a `FoodItem` schema migration to standardize `baseCalories` semantics.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 50 | Per-unit pre-scale in `acceptParsedFood()` (not per-total) | Form fields represent per-unit values per UX contract (`"Nutrition (per tbsp)"`). `scaleMacro() = value × qty` is correct only when value is per-unit. Pre-scaling by total qty caused a 2× overcounting. |
| 51 | `servingWeightG` in `NutritionInfo` (not in `FoodItem`) | FoodItem already stores per-100g base macros. `servingWeightG` is a lookup-time hint used only during the accept/log-all flow to compute the initial multiplier. It is not persisted — no schema change needed. |
| 52 | `UnitConverter.computeServingMultiplier()` with optional `servingWeightG` | Keeps the converter as the single source of truth for all unit math. Optional param is backward-compatible — all existing callers work unchanged. |

---

## Phase 13: Grams Display Fix & Catalog-Hit Quantity Correction

**Status:** ✅ Completed
**Date:** May 15, 2026

### Summary

Two related display/calculation bugs were discovered during HomeScreen testing:

1. **Grams display — "2.0 g" instead of "200 g"**: `consumedQty` for gram-logged items is stored as a multiplier (`grams ÷ 100`, e.g. 200g → `consumedQty = 2.0`). `FoodLogItem` and `HomeViewModel.startEditLog()` were rendering the raw multiplier directly (`"2.0 g"`). Fixed with two symmetric helpers in `UnitConverter`: `toDisplayQty()` (multiply by 100 for grams) and `fromDisplayQty()` (divide by 100 on save). Applied at every display and edit pre-fill site.

2. **Log-All catalog hit — "20000 g / 33000 kcal"**: `acceptAndLogAllParsed()` `isFromCatalog` branch always stored raw `food.quantity` (e.g. 200) as `effectiveQty`. For gram units this is wrong: `baseCalories` is per-100g, so `165 × 200 = 33 000 kcal`. Compounded by the display fix above: `toDisplayQty(200, "g") = 20 000 g`. Fixed by splitting the `isFromCatalog` branch on `isGramsUnit`: grams use `computeServingMultiplier(food.quantity, food.unit)` (= `qty / 100 = 2.0`); non-gram units keep raw qty.

3. **Log-All non-gram display — "0.3 tbsp" / "2.4 cups"**: FDC non-catalog path was storing `computeServingMultiplier(qty, unit)` (the total multiplier) as `consumedQty`. For "2 tbsp olive oil" → `consumedQty = 0.3`; HomeScreen showed "0.3 tbsp". Fixed in the same session as Gap 3 by rewriting the non-gram path to store per-unit base calories + raw qty — matching the semantics of `saveLog()`.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `util/UnitConverter.kt` | Updated | Added `toDisplayQty(consumedQty, unit): Double` — for grams: `consumedQty × 100`; others: unchanged. Recovers human-readable quantity from stored multiplier. |
| 2 | `util/UnitConverter.kt` | Updated | Added `fromDisplayQty(displayQty, unit): Double` — for grams: `displayQty ÷ 100`; others: unchanged. Converts user-entered qty back to stored multiplier form. |
| 3 | `presentation/components/FoodLogItem.kt` | Updated | Quantity display: `log.consumedQty` → `UnitConverter.toDisplayQty(log.consumedQty, log.consumedUnit)`. Added `UnitConverter` import. |
| 4 | `presentation/screens/home/HomeViewModel.kt` | Updated | `startEditLog()`: qty pre-fill changed from `log.consumedQty.formatMacro()` to `UnitConverter.toDisplayQty(log.consumedQty, log.consumedUnit).formatMacro()`. Added `UnitConverter` import. |
| 5 | `presentation/screens/home/HomeViewModel.kt` | Updated | `saveEditedLog()`: `consumedQty = qty` → `UnitConverter.fromDisplayQty(qty, resolvedUnit)`. Prevents double-conversion on save. |
| 6 | `presentation/screens/log/LogViewModel.kt` | Updated | `acceptAndLogAllParsed()` `isFromCatalog` branch: `effectiveQty` now uses `computeServingMultiplier(food.quantity, food.unit)` for gram units, raw `food.quantity` for all others. Fixes "20000 g / 33000 kcal" catalog-hit grams bug. |

### Calculation Correctness — Before / After

| Scenario | Before | After |
|----------|--------|-------|
| "200g chicken" logged → HomeScreen display | "2.0 g, 330 kcal" ❌ | "200 g, 330 kcal" ✅ |
| "200g chicken" edit pre-fill | "2.0" ❌ | "200" ✅ |
| Edit qty 200→400, save | 165 × 200 = 33 000 kcal ❌ | 165 × 4.0 = 660 kcal ✅ |
| "200g chicken" via AI parse (catalog hit) | "20 000 g, 33 000 kcal" ❌ | "200 g, 330 kcal" ✅ |
| "2 tbsp olive oil" via AI parse (Log All) | "0.3 tbsp" ❌ | "2 tbsp" ✅ |
| "1 cup rolled oats" via AI parse (Log All) | "2.4 cups" ❌ | "1 cup" ✅ |
| Non-gram catalog hit ("2 tbsp olive oil") | Unchanged ✅ | Unchanged ✅ |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 53 | `toDisplayQty` / `fromDisplayQty` in `UnitConverter` (not schema change) | `consumedQty` multiplier semantics are correct for math (`totalCalories = baseCalories × consumedQty`). No DB migration needed — just a display-time conversion at every render/pre-fill site. |
| 54 | `isFromCatalog` + `isGramsUnit` split in `acceptAndLogAllParsed()` | Catalog `baseCalories` is always per-100g for gram-logged items. Raw qty cannot be used directly as `consumedQty` for grams — only as a display value. The split preserves non-gram behavior (raw qty = natural unit count). |

---

## Phase 10: Polish — Empty State Icons & Error Feedback

**Status:** ✅ Completed
**Date:** May 12, 2026

### Summary
Added illustrative icons to empty states on Home and Insights screens (matching the existing Catalog pattern). Added user-visible error feedback when log deletion fails on the Home screen.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/home/HomeScreen.kt` | Updated | Added 64dp `Restaurant` icon above "No food logged" text in `EmptyLogState`. Added `Icons.Default.Restaurant` and `Modifier.size` imports. |
| 2 | `presentation/screens/insights/InsightsScreen.kt` | Updated | Added 64dp `BarChart` icon above "No data yet" text in `EmptyChartState`. Added `Icons`, `BarChart`, `Icon`, `size` imports. |
| 3 | `presentation/screens/home/HomeViewModel.kt` | Updated | `confirmDeleteLog()` now emits "Failed to delete entry" to `_refreshMessage` on exception, surfacing a top-anchored snackbar instead of silently swallowing the error. |

---

## Phase 14: Macro Goals Cross-Platform Sync

**Status:** ✅ Completed
**Date:** May 22, 2026

### Summary

Migrate Android macro goals from local-only DataStore to Room + Supabase bidirectional sync, enabling cross-platform goal sharing with the webapp. The webapp already reads/writes goals via the Supabase `user_preferences` table (migration `006`). Android must sync with this table while **preserving offline-first architecture** — Room is the local read source, Supabase is the sync target.

### Design Constraint

`HomeViewModel` must NEVER depend on network availability for macro goal reads. The local Room table is always the read source. Sync merely keeps it in sync with Supabase. This follows the same pattern as catalogs, food items, and daily logs.

### Planned Changes

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `data/local/entity/UserPreferencesEntity.kt` | Create | Room entity: `userId` (PK), `calorieGoal`, `proteinGoal`, `carbsGoal`, `fatGoal`, `isSynced`, `lastModifiedAt`. Defaults match `MacroGoals.kt`: 2000/150/250/65 |
| 2 | `data/local/dao/UserPreferencesDao.kt` | Create | DAO: `getPreferences(): Flow<UserPreferencesEntity?>`, `upsertPreferences()`, `getUnsyncedPreferences(): UserPreferencesEntity?`, `markAsSynced()` |
| 3 | `data/local/NutriAiDatabase.kt` | Update | Add `UserPreferencesEntity` to `@Database(entities = [...])`, bump DB version, add migration in `Migrations.kt` |
| 4 | `data/remote/dto/RemoteUserPreferencesDto.kt` | Create | Supabase DTO mapping `user_preferences` table columns. `toEntity()` and `toRemoteDto()` mappers |
| 5 | `data/remote/api/SupabaseDbApiService.kt` | Update | Add `@GET("user_preferences")` + `@PUT("user_preferences")` endpoints with `user_id` filter |
| 6 | `data/repository/SyncRepositoryImpl.kt` | Update | `pushLocalChanges()`: push unsynced prefs → Supabase `user_preferences`. `pullRemoteChanges()`: pull remote prefs → upsert to Room with LWW by `lastModifiedAt` |
| 7 | `presentation/screens/profile/ProfileViewModel.kt` (or equivalent) | Update | Read goals from `UserPreferencesDao` instead of DataStore. Write to Room DAO + set `isSynced = false` → triggers `SyncPushManager` |
| 8 | `data/sync/SyncPushManager.kt` | Update | Add `SyncEntityType.USER_PREFERENCES` enum value |
| 9 | `data/local/migrations/Migrations.kt` | Update | Add Room migration for new `user_preferences` table (schema version N → N+1) |

### Sync Flow

```
Android write:
  User sets goal → Room DAO upsert (isSynced=false) → SyncPushManager debounced push
  → Supabase PUT user_preferences → mark synced locally

Android pull:
  SyncRepositoryImpl.pullRemoteChanges() → GET user_preferences
  → LWW compare lastModifiedAt → upsert to Room if remote is newer

Web write:
  Settings page → Supabase user_preferences updated directly
  → Android next sync pull picks up the change → Room updated → HomeViewModel reads new goals
```

### Migration from DataStore

- Phase 14 reads/writes goals via Room DAO
- Existing DataStore `MacroGoals` code kept as one-time migration source: on first launch after update, if Room `user_preferences` is empty but DataStore has non-default goals, seed Room from DataStore values
- After migration, DataStore goal keys become dead code and can be removed

---

## Phase W5: Cross-Platform Testing — Android ↔ Web Sync

**Status:** ✅ Complete (6/6 PASSED)
**Date:** May 22, 2026

### Summary

Systematic end-to-end validation of bidirectional sync between the Android app and the Next.js webapp.

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

#### Test Steps
1. Sign in on Android with a clean account (Supabase tables cleared of prior data)
2. Create an Ingredients catalog entry ("Chicken Breast") with macros
3. Log a food entry using that catalog item
4. Trigger manual sync on Android
5. Open webapp → verify catalog item and daily log appear correctly

#### Bugs Found & Fixed

**Bug W5-1: PGRST102 "All object keys must match" on batch upsert**

- **Root cause:** `kotlinx.serialization` configured with `explicitNulls = false` (app-wide default, required by Gemini AI) silently omitted nullable fields — `brand`, `deleted_at`, `external_api_id` — from some objects in a batch POST array. PostgREST requires every object in a batch to have an identical set of JSON keys. Objects with different key counts triggered PGRST102.
- **Fix:** Added `@Named("supabase") Json` provider in `SupabaseModule.kt` with `explicitNulls = true`. Injected it into `provideSupabaseRetrofit` via `@Named("supabase") json: Json`. The app-wide `Json` (used by Gemini) keeps `explicitNulls = false`.
- **File:** `di/SupabaseModule.kt`

**Bug W5-2: 403 RLS violation — cross-user Room data contamination**

- **Root cause:** Room DB was not cleared on sign-out. User A's food items (catalog IDs prefixed with User A's UUID) persisted locally. When User B signed in on the same device and synced, those orphaned rows were pushed with User B's auth token. PostgREST's UPDATE policy checked `catalog_id` ownership against User B's UUID — found a mismatch — and rejected with `403 new row violates row-level security policy (USING expression)`.
- **Cascading effect:** Daily logs referencing the rejected food items failed with `409 FK constraint violation`.
- **Fix (immediate):** Orphaned `food_items` row and its dependent `daily_logs` row deleted directly from Supabase SQL Editor.
- **Fix (preventative):**
  - Added `clearAll()` to `UserDao`, `CatalogDao`, `FoodItemDao`, `DailyLogDao`
  - Updated `AuthRepositoryImpl.signOut()` to wipe all user-specific Room tables in FK order: `daily_logs` → `food_items` → `catalogs` → `users`. `ifct_foods` excluded (static IFCT 2017 seed data — not restorable from sync).
  - Cleared `last_sync_at` cursor in `AuthPreferences.clearSession()` so the next sign-in always starts with a full pull, preventing stale cursor confusion.
- **Files:** `data/local/dao/UserDao.kt`, `CatalogDao.kt`, `FoodItemDao.kt`, `DailyLogDao.kt`, `data/repository/AuthRepositoryImpl.kt`, `data/local/preferences/AuthPreferences.kt`

#### Result
All three tables (`catalogs`, `food_items`, `daily_logs`) synced successfully to Supabase. Webapp displayed the correct catalog entry and daily log with accurate macros. TC-30 **PASSED**.

### TC-31: Web → Android Sync

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Test Steps
1. Sign in on the webapp with the same test account
2. Create a new catalog item via the webapp
3. Log a food entry using that item
4. Switch to Android (same account, already signed in)
5. Trigger pull-to-refresh / manual sync on the Home screen
6. Verify the catalog item appears in the Ingredients catalog and the daily log appears on Home with correct macros

#### Bugs Found & Fixed
None — pull sync worked correctly on first attempt. No Android or server-side changes required.

#### Result
Catalog item and daily log created on the webapp appeared on Android after pull sync with correct names, macros, and quantities. TC-31 **PASSED**.

### TC-32: Edit on Android → Web Sync

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Test Steps
1. On Android, edit an existing catalog item name (tc31 → tc 32)
2. Edit the daily log entry for that item — change quantity from 1 to 2 servings
3. Trigger sync on Android
4. Open webapp → verify updated name, quantity, and recalculated macros appear

#### Bugs Found & Fixed

**Bug W5-3: EditLogSheet does not auto-recalculate macros when quantity changes**

- **Root cause:** The `EditLogSheet` treated quantity and macro fields as independent text inputs. Pre-fill loaded `totalCalories` (the current total for 1 serving). When the user changed qty from 1→2 without manually editing the calorie field, the old 1-serving total was saved and pushed to Supabase. The webapp reads `total_*` columns directly — so it showed qty=2 but macros for qty=1.
- **Fix:**
  - Added `baseCalories`, `baseProtein`, `baseCarbs`, `baseFat` to `EditLogSheet` data class — per-serving values derived at pre-fill time as `total / consumedQty`.
  - `updateEditQty()` now auto-recalculates: `newTotal = base × fromDisplayQty(newQty, unit)`.
  - `updateEditUnit()` also recalculates totals (unit change affects the stored multiplier).
  - Users can still manually override any macro field after auto-calc.
- **File:** `presentation/screens/home/HomeViewModel.kt`

#### Result
Catalog item name updated correctly on webapp. Daily log showed qty=2 with correctly doubled macros. TC-32 **PASSED**.

### TC-34: Soft-Delete on Android → Web

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Test Steps
1. On Android, delete a daily log entry (soft-delete sets `deleted_at` timestamp)
2. Trigger sync — tombstone pushed to Supabase
3. Open webapp → verify the item no longer appears (filtered by `deleted_at IS NULL`)

#### Bugs Found & Fixed
None — soft-delete tombstone synced correctly. Webapp queries filter on `deleted_at IS NULL` as expected.

#### Result
Deleted item disappeared from webapp after Android sync push. TC-34 **PASSED**.

### TC-35: Soft-Delete on Web → Android

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Test Steps
1. On the webapp, delete a daily log entry (soft-delete via Edge Function)
2. On Android, trigger pull-to-refresh
3. Verify the item disappears from the Android Home screen

#### Bugs Found & Fixed
None — pull sync applied the remote soft-delete correctly. Delete-wins conflict rule in `pullRemoteChanges()` handled the tombstone as expected.

#### Result
Deleted item disappeared from Android after pull sync. TC-35 **PASSED**.

### TC-33: Macro Goals Cross-Platform Sync

**Status:** ✅ PASSED
**Date:** May 22, 2026

#### Pre-requisite: Phase 14 Implementation
Implemented the full macro goals cross-platform sync (Phase 14) to unblock this test case. See Phase 14 section for details.

#### Test Steps
1. On the webapp, set custom macro goals (e.g. calories=2500, protein=180, carbs=300, fat=70)
2. On Android, trigger pull-to-refresh
3. Verify Android Home screen macro summary card shows the webapp's goals
4. On Android, change goals (e.g. calories=1800) via the Nutrition Goals sheet
5. Trigger sync on Android
6. Refresh the webapp → verify goals updated from Android

#### Bugs Found & Fixed

**Bug W5-4: Push returns 400 — `updated_at: null` violates NOT NULL constraint**

- **Root cause:** The Supabase `user_preferences` table defines `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`. The `RemoteUserPreferencesDto` had `updatedAt: String? = null` which, with `explicitNulls = true`, serialized as `"updated_at": null` in the push payload. PostgREST tried to set a NOT NULL column to null → HTTP 400.
- **Fix:** Created a separate `RemoteUserPreferencesPushDto` without the `updated_at` field for push operations. The full `RemoteUserPreferencesDto` (with `updatedAt`) is still used for pull responses. The server trigger manages `updated_at` automatically.
- **Files:** `data/remote/dto/SupabaseSyncDto.kt`, `data/remote/api/SupabaseDbApiService.kt`

#### Result
Goals set on webapp appeared on Android after pull. Goals set on Android appeared on webapp after push. Bidirectional sync confirmed working. TC-33 **PASSED**.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 55 | `@Named("supabase") Json` with `explicitNulls = true` | Supabase PostgREST requires all batch-upsert objects to have identical JSON key sets (PGRST102). Gemini API requires nullable fields to be omitted (`explicitNulls = false`). Two conflicting requirements cannot share one `Json` instance — named qualifier resolves the conflict cleanly. |
| 56 | Room wipe on sign-out in FK order | User-specific data must never cross account boundaries. Sign-out is the only safe point to guarantee a clean slate. FK order (daily_logs → food_items → catalogs → users) prevents constraint violations. `ifct_foods` excluded because it is a static seed that cannot be restored via sync. |
| 57 | `last_sync_at` cursor cleared on sign-out | A new user signing in on a previously-used device must start with a full pull, not an incremental one. Stale cursors would cause the new user to miss remote rows that pre-date the cursor timestamp. |

---

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Clean Architecture (Domain/Data/Presentation) | Separation of concerns, testability, scalable for team growth | Apr 27, 2026 |
| 2 | Hilt for DI | Official Android recommendation, simpler than Dagger, integrates with ViewModel/WorkManager | Apr 27, 2026 |
| 3 | Room as Single Source of Truth | Offline-first approach per system design; all UI reads from Room via Flows | Apr 27, 2026 |
| 4 | Kotlinx Serialization over Moshi/Gson | First-party Kotlin support, multiplatform-ready, smaller footprint | Apr 27, 2026 |
| 5 | KSP over KAPT | 2x faster annotation processing, future-proof (KAPT is deprecated) | Apr 27, 2026 |
| 6 | Phased MVP approach | Reduces risk, each phase is independently functional and testable | Apr 27, 2026 |
| 7 | Destructive migration for v1 | Pre-release schema is unstable; proper migrations added after schema freeze | Apr 27, 2026 |
| 8 | Split Hilt modules (Provides + Binds) | Concrete DB/DAO instances via @Provides, interface→impl via @Binds — cleaner DI graph | Apr 27, 2026 |
| 9 | Foreign keys with CASCADE delete | Ensures referential integrity across User→Catalog→FoodItem→DailyLog chain | Apr 27, 2026 |
| 10 | Proactive sync-ready DAOs | `getUnsynced`, `markAsSynced`, `hardDelete` methods ready for Phase 6 Supabase sync | Apr 27, 2026 |
| 11 | StateFlow + WhileSubscribed(5s) | Prevents unnecessary upstream Flow collection when UI is in background; 5s buffer covers config changes | Apr 27, 2026 |
| 12 | SharedFlow for one-time events | `LogViewModel` uses `MutableSharedFlow<LogEvent>` for navigation/snackbar — avoids state re-consumption issues | Apr 27, 2026 |
| 13 | Content/state separation in screens | Each screen: public `@Composable` (with ViewModel) + private `Content` (pure state) — enables `@Preview` without Hilt | Apr 27, 2026 |
| 14 | UseCases as single-invoke classes | Each UseCase has one `operator fun invoke()` — composable, testable, follows SRP | Apr 27, 2026 |
| 15 | Animated bottom bar visibility | `AnimatedVisibility` hides bottom nav on full-screen forms (Log screen) for cleaner UX | Apr 27, 2026 |
| 16 | Gemma 4 over Gemini models | `gemma-4-26b-a4b-it` (Apache 2.0 open model) — future-proofs against Gemini deprecation cycles (2.0 EOL June 2026) | Apr 27, 2026 |
| 17 | Thinking mode minimized for structured output | `thinkingLevel = "MINIMAL"` prevents Gemma 4 from wrapping JSON in thinking tags (fixed from `thinkingBudget = 0` in Phase 4.5) | Apr 27, 2026 |
| 18 | Dual schema reinforcement | JSON schema in both system instruction AND user prompt — Gemma 4 compliance is more reliable with both | Apr 27, 2026 |
| 19 | Entity extraction only (no macro estimation) | AI extracts names/quantities/units; macros come from OpenFoodFacts (Phase 5) — avoids hallucinated nutrition data | Apr 27, 2026 |
| 20 | Two input modes (AI + Manual) with tabs | AI Parse is default for discoverability; Manual always available as fallback; state preserved on switch | Apr 27, 2026 |
| 21 | Fallback JSON extraction (`extractJson()`) | Regex-based cleanup for markdown fences, thinking tags, extra text — handles Gemma 4 edge cases gracefully | Apr 27, 2026 |
| 22 | AI-side recipe detection (not client heuristics) | Gemma 4 detects recipe patterns in prompt and returns `is_recipe: true` + nested `ingredients` — more accurate than regex-based client detection | Apr 27, 2026 |
| 23 | Local catalog cache (Room-only, no remote) | Ingredient reuse checks local Room DB only — fast, offline-friendly, no network dependency. Remote sync deferred to Phase 6 | Apr 27, 2026 |
| 24 | `thinkingLevel: "MINIMAL"` for Gemma 4 | String-enum thinking config replaces integer `thinkingBudget`. MINIMAL allows lightweight thinking without corrupting JSON output | Apr 27, 2026 |
| 25 | Multi-part response filtering (skip `thought` parts) | Gemma 4 returns thinking parts alongside content parts. Extraction skips `thought: true` parts to get clean JSON | Apr 27, 2026 |
| 26 | Ingredient-level selection within recipes | `selectedIngredientIndex` state enables editing individual ingredients. Tappable rows with visual highlight + contextual button labels | Apr 27, 2026 |
| 27 | Catalog names fetched in `AiRepositoryImpl`, not UseCase/ViewModel | Keeps name standardization internal to the AI data layer. UseCase/ViewModel stay agnostic — call `parseFood(input)` unchanged | Apr 27, 2026 |
| 28 | Per-request catalog fetch (not cached in memory) | Room queries are fast; catalog is the source of truth. In-memory cache risks stale data if the user adds items mid-session | Apr 27, 2026 |
| 29 | Empty catalog lists → prompt sections omitted | Avoids empty/misleading headers on first launch. Cleaner prompt, no wasted tokens; sections only appear when the catalog has data | Apr 27, 2026 |

---

## Known Issues & Tech Debt

| # | Issue | Severity | Phase | Status | Notes |
|---|-------|----------|-------|--------|-------|
| 1 | ~~Food log showed UUID instead of name~~ | ~~High~~ | 3 | ✅ Fixed | Resolved in Phase 3.5 — `HomeViewModel` now resolves food names via `FoodRepository` |
| 2 | ~~Catalog ID mismatch between UseCases~~ | ~~High~~ | 3 | ✅ Fixed | Resolved in Phase 3.5 — `Constants.DEFAULT_CATALOG_ID` used everywhere |
| 3 | ~~`GetCatalogsUseCase` only read first catalog~~ | ~~Medium~~ | 3 | ✅ Fixed | Resolved in Phase 3.5 — now aggregates ALL user catalogs via `combine()` |
| 4 | ~~Food log appeared on wrong date~~ | ~~High~~ | 3 | ✅ Fixed | Resolved in Phase 3.5 — DAO uses half-open interval `[start, end)` instead of `BETWEEN` |
| 5 | ~~No visible delete action on food log~~ | ~~Medium~~ | 3 | ✅ Fixed | Resolved in Phase 3.5 — visible edit/delete icons added to `FoodLogItem` |
| 6 | ~~Edit button not wired on food log items~~ | ~~Medium~~ | 3 | ✅ Fixed (Phase 6) | `EditLogSheet` bottom sheet fully wired in `HomeViewModel`/`HomeScreen` since Phase 6. |
| 7 | ~~Deleting catalog food shows "Unknown Food" on Home~~ | ~~Low~~ | 3 | ✅ Fixed (post-Phase 7) | `foodName: String` snapshotted into `DailyLog`/`DailyLogEntity` at log-creation time. `HomeViewModel` reads `log.foodName` directly — no `FoodRepository` lookup. Soft-deleting or renaming a `FoodItem` no longer affects the display name of existing log entries. DB v2 → v3. |
| 8 | ~~Macro goals are hardcoded~~ | ~~Low~~ | 3 | ✅ Fixed (Phase 7) | `MacroGoals` domain model + DataStore storage added. User sets targets via Profile tab "Nutrition Goals" sheet. `HomeViewModel` exposes `macroGoals: StateFlow<MacroGoals>` wired to `MacroSummaryCard`. |
| 9 | ~~No confirmation dialog on delete~~ | ~~Low~~ | 3 | ✅ Fixed (Phase 7) | `ConfirmDeleteDialog` added. Home and Catalog use pending-delete staging (`requestDeleteLog` / `confirmDeleteLog`) so delete fires only after user confirms. |
| 10 | ~~AI parsing not runtime-tested~~ | ~~High~~ | 4 | ✅ Fixed | Resolved in Phase 4.5 — fixed model name (`gemma-4-26b-a4b-it`), thinking config (`thinkingLevel: "MINIMAL"`), and response extraction (filter `thought` parts). Verified on-device. |
| 11 | ~~`thinkingConfig` placement may be wrong~~ | ~~Medium~~ | 4 | ✅ Fixed | Resolved in Phase 4.5 — changed from `thinkingBudget: Int` to `thinkingLevel: String`. Gemma 4 uses string-enum config. Confirmed working on-device. |
| 12 | `responseMimeType` may not work for Gemma 4 | Medium | 4 | ✅ Works | Confirmed working on-device — Gemma 4 respects `responseMimeType: "application/json"` and returns valid JSON. `extractJson()` fallback still in place as safety net. |
| 13 | ~~Batch "Log All" saves 0-calorie entries~~ | ~~Low~~ | 4 | ✅ Fixed | Resolved in Phase 5 — `acceptAndLogAllParsed()` uses OFFs nutrition data (scaled to serving) when available, falling back to 0 only when both catalog and OFFs lookups miss. |
| 14 | ~~Recipe ingredient macros all zero until Phase 5~~ | ~~Low~~ | 4.5 | ✅ Fixed | Resolved in Phase 5 — ingredient nutrition lookups run concurrently after AI parse. New ingredients with OFFs data are enriched before logging and persisted to the Ingredients catalog. |
| 15 | No way to reorder or remove individual ingredients from a recipe | Low | 4.5 | 🔲 Open | Recipe ingredient list is fixed after AI parsing. User can edit macros per ingredient but cannot remove or reorder them. |
| 16 | ~~OpenFoodFacts blocked on Walmart VPN~~ | ~~High~~ | 5 | ✅ Fixed (Phase 5.5) | Replaced OFFs with USDA FDC (`.gov` domain) + IFCT 2017 offline CSV. FDC lookup falls through silently on network failure; IFCT handles Indian foods without any network at all. |
| 17 | ~~Poor Indian/South Asian food coverage in OpenFoodFacts~~ | ~~Medium~~ | 5 | ✅ Fixed (Phase 5.5) | IFCT 2017 CSV (120 NIN-verified Indian foods) bundled in `assets/` and seeded to Room on first launch. Covers grains, pulses, vegetables, dairy, meat, fish, nuts, prepared dishes, sweets, beverages. |
| 18 | ~~JWT access token not refreshed automatically~~ | ~~High~~ | 6 | ✅ Fixed (Phase 7) | `AuthRepository.refreshSessionIfNeeded()` added. `SyncDataUseCase` calls it before every sync — if token expires within 5 min it is proactively refreshed; "Session expired" error surfaces to UI on hard expiry. |
| 19 | ~~False-positive "Sync complete" when offline~~ | ~~High~~ | 6 | ✅ Fixed | `SyncDataUseCase` now checks `ConnectivityManager.NET_CAPABILITY_VALIDATED` before any network call; returns `Resource.Error("No internet connection")` immediately. |
| 20 | ~~Edit log entry not wired up~~ | ~~Medium~~ | 3/7 | ✅ Fixed (Phase 6) | Full `EditLogSheet` bottom sheet with pre-populated fields wired in `HomeViewModel`/`HomeScreen` since Phase 6. Confirmed present and working during Phase 7 exploration. |
| 21 | ~~Pull sync downloads all rows every sync~~ | Medium | 6 | ✅ Fixed (Phase 8 Pre-work II) | Incremental pull with server-side `updated_at` cursor. Full pull with 500-row pagination on first sync / cursor reset. |
| 22 | ~~`fallbackToDestructiveMigration()` wipes user data~~ | High | 2 | ✅ Fixed (Phase 8 Pre-work II) | Explicit migrations from v3 onward via `Migrations.ALL`. Destructive fallback limited to pre-release v1/v2 only. |
| 23 | ~~Stale push can silently overwrite newer server data~~ | High | 6 | ✅ Fixed (Phase 8 Pre-work II) | Server-side LWW guard trigger (`guard_lww`) rejects UPDATEs where incoming `last_modified_at < existing.last_modified_at`. Deployed via `supabase/migrations/001_sync_infrastructure.sql`. |
| 24 | ~~Delete+edit conflict could resurrect deleted rows~~ | Medium | 6 | ✅ Fixed (Phase 8 Pre-work II) | Delete-wins rule in `pullRemoteChanges()`. Uses `*IncludingDeleted` DAO queries so locally-deleted rows are visible to the conflict resolver. |
| 25 | ~~Sync only runs every 12h or on manual trigger~~ | Medium | 6 | ✅ Fixed (Phase 8 Pre-work II) | Push-on-write (debounced 500ms via `SyncPushManager`) + foreground pull on resume (`ForegroundSyncViewModel`). Periodic sync reduced to 24h backstop. |
| 26 | ~~Daily logs showed `1 kcal, 0g` after food item macro edit~~ | High | 8 | ✅ Fixed (Phase 8 Post-Testing) | `recalculateLogsForFoodItem` SQL divided by `baseServingG` incorrectly — `consumed_qty` is in servings not grams. Fixed: formula is now `base_macro × consumed_qty`. |
| 27 | ~~Push-on-write returned 401 after cold start~~ | High | 8 | ✅ Fixed (Phase 8 Post-Testing) | `cachedToken` was null after process restart; OkHttp sent requests without auth header. Fixed: `AuthPreferences.init` coroutine warms cache from DataStore on singleton creation. |
| 28 | ~~FK constraint crash during incremental pull~~ | High | 8 | ✅ Fixed (Phase 8 Post-Testing) | Incremental food items pull could skip old unchanged items; daily logs referencing them → FK violation. Fixed: food items always full-fetched (paginated, no cursor filter). |
| 29 | ~~Daily logs disappeared after rebuild / partial restore~~ | High | 8 | ✅ Fixed (Phase 8 Post-Testing) | Partial-restore (crash mid-pull) left catalogs present but logs missing; sentinel only checked `catalogCount == 0`. Fixed: daily logs also always full-fetched; `logCount == 0` sentinel added. |
| 30 | ~~Pull cursor regressed after incremental sync~~ | Medium | 8 | ✅ Fixed (Phase 8 Post-Testing) | Full-fetched food items' old `updated_at` values lowered the cursor max. Fixed: cursor only advances from daily log `updated_at`; explicit never-regress guard added. |
| 31 | ~~Pull-to-refresh UI polish needed~~ | ~~Low~~ | 8 | ✅ Fixed | Snackbar anchored near top via `Box(contentAlignment = TopCenter)` wrapper. Both pull indicators (`LinearProgressIndicator`) replaced with `CircularProgressIndicator` — determinate during drag, indeterminate while syncing. |
| 32 | ~~`acceptAndLogAllParsed()` OFFs scaling for grams~~ | ~~Low~~ | 11 | ✅ Fixed | The "Log All" fast path was double-scaling: `per100g × multiplier` as calories, then `LogFoodUseCase` multiplied again by raw quantity. Fixed: nutrition-data path now passes raw per-100g values as base macros and uses `computeServingMultiplier()` as the effective quantity — matching `saveLog()`'s approach. Catalog-hit path unchanged (already correct). |
| 33 | ~~Android unit list out of sync with webapp~~ | ~~Medium~~ | 12 | ✅ Fixed | `UNIT_OPTIONS` now matches webapp: `g/serving/tsp/tbsp/cup/ml/piece/slice/bowl`. References to `"grams"`, `"cups"` updated throughout. `HomeScreen` unit field constrained to dropdown. |
| 34 | ~~`unit == "grams"` hardcoded string check~~ | ~~High~~ | 12 | ✅ Fixed | `LogViewModel.scaleMacro()` and `saveLog()` both used literal `"grams"` check, breaking calculations when AI returned `"g"` or `"gram"`. Replaced with `UnitConverter.isGramsUnit()`. |
| 35 | ~~`acceptParsedFood()` double-scales FDC macros for non-gram units~~ | ~~High~~ | 12 | ✅ Fixed | Pre-scaled by total `servingMultiplier`; `saveLog()` then multiplied by rawQty again. "2 tbsp oil" → 530 kcal instead of 265. Fixed: use `perUnitMultiplier = computeServingMultiplier(1.0, unit)` so form holds per-unit values. |
| 36 | ~~"1 piece egg" logs 156 kcal instead of ~78 kcal~~ | ~~High~~ | 12 | ✅ Fixed | FDC `servingSize`/`servingSizeUnit` fields were discarded. Added `servingWeightG` to `NutritionInfo` and `FdcFood` DTO; `UnitConverter` now uses actual gram weight for piece/slice/bowl units when available. |
| 37 | ~~`food_items` INSERT RLS violation on webapp~~ | ~~High~~ | 12 | ✅ Fixed | Missing INSERT policy on `food_items`. Apply migration 007 in Supabase SQL Editor. |
| 38 | ~~Android signup crash (HTTP 500) after migration 008~~ | ~~Critical~~ | 12 | ✅ Fixed | `handle_new_user()` trigger inserted into `catalogs` before `public.users` existed (FK violation). Fixed: insert `public.users` first, add `::bigint` casts, add `ON CONFLICT DO NOTHING`. |
| 39 | ~~Catalog-hit grams in Log-All: raw qty stored as `effectiveQty` → 33 000 kcal / 20 000 g~~ | ~~Critical~~ | 12 | ✅ Fixed (Phase 13) | `isFromCatalog` branch always used `food.quantity` (e.g. 200) as `consumedQty`. For grams `baseCalories` is per-100g, so `165 × 200 = 33 000 kcal`. Display then showed `toDisplayQty(200, "g") = 20 000 g`. Fixed: `isFromCatalog` now splits on `isGramsUnit` — grams use `computeServingMultiplier(qty, unit)` (= 2.0), non-gram keeps raw qty. |
| 40 | IFCT-sourced foods: discrete units assume 100g/unit | Low | 12 | 🔲 Open | `servingWeightG` is always null for IFCT data. "1 piece egg" still logs 156 kcal when FDC is unavailable. Workaround: use `"g"` unit with actual gram weight. |
| 41 | ~~Grams display shows multiplier instead of human qty ("2.0 g" not "200 g")~~ | ~~High~~ | 13 | ✅ Fixed (Phase 13) | `FoodLogItem` and `HomeViewModel.startEditLog()` rendered raw `consumedQty` (the multiplier). Fixed: `UnitConverter.toDisplayQty()` applied at all display/pre-fill sites. `UnitConverter.fromDisplayQty()` applied on save to prevent double-conversion. |
| 42 | ~~Log-All non-gram display: "0.3 tbsp" / "2.4 cups" instead of "2 tbsp" / "1 cup"~~ | ~~High~~ | 13 | ✅ Fixed (Phase 13) | FDC non-catalog path stored total `computeServingMultiplier(qty, unit)` as `consumedQty`. "2 tbsp" → stored 0.3. Fixed by rewriting non-gram FDC path to store per-unit base calories + raw qty — matching `saveLog()` semantics. |
| 43 | ~~Android macro goals use local DataStore only — no cross-platform sync~~ | ~~Medium~~ | 14 | ✅ Fixed (Phase 14) | Migrated from DataStore to Room `UserPreferencesEntity` + bidirectional Supabase sync. Room is read source, Supabase is sync target. `UserPreferences` rewritten to back on Room DAO with one-time DataStore migration. Push-on-write via `SyncPushManager`, pull in `SyncRepositoryImpl`. TC-33 validated. |
| 44 | ~~Android→Supabase batch upsert failed with PGRST102 "All object keys must match"~~ | ~~Critical~~ | W5 | ✅ Fixed | `kotlinx.serialization` with `explicitNulls = false` (app-wide default) silently omitted nullable fields (`brand`, `deleted_at`, `external_api_id`) from some objects in a batch POST array, giving them fewer keys than others. PostgREST requires all objects in a batch to have identical key sets. Fix: added `@Named("supabase") Json` provider in `SupabaseModule` with `explicitNulls = true` and injected it into `provideSupabaseRetrofit`. App-wide `Json` (used by Gemini AI) keeps `explicitNulls = false` to avoid rejections from the Gemini API. |
| 45 | ~~Cross-user Room data caused 403 RLS violation on food_items upsert~~ | ~~Critical~~ | W5 | ✅ Fixed | Room DB persisted across sign-out/sign-in. User A's food items (with catalog IDs prefixed by User A's UUID) remained in local Room when User B signed in. On next sync, those orphaned rows were pushed with User B's auth token — the UPDATE path of the Supabase upsert policy checked the existing row's `catalog_id` against User B's UUID prefix, found a mismatch, and rejected with `403 new row violates row-level security policy (USING expression)`. Daily logs referencing those food items then failed with `409 FK constraint`. Fix: (1) deleted orphaned rows from Supabase manually, (2) added `clearAll()` to all user-data DAOs (`UserDao`, `CatalogDao`, `FoodItemDao`, `DailyLogDao`) and (3) updated `AuthRepositoryImpl.signOut()` to wipe all user-specific Room tables in FK order (daily_logs → food_items → catalogs → users) on every sign-out. `ifct_foods` excluded — static seed data. Also cleared the `last_sync_at` cursor in `AuthPreferences.clearSession()` so the next sign-in always starts with a full pull. |
| 46 | ~~EditLogSheet does not auto-recalculate macros when qty changes~~ | ~~High~~ | W5 | ✅ Fixed | Edit sheet treated qty and macro fields as independent text inputs. Changing qty from 1→2 left `total_*` unchanged — pushed stale 1-serving totals to Supabase. Webapp showed qty=2 but macros for qty=1. Fix: added `baseCalories/Protein/Carbs/Fat` (per-serving) to `EditLogSheet`, derived at pre-fill as `total / consumedQty`. `updateEditQty()` and `updateEditUnit()` now auto-recalculate totals as `base × newStoredQty`. User can still manually override after auto-calc. |
| 47 | ~~User preferences push returns 400 — `updated_at: null` violates NOT NULL~~ | ~~High~~ | 14 | ✅ Fixed | `user_preferences.updated_at` is `TIMESTAMPTZ NOT NULL DEFAULT now()` (trigger-managed). With `explicitNulls = true`, the DTO serialized `"updated_at": null`, violating the constraint. Fix: separate `RemoteUserPreferencesPushDto` without `updated_at` for push; full `RemoteUserPreferencesDto` (with `updatedAt`) for pull. Server trigger manages the column automatically. |
| 48 | Per-serving vs per-100g macro mismatch — cross-platform | Critical | 18 | 🔲 Planned | `food_items.base_calories` stores per-serving macros (e.g., 321 kcal for 200g) but `computeServingMultiplier()` and all downstream math assume per-100g. Causes wrong totals when editing logs across unit types (Scenario 2: 481 kcal instead of 241). Phase 18 guard is symptomatic. Architectural fix: normalize all `base_*` to per-100g at write time (`raw × 100/baseServingG`), update `computeServingMultiplier` to use `servingWeightG` for "serving" units, migrate existing data. Plan: `~/.wibey/plans/per100g_normalization_b0f402b8.plan.md`. |
| 49 | Validation/save path mismatch in recipe mode (Scenario 1: 0 macros) | High | 18 | 🔲 Planned | When `isLoggingRecipe=true` but no ingredients added, `isValid` passes via flat check (calories field non-zero) but save calls `saveManualRecipe()` which aggregates 0 from empty ingredients list. Additionally `logRecipe()` stores raw gram quantity without `/100` conversion. To be fixed in per-100g normalization. |

---

## Phase 15: Sign-Up Flow — Confirmation Before Sign-In

**Status:** ✅ Completed
**Date:** May 23, 2026

### Summary

Changed the sign-up success flow so users must explicitly sign in after creating an account, rather than being automatically navigated to the Home screen. After a successful `signUpUseCase` call, the app now shows an "Account created! Please sign in." snackbar and returns to the sign-in form. The user then signs in manually, which triggers the existing `NavigateToHome` + `SyncScheduler` path.

Previously: Sign up → `NavigateToHome` event → Home screen (sync triggered immediately)
Now: Sign up → `SignUpSuccess` event → snackbar + sign-in form → user signs in → Home screen

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/auth/AuthViewModel.kt` | Updated | Added `AuthEvent.SignUpSuccess` to the sealed class. In `signUp()` success branch: replaced `NavigateToHome` event + `SyncScheduler.schedule()` + `syncDataUseCase()` with `SignUpSuccess` event only. Sync and scheduling now happen exclusively after sign-in. |
| 2 | `presentation/screens/auth/AuthScreen.kt` | Updated | Added `AuthEvent.SignUpSuccess` branch in `LaunchedEffect(Unit)` event collector: shows "Account created! Please sign in." snackbar, then calls `viewModel.toggleSignUpMode()` to switch the form back to sign-in mode. No new composables needed. |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 58 | `SignUpSuccess` event separate from `NavigateToHome` | Sign-up and sign-in are distinct operations with different post-action behaviors. Sign-in should own `SyncScheduler.schedule()` and the initial `syncDataUseCase()` call — these require an active authenticated session. Triggering sync immediately after sign-up (before the user has explicitly signed in) is premature and could cause race conditions on accounts with no existing Supabase data. |
| 59 | Return to sign-in form (not navigate to new screen) | The existing `AuthScreen` already has a sign-in form with the same email pre-fillable by the user. `toggleSignUpMode()` resets to sign-in view cleanly within the same screen — no new route, no additional navigation stack entry. |

---

## Phase 16: Manual Recipe Builder + Catalog Navigation Fix + Catalog Miss Bug

**Status:** ✅ Completed
**Date:** May 27, 2026

### Summary

Four related improvements delivered together:

1. **Manual recipe builder mode on Log screen** — `ManualInputSection` now branches between a flat ingredient form and a full recipe builder. When `isLoggingRecipe` is true the screen shows a dynamic ingredient list with per-ingredient catalog search, qty/unit inputs, macro fields, and an aggregated macro preview card. When false, the existing flat form is shown unchanged.

2. **Catalog page FAB opens recipe builder mode** — Navigating to the Log screen via the `+` FAB on the Recipes catalog tab previously opened the flat ingredient form. Fixed by making `setCatalogType()` set `isLoggingRecipe = true` when `catalogType == RECIPE_CATALOG_ID`, so the UI enters recipe builder mode immediately.

3. **Recipe name edit collapsing ingredient list** — `updateFoodName()` was resetting `isLoggingRecipe = false` on every keystroke, collapsing the ingredient list whenever the user corrected the recipe name. Removed that reset.

4. **Redundant USDA lookup for catalog-saved recipes** — `ResolveCatalogCacheUseCase` only searched `INGREDIENT_CATALOG_ID` for flat items (`isRecipe=false`). Gemini sometimes emits `isRecipe=false` for recipes that exist in the user's recipe catalog (e.g. "banana bread"). The catalog miss caused `NutritionLookupDelegate` to fire a USDA FDC lookup even though macros were already known locally. Fixed by falling back to `RECIPE_CATALOG_ID` when the ingredient catalog returns null.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/log/LogScreen.kt` | Updated | Added `ManualRecipeIngredientsSection`, `ManualRecipeIngredientRow`, `IngredientMacroBadge` composables. `ManualInputSection` signature extended with recipe builder lambdas (`onAddIngredient`, `onRemoveIngredient`, `onUpdateIngredient`, `onSelectCatalogIngredient`, `onSaveRecipeClick`, `searchIngredientCatalog`). Body branches: `if (uiState.isLoggingRecipe)` → recipe builder with aggregated macro card; `else` → unchanged flat form. Debounced catalog search via `LaunchedEffect(searchText)` with 300ms `delay` + Flow collection. |
| 2 | `presentation/screens/log/LogScreen.kt` | Updated | `ManualRecipeIngredientRow` layout changed from a single horizontal Row (name + qty + unit + remove) to two stacked Rows: Row 1 `[name field, weight(1f)] [remove button]`, Row 2 `[qty, width(96.dp)] [unit dropdown, weight(1f)]`. Fixes vertical letter overflow on narrow screens (~360dp) where the original layout left only ~84dp for the ingredient TextField. |
| 3 | `presentation/screens/log/LogViewModel.kt` | Updated | `setCatalogType()` now sets `isLoggingRecipe = catalogType == Constants.RECIPE_CATALOG_ID` alongside `catalogType`. `updateFoodName()` no longer resets `isLoggingRecipe = false` — typing a recipe name no longer collapses the ingredient list. |
| 4 | `domain/usecase/ResolveCatalogCacheUseCase.kt` | Updated | For flat items (`isRecipe=false`): look up ingredient catalog first; if null, fall back to recipe catalog via Kotlin `?:` operator. Prevents redundant USDA lookups for recipes Gemini mis-classifies as non-recipe. Updated KDoc class comment to reflect the actual dual-catalog behaviour. |

### Key Implementation Details

**Recipe builder composables (`LogScreen.kt`):**
```kotlin
// Debounced catalog search
LaunchedEffect(searchText) {
    delay(300)
    debouncedQuery = searchText
}
LaunchedEffect(debouncedQuery) {
    searchCatalog(debouncedQuery).collect { results ->
        catalogResults = results
    }
}
```

**Aggregated macro preview in recipe mode:**
```kotlin
val recipeCalories = uiState.manualRecipeIngredients.sumOf { r ->
    val q = r.quantity.toDoubleOrNull() ?: 0.0
    val m = UnitConverter.computeServingMultiplier(q, r.unit)
    (r.catalogItem?.baseCalories ?: r.calories.toDoubleOrNull() ?: 0.0) * m
}
```

**Catalog fallback (`ResolveCatalogCacheUseCase.kt`):**
```kotlin
val ingredientMatch = foodRepository.searchFoodByNameExact(
    parsed.name, Constants.INGREDIENT_CATALOG_ID
)
val match = ingredientMatch ?: foodRepository.searchFoodByNameExact(
    parsed.name, Constants.RECIPE_CATALOG_ID
)
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 60 | `isLoggingRecipe` driven by `setCatalogType()`, not just a manual toggle | The entry point matters — users arriving from the Recipes catalog FAB expect recipe builder mode, not a flat form. Centralising the mode decision in `setCatalogType()` ensures all navigation paths (catalog FAB, manual toggle) produce consistent state. |
| 61 | `updateFoodName()` must NOT clear `isLoggingRecipe` | Editing the recipe name is not a mode change. Clearing `isLoggingRecipe` on every keystroke collapses the ingredient list mid-editing, which is a destructive UX regression. Mode changes are only initiated explicitly by the user. |
| 62 | Dual-catalog fallback in `ResolveCatalogCacheUseCase` | Gemini's `isRecipe` classification is heuristic — a recipe the user already logged (stored in `RECIPE_CATALOG_ID`) may be returned as `isRecipe=false` on subsequent parses. Falling back to the recipe catalog prevents an unnecessary external API call and ensures catalog items are always reused regardless of AI classification drift. |
| 63 | Two-row layout for `ManualRecipeIngredientRow` (name + remove / qty + unit) | Placing all four controls in a single horizontal Row left the ingredient name field only ~84dp on a 360dp phone — too narrow for `OutlinedTextField` to render without vertical letter overflow. Splitting into two rows gives the name field the full card width (minus remove button), matching the webapp's `flex-col sm:flex-row` responsive pattern. |

---

## Phase 17: Serving Size Clarification — Brand-Aware Nutrition Lookup

**Status:** ✅ Completed
**Date:** May 27, 2026

### Summary

Foods with variable serving sizes (bread slices, cheese slices, tortillas, protein bars) now trigger an interactive clarification flow on the Log screen. The Gemini prompt detects serving-size ambiguity and sets `needsClarification = true` with a helpful hint. The nutrition lookup is paused until the user resolves the ambiguity by:

1. **"Use generic"** — accept the standard USDA/IFCT estimate
2. **Brand name** — trigger a brand-specific FDC Branded lookup (e.g. "Nature's Own" → FDC branded wheat bread)
3. **Weight override** — provide an explicit gram weight per serving unit (e.g. 40g)

The Android `NutritionRepositoryImpl` now supports a brand-aware tiered fallback: FDC Branded (when brand provided) → FDC All Types → IFCT full phrase → IFCT word-by-word → null. Each result includes a `matchType` field ("branded" / "generic" / null) for transparent match quality badges in the Compose UI.

Cross-platform parity with webapp Phase W10.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `util/GeminiPrompts.kt` | Updated | Added SERVING SIZE AMBIGUITY DETECTION section to `SYSTEM_INSTRUCTION`. Updated JSON schema in `buildUserPrompt()` with `needs_clarification` (Boolean) and `clarification_hint` (String?) fields. Mirrors webapp `prompts.ts`. |
| 2 | `data/remote/dto/GeminiResponse.kt` | Updated | Added `needsClarification: Boolean = false` and `clarificationHint: String? = null` to `GeminiParsedFoodDto`. |
| 3 | `domain/model/ParsedFood.kt` | Updated | Added `needsClarification: Boolean = false` and `clarificationHint: String? = null` to domain model. |
| 4 | `domain/model/NutritionInfo.kt` | Updated | Added `matchType: String? = null` for "branded"/"generic"/null. |
| 5 | `data/repository/AiRepositoryImpl.kt` | Updated | DTO→domain mapping now includes `needsClarification` and `clarificationHint`. |
| 6 | `domain/repository/NutritionRepository.kt` | Updated | `searchNutrition()` signature extended with `brand: String? = null` parameter. |
| 7 | `domain/usecase/LookupNutritionUseCase.kt` | Updated | Added `brand: String? = null` parameter, passes to repository. |
| 8 | `data/repository/NutritionRepositoryImpl.kt` | Updated | Brand-aware tiered fallback: Tier 1a: FDC Branded (`tryFdc("$brand $foodName", dataType = "Branded")`) → Tier 1b: FDC All Types → Tier 2–3: IFCT → null. Added `dataType` parameter to `tryFdc()`. Added `mapResults()` extension for applying `matchType`. |
| 9 | `data/remote/dto/FoodDataCentralResponse.kt` | Updated | Added `matchType = null` to `toNutritionInfo()` return. |
| 10 | `data/local/entity/IfctFoodEntity.kt` | Updated | Added `matchType = null` to `toNutritionInfo()` return. |
| 11 | `presentation/screens/log/LogViewModel.kt` | Updated | Added `ClarificationType` enum, `ClarificationResolution` data class, `clarificationResolutions` state in `LogUiState`. Added `resolveClarificationGeneric()`, `resolveClarificationWithBrand()`, `resolveClarificationWithWeight()`. Updated `acceptParsedFood()` to use `effectiveServingWeightG` from clarification override. Reset `clarificationResolutions` in `clearParsedFoods`/`parseWithAi`. |
| 12 | `presentation/screens/log/NutritionLookupDelegate.kt` | Updated | `performNutritionLookup()` accepts `brand: String? = null`. Added `performAndUpdateLookup(index, foodName, brand?)` for single-item re-lookups. Initial batch lookup skips `food.needsClarification` items. |
| 13 | `presentation/screens/log/LogScreen.kt` | Updated | Added `ClarificationBanner` composable: Card with warning hint, `OutlinedTextField`, "Use generic" / "Update & Lookup" buttons. Added `MatchTypeBadge` composable: `Surface` chip with color-coded text (green branded / amber generic). Wired into `ParsedFoodCard` — shows banner when `needsClarification && !resolved`, shows badge after nutrition resolves. Added `Surface` import. |

### Key Implementation Details

**Brand-aware tiered fallback in `NutritionRepositoryImpl`:**
```kotlin
override suspend fun searchNutrition(foodName: String, brand: String?): Resource<NutritionInfo> {
    // Tier 1a: FDC Branded (when brand provided)
    if (brand != null) {
        val branded = tryFdc("$brand $foodName", dataType = "Branded")
        if (branded != null) return Resource.Success(branded.mapResults("branded"))
    }
    // Tier 1b: FDC All Types (generic)
    val generic = tryFdc(foodName)
    if (generic != null) return Resource.Success(generic.mapResults("generic"))
    // Tier 2–3: IFCT → Tier 4: null
}
```

**Weight override priority in `LogViewModel.acceptParsedFood()`:**
```kotlin
val clarification = state.clarificationResolutions[state.selectedParsedFoodIndex]
val effectiveServingWeightG = clarification?.weightOverrideG ?: nutritionInfo.servingWeightG
val perUnitMultiplier = UnitConverter.computeServingMultiplier(
    quantity = 1.0,
    unit = targetFood.unit,
    servingWeightG = effectiveServingWeightG
)
```

**ClarificationBanner Compose UI:**
```kotlin
@Composable
private fun ClarificationBanner(
    hint: String,
    onUseGeneric: () -> Unit,
    onSubmitClarification: (String) -> Unit,
    isLoading: Boolean = false
) {
    var inputText by remember { mutableStateOf("") }
    // Card with hint, OutlinedTextField, two action buttons
}
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 64 | AI-level ambiguity detection over client-side heuristics | The Gemini model has food knowledge to determine which items have genuinely variable serving sizes. Client heuristics (e.g. "if unit is slice") would be brittle and miss edge cases like protein bars vs candy bars. |
| 65 | Brand-aware tiered FDC fallback in `NutritionRepositoryImpl` | Brand-specific data is most accurate for variable-size products. Graceful degradation (branded → generic → IFCT → null) ensures the user always gets some estimate. |
| 66 | `matchType` field for transparent match quality in UI | Badge color (green vs amber) communicates whether macros came from an exact brand match or a generic USDA average, helping users decide whether to trust or override the values. |
| 67 | Weight override applied in `acceptParsedFood()`, not in the lookup | When the user says "my bread slices are 40g", the per-100g macros from USDA are unchanged — only the serving multiplier changes. Applying the override at form-fill time keeps the lookup pipeline clean. |
| 68 | Nutrition lookup paused until clarification resolved | Firing a generic lookup and then overwriting it wastes bandwidth and causes a visual flicker. Pausing until the user acts is cleaner UX and consistent with the webapp (Phase W10). |

---

## Phase 17.1: Clarification Robustness — Prompt Expansion & Brand Validation

**Status:** ✅ Completed
**Date:** May 28, 2026

### Summary

Two post-implementation fixes based on real-world testing of Phase 17:

1. **AI prompt expanded to three ambiguity cases** — The original prompt only flagged variable-size discrete units (Case A: "1 slice bread"). Now also flags bare generic food names without type (Case B: "cheese" → cheddar? mozzarella? paneer?) and specific foods without concrete amounts (Case C: "cheddar cheese" → how much?). Hints guide users to specify weight in grams.

2. **FDC Branded result brand validation** — Searching FDC Branded for "Amul cheese" returned "Food Lion cheese" (wrong brand) because FDC's search is keyword-based. Now validates that the top result's `brand` field contains the user's requested brand (case-insensitive). Mismatches fall through to generic tier → UI correctly shows amber "Brand not found, using generic" badge.

Cross-platform parity with webapp Phase W10.1.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `util/GeminiPrompts.kt` | Updated | Expanded ambiguity detection: Case A (variable discrete units), Case B (bare generic names like "cheese", "bread", "milk"), Case C (specific food without concrete amount like "cheddar cheese", "white bread"). Weight-focused hint style with gram reference points. Mirrors webapp `prompts.ts`. |
| 2 | `data/repository/NutritionRepositoryImpl.kt` | Updated | Tier 1a brand validation: after `tryFdc("$brand $foodName", dataType = "Branded")`, checks that `topResult.brand` contains the requested brand string (case-insensitive, word-level matching). Mismatches log to TAG and fall through to generic tier. |

### Key Implementation Details

**Brand validation in `NutritionRepositoryImpl.kt`:**
```kotlin
val topResult = (brandedResult as? Resource.Success)?.data?.firstOrNull()
val resultBrand = (topResult?.brand ?: "").lowercase()
val requestedBrand = brand.lowercase().trim()
val brandMatches = resultBrand.contains(requestedBrand) ||
    requestedBrand.split("\\s+".toRegex()).any { word ->
        word.length > 2 && resultBrand.contains(word)
    }

if (brandMatches) {
    return brandedResult.mapResults { it.copy(matchType = "branded") }
}
// Fall through to generic — UI shows amber "Brand not found" badge
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 69 | Three-case ambiguity detection (A/B/C) over single-case | Case A alone missed two common scenarios: bare generic names ("cheese") and specific foods without amounts ("cheddar cheese"). Both produce meaningless default `1 serving` lookups. Case B+C catch these with weight-focused hints. |
| 70 | Brand validation via result field matching, not query restructuring | FDC's keyword search cannot be constrained to an exact brand. Post-hoc validation (checking the returned `brand` field) is simple, reliable, and degrades gracefully to generic when the brand isn't in FDC. |

---

## Phase 18: Edit Log Macro Scaling Guard — Gram ↔ Non-Gram Boundary

**Status:** ✅ Completed (symptomatic fix — architectural fix planned in per-100g normalization)
**Date:** June 2, 2026

### Summary

Fixed incorrect macro recalculation when editing a daily log entry on the Home screen and changing between gram and non-gram unit types. Root cause: `startEditLog()` derives `baseCalories = totalCalories / storedQty`, which produces a per-100g-relative value for gram entries but a per-serving value for non-gram entries. When the user changes the unit type (e.g., from "1 serving" to "150g"), `fromDisplayQty(150, "g")` returns 1.5, and `321 × 1.5 = 481 kcal` — wrong (expected: 241 kcal for 150g of a 321 kcal per 200g item).

**Symptomatic fix:** Skip auto-scaling entirely when the unit type crosses the gram ↔ non-gram boundary. The user must adjust macros manually after a unit-type change. This will be superseded by the per-100g macro normalization (planned), which will make all base macros per-100g and eliminate the mismatch.

### Bug Reproduction (Scenario 2)

1. Banana bread in recipe catalog: 321 kcal per 200g serving
2. AI parse → Log All (logs as "1 serving", 321 kcal)
3. Navigate to Home → edit entry → change to "150g"
4. **Before fix:** 481 kcal (321 × fromDisplayQty(150, "g") = 321 × 1.5)
5. **After fix:** Macros unchanged — user adjusts manually

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/home/HomeViewModel.kt` | Updated | `updateEditQty()` (lines 266–293): Added gram↔non-gram guard — compares `UnitConverter.isGramsUnit(unit)` against `UnitConverter.isGramsUnit(originalUnit)` (from `sheet.log.consumedUnit`). When they differ, updates qty text but skips macro recalculation. |
| 2 | `presentation/screens/home/HomeViewModel.kt` | Updated | `updateEditUnit()` (lines 305–327): Same guard — when new unit type differs from original log's unit type, updates unit field but skips macro recalculation. |

### Key Implementation Details

**Guard in `updateEditQty()`:**
```kotlin
val originalUnit = sheet.log.consumedUnit
if (UnitConverter.isGramsUnit(unit) != UnitConverter.isGramsUnit(originalUnit)) {
    _editSheet.value = sheet.copy(qty = value, errorMessage = null)
    return
}
```

**Why this is a symptomatic fix:**
- The real issue is that `base_calories` on `FoodItem` is per-serving (e.g., 321 kcal for a 200g serving) but `computeServingMultiplier()` and `fromDisplayQty()` assume per-100g base
- The guard prevents the wrong calculation from running, but doesn't fix the underlying data model
- The planned per-100g normalization will store `base_calories = 321 × (100/200) = 160.5` and the formula `160.5 × 1.5 = 240.75 ≈ 241 kcal` will be correct, making this guard unnecessary

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 77 | Skip auto-scaling on gram ↔ non-gram unit type change | Per-serving macros cannot be meaningfully converted to per-100g (or vice versa) without the serving weight, which the daily log does not carry. Skipping is safe — the user can still manually edit macro fields. This guard will be removed after per-100g normalization. |

---

## Feature 1: Internet Recommendations Infrastructure

### Phase R4: User Profile Setup — Android

**Status:** ✅ Completed
**Date:** June 2, 2026

### Summary

Full user profile setup for AI-powered meal recommendations. Users can configure dietary preferences (diet type, cuisines, allergies, weight goal, age, gender, weight) which feed into the recommendation prompt when `includeInternet=true`. Profile is persisted in Room (merged into the existing `user_preferences` table) and synced to Supabase via the existing bidirectional sync pipeline.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `domain/model/UserProfile.kt` | **New** | Domain model with 8 profile fields: `age`, `gender`, `weightKg`, `weightGoal`, `dietType`, `cuisinePreferences: List<String>`, `allergies: List<String>`, `recommendationsEnabled`. Computed `isComplete` property: `dietType != null && recommendationsEnabled`. |
| 2 | `data/local/entity/UserPreferencesEntity.kt` | Updated | Added 8 nullable columns (`age`, `gender`, `weight_kg`, `weight_goal`, `diet_type`, `cuisine_preferences`, `allergies`, `recommendations_enabled`). Arrays stored as CSV strings in SQLite TEXT columns. Added `toUserProfile()` mapper splitting CSV → `List<String>`. |
| 3 | `data/local/migrations/Migrations.kt` | Updated | Added `MIGRATION_7_8` — 8 `ALTER TABLE user_preferences ADD COLUMN` statements. SQLite types: `INTEGER` for age, `TEXT` for strings, `REAL` for weight_kg, `INTEGER NOT NULL DEFAULT 0` for recommendations_enabled. Updated `ALL` array. |
| 4 | `data/local/NutriAiDatabase.kt` | Updated | Bumped `version = 7` → `version = 8`. Added v8 version history comment. |
| 5 | `util/Constants.kt` | Updated | Bumped `DATABASE_VERSION = 7` → `DATABASE_VERSION = 8`. |
| 6 | `data/local/preferences/UserPreferences.kt` | Updated | Added `profileFlow: Flow<UserProfile>` reading from Room. Added `saveProfile()` with read-before-write pattern to preserve macro goal columns. **Critical fix:** Changed `saveMacroGoals()` from creating a new entity (which wiped profile fields) to read-before-write with `.copy()` to preserve existing profile data. |
| 7 | `data/remote/dto/SupabaseSyncDto.kt` | Updated | Extended `RemoteUserPreferencesDto` (pull) and `RemoteUserPreferencesPushDto` (push) with 8 profile fields. `List<String>` for cuisines/allergies (Supabase TEXT[] deserializes as `List`). Updated `toRemoteDto()` mapper: CSV → `split(",")` → `List<String>`. Updated `toEntity()` mapper: `List<String>` → `joinToString(",")` → CSV. |
| 8 | `presentation/screens/auth/ProfileSetupSheet.kt` | **New** | Bottom sheet content composable with: enable toggle, age/weight text fields, gender/diet type dropdowns (`ExposedDropdownMenuBox`), weight goal `FilterChip` grid, cuisine/allergy multi-select `FilterChip` grids with custom text input. Reusable `DropdownField` helper. |
| 9 | `presentation/screens/auth/AuthViewModel.kt` | Updated | Added `userProfile: StateFlow<UserProfile>` collected from `userPreferences.profileFlow`. Added `saveProfile(profile: UserProfile)` dispatching to `userPreferences.saveProfile()`. |
| 10 | `presentation/screens/auth/AuthScreen.kt` | Updated | Added AI Recommendations `ElevatedCard` in `ProfilePanel` between Nutrition Goals and Sign Out. Card shows `AutoAwesome` icon, status text (Enabled/Set up), opens `ModalBottomSheet` with `ProfileSetupSheetContent`. Added `userProfile` parameter threading through `AuthContent` → `ProfilePanel`. |

### Key Implementation Details

**Room migration v7 → v8:**
```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN age INTEGER")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN gender TEXT")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN weight_kg REAL")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN weight_goal TEXT")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN diet_type TEXT")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN cuisine_preferences TEXT")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN allergies TEXT")
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN recommendations_enabled INTEGER NOT NULL DEFAULT 0")
    }
}
```

**Read-before-write pattern (critical for dual-purpose table):**
```kotlin
suspend fun saveMacroGoals(goals: MacroGoals) {
    val existing = userPreferencesDao.getPreferences(Constants.LOCAL_USER_ID)
    val entity = (existing ?: UserPreferencesEntity(userId = Constants.LOCAL_USER_ID)).copy(
        calorieGoal = goals.calorieGoal, proteinGoal = goals.proteinGoal,
        carbsGoal = goals.carbsGoal, fatGoal = goals.fatGoal,
        isSynced = false, lastModifiedAt = System.currentTimeMillis()
    )
    userPreferencesDao.upsertPreferences(entity)
}
```

**Custom cuisine/allergy input with sanitization:**
```kotlin
private const val MAX_ENTRY_LENGTH = 40

private fun sanitizeEntry(raw: String): String =
    raw.replace(Regex("<[^>]*>"), "")
        .replace(Regex("[#*_~`\\[\\]{}()|\\\\]"), "")
        .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
        .trim()
        .take(MAX_ENTRY_LENGTH)
```

### Prompt Injection Defense (3-Layer)

Custom cuisine and allergy entries are user-provided free text that flows into the AI recommendation prompt. Three layers of sanitization prevent prompt injection:

| Layer | Location | Function | Purpose |
|-------|----------|----------|---------|
| 1 (Authoritative) | `supabase/functions/recommend-meals/index.ts` | `sanitizeProfileEntry()` | Server-side — strips HTML, markdown, control chars, truncates to 40 chars. Applied when reading from Supabase before injecting into prompt. |
| 2 | `webapp/app/settings/profile-section.tsx` | `sanitizeEntry()` + `maxLength={40}` | Webapp client — identical logic, pre-save. |
| 3 | `ProfileSetupSheet.kt` | `sanitizeEntry()` + `MAX_ENTRY_LENGTH = 40` | Android client — identical logic, pre-save. |

All three strip: HTML tags (`<[^>]*>`), markdown/special chars (`[#*_~`\[\]{}()|\\]`), control characters (`[\x00-\x1F\x7F]`), and truncate to 40 characters.

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 71 | Profile columns merged into existing `user_preferences` table | Single-row-per-user table already exists for macro goals. Separate table would require a second sync entity, double the upsert logic, and complicate the read-before-write pattern. 8 nullable columns on the existing table keeps things simple. |
| 72 | CSV string storage for arrays in Room | Room doesn't support `List<String>` columns natively. TypeConverters add annotation boilerplate. CSV with `split(",")` / `joinToString(",")` is transparent, searchable via `LIKE`, and maps cleanly to Supabase `TEXT[]` via the sync DTO layer. |
| 73 | Read-before-write for both `saveMacroGoals()` and `saveProfile()` | The `user_preferences` entity serves dual purpose (macro goals + profile). Without read-before-write, saving goals would wipe profile fields and vice versa. Reading the existing entity and `.copy()`-ing only the changed fields preserves both sets of data. |
| 74 | 3-layer sanitization for custom profile entries | Server-side sanitization is authoritative (defense in depth). Client-side sanitization provides immediate UX feedback and reduces round-trip waste. Identical logic on both clients prevents platform-specific injection vectors. |
| 75 | Ephemeral `ModalBottomSheet` — no race condition | Unlike the webapp (which uses persistent React state that survives save/refetch cycles), Android's `ModalBottomSheet` state is destroyed on dismiss. When reopened, `rememberSaveable` re-initializes from the fresh `initialProfile` — no stale cache interleaving possible. |
| 76 | `MAX_ENTRY_LENGTH = 40` for custom entries | No real cuisine or allergy name exceeds 40 characters. Truncation limits prompt token waste from malicious long inputs and is enforced at all three layers. |

### Edge Function Deployment Required

```bash
supabase functions deploy recommend-meals  # sanitizeProfileEntry + profile-aware prompts
```

---

## Dev Environment Setup

**Date:** April 27, 2026

### Prerequisites Installed
| Component | Version | Method |
|-----------|---------|--------|
| Java JDK | 17 (OpenJDK via Homebrew) | `brew install openjdk@17` |
| Android SDK Command-Line Tools | 20.0 | Manual download + extract |
| Android SDK Platform | 35 (Android 15) | `sdkmanager` |
| Android SDK Build-Tools | 35.0.0 | `sdkmanager` |
| Android Platform-Tools | 37.0.0 | `sdkmanager` |
| Gradle | 8.11.1 (via wrapper) | Auto-downloaded |

### Environment Variables (added to `~/.zshrc`)
```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/35.0.0:$PATH"
```

### Build Commands
```bash
# Debug build
cd ~/Documents/Wibey/NutriAI
JAVA_HOME="/opt/homebrew/opt/openjdk@17" ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew assembleDebug

# APK location
ls app/build/outputs/apk/debug/app-debug.apk
```

### Network Note
Corporate proxy (`proxy-intlho.wal-mart.com:8080`) blocks Gradle dependency downloads. **Disconnect from VPN** and clear `~/.gradle/gradle.properties` before building. Dependencies are cached after first successful build.

---

## Continuation Summary (for new chat sessions)

> Paste the section below into a new chat to continue development from where we left off.

### Project Location
`~/Documents/Wibey/NutriAI`

### What's Been Completed

**Phase 1: Project Scaffolding & Core Architecture** ✅
- Full Clean Architecture project structure (domain/data/presentation layers)
- Gradle build with version catalog (`libs.versions.toml`) — Compose BOM, Hilt, Room, Retrofit, WorkManager, Kotlinx Serialization
- Hilt DI setup (`NutriAiApplication`, `AppModule` with OkHttp, Retrofit instances)
- Material3 theme (green/health palette, light/dark, dynamic colors)
- Navigation with bottom bar (Home, Catalog, Profile) + Log screen
- Domain models: `User`, `Catalog`, `FoodItem`, `DailyLog`
- Repository interfaces: `UserRepository`, `CatalogRepository`, `FoodRepository`, `DailyLogRepository`
- Utility classes: `Resource<T>` sealed class, `Constants`

**Phase 2: Room Database & Local Data Layer** ✅
- Room entities with `is_synced`, `deleted_at` tombstones, `last_modified_at` for LWW
- Foreign keys with CASCADE: User→Catalog→FoodItem, User/FoodItem→DailyLog
- Flow-based DAOs, Entity↔Domain mappers, Repository implementations
- Hilt DI: `DatabaseProviderModule` + `RepositoryBindingsModule`

**Phase 3: Presentation Layer — Core UI Screens** ✅
- 6 domain UseCases, 3 `@HiltViewModel` classes, 3 reusable Compose components
- Home screen (date nav, macro summary, food log), Log screen (manual entry form), Catalog screen (search + delete)
- Content/state separation pattern for `@Preview` support

**Phase 3.5: Post-Implementation Bug Fixes** ✅
- Fixed UUID display, catalog ID mismatch, single-catalog bug, off-by-one date, missing delete buttons

**Phase 4: AI Pipeline — Gemma 4 API Integration** ✅
- **Model:** `gemma-4-26b-a4b-it` (Google's open Gemma 4 model, Apache 2.0) hosted on the Gemini API
- **Endpoint:** `POST v1beta/models/gemma-4-26b-a4b-it:generateContent` with API key query param
- **API key:** `BuildConfig.GEMINI_API_KEY` from `local.properties` → setup at [Google AI Studio](https://aistudio.google.com/app/apikey)
- Two input modes: AI Parse (default) + Manual Entry, switchable via tabs
- `extractJson()` fallback for markdown fences/thinking tags, `responseMimeType: "application/json"`, low temperature (0.1)
- Graceful fallback: AI errors show "Enter Manually Instead" button

**Phase 4.5: Recipe Detection, Catalog Cache & Bug Fixes** ✅
- **Bug fixes:** Fixed 404 error (wrong model name `gemma-4-27b-it` → `gemma-4-26b-a4b-it`), empty AI response (Gemma 4 returns `thought: true` parts — now filtered), wrong thinking config (`thinkingBudget: Int` → `thinkingLevel: "MINIMAL"`)
- **Recipe-aware AI parsing:** Gemma 4 prompt detects compound dishes (e.g., "Dosa and Chutney: ingredients - 100g sooji, 50g yogurt, 20g chutney powder, 1tsp oil") and returns nested `is_recipe: true` + `ingredients` array. `ParsedFood` model supports `isRecipe: Boolean` and `ingredients: List<ParsedFood>`. `GeminiParsedFoodDto` has self-referential `ingredients` list.
- **Catalog caching:** `ResolveCatalogCacheUseCase` checks each parsed ingredient against local Room DB (case-insensitive exact name match via `FoodItemDao.searchFoodByNameExact()`). `CatalogMatch` domain model links `ParsedFood` → cached `FoodItem`. UI shows "In catalog ✅" for cached ingredients, "New" for new ones.
- **Recipe logging:** `LogFoodUseCase.logRecipe()` creates individual `FoodItem` for each new ingredient + recipe `FoodItem` with aggregated macros + `DailyLog` entry
- **Ingredient-level editing:** `selectedIngredientIndex: Int?` state in `LogViewModel`. Tappable `IngredientRow` in recipe cards with primary color highlight + edit icon. "Edit Ingredient" opens manual form pre-filled with that ingredient's data (including cached macros from catalog if available).
- **Verified on-device:** Recipe parsing, catalog badges, and ingredient editing all working correctly

**Phase 4.6: AI Name Standardization via Catalog Context** ✅
- **Goal:** Prevent duplicate catalog entries from synonym variations (e.g., "besan flour" → "besan" when "besan" already exists)
- **Data layer:** Added `getAllFoodNames(catalogId): List<String>` to `FoodItemDao`, `FoodRepository`, `FoodRepositoryImpl` — lightweight `SELECT name` query, scoped to a catalog, filtered to non-deleted rows
- **Prompt:** `SYSTEM_INSTRUCTION` has a new `NAME STANDARDIZATION RULES` section (static rules — case-insensitive matching, preserve catalog casing, only normalize on confident match). `buildUserPrompt()` now accepts `existingIngredients` and `existingRecipes` lists; injects both as labelled sections before the JSON schema when non-empty; defaults to `emptyList()` (sections omitted on first launch)
- **`AiRepositoryImpl`:** Injects `FoodRepository`; fetches both lists before each request; passes to `buildUserPrompt()`. No UseCase or ViewModel changes required.
- **Token cost:** ~1,500–2,000 tokens for a full catalog (~200 items) — negligible for a single-user, single-turn app

**Open Bugs (at time of Phase 4.6 — all subsequently fixed):**
- ~~Deleting food from Catalog causes "Unknown Food" on Home~~ — ✅ Fixed (Phase 8: `food_name` snapshot + SET NULL FK)
- ~~Macro goals hardcoded~~ — ✅ Fixed (Phase 7: `MacroGoals` DataStore + goals sheet)
- ~~No confirmation dialog on delete actions~~ — ✅ Fixed (Phase 7: `ConfirmDeleteDialog`)

### Key Files
- **Plan:** `~/.wibey/plans/ai_nutrition_app_492b3550.plan.md` — Full 7-phase implementation plan
- **Dev Log:** `~/Documents/Wibey/NutriAI/DEVLOG.md` — Tracks all changes, decisions, and setup
- **Phase 4.5 Plan:** `~/.wibey/plans/phase4_recipe_detection_and_catalog_cache.plan.md` — Recipe detection + catalog cache implementation plan

### What's Completed: Phase 5 + 5.5 — Nutrition Grounding ✅

**Phase 5** built the full nutrition pipeline: `NutritionRepository` interface, `LookupNutritionUseCase`, `NutritionLookupState` sealed class, concurrent `async`/`awaitAll` lookups in `LogViewModel`, `computeServingMultiplier()`, and per-item UI states.

**Phase 5.5** replaced OpenFoodFacts with a two-tier chain:
- **USDA FDC** (online, `.gov` domain, passes corporate proxy) — key stored in `local.properties` as `USDA_FDC_API_KEY`
- **IFCT 2017** (offline, 120 NIN-verified Indian foods in `assets/ifct2017.csv`) — seeded to Room `ifct_foods` table on first launch via `IfctCsvLoader`
- Falls through to empty list → user fills manually

**Pending:** Add `USDA_FDC_API_KEY` to `local.properties` (free — https://fdc.nal.usda.gov/api-key-signup.html)

---

### What's Completed: Phase 6 — Authentication & Supabase Cloud Sync ✅

**Phase 6** implemented full offline-first auth + cloud sync:
- **Supabase GoTrue auth** — email/password sign-up and sign-in via Retrofit (no Supabase SDK). `AuthRepositoryImpl` maps GoTrue responses to `AuthSession`; tokens persisted in DataStore. `@Volatile cachedToken` bridges async DataStore with synchronous OkHttp interceptors.
- **Supabase PostgREST sync** — `SyncRepositoryImpl` pushes unsynced Room entities and pulls remote rows with LWW conflict resolution. Catalog IDs prefixed with Supabase UUID (`{uuid}_local_user_recipes`) to prevent PRIMARY KEY collisions.
- **WorkManager background sync** — `SyncWorker` (`@HiltWorker`) runs every 12h on `CONNECTED` network. `NutriAiApplication` implements `Configuration.Provider` + `HiltWorkerFactory`; default `WorkManagerInitializer` removed from Manifest.
- **Auth/Profile screen** — replaced placeholder with 3-panel `AnimatedContent`: Loading → Sign-In/Sign-Up form → Profile card (email, sync status, sign out).
- **Offline fix** — `SyncDataUseCase` checks `NET_CAPABILITY_VALIDATED` before any network call; returns `Resource.Error("No internet connection")` immediately when offline instead of false-positive success.

**Supabase setup required (one-time):**
Run in Supabase SQL Editor: `CREATE TABLE` for `users`, `catalogs`, `food_items`, `daily_logs` + `ENABLE ROW LEVEL SECURITY` + `CREATE POLICY` + `GRANT ... TO authenticated`. Schema is documented in the Phase 6 section above.

**Open bugs at end of Phase 6 (all subsequently fixed):**
- ~~JWT auto-refresh not implemented~~ — ✅ Fixed (Phase 9)
- ~~Edit log entry not wired up~~ — ✅ Fixed (Phase 6, confirmed in Phase 7 exploration)
- ~~Macro goals hardcoded~~ — ✅ Fixed (Phase 7)

### What's Completed: Phase 8 — Live Macro Sync & Data Integrity ✅

**Problem solved:** Editing a food item's macros in the Catalog (or Supabase dashboard) was not reflected in the Daily Log page. Daily logs stored a static snapshot at creation time with no mechanism to update them.

**Phase 8 Pre-work I — Sync Infrastructure (already in v4)**
- Added `updated_at` server-side timestamp columns (Postgres trigger) to `catalogs`, `food_items`, `daily_logs` — eliminates client clock skew for incremental pull cursor
- Last-Write-Wins (LWW) guard triggers on all three tables — silently reject stale pushes
- Performance indexes on `(user_id, updated_at)` / `(catalog_id, updated_at)` for incremental pull queries
- Migration `MIGRATION_3_4` — no schema change, bootstraps explicit migration infrastructure

**Phase 8 — Live Macro Sync (v5)**

*Dynamic display layer:*
- `DailyLogWithFood` POJO — Room LEFT JOIN result pairing `DailyLogEntity` with optional `FoodItemEntity` (prefix `fi_` to avoid `food_name` column collision)
- `getLogsWithFoodByDateRange()` in `DailyLogDao` — LEFT JOIN query with `fi_` aliased columns; Room auto-invalidates the Flow when either `daily_logs` or `food_items` changes
- `DailyLogWithFood.toDomain()` mapper — computes `food.baseMacro × consumedQty` when food item is live; falls back to stored snapshot when food is null (deleted/purged). `consumedQty` is always "number of servings" — unit conversion is baked into base macros at food-item creation time, so the formula is correct for all unit types (g, piece, tsp, etc.)
- `getLogsWithFoodByDateRange()` wired through `DailyLogRepository` → `DailyLogRepositoryImpl` → `GetDailyLogsUseCase`

*Stored value recalculation (sync layer):*
- `recalculateLogsForFoodItem(foodItemId)` SQL in `DailyLogDao` — bulk-updates `total_*` columns and marks rows `is_synced = 0` for all active logs referencing the updated food item
- Called from `SyncRepositoryImpl.pullRemoteChanges()` after a food item UPDATE is applied locally, gated on `didUpdate && entity.deletedAt == null`
- Called from `FoodRepositoryImpl.updateFood()` so local edits also recalculate immediately

*Post-pull push (same-cycle sync):*
- `syncAll()` now runs: push → pull → **post-pull push**
- Ensures recalculated logs (dirtied during pull) are pushed to Supabase in the same sync cycle, not the next one. Post-pull push failure is non-fatal — dirty rows retry on next sync.

*FK SET NULL migration (data integrity):*
- Root cause found: `ON DELETE CASCADE` on `daily_logs.food_item_id → food_items.id` caused tombstone purge to cascade-delete all associated daily log rows, wiping historical macro data
- `DailyLogEntity.foodItemId` changed to `String? = null`; `DailyLog.foodItemId` and `RemoteDailyLogDto.foodItemId` also made nullable
- FK changed to `ON DELETE SET NULL` — purging a food item tombstone now nullifies `food_item_id` in associated logs instead of deleting the log rows
- `MIGRATION_4_5` — full table recreation (SQLite doesn't support `ALTER TABLE` to change FK constraints): CREATE new → INSERT → DROP old → RENAME → recreate 3 indices
- All three tombstone purges restored in `pushLocalChanges()` (were temporarily removed as a workaround)
- Orphaned logs (`food_item_id = NULL` after purge) filtered from push and marked `is_synced = 1` to prevent accumulation
- `NutriAiDatabase` bumped to v5

*Remote tombstone TTL:*
- `supabase/migrations/002_tombstone_purge_cron.sql` — `pg_cron` weekly job (every Sunday 03:00 UTC)
- Hard-deletes tombstones older than 15 days from `daily_logs`, `food_items`, `catalogs`
- Deletion order: children first (`daily_logs → food_items → catalogs`) to avoid unnecessary FK nullification
- `deleted_at` is epoch milliseconds — threshold: `EXTRACT(EPOCH FROM now() - interval '15 days') * 1000`

**Files changed:**
- `data/local/entity/DailyLogWithFood.kt` — NEW
- `data/local/entity/DailyLogEntity.kt` — `foodItemId` nullable, FK SET_NULL
- `data/local/dao/DailyLogDao.kt` — `getLogsWithFoodByDateRange()`, `recalculateLogsForFoodItem()`
- `data/local/mapper/DailyLogMapper.kt` — `DailyLogWithFood.toDomain()` with dynamic calc + fallback
- `data/local/migrations/Migrations.kt` — `MIGRATION_4_5`, added to `ALL`
- `data/local/NutriAiDatabase.kt` — version 4 → 5
- `data/remote/dto/SupabaseSyncDto.kt` — `RemoteDailyLogDto.foodItemId` nullable
- `data/repository/DailyLogRepositoryImpl.kt` — `getLogsWithFoodByDateRange()` implementation
- `data/repository/FoodRepositoryImpl.kt` — `recalculateLogsForFoodItem()` on `updateFood()`
- `data/repository/SyncRepositoryImpl.kt` — recalculate on pull, post-pull push, orphan filter, tombstone purges restored
- `domain/model/DailyLog.kt` — `foodItemId` nullable
- `domain/repository/DailyLogRepository.kt` — `getLogsWithFoodByDateRange()` interface
- `domain/usecase/GetDailyLogsUseCase.kt` — switched to JOIN query
- `supabase/migrations/002_tombstone_purge_cron.sql` — NEW

**Open bugs at end of Phase 8 (all subsequently fixed):**
- ~~JWT auto-refresh not implemented~~ — ✅ Fixed (Phase 9)
- ~~Edit log entry not wired up~~ — ✅ Fixed (Phase 6, confirmed during Phase 7/8 exploration)
- ~~Macro goals hardcoded~~ — ✅ Fixed (Phase 7)

### What's Completed: Phase 9 — JWT Auto-Refresh ✅

**Problem solved:** Supabase access tokens expire in ~1 hour. After expiry, all PostgREST sync calls (push/pull) silently failed. `SyncDataUseCase` had a proactive 5-minute pre-expiry check but push-on-write (`SyncPushManager`, fires 500ms after every mutation) had no refresh path at all — any write after 1 hour would fail silently.

**Fix:** Implemented `SupabaseAuthenticator` — an OkHttp `Authenticator` that fires automatically on any 401 response from Supabase, refreshes the access token transparently, and retries the original request with the new JWT.

- `data/remote/auth/SupabaseAuthenticator.kt` — NEW
  - Makes a **raw** (non-Retrofit) HTTP call to GoTrue `/auth/v1/token?grant_type=refresh_token` to avoid Retrofit interceptor/authenticator recursion
  - On success: updates `AuthPreferences` (persisted + in-memory cache), retries the original request
  - On 400/401 from GoTrue: clears the session — user is prompted to sign in again
  - `X-Auth-Retry` header prevents infinite retry loops (gives up after one retry)
  - Uses bare `OkHttpClient` with `Proxy.NO_PROXY` for the refresh call (matches main client config)
- `di/SupabaseModule.kt` — added `SupabaseAuthenticator` injection + `.authenticator(authenticator)` on the Supabase `OkHttpClient`

**Layered defence:**
- `SyncDataUseCase.refreshSessionIfNeeded()` — proactive: refreshes before sync if token expires within 5 min (avoids the 401 round-trip when possible)
- `SupabaseAuthenticator` — reactive: handles the 401 case mid-operation (e.g. push-on-write after token expiry, or token expired between the pre-check and the actual HTTP call)

### What's Completed: Phase 11 — Nutrition Label Scanner ✅

See full Phase 11 entry above. Camera button on Log screen → photo picker → Gemma 4 vision reads label → macros pre-fill form as per-100g values. Photos stored locally with 10-day TTL. Never synced to Supabase.

### What's Next: Phase 10 — Further Polish & Production Readiness

### System Design Reference
- **Architecture:** MVVM + Clean Architecture with Hilt DI
- **Data flow:** UI → ViewModel → UseCase → Repository → Room DB (single source of truth) → Kotlin Flows back up
- **AI Pipeline:** Natural language → Gemma 4 API (entity extraction + recipe detection) → Catalog cache resolution → OpenFoodFacts (nutrition grounding, Phase 5) → Local Kotlin math → Room
- **Sync:** WorkManager every 12h on Wi-Fi, batch upsert to Supabase, tombstone-based soft deletes, Last-Write-Wins conflict resolution
- **Schema:** User → Catalog → FoodItem, User → DailyLog → FoodItem

### Build Command
```bash
cd ~/Documents/Wibey/NutriAI
JAVA_HOME="/opt/homebrew/opt/openjdk@17" ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew assembleDebug
```

### Dev Environment
- Android SDK at `~/Library/Android/sdk` (Platform 35, Build-Tools 35)
- Java 17 (OpenJDK via Homebrew)
- **Important:** Must disconnect VPN and clear `~/.gradle/gradle.properties` proxy settings before building

### Project Structure (Post-Phase 5)
```
com/app/nutriai/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── UserDao.kt, CatalogDao.kt, DailyLogDao.kt  (Phase 2)
│   │   │   └── FoodItemDao.kt              (Phase 2 + 4.5: searchFoodByNameExact + 4.6: getAllFoodNames)
│   │   ├── entity/                         (Phase 2: all 4 entities)
│   │   ├── mapper/                         (Phase 2: all 4 mappers)
│   │   └── NutriAiDatabase.kt             (Phase 2)
│   ├── remote/
│   │   ├── api/
│   │   │   ├── GeminiApiService.kt          (Phase 4, fixed in 4.5: model name)
│   │   │   └── OpenFoodFactsApiService.kt   (Phase 5: NEW — to be replaced in Phase 5.5)
│   │   └── dto/
│   │       ├── GeminiRequest.kt             (Phase 4, fixed in 4.5: thinkingLevel)
│   │       ├── GeminiResponse.kt            (Phase 4 + 4.5: thought field, isRecipe, ingredients)
│   │       └── OpenFoodFactsResponse.kt     (Phase 5: NEW — OFFSearchResponse, OFFProduct, OFFNutriments)
│   └── repository/
│       ├── AiRepositoryImpl.kt              (Phase 4 + 4.5 + 4.6: thought filtering, ingredients, catalog names)
│       ├── NutritionRepositoryImpl.kt       (Phase 5: NEW — OFFs lookup, macro ranking, graceful error handling)
│       ├── FoodRepositoryImpl.kt            (Phase 2 + 4.5 + 4.6: searchFoodByNameExact, getAllFoodNames)
│       ├── UserRepositoryImpl.kt            (Phase 2)
│       ├── CatalogRepositoryImpl.kt         (Phase 2)
│       └── DailyLogRepositoryImpl.kt        (Phase 2)
├── domain/
│   ├── model/
│   │   ├── NutritionInfo.kt                (Phase 5: NEW — per-100g domain model with source + externalId)
│   │   ├── ParsedFood.kt                   (Phase 4 + 4.5: isRecipe, ingredients)
│   │   ├── CatalogMatch.kt                 (Phase 4.5: NEW — parsedFood + matchedFoodItem)
│   │   └── User.kt, Catalog.kt, FoodItem.kt, DailyLog.kt  (Phase 1)
│   ├── repository/
│   │   ├── NutritionRepository.kt          (Phase 5: NEW — searchNutrition interface)
│   │   ├── AiRepository.kt                 (Phase 4)
│   │   ├── FoodRepository.kt               (Phase 1 + 4.5 + 4.6)
│   │   └── User/Catalog/DailyLog repos     (Phase 1)
│   └── usecase/
│       ├── LookupNutritionUseCase.kt        (Phase 5: NEW — with graceful degradation for blocked domains)
│       ├── ParseFoodWithAiUseCase.kt        (Phase 4)
│       ├── ResolveCatalogCacheUseCase.kt    (Phase 4.5: NEW — catalog cache resolution)
│       ├── LogFoodUseCase.kt                (Phase 3 + 4.5 + 5: logRecipe 3-case ingredient handling)
│       └── 5 other UseCases                 (Phase 3)
├── di/
│   ├── AppModule.kt        (Phase 1 + 4 + 5: GeminiApiService, @Named("openfoodfacts") OkHttpClient + Retrofit)
│   └── DatabaseModule.kt   (Phase 2 + 4 + 5: AiRepository + NutritionRepository bindings)
├── presentation/
│   ├── components/
│   │   └── NutritionMatchCard.kt           (Phase 5: NEW — per-100g macro display card)
│   ├── screens/log/
│   │   ├── LogViewModel.kt  (Phase 3 + 4 + 4.5 + 5: NutritionLookupState, lookupNutrition, computeServingMultiplier)
│   │   └── LogScreen.kt     (Phase 3 + 4 + 4.5 + 5: NutritionStatusRow, ingredient nutrition badges)
│   └── ... (other screens unchanged)
└── util/
    ├── GeminiPrompts.kt     (Phase 4 + 4.5 + 4.6: recipe detection + name standardization rules)
    ├── Constants.kt         (Phase 1 + 3.5: DEFAULT_CATALOG_ID)
    └── Resource.kt          (Phase 1)
```

**Phase 5.5 additions (planned):**
```
data/
├── local/
│   ├── dao/IFCTFoodDao.kt                   (IFCT 2017 local search)
│   └── entity/IFCTFoodEntity.kt             (per-100g macros, food name, food group)
└── remote/
    ├── api/FoodDataCentralApiService.kt     (USDA FDC search endpoint)
    └── dto/FDCResponse.kt                   (foodNutrients[] name→macro mapping)
domain/repository/
└── NutritionRepository.kt                  (unchanged — same interface)
assets/
└── ifct2017.csv                             (bundled ~900-row Indian NIN dataset)
util/
└── IFCTLoader.kt                            (idempotent CSV→Room seeder, runs on first launch)
```

---

## Phase 19: Catalog Re-Log Macro Fix — Per-100g De-Normalization

**Status:** ✅ Completed
**Date:** June 4, 2026

### Summary

Fixed incorrect daily log totals when re-logging a catalog item whose `base_serving_g ≠ 100` (e.g., recipes created via the recipe builder). The `log-food` Edge Function / `LogFoodUseCase` expects **per-serving** macros, but catalog items store **per-100g** macros. Two of three save paths were sending per-100g values without de-normalizing, causing the Edge Function to double-normalize.

**Symptom:** A recipe with 365 kcal per 400g serving (stored as `base_calories = 91.25` per-100g, `base_serving_g = 400`) would log as 91 kcal instead of 365 kcal for 1 serving.

**Root cause:** The Edge Function normalizes incoming macros via `normMacro = rawMacro × (100 / servingG)`. When per-100g values are sent with `servingG = 400`, double-normalization occurs: `91.25 × (100/400) = 22.8`, then `22.8 × scaleFactor(4.0) = 91.25` — producing the per-100g value instead of the per-serving total.

**Fix:** De-normalize catalog macros before sending to the save function: `perServing = per100g × (servingG / 100)`. This mirrors the existing correct logic in `acceptAndLogAllParsed()` (lines 818-821).

### Bug Reproduction

1. Create recipe "Mango Smoothie" via manual recipe builder with ingredients totaling 400g and 365 kcal
2. Recipe stored: `base_serving_g = 400`, `base_calories = 91.25` (per-100g)
3. Later, log "mango smoothie" via AI → catalog match found
4. **Before fix:** Daily log shows 91 kcal (per-100g value, not per-serving)
5. **After fix:** Daily log shows 365 kcal (correct per-serving total)

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/log/LogViewModel.kt` | Updated | Added `val servingG: Double = Constants.PER_100G_BASE` to `LogUiState` data class. Preserves the catalog item's actual serving weight for the `saveLog()` de-normalization step. |
| 2 | `presentation/screens/log/LogViewModel.kt` | Updated | `acceptParsedFood()`: Sets `servingG = cachedFood?.baseServingG ?: Constants.PER_100G_BASE` when populating the manual form from a catalog hit. Defaults to 100 for nutrition-lookup and manual-entry paths. |
| 3 | `presentation/screens/log/LogViewModel.kt` | Updated | `saveLog()`: Replaced hardcoded `servingG = Constants.PER_100G_BASE` with `state.servingG`. Added `deNorm = actualServingG / PER_100G_BASE` multiplier applied to all macro fields before calling `LogFoodUseCase.invoke()`. For default `servingG = 100`, `deNorm = 1.0` — no change to existing behavior. |
| 4 | `presentation/screens/log/LogViewModel.kt` | Updated | `updateFoodName()`: Resets `servingG = Constants.PER_100G_BASE` when user edits the food name (breaks catalog link). Prevents stale catalog serving weight from persisting to a new custom item. |

### Key Implementation Details

**De-normalization in `saveLog()`:**
```kotlin
val actualServingG = state.servingG
val deNorm = actualServingG / Constants.PER_100G_BASE

logFoodUseCase(
    servingG = actualServingG,
    calories = state.calories.toDouble() * deNorm,
    protein = (state.protein.toDoubleOrNull() ?: 0.0) * deNorm,
    carbs = (state.carbs.toDoubleOrNull() ?: 0.0) * deNorm,
    fat = (state.fat.toDoubleOrNull() ?: 0.0) * deNorm,
    ...
)
```

**Affected paths and status:**

| Path | Before | After |
|------|--------|-------|
| `acceptAndLogAllParsed()` (Log All) | ✅ Already correct (had de-normalization) | No change |
| `acceptParsedFood()` → `saveLog()` (Accept → Log) | ❌ Hardcoded `servingG=100` | ✅ Fixed — uses `state.servingG` + `deNorm` |

**Scope of impact:** Any catalog item where `base_serving_g ≠ 100`, including:
- All recipes created via the recipe builder (`base_serving_g = N × 100`)
- Any food edited in the Catalog screen to change its serving size

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 78 | Store `servingG` in `LogUiState` from catalog item | The `saveLog()` path needs the actual serving weight to de-normalize per-100g form macros to per-serving for `LogFoodUseCase`. Hardcoding 100 was only correct when all catalog items had `base_serving_g = 100`. The recipe builder creates items with `base_serving_g = N × 100`, breaking the assumption. |
| 79 | Reset `servingG` on food name edit | When the user edits the food name in the manual form, they're creating a custom item. The catalog link (`sourceCatalogFoodItemId`) is cleared, so `servingG` must also reset to 100 to prevent the old catalog serving weight from affecting the new item's normalization. |

---

## Phase 20: AI Migration — Direct Gemini → Shared Supabase Edge Functions

**Status:** ✅ Completed
**Date:** June 4, 2026

### Summary

Replaced all direct Gemini API calls in Android with shared Supabase Edge Functions (`parse-food` and `scan-label`) already used by the webapp. This ensures strict cross-platform parity: both platforms now use the same AI backend, same prompts, and same response schemas. The local `ResolveCatalogCacheUseCase` fallback is preserved for offline/unsynced data.

**Motivation:**
- Eliminate divergent AI prompts between Android (direct Gemini) and webapp (Edge Functions)
- Centralize prompt engineering — update once in Edge Functions, both platforms benefit
- Remove client-side Gemini API key exposure from the Android app
- Edge Functions return pre-resolved `catalogMatch` data, reducing client-side catalog resolution work

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `data/remote/dto/ParseFoodEdgeDto.kt` | **Created** | DTOs for the `parse-food` Edge Function: `ParseFoodRequest`, `ParseFoodEdgeResponse`, `ParsedFoodDto`, `CatalogMatchDto`, `FoodItemDto` |
| 2 | `data/remote/dto/ScanLabelEdgeDto.kt` | **Created** | DTOs for the `scan-label` Edge Function: `ScanLabelRequest`, `ScanLabelEdgeResponse` with raw + converted per-100g fields |
| 3 | `data/remote/api/SupabaseEdgeFunctionService.kt` | Updated | Added `parseFoodViaEdge()` and `scanLabelViaEdge()` endpoints |
| 4 | `domain/model/ParsedFood.kt` | Updated | Added `edgeCatalogMatch: EdgeCatalogMatch? = null` field and new `EdgeCatalogMatch` data class (`isFromCatalog: Boolean`, `foodItem: FoodItem?`) |
| 5 | `domain/model/ExtractedLabelData.kt` | Updated | Added pre-computed per-100g fields (`calories`, `protein`, `carbs`, `fat`) defaulting to raw values, plus `suggestedQuantity` and `suggestedUnit` |
| 6 | `data/repository/AiRepositoryImpl.kt` | **Rewritten** | Constructor takes only `SupabaseEdgeFunctionService`. `parseFood()` calls `parseFoodViaEdge()`, `extractLabelFromImage()` calls `scanLabelViaEdge()`. Includes DTO-to-domain mapping extension functions |
| 7 | `domain/usecase/ResolveCatalogCacheUseCase.kt` | Updated | Added short-circuit for `edgeCatalogMatch` pre-resolved items, extracted `resolveIngredientSingle()` helper, kept Room fallback for unsynced data |
| 8 | `presentation/screens/log/LabelScannerDelegate.kt` | Updated | Removed per-serving→per-100g conversion math, uses `data.calories`, `data.protein`, `data.carbs`, `data.fat`, `data.suggestedQuantity`, `data.suggestedUnit` directly from pre-computed fields |
| 9 | `di/AppModule.kt` | Updated | Removed generic `provideOkHttpClient()`, `@Named("gemini")` Retrofit, `provideGeminiApiService()` |
| 10 | `util/Constants.kt` | Updated | Removed `GEMINI_BASE_URL` |
| 11 | `app/build.gradle.kts` | Updated | Removed `GEMINI_API_KEY` BuildConfig field |
| 12 | `data/remote/api/GeminiApiService.kt` | **Deleted** | Direct Gemini API interface — replaced by Edge Functions |
| 13 | `data/remote/dto/GeminiRequest.kt` | **Deleted** | Gemini request DTOs — no longer needed |
| 14 | `data/remote/dto/GeminiResponse.kt` | **Deleted** | Gemini response DTOs — no longer needed |
| 15 | `util/GeminiPrompts.kt` | **Deleted** | Client-side prompts for food parsing — now in Edge Function |
| 16 | `util/GeminiLabelPrompts.kt` | **Deleted** | Client-side prompts for label scanning — now in Edge Function |

### Key Implementation Details

**AiRepositoryImpl rewrite:**
```kotlin
class AiRepositoryImpl @Inject constructor(
    private val edgeFunctionService: SupabaseEdgeFunctionService
) : AiRepository {
    override suspend fun parseFood(description: String): List<ParsedFood> {
        val response = edgeFunctionService.parseFoodViaEdge(
            ParseFoodRequest(foodDescription = description)
        )
        return response.foods.map { it.toDomain() }
    }
}
```

**Edge catalog match short-circuit in ResolveCatalogCacheUseCase:**
```kotlin
// If the Edge Function already resolved a catalog match, use it directly
if (food.edgeCatalogMatch?.isFromCatalog == true) {
    food.edgeCatalogMatch.foodItem?.let { return it }
}
// Otherwise fall back to local Room cache lookup
```

**Data flow (before vs after):**

| Step | Before (Direct Gemini) | After (Edge Functions) |
|------|----------------------|----------------------|
| 1. AI Parse | Android → Gemini API (client-side prompt) | Android → Supabase EF → Gemini API (server-side prompt) |
| 2. Catalog Match | Client-side `ResolveCatalogCacheUseCase` only | Edge Function pre-resolves + client fallback for unsynced |
| 3. Label Scan | Android → Gemini API (client-side prompt + manual per-100g conversion) | Android → Supabase EF → Gemini API (server returns pre-computed per-100g) |

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 80 | Keep `AiRepository` interface unchanged | `ParseFoodWithAiUseCase` and `ExtractLabelUseCase` still call the same `aiRepository.parseFood()` / `aiRepository.extractLabelFromImage()`. Only the implementation swaps from direct Gemini to Edge Functions. Zero changes to use cases or ViewModels. |
| 81 | Keep `ResolveCatalogCacheUseCase` as fallback | Edge Functions pre-resolve catalog matches using cloud data, but the user's local Room DB may have items not yet synced. The local fallback ensures catalog hits for offline-created items. |
| 82 | Pre-compute per-100g values in `scan-label` Edge Function | Eliminates duplicated per-serving→per-100g conversion logic on each client. The Edge Function returns both raw and normalized values; Android uses the pre-computed ones directly. |
| 83 | Remove `GEMINI_API_KEY` from Android build | The API key is no longer needed client-side — all AI calls go through Supabase Edge Functions which hold the key server-side. Eliminates client-side key exposure risk. |

---

## Phase 21: Ingredient Inline Edit — AI Parsed Mode

**Status:** ✅ Completed
**Date:** June 6, 2026

### Summary

Users can now edit an ingredient's quantity and unit directly inside a recipe card in AI parsed mode via a pencil icon button in each `IngredientRow`. Tapping the pencil opens an `AlertDialog` pre-filled with the ingredient's current quantity and unit. Saving patches the ingredient in `parsedFoods` in-place without breaking it out of the recipe. When "Log All" fires, the corrected values flow through the existing `acceptAndLogAllParsed()` paths automatically.

Cross-platform parity with webapp Phase W17.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/log/IngredientListDelegate.kt` | Updated | Added `updateParsedIngredient(foodIndex, ingredientIndex, quantity, unit)` — patches a single ingredient's `quantity` and `unit` in-place. Does **not** touch `ingredientCatalogMatches` or `ingredientNutritionLookups` (keyed by name, still valid). |
| 2 | `presentation/screens/log/LogViewModel.kt` | Updated | Added `EditingIngredientState` data class inside `LogUiState` companion. Added `editingIngredient: EditingIngredientState? = null` to `LogUiState`. Added `updateParsedIngredient()` delegate method. Added `beginEditIngredient()`, `dismissEditIngredient()`, `confirmEditIngredient()` in `LogViewModel`. |
| 3 | `presentation/screens/log/LogScreen.kt` | Updated | Added `AlertDialog` and `TextButton` imports. Added `onEditIngredient`, `onDismissEditIngredient`, `onConfirmEditIngredient` params to `AiInputSection`. Added edit `IconButton` (pencil) to `IngredientRow`. Added `onEdit` param to `IngredientRow`. Added `AlertDialog` at end of `AiInputSection` driven by `uiState.editingIngredient` with qty `OutlinedTextField` + unit `DropdownMenu`. Wired all three callbacks from the `LogScreen` call site via `viewModel::beginEditIngredient`, `viewModel::dismissEditIngredient`, `viewModel::confirmEditIngredient`. |

### Key Implementation Details

**`IngredientListDelegate.updateParsedIngredient()`:**
```kotlin
fun updateParsedIngredient(foodIndex: Int, ingredientIndex: Int, quantity: Double, unit: String) {
    uiState.update { state ->
        val foods = state.parsedFoods.toMutableList()
        val food = foods.getOrNull(foodIndex) ?: return@update state
        val ingredients = food.ingredients.toMutableList()
        if (ingredientIndex !in ingredients.indices) return@update state
        ingredients[ingredientIndex] = ingredients[ingredientIndex].copy(quantity = quantity, unit = unit)
        foods[foodIndex] = food.copy(ingredients = ingredients)
        state.copy(parsedFoods = foods)
    }
}
```

**`LogUiState.EditingIngredientState`:**
```kotlin
data class EditingIngredientState(
    val foodIndex: Int,
    val ingredientIndex: Int,
    val name: String,
    val quantity: String,   // bound to OutlinedTextField
    val unit: String        // bound to DropdownMenu
)
```

**AlertDialog in `AiInputSection` (driven by ViewModel state):**
```kotlin
val editing = uiState.editingIngredient
if (editing != null) {
    var localQty by remember(editing.foodIndex, editing.ingredientIndex) { mutableStateOf(editing.quantity) }
    var localUnit by remember(editing.foodIndex, editing.ingredientIndex) { mutableStateOf(editing.unit) }
    AlertDialog(
        onDismissRequest = onDismissEditIngredient,
        title = { Text("Edit: ${editing.name}") },
        text = {
            // OutlinedTextField for qty + Box/DropdownMenu for unit
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmEditIngredient(localQty, localUnit) },
                enabled = localQty.toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismissEditIngredient) { Text("Cancel") } }
    )
}
```

**`confirmEditIngredient()` validation:**
```kotlin
fun confirmEditIngredient(quantity: String, unit: String) {
    val editing = _uiState.value.editingIngredient ?: return
    val qty = quantity.toDoubleOrNull()?.takeIf { it > 0 } ?: return  // silent no-op on bad input
    updateParsedIngredient(editing.foodIndex, editing.ingredientIndex, qty, unit)
    dismissEditIngredient()
}
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 84 | `EditingIngredientState` in `LogUiState`, not in local Compose `remember` | Android dialogs must survive configuration changes (rotation). Storing dialog state in the ViewModel `StateFlow` ensures the `AlertDialog` re-opens correctly after a configuration change, unlike local `remember { mutableStateOf }` which is destroyed and recreated. |
| 85 | Dialog text field state uses `remember(editing.foodIndex, editing.ingredientIndex)` | Using the ingredient indices as `remember` keys resets the local qty/unit text state when a different ingredient is opened for editing. Without keys, the stale values from the previous edit would persist. |
| 86 | `updateParsedIngredient` does NOT clear catalog matches or nutrition lookups | Both `ingredientCatalogMatches` and `ingredientNutritionLookups` are keyed by ingredient name, not by quantity/unit. Changing the quantity doesn't invalidate the nutrition data — `acceptAndLogAllParsed()` applies `computeServingMultiplier(newQty, newUnit, ...)` at log time to compute the correct macros. |
| 87 | Pencil button is separate from `onClick` (ingredient selection) | Tapping an ingredient row highlights it for "Edit Selected" (opens the full manual form). The pencil is a distinct action — inline qty/unit edit without breaking out of the recipe. Separate `onEdit` callback keeps the two paths independent. |

---

## Phase 22: Recipe Card Nutrition Display + Edit Selected Recipe Fix

**Status:** ✅ Completed
**Date:** June 6, 2026

### Summary

Fixed two bugs affecting recipe cards in AI parsed mode:

1. **Recipe card showed no aggregated nutrition total** — `ParsedFoodCard` displayed ingredient rows but had no recipe-level macro summary. Users had no way to see the expected caloric content of the full dish. Fix: added a recipe total row below the ingredient list, computed from `ingredientNutritionStates` (a `Map<Int, NutritionLookupState>` keyed by ingredient index). Shows "Looking up ingredients…" while any ingredient lookup is `Loading`, then `~N kcal · Xg P · Yg C · Zg F` once resolved.

2. **"Accept" on a recipe card opened flat Manual form** — `acceptParsedFood()` had no recipe branch. Accepting a recipe card set `foodName = topLevelFood.name` and opened flat Ingredient mode, discarding the AI-detected ingredient structure. Fix: when `!isEditingIngredient && topLevelFood.isRecipe`, builds `updatedRecipeIngredients` from each ingredient's `ingredientNutritionLookups` / `ingredientCatalogMatches`, sets `isLoggingRecipe = true`, and pre-populates `manualRecipeIngredients`.

Cross-platform parity with webapp Phase W18.

### Bug Reproduction

**Bug 1:**
1. Type "banana smoothie (50g banana, 100g milk, 50g yogurt)" → Parse with AI
2. Ingredient nutrition lookups complete
3. **Before fix:** Recipe card shows ingredient rows only — no total kcal displayed
4. **After fix:** Shows "~245 kcal · 8.2g P · 42.1g C · 4.3g F" below ingredients

**Bug 2:**
1. Parse "banana smoothie" → recipe card appears → select card → tap "Accept"
2. **Before fix:** Manual form opens flat Ingredient mode with "banana smoothie" as food name
3. **After fix:** Manual form opens Recipe builder mode with ingredients pre-filled

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `presentation/screens/log/LogViewModel.kt` | Updated | `acceptParsedFood()`: Added recipe branch — when `!isEditingIngredient && topLevelFood.isRecipe`, maps each ingredient to `ManualRecipeIngredient` using `IngredientKey(selectedParsedFoodIndex, idx)` to look up `ingredientNutritionLookups` (priority 2) and `ingredientCatalogMatches` (priority 1). Appends a trailing empty `ManualRecipeIngredient()`. State update includes `isLoggingRecipe = true` and `manualRecipeIngredients = updatedRecipeIngredients`. |
| 2 | `presentation/screens/log/LogScreen.kt` | Updated | `ParsedFoodCard` composable: Added `ingredientNutritionStates: Map<Int, NutritionLookupState> = emptyMap()` parameter. Added recipe total section below ingredient list — if any index is `NutritionLookupState.Loading`, shows "Looking up ingredients…"; otherwise iterates ingredients, reads catalog items (priority 1) or `NutritionLookupState.Found` data (priority 2), applies `UnitConverter.computeServingMultiplier`, and displays aggregated `~N kcal · Xg P · Yg C · Zg F`. At the call site in `AiInputSection`, `ingredientNutritionStates` is built by associating `food.ingredients.indices` to `ingredientNutritionLookups[IngredientKey(index, ingIdx)]` and filtering nulls. |

### Key Implementation Details

**`updatedRecipeIngredients` in `acceptParsedFood()` (`LogViewModel.kt`):**
```kotlin
val updatedRecipeIngredients = if (!isEditingIngredient && topLevelFood.isRecipe) {
    val ingList = topLevelFood.ingredients.mapIndexed { idx, ing ->
        val key = IngredientKey(state.selectedParsedFoodIndex, idx)
        val ingNutrition = (state.ingredientNutritionLookups[key] as? NutritionLookupState.Found)?.info
        val ingCatalogMatches = state.ingredientCatalogMatches[state.selectedParsedFoodIndex]
        val ingCatalogItem = if (ingCatalogMatches?.getOrNull(idx)?.isFromCatalog == true)
            ingCatalogMatches[idx].matchedFoodItem else null
        ManualRecipeIngredient(
            catalogItem = ingCatalogItem,
            customName = ingCatalogItem?.name ?: ing.name,
            quantity = ing.quantity.formatMacro(),
            unit = ing.unit,
            calories = ingCatalogItem?.baseCalories?.formatMacro()
                ?: ingNutrition?.caloriesPer100g?.formatMacro() ?: "",
            protein  = ingCatalogItem?.baseProtein?.formatMacro()
                ?: ingNutrition?.proteinPer100g?.formatMacro() ?: "",
            carbs    = ingCatalogItem?.baseCarbs?.formatMacro()
                ?: ingNutrition?.carbsPer100g?.formatMacro() ?: "",
            fat      = ingCatalogItem?.baseFat?.formatMacro()
                ?: ingNutrition?.fatPer100g?.formatMacro() ?: "",
        )
    }
    ingList + listOf(ManualRecipeIngredient()) // trailing empty row
} else null
```

**Recipe total in `ParsedFoodCard` composable (`LogScreen.kt`):**
```kotlin
val anyIngLoading = food.ingredients.indices.any { idx ->
    ingredientNutritionStates[idx] is NutritionLookupState.Loading
}
// When not loading — compute totals
var totalCal = 0.0; var totalProt = 0.0
var totalCarb = 0.0; var totalFat = 0.0; var hasData = false
food.ingredients.forEachIndexed { idx, ing ->
    val catItem = ingredientMatches?.getOrNull(idx)
        ?.takeIf { it.isFromCatalog }?.matchedFoodItem
    val nut = (ingredientNutritionStates[idx] as? NutritionLookupState.Found)?.info
    if (catItem != null) {
        val m = UnitConverter.computeServingMultiplier(ing.quantity, ing.unit, catItem.baseServingG)
        totalCal += catItem.baseCalories * m; hasData = true
    } else if (nut != null) {
        val m = UnitConverter.computeServingMultiplier(ing.quantity, ing.unit, nut.servingWeightG?.toDouble())
        totalCal += nut.caloriesPer100g * m; hasData = true
    }
}
// Display: "~N kcal · Xg P · Yg C · Zg F"
```

**Call-site `ingredientNutritionStates` construction (in `AiInputSection`):**
```kotlin
val ingredientNutritionStates = if (food.isRecipe) {
    food.ingredients.indices.associate { ingIdx ->
        ingIdx to uiState.ingredientNutritionLookups[IngredientKey(index, ingIdx)]
    }.filterValues { it != null }
        .mapValues { it.value!! }
} else emptyMap()
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 88 | Build `ingredientNutritionStates` at the `AiInputSection` call site, not inside `ParsedFoodCard` | `ParsedFoodCard` is a stateless composable that receives data as parameters. Reading `LogViewModel` state directly inside would violate the composable's separation of concerns and break `@Preview` support. Building the map at the call site keeps the composable testable and consistent with the existing `catalogMatch` / `ingredientMatches` pattern. |
| 89 | Recipe total uses the same `UnitConverter.computeServingMultiplier` as the save path | Consistency between the preview total and the saved log total is critical. Using the same conversion function ensures that what the user sees in the card (~N kcal) matches what `LogFoodUseCase.logRecipe()` will compute when they tap "Log All". |
| 90 | Pre-fill `manualRecipeIngredients` from `acceptParsedFood()` recipe branch | Opening the flat Manual form for a recipe discards the AI-detected ingredient structure and routes the save through the single-food path instead of `logRecipe()`. Pre-filling the Recipe builder preserves the breakdown, enables the user to adjust per-ingredient quantities, and ensures correct macro aggregation at save time. |

---

## Phase 23: Multi-Turn Recipe-First AI Parsing & Selection Flow Cleanup

**Status:** ✅ Completed
**Date:** June 8, 2026

### Summary

Upgraded the AI food parsing and lookup pipeline to support sequential multi-turn clarifications generated by Gemini, and corrected critical UI issues in the recipe selection/editing flow. Fixed a multi-turn infinite clarification loop by dynamically appending clarification answers to the prompt food entry and introducing high-priority LLM override rules. Also added the `MealTypeSelector` back to the AI parsed mode UI on Android, and fixed configuration settings that caused Supabase connection failures in local debug environments.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `local.properties` | Updated | Replaced placeholder Supabase configuration URLs and anon keys with actual credentials to resolve `UnknownHostException` connection errors on debug APK builds. |
| 2 | `app/src/main/java/com/app/nutriai/data/repository/NutritionRepositoryImpl.kt` | Updated | Modified the FDC online search sorting comparator to prioritize exact name matches and starts-with matches before looking at macro completeness scores, preventing keywords like "butter" from matching "butter lettuce" as the top generic result. |
| 3 | `app/src/main/java/com/app/nutriai/presentation/screens/log/LogScreen.kt` | Updated | Cleaned up the recipe card UI (removed redundant ingredient selection flow), restored `MealTypeSelector` inside the AI parsed results section, and passed selected type changes to `LogViewModel`. |
| 4 | `supabase/functions/_shared/prompts.ts` | Updated | Expanded instructions to handle custom entry redirections, added high-priority override rules, and appended previous clarification answers to the `Food entry` prompt string to resolve loops. |
| 5 | `supabase/functions/lookup-nutrition/index.ts` | Updated | Ported the exact name and prefix match sorting logic to the FDC lookup within the lookup-nutrition Edge Function to achieve full parity for the Web companion. |


### Key Implementation Details

**Prioritizing Exact/Prefix Matches in Nutrition Repository:**
```kotlin
val results = response.foods
    .filter { it.hasUsableData }
    .mapNotNull { it.toNutritionInfo() }
    .sortedWith(compareBy(
        { it.productName.lowercase().trim() != foodName.lowercase().trim() },
        { !it.productName.lowercase().trim().startsWith(foodName.lowercase().trim()) },
        {
            var score = 0
            if (it.caloriesPer100g > 0) score += 4
            if (it.proteinPer100g > 0) score += 1
            if (it.carbsPer100g > 0) score += 1
            if (it.fatPer100g > 0) score += 1
            -score
        }
    ))
```

### Architecture Decisions Added

| # | Decision | Rationale |
|---|----------|-----------|
| 91 | Remove individual ingredient selection from parsed cards | Selecting a single ingredient inside a parsed recipe card and opening it in the flat manual form stripped the recipe context, creating inconsistencies with the Manual Recipe Builder. The inline ✏️ icon dialog and the manual recipe builder render the selection highlight redundant. |
| 92 | Centralize custom redirection rules inside the LLM prompt | Guiding the AI to rename the parsed entity during multi-turn parsing resolves identity-shifting custom entries (like "butter" -> "butter noodles") before the name reaches the client-side database lookup. |

---

## Phase 24: Google Sign-In Integration (Android Client & Build Config)

**Status:** ✅ Completed
**Date:** July 2, 2026

### Summary

Integrated Google Sign-In into the Android application via the Google Credential Manager library, providing a unified authentication flow that exchanges Google ID tokens with Supabase GoTrue REST endpoints. Exposed build variables from `local.properties` to allow secure injection of client IDs.

### Changes Made

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `gradle/libs.versions.toml` | Updated | Added Android Credential Manager libraries and Google Identity credentials options definition to version catalog. |
| 2 | `app/build.gradle.kts` | Updated | Added dependencies for `androidx.credentials`, `credentials-play-services-auth`, and `googleid`. Exposed `GOOGLE_WEB_CLIENT_ID` to BuildConfig. |
| 3 | `local.properties.example` | Updated | Added a configuration placeholder for `GOOGLE_WEB_CLIENT_ID`. |
| 4 | `local.properties` | Updated | Appended local development placeholder variable `GOOGLE_WEB_CLIENT_ID`. |
| 5 | `app/src/main/res/drawable/ic_google_logo.xml` | Created | Added custom Google G logo vector resource drawable. |
| 6 | `app/src/main/java/com/app/nutriai/data/remote/dto/SupabaseAuthDto.kt` | Updated | Defined `IdTokenSignInRequest` data transfer object for authentication requests. |
| 7 | `app/src/main/java/com/app/nutriai/data/remote/api/SupabaseAuthApiService.kt` | Updated | Added `signInWithIdToken` function declaration targeting `/auth/v1/token` endpoint. |
| 8 | `app/src/main/java/com/app/nutriai/domain/repository/AuthRepository.kt` | Updated | Added `signInWithGoogle(idToken)` abstract method to AuthRepository domain. |
| 9 | `app/src/main/java/com/app/nutriai/data/repository/AuthRepositoryImpl.kt` | Updated | Implemented `signInWithGoogle(idToken)` to dispatch token verification calls to Supabase API. |
| 10 | `app/src/main/java/com/app/nutriai/domain/usecase/SignInWithGoogleUseCase.kt` | Created | Added single-responsibility UseCase class to handle Google authentication delegation. |
| 11 | `app/src/main/java/com/app/nutriai/presentation/screens/auth/AuthViewModel.kt` | Updated | Injected `SignInWithGoogleUseCase`, implemented `signInWithGoogle(idToken)` action, and added `getLinkGoogleUrl()` helper for OAuth account linking. |
| 12 | `app/src/main/java/com/app/nutriai/presentation/screens/auth/AuthScreen.kt` | Updated | Wired client-side Google Credential Manager handlers, added Google OutlinedButton, added Google Account Linking card inside ProfilePanel, and fixed preview layouts. |
| 13 | `app/src/main/java/com/app/nutriai/domain/usecase/README.md` | Updated | Manifest listing update for the added `SignInWithGoogleUseCase` class. |
| 14 | `app/src/main/java/com/app/nutriai/presentation/components/RecommendationCard.kt` | Updated | Added `FormattedRecipeText` parser composable to render AI recipes as structured, bulleted lists matching Webapp layout formatting. |
| 15 | `app/src/main/java/com/app/nutriai/presentation/navigation/NutriAiNavHost.kt` | Updated | Renamed the bottom navigation tab from 'Profile' to 'Settings' and changed its icon to `Icons.Default.Settings` to achieve layout parity with the Web Companion. |

### Key Implementation Details

**Google Credential Manager Token Request & Verification Dispatch:**
```kotlin
val credentialManager = CredentialManager.create(context)
val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
val googleIdOption = GetGoogleIdOption.Builder()
    .setServerClientId(webClientId)
    .setFilterByAuthorizedAccounts(false)
    .setAutoSelectEnabled(false)
    .build()

val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

val result = credentialManager.getCredential(context = context, request = request)
val credential = result.credential
if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
    viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
}
```

---

## Appendix: API Keys & Secrets

> ⚠️ **Never commit API keys to version control.**
> Copy `local.properties.example` → `local.properties` and fill in your keys.

| Service | Key Name | Where to Get |
|---------|----------|--------------|
| ~~Google Gemini~~ | ~~`GEMINI_API_KEY`~~ | ~~Removed in Phase 20 — AI calls go through Edge Functions~~ |
| Supabase | `SUPABASE_URL` | [Supabase Dashboard](https://supabase.com/dashboard) |
| Supabase | `SUPABASE_ANON_KEY` | [Supabase Dashboard](https://supabase.com/dashboard) |
| Google Sign-In | `GOOGLE_WEB_CLIENT_ID` | [Google Cloud Console Credentials](https://console.cloud.google.com/) |
