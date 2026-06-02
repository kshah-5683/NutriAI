# Feature 1: Internet Recommendations Infrastructure — Implementation Plan

**Parent plan:** `ai_recommendations.plan.md` (v4) — Phases R1 + R4
**Prerequisite:** None — this is the foundation for all recommendation features
**Scope:** Edge Function + prompts + Supabase migration + user profile setup + sync (both platforms)
**User-facing:** Profile settings UI only. No recommendation cards — those come in Feature 2.

---

## Why Infrastructure First

Internet recommendations are not a standalone user feature — they are **plumbing** that the visible features (Home recs, Dedicated screen) depend on. Building them first means:

1. The Edge Function (`recommend-meals`) is deployed and testable via curl before any client UI exists
2. Profile columns are in Supabase and synced cross-platform before any recommendation card tries to read `includeInternet`
3. Feature 2 (Home screen recs) can ship with full catalog + internet support from day one, instead of catalog-only → internet-upgrade two-phase rollout
4. The "no food logged at dinner" edge case is solved from the start — internet recs fill the gap when the catalog is empty/sparse

---

## Phase R1: Edge Function + Prompt + Migration

### Step 1.1 — Supabase Migration: `009_user_profile_columns.sql`

**File:** `supabase/migrations/009_user_profile_columns.sql`

Add profile columns to the existing `user_preferences` table. All profile columns are nullable so existing rows are unaffected.

```sql
-- Add profile columns to existing user_preferences table
-- Phase R1: AI Recommendations prerequisite
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS age INTEGER;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS gender TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS weight_kg DOUBLE PRECISION;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS weight_goal TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS diet_type TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS cuisine_preferences TEXT[] DEFAULT '{}';
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS allergies TEXT[] DEFAULT '{}';
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS recommendations_enabled BOOLEAN NOT NULL DEFAULT false;
```

**Run in:** Supabase Dashboard → SQL Editor

### Step 1.2 — Shared Prompt: Add to `prompts.ts`

**File:** `supabase/functions/_shared/prompts.ts`

Append the recommendation system instruction and prompt builder function after the existing `LABEL_USER_PROMPT` export. Do NOT modify any existing exports.

**New exports to add:**

1. `RECOMMENDATION_SYSTEM_INSTRUCTION` — The full system prompt from the v4 plan (scope restriction, rules 1-12, profile null handling, exceeded budget handling, JSON schema with `suggested_quantity` and `reason` fields)

2. `buildRecommendationPrompt(params)` — Function that assembles the user prompt:
   ```ts
   export function buildRecommendationPrompt(params: {
     mode: "time_based" | "query";
     timeOfDay: string;
     remainingMacros: { calories: number; protein: number; carbs: number; fat: number };
     catalogItems: Array<{ id: string; name: string; kcal: number; p: number; c: number; f: number }>;
     query?: string;
     profile?: {
       dietType?: string | null;
       cuisines?: string[] | null;
       allergies?: string[] | null;
       weightGoal?: string | null;
     } | null;
   }): string
   ```

   **Key behaviors:**
   - If `remainingMacros.calories <= 0`: Replace the macros line with `"User has exceeded their daily calorie goal. Suggest only zero-calorie beverages (water, black coffee, herbal tea) or offer a supportive 'You've hit your daily targets!' message."` — do NOT pass negative numbers to the model
   - If `profile` is null or all fields are null: Omit the "User preferences" section entirely from the prompt
   - If `profile` has some non-null fields: Only include those that are non-null (e.g., if `dietType` is set but `cuisines` is empty, only mention the diet type)
   - `catalogItems` array is injected as compact JSON: `[{id:"x",name:"Y",kcal:N,p:N,c:N,f:N}, ...]`
   - For `mode=query`: Include the query string in the prompt as `User query: "{query}"`
   - For `mode=time_based`: Omit the user query section

### Step 1.3 — Edge Function: `recommend-meals/index.ts`

**File:** `supabase/functions/recommend-meals/index.ts`

Follow the exact pattern of `parse-food/index.ts`:

