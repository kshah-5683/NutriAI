# Feature 2: Home Screen Recommendations — Implementation Plan

**Parent plan:** `ai_recommendations.plan.md` (v4) — Phases R2 + R3 + R5
**Prerequisite:** Feature 1 (Internet Recommendations Infrastructure) — Edge Function deployed, profile columns synced, profile UI on both platforms
**Scope:** Home screen recommendation cards on both platforms with full catalog + internet support

---

## What This Feature Delivers

The visible recommendation experience on the Home screen. On today's date, a time-based recommendation card appears between the MacroSummaryCard and the Food Log. It shows 1 featured suggestion + 2-3 expandable alternatives.

**Both catalog and internet sources are supported from day one.** The `includeInternet` flag is read from the user's profile (`recommendationsEnabled`). Users who haven't set up their profile get catalog-only recs. Users who have enabled recommendations get the full catalog + internet experience.

**What Feature 1 already provides:**
- `recommend-meals` Edge Function (deployed + tested)
- `RECOMMENDATION_SYSTEM_INSTRUCTION` + `buildRecommendationPrompt()` in `prompts.ts`
- Profile columns in Supabase + Room (synced cross-platform)
- `UserProfile.kt` domain model + `profileFlow` in `UserPreferences.kt`
- `useUserProfile()` hook on webapp
- Profile UI on both platforms (Settings page / ProfileSetupSheet)

**What this plan adds:**
- `Recommendation.kt` domain model + repository + use case (Android)
- `recommendation.ts` types + `use-recommendations.ts` hook (Webapp)
- `RecommendationCard` UI component handling both catalog + internet sources (both platforms)
- Home screen integration (both platforms)
- "Add to My Foods" flow for internet suggestions (both platforms)
- Internet card UI: recipe text, YouTube/Google search links, estimated macros disclaimer

---

## Phase R2: Android — Home Screen Recommendations

### Step 2.1 — Domain Model: `Recommendation.kt`

**File:** `app/src/main/java/com/app/nutriai/domain/model/Recommendation.kt` (NEW)

```kotlin
data class Recommendation(
    val name: String,
    val description: String,
    val reason: String,
    val suggestedQuantity: Double = 1.0,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val source: RecommendationSource,
    val foodItemId: String? = null,
    val recipeText: String? = null,
    val searchQuery: String? = null,
    val cuisineTag: String? = null
)

enum class RecommendationSource { CATALOG, INTERNET }
```

### Step 2.2 — Repository Interface: `RecommendationRepository.kt`

**File:** `app/src/main/java/com/app/nutriai/domain/repository/RecommendationRepository.kt` (NEW)

```kotlin
interface RecommendationRepository {
    suspend fun getRecommendations(
        mode: String,                           // "time_based" | "query"
        timeOfDay: String,                       // "morning" | "afternoon" | "evening" | "night"
        remainingMacros: RemainingMacros,
        query: String? = null,
        includeInternet: Boolean = false
    ): Resource<List<Recommendation>>
}

data class RemainingMacros(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
```

### Step 2.3 — Repository Implementation: `RecommendationRepositoryImpl.kt`

**File:** `app/src/main/java/com/app/nutriai/data/repository/RecommendationRepositoryImpl.kt` (NEW)

**Key decision: Edge Function via `@Named("supabase")` Retrofit, NOT direct Gemini.**

The Android app currently calls Gemini directly via `GeminiApiService` (Retrofit). For recommendations, we call the **Edge Function** instead, because:
- The Edge Function handles catalog pre-filtering, frequency ranking, and profile fetching server-side
- The Android client doesn't need to replicate any of that logic
- This keeps both platforms (web + Android) using the exact same AI path

**How to call the Edge Function from Android:**

The existing `@Named("supabase")` Retrofit points at `BuildConfig.SUPABASE_URL` (e.g., `https://xxxx.supabase.co/`). Supabase Edge Functions live at `/functions/v1/{function-name}`. The `@Named("supabase")` OkHttpClient already injects `apikey` and `Authorization: Bearer <jwt>` headers.

Add a new Retrofit interface method:

