package com.app.nutriai.presentation.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.usecase.DeleteFoodUseCase
import com.app.nutriai.domain.usecase.GetCatalogsUseCase
import com.app.nutriai.domain.usecase.SearchCatalogUseCase
import com.app.nutriai.domain.usecase.UpdateFoodUseCase
import com.app.nutriai.util.ConnectivityObserver
import com.app.nutriai.util.Constants
import com.app.nutriai.util.formatMacro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Catalog tabs — Recipes and Ingredients.
 */
enum class CatalogTab(val label: String, val catalogId: String) {
    RECIPES("Recipes", Constants.RECIPE_CATALOG_ID),
    INGREDIENTS("Ingredients", Constants.INGREDIENT_CATALOG_ID)
}

/**
 * UI state for the Catalog screen.
 */
data class CatalogUiState(
    val isLoading: Boolean = true,
    val foodItems: List<FoodItem> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel for the Catalog screen with Recipes and Ingredients tabs.
 * Observes food items for the active tab, handles search queries scoped
 * to the active catalog, and supports delete actions.
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val getCatalogsUseCase: GetCatalogsUseCase,
    private val searchCatalogUseCase: SearchCatalogUseCase,
    private val updateFoodUseCase: UpdateFoodUseCase,
    private val deleteFoodUseCase: DeleteFoodUseCase,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    /** True when the device has a validated internet connection. */
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    private val _selectedTab = MutableStateFlow(CatalogTab.RECIPES)
    val selectedTab: StateFlow<CatalogTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Reactive UI state — switches data source based on active tab and search query.
     * When search is blank: shows all items for the active tab's catalog.
     * When search has text: filters items by name within the active tab's catalog.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CatalogUiState> = combine(
        _selectedTab,
        _searchQuery
    ) { tab, query -> Pair(tab, query) }
        .flatMapLatest { (tab, query) ->
            if (query.isBlank()) {
                getCatalogsUseCase(tab.catalogId)
            } else {
                searchCatalogUseCase(query, tab.catalogId)
            }.map { items ->
                CatalogUiState(
                    isLoading = false,
                    foodItems = items
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CatalogUiState()
        )

    /**
     * Switch the active catalog tab. Clears the search query.
     */
    fun selectTab(tab: CatalogTab) {
        _searchQuery.value = ""
        _selectedTab.value = tab
    }

    /**
     * Update the search query. The UI state will reactively
     * filter results within the active tab's catalog.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ID of the food item pending deletion confirmation.
    private val _pendingDeleteFoodId = MutableStateFlow<String?>(null)
    val pendingDeleteFoodId: StateFlow<String?> = _pendingDeleteFoodId.asStateFlow()

    /**
     * Stage a food item for deletion — shows the ConfirmDeleteDialog.
     */
    fun requestDeleteFood(foodId: String) {
        _pendingDeleteFoodId.value = foodId
    }

    /**
     * The user confirmed deletion — perform the soft-delete and hide the dialog.
     */
    fun confirmDeleteFood() {
        val foodId = _pendingDeleteFoodId.value ?: return
        _pendingDeleteFoodId.value = null
        viewModelScope.launch {
            try {
                deleteFoodUseCase(foodId)
            } catch (e: Exception) {
                // Non-critical — Room soft-delete failures are rare
            }
        }
    }

    /**
     * The user cancelled deletion — hide the dialog without deleting.
     */
    fun cancelDeleteFood() {
        _pendingDeleteFoodId.value = null
    }

    // ── Edit food bottom sheet ─────────────────────────────────────────

    private val _editSheet = MutableStateFlow<EditFoodSheet?>(null)
    val editSheet: StateFlow<EditFoodSheet?> = _editSheet.asStateFlow()

    /**
     * Open the edit bottom sheet for a catalog [FoodItem].
     * Pre-fills all editable fields from the selected item.
     */
    fun startEditFood(foodId: String) {
        val food = uiState.value.foodItems.find { it.id == foodId } ?: return
        _editSheet.value = EditFoodSheet(
            food = food,
            name = food.name,
            brand = food.brand ?: "",
            servingG = food.baseServingG.formatMacro(),
            calories = food.baseCalories.formatMacro(),
            protein = food.baseProtein.formatMacro(),
            carbs = food.baseCarbs.formatMacro(),
            fat = food.baseFat.formatMacro()
        )
    }

    fun updateEditName(value: String) { _editSheet.value = _editSheet.value?.copy(name = value, errorMessage = null) }
    fun updateEditBrand(value: String) { _editSheet.value = _editSheet.value?.copy(brand = value) }
    fun updateEditServingG(value: String) { _editSheet.value = _editSheet.value?.copy(servingG = value, errorMessage = null) }
    fun updateEditCalories(value: String) { _editSheet.value = _editSheet.value?.copy(calories = value) }
    fun updateEditProtein(value: String) { _editSheet.value = _editSheet.value?.copy(protein = value) }
    fun updateEditCarbs(value: String) { _editSheet.value = _editSheet.value?.copy(carbs = value) }
    fun updateEditFat(value: String) { _editSheet.value = _editSheet.value?.copy(fat = value) }

    fun cancelEdit() { _editSheet.value = null }

    /**
     * Save the edited [FoodItem] back to the catalog.
     * Validates name and serving size, then calls [FoodRepository.updateFood].
     */
    fun saveEditedFood() {
        val sheet = _editSheet.value ?: return
        if (sheet.name.isBlank()) {
            _editSheet.value = sheet.copy(errorMessage = "Name cannot be empty")
            return
        }
        val servingG = sheet.servingG.toDoubleOrNull()
        if (servingG == null || servingG <= 0) {
            _editSheet.value = sheet.copy(errorMessage = "Serving size must be a positive number")
            return
        }
        val calories = sheet.calories.toDoubleOrNull() ?: 0.0
        val protein = sheet.protein.toDoubleOrNull() ?: 0.0
        val carbs = sheet.carbs.toDoubleOrNull() ?: 0.0
        val fat = sheet.fat.toDoubleOrNull() ?: 0.0

        _editSheet.value = sheet.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val updated = sheet.food.copy(
                    name = sheet.name.trim(),
                    brand = sheet.brand.trim().ifBlank { null },
                    baseServingG = servingG,
                    baseCalories = calories,
                    baseProtein = protein,
                    baseCarbs = carbs,
                    baseFat = fat,
                    lastModifiedAt = System.currentTimeMillis()
                )
                updateFoodUseCase(updated)
                _editSheet.value = null
            } catch (e: Exception) {
                _editSheet.value = _editSheet.value?.copy(
                    isSaving = false,
                    errorMessage = "Failed to save changes. Please try again."
                )
            }
        }
    }
}

/**
 * State for the edit food catalog bottom sheet.
 */
data class EditFoodSheet(
    val food: FoodItem,
    val name: String,
    val brand: String,
    val servingG: String,
    val calories: String,
    val protein: String,
    val carbs: String,
    val fat: String,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)
