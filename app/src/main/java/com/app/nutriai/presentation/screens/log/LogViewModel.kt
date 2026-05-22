package com.app.nutriai.presentation.screens.log

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.domain.model.CatalogMatch
import com.app.nutriai.domain.model.IngredientKey
import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.domain.model.ParsedFood
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
    val foodName: String = "",
    val brand: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val quantity: String = "1",
    val unit: String = "g",
    val isUnitDropdownExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        /** Available unit options for the dropdown — kept in sync with webapp UNIT_OPTIONS */
        val UNIT_OPTIONS = listOf("g", "serving", "tsp", "tbsp", "cup", "ml", "piece", "slice", "bowl")
    }

    /**
     * Whether the form has enough data to save.
     * Requires food name and valid calorie value at minimum.
     */
    val isValid: Boolean
        get() = foodName.isNotBlank()
                && calories.toDoubleOrNull() != null
                && (calories.toDoubleOrNull() ?: 0.0) >= 0
                && (quantity.toDoubleOrNull() ?: 0.0) > 0

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
    lookupNutritionUseCase: LookupNutritionUseCase,
    extractLabelUseCase: ExtractLabelUseCase,
    imageCompressor: ImageCompressor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogEvent>()
    val events: SharedFlow<LogEvent> = _events.asSharedFlow()

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
     */
    fun setCatalogType(catalogType: String?) {
        _uiState.update { it.copy(catalogType = catalogType) }
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
                    ingredientNutritionLookups = emptyMap()
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

            _uiState.update {
                it.copy(
                    catalogMatches = matches,
                    ingredientCatalogMatches = ingredientMatches,
                    isResolvingCache = false
                )
            }

            // Phase 5: Kick off nutrition lookup after catalog resolution.
            // Skips items already found in the catalog (macros already available).
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
                // Per-UNIT multiplier: multiplier for exactly 1 unit (not all qty units).
                val perUnitMultiplier = UnitConverter.computeServingMultiplier(
                    quantity = 1.0,
                    unit = targetFood.unit,
                    servingWeightG = nutritionInfo.servingWeightG
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
                            skipDailyLog = isCatalogOnly
                        )
                    } else {
                        // Flat item: determine macros from cache → nutrition → 0
                        val catalogMatch = state.catalogMatches.getOrNull(index)
                        val cachedFood = catalogMatch?.matchedFoodItem
                        val isFromCatalog = catalogMatch?.isFromCatalog == true

                        val nutritionState = state.nutritionLookups[index]
                        val nutritionInfo = (nutritionState as? NutritionLookupState.Found)?.info

                        // Storage strategy — aligned with saveLog() so consumedQty always
                        // reflects the user's actual entered quantity ("2 tbsp", "1 cup"):
                        //
                        // • Catalog hit   → baseCalories is per-unit; raw qty as consumedQty
                        // • FDC + grams   → per-100g base + qty÷100 multiplier as consumedQty
                        //                   (same as saveLog() grams path)
                        // • FDC + non-gram→ per-UNIT base (per100g × 1-unit multiplier)
                        //                   + raw qty as consumedQty
                        //                   → "2 tbsp" stored as consumedQty=2, not 0.3
                        //
                        // Total calories are identical either way: (A × k) × (B ÷ k) = A × B.
                        // Benefit: HomeScreen displays "2 tbsp" not "0.3 tbsp"; edit UX is natural.
                        val isGramsUnit = UnitConverter.isGramsUnit(food.unit)

                        val logCalories: Double
                        val logProtein: Double
                        val logCarbs: Double
                        val logFat: Double
                        val effectiveQty: Double

                        when {
                            isFromCatalog -> {
                                // Catalog hit: baseCalories is per-100g for gram units,
                                // per-unit for all other units.
                                // For grams: store the multiplier (qty÷100) so that
                                //   totalCalories = baseCalories × consumedQty is correct,
                                //   and toDisplayQty recovers the original grams value.
                                // For non-gram: raw qty is already the natural unit count.
                                effectiveQty = if (isGramsUnit)
                                    UnitConverter.computeServingMultiplier(food.quantity, food.unit)
                                else
                                    food.quantity
                                logCalories  = cachedFood?.baseCalories ?: 0.0
                                logProtein   = cachedFood?.baseProtein  ?: 0.0
                                logCarbs     = cachedFood?.baseCarbs    ?: 0.0
                                logFat       = cachedFood?.baseFat      ?: 0.0
                            }
                            nutritionInfo != null && isGramsUnit -> {
                                // Grams + FDC: per-100g base + qty/100 multiplier
                                effectiveQty = UnitConverter.computeServingMultiplier(food.quantity, food.unit)
                                logCalories  = nutritionInfo.caloriesPer100g
                                logProtein   = nutritionInfo.proteinPer100g
                                logCarbs     = nutritionInfo.carbsPer100g
                                logFat       = nutritionInfo.fatPer100g
                            }
                            nutritionInfo != null -> {
                                // Non-gram + FDC: per-unit base + raw qty
                                val perUnitMult = UnitConverter.computeServingMultiplier(
                                    1.0, food.unit, nutritionInfo.servingWeightG
                                )
                                effectiveQty = food.quantity
                                logCalories  = nutritionInfo.caloriesPer100g * perUnitMult
                                logProtein   = nutritionInfo.proteinPer100g  * perUnitMult
                                logCarbs     = nutritionInfo.carbsPer100g    * perUnitMult
                                logFat       = nutritionInfo.fatPer100g      * perUnitMult
                            }
                            else -> {
                                effectiveQty = food.quantity
                                logCalories  = 0.0
                                logProtein   = 0.0
                                logCarbs     = 0.0
                                logFat       = 0.0
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
                            servingG = cachedFood?.baseServingG ?: Constants.PER_100G_BASE,
                            calories = logCalories,
                            protein  = logProtein,
                            carbs    = logCarbs,
                            fat      = logFat,
                            quantity = effectiveQty,
                            unit = food.unit,
                            dateTimestamp = todayMillis,
                            catalogId = flatCatalogId,
                            skipDailyLog = isCatalogOnly,
                            externalApiId = if (isFromCatalog) null else nutritionInfo?.externalId,
                            // Reuse the existing FoodItem ID for catalog hits to prevent duplicates.
                            existingFoodItemId = if (isFromCatalog) cachedFood?.id else null
                        )
                    }
                }

                val catalogType = _uiState.value.catalogType
                _uiState.update { LogUiState(catalogType = catalogType) }
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

    // -- Manual form methods (Phase 3 — preserved) --

    fun updateFoodName(name: String) {
        _uiState.update {
            it.copy(
                foodName = name,
                // If the user edits the food name, they're creating a custom item.
                // Clear the catalog link so we don't update an existing catalog entry.
                sourceCatalogFoodItemId = null,
                // Also clear recipe routing — name change means a fresh custom item.
                isLoggingRecipe = false
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

                // For grams: macros are per 100g, so 1 "serving" = 100g.
                // Normalise quantity to servings so LogFoodUseCase.totalCalories = calories × qty
                // gives the right daily total (e.g. 200g → effectiveQty = 2.0).
                val rawQty = state.quantity.toDouble()
                val effectiveQty = if (UnitConverter.isGramsUnit(state.unit)) rawQty / Constants.PER_100G_BASE else rawQty

                logFoodUseCase(
                    foodName = state.foodName,
                    brand = state.brand.ifBlank { null },
                    servingG = Constants.PER_100G_BASE,
                    calories = state.calories.toDouble(),
                    protein = state.protein.toDoubleOrNull() ?: 0.0,
                    carbs = state.carbs.toDoubleOrNull() ?: 0.0,
                    fat = state.fat.toDoubleOrNull() ?: 0.0,
                    quantity = effectiveQty,
                    unit = state.unit,
                    dateTimestamp = todayMillis,
                    catalogId = targetCatalogId,
                    skipDailyLog = state.catalogType != null,
                    // Reuse the existing catalog FoodItem ID if this form was populated
                    // from a catalog hit — prevents inserting a duplicate FoodItem.
                    existingFoodItemId = state.sourceCatalogFoodItemId
                )
                _uiState.update { LogUiState(catalogType = state.catalogType) }
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

    /**
     * Clear the error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }


}