```kotlin
// In a new SupabaseEdgeFunctionService.kt OR added to SupabaseDbApiService.kt
@POST("functions/v1/recommend-meals")
suspend fun recommendMeals(
    @Body body: RecommendMealsRequest
): Response<RecommendMealsResponse>
```

**DTOs needed:**

```kotlin
// RecommendMealsRequest.kt
@Serializable
data class RecommendMealsRequest(
    val mode: String,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    @SerialName("remaining_macros") val remainingMacros: RemainingMacrosDto,
    val query: String? = null,
    @SerialName("include_internet") val includeInternet: Boolean = false
)

@Serializable
data class RemainingMacrosDto(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

// RecommendMealsResponse.kt
@Serializable
data class RecommendMealsResponse(
    val recommendations: List<RecommendationDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class RecommendationDto(
    val name: String,
    val description: String,
    val reason: String = "",
    @SerialName("suggested_quantity") val suggestedQuantity: Double = 1.0,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val source: String,  // "catalog" | "internet"
    @SerialName("food_item_id") val foodItemId: String? = null,
    @SerialName("recipe_text") val recipeText: String? = null,
    @SerialName("search_query") val searchQuery: String? = null,
    @SerialName("cuisine_tag") val cuisineTag: String? = null
)
```

**Implementation flow:**
1. Call `supabaseDbApiService.recommendMeals(request)`
2. Map `RecommendMealsResponse` → `List<Recommendation>` domain models
3. If `response.error` is non-null, return `Resource.Error(response.error)`
4. Wrap in `Resource.Success`
5. Full HTTP error handling matching `AiRepositoryImpl` pattern (429 → rate limit, 502 → AI error, etc.)

### Step 2.4 — Use Case: `GetTimeBasedRecsUseCase.kt`

**File:** `app/src/main/java/com/app/nutriai/domain/usecase/GetTimeBasedRecsUseCase.kt` (NEW)

```kotlin
class GetTimeBasedRecsUseCase @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val syncDataUseCase: SyncDataUseCase,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(
        totalCalories: Double,
        totalProtein: Double,
        totalCarbs: Double,
        totalFat: Double,
        goals: MacroGoals
    ): Resource<List<Recommendation>> {
        // 1. Calculate remaining macros
        val remaining = RemainingMacros(
            calories = goals.calorieGoal - totalCalories,
            protein = goals.proteinGoal - totalProtein,
            carbs = goals.carbsGoal - totalCarbs,
            fat = goals.fatGoal - totalFat
        )

        // 2. Determine time of day from device clock
        val hour = java.time.LocalTime.now().hour
        val timeOfDay = when {
            hour in 6..10  -> "morning"
            hour in 11..14 -> "afternoon"
            hour in 15..18 -> "evening"
            hour in 19..21 -> "night"
            else -> return Resource.Success(emptyList())  // late night — skip recs
        }

        // 3. Trigger foreground sync to ensure catalog is current in Supabase
        // Fire-and-forget — don't block recs on sync failure
        try { syncDataUseCase() } catch (_: Exception) {}

        // 4. Read profile to determine if internet recs are enabled
        val profile = userPreferences.profileFlow.first()
        val includeInternet = profile.recommendationsEnabled

        // 5. Call Edge Function
        return recommendationRepository.getRecommendations(
            mode = "time_based",
            timeOfDay = timeOfDay,
            remainingMacros = remaining,
            includeInternet = includeInternet
        )
    }
}
```

**Key difference from old plan:** `includeInternet` is NOT hardcoded `false`. It reads from the user's profile. Users without a profile get `false` (default). Users who enabled recommendations get `true`.

### Step 2.5 — Hilt DI: `AppModule.kt` changes

**File:** `app/src/main/java/com/app/nutriai/di/AppModule.kt` (or `SupabaseModule.kt`)

Add to `SupabaseModule.kt` (since it already has the `@Named("supabase")` Retrofit):

```kotlin
// No new Retrofit instance needed — reuse @Named("supabase") Retrofit
// The Edge Function endpoint is just another path on the same Supabase base URL
```

Add to a new `RecommendationModule.kt` or the existing module:

```kotlin
@Provides
@Singleton
fun provideRecommendationRepository(
    supabaseDbApiService: SupabaseDbApiService,  // reuse existing service
    json: Json
): RecommendationRepository = RecommendationRepositoryImpl(supabaseDbApiService, json)
```

**OR** — if adding the Edge Function call to `SupabaseDbApiService` feels wrong (it's a PostgREST service), create a dedicated `SupabaseEdgeFunctionService` Retrofit interface using the same `@Named("supabase")` Retrofit instance. The URL path is `/functions/v1/recommend-meals` — same base URL, different path prefix.

### Step 2.6 — ViewModel: `HomeViewModel.kt` changes

**File:** `app/src/main/java/com/app/nutriai/presentation/screens/home/HomeViewModel.kt`

**New DI parameter in constructor:**

```kotlin
class HomeViewModel @Inject constructor(
    // ... existing params ...
    private val getTimeBasedRecsUseCase: GetTimeBasedRecsUseCase,
    private val foodRepository: FoodRepository  // for "Add to My Foods"
) : ViewModel() {
```

**Separate StateFlows (NOT merged into HomeUiState):**

```kotlin
// Recommendation state — lifecycle independent of daily logs flow
private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

private val _recsLoading = MutableStateFlow(false)
val recsLoading: StateFlow<Boolean> = _recsLoading.asStateFlow()

private val _recsError = MutableStateFlow<String?>(null)
val recsError: StateFlow<String?> = _recsError.asStateFlow()

// Track "Add to My Foods" state per recommendation (by name, since internet recs have no ID)
private val _addedToFoods = MutableStateFlow<Set<String>>(emptySet())
val addedToFoods: StateFlow<Set<String>> = _addedToFoods.asStateFlow()

// Track if we've already fetched recs for this session
private var recsFetchedForDate: LocalDate? = null
```

**Why separate StateFlows:** The recs lifecycle is independent of the daily logs flow (`flatMapLatest` on `_selectedDate`). Merging them into `HomeUiState` would mean recs get cleared and refetched every time the user changes the date.

**New function — called from `init` and `onPullToRefresh`:**

```kotlin
private fun fetchRecommendations() {
    val state = uiState.value
    val date = _selectedDate.value

    // Only show recs for today
    if (date != LocalDate.now()) return
    // Skip if already fetched for this date (unless pull-to-refresh)
    if (recsFetchedForDate == date) return

    viewModelScope.launch {
        _recsLoading.value = true

        val result = getTimeBasedRecsUseCase(
            totalCalories = state.totalCalories,
            totalProtein = state.totalProtein,
            totalCarbs = state.totalCarbs,
            totalFat = state.totalFat,
            goals = macroGoals.value
        )

        when (result) {
            is Resource.Success -> {
                _recommendations.value = result.data ?: emptyList()
                _recsError.value = null
                recsFetchedForDate = date
            }
            is Resource.Error -> {
                _recsError.value = result.message
                _recommendations.value = emptyList()
            }
            is Resource.Loading -> {}
        }
        _recsLoading.value = false
    }
}
```

**Trigger on init (after uiState has first emission with today's logs):**

```kotlin
init {
    // ... existing initializeUserUseCase ...

    // Fetch recommendations once daily logs are loaded for today
    viewModelScope.launch {
        uiState.first { !it.isLoading && it.selectedDate == LocalDate.now() }
        fetchRecommendations()
    }
}
```

**Integrate with pull-to-refresh:**

```kotlin
fun onPullToRefresh() {
    viewModelScope.launch {
        _isRefreshing.value = true
        val result = syncThrottleManager.triggerSync()
        _isRefreshing.value = false
        _refreshMessage.tryEmit(result.message)

        // Also refresh recommendations
        recsFetchedForDate = null  // force re-fetch
        fetchRecommendations()
    }
}
```

**"Add to My Foods" function:**

```kotlin
fun addRecommendationToCatalog(rec: Recommendation) {
    viewModelScope.launch {
        try {
            val foodItem = FoodItemEntity(
                id = UUID.randomUUID().toString(),
                catalogId = Constants.INGREDIENT_CATALOG_ID,
                name = rec.name,
                baseServingG = 100.0,
                baseCalories = rec.calories / rec.suggestedQuantity,
                baseProtein = rec.protein / rec.suggestedQuantity,
                baseCarbs = rec.carbs / rec.suggestedQuantity,
                baseFat = rec.fat / rec.suggestedQuantity,
                isSynced = false,
                lastModifiedAt = System.currentTimeMillis()
            )
            foodRepository.insertFood(foodItem)
            _addedToFoods.value = _addedToFoods.value + rec.name
        } catch (e: Exception) {
            // Show error via snackbar
            _refreshMessage.tryEmit("Failed to add ${rec.name} to your foods")
        }
    }
}
```

### Step 2.7 — UI Component: `RecommendationCard.kt`

**File:** `app/src/main/java/com/app/nutriai/presentation/components/RecommendationCard.kt` (NEW)

**Composable signature:**

```kotlin
@Composable
fun RecommendationCard(
    recommendations: List<Recommendation>,
    isLoading: Boolean,
    error: String?,
    addedToFoods: Set<String>,
    onLogCatalogItem: (Recommendation) -> Unit,
    onAddToMyFoods: (Recommendation) -> Unit,
    modifier: Modifier = Modifier
)
```

**UI structure:**

```
Card(shape = RoundedCornerShape(16.dp)) {
  // Header row: "✨ Suggested for you" + time label ("Breakfast", "Dinner", etc.)

  if (isLoading) → ShimmerPlaceholder (3 lines)

  if (error != null) → Small error text, non-blocking

  if (recommendations.isNotEmpty()) {
    // Featured recommendation (first item)
    RecommendationItem(recommendations[0], expanded = true)

    // "Show more" expand button if > 1
    if (expanded && recommendations.size > 1) {
      recommendations.drop(1).forEach { rec ->
        RecommendationItem(rec, expanded = false)
      }
    }
  }
}
```

**RecommendationItem handles both sources:**

```kotlin
@Composable
fun RecommendationItem(
    rec: Recommendation,
    expanded: Boolean,
    isAdded: Boolean,
    onLogCatalogItem: (Recommendation) -> Unit,
    onAddToMyFoods: (Recommendation) -> Unit
) {
    Column {
        // Name (+ "(x2)" if suggestedQuantity > 1)
        Text(rec.name + if (rec.suggestedQuantity > 1) " (x${rec.suggestedQuantity.toInt()})" else "")

        // Reason (only in expanded/featured view)
        if (expanded) {
            Text(rec.reason, style = italic, color = secondary)
        }

        // Macro pills row
        Row { CaloriePill | ProteinPill | CarbsPill | FatPill }

        // Source-specific content
        when (rec.source) {
            RecommendationSource.CATALOG -> {
                Text("From: Your Catalog", caption, color = primary)
                // Future: "Log It" button (navigates to log with pre-filled quantity)
            }
            RecommendationSource.INTERNET -> {
                Text("AI Suggestion", caption, color = tertiary)
                Text("~Estimated macros~", italic, secondary, fontSize = 11.sp)

                // Recipe text (expandable)
                if (rec.recipeText != null) {
                    ExpandableText("View Recipe", rec.recipeText)
                }

                // Action buttons row
                Row(horizontalArrangement = spacedBy(8.dp)) {
                    // "Search YouTube" button
                    if (rec.searchQuery != null) {
                        OutlinedButton(onClick = {
                            val ytUrl = "https://www.youtube.com/results?search_query=" +
                                URLEncoder.encode(rec.searchQuery, "UTF-8")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)))
                        }) {
                            Icon(Icons.Default.PlayCircle, size = 16.dp)
                            Text("YouTube", fontSize = 12.sp)
                        }
                    }

                    // "Add to My Foods" button (idempotent)
                    AddToMyFoodsButton(
                        isAdded = isAdded,
                        onAdd = { onAddToMyFoods(rec) }
                    )
                }
            }
        }
    }
}
```

**`AddToMyFoodsButton` — Idempotent state machine:**

```kotlin
@Composable
fun AddToMyFoodsButton(
    isAdded: Boolean,
    onAdd: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!loading && !isAdded) {
                loading = true
                onAdd()
                // loading → false happens when addedToFoods set updates via recomposition
            }
        },
        enabled = !loading && !isAdded
    ) {
        when {
            isAdded -> { Icon(Icons.Default.Check); Text("Added ✓") }
            loading -> { CircularProgressIndicator(size = 16.dp) }
            else -> { Icon(Icons.Default.Add); Text("Add to My Foods") }
        }
    }
}
```

### Step 2.8 — HomeScreen: Insert RecommendationCard

**File:** `app/src/main/java/com/app/nutriai/presentation/screens/home/HomeScreen.kt`

In the `LazyColumn`, insert a new `item {}` between the MacroSummaryCard item and the "Food Log" section header:

```kotlin
// After MacroSummaryCard item (line ~301)
item {
    MacroSummaryCard(...)
}

// NEW — Recommendation card (only for today)
if (uiState.selectedDate == LocalDate.now()) {
    item {
        RecommendationCard(
            recommendations = recommendations,
            isLoading = recsLoading,
            error = recsError,
            addedToFoods = addedToFoods,
            onLogCatalogItem = { /* Future: navigate to log with pre-fill */ },
            onAddToMyFoods = { rec -> viewModel.addRecommendationToCatalog(rec) }
        )
    }
}

// Existing — Food Log section header
item {
    Row(...) { Text("Food Log") ... }
}
```

**HomeScreen composable — collect separate StateFlows:**

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Separate StateFlows for recommendations
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val recsLoading by viewModel.recsLoading.collectAsStateWithLifecycle()
    val recsError by viewModel.recsError.collectAsStateWithLifecycle()
    val addedToFoods by viewModel.addedToFoods.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        recommendations = recommendations,
        recsLoading = recsLoading,
        recsError = recsError,
        addedToFoods = addedToFoods,
        // ... existing params ...
    )
}
```

---

## Phase R3: Webapp — Home Screen Recommendations

### Step 3.1 — Types: `recommendation.ts`

**File:** `webapp/lib/types/recommendation.ts` (NEW)

```ts
export interface Recommendation {
  name: string;
  description: string;
  reason: string;
  suggestedQuantity: number;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  source: "catalog" | "internet";
  foodItemId: string | null;
  recipeText: string | null;
  searchQuery: string | null;
  cuisineTag: string | null;
}