```
Import order:
  serve, createClient, prompts, extractJson, corsHeaders/handleCors

serve(async (req) => {
  if OPTIONS → handleCors()

  try {
    // 1. Parse + validate request body
    const { mode, query, timeOfDay, remainingMacros, includeInternet } = await req.json();

    // Validate mode
    if (!["time_based", "query"].includes(mode)) → 400

    // Validate query for mode=query
    if (mode === "query") {
      if (!query?.trim() || query.length > 200) → 400
      // Strip HTML/markdown tags
      sanitizedQuery = query.replace(/<[^>]*>/g, "").replace(/[#*_~`]/g, "").trim()
    }

    // Validate remainingMacros shape
    if remainingMacros missing or not object with calories/protein/carbs/fat → 400

    // 2. Auth — get user from JWT
    const supabase = createClient(...)
    const user = (await supabase.auth.getUser()).data.user!;
    const userId = user.id;

    // 3. Catalog pre-filtering query
    // Run the aggregation query to get top 20 food items ranked by usage
    const { data: logFrequencies } = await supabase.rpc('get_catalog_frequencies', {
      p_user_id: userId,
      p_time_of_day: timeOfDay ?? 'morning'
    });
    // OR: Direct SQL query via supabase.from('daily_logs')...
    // (see "Pre-filtering Query" section below for both options)

    // 4. Fetch food item details for the top 20 IDs
    const ingredientCatalogId = `${userId}_local_user_ingredients`;
    const recipeCatalogId = `${userId}_local_user_recipes`;

    const { data: foodItems } = await supabase
      .from("food_items")
      .select("id, name, base_calories, base_protein, base_carbs, base_fat")
      .in("catalog_id", [ingredientCatalogId, recipeCatalogId])
      .is("deleted_at", null);

    // 5. Rank + filter + shuffle
    // Join foodItems with logFrequencies, sort by frequency, take top 20
    // Shuffle the top 20 using Fisher-Yates, slice first 15
    const catalogForPrompt = shuffle(ranked).slice(0, 15).map(item => ({
      id: item.id,
      name: item.name,
      kcal: item.base_calories,
      p: item.base_protein,
      c: item.base_carbs,
      f: item.base_fat,
    }));

    // 6. Optionally fetch profile (only if includeInternet)
    let profile = null;
    if (includeInternet) {
      const { data: prefs } = await supabase
        .from("user_preferences")
        .select("diet_type, cuisine_preferences, allergies, weight_goal, recommendations_enabled")
        .eq("user_id", userId)
        .maybeSingle();
      if (prefs?.recommendations_enabled) {
        profile = {
          dietType: prefs.diet_type,
          cuisines: prefs.cuisine_preferences,
          allergies: prefs.allergies,
          weightGoal: prefs.weight_goal,
        };
      }
    }

    // 7. Build prompt
    const userPrompt = buildRecommendationPrompt({
      mode,
      timeOfDay: timeOfDay ?? inferTimeOfDay(),
      remainingMacros,
      catalogItems: catalogForPrompt,
      query: mode === "query" ? sanitizedQuery : undefined,
      profile,
    });

    // 8. Call Gemma 4
    const geminiKey = Deno.env.get("GEMINI_API_KEY")!;
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemma-4-26b-a4b-it:generateContent?key=${geminiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: userPrompt }] }],
          systemInstruction: {
            role: "user",
            parts: [{ text: RECOMMENDATION_SYSTEM_INSTRUCTION }],
          },
          generationConfig: {
            temperature: 0.7,      // Higher than parse-food (0.1) for variety
            topP: 0.9,
            topK: 40,
            maxOutputTokens: 1024, // Capped for timeout mitigation
            responseMimeType: "application/json",
            thinkingConfig: { thinkingLevel: "MINIMAL" },
          },
        }),
      }
    );

    if (!response.ok) → 502 with "AI service error"

    // 9. Extract JSON (same pattern as parse-food)
    const data = await response.json();
    const parts = data.candidates?.[0]?.content?.parts ?? [];
    const contentPart = parts.findLast((p) => p.thought !== true && p.text?.trim());
    const parsed = extractJson(contentPart?.text ?? '{"recommendations": []}');

    // 10. Return response
    return new Response(JSON.stringify(parsed), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });

  } catch (err) { → 500 }
});

// Helper: Fisher-Yates shuffle
function shuffle<T>(arr: T[]): T[] { ... }

// Helper: infer time of day from server clock (fallback if client didn't send it)
function inferTimeOfDay(): string { ... }
```

### Pre-filtering Query — Two Options

**Option A — Direct query in the Edge Function (simpler, no RPC needed):**

The Edge Function runs two parallel queries and joins in memory:

```ts
// Query 1: Get log frequencies per food_item_id
const { data: freqs } = await supabase
  .from("daily_logs")
  .select("food_item_id")
  .eq("user_id", userId)
  .is("deleted_at", null)
  .not("food_item_id", "is", null);

// Count frequencies in JS
const freqMap = new Map<string, number>();
for (const row of freqs ?? []) {
  freqMap.set(row.food_item_id, (freqMap.get(row.food_item_id) ?? 0) + 1);
}

// Query 2: Get all catalog food items (already fetched above)
// Sort by frequency, take top 20
const ranked = (foodItems ?? [])
  .map(item => ({ ...item, freq: freqMap.get(item.id) ?? 0 }))
  .sort((a, b) => b.freq - a.freq)
  .slice(0, 20);
```

**Option B — PostgreSQL RPC function (better for large catalogs, uses DB indexes):**

Create an RPC function in the migration:

```sql
CREATE OR REPLACE FUNCTION get_catalog_frequencies(
  p_user_id UUID,
  p_time_of_day TEXT DEFAULT 'morning'
)
RETURNS TABLE (food_item_id UUID, log_count BIGINT, morning_count BIGINT) AS $$
BEGIN
  RETURN QUERY
  SELECT
    dl.food_item_id,
    COUNT(*)::BIGINT as log_count,
    COUNT(*) FILTER (
      WHERE EXTRACT(HOUR FROM TO_TIMESTAMP(dl.date_timestamp / 1000)) < 12
    )::BIGINT as morning_count
  FROM daily_logs dl
  WHERE dl.user_id = p_user_id AND dl.deleted_at IS NULL AND dl.food_item_id IS NOT NULL
  GROUP BY dl.food_item_id
  ORDER BY
    CASE WHEN p_time_of_day = 'morning' THEN
      COUNT(*) FILTER (WHERE EXTRACT(HOUR FROM TO_TIMESTAMP(dl.date_timestamp / 1000)) < 12)
    END DESC NULLS LAST,
    COUNT(*) DESC
  LIMIT 20;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Recommendation: Start with Option A** (simpler). Move to Option B if catalog sizes exceed ~200 items and the in-memory join becomes slow. Option A avoids creating an RPC function and is easier to debug.

