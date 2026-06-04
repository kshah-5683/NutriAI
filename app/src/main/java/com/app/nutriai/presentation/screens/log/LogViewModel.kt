package com.app.nutriai.presentation.screens.log

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.domain.model.CatalogMatch
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.model.IngredientKey
import com.app.nutriai.domain.model.MealType
import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.domain.repository.FoodRepository
import com.app.nutriai.domain.usecase.ExtractLabelUseCase
import com.app.nutriai.domain.usecase.LogFoodUseCase
import com.app.nutriai.domain.usecase.LookupNutritionUseCase
import com.app.nutriai.domain.usecase.ParseFoodWithAiUseCase
import com.app.nutriai.domain.usecase.ResolveCatalogCacheUseCase
import com.app.nutriai.util.Constants
import com.app.nutriai.util.ImageCompressor
import com.app.nutriai.util.Resource
import com.app.nutriai.util.UnitConverter
import com.app.nutriai.util.formatMacro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * Represents the current input mode on the Log screen.
 */
enum class LogInputMode {
    /** AI natural language input — user describes food, Gemini parses it */
    AI_INPUT,
    /** Manual form entry — user fills in each field directly */
    MANUAL_INPUT
}

/**
 * Phase 5: Represents the result of an USDA FDC nutrition lookup for a single food item.
 *
 * - [Loading] — lookup is in progress
 * - [Found] — a nutrition match was found; [NutritionInfo] contains per-100g macros
 * - [NotFound] — the API returned no results with usable data for this food name
 * - [Error] — lookup failed due to network or parsing error
 */
sealed class NutritionLookupState {
    data object Loading : NutritionLookupState()
    data class Found(val info: NutritionInfo) : NutritionLookupState()
    data object NotFound : NutritionLookupState()
    data class Error(val message: String) : NutritionLookupState()
}

/**
 * How a serving-size ambiguity was resolved for a specific parsed food item.
 */
enum class ClarificationType { GENERIC, BRAND, WEIGHT_OVERRIDE }

/**
 * Resolution data for a serving-size clarification prompt.
 *
 * @property type How the ambiguity was resolved
 * @property brand Brand name provided by the user (only when [type] == [ClarificationType.BRAND])
 * @property weightOverrideG Gram weight per unit provided by the user (only when [type] == [ClarificationType.WEIGHT_OVERRIDE])
 */
data class ClarificationResolution(
    val type: ClarificationType,
    val brand: String? = null,
    val weightOverrideG: Double? = null
)

/**
 * A single ingredient row in the manual recipe builder.
 *
 * Macros are stored as per-100g strings (same convention as the flat manual form).
 * For discrete units (piece, slice, bowl) they represent per-unit values.
 *
 * [catalogItem] is non-null when the user selected an item from their ingredient catalog.
 * When non-null, macros are pre-filled from catalog data but remain editable.
 */
data class ManualRecipeIngredient(
    val id: String = UUID.randomUUID().toString(),
    /** Non-null when user selected from catalog. */
    val catalogItem: FoodItem? = null,
    /** Name typed manually, or copied from catalog item on selection. */
    val customName: String = "",
    val quantity: String = "1",
    val unit: String = "g",
    /** Per-100g (or per-unit for discrete) — pre-filled from catalog or user-entered. */
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
) {
    /** True when this row has a usable name (catalog or custom). */
    val hasName: Boolean get() = catalogItem != null || customName.isNotBlank()

    /** True when this row has a positive quantity. */
    val hasQuantity: Boolean get() = (quantity.toDoubleOrNull() ?: 0.0) > 0
}

/**
 * UI state for the Log screen food entry form.
 *
 * Supports two input modes:
 * - [LogInputMode.AI_INPUT]: User types a natural language description, AI parses it
 * - [LogInputMode.MANUAL_INPUT]: User fills in individual fields (name, serving, macros)
 *
 * Phase 3 fields (manual entry) are preserved.
 * Phase 4 adds AI-specific fields: [aiInput], [isParsing], [parsedFoods], [selectedParsedFoodIndex].
 * Phase 4.5 adds catalog cache fields: [catalogMatches], [ingredientCatalogMatches], [isResolvingCache].
 * Phase 5 adds nutrition lookup fields: [nutritionLookups], [ingredientNutritionLookups].
 */