export interface RecommendMealsResponse {
  recommendations: Recommendation[];
  error?: string;
}

export type TimeOfDay = "morning" | "afternoon" | "evening" | "night";
```

### Step 3.2 — Constants: Add Edge Function name

**File:** `webapp/lib/utils/constants.ts`

Add to `EDGE_FUNCTIONS` (if not already added in Feature 1):

```ts
export const EDGE_FUNCTIONS = {
  // ... existing ...
  RECOMMEND_MEALS: "recommend-meals",
} as const;
```

### Step 3.3 — Hook: `use-recommendations.ts`

**File:** `webapp/lib/hooks/use-recommendations.ts` (NEW)

```ts
"use client";

import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useDailyLogs } from "@/lib/hooks/use-daily-logs";
import { useMacroGoals } from "@/lib/hooks/use-macro-goals";
import { useUserProfile } from "@/lib/hooks/use-user-profile";
import { useDateStore } from "@/lib/stores/date-store";
import { EDGE_FUNCTIONS } from "@/lib/utils/constants";
import { computeDailyTotals } from "@/lib/utils/compute-daily-totals";
import type { RecommendMealsResponse, TimeOfDay } from "@/lib/types/recommendation";

function getTimeOfDay(): TimeOfDay | null {
  const hour = new Date().getHours();
  if (hour >= 6 && hour <= 10) return "morning";
  if (hour >= 11 && hour <= 14) return "afternoon";
  if (hour >= 15 && hour <= 18) return "evening";
  if (hour >= 19 && hour <= 21) return "night";
  return null; // late night — skip recs
}