---

## Phase R4: User Profile Setup (Both Platforms)

### Step 4.1 — Android: Room Entity Extension — `UserPreferencesEntity.kt`

**File:** `app/src/main/java/com/app/nutriai/data/local/entity/UserPreferencesEntity.kt`

Add 8 new columns after `lastModifiedAt`. All are nullable except `recommendationsEnabled` (defaults to `false`).

```kotlin
// ADD after lastModifiedAt:

@ColumnInfo(name = "age")
val age: Int? = null,

@ColumnInfo(name = "gender")
val gender: String? = null,

@ColumnInfo(name = "weight_kg")
val weightKg: Double? = null,

@ColumnInfo(name = "weight_goal")
val weightGoal: String? = null,

@ColumnInfo(name = "diet_type")
val dietType: String? = null,

/** Stored as comma-separated string in Room (e.g., "Indian,Italian,Japanese").
 *  Room has no native array type — CSV string with split/join in mappers.
 *  Supabase uses TEXT[] (Postgres array). */
@ColumnInfo(name = "cuisine_preferences")
val cuisinePreferences: String? = null,

/** Stored as comma-separated string in Room (e.g., "Gluten,Dairy,Nuts").
 *  Same CSV approach as cuisinePreferences. */
@ColumnInfo(name = "allergies")
val allergies: String? = null,

@ColumnInfo(name = "recommendations_enabled")
val recommendationsEnabled: Boolean = false
```

**IMPORTANT:** The existing `toMacroGoals()` method is unchanged — it only reads the 4 macro fields. Add a new companion conversion:

```kotlin
/** Converts profile columns to the domain model consumed by ViewModels. */
fun toUserProfile(): UserProfile = UserProfile(
    age = age,
    gender = gender,
    weightKg = weightKg,
    weightGoal = weightGoal,
    dietType = dietType,
    cuisinePreferences = cuisinePreferences
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList(),
    allergies = allergies
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList(),
    recommendationsEnabled = recommendationsEnabled
)
```

### Step 4.2 — Android: Room Migration v7 → v8 — `Migrations.kt`

**File:** `app/src/main/java/com/app/nutriai/data/local/migrations/Migrations.kt`

Add `MIGRATION_7_8` after `MIGRATION_6_7`:

```kotlin
/**
 * v7 → v8: Add user profile columns to user_preferences table (AI Recommendations Phase R4).
 *
 * Extends the existing macro goals table with dietary profile fields:
 * age, gender, weight, weight goal, diet type, cuisine preferences,
 * allergies, and a recommendations toggle.
 * All columns are nullable except recommendations_enabled (NOT NULL DEFAULT 0).
 * Existing rows are unaffected — all new columns use DEFAULT values.
 */
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

**Update `ALL` array:**
```kotlin
val ALL: Array<Migration> = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8  // ← NEW
)
```

### Step 4.3 — Android: Database Version Bump — `NutriAiDatabase.kt` + `Constants.kt`

**File:** `app/src/main/java/com/app/nutriai/data/local/NutriAiDatabase.kt`
- Change `version = 7` → `version = 8`
- Add version history comment: `v8 — AI Recommendations: Added profile columns to UserPreferencesEntity`

**File:** `app/src/main/java/com/app/nutriai/util/Constants.kt`
- Change `DATABASE_VERSION = 7` → `DATABASE_VERSION = 8`

### Step 4.4 — Android: Domain Model — `UserProfile.kt`

**File:** `app/src/main/java/com/app/nutriai/domain/model/UserProfile.kt` (NEW)

```kotlin
package com.app.nutriai.domain.model

/**
 * User's dietary profile — drives internet recommendation personalization.
 *
 * All fields are optional. When null/empty, the recommendation prompt omits
 * that constraint entirely (no defaults assumed).
 */
data class UserProfile(
    val age: Int? = null,
    val gender: String? = null,
    val weightKg: Double? = null,
    val weightGoal: String? = null,        // "lose" | "maintain" | "gain"
    val dietType: String? = null,          // "vegetarian" | "veg_eggs" | "non_veg" | "pescatarian" | "vegan"
    val cuisinePreferences: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val recommendationsEnabled: Boolean = false
) {
    /** True if the user has filled out at least the required fields for internet recs. */
    val isComplete: Boolean
        get() = dietType != null && recommendationsEnabled
}
```

### Step 4.5 — Android: UserPreferences Wrapper Extension — `UserPreferences.kt`

**File:** `app/src/main/java/com/app/nutriai/data/local/preferences/UserPreferences.kt`

Add alongside existing `macroGoalsFlow` and `saveMacroGoals()`:

```kotlin
/**
 * Emits the user's dietary profile from Room.
 * Returns [UserProfile] defaults when no row exists.
 */