data class LogUiState(
    // -- Input mode --
    val inputMode: LogInputMode = LogInputMode.AI_INPUT,

    // -- Catalog routing --
    // When set from Catalog screen FAB, determines which catalog to save to.
    // null = default behavior (AI decides: recipes → recipe catalog, ingredients → ingredient catalog)
    val catalogType: String? = null,

    // -- AI input fields (Phase 4) --
    val aiInput: String = "",
    val isParsing: Boolean = false,
    val parsedFoods: List<ParsedFood> = emptyList(),
    val selectedParsedFoodIndex: Int = 0,
    val aiErrorMessage: String? = null,

    // -- Phase 4.5: Catalog cache resolution --
    val catalogMatches: List<CatalogMatch> = emptyList(),
    val ingredientCatalogMatches: Map<Int, List<CatalogMatch>> = emptyMap(),
    val isResolvingCache: Boolean = false,

    // -- Phase 4.5: Ingredient-level selection within recipes --
    // When non-null, an ingredient inside the selected recipe is focused for editing.
    // The value is the index into parsedFoods[selectedParsedFoodIndex].ingredients.
    val selectedIngredientIndex: Int? = null,

    // -- Phase 5: Nutrition lookup per parsed food index --
    // Key = index in [parsedFoods]. Absent key = lookup not yet started.
    // For flat items: lookup is for the item itself.
    // For recipes: lookup is for each ingredient (see [ingredientNutritionLookups]).
    val nutritionLookups: Map<Int, NutritionLookupState> = emptyMap(),

    // -- Phase 5: Nutrition lookup per recipe ingredient --
    val ingredientNutritionLookups: Map<IngredientKey, NutritionLookupState> = emptyMap(),

    // -- Recipe override: user-toggled recipe flag per parsed food card index --
    // When an index is present here, that flat item will be routed to the Recipes catalog
    // instead of the Ingredients catalog, regardless of what the AI returned (is_recipe=false).
    // Cleared when parsed foods are cleared.
    val recipeOverrides: Set<Int> = emptySet(),

    // -- Serving size clarification state (Phase 17) --
    // Key = index in [parsedFoods]. Absent key = not yet resolved (banner showing).
    // Present key = user has resolved the ambiguity (via generic, brand, or weight override).
    val clarificationResolutions: Map<Int, ClarificationResolution> = emptyMap(),

    // -- Phase 11: Label scanner state --
    /** True while the image is being compressed and sent to Gemma 4 for label reading. */
    val isExtractingLabel: Boolean = false,
    /** Error message from a failed label extraction — shown to the user with retry option. */
    val labelExtractionError: String? = null,
    /** ID of the [LabelPhotoEntity] for the current scan — kept for future reference/cleanup. */
    val labelPhotoId: String? = null,

    // -- Manual form fields (Phase 3) --
    // When non-null, the form was populated from a catalog hit and this is the
    // existing FoodItem ID. LogFoodUseCase will reuse it for the DailyLog instead
    // of inserting a duplicate. Cleared whenever the user edits the food name.
    val sourceCatalogFoodItemId: String? = null,
    // True when the manual form is being used to log a recipe (not a flat ingredient).
    // Routes saveLog() to RECIPE_CATALOG_ID. Set when:
    //   1. acceptParsedFood() accepts the top-level item of a recipe card (isRecipe = true)
    //   2. User taps the "Recipe / Ingredient" toggle in the manual form
    // Cleared when the user edits the food name (they're creating a new item).
    val isLoggingRecipe: Boolean = false,
    // -- Manual recipe builder (new) --
    // Active when isLoggingRecipe == true AND the user has started adding ingredients.
    // Preserved across isLoggingRecipe toggle — only cleared on full reset.
    val manualRecipeIngredients: List<ManualRecipeIngredient> = listOf(ManualRecipeIngredient()),
    val recipeServingQuantity: String = "1",
    val recipeServingUnit: String = "serving",
    val foodName: String = "",
    val brand: String = "",
    // Serving weight in grams — set from catalog item by acceptParsedFood(), defaults to 100.
    // Used by saveLog() to de-normalize per-100g form macros to per-serving for LogFoodUseCase.
    val servingG: Double = Constants.PER_100G_BASE,
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val quantity: String = "1",
    val unit: String = "g",
    val isUnitDropdownExpanded: Boolean = false,
    // -- Meal type (Phase M-Android) --
    val mealType: MealType = MealType.inferFromCurrentTime(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        /** Available unit options for the dropdown — kept in sync with webapp UNIT_OPTIONS */
        val UNIT_OPTIONS = listOf("g", "serving", "tsp", "tbsp", "cup", "ml", "piece", "slice", "bowl")
    }

    /**
     * Whether the form has enough data to save.
     *
     * Recipe builder mode: requires recipe name + at least one ingredient with name + qty.
     * Flat mode: requires food name, valid calories, and positive quantity.
     */
    val isValid: Boolean
        get() {
            val hasBuilderIngredients = manualRecipeIngredients.any { it.hasName }
            return if (isLoggingRecipe && hasBuilderIngredients) {
                // Recipe builder mode
                foodName.isNotBlank()
                        && manualRecipeIngredients.any { it.hasName && it.hasQuantity }
                        && (recipeServingQuantity.toDoubleOrNull() ?: 0.0) > 0
            } else {
                // Flat ingredient mode
                foodName.isNotBlank()
                        && calories.toDoubleOrNull() != null
                        && (calories.toDoubleOrNull() ?: 0.0) >= 0
                        && (quantity.toDoubleOrNull() ?: 0.0) > 0
            }
        }

    /**
     * Whether AI has parsed foods and user can accept them.
     */
    val hasParsedFoods: Boolean
        get() = parsedFoods.isNotEmpty()

    /**
     * The currently selected parsed food (if any).
     */
    val selectedParsedFood: ParsedFood?
        get() = parsedFoods.getOrNull(selectedParsedFoodIndex)

    /**
     * Preview computed totals for the current input.
     *
     * When unit is "grams", macros are entered per 100g (food-label convention) so we
     * scale by quantity / 100. For all other units (serving, tsp, tbsp, cups, ml) macros
     * represent one unit of the selected measure, so we simply multiply by quantity.
     */
    val previewCalories: Double get() = scaleMacro(calories)
    val previewProtein: Double get() = scaleMacro(protein)
    val previewCarbs: Double get() = scaleMacro(carbs)
    val previewFat: Double get() = scaleMacro(fat)

    private fun scaleMacro(macroStr: String): Double {
        val value = macroStr.toDoubleOrNull() ?: 0.0
        val qty = quantity.toDoubleOrNull() ?: 1.0
        return if (UnitConverter.isGramsUnit(unit)) value * (qty / Constants.PER_100G_BASE) else value * qty
    }

    /**
     * Phase 5: True when any nutrition lookup is still in progress.
     */
    val isLookingUpNutrition: Boolean
        get() = nutritionLookups.values.any { it is NutritionLookupState.Loading }
                || ingredientNutritionLookups.values.any { it is NutritionLookupState.Loading }
}

/**
 * One-time events emitted from the LogViewModel.
 */
sealed class LogEvent {
    data object SaveSuccess : LogEvent()
    data class SaveError(val message: String) : LogEvent()
}

/**
 * ViewModel for the food logging screen. Manages both AI-powered and manual
 * food entry, validates input, and saves via [LogFoodUseCase].
 *
 * Phase 3: Manual quantity/macro input.
 * Phase 4: AI-parsed input via [ParseFoodWithAiUseCase] auto-fills form fields.
 * Phase 4.5: Recipe detection + catalog cache resolution.
 * Phase 5: USDA FDC nutrition lookup auto-fills calorie/macro fields.
 *
 * Flow for AI input:
 * 1. User types natural language food description in AI input field
 * 2. Taps "Parse with AI" button -> [parseWithAi] is called
 * 3. Gemma 4 extracts food entities -> [parsedFoods] populated in state
 * 4. Auto-triggers catalog cache resolution -> [catalogMatches] populated
 * 5. Auto-triggers nutrition lookup (Phase 5) -> [nutritionLookups] populated
 * 6. User reviews parsed results with cache badges + nutrition status
 * 7. Taps "Accept" -> [acceptParsedFood] copies name/qty/unit/macros into manual form
 *    (macros pre-filled from catalog cache or USDA FDC result, whichever found first)
 * 8. User reviews/adjusts macros, taps "Log Food" -> [saveLog] saves to Room
 *
 * Graceful fallback: If AI fails, user switches to manual input mode.
 */
