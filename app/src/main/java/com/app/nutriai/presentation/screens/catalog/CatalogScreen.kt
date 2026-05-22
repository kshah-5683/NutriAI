package com.app.nutriai.presentation.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.presentation.components.ConfirmDeleteDialog
import com.app.nutriai.presentation.components.OfflineBanner
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Catalog screen with Recipes and Ingredients tabs.
 * Each tab shows its respective catalog items with search and delete support.
 * FAB navigates to the Log screen to add a new item to the active tab's catalog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToAddItem: (catalogType: String) -> Unit = {},
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val editSheet by viewModel.editSheet.collectAsState()
    val pendingDeleteFoodId by viewModel.pendingDeleteFoodId.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CatalogScreenContent(
        uiState = uiState,
        isOnline = isOnline,
        searchQuery = searchQuery,
        selectedTab = selectedTab,
        onTabSelected = viewModel::selectTab,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onDeleteFood = viewModel::requestDeleteFood,
        onEditFood = viewModel::startEditFood,
        onAddItem = { onNavigateToAddItem(selectedTab.catalogId) }
    )

    // Delete confirmation dialog
    if (pendingDeleteFoodId != null) {
        val foodName = uiState.foodItems.find { it.id == pendingDeleteFoodId }?.name ?: "this item"
        ConfirmDeleteDialog(
            title = "Delete from Catalog",
            message = "Remove \"$foodName\" from your catalog? Existing log entries won't be affected.",
            onConfirm = viewModel::confirmDeleteFood,
            onDismiss = viewModel::cancelDeleteFood
        )
    }

    // Edit food bottom sheet
    if (editSheet != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelEdit,
            sheetState = sheetState
        ) {
            EditFoodSheetContent(
                sheet = editSheet!!,
                onNameChange = viewModel::updateEditName,
                onBrandChange = viewModel::updateEditBrand,
                onServingGChange = viewModel::updateEditServingG,
                onCaloriesChange = viewModel::updateEditCalories,
                onProteinChange = viewModel::updateEditProtein,
                onCarbsChange = viewModel::updateEditCarbs,
                onFatChange = viewModel::updateEditFat,
                onSave = viewModel::saveEditedFood,
                onCancel = viewModel::cancelEdit
            )
        }
    }
}