val profileFlow: Flow<UserProfile> =
    userPreferencesDao.getPreferencesFlow(Constants.LOCAL_USER_ID).map { entity ->
        entity?.toUserProfile() ?: UserProfile()
    }

/**
 * Persist updated profile to Room and trigger a debounced push to Supabase.
 *
 * IMPORTANT: This must preserve existing macro goal values.
 * We read the current entity, overlay profile fields, then upsert.
 */
suspend fun saveProfile(profile: UserProfile) {
    val existing = userPreferencesDao.getPreferences(Constants.LOCAL_USER_ID)
    val entity = (existing ?: UserPreferencesEntity(userId = Constants.LOCAL_USER_ID)).copy(
        age = profile.age,
        gender = profile.gender,
        weightKg = profile.weightKg,
        weightGoal = profile.weightGoal,
        dietType = profile.dietType,
        cuisinePreferences = profile.cuisinePreferences
            .filter { it.isNotBlank() }
            .joinToString(","),
        allergies = profile.allergies
            .filter { it.isNotBlank() }
            .joinToString(","),
        recommendationsEnabled = profile.recommendationsEnabled,
        isSynced = false,
        lastModifiedAt = System.currentTimeMillis()
    )
    userPreferencesDao.upsertPreferences(entity)
    syncPushManager.schedulePush(SyncEntityType.USER_PREFERENCES, listOf(Constants.LOCAL_USER_ID))
}
```

**Key safety:** `saveProfile()` reads the existing entity first to preserve macro goals. It doesn't blindly create a new entity that would reset calorie/protein/carbs/fat to defaults.

### Step 4.6 — Android: Sync DTO Extension — `SupabaseSyncDto.kt`

**File:** `app/src/main/java/com/app/nutriai/data/remote/dto/SupabaseSyncDto.kt`

**Extend `RemoteUserPreferencesDto` (pull direction):**

```kotlin
@Serializable
data class RemoteUserPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("calorie_goal") val calorieGoal: Double = 2000.0,
    @SerialName("protein_goal") val proteinGoal: Double = 150.0,
    @SerialName("carbs_goal") val carbsGoal: Double = 250.0,
    @SerialName("fat_goal") val fatGoal: Double = 65.0,
    // ── Profile fields ──
    val age: Int? = null,
    val gender: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("weight_goal") val weightGoal: String? = null,
    @SerialName("diet_type") val dietType: String? = null,
    /** Supabase TEXT[] deserializes as List<String> via kotlinx.serialization. */
    @SerialName("cuisine_preferences") val cuisinePreferences: List<String> = emptyList(),
    /** Supabase TEXT[] deserializes as List<String>. */
    val allergies: List<String> = emptyList(),
    @SerialName("recommendations_enabled") val recommendationsEnabled: Boolean = false,
    // ── End profile fields ──
    @SerialName("updated_at") val updatedAt: String? = null
)
```

**Extend `RemoteUserPreferencesPushDto` (push direction):**

```kotlin
@Serializable
data class RemoteUserPreferencesPushDto(
    @SerialName("user_id") val userId: String,
    @SerialName("calorie_goal") val calorieGoal: Double,
    @SerialName("protein_goal") val proteinGoal: Double,
    @SerialName("carbs_goal") val carbsGoal: Double,
    @SerialName("fat_goal") val fatGoal: Double,
    // ── Profile fields ──
    val age: Int? = null,
    val gender: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("weight_goal") val weightGoal: String? = null,
    @SerialName("diet_type") val dietType: String? = null,
    /** Supabase TEXT[] — kotlinx.serialization serializes List<String> as JSON array,
     *  which PostgREST accepts for TEXT[] columns. */
    @SerialName("cuisine_preferences") val cuisinePreferences: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    @SerialName("recommendations_enabled") val recommendationsEnabled: Boolean = false
)
```

**Update `toRemoteDto()` mapper (entity → push DTO):**

```kotlin
fun UserPreferencesEntity.toRemoteDto(supabaseUserId: String): RemoteUserPreferencesPushDto =
    RemoteUserPreferencesPushDto(
        userId = supabaseUserId,
        calorieGoal = calorieGoal,
        proteinGoal = proteinGoal,
        carbsGoal = carbsGoal,
        fatGoal = fatGoal,
        // Profile fields
        age = age,
        gender = gender,
        weightKg = weightKg,
        weightGoal = weightGoal,
        dietType = dietType,
        cuisinePreferences = cuisinePreferences
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        allergies = allergies
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        recommendationsEnabled = recommendationsEnabled
    )