@HiltViewModel
class LogViewModel @Inject constructor(
    private val logFoodUseCase: LogFoodUseCase,
    private val parseFoodWithAiUseCase: ParseFoodWithAiUseCase,
    private val resolveCatalogCacheUseCase: ResolveCatalogCacheUseCase,
    private val foodRepository: FoodRepository,
    private val recommendationRepository: com.app.nutriai.domain.repository.RecommendationRepository,
    lookupNutritionUseCase: LookupNutritionUseCase,
    extractLabelUseCase: ExtractLabelUseCase,
    imageCompressor: ImageCompressor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogEvent>()
    val events: SharedFlow<LogEvent> = _events.asSharedFlow()

    // ─── Prefetch trigger (fire-and-forget after every successful log) ──

    /**
     * Triggers the `prefetch-recommendations` Edge Function to refresh the
     * recommendation cache after a food log. Best-effort — errors are swallowed.
     *
     * Phase R2.1: Cache-first Android recommendations.
     */
    private fun triggerPrefetchInBackground() {
        viewModelScope.launch {
            try {
                val startOfDayMs = java.time.LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val currentHour = java.time.LocalTime.now().hour
                recommendationRepository.triggerPrefetch(startOfDayMs, currentHour)
            } catch (_: Exception) {
                // Best-effort — never block the UI
            }
        }
    }

    // ─── Delegates ──────────────────────────────────────────────────────
    private val labelScannerDelegate = LabelScannerDelegate(
        extractLabelUseCase, imageCompressor, _uiState, viewModelScope
    )
    private val nutritionLookupDelegate = NutritionLookupDelegate(
        lookupNutritionUseCase, _uiState, viewModelScope
    )
    private val ingredientListDelegate = IngredientListDelegate(_uiState)

    /**
     * Set the catalog type from navigation arguments.
     * When navigating from Catalog screen FAB, this determines which catalog to save to.
     * When navigating from Home screen FAB, this is null (default AI routing).
     *
     * When the target catalog is the Recipes catalog, [isLoggingRecipe] is automatically
     * set to true so the recipe builder is shown immediately. The Recipe/Ingredient toggle
     * is hidden in catalog mode (showTypeSelector=false), so without this the user would
     * be stuck in the flat ingredient form with no way to switch to the recipe builder.
     */
    fun setCatalogType(catalogType: String?) {
        _uiState.update { state ->
            state.copy(
                catalogType = catalogType,
                isLoggingRecipe = catalogType == Constants.RECIPE_CATALOG_ID
            )
        }
    }

    // -- Input mode switching --

    fun switchToAiInput() {
        _uiState.update { it.copy(inputMode = LogInputMode.AI_INPUT, errorMessage = null) }
    }

    fun switchToManualInput() {
        _uiState.update { it.copy(inputMode = LogInputMode.MANUAL_INPUT, aiErrorMessage = null) }
    }

    // -- Label scanner (delegated to LabelScannerDelegate) --

    fun onLabelPhotoSelected(uri: Uri, sourceType: String = "gallery") =
        labelScannerDelegate.onLabelPhotoSelected(uri, sourceType)

    fun clearLabelExtraction() = labelScannerDelegate.clearLabelExtraction()

    // -- AI input methods (Phase 4) --

    fun updateAiInput(input: String) {
        _uiState.update { it.copy(aiInput = input, aiErrorMessage = null) }
    }

    /**
     * Send the natural language food description to Gemma 4 for parsing.
     * On success, populates [LogUiState.parsedFoods] with extracted food entities.
     * Phase 4.5: After successful parse, auto-triggers catalog cache resolution.
     * Phase 5: After cache resolution, auto-triggers USDA FDC nutrition lookup.
     * On failure, shows error and user can retry or switch to manual entry.
     */
    fun parseWithAi() {
        val input = _uiState.value.aiInput
        if (input.isBlank()) {
            _uiState.update { it.copy(aiErrorMessage = "Please describe what you ate") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    aiErrorMessage = null,
                    parsedFoods = emptyList(),
                    catalogMatches = emptyList(),
                    ingredientCatalogMatches = emptyMap(),
                    nutritionLookups = emptyMap(),
                    ingredientNutritionLookups = emptyMap(),
                    clarificationResolutions = emptyMap()
                )
            }

            when (val result = parseFoodWithAiUseCase(input)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            parsedFoods = result.data,
                            selectedParsedFoodIndex = 0,
                            aiErrorMessage = null
                        )
                    }
                    // Phase 4.5: Resolve catalog cache, then Phase 5: lookup nutrition
                    resolveCatalogCache(result.data)
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            parsedFoods = emptyList(),
                            aiErrorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    // Shouldn't reach here (suspend function), but handle gracefully
                }
            }
        }
    }

    /**
     * Phase 4.5: Resolve parsed foods against the local catalogs.
     * Phase 5: After resolution completes, auto-triggers [lookupNutrition].
     */
    private suspend fun resolveCatalogCache(parsedFoods: List<ParsedFood>) {
        _uiState.update { it.copy(isResolvingCache = true) }
        try {
            val matches = resolveCatalogCacheUseCase(parsedFoods)

            val ingredientMatches = mutableMapOf<Int, List<CatalogMatch>>()
            parsedFoods.forEachIndexed { index, food ->
                if (food.isRecipe && food.ingredients.isNotEmpty()) {
                    val ingMatches = resolveCatalogCacheUseCase.resolveIngredients(food.ingredients)
                    ingredientMatches[index] = ingMatches
                }
            }

            // Phase 17: Auto-resolve clarification for catalog-matched items.
            // Catalog data is the user's own historical macros — no need for clarification.
            val autoResolved = mutableMapOf<Int, ClarificationResolution>()
            parsedFoods.forEachIndexed { index, food ->
                val isFromCatalog = matches.getOrNull(index)?.isFromCatalog == true
                if (food.needsClarification && isFromCatalog) {
                    autoResolved[index] = ClarificationResolution(type = ClarificationType.GENERIC)
                }
            }

            _uiState.update {
                it.copy(
                    catalogMatches = matches,
                    ingredientCatalogMatches = ingredientMatches,
                    isResolvingCache = false,
                    clarificationResolutions = it.clarificationResolutions + autoResolved
                )
            }

            // Phase 5: Kick off nutrition lookup after catalog resolution.
            // Skips items already found in the catalog (macros already available).
            // Phase 17: Also skips items that need clarification (paused until user resolves).
            lookupNutrition(parsedFoods, matches, ingredientMatches)

        } catch (e: Exception) {
            // Cache resolution failure is non-critical — just skip badges
            _uiState.update { it.copy(isResolvingCache = false) }
            // Still attempt nutrition lookup even if cache resolution failed
            lookupNutrition(parsedFoods, emptyList(), emptyMap())
        }
    }

    // ─── Nutrition lookup (delegated to NutritionLookupDelegate) ────────

    private suspend fun lookupNutrition(
        parsedFoods: List<ParsedFood>,
        catalogMatches: List<CatalogMatch>,
        ingredientCatalogMatches: Map<Int, List<CatalogMatch>>
    ) = nutritionLookupDelegate.lookupNutrition(parsedFoods, catalogMatches, ingredientCatalogMatches)

    /**
     * Select a parsed food item from the AI results list.
     * Resets any ingredient-level selection.
     */
    fun selectParsedFood(index: Int) {
        _uiState.update {
            it.copy(
                selectedParsedFoodIndex = index.coerceIn(0, it.parsedFoods.lastIndex.coerceAtLeast(0)),
                selectedIngredientIndex = null
            )
        }
    }

    /**
     * Phase 4.5: Select a specific ingredient within the currently selected recipe.
     * This enables "Edit Selected" to open the manual form for that ingredient.
     *
     * @param ingredientIndex Index into the recipe's ingredients list.
     *        Pass null to deselect (back to recipe-level selection).
     */
    fun selectIngredient(ingredientIndex: Int?) {
        _uiState.update { it.copy(selectedIngredientIndex = ingredientIndex) }
    }

    // ─── Serving size clarification actions (Phase 17) ──────────────────

    /**
     * User chose "Use generic" — accept the generic USDA/IFCT estimate.
     * Resolves the clarification and triggers a standard nutrition lookup.
     */
    fun resolveClarificationGeneric(index: Int) {
        _uiState.update {
            it.copy(
                clarificationResolutions = it.clarificationResolutions +
                    (index to ClarificationResolution(type = ClarificationType.GENERIC))
            )
        }
        // Trigger standard lookup now that clarification is resolved
        val food = _uiState.value.parsedFoods.getOrNull(index) ?: return
        val catalogMatch = _uiState.value.catalogMatches.getOrNull(index)
        if (catalogMatch?.isFromCatalog != true) {
            viewModelScope.launch {
                nutritionLookupDelegate.performAndUpdateLookup(index, food.name)
            }
        }
    }

    /**
     * User provided a brand name — trigger a brand-specific FDC lookup.
     */
    fun resolveClarificationWithBrand(index: Int, brand: String) {
        _uiState.update {
            it.copy(
                clarificationResolutions = it.clarificationResolutions +
                    (index to ClarificationResolution(type = ClarificationType.BRAND, brand = brand))
            )
        }
        val food = _uiState.value.parsedFoods.getOrNull(index) ?: return
        viewModelScope.launch {
            nutritionLookupDelegate.performAndUpdateLookup(index, food.name, brand = brand)
        }
    }

    /**
     * User provided a weight in grams — override servingWeightG client-side.
     * Triggers a standard lookup (no brand); the weight override will be
     * applied when accepting the parsed food into the manual form.
     */
    fun resolveClarificationWithWeight(index: Int, weightG: Double) {
        _uiState.update {
            it.copy(
                clarificationResolutions = it.clarificationResolutions +
                    (index to ClarificationResolution(type = ClarificationType.WEIGHT_OVERRIDE, weightOverrideG = weightG))
            )
        }
        val food = _uiState.value.parsedFoods.getOrNull(index) ?: return
        val catalogMatch = _uiState.value.catalogMatches.getOrNull(index)
        if (catalogMatch?.isFromCatalog != true) {
            viewModelScope.launch {
                nutritionLookupDelegate.performAndUpdateLookup(index, food.name)
            }
        }
    }

    /**
     * Accept the selected parsed food and populate the manual form fields.
     * Copies name, quantity, and unit from the AI result into the form.
     *
     * Macro pre-fill priority (Phase 5):
     * 1. Catalog cache hit (Phase 4.5) — user's own historical data
     * 2. USDA FDC match (Phase 5) — scaled from per-100g to serving size
     * 3. Empty — user must fill in manually
     *
     * When a specific ingredient is selected within a recipe
     * ([LogUiState.selectedIngredientIndex] is non-null), edits that ingredient.
     * Otherwise, edits the top-level item (recipe or flat food).
     */
    fun acceptParsedFood() {
        val state = _uiState.value
        val topLevelFood = state.selectedParsedFood ?: return

        val ingredientIdx = state.selectedIngredientIndex
        val isEditingIngredient = topLevelFood.isRecipe
                && ingredientIdx != null
                && ingredientIdx in topLevelFood.ingredients.indices

        val targetFood: ParsedFood
        val cachedFood: com.app.nutriai.domain.model.FoodItem?
        val nutritionInfo: NutritionInfo?

        if (isEditingIngredient) {
            targetFood = topLevelFood.ingredients[ingredientIdx!!]
            val ingMatches = state.ingredientCatalogMatches[state.selectedParsedFoodIndex]
            cachedFood = ingMatches?.getOrNull(ingredientIdx)?.matchedFoodItem

            // Phase 5: get nutrition from ingredient lookups
            val key = IngredientKey(state.selectedParsedFoodIndex, ingredientIdx)
            nutritionInfo = (state.ingredientNutritionLookups[key] as? NutritionLookupState.Found)?.info
        } else {
            targetFood = topLevelFood
            val catalogMatch = state.catalogMatches.getOrNull(state.selectedParsedFoodIndex)
            cachedFood = catalogMatch?.matchedFoodItem

            // Phase 5: get nutrition from top-level lookups
            nutritionInfo = (state.nutritionLookups[state.selectedParsedFoodIndex] as? NutritionLookupState.Found)?.info
        }

        val resolvedCalories: String
        val resolvedProtein: String
        val resolvedCarbs: String
        val resolvedFat: String

        when {
            // Priority 1: Catalog cache (user's own data — most accurate for their portion sizes)
            cachedFood != null -> {
                resolvedCalories = cachedFood.baseCalories.formatMacro()
                resolvedProtein = cachedFood.baseProtein.formatMacro()
                resolvedCarbs = cachedFood.baseCarbs.formatMacro()
                resolvedFat = cachedFood.baseFat.formatMacro()
            }
            // Priority 2: USDA FDC match.
            // Form fields represent PER-UNIT values (per 1g, per 1 tsp, per 1 piece, …).
            // scaleMacro() = value × qty and saveLog() = calories × rawQty — so we must
            // NOT pre-multiply by total qty here or we double-scale.
            //
            // For "g": store raw per-100g value — scaleMacro scales by qty/100 itself.
            // For all other units: compute the multiplier for exactly 1 unit of measure
            //   (e.g. 1 tbsp = 15g → multiplier 0.15) and pre-scale once to get per-unit
            //   calories. The form label "Nutrition (per tbsp)" confirms per-unit semantics.
            // For "piece/slice/bowl": uses servingWeightG from FDC when available so that
            //   "1 piece egg (50g)" correctly yields ~78 kcal instead of 156 kcal/100g.
            nutritionInfo != null -> {
                val isGrams = UnitConverter.isGramsUnit(targetFood.unit)
                // Phase 17: Weight override from clarification takes priority over FDC servingWeightG
                val clarification = state.clarificationResolutions[state.selectedParsedFoodIndex]
                val effectiveServingWeightG = clarification?.weightOverrideG ?: nutritionInfo.servingWeightG
                // Per-UNIT multiplier: multiplier for exactly 1 unit (not all qty units).
                val perUnitMultiplier = UnitConverter.computeServingMultiplier(
                    quantity = 1.0,
                    unit = targetFood.unit,
                    servingWeightG = effectiveServingWeightG
                )
                resolvedCalories = if (isGrams) nutritionInfo.caloriesPer100g.formatMacro()
                                   else (nutritionInfo.caloriesPer100g * perUnitMultiplier).formatMacro()
                resolvedProtein  = if (isGrams) nutritionInfo.proteinPer100g.formatMacro()
                                   else (nutritionInfo.proteinPer100g * perUnitMultiplier).formatMacro()
                resolvedCarbs    = if (isGrams) nutritionInfo.carbsPer100g.formatMacro()
                                   else (nutritionInfo.carbsPer100g * perUnitMultiplier).formatMacro()
                resolvedFat      = if (isGrams) nutritionInfo.fatPer100g.formatMacro()
                                   else (nutritionInfo.fatPer100g * perUnitMultiplier).formatMacro()
            }
            // Priority 3: No data — user fills in manually
            else -> {
                resolvedCalories = ""
                resolvedProtein = ""
                resolvedCarbs = ""
                resolvedFat = ""
            }
        }

        _uiState.update {
            it.copy(
                inputMode = LogInputMode.MANUAL_INPUT,
                foodName = targetFood.name,
                quantity = targetFood.quantity.formatMacro(),
                unit = targetFood.unit,
                // Preserve catalog serving weight so saveLog() can de-normalize per-100g
                // form macros to per-serving for LogFoodUseCase. Defaults to 100 when
                // there's no catalog item (nutrition lookup or manual entry).
                servingG = cachedFood?.baseServingG ?: Constants.PER_100G_BASE,
                calories = resolvedCalories,
                protein = resolvedProtein,
                carbs = resolvedCarbs,
                fat = resolvedFat,
                // Track catalog item ID so saveLog() can reuse it instead of inserting a duplicate.
                // Only set when macros came from the catalog cache (Priority 1).
                // Priority 2 (OFFs) and 3 (empty) always create a new FoodItem.
                sourceCatalogFoodItemId = cachedFood?.id,
                // Route to Recipes catalog when:
                //  a) editing the top-level item of an AI-detected recipe card, OR
                //  b) the user manually toggled this card to "Recipe" via the chip override.
                // Editing an individual ingredient (isEditingIngredient=true) stays in Ingredients.
                isLoggingRecipe = !isEditingIngredient &&
                        (topLevelFood.isRecipe || state.recipeOverrides.contains(state.selectedParsedFoodIndex)),
                // Clear selection state
                aiErrorMessage = null,
                selectedIngredientIndex = null,
                errorMessage = null
            )
        }
    }

    /**
     * Accept ALL parsed foods and log them in one batch.
     *
     * Phase 5: Macro priority for each food when saving:
     * 1. Catalog cache macros (if the food was found in the local catalog)
     * 2. USDA FDC macros scaled to serving size
     * 3. 0-value macros (user can update later)
     *
     * Phase 4.5: Recipes are logged with aggregated ingredient macros via [LogFoodUseCase.logRecipe].
     */
    fun acceptAndLogAllParsed() {
        val state = _uiState.value
        val foods = state.parsedFoods
        if (foods.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val todayMillis = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val isCatalogOnly = state.catalogType != null

                foods.forEachIndexed { index, food ->
                    if (food.isRecipe && food.ingredients.isNotEmpty()) {
                        // Phase 4.5: Log recipe with ingredient matches
                        val ingredientMatches = state.ingredientCatalogMatches[index]
                            ?: food.ingredients.map { CatalogMatch(parsedFood = it) }

                        // Phase 5: Enrich ingredient matches with nutrition data
                        val enrichedIngredientMatches = ingredientMatches.mapIndexed { ingIndex, match ->
                            if (match.isFromCatalog) {
                                // Already has macros from catalog — use as-is
                                match
                            } else {
                                val key = IngredientKey(index, ingIndex)
                                val nutritionState = state.ingredientNutritionLookups[key]
                                val nutritionInfo = (nutritionState as? NutritionLookupState.Found)?.info
                                if (nutritionInfo != null) {
                                    val ingredient = match.parsedFood
                                    val multiplier = UnitConverter.computeServingMultiplier(
                                        ingredient.quantity,
                                        ingredient.unit,
                                        nutritionInfo.servingWeightG
                                    )
                                    val enrichedFoodItem = com.app.nutriai.domain.model.FoodItem(
                                        id = java.util.UUID.randomUUID().toString(),
                                        catalogId = Constants.INGREDIENT_CATALOG_ID,
                                        name = ingredient.name,
                                        baseServingG = Constants.PER_100G_BASE,
                                        baseCalories = nutritionInfo.caloriesPer100g * multiplier,
                                        baseProtein = nutritionInfo.proteinPer100g * multiplier,
                                        baseCarbs = nutritionInfo.carbsPer100g * multiplier,
                                        baseFat = nutritionInfo.fatPer100g * multiplier,
                                        externalApiId = nutritionInfo.externalId,
                                        lastModifiedAt = System.currentTimeMillis()
                                    )
                                    match.copy(matchedFoodItem = enrichedFoodItem)
                                } else {
                                    match
                                }
                            }
                        }

                        logFoodUseCase.logRecipe(
                            recipeName = food.name,
                            ingredientMatches = enrichedIngredientMatches,
                            quantity = food.quantity,
                            unit = food.unit,
                            dateTimestamp = todayMillis,
                            skipDailyLog = isCatalogOnly,
                            mealType = state.mealType.value
                        )
                    } else {
                        // Flat item: determine macros from cache → nutrition → 0
                        val catalogMatch = state.catalogMatches.getOrNull(index)
                        val cachedFood = catalogMatch?.matchedFoodItem
                        val isFromCatalog = catalogMatch?.isFromCatalog == true

                        val nutritionState = state.nutritionLookups[index]
                        val nutritionInfo = (nutritionState as? NutritionLookupState.Found)?.info

                        // LogFoodUseCase now handles:
                        //   1. Normalizing per-serving macros to per-100g
                        //   2. Gram→multiplier conversion for storedQty
                        //   3. computeServingMultiplier with servingG for daily log
                        //
                        // All callers pass RAW quantities and PER-SERVING macros.
                        // The use case does the rest.
                        val logCalories: Double
                        val logProtein: Double
                        val logCarbs: Double
                        val logFat: Double
                        val logServingG: Double

                        when {
                            isFromCatalog -> {
                                // Catalog hit: baseCalories is per-100g (after migration).
                                // De-normalize to per-serving for the use case contract.
                                val scale = (cachedFood?.baseServingG ?: Constants.PER_100G_BASE) / Constants.PER_100G_BASE
                                logCalories  = (cachedFood?.baseCalories ?: 0.0) * scale
                                logProtein   = (cachedFood?.baseProtein  ?: 0.0) * scale
                                logCarbs     = (cachedFood?.baseCarbs    ?: 0.0) * scale
                                logFat       = (cachedFood?.baseFat      ?: 0.0) * scale
                                logServingG  = cachedFood?.baseServingG ?: Constants.PER_100G_BASE
                            }
                            nutritionInfo != null && UnitConverter.isGramsUnit(food.unit) -> {
                                // Grams + FDC: per-100g base. servingG=100 so normFactor=1.0
                                logCalories  = nutritionInfo.caloriesPer100g
                                logProtein   = nutritionInfo.proteinPer100g
                                logCarbs     = nutritionInfo.carbsPer100g
                                logFat       = nutritionInfo.fatPer100g
                                logServingG  = Constants.PER_100G_BASE
                            }
                            nutritionInfo != null -> {
                                // Non-gram + FDC: per-unit base (per100g × 1-unit multiplier).
                                // servingG=100 so normFactor=1.0 — these are effectively "per serving"
                                // where 1 serving ≈ 1 unit.
                                val perUnitMult = UnitConverter.computeServingMultiplier(
                                    1.0, food.unit, nutritionInfo.servingWeightG
                                )
                                logCalories  = nutritionInfo.caloriesPer100g * perUnitMult
                                logProtein   = nutritionInfo.proteinPer100g  * perUnitMult
                                logCarbs     = nutritionInfo.carbsPer100g    * perUnitMult
                                logFat       = nutritionInfo.fatPer100g      * perUnitMult
                                logServingG  = Constants.PER_100G_BASE
                            }
                            else -> {
                                logCalories  = 0.0
                                logProtein   = 0.0
                                logCarbs     = 0.0
                                logFat       = 0.0
                                logServingG  = Constants.PER_100G_BASE
                            }
                        }

                        // Route to Recipes catalog if the user toggled this card as a recipe.
                        // Otherwise default to the Ingredients catalog (or navigation catalogType).
                        val flatCatalogId = when {
                            isCatalogOnly -> state.catalogType ?: Constants.INGREDIENT_CATALOG_ID
                            state.recipeOverrides.contains(index) -> Constants.RECIPE_CATALOG_ID
                            else -> Constants.INGREDIENT_CATALOG_ID
                        }

                        logFoodUseCase(
                            foodName = food.name,
                            brand = if (isFromCatalog) cachedFood?.brand else nutritionInfo?.brand,
                            servingG = logServingG,
                            calories = logCalories,
                            protein  = logProtein,
                            carbs    = logCarbs,
                            fat      = logFat,
                            quantity = food.quantity,  // raw — use case handles gram conversion
                            unit = food.unit,
                            dateTimestamp = todayMillis,
                            catalogId = flatCatalogId,
                            skipDailyLog = isCatalogOnly,
                            externalApiId = if (isFromCatalog) null else nutritionInfo?.externalId,
                            // Reuse the existing FoodItem ID for catalog hits to prevent duplicates.
                            existingFoodItemId = if (isFromCatalog) cachedFood?.id else null,
                            mealType = state.mealType.value
                        )
                    }
                }

                val catalogType = _uiState.value.catalogType
                _uiState.update { LogUiState(catalogType = catalogType) }
                triggerPrefetchInBackground()
                _events.emit(LogEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save food logs"
                    )
                }
                _events.emit(LogEvent.SaveError(e.message ?: "Unknown error"))
            }
        }
    }

    // ── Ingredient list mutation ───────────────────────────────────────

    /** Remove a single ingredient from a parsed recipe card. Delegates to [IngredientListDelegate]. */
    fun removeIngredient(foodIndex: Int, ingredientIndex: Int) =
        ingredientListDelegate.removeIngredient(foodIndex, ingredientIndex)

    /** Move an ingredient one position up. Delegates to [IngredientListDelegate]. */
    fun moveIngredientUp(foodIndex: Int, ingredientIndex: Int) =
        ingredientListDelegate.moveIngredientUp(foodIndex, ingredientIndex)

    /** Move an ingredient one position down. Delegates to [IngredientListDelegate]. */
    fun moveIngredientDown(foodIndex: Int, ingredientIndex: Int) =
        ingredientListDelegate.moveIngredientDown(foodIndex, ingredientIndex)

    /**
     * Clear parsed foods and return to AI input.
     */
    fun clearParsedFoods() {
        _uiState.update {
            it.copy(
                parsedFoods = emptyList(),
                selectedParsedFoodIndex = 0,
                selectedIngredientIndex = null,
                aiErrorMessage = null,
                catalogMatches = emptyList(),
                ingredientCatalogMatches = emptyMap(),
                nutritionLookups = emptyMap(),
                ingredientNutritionLookups = emptyMap(),
                recipeOverrides = emptySet(),
                clarificationResolutions = emptyMap(),
                // Phase 11: also clear label extraction state
                isExtractingLabel = false,
                labelExtractionError = null,
                labelPhotoId = null
            )
        }
    }

    /**
     * Toggles whether the manual form entry is treated as a recipe or an ingredient.
     * Recipes are saved to [Constants.RECIPE_CATALOG_ID]; ingredients to [Constants.INGREDIENT_CATALOG_ID].
     * Called from the Recipe/Ingredient segmented button on the Log screen.
     */
    fun toggleIsLoggingRecipe(isRecipe: Boolean) {
        _uiState.update { it.copy(isLoggingRecipe = isRecipe, sourceCatalogFoodItemId = null) }
    }

    /**
     * Toggle whether a flat parsed food card (AI returned is_recipe=false) should be
     * treated as a recipe and saved to the Recipes catalog.
     *
     * Tapping the chip a second time restores it to Ingredient routing.
     * Has no effect on cards where [ParsedFood.isRecipe] is already true (those are
     * always routed to Recipes regardless).
     */
    fun toggleRecipeOverride(index: Int) {
        _uiState.update { state ->
            val current = state.recipeOverrides
            val updated = if (current.contains(index)) current - index else current + index
            state.copy(recipeOverrides = updated)
        }
    }

    // -- Meal type (Phase M-Android) --

    fun setMealType(type: MealType) {
        _uiState.update { it.copy(mealType = type) }
    }

    // -- Manual form methods (Phase 3 — preserved) --

    fun updateFoodName(name: String) {
        _uiState.update {
            it.copy(
                foodName = name,
                // If the user edits the food name, they're creating a custom item.
                // Clear the catalog link and reset servingG so we don't carry over
                // the catalog item's serving weight to a new custom item.
                sourceCatalogFoodItemId = null,
                servingG = Constants.PER_100G_BASE,
                // NOTE: isLoggingRecipe is intentionally NOT cleared here.
                // Previously this reset it to false, but that broke the manual recipe builder:
                // typing the recipe name collapsed the ingredient list on every keystroke.
                // Recipe routing is controlled solely by toggleIsLoggingRecipe() (the
                // segmented button) and setCatalogType() (navigation from Recipes catalog).
            )
        }
    }

    fun updateBrand(brand: String) {
        _uiState.update { it.copy(brand = brand) }
    }

    fun updateCalories(cal: String) {
        _uiState.update { it.copy(calories = cal) }
    }

    fun updateProtein(protein: String) {
        _uiState.update { it.copy(protein = protein) }
    }

    fun updateCarbs(carbs: String) {
        _uiState.update { it.copy(carbs = carbs) }
    }

    fun updateFat(fat: String) {
        _uiState.update { it.copy(fat = fat) }
    }

    fun updateQuantity(qty: String) {
        _uiState.update { it.copy(quantity = qty) }
    }

    fun updateUnit(unit: String) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun toggleUnitDropdown() {
        _uiState.update { it.copy(isUnitDropdownExpanded = !it.isUnitDropdownExpanded) }
    }

    fun dismissUnitDropdown() {
        _uiState.update { it.copy(isUnitDropdownExpanded = false) }
    }

    fun selectUnit(unit: String) {
        _uiState.update { it.copy(unit = unit, isUnitDropdownExpanded = false) }
    }

    /**
     * Validate input and save the food log to Room.
     */
    fun saveLog() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val todayMillis = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                // Resolve catalog routing priority:
                // 1. Explicit catalogType from navigation (Catalog screen FAB) — always wins
                // 2. isLoggingRecipe flag (set by acceptParsedFood or the Recipe/Ingredient toggle)
                // 3. Default: Ingredients
                val targetCatalogId = state.catalogType
                    ?: if (state.isLoggingRecipe) Constants.RECIPE_CATALOG_ID
                    else Constants.INGREDIENT_CATALOG_ID

                // LogFoodUseCase normalizes macros to per-100g and handles
                // gram→multiplier conversion internally. Pass raw quantity.
                val rawQty = state.quantity.toDouble()

                // Form macros are per-100g. LogFoodUseCase expects per-serving.
                // De-normalize: perServing = per100g × (servingG / 100).
                // servingG is set from catalog item by acceptParsedFood(); defaults to 100
                // for manual entry (deNorm = 1.0 — no change).
                val actualServingG = state.servingG
                val deNorm = actualServingG / Constants.PER_100G_BASE

                logFoodUseCase(
                    foodName = state.foodName,
                    brand = state.brand.ifBlank { null },
                    servingG = actualServingG,
                    calories = state.calories.toDouble() * deNorm,
                    protein = (state.protein.toDoubleOrNull() ?: 0.0) * deNorm,
                    carbs = (state.carbs.toDoubleOrNull() ?: 0.0) * deNorm,
                    fat = (state.fat.toDoubleOrNull() ?: 0.0) * deNorm,
                    quantity = rawQty,
                    unit = state.unit,
                    dateTimestamp = todayMillis,
                    catalogId = targetCatalogId,
                    skipDailyLog = state.catalogType != null,
                    // Reuse the existing catalog FoodItem ID if this form was populated
                    // from a catalog hit — prevents inserting a duplicate FoodItem.
                    existingFoodItemId = state.sourceCatalogFoodItemId,
                    mealType = state.mealType.value
                )
                _uiState.update { LogUiState(catalogType = state.catalogType) }
                triggerPrefetchInBackground()
                _events.emit(LogEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save food log"
                    )
                }
                _events.emit(LogEvent.SaveError(e.message ?: "Unknown error"))
            }
        }
    }

    // ── Manual recipe builder ──────────────────────────────────────────

    /**
     * Returns a Flow of ingredient catalog items matching [query].
     * Used by the UI to populate the ingredient search dropdown.
     * Delegates to [FoodRepository.searchFoodsByNameInCatalog] — no new query needed.
     */
    fun searchIngredientCatalog(query: String): Flow<List<FoodItem>> =
        foodRepository.searchFoodsByNameInCatalog(query, Constants.INGREDIENT_CATALOG_ID)

    /** Append a new empty ingredient row. */
    fun addManualIngredient() {
        _uiState.update { state ->
            state.copy(
                manualRecipeIngredients = state.manualRecipeIngredients + ManualRecipeIngredient()
            )
        }
    }

    /**
     * Remove the ingredient row with [id].
     * No-op when only one row remains — the list must always have at least one row.
     */
    fun removeManualIngredient(id: String) {
        _uiState.update { state ->
            if (state.manualRecipeIngredients.size <= 1) return@update state
            state.copy(
                manualRecipeIngredients = state.manualRecipeIngredients.filter { it.id != id }
            )
        }
    }

    /** Update the [quantity] for the ingredient with [id]. */
    fun updateManualIngredientQuantity(id: String, quantity: String) {
        _uiState.update { state ->
            state.copy(
                manualRecipeIngredients = state.manualRecipeIngredients.map { r ->
                    if (r.id == id) r.copy(quantity = quantity) else r
                }
            )
        }
    }

    /** Update the [unit] for the ingredient with [id]. */
    fun updateManualIngredientUnit(id: String, unit: String) {
        _uiState.update { state ->
            state.copy(
                manualRecipeIngredients = state.manualRecipeIngredients.map { r ->
                    if (r.id == id) r.copy(unit = unit) else r
                }
            )
        }
    }

    /** Update the custom name for the ingredient with [id] (typed by user, not from catalog). */
    fun updateManualIngredientName(id: String, name: String) {
        _uiState.update { state ->
            val updated = state.manualRecipeIngredients.map { r ->
                if (r.id == id) r.copy(customName = name, catalogItem = null) else r
            }
            // Auto-grow: if the last row now has a name, append a new empty row.
            val last = updated.last()
            val needsGrow = last.id == id && last.customName.isNotBlank()
            state.copy(
                manualRecipeIngredients = if (needsGrow) updated + ManualRecipeIngredient() else updated
            )
        }
    }

    /** Update a macro field (calories/protein/carbs/fat) for the ingredient with [id]. */
    fun updateManualIngredientMacro(id: String, field: String, value: String) {
        _uiState.update { state ->
            state.copy(
                manualRecipeIngredients = state.manualRecipeIngredients.map { r ->
                    if (r.id != id) return@map r
                    when (field) {
                        "calories" -> r.copy(calories = value)
                        "protein"  -> r.copy(protein = value)
                        "carbs"    -> r.copy(carbs = value)
                        "fat"      -> r.copy(fat = value)
                        else       -> r
                    }
                }
            )
        }
    }

    /**
     * Select a catalog [FoodItem] for the ingredient row with [id].
     * Pre-fills macros from catalog data. Auto-appends a new empty row when
     * this was the last row in the list.
     */
    fun selectCatalogItemForIngredient(id: String, foodItem: FoodItem) {
        _uiState.update { state ->
            val updated = state.manualRecipeIngredients.map { r ->
                if (r.id != id) return@map r
                r.copy(
                    catalogItem = foodItem,
                    customName = foodItem.name,
                    calories = foodItem.baseCalories.toString(),
                    protein = foodItem.baseProtein.toString(),
                    carbs = foodItem.baseCarbs.toString(),
                    fat = foodItem.baseFat.toString(),
                )
            }
            val wasLast = updated.last().id == id
            state.copy(
                manualRecipeIngredients = if (wasLast) updated + ManualRecipeIngredient() else updated
            )
        }
    }

    /**
     * Clear the catalog selection for the ingredient row with [id].
     * Keeps the custom name and macros so the user can edit them freely.
     */
    fun clearCatalogItemForIngredient(id: String) {
        _uiState.update { state ->
            state.copy(
                manualRecipeIngredients = state.manualRecipeIngredients.map { r ->
                    if (r.id == id) r.copy(catalogItem = null) else r
                }
            )
        }
    }

    fun updateRecipeServingQuantity(qty: String) {
        _uiState.update { it.copy(recipeServingQuantity = qty) }
    }

    fun updateRecipeServingUnit(unit: String) {
        _uiState.update { it.copy(recipeServingUnit = unit) }
    }

    /**
     * Save a manually-built recipe via [LogFoodUseCase.logRecipe].
     *
     * Pre-scales each ingredient's macros by [UnitConverter.computeServingMultiplier]
     * before building the [CatalogMatch] list. This ensures the Edge Function / logRecipe's
     * direct sum produces the correct per-serving recipe total.
     *
     * CRITICAL: base macros in FoodItem are per-100g. The pre-scaled value represents the
     * ingredient's actual contribution to 1 serving of the recipe — matching the contract
     * that the existing logRecipe() already uses for AI-parsed ingredients.
     */
    fun saveManualRecipe() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields") }
            return
        }

        // If isLoggingRecipe is set but no ingredients have been added,
        // the user filled in flat macros — fall back to saveLog() which
        // handles flat items correctly (Scenario 1 fix).
        val hasBuilderIngredients = state.manualRecipeIngredients.any { it.hasName }
        if (!hasBuilderIngredients) {
            saveLog()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val todayMillis = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val validIngredients = state.manualRecipeIngredients.filter {
                    it.hasName && it.hasQuantity
                }

                val ingredientMatches = validIngredients.map { r ->
                    val baseCal = r.catalogItem?.baseCalories ?: r.calories.toDoubleOrNull() ?: 0.0
                    val baseProt = r.catalogItem?.baseProtein ?: r.protein.toDoubleOrNull() ?: 0.0
                    val baseCarb = r.catalogItem?.baseCarbs ?: r.carbs.toDoubleOrNull() ?: 0.0
                    val baseFat = r.catalogItem?.baseFat ?: r.fat.toDoubleOrNull() ?: 0.0

                    val qty = r.quantity.toDoubleOrNull() ?: 0.0
                    val multiplier = UnitConverter.computeServingMultiplier(qty, r.unit)

                    // Pre-scaled: actual contribution of this ingredient to 1 serving of the recipe.
                    val scaledItem = FoodItem(
                        id = r.catalogItem?.id ?: UUID.randomUUID().toString(),
                        catalogId = Constants.INGREDIENT_CATALOG_ID,
                        name = r.catalogItem?.name ?: r.customName.trim(),
                        baseServingG = Constants.PER_100G_BASE,
                        baseCalories = baseCal * multiplier,
                        baseProtein = baseProt * multiplier,
                        baseCarbs = baseCarb * multiplier,
                        baseFat = baseFat * multiplier,
                        lastModifiedAt = System.currentTimeMillis()
                    )

                    CatalogMatch(
                        isFromCatalog = r.catalogItem != null,
                        parsedFood = com.app.nutriai.domain.model.ParsedFood(
                            name = scaledItem.name,
                            quantity = qty,
                            unit = r.unit,
                            confidence = 1.0,
                            isRecipe = false,
                            ingredients = emptyList()
                        ),
                        matchedFoodItem = scaledItem
                    )
                }

                val recipeQty = state.recipeServingQuantity.toDoubleOrNull() ?: 1.0

                logFoodUseCase.logRecipe(
                    recipeName = state.foodName,
                    ingredientMatches = ingredientMatches,
                    quantity = recipeQty,
                    unit = state.recipeServingUnit,
                    dateTimestamp = todayMillis,
                    skipDailyLog = state.catalogType != null,
                    mealType = state.mealType.value
                )

                _uiState.update { LogUiState(catalogType = state.catalogType) }
                triggerPrefetchInBackground()
                _events.emit(LogEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save recipe")
                }
                _events.emit(LogEvent.SaveError(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Clear the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }


}