@Composable
private fun CatalogScreenContent(
    uiState: CatalogUiState,
    isOnline: Boolean = true,
    searchQuery: String,
    selectedTab: CatalogTab,
    onTabSelected: (CatalogTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteFood: (String) -> Unit,
    onEditFood: (String) -> Unit = {},
    onAddItem: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add ${selectedTab.label.dropLast(1)}"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline banner (animated — slides in when offline)
            OfflineBanner(isVisible = !isOnline)

            // Header
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Foods",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your saved recipes and ingredients",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs — Recipes | Ingredients
            val tabs = CatalogTab.entries
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (tab == CatalogTab.RECIPES) Icons.Default.Restaurant else Icons.Default.Egg,
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search ${selectedTab.label.lowercase()}...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.foodItems.isEmpty() -> {
                    EmptyCatalogState(
                        isSearching = searchQuery.isNotBlank(),
                        tab = selectedTab
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${uiState.foodItems.size} ${selectedTab.label.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(
                            items = uiState.foodItems,
                            key = { it.id }
                        ) { foodItem ->
                            CatalogFoodCard(
                                foodItem = foodItem,
                                isRecipe = selectedTab == CatalogTab.RECIPES,
                                onEdit = { onEditFood(foodItem.id) },
                                onDelete = { onDeleteFood(foodItem.id) }
                            )
                        }
                        // Bottom spacer for FAB clearance
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a single food item in the catalog.
 * Shows a restaurant icon for recipes and an egg icon for ingredients.
 */
@Composable
private fun CatalogFoodCard(
    foodItem: FoodItem,
    isRecipe: Boolean,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Icon(
                imageVector = if (isRecipe) Icons.Default.Restaurant else Icons.Default.Egg,
                contentDescription = if (isRecipe) "Recipe" else "Ingredient",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = foodItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (foodItem.brand != null) {
                    Text(
                        text = foodItem.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroLabel("${foodItem.baseCalories.roundToInt()} kcal", CalorieColor)
                    MacroLabel("P: ${foodItem.baseProtein.roundToInt()}g", ProteinColor)
                    MacroLabel("C: ${foodItem.baseCarbs.roundToInt()}g", CarbsColor)
                    MacroLabel("F: ${foodItem.baseFat.roundToInt()}g", FatColor)
                }
                Text(
                    text = "per ${foodItem.baseServingG.roundToInt()}g serving",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit food",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete food",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

/**
 * Empty state per tab — different messages for Recipes vs Ingredients.
 */
@Composable
private fun EmptyCatalogState(
    isSearching: Boolean,
    tab: CatalogTab
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (tab == CatalogTab.RECIPES) Icons.Default.Restaurant else Icons.Default.Egg,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.height(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                isSearching -> "No ${tab.label.lowercase()} found"
                tab == CatalogTab.RECIPES -> "No recipes yet"
                else -> "No ingredients yet"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                isSearching -> "Try a different search term"
                tab == CatalogTab.RECIPES -> "Recipes you log will appear here. Tap + to add one."
                else -> "Ingredients you log will appear here. Tap + to add one."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Content of the edit food item bottom sheet.
 * Allows the user to update a catalog [FoodItem]'s name, brand, serving size, and macros.
 */
@Composable
private fun EditFoodSheetContent(
    sheet: EditFoodSheet,
    onNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onServingGChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .imePadding()
    ) {
        Text(
            text = "Edit Food Item",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Name
        OutlinedTextField(
            value = sheet.name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = sheet.errorMessage != null && sheet.name.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Brand (optional)
        OutlinedTextField(
            value = sheet.brand,
            onValueChange = onBrandChange,
            label = { Text("Brand (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Serving size
        OutlinedTextField(
            value = sheet.servingG,
            onValueChange = onServingGChange,
            label = { Text("Serving size (g) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = sheet.errorMessage != null && sheet.servingG.toDoubleOrNull()?.let { it <= 0 } != false
        )

        if (sheet.errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sheet.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Macros — 2x2 grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sheet.calories,
                onValueChange = onCaloriesChange,
                label = { Text("Calories (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = sheet.protein,
                onValueChange = onProteinChange,
                label = { Text("Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sheet.carbs,
                onValueChange = onCarbsChange,
                label = { Text("Carbs (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = sheet.fat,
                onValueChange = onFatChange,
                label = { Text("Fat (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !sheet.isSaving
            ) { Text("Cancel") }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !sheet.isSaving
            ) {
                if (sheet.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogScreenRecipesPreview() {
    NutriAiTheme {
        CatalogScreenContent(
            uiState = CatalogUiState(
                isLoading = false,
                foodItems = listOf(
                    FoodItem(
                        id = "1",
                        catalogId = "recipes",
                        name = "Besan Chila",
                        brand = null,
                        baseServingG = 250.0,
                        baseCalories = 320.0,
                        baseProtein = 14.0,
                        baseCarbs = 38.0,
                        baseFat = 12.0,
                        lastModifiedAt = System.currentTimeMillis()
                    ),
                    FoodItem(
                        id = "2",
                        catalogId = "recipes",
                        name = "Oatmeal with Berries",
                        brand = null,
                        baseServingG = 300.0,
                        baseCalories = 280.0,
                        baseProtein = 8.0,
                        baseCarbs = 48.0,
                        baseFat = 6.0,
                        lastModifiedAt = System.currentTimeMillis()
                    )
                )
            ),
            searchQuery = "",
            selectedTab = CatalogTab.RECIPES,
            onTabSelected = {},
            onSearchQueryChange = {},
            onDeleteFood = {},
            onAddItem = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogScreenIngredientsPreview() {
    NutriAiTheme {
        CatalogScreenContent(
            uiState = CatalogUiState(
                isLoading = false,
                foodItems = listOf(
                    FoodItem(
                        id = "1",
                        catalogId = "ingredients",
                        name = "Chicken Breast",
                        brand = null,
                        baseServingG = 120.0,
                        baseCalories = 165.0,
                        baseProtein = 31.0,
                        baseCarbs = 0.0,
                        baseFat = 3.6,
                        lastModifiedAt = System.currentTimeMillis()
                    )
                )
            ),
            searchQuery = "",
            selectedTab = CatalogTab.INGREDIENTS,
            onTabSelected = {},
            onSearchQueryChange = {},
            onDeleteFood = {},
            onAddItem = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogScreenEmptyPreview() {
    NutriAiTheme {
        CatalogScreenContent(
            uiState = CatalogUiState(isLoading = false),
            searchQuery = "",
            selectedTab = CatalogTab.RECIPES,
            onTabSelected = {},
            onSearchQueryChange = {},
            onDeleteFood = {},
            onAddItem = {}
        )
    }
}