```

**Update `toEntity()` mapper (pull DTO → entity):**

```kotlin
fun RemoteUserPreferencesDto.toEntity(localUserId: String): UserPreferencesEntity =
    UserPreferencesEntity(
        userId = localUserId,
        calorieGoal = calorieGoal,
        proteinGoal = proteinGoal,
        carbsGoal = carbsGoal,
        fatGoal = fatGoal,
        // Profile fields
        age = age,
        gender = gender,
        weightKg = weightKg,
        weightGoal = weightGoal,
        dietType = dietType,
        cuisinePreferences = cuisinePreferences
            .filter { it.isNotBlank() }
            .joinToString(","),
        allergies = allergies
            .filter { it.isNotBlank() }
            .joinToString(","),
        recommendationsEnabled = recommendationsEnabled,
        isSynced = true,
        lastModifiedAt = System.currentTimeMillis()
    )
```

**Array conversion pattern:**
- **Room → Supabase (push):** CSV string `"Indian,Italian"` → `split(",")` → `List<String>` → serialized as JSON array `["Indian","Italian"]` → PostgREST accepts this for `TEXT[]` columns
- **Supabase → Room (pull):** Postgres `TEXT[]` → JSON array `["Indian","Italian"]` → deserialized as `List<String>` → `joinToString(",")` → CSV string `"Indian,Italian"`

### Step 4.7 — Android: ProfileSetupSheet — `ProfileSetupSheet.kt`

**File:** `app/src/main/java/com/app/nutriai/presentation/screens/auth/ProfileSetupSheet.kt` (NEW)

A `ModalBottomSheet` composable for profile entry. Accessed from the Auth/Profile screen.

**UI Layout:**

```
ModalBottomSheet {
  Column(verticalScroll) {
    // Header
    Text("Set Up AI Recommendations")
    Text("Personalize your meal suggestions", secondary)

    // Enable toggle — at the top so user sees it immediately
    Row { Text("Enable AI Recommendations") + Switch(recommendationsEnabled) }
    
    Divider()

    // Only show profile fields when toggle is ON
    AnimatedVisibility(recommendationsEnabled) {
      Column {
        // Age
        OutlinedTextField(age, label = "Age", keyboardType = Number)

        // Gender
        ExposedDropdownMenu(gender, options = ["Male", "Female", "Other", "Prefer not to say"])

        // Weight
        OutlinedTextField(weightKg, label = "Weight (kg)", keyboardType = Decimal)

        // Weight Goal
        Row of FilterChips: [Lose] [Maintain] [Gain]

        // Diet Type
        ExposedDropdownMenu(dietType, options = [
          "Vegetarian", "Veg + Eggs", "Non-Vegetarian", "Pescatarian", "Vegan"
        ])

        // Cuisine Preferences (multi-select)
        Text("Preferred Cuisines")
        FlowRow of FilterChips: [Indian] [South Indian] [Maharashtrian] [Gujarati]
                                [Italian] [French] [Mexican] [Japanese]
                                [Mediterranean] [Chinese] [Thai] [Korean]
        // Each chip is toggleable — selected chips are collected into a list

        // Allergies (multi-select)
        Text("Allergies & Restrictions")
        FlowRow of FilterChips: [Gluten] [Dairy] [Nuts] [Soy] [Shellfish]
                                [Eggs] [Fish] [Sesame]

        Spacer(16.dp)
      }
    }

    // Save button
    Button("Save Profile", enabled = !saving, onClick = onSave)
  }
}
```

**State management:** The sheet takes a `UserProfile` as initial state and a `(UserProfile) -> Unit` callback for save. The parent ViewModel handles persistence.

**Value mappings for Supabase/prompt compatibility:**
- Diet types stored as: `"vegetarian"`, `"veg_eggs"`, `"non_veg"`, `"pescatarian"`, `"vegan"`
- Weight goals stored as: `"lose"`, `"maintain"`, `"gain"`
- Gender stored as: `"male"`, `"female"`, `"other"`, `"prefer_not_to_say"`
- Cuisines and allergies stored as display strings: `"Indian"`, `"Gluten"`, etc.

### Step 4.8 — Android: AuthScreen Integration — `AuthScreen.kt`

**File:** `app/src/main/java/com/app/nutriai/presentation/screens/auth/AuthScreen.kt`

Add a "Set Up Recommendations" button in the authenticated section (where the user sees their profile info, "Sync Now" button, and "Sign Out" button). This button opens the `ProfileSetupSheet`.

```kotlin
// After the existing "Sync Now" button, before "Sign Out":

// Recommendations profile button
var showProfileSheet by remember { mutableStateOf(false) }

OutlinedButton(
    onClick = { showProfileSheet = true },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.AutoAwesome, contentDescription = null)  // sparkle icon
    Spacer(Modifier.width(8.dp))
    Text(if (profile.isComplete) "Edit Recommendations Profile" else "Set Up Recommendations")
}

