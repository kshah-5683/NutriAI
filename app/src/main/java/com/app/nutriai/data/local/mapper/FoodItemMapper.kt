package com.app.nutriai.data.local.mapper

import com.app.nutriai.data.local.entity.FoodItemEntity
import com.app.nutriai.domain.model.FoodItem

/**
 * Extension functions mapping between [FoodItemEntity] (data layer) and [FoodItem] (domain layer).
 */

fun FoodItemEntity.toDomain(): FoodItem = FoodItem(
    id = id,
    catalogId = catalogId,
    name = name,
    brand = brand,
    baseServingG = baseServingG,
    baseCalories = baseCalories,
    baseProtein = baseProtein,
    baseCarbs = baseCarbs,
    baseFat = baseFat,
    externalApiId = externalApiId,
    lastModifiedAt = lastModifiedAt,
    deletedAt = deletedAt
)

fun FoodItem.toEntity(isSynced: Boolean = false): FoodItemEntity = FoodItemEntity(
    id = id,
    catalogId = catalogId,
    name = name,
    brand = brand,
    baseServingG = baseServingG,
    baseCalories = baseCalories,
    baseProtein = baseProtein,
    baseCarbs = baseCarbs,
    baseFat = baseFat,
    externalApiId = externalApiId,
    lastModifiedAt = lastModifiedAt,
    isSynced = isSynced,
    deletedAt = deletedAt
)
