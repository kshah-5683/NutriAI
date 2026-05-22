package com.app.nutriai.data.repository

import com.app.nutriai.data.local.dao.DailyLogDao
import com.app.nutriai.data.local.dao.FoodItemDao
import com.app.nutriai.data.local.mapper.toDomain
import com.app.nutriai.data.local.mapper.toEntity
import com.app.nutriai.data.sync.SyncEntityType
import com.app.nutriai.data.sync.SyncPushManager
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [FoodRepository] backed by Room via [FoodItemDao].
 * Injected by Hilt as a singleton.
 *
 * Search uses LIKE-based partial matching on food name.
 * Every mutation schedules a debounced push-on-write via [SyncPushManager].
 *
 * ## Macro consistency
 * [food_items] is the source of truth for nutritional data. [daily_logs] stores
 * derived totals (base_macro × consumed_qty). Whenever a food
 * item's macros are updated via [updateFood], all non-deleted daily logs referencing
 * that food are immediately recalculated to stay consistent. The recalculated logs
 * are marked unsynced and pushed on the next debounced sync.
 *
 * The reverse direction (editing a daily log's total directly) is NOT cascaded back
 * to the food item — daily logs are derived values, not the macro source.
 */
@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val dailyLogDao: DailyLogDao,
    private val syncPushManager: SyncPushManager
) : FoodRepository {

    override fun getFoodsByCatalogId(catalogId: String): Flow<List<FoodItem>> {
        return foodItemDao.getFoodsByCatalogId(catalogId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchFoodsByName(query: String): Flow<List<FoodItem>> {
        return foodItemDao.searchFoodsByName(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchFoodsByNameInCatalog(query: String, catalogId: String): Flow<List<FoodItem>> {
        return foodItemDao.searchFoodsByNameInCatalog(query, catalogId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFoodById(id: String): FoodItem? {
        return foodItemDao.getFoodById(id)?.toDomain()
    }

    override suspend fun insertFood(food: FoodItem) {
        foodItemDao.insertFood(food.toEntity())
        syncPushManager.schedulePush(SyncEntityType.FOOD_ITEM, listOf(food.id))
    }

    override suspend fun updateFood(food: FoodItem) {
        foodItemDao.updateFood(food.toEntity())
        // Cascade: recalculate totals for all daily logs that reference this food item.
        // The recalculated logs are marked is_synced=0 so they are pushed with the food item.
        dailyLogDao.recalculateLogsForFoodItem(
            foodItemId   = food.id,
            baseCalories = food.baseCalories,
            baseProtein  = food.baseProtein,
            baseCarbs    = food.baseCarbs,
            baseFat      = food.baseFat
        )
        // Single push call covers both the updated food item and all recalculated logs,
        // since pushLocalChanges() picks up everything with is_synced=0.
        syncPushManager.schedulePush(SyncEntityType.FOOD_ITEM, listOf(food.id))
    }

    override suspend fun softDeleteFood(id: String) {
        foodItemDao.softDeleteFood(id)
        syncPushManager.schedulePush(SyncEntityType.FOOD_ITEM, listOf(id))
    }

    override suspend fun searchFoodByNameExact(name: String, catalogId: String): FoodItem? {
        return foodItemDao.searchFoodByNameExact(name, catalogId)?.toDomain()
    }

    override suspend fun getAllFoodNames(catalogId: String): List<String> {
        return foodItemDao.getAllFoodNames(catalogId)
    }
}