// Show bottom sheet
if (showProfileSheet) {
    ProfileSetupSheet(
        initialProfile = profile,
        onSave = { updatedProfile ->
            viewModel.saveProfile(updatedProfile)
            showProfileSheet = false
        },
        onDismiss = { showProfileSheet = false }
    )
}
```

**ViewModel changes (`AuthViewModel.kt`):**
- Inject `UserPreferences` (already injected for macro goals)
- Expose `profileFlow: StateFlow<UserProfile>` collected from `userPreferences.profileFlow`
- Add `saveProfile(profile: UserProfile)` → calls `userPreferences.saveProfile(profile)` in `viewModelScope`

### Step 4.9 — Webapp: Types Extension — `domain.ts` + `database.ts`

**File:** `webapp/lib/types/domain.ts`

Add `UserProfile` interface after existing `UserPreferences`:

```ts
/**
 * User's dietary profile — drives internet recommendation personalization.
 * Port of UserProfile.kt
 */
export interface UserProfile {
  age: number | null;
  gender: string | null;
  weightKg: number | null;
  weightGoal: string | null;       // "lose" | "maintain" | "gain"
  dietType: string | null;         // "vegetarian" | "veg_eggs" | "non_veg" | "pescatarian" | "vegan"
  cuisinePreferences: string[];
  allergies: string[];
  recommendationsEnabled: boolean;
}
```

Extend `UserPreferences` interface:

```ts
export interface UserPreferences {
  id: string;
  userId: string;
  calorieGoal: number;
  proteinGoal: number;
  carbsGoal: number;
  fatGoal: number;
  lastModifiedAt: number;
  // Profile fields
  age: number | null;
  gender: string | null;
  weightKg: number | null;
  weightGoal: string | null;
  dietType: string | null;
  cuisinePreferences: string[];
  allergies: string[];
  recommendationsEnabled: boolean;
}
```

**File:** `webapp/lib/types/database.ts`

Extend the `user_preferences` table type with profile columns:

```ts
user_preferences: {
  Row: {
    user_id: string;
    calorie_goal: number;
    protein_goal: number;
    carbs_goal: number;
    fat_goal: number;
    // Profile columns
    age: number | null;
    gender: string | null;
    weight_kg: number | null;
    weight_goal: string | null;
    diet_type: string | null;
    cuisine_preferences: string[];
    allergies: string[];
    recommendations_enabled: boolean;
    updated_at: string;
  };
  Insert: {
    user_id: string;
    calorie_goal?: number;
    protein_goal?: number;
    carbs_goal?: number;
    fat_goal?: number;
    // Profile columns
    age?: number | null;
    gender?: string | null;
    weight_kg?: number | null;
    weight_goal?: string | null;
    diet_type?: string | null;
    cuisine_preferences?: string[];
    allergies?: string[];
    recommendations_enabled?: boolean;
    updated_at?: string;
  };
  Update: {
    user_id?: string;
    calorie_goal?: number;
    protein_goal?: number;
    carbs_goal?: number;
    fat_goal?: number;
    // Profile columns
    age?: number | null;
    gender?: string | null;
    weight_kg?: number | null;
    weight_goal?: string | null;
    diet_type?: string | null;
    cuisine_preferences?: string[];
    allergies?: string[];
    recommendations_enabled?: boolean;
    updated_at?: string;
  };
  Relationships: [];
};
```

### Step 4.10 — Webapp: Profile Hook + Settings UI

**File:** `webapp/lib/hooks/use-user-profile.ts` (NEW)

```ts
"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useAuth } from "@/lib/hooks/use-auth";
import type { UserProfile } from "@/lib/types/domain";

/**
 * Fetches the user's dietary profile from the user_preferences row.
 * Returns null-safe defaults when no profile has been set.
 */
export function useUserProfile() {
  const supabase = useSupabase();
  const { user } = useAuth();

  return useQuery({
    queryKey: ["user-profile", user?.id],
    queryFn: async () => {
      const { data, error } = await supabase
        .from("user_preferences")
        .select("age, gender, weight_kg, weight_goal, diet_type, cuisine_preferences, allergies, recommendations_enabled")
        .eq("user_id", user!.id)
        .maybeSingle();

      if (error) throw error;

      const profile: UserProfile = {
        age: data?.age ?? null,
        gender: data?.gender ?? null,
        weightKg: data?.weight_kg ?? null,
        weightGoal: data?.weight_goal ?? null,
        dietType: data?.diet_type ?? null,
        cuisinePreferences: data?.cuisine_preferences ?? [],
        allergies: data?.allergies ?? [],
        recommendationsEnabled: data?.recommendations_enabled ?? false,
      };
      return profile;
    },
    enabled: !!user?.id,
  });
}

/**
 * Mutation to update profile columns in user_preferences.
 * Only updates profile fields — does NOT touch macro goal columns.
 */
