package com.app.nutriai.domain.model

/**
 * Domain model representing a food item with baseline nutritional data.
 * Macros are stored per [baseServingG] grams (the reference serving size).
 */
data class FoodItem(
    val id: String,
    val catalogId: String,
    val name: String,
    val brand: String? = null,
    val baseServingG: Double,
    val baseCalories: Double,
    val baseProtein: Double,
    val baseCarbs: Double,
    val baseFat: Double,
    val externalApiId: String? = null,
    val lastModifiedAt: Long,
    val deletedAt: Long? = null
)