function isToday(dateStr: string): boolean {
  return dateStr === new Date().toISOString().slice(0, 10);
}

export function useRecommendations() {
  const supabase = useSupabase();
  const selectedDate = useDateStore((s) => s.selectedDate);
  const { data: logs = [] } = useDailyLogs();
  const { data: goals } = useMacroGoals();
  const { data: profile } = useUserProfile();

  const timeOfDay = getTimeOfDay();
  const todaySelected = isToday(selectedDate);
  const includeInternet = profile?.recommendationsEnabled ?? false;

  // Compute remaining macros
  const totals = computeDailyTotals(logs, selectedDate);
  const safeGoals = goals ?? { calorieGoal: 2000, proteinGoal: 150, carbsGoal: 250, fatGoal: 65 };
  const remainingMacros = {
    calories: safeGoals.calorieGoal - totals.totalCalories,
    protein: safeGoals.proteinGoal - totals.totalProtein,
    carbs: safeGoals.carbsGoal - totals.totalCarbs,
    fat: safeGoals.fatGoal - totals.totalFat,
  };

  return useQuery({
    queryKey: ["recommendations", "time_based", timeOfDay, includeInternet],
    queryFn: async () => {
      const { data, error } = await supabase.functions.invoke(
        EDGE_FUNCTIONS.RECOMMEND_MEALS,
        {
          body: {
            mode: "time_based",
            timeOfDay,
            remainingMacros,
            includeInternet,
          },
        }
      );
      if (error) throw new Error(error.message ?? "Failed to get recommendations");
      if (data?.error) throw new Error(data.error);
      return data as RecommendMealsResponse;
    },
    // Only fetch if it's today AND not late night
    enabled: todaySelected && timeOfDay !== null,
    staleTime: 30 * 60 * 1000,      // 30 min — don't refetch on every navigation
    gcTime: 60 * 60 * 1000,          // 1 hour garbage collection
    retry: 1,                         // Retry once on failure
    refetchOnWindowFocus: false,      // Don't refetch when user tabs back
  });
}
```

**CRITICAL — Time-zone safety:** `getTimeOfDay()` uses `new Date()` which is always the client's local time. This hook is `"use client"` — it never runs on the server. No hydration mismatch risk.

**CRITICAL — `includeInternet` in queryKey:** Adding `includeInternet` to the `queryKey` ensures TanStack Query refetches when the user toggles recommendations on/off in Settings. Without it, cached catalog-only results would persist even after enabling internet recs.

### Step 3.4 — Component: `recommendation-card.tsx`

**File:** `webapp/components/recommendation-card.tsx` (NEW)

```tsx
"use client";