export function useUpdateProfile() {
  const supabase = useSupabase();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (profile: UserProfile) => {
      const { error } = await supabase
        .from("user_preferences")
        .upsert({
          user_id: user!.id,
          age: profile.age,
          gender: profile.gender,
          weight_kg: profile.weightKg,
          weight_goal: profile.weightGoal,
          diet_type: profile.dietType,
          cuisine_preferences: profile.cuisinePreferences,
          allergies: profile.allergies,
          recommendations_enabled: profile.recommendationsEnabled,
        }, { onConflict: "user_id" });

      if (error) throw error;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-profile"] });
      // Also invalidate recommendations so they refetch with new profile
      queryClient.invalidateQueries({ queryKey: ["recommendations"] });
    },
  });
}
```

**IMPORTANT — upsert safety:** The webapp's `useUpdateProfile` uses `.upsert()` with `onConflict: "user_id"`. This creates the row if it doesn't exist (new user who hasn't saved macro goals yet). However, it only sends profile columns — macro goal columns use their Postgres DEFAULT values (2000/150/250/65) if this is the first upsert. This is acceptable because:
- If the user has already saved macro goals, the existing row is updated (only profile columns change)
- If the user hasn't saved macro goals, they get sensible defaults
- The `updated_at` trigger fires, which will be picked up by Android's next sync pull

**File:** `webapp/app/settings/profile-section.tsx` (NEW)

A form component for the profile, inserted between the "Daily Macro Goals" card and the "Appearance" card on the Settings page.

```tsx
"use client";

// ProfileSection component
// Props: none (uses hooks internally)
//
// Layout:
// Card {
//   h2: "AI Recommendations Profile"
//   p: "Personalize your meal suggestions with your dietary preferences."
//
//   // Enable toggle — at the top
//   Row { label: "Enable AI Recommendations" + Toggle switch }
//
//   // Profile fields (shown when toggle is ON, with AnimatePresence or CSS transition)
//   AnimatedSection(visible = recommendationsEnabled) {
//     // Age — number input
//     GoalInput(label="Age", unit="years", value, onChange)
//
//     // Gender — select dropdown
//     Select(options = ["Male", "Female", "Other", "Prefer not to say"])
//
//     // Weight — number input
//     GoalInput(label="Weight", unit="kg", value, onChange)
//
//     // Weight Goal — radio group
//     RadioGroup(options = [
//       { value: "lose", label: "Lose Weight" },
//       { value: "maintain", label: "Maintain" },
//       { value: "gain", label: "Gain Weight" },
//     ])
//
//     // Diet Type — select dropdown
//     Select(options = [
//       { value: "vegetarian", label: "Vegetarian" },
//       { value: "veg_eggs", label: "Veg + Eggs" },
//       { value: "non_veg", label: "Non-Vegetarian" },
//       { value: "pescatarian", label: "Pescatarian" },
//       { value: "vegan", label: "Vegan" },
//     ])
//
//     // Cuisine Preferences — multi-select chip grid
//     ChipGrid(
//       label: "Preferred Cuisines",
//       options: ["Indian", "South Indian", "Maharashtrian", "Gujarati",
//                 "Italian", "French", "Mexican", "Japanese",
//                 "Mediterranean", "Chinese", "Thai", "Korean"],
//       selected: cuisinePreferences,
//       onToggle: (cuisine) => toggle in/out of list
//     )
//
//     // Allergies — multi-select chip grid
//     ChipGrid(
//       label: "Allergies & Restrictions",
//       options: ["Gluten", "Dairy", "Nuts", "Soy", "Shellfish",
//                 "Eggs", "Fish", "Sesame"],
//       selected: allergies,
//       onToggle: (allergy) => toggle in/out of list
//     )
//   }
//
//   // Save button
//   Button("Save Profile", disabled = !hasChanges || saving)
//
//   // Success/error feedback (same pattern as macro goals card)
// }
```

**Styling:** Follow the exact same card pattern used by the "Daily Macro Goals" card above it:
- Same `rounded-md border p-4 space-y-4` container
- Same `var(--bg-surface)` background, `var(--border-variant)` border
- Same `text-sm font-semibold` heading, `text-xs` description
- ChipGrid uses `flex flex-wrap gap-2` with each chip as a small rounded button that toggles `var(--color-primary)` background when selected

**File:** `webapp/app/settings/page.tsx`

Import and render `ProfileSection` between the Daily Goals card and the Appearance card:

```tsx
import { ProfileSection } from "./profile-section";

// In JSX, after the Daily Goals card closing </div> and before the Appearance card:
<ProfileSection />
```

---

## File Inventory

### New files (7):

| # | File | Platform |
|---|------|----------|
| 1 | `supabase/migrations/009_user_profile_columns.sql` | Backend |
| 2 | `supabase/functions/recommend-meals/index.ts` | Backend |
| 3 | `app/.../domain/model/UserProfile.kt` | Android |
| 4 | `app/.../presentation/screens/auth/ProfileSetupSheet.kt` | Android |
| 5 | `webapp/lib/hooks/use-user-profile.ts` | Web |
| 6 | `webapp/app/settings/profile-section.tsx` | Web |
| 7 | `webapp/lib/utils/constants.ts` | Web (add `RECOMMEND_MEALS`) |

### Modified files (10):

| # | File | Change |
|---|------|--------|
| 1 | `supabase/functions/_shared/prompts.ts` | Add `RECOMMENDATION_SYSTEM_INSTRUCTION` + `buildRecommendationPrompt()` |
| 2 | `app/.../data/local/entity/UserPreferencesEntity.kt` | Add 8 profile columns + `toUserProfile()` |
| 3 | `app/.../data/local/migrations/Migrations.kt` | Add `MIGRATION_7_8` + update `ALL` array |
| 4 | `app/.../data/local/NutriAiDatabase.kt` | Bump `version = 7` → `8` |
| 5 | `app/.../util/Constants.kt` | Bump `DATABASE_VERSION = 7` → `8` |
| 6 | `app/.../data/local/preferences/UserPreferences.kt` | Add `profileFlow` + `saveProfile()` |
| 7 | `app/.../data/remote/dto/SupabaseSyncDto.kt` | Extend both DTOs + both mappers with profile fields |
| 8 | `app/.../presentation/screens/auth/AuthScreen.kt` | Add "Set Up Recommendations" button |
| 9 | `app/.../presentation/screens/auth/AuthViewModel.kt` | Expose `profileFlow` + `saveProfile()` |
| 10 | `webapp/lib/types/domain.ts` | Add `UserProfile` interface + extend `UserPreferences` |
| 11 | `webapp/lib/types/database.ts` | Extend `user_preferences` table type |
| 12 | `webapp/app/settings/page.tsx` | Import + render `ProfileSection` |

---

## Implementation Order (within Feature 1)

```
1.1  Migration SQL                    ← Run in Supabase Dashboard
1.2  prompts.ts additions             ← Shared prompt + builder
1.3  recommend-meals/index.ts         ← Edge Function
     ↓ (test with curl / Postman — both includeInternet=false and true)
