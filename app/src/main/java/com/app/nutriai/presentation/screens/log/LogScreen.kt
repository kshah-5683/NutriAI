package com.app.nutriai.presentation.screens.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.nutriai.domain.model.CatalogMatch
import com.app.nutriai.domain.model.IngredientKey
import com.app.nutriai.domain.model.MealType
import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.presentation.components.MealTypeSelector
import com.app.nutriai.presentation.components.NutritionMatchCard
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.util.Constants
import com.app.nutriai.util.UnitConverter
import com.app.nutriai.util.formatMacro
import com.app.nutriai.util.formatQuantity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * Food logging screen with two input modes:
 *
 * 1. **AI Input** (default): User types a natural language food description
 *    (e.g., "2 eggs and a bowl of oatmeal"), taps "Parse with AI", reviews
 *    the extracted food entities, and accepts them into the form.
 *
 * 2. **Manual Input**: User fills in each field directly (name, serving, macros).
 *
 * Phase 3: Manual form fields.
 * Phase 4: AI-powered parsing via Gemini API with "Parse with AI" button.
 * Phase 5: USDA FDC lookup will auto-fill macros after AI parsing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    catalogType: String? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: LogViewModel = hiltViewModel()
) {
    // Set catalog type from navigation arguments (once)
    LaunchedEffect(catalogType) {
        viewModel.setCatalogType(catalogType)
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Determine if we're in catalog-only mode (navigated from Catalog screen)
    val isCatalogMode = uiState.catalogType != null
    val isRecipeCatalog = uiState.catalogType == Constants.RECIPE_CATALOG_ID

    // Dynamic labels based on navigation context
    val screenTitle = when {
        isRecipeCatalog -> "Add Recipe"
        isCatalogMode -> "Add Ingredient"
        else -> "Log Food"
    }
    val successMessage = if (isCatalogMode) "Saved to catalog!" else "Food logged successfully!"
    val saveButtonLabel = if (isCatalogMode) "Save to Catalog" else "Log Food"
    val saveAllLabel = if (isCatalogMode) "Save All" else "Log All"
    val headingText = if (isCatalogMode) "Add to your catalog" else "What did you eat?"
    val subheadingAi = if (isCatalogMode) {
        "Describe the food and let AI parse it"
    } else {
        "Describe your meal and let AI parse it"
    }
    val subheadingManual = if (isCatalogMode) {
        "Enter food details to save to your catalog"
    } else {
        "Enter food details manually below"
    }

    // Listen for one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LogEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar(successMessage)
                    onNavigateBack()
                }
                is LogEvent.SaveError -> {
                    snackbarHostState.showSnackbar("Error: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = headingText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (uiState.inputMode == LogInputMode.AI_INPUT) {
                    subheadingAi
                } else {
                    subheadingManual
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input mode tabs
            InputModeTabs(
                currentMode = uiState.inputMode,
                onModeSelected = { mode ->
                    when (mode) {
                        LogInputMode.AI_INPUT -> viewModel.switchToAiInput()
                        LogInputMode.MANUAL_INPUT -> viewModel.switchToManualInput()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on input mode
            when (uiState.inputMode) {
                LogInputMode.AI_INPUT -> {
                    AiInputSection(
                        uiState = uiState,
                        saveAllLabel = saveAllLabel,
                        isCatalogMode = isCatalogMode,
                        onAiInputChange = viewModel::updateAiInput,
                        onParseClick = viewModel::parseWithAi,
                        onSelectFood = viewModel::selectParsedFood,
                        onSelectIngredient = viewModel::selectIngredient,
                        onAcceptFood = viewModel::acceptParsedFood,
                        onAcceptAllAndLog = viewModel::acceptAndLogAllParsed,
                        onClearParsed = viewModel::clearParsedFoods,
                        onSwitchToManual = viewModel::switchToManualInput,
                        onToggleRecipeOverride = viewModel::toggleRecipeOverride,
                        onRemoveIngredient = viewModel::removeIngredient,
                        onMoveIngredientUp = viewModel::moveIngredientUp,
                        onMoveIngredientDown = viewModel::moveIngredientDown,
                        onEditIngredient = viewModel::beginEditIngredient,
                        onDismissEditIngredient = viewModel::dismissEditIngredient,
                        onConfirmEditIngredient = viewModel::confirmEditIngredient,
                        onUseGeneric = viewModel::resolveClarificationGeneric,
                        onSubmitClarification = { index, input ->
                            val weightRegex = Regex("""(\d+\.?\d*)\s*g?""")
                            val weightMatch = weightRegex.matchEntire(input.trim())
                            if (weightMatch != null) {
                                viewModel.resolveClarificationWithWeight(index, weightMatch.groupValues[1].toDouble())
                            } else {
                                viewModel.resolveClarificationWithBrand(index, input.trim())
                            }
                        },
                        onLabelPhotoSelected = { uri ->
                            viewModel.onLabelPhotoSelected(uri, "gallery")
                        },
                        onClearLabelError = viewModel::clearLabelExtraction
                    )
                }
                LogInputMode.MANUAL_INPUT -> {
                    // Phase 11: Show a banner when the form was prefilled from a label scan
                    if (uiState.labelPhotoId != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Macros prefilled from label scan — add food name and serving to save.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    // Meal type selector — shown when logging food (not adding to catalog)
                    if (!isCatalogMode) {
                        MealTypeSelector(
                            selected = uiState.mealType,
                            onSelect = viewModel::setMealType
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ManualInputSection(
                        uiState = uiState,
                        saveButtonLabel = saveButtonLabel,
                        showTypeSelector = uiState.catalogType == null,
                        onFoodNameChange = viewModel::updateFoodName,
                        onBrandChange = viewModel::updateBrand,
                        onQuantityChange = viewModel::updateQuantity,
                        onToggleUnitDropdown = viewModel::toggleUnitDropdown,
                        onDismissUnitDropdown = viewModel::dismissUnitDropdown,
                        onSelectUnit = viewModel::selectUnit,
                        onCaloriesChange = viewModel::updateCalories,
                        onProteinChange = viewModel::updateProtein,
                        onCarbsChange = viewModel::updateCarbs,
                        onFatChange = viewModel::updateFat,
                        onSaveClick = viewModel::saveLog,
                        onIsLoggingRecipeChange = viewModel::toggleIsLoggingRecipe,
                        // Recipe builder callbacks
                        onAddIngredient = viewModel::addManualIngredient,
                        onRemoveIngredient = viewModel::removeManualIngredient,
                        onUpdateIngredientName = viewModel::updateManualIngredientName,
                        onUpdateIngredientQuantity = viewModel::updateManualIngredientQuantity,
                        onUpdateIngredientUnit = viewModel::updateManualIngredientUnit,
                        onUpdateIngredientMacro = viewModel::updateManualIngredientMacro,
                        onSelectCatalogItem = viewModel::selectCatalogItemForIngredient,
                        onClearCatalogItem = viewModel::clearCatalogItemForIngredient,
                        onUpdateRecipeServingQuantity = viewModel::updateRecipeServingQuantity,
                        onUpdateRecipeServingUnit = viewModel::updateRecipeServingUnit,
                        onSaveRecipeClick = viewModel::saveManualRecipe,
                        searchCatalog = viewModel::searchIngredientCatalog
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Input Mode Tabs ────────────────────────────────────────────────

@Composable
private fun InputModeTabs(
    currentMode: LogInputMode,
    onModeSelected: (LogInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = if (currentMode == LogInputMode.AI_INPUT) 0 else 1,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Tab(
            selected = currentMode == LogInputMode.AI_INPUT,
            onClick = { onModeSelected(LogInputMode.AI_INPUT) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Parse")
                }
            }
        )
        Tab(
            selected = currentMode == LogInputMode.MANUAL_INPUT,
            onClick = { onModeSelected(LogInputMode.MANUAL_INPUT) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual")
                }
            }
        )
    }
}

// ─── AI Input Section ───────────────────────────────────────────────

@Composable
private fun AiInputSection(
    uiState: LogUiState,
    saveAllLabel: String = "Log All",
    isCatalogMode: Boolean = false,
    onAiInputChange: (String) -> Unit,
    onParseClick: () -> Unit,
    onSelectFood: (Int) -> Unit,
    onSelectIngredient: (Int?) -> Unit,
    onAcceptFood: () -> Unit,
    onAcceptAllAndLog: () -> Unit,
    onClearParsed: () -> Unit,
    onSwitchToManual: () -> Unit,
    onToggleRecipeOverride: (Int) -> Unit = {},
    onRemoveIngredient: (foodIndex: Int, ingredientIndex: Int) -> Unit = { _, _ -> },
    onMoveIngredientUp: (foodIndex: Int, ingredientIndex: Int) -> Unit = { _, _ -> },
    onMoveIngredientDown: (foodIndex: Int, ingredientIndex: Int) -> Unit = { _, _ -> },
    onEditIngredient: (foodIndex: Int, ingredientIndex: Int) -> Unit = { _, _ -> },
    onDismissEditIngredient: () -> Unit = {},
    onConfirmEditIngredient: (qty: String, unit: String) -> Unit = { _, _ -> },
    onUseGeneric: (Int) -> Unit = {},
    onSubmitClarification: (Int, String) -> Unit = { _, _ -> },
    onLabelPhotoSelected: (Uri) -> Unit = {},
    onClearLabelError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Phase 11: Photo picker launcher — opens Android's photo picker (no permission required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            focusManager.clearFocus()
            onLabelPhotoSelected(uri)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // AI text input field
        OutlinedTextField(
            value = uiState.aiInput,
            onValueChange = onAiInputChange,
            label = { Text("Describe your meal") },
            placeholder = { Text("e.g., 2 eggs, toast with butter, and a glass of milk") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            enabled = !uiState.isParsing && !uiState.isExtractingLabel,
            isError = uiState.aiErrorMessage != null,
            supportingText = if (uiState.aiErrorMessage != null) {
                { Text(uiState.aiErrorMessage, color = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onParseClick()
                }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Parse button row — "Parse with AI" and camera icon side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primary action: text AI parse
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onParseClick()
                },
                modifier = Modifier.weight(1f),
                enabled = uiState.aiInput.isNotBlank() && !uiState.isParsing && !uiState.isExtractingLabel
            ) {
                if (uiState.isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parsing...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parse with AI")
                }
            }

            // Phase 11: Camera / photo picker button
            // Shown as an outlined icon button so it's compact but discoverable
            OutlinedIconButton(
                onClick = {
                    focusManager.clearFocus()
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !uiState.isParsing && !uiState.isExtractingLabel,
                modifier = Modifier.size(48.dp)
            ) {
                if (uiState.isExtractingLabel) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Scan nutrition label",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Phase 11: Label extraction loading state
        AnimatedVisibility(
            visible = uiState.isExtractingLabel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reading nutrition label...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Phase 11: Label extraction error — shown when Gemma 4 couldn't read the label
        AnimatedVisibility(
            visible = uiState.labelExtractionError != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.labelExtractionError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearLabelError,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Parsed results section
        AnimatedVisibility(
            visible = uiState.hasParsedFoods,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                // Section header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI found ${uiState.parsedFoods.size} item${if (uiState.parsedFoods.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to select",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phase 4.5: Catalog resolution indicator
                // Phase 5: Nutrition lookup indicator
                val statusText = when {
                    uiState.isResolvingCache -> "Checking catalog..."
                    uiState.isLookingUpNutrition -> "Looking up nutrition..."
                    else -> null
                }
                if (statusText != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Parsed food items list (Phase 4.5: catalog match, Phase 5: nutrition lookup)
                uiState.parsedFoods.forEachIndexed { index, food ->
                    val catalogMatch = uiState.catalogMatches.getOrNull(index)
                    val ingredientMatches = uiState.ingredientCatalogMatches[index]
                    val isThisCardSelected = index == uiState.selectedParsedFoodIndex

                    // Phase 5: nutrition lookup states for this card
                    val nutritionState = uiState.nutritionLookups[index]
                    val ingredientNutritionStates = if (food.isRecipe) {
                        food.ingredients.indices.associate { ingIdx ->
                            ingIdx to uiState.ingredientNutritionLookups[IngredientKey(index, ingIdx)]
                        }.filterValues { it != null }
                            .mapValues { it.value!! }
                    } else emptyMap()

                    ParsedFoodCard(
                        food = food,
                        isSelected = isThisCardSelected,
                        catalogMatch = catalogMatch,
                        ingredientMatches = ingredientMatches,
                        selectedIngredientIndex = if (isThisCardSelected) uiState.selectedIngredientIndex else null,
                        nutritionState = nutritionState,
                        ingredientNutritionStates = ingredientNutritionStates,
                        isRecipeOverride = uiState.recipeOverrides.contains(index),
                        clarificationResolution = uiState.clarificationResolutions[index],
                        onUseGeneric = { onUseGeneric(index) },
                        onSubmitClarification = { input -> onSubmitClarification(index, input) },
                        onClick = { onSelectFood(index) },
                        onIngredientClick = { ingIdx -> onSelectIngredient(ingIdx) },
                        onToggleRecipeOverride = { onToggleRecipeOverride(index) },
                        onRemoveIngredient = { ingIdx -> onRemoveIngredient(index, ingIdx) },
                        onMoveIngredientUp = { ingIdx -> onMoveIngredientUp(index, ingIdx) },
                        onMoveIngredientDown = { ingIdx -> onMoveIngredientDown(index, ingIdx) },
                        onEditIngredient = { ingIdx -> onEditIngredient(index, ingIdx) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons for parsed foods
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Accept selected → fill manual form
                    // Shows contextual label: "Edit Ingredient" when one is selected in a recipe
                    val selectedFood = uiState.selectedParsedFood
                    val hasIngredientSelected = selectedFood?.isRecipe == true
                            && uiState.selectedIngredientIndex != null
                    OutlinedButton(
                        onClick = onAcceptFood,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasIngredientSelected) "Edit Ingredient" else "Edit Selected",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Log all parsed foods at once
                    Button(
                        onClick = onAcceptAllAndLog,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(saveAllLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clear / try again button
                OutlinedButton(
                    onClick = onClearParsed,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Clear & Try Again")
                }

                // Phase 5: Info card — describes macro auto-fill behaviour
                val anyNutritionFound = uiState.nutritionLookups.values.any {
                    it is NutritionLookupState.Found
                } || uiState.ingredientNutritionLookups.values.any {
                    it is NutritionLookupState.Found
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = when {
                            anyNutritionFound && isCatalogMode ->
                                "Nutrition data auto-filled from USDA FDC ✓ " +
                                        "Tap \"Edit Selected\" to review or adjust macros, " +
                                        "or \"Save All\" to save to your catalog."
                            anyNutritionFound ->
                                "Nutrition data auto-filled from USDA FDC ✓ " +
                                        "Tap \"Edit Selected\" to review or adjust macros, " +
                                        "or \"Log All\" to save with the auto-filled values."
                            isCatalogMode ->
                                "AI extracts food names and quantities. " +
                                        "Tap \"Edit Selected\" to add nutrition details, " +
                                        "or \"Save All\" to save to your catalog and fill in data later."
                            else ->
                                "AI extracts food names and quantities. " +
                                        "Tap \"Edit Selected\" to add nutrition details, " +
                                        "or \"Log All\" to save now and fill in data later."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Error state with fallback to manual
        if (uiState.aiErrorMessage != null && !uiState.hasParsedFoods) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSwitchToManual,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Enter Manually Instead")
            }
        }

        // Save error from batch log
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Inline ingredient edit dialog — driven by uiState.editingIngredient
        val editing = uiState.editingIngredient
        if (editing != null) {
            var localQty by remember(editing.foodIndex, editing.ingredientIndex) {
                mutableStateOf(editing.quantity)
            }
            var localUnit by remember(editing.foodIndex, editing.ingredientIndex) {
                mutableStateOf(editing.unit)
            }
            var unitDropdownExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismissEditIngredient,
                title = { Text("Edit: ${editing.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = localQty,
                            onValueChange = { localQty = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = localUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = {
                                    IconButton(onClick = { unitDropdownExpanded = !unitDropdownExpanded }) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Select unit"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false }
                            ) {
                                LogUiState.UNIT_OPTIONS.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            localUnit = option
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { onConfirmEditIngredient(localQty, localUnit) },
                        enabled = localQty.toDoubleOrNull()?.let { it > 0 } == true
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissEditIngredient) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ─── Parsed Food Card (Phase 4.5: Recipe + Cache support) ──────────

/**
 * Card displaying a parsed food item.
 * Phase 4.5: Handles both recipe items (with ingredient list) and flat items.
 * Shows catalog cache badges when ingredient matches are available.
 * Phase 5: Shows nutrition lookup status (loading, found, not-found) per item.
 */
@Composable
private fun ParsedFoodCard(
    food: ParsedFood,
    isSelected: Boolean,
    catalogMatch: CatalogMatch? = null,
    ingredientMatches: List<CatalogMatch>? = null,
    selectedIngredientIndex: Int? = null,
    nutritionState: NutritionLookupState? = null,
    ingredientNutritionStates: Map<Int, NutritionLookupState> = emptyMap(),
    isRecipeOverride: Boolean = false,
    clarificationResolution: ClarificationResolution? = null,
    onUseGeneric: () -> Unit = {},
    onSubmitClarification: (String) -> Unit = {},
    onClick: () -> Unit,
    onIngredientClick: (Int) -> Unit = {},
    onToggleRecipeOverride: () -> Unit = {},
    onRemoveIngredient: (Int) -> Unit = {},
    onMoveIngredientUp: (Int) -> Unit = {},
    onMoveIngredientDown: (Int) -> Unit = {},
    onEditIngredient: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Recipe icon for AI-detected recipes OR user-overridden flat items
                        if (food.isRecipe || isRecipeOverride) {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = "Recipe",
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = food.name,
                            style = if (food.isRecipe) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                            fontWeight = if (food.isRecipe) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildString {
                                append("${food.quantity.formatQuantity()} ${food.unit}")
                                if (food.isRecipe) {
                                    append(" • ${food.ingredients.size} ingredient${if (food.ingredients.size != 1) "s" else ""}")
                                }
                                if (food.confidence > 0) {
                                    append(" • ${(food.confidence * 100).roundToInt()}%")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        // Catalog cache badge for the item itself
                        if (catalogMatch?.isFromCatalog == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CatalogBadge()
                        }
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Phase 17: Clarification banner for variable serving-size items
            val needsClarification = food.needsClarification && catalogMatch?.isFromCatalog != true
            val showClarificationBanner = needsClarification && clarificationResolution == null

            if (showClarificationBanner) {
                Spacer(modifier = Modifier.height(6.dp))
                ClarificationBanner(
                    hint = food.clarificationHint ?: "Serving size varies by brand. Specify a brand or weight for better accuracy?",
                    onUseGeneric = onUseGeneric,
                    onSubmitClarification = onSubmitClarification,
                    isLoading = nutritionState is NutritionLookupState.Loading
                )
            }

            // Phase 5: Nutrition lookup status for flat items (not recipes)
            // Phase 17: Hide during active clarification banner (lookup hasn't started yet)
            if (!food.isRecipe && catalogMatch?.isFromCatalog != true && !showClarificationBanner) {
                NutritionStatusRow(
                    nutritionState = nutritionState,
                    isSelected = isSelected,
                    modifier = Modifier.padding(top = 6.dp)
                )

                // Phase 17: Match type badge after nutrition resolves
                val foundInfo = (nutritionState as? NutritionLookupState.Found)?.info
                if (foundInfo?.matchType != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MatchTypeBadge(
                        matchType = foundInfo.matchType,
                        brandNotFound = clarificationResolution?.type == ClarificationType.BRAND
                                && foundInfo.matchType == "generic"
                    )
                }
            }

            // Recipe override toggle — shown on flat items only (AI returned is_recipe=false).
            // Lets the user mark a dish as a recipe so it routes to the Recipes catalog.
            if (!food.isRecipe) {
                Spacer(modifier = Modifier.height(6.dp))
                FilterChip(
                    selected = isRecipeOverride,
                    onClick = onToggleRecipeOverride,
                    label = {
                        Text(
                            text = if (isRecipeOverride) "Recipe" else "Mark as Recipe",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Phase 4.5: Recipe ingredient list (indented, tappable for editing)
            if (food.isRecipe && food.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp)
                ) {
                    food.ingredients.forEachIndexed { ingIndex, ingredient ->
                        val ingMatch = ingredientMatches?.getOrNull(ingIndex)
                        val isIngSelected = isSelected && selectedIngredientIndex == ingIndex
                        val ingNutritionState = ingredientNutritionStates[ingIndex]
                        IngredientRow(
                            ingredient = ingredient,
                            isFromCatalog = ingMatch?.isFromCatalog == true,
                            isSelected = isSelected,
                            isIngredientSelected = isIngSelected,
                            nutritionState = ingNutritionState,
                            canMoveUp = ingIndex > 0,
                            canMoveDown = ingIndex < food.ingredients.lastIndex,
                            onClick = { onIngredientClick(ingIndex) },
                            onMoveUp = { onMoveIngredientUp(ingIndex) },
                            onMoveDown = { onMoveIngredientDown(ingIndex) },
                            onRemove = { onRemoveIngredient(ingIndex) },
                            onEdit = { onEditIngredient(ingIndex) }
                        )
                    }
                }
                // Hint for ingredient editing
                if (isSelected && selectedIngredientIndex == null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap an ingredient to edit its macros",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Phase 4.5: A single ingredient row inside a recipe card.
 * Shows name, quantity, unit, and a catalog badge if matched.
 * Tappable — when selected, "Edit Selected" will edit this specific ingredient.
 */
@Composable
private fun IngredientRow(
    ingredient: ParsedFood,
    isFromCatalog: Boolean,
    isSelected: Boolean,
    isIngredientSelected: Boolean = false,
    nutritionState: NutritionLookupState? = null,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onClick: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRemove: () -> Unit = {},
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rowBackground = if (isIngredientSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selected indicator or bullet
        if (isIngredientSelected) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Selected for editing",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "\u2022",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildString {
                append(ingredient.name)
                append(" (${ingredient.quantity.formatQuantity()} ${ingredient.unit})")
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isIngredientSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isIngredientSelected) {
                MaterialTheme.colorScheme.primary
            } else if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
        if (isFromCatalog) {
            Spacer(modifier = Modifier.width(4.dp))
            CatalogBadge(compact = true)
        } else {
            // Phase 5: show nutrition lookup mini-badge when not from catalog
            when (nutritionState) {
                is NutritionLookupState.Loading -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is NutritionLookupState.Found -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Nutrition found",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Edit, reorder and remove action buttons
        Spacer(modifier = Modifier.width(2.dp))
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit quantity / unit",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Move up",
                modifier = Modifier.size(14.dp),
                tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        }
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Move down",
                modifier = Modifier.size(14.dp),
                tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove ingredient",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Phase 4.5: Small badge indicating an ingredient was found in the local catalog.
 */
@Composable
private fun CatalogBadge(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Found in catalog",
            modifier = Modifier.size(if (compact) 10.dp else 12.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        if (!compact) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "In catalog",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─── Phase 5: Nutrition Status Row ──────────────────────────────────

/**
 * Shows the nutrition lookup status for a flat parsed food item.
 *
 * - [NutritionLookupState.Loading] → small spinner + "Looking up nutrition..."
 * - [NutritionLookupState.Found] → green check + product name + "USDA FDC" source
 * - [NutritionLookupState.NotFound] → muted "No nutrition data found"
 * - [NutritionLookupState.Error] → muted "Nutrition lookup failed"
 * - null → nothing rendered (lookup not yet started or item is from catalog)
 */
@Composable
private fun NutritionStatusRow(
    nutritionState: NutritionLookupState?,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    when (nutritionState) {
        is NutritionLookupState.Loading -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Looking up nutrition...",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is NutritionLookupState.Found -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Nutrition found",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buildString {
                        append("Nutrition found")
                        val brand = nutritionState.info.brand
                        if (!brand.isNullOrBlank()) append(" · $brand")
                        append(" · ${nutritionState.info.caloriesPer100g.toInt()} kcal/100g")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        is NutritionLookupState.NotFound -> {
            Text(
                text = "No nutrition data found — fill in manually",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline,
                modifier = modifier
            )
        }

        is NutritionLookupState.Error -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    // Show the actual error message so we can diagnose the root cause
                    text = nutritionState.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        null -> { /* Not started yet or from catalog — show nothing */ }
    }
}

// ─── Clarification Banner (Phase 17) ────────────────────────────────

/**
 * Banner shown inside ParsedFoodCard when AI flags variable serving size.
 * Allows user to accept generic estimate or provide brand/weight for better accuracy.
 */
@Composable
private fun ClarificationBanner(
    hint: String,
    onUseGeneric: () -> Unit,
    onSubmitClarification: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Warning hint text
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "\u26A0\uFE0F",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Brand name or weight (e.g. 40g)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                enabled = !isLoading,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (inputText.isNotBlank()) onSubmitClarification(inputText.trim())
                    }
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUseGeneric,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text("Use generic", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) onSubmitClarification(inputText.trim())
                    },
                    enabled = !isLoading && inputText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        if (isLoading) "Looking up..." else "Update & Lookup",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Badge showing the quality of the nutrition match after lookup.
 */
@Composable
private fun MatchTypeBadge(
    matchType: String?,
    brandNotFound: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, contentColor) = when {
        matchType == "branded" -> Triple(
            "\uD83D\uDFE2 Exact brand match",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        matchType == "generic" && brandNotFound -> Triple(
            "\uD83D\uDFE1 Brand not found, using generic",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        matchType == "generic" -> Triple(
            "\uD83D\uDFE1 Generic estimate",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> return // Don't show anything for null matchType
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ─── Manual Input Section (Phase 3 — preserved) ────────────────────

@Composable
private fun ManualInputSection(
    uiState: LogUiState,
    saveButtonLabel: String = "Log Food",
    showTypeSelector: Boolean = true,
    onFoodNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onToggleUnitDropdown: () -> Unit,
    onDismissUnitDropdown: () -> Unit,
    onSelectUnit: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onIsLoggingRecipeChange: (Boolean) -> Unit = {},
    // Recipe builder callbacks
    onAddIngredient: () -> Unit = {},
    onRemoveIngredient: (String) -> Unit = {},
    onUpdateIngredientName: (String, String) -> Unit = { _, _ -> },
    onUpdateIngredientQuantity: (String, String) -> Unit = { _, _ -> },
    onUpdateIngredientUnit: (String, String) -> Unit = { _, _ -> },
    onUpdateIngredientMacro: (String, String, String) -> Unit = { _, _, _ -> },
    onSelectCatalogItem: (String, FoodItem) -> Unit = { _, _ -> },
    onClearCatalogItem: (String) -> Unit = {},
    onUpdateRecipeServingQuantity: (String) -> Unit = {},
    onUpdateRecipeServingUnit: (String) -> Unit = {},
    onSaveRecipeClick: () -> Unit = {},
    searchCatalog: (String) -> Flow<List<FoodItem>> = { kotlinx.coroutines.flow.flowOf(emptyList()) },
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // Recipe / Ingredient selector — only shown when not navigated from a catalog tab
        if (showTypeSelector) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !uiState.isLoggingRecipe,
                    onClick = { onIsLoggingRecipeChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = !uiState.isLoggingRecipe) {
                            Icon(
                                imageVector = Icons.Filled.Egg,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    }
                ) { Text("Ingredient") }

                SegmentedButton(
                    selected = uiState.isLoggingRecipe,
                    onClick = { onIsLoggingRecipeChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = uiState.isLoggingRecipe) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    }
                ) { Text("Recipe") }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Food name
        OutlinedTextField(
            value = uiState.foodName,
            onValueChange = onFoodNameChange,
            label = { Text(if (uiState.isLoggingRecipe) "Recipe name *" else "Food name *") },
            placeholder = { Text(if (uiState.isLoggingRecipe) "e.g., Besan Chila" else "e.g., Oatmeal with honey") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.errorMessage != null && uiState.foodName.isBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Brand (optional)
        OutlinedTextField(
            value = uiState.brand,
            onValueChange = onBrandChange,
            label = { Text("Brand (optional)") },
            placeholder = { Text("e.g., Quaker") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoggingRecipe) {
            // ── Recipe builder mode ──────────────────────────────────────────
            ManualRecipeIngredientsSection(
                uiState = uiState,
                searchCatalog = searchCatalog,
                onAddIngredient = onAddIngredient,
                onRemoveIngredient = onRemoveIngredient,
                onUpdateIngredientName = onUpdateIngredientName,
                onUpdateIngredientQuantity = onUpdateIngredientQuantity,
                onUpdateIngredientUnit = onUpdateIngredientUnit,
                onUpdateIngredientMacro = onUpdateIngredientMacro,
                onSelectCatalogItem = onSelectCatalogItem,
                onClearCatalogItem = onClearCatalogItem,
                onUpdateRecipeServingQuantity = onUpdateRecipeServingQuantity,
                onUpdateRecipeServingUnit = onUpdateRecipeServingUnit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aggregated macro preview (totals for the stated number of servings)
            val recipeCalories = uiState.manualRecipeIngredients.sumOf { r ->
                val q = r.quantity.toDoubleOrNull() ?: 0.0
                val m = UnitConverter.computeServingMultiplier(q, r.unit)
                (r.catalogItem?.baseCalories ?: r.calories.toDoubleOrNull() ?: 0.0) * m
            }
            val recipeProtein = uiState.manualRecipeIngredients.sumOf { r ->
                val q = r.quantity.toDoubleOrNull() ?: 0.0
                val m = UnitConverter.computeServingMultiplier(q, r.unit)
                (r.catalogItem?.baseProtein ?: r.protein.toDoubleOrNull() ?: 0.0) * m
            }
            val recipeCarbs = uiState.manualRecipeIngredients.sumOf { r ->
                val q = r.quantity.toDoubleOrNull() ?: 0.0
                val m = UnitConverter.computeServingMultiplier(q, r.unit)
                (r.catalogItem?.baseCarbs ?: r.carbs.toDoubleOrNull() ?: 0.0) * m
            }
            val recipeFat = uiState.manualRecipeIngredients.sumOf { r ->
                val q = r.quantity.toDoubleOrNull() ?: 0.0
                val m = UnitConverter.computeServingMultiplier(q, r.unit)
                (r.catalogItem?.baseFat ?: r.fat.toDoubleOrNull() ?: 0.0) * m
            }
            val servingQty = uiState.recipeServingQuantity.toDoubleOrNull() ?: 1.0

            if (recipeCalories > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total (${servingQty.formatQuantity()} × ${uiState.recipeServingUnit})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroPreviewItem("Cal", "${(recipeCalories * servingQty).roundToInt()}", CalorieColor)
                            MacroPreviewItem("Protein", "${(recipeProtein * servingQty).roundToInt()}g", ProteinColor)
                            MacroPreviewItem("Carbs", "${(recipeCarbs * servingQty).roundToInt()}g", CarbsColor)
                            MacroPreviewItem("Fat", "${(recipeFat * servingQty).roundToInt()}g", FatColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Save button
            Button(
                onClick = onSaveRecipeClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isValid && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(saveButtonLabel)
                }
            }

        } else {
            // ── Flat ingredient mode ─────────────────────────────────────────

            // Quantity and Unit row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Quantity *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Unit dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = {},
                        label = { Text("Unit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = onToggleUnitDropdown) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select unit"
                                )
                            }
                        }
                    )
                    // Invisible clickable overlay to open dropdown on tap anywhere
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClick = onToggleUnitDropdown)
                    )
                    DropdownMenu(
                        expanded = uiState.isUnitDropdownExpanded,
                        onDismissRequest = onDismissUnitDropdown
                    ) {
                        LogUiState.UNIT_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { onSelectUnit(option) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Macro input section header — label reflects the unit so the user knows
            // what base the numbers should be entered against.
            Text(
                text = when (uiState.unit) {
                    "g" -> "Nutrition (per 100g)"
                    else    -> "Nutrition (per ${uiState.unit})"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Macros row 1: Calories
            OutlinedTextField(
                value = uiState.calories,
                onValueChange = onCaloriesChange,
                label = { Text("Calories (kcal) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.errorMessage != null && uiState.calories.isBlank()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Macros row 2: Protein, Carbs, Fat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.protein,
                    onValueChange = onProteinChange,
                    label = { Text("Protein (g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = uiState.carbs,
                    onValueChange = onCarbsChange,
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = uiState.fat,
                    onValueChange = onFatChange,
                    label = { Text("Fat (g)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Macro preview card
            if (uiState.calories.isNotBlank()) {
                MacroPreviewCard(uiState = uiState)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Save button
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isValid && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(saveButtonLabel)
                }
            }
        }
    }
}

// ─── Macro Preview Card (Phase 3 — preserved) ──────────────────────

/**
 * Preview card showing computed macro totals based on quantity x per-serving values.
 */
@Composable
private fun MacroPreviewCard(
    uiState: LogUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val fmtQty = uiState.quantity.toDoubleOrNull()?.formatQuantity() ?: "1"
            Text(
                text = when (uiState.unit) {
                    "g" -> "Total (${fmtQty}g)"
                    else    -> "Total ($fmtQty × ${uiState.unit})"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroPreviewItem("Cal", "${uiState.previewCalories.roundToInt()}", CalorieColor)
                MacroPreviewItem("Protein", "${uiState.previewProtein.roundToInt()}g", ProteinColor)
                MacroPreviewItem("Carbs", "${uiState.previewCarbs.roundToInt()}g", CarbsColor)
                MacroPreviewItem("Fat", "${uiState.previewFat.roundToInt()}g", FatColor)
            }
        }
    }
}

@Composable
private fun MacroPreviewItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Manual Recipe Builder ──────────────────────────────────────────

/** Unit options shown in the per-ingredient unit dropdown. */
private val INGREDIENT_UNIT_OPTIONS = listOf(
    "g", "ml", "serving", "tsp", "tbsp", "cup", "piece", "slice", "bowl"
)

/**
 * The full ingredient list section in the manual recipe builder.
 *
 * Shows:
 * - Section header "Ingredients (defines 1 serving)"
 * - One [ManualRecipeIngredientRow] per ingredient in [LogUiState.manualRecipeIngredients]
 * - "+ Add Ingredient" button
 * - "How many servings did you eat?" quantity + unit row
 */
@Composable
private fun ManualRecipeIngredientsSection(
    uiState: LogUiState,
    searchCatalog: (String) -> Flow<List<FoodItem>>,
    onAddIngredient: () -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onUpdateIngredientName: (String, String) -> Unit,
    onUpdateIngredientQuantity: (String, String) -> Unit,
    onUpdateIngredientUnit: (String, String) -> Unit,
    onUpdateIngredientMacro: (String, String, String) -> Unit,
    onSelectCatalogItem: (String, FoodItem) -> Unit,
    onClearCatalogItem: (String) -> Unit,
    onUpdateRecipeServingQuantity: (String) -> Unit,
    onUpdateRecipeServingUnit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var recipeServingUnitExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section header
        Text(
            text = "Ingredients (defines 1 serving)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Ingredient rows
        uiState.manualRecipeIngredients.forEach { ingredient ->
            ManualRecipeIngredientRow(
                ingredient = ingredient,
                isOnly = uiState.manualRecipeIngredients.size == 1,
                onUpdateName = { onUpdateIngredientName(ingredient.id, it) },
                onUpdateQuantity = { onUpdateIngredientQuantity(ingredient.id, it) },
                onUpdateUnit = { onUpdateIngredientUnit(ingredient.id, it) },
                onUpdateMacro = { field, value -> onUpdateIngredientMacro(ingredient.id, field, value) },
                onSelectCatalogItem = { onSelectCatalogItem(ingredient.id, it) },
                onClearCatalogItem = { onClearCatalogItem(ingredient.id) },
                onRemove = { onRemoveIngredient(ingredient.id) },
                searchCatalog = searchCatalog
            )
        }

        // + Add Ingredient button
        OutlinedButton(
            onClick = onAddIngredient,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("+ Add Ingredient")
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Recipe-level serving row — "how many servings did you eat?"
        Text(
            text = "How many servings did you eat?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.recipeServingQuantity,
                onValueChange = onUpdateRecipeServingQuantity,
                label = { Text("Servings *") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = uiState.recipeServingUnit,
                    onValueChange = {},
                    label = { Text("Unit") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { recipeServingUnitExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select unit"
                            )
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { recipeServingUnitExpanded = true }
                )
                DropdownMenu(
                    expanded = recipeServingUnitExpanded,
                    onDismissRequest = { recipeServingUnitExpanded = false }
                ) {
                    LogUiState.UNIT_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onUpdateRecipeServingUnit(option)
                                recipeServingUnitExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single ingredient row in the manual recipe builder.
 *
 * Shows:
 * - Ingredient search input (with live catalog dropdown) OR a catalog chip when an item is selected
 * - Quantity input + unit dropdown
 * - Remove button (hidden when only one row remains)
 * - Inline macro input fields for custom (non-catalog) ingredients that have a name
 * - Per-row scaled macro badge showing the ingredient's contribution to 1 recipe serving
 */
@Composable
private fun ManualRecipeIngredientRow(
    ingredient: ManualRecipeIngredient,
    isOnly: Boolean,
    onUpdateName: (String) -> Unit,
    onUpdateQuantity: (String) -> Unit,
    onUpdateUnit: (String) -> Unit,
    onUpdateMacro: (field: String, value: String) -> Unit,
    onSelectCatalogItem: (FoodItem) -> Unit,
    onClearCatalogItem: () -> Unit,
    onRemove: () -> Unit,
    searchCatalog: (String) -> Flow<List<FoodItem>>,
    modifier: Modifier = Modifier
) {
    // Local search state — separate from the stored customName so we can show
    // a clear empty input after clearing a catalog selection.
    var searchText by remember(ingredient.id) {
        mutableStateOf(if (ingredient.catalogItem != null) "" else ingredient.customName)
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var catalogResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var debouncedQuery by remember { mutableStateOf("") }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // 300 ms debounce on search input
    LaunchedEffect(searchText) {
        delay(300)
        debouncedQuery = searchText
    }

    // Collect catalog results reactively — re-runs when debouncedQuery changes
    LaunchedEffect(debouncedQuery) {
        searchCatalog(debouncedQuery).collect { results ->
            catalogResults = results
        }
    }

    // Pre-compute scaled macros for the per-row badge
    val qty = ingredient.quantity.toDoubleOrNull() ?: 0.0
    val multiplier = UnitConverter.computeServingMultiplier(qty, ingredient.unit)
    val baseCal = ingredient.catalogItem?.baseCalories ?: ingredient.calories.toDoubleOrNull() ?: 0.0
    val baseProt = ingredient.catalogItem?.baseProtein ?: ingredient.protein.toDoubleOrNull() ?: 0.0
    val baseCarb = ingredient.catalogItem?.baseCarbs ?: ingredient.carbs.toDoubleOrNull() ?: 0.0
    val baseFat = ingredient.catalogItem?.baseFat ?: ingredient.fat.toDoubleOrNull() ?: 0.0
    val scaledCal = Math.round(baseCal * multiplier * 10) / 10.0
    val scaledProt = Math.round(baseProt * multiplier * 10) / 10.0
    val scaledCarb = Math.round(baseCarb * multiplier * 10) / 10.0
    val scaledFat = Math.round(baseFat * multiplier * 10) / 10.0
    val showBadge = ingredient.hasName && qty > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Row 1: Name  |  [×] ─────────────────────────────────────────
            // Ingredient name takes full available width; remove button is pinned to end.
            // Qty and Unit live in their own row below so the name field is never squeezed.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ingredient name — catalog chip when selected, search field otherwise
                Box(modifier = Modifier.weight(1f)) {
                    if (ingredient.catalogItem != null) {
                        // Catalog chip: green dot + name + clear button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ingredient.catalogItem.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    onClearCatalogItem()
                                    searchText = ""
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear selection",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Search input with catalog dropdown
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { v ->
                                searchText = v
                                onUpdateName(v)
                                if (v.isNotEmpty()) dropdownExpanded = true
                            },
                            label = { Text("Ingredient") },
                            placeholder = { Text("Search or add…") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        // Catalog dropdown
                        DropdownMenu(
                            expanded = dropdownExpanded &&
                                    (catalogResults.isNotEmpty() || searchText.isNotBlank()),
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            // "Add as new custom ingredient" — always shown when text is typed
                            if (searchText.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Add \"${searchText.trim()}\" as new",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onUpdateName(searchText.trim())
                                        dropdownExpanded = false
                                    }
                                )
                            }

                            // Catalog matches
                            if (catalogResults.isNotEmpty()) {
                                if (searchText.isNotBlank()) {
                                    androidx.compose.material3.HorizontalDivider()
                                }
                                catalogResults.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.baseCalories.toInt()} kcal/100g",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectCatalogItem(item)
                                            searchText = item.name
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            } else if (searchText.isBlank()) {
                                // Empty state — no text typed yet
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Type to search your ingredient catalog",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = { dropdownExpanded = false },
                                    enabled = false
                                )
                            }
                        }
                    }
                }

                // Remove button — pinned to end of name row
                if (!isOnly) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove ingredient",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── Row 2: Qty  |  Unit ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity input
                OutlinedTextField(
                    value = ingredient.quantity,
                    onValueChange = onUpdateQuantity,
                    label = { Text("Qty") },
                    modifier = Modifier.width(96.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                // Unit dropdown — takes remaining row width after Qty field
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = ingredient.unit,
                        onValueChange = {},
                        label = { Text("Unit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { unitDropdownExpanded = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select unit"
                                )
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { unitDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = unitDropdownExpanded,
                        onDismissRequest = { unitDropdownExpanded = false }
                    ) {
                        INGREDIENT_UNIT_OPTIONS.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    onUpdateUnit(u)
                                    unitDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

            }

            // ── Custom macro inputs ──────────────────────────────────────────
            // Shown only for custom (non-catalog) ingredients once a name has been entered.
            if (ingredient.catalogItem == null && ingredient.hasName) {
                val macroLabel = when {
                    UnitConverter.isGramsUnit(ingredient.unit) -> "Nutrition per 100g"
                    ingredient.unit.trim().lowercase() in listOf(
                        "piece", "pieces", "slice", "slices", "bowl", "bowls"
                    ) -> "Nutrition per ${ingredient.unit}"
                    else -> "Nutrition per serving"
                }
                Text(
                    text = macroLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("calories", "Cal (kcal)", ingredient.calories),
                        Triple("protein", "P (g)", ingredient.protein),
                        Triple("carbs", "C (g)", ingredient.carbs),
                        Triple("fat", "F (g)", ingredient.fat),
                    ).forEach { (field, label, value) ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = { onUpdateMacro(field, it) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }
            }

            // ── Per-row scaled macro badge ───────────────────────────────────
            if (showBadge) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${qty.formatQuantity()}${ingredient.unit}:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IngredientMacroBadge(value = scaledCal, label = "kcal", color = CalorieColor)
                    IngredientMacroBadge(value = scaledProt, label = "P", color = ProteinColor, unit = "g")
                    IngredientMacroBadge(value = scaledCarb, label = "C", color = CarbsColor, unit = "g")
                    IngredientMacroBadge(value = scaledFat, label = "F", color = FatColor, unit = "g")
                }
            }
        }
    }
}

/**
 * Compact inline macro value + label badge used in [ManualRecipeIngredientRow].
 * Renders a coloured dot, the formatted value (with optional unit), and the label.
 */
@Composable
private fun IngredientMacroBadge(
    value: Double,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, shape = CircleShape)
        )
        Text(
            text = "${value.formatMacro()}$unit",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Preview ────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun LogScreenPreview() {
    NutriAiTheme {
        // Static preview without ViewModel
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Log Food") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                Text("Log Food Preview", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