// Expandable card showing 1 featured + N alternatives
// Handles BOTH catalog and internet sources from day one
//
// Props:
//   recommendations: Recommendation[]
//   isLoading: boolean
//   error: string | null
//
// States:
//   isLoading → shimmer placeholder (3 pulsing lines)
//   error → small inline error text (non-blocking)
//   empty → nothing rendered (return null)
//   data → featured card + "Show N more" button
//
// Featured item layout:
//   Name (+ "(x2)" if suggestedQuantity > 1)
//   "reason" in italic secondary text
//   Macro pills row: Cal | Protein | Carbs | Fat (using MACRO_COLORS)
//   Source badge: "From: Your Catalog" or "AI Suggestion"
//
// Internet source items additionally show:
//   "~Estimated macros~" italic disclaimer
//   Expandable recipe text section (if recipeText is non-null)
//   "Search YouTube" link: <a href={youtubeUrl} target="_blank">
//   "Search Google" link: <a href={googleUrl} target="_blank">
//   "Add to My Foods" button with loading → "Added ✓" states
//
// URL construction (client-side, deterministic — no AI-generated URLs):
//   const youtubeUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(rec.searchQuery ?? rec.name + " recipe")}`;
//   const googleUrl = `https://www.google.com/search?q=${encodeURIComponent(rec.searchQuery ?? rec.name + " recipe")}`;
//
// Expand/collapse:
//   "Show 2 more suggestions" button → reveals compact items
//   Each compact item: name + macro summary + source badge (no reason)
```

**"Add to My Foods" — webapp implementation:**

```ts
// use-add-to-catalog.ts hook or inline in recommendation-card.tsx

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { useAuth } from "@/lib/hooks/use-auth";
import type { Recommendation } from "@/lib/types/recommendation";