4.1  UserPreferencesEntity.kt         ← Add 8 columns
4.2  Migrations.kt                    ← Add MIGRATION_7_8
4.3  NutriAiDatabase.kt + Constants   ← Version bump
     ↓ (Android compiles + Room schema validates)
4.4  UserProfile.kt                   ← Domain model
4.5  UserPreferences.kt               ← profileFlow + saveProfile()
4.6  SupabaseSyncDto.kt               ← Extend DTOs + mappers
     ↓ (sync round-trip test: save profile → push → pull → verify)
4.9  domain.ts + database.ts          ← Web types
4.10 use-user-profile.ts + profile-section.tsx + settings/page.tsx ← Web UI
     ↓ (test in browser: save profile → verify in Supabase)
4.7  ProfileSetupSheet.kt             ← Android UI
4.8  AuthScreen.kt                    ← Button + sheet integration
     ↓ (test in emulator: save profile → sync → verify cross-platform)
```

Web profile UI before Android profile UI because it's faster to iterate on and immediately verifiable in Supabase.

---

## Verification Checklist (Feature 1)

### Phase R1 — Edge Function
- [ ] Migration runs without error on existing `user_preferences` data
- [ ] `recommend-meals` Edge Function deploys successfully
- [ ] Returns valid JSON for `mode=time_based` with `includeInternet: false`
- [ ] Returns valid JSON for `mode=time_based` with `includeInternet: true` (when profile exists)
- [ ] Returns valid JSON for `mode=query` with a food-related query
- [ ] Response contains `recommendations` array with `name`, `reason`, `suggested_quantity`, macros, `source`
- [ ] Catalog items have `source: "catalog"` and valid `food_item_id`
- [ ] Internet items have `source: "internet"`, `recipe_text`, and `search_query` (no URLs)
- [ ] Negative remaining macros → "You've hit your daily targets" response
- [ ] Off-topic query → scope error message
- [ ] Query > 200 chars → 400 error
- [ ] Profile null → no errors, omits preferences from prompt

### Phase R4 — Profile Setup
- [ ] Room migration v7→v8 runs without error (existing user_preferences data preserved)
- [ ] `DATABASE_VERSION` = 8 in Constants.kt, `version = 8` in NutriAiDatabase.kt
- [ ] Macro goals still work correctly after migration (no regression)
- [ ] Android: Profile saved via `saveProfile()` → `isSynced = false` → SyncPushManager fires → Supabase row updated with profile columns
- [ ] Android: Profile pulled from Supabase → CSV strings correctly split into lists → `profileFlow` emits updated profile
- [ ] Android: Saving profile preserves existing macro goals (read-before-write in `saveProfile`)
- [ ] Android: ProfileSetupSheet opens from Auth screen, pre-populates from existing profile
- [ ] Android: Toggle OFF → profile fields hidden. Toggle ON → fields appear with animation
- [ ] Webapp: ProfileSection renders on Settings page between Daily Goals and Appearance cards
- [ ] Webapp: Profile upsert creates row if none exists (new user case)
- [ ] Webapp: Saving profile does NOT overwrite macro goals (separate column update)
- [ ] Webapp: Chip grid toggles correctly for cuisines and allergies (multi-select)
- [ ] Cross-platform: Save profile on web → sync on Android → profile appears in ProfileSetupSheet
- [ ] Cross-platform: Save profile on Android → open web Settings → profile fields populated

### Edge Function testable independently
- [ ] `curl -X POST .../functions/v1/recommend-meals -H "Authorization: Bearer <jwt>" -d '{"mode":"time_based","timeOfDay":"morning","remainingMacros":{"calories":1500,"protein":100,"carbs":200,"fat":50},"includeInternet":false}'` returns catalog recs
- [ ] Same with `"includeInternet":true` returns catalog + internet recs (when profile exists)
- [ ] No client UI needed to verify — this is infrastructure