export function useAddToCatalog() {
  const supabase = useSupabase();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (rec: Recommendation) => {
      const tempId = crypto.randomUUID();
      const catalogId = `${user!.id}_local_user_ingredients`;

      const { error } = await supabase.from("food_items").insert({
        id: tempId,
        catalog_id: catalogId,
        name: rec.name,
        base_serving_g: 100,           // Default — editable later
        base_calories: rec.calories / rec.suggestedQuantity,   // Per-serving macros
        base_protein: rec.protein / rec.suggestedQuantity,
        base_carbs: rec.carbs / rec.suggestedQuantity,
        base_fat: rec.fat / rec.suggestedQuantity,
        is_synced: true,
        last_modified_at: Date.now(),
      });

      if (error) throw error;
      return tempId;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["food-items"] });
    },
  });
}
```

**CRITICAL — per-serving conversion:** The recommendation's macros are `total for suggested_quantity servings`. When adding to the catalog, we store **per-serving** macros: `rec.calories / rec.suggestedQuantity`. This matches the catalog convention (`base_calories` = macros per `base_serving_g`).

**"Add to My Foods" button state:** Managed via local component state (`useState`):
1. `idle` → shows "Add to My Foods" button
2. `loading` → shows spinner, button disabled (prevents double-tap)
3. `added` → shows "Added ✓", button permanently disabled for this session
4. `error` → reverts to `idle` with an error toast

### Step 3.5 — Home Page: Insert RecommendationCard

**File:** `webapp/app/page.tsx`

Add between `<MacroSummaryCard>` and `<FoodLogList>`:

```tsx
import { useRecommendations } from "@/lib/hooks/use-recommendations";
import { RecommendationCard } from "@/components/recommendation-card";

// Inside HomePage():
const { data: recsData, isLoading: recsLoading, error: recsError } = useRecommendations();

// In JSX, between MacroSummaryCard and the FoodLogList div:
<RecommendationCard
  recommendations={recsData?.recommendations ?? []}
  isLoading={recsLoading}
  error={recsError?.message ?? null}
/>
```

---

## File Inventory

### New files (8):

| # | File | Platform |
|---|------|----------|
| 1 | `webapp/lib/types/recommendation.ts` | Web |
| 2 | `webapp/lib/hooks/use-recommendations.ts` | Web |
| 3 | `webapp/lib/hooks/use-add-to-catalog.ts` | Web |
| 4 | `webapp/components/recommendation-card.tsx` | Web |
| 5 | `app/.../domain/model/Recommendation.kt` | Android |
| 6 | `app/.../domain/repository/RecommendationRepository.kt` | Android |
| 7 | `app/.../data/repository/RecommendationRepositoryImpl.kt` | Android |
| 8 | `app/.../domain/usecase/GetTimeBasedRecsUseCase.kt` | Android |

Plus DTOs and UI:
| 9 | `app/.../data/remote/dto/RecommendMealsDto.kt` | Android |
| 10 | `app/.../presentation/components/RecommendationCard.kt` | Android |

### Modified files (5):

| # | File | Change |
|---|------|--------|
| 1 | `webapp/lib/utils/constants.ts` | Add `RECOMMEND_MEALS` to `EDGE_FUNCTIONS` (if not in Feature 1) |
| 2 | `webapp/app/page.tsx` | Import + render `RecommendationCard` |
| 3 | `app/.../presentation/screens/home/HomeViewModel.kt` | Add recs state + fetch + addToMyFoods |
| 4 | `app/.../presentation/screens/home/HomeScreen.kt` | Add `RecommendationCard` to LazyColumn |
| 5 | `app/.../di/SupabaseModule.kt` OR new `RecommendationModule.kt` | Provide `RecommendationRepository` |
| 6 | `app/.../data/remote/api/SupabaseDbApiService.kt` | Add `recommendMeals()` endpoint |

---

## Implementation Order (within Feature 2)

```
3.1  recommendation.ts (types)        ← Web types
3.2  constants.ts (edge fn name)      ← Web constant
3.3  use-recommendations.ts           ← Web hook (with includeInternet from profile)
3.4  recommendation-card.tsx          ← Web component (both sources)
     + use-add-to-catalog.ts          ← "Add to My Foods" hook
3.5  page.tsx (home integration)      ← Web wiring
     ↓ (test in browser — verify catalog + internet recs)
2.1  Recommendation.kt               ← Android domain model
2.2  RecommendationRepository.kt      ← Android interface
2.3  RecommendationRepositoryImpl.kt  ← Android implementation + DTOs
2.4  GetTimeBasedRecsUseCase.kt       ← Android use case (reads includeInternet from profile)
2.5  DI wiring                        ← Hilt module
2.6  HomeViewModel.kt changes         ← Android state + addToMyFoods
2.7  RecommendationCard.kt            ← Android composable (both sources)
2.8  HomeScreen.kt changes            ← Android wiring
```

Web before Android because the webapp is faster to iterate on (hot reload, no emulator boot), and the Edge Function (deployed in Feature 1) can be tested via the web UI immediately.

---

## Verification Checklist (Feature 2)

### Home Screen — Catalog Recommendations
- [ ] Webapp: Recommendation card appears on Home below MacroSummaryCard (today only)
- [ ] Webapp: Card shows shimmer during loading, error text on failure
- [ ] Webapp: Navigating to yesterday → no recommendation card shown
- [ ] Webapp: `timeOfDay` computed client-side (no hydration mismatch)
- [ ] Webapp: 30-min staleTime prevents refetch on tab switch
- [ ] Android: Foreground sync fires before rec fetch
- [ ] Android: Recs appear between MacroSummaryCard and Food Log
- [ ] Android: Pull-to-refresh also refreshes recommendations
- [ ] Android: Late night (22:00-05:59) → no recs fetched
- [ ] Android: Recs survive recomposition (StateFlow cache)
- [ ] Android: Changing date to non-today → recs card hidden
- [ ] Catalog items show "From: Your Catalog" badge + valid `food_item_id`

### Home Screen — Internet Recommendations
- [ ] User with `recommendationsEnabled = false` → catalog-only recs (no internet items)
- [ ] User with `recommendationsEnabled = true` → catalog + internet recs
- [ ] Internet items show "AI Suggestion" badge
- [ ] "~Estimated macros~" disclaimer shown on internet rec cards
- [ ] Recipe text expandable section works (if `recipeText` is non-null)
- [ ] "Search YouTube" opens correct URL with encoded `searchQuery`
- [ ] "Search Google" opens correct URL with encoded `searchQuery`
- [ ] No AI-generated URLs anywhere — all links constructed client-side
- [ ] Catalog items still prioritized over internet suggestions (Gemma prompt rule #1)
- [ ] Diet/cuisine/allergy constraints respected
- [ ] Toggling recommendations off in Settings → refetch returns catalog-only results

### "Add to My Foods" Flow
- [ ] Button: tap → loading spinner → "Added ✓" (disabled)
- [ ] Idempotency: double-tap impossible (button disabled during loading)
- [ ] Added food item appears in catalog with per-serving macros (divided by `suggestedQuantity`)
- [ ] Added food item editable via existing edit-food flow
- [ ] Error on "Add to My Foods" → button reverts to original state + error toast
- [ ] Android: `foodRepository.insertFood()` called → `isSynced = false` → SyncPushManager fires

### Edge Cases
- [ ] Empty catalog + `includeInternet = true` → internet-only suggestions (no errors)
- [ ] Empty catalog + `includeInternet = false` → empty/minimal recommendations (graceful)
- [ ] Negative remaining macros → "You've hit your daily targets" response
- [ ] Null profile (no fields filled) → recommendations still work (catalog-only)
- [ ] `suggested_quantity > 1` → card shows "(x2)" suffix, macros reflect total
- [ ] `reason` field displayed on featured recommendation
