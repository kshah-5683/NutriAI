package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a food item with baseline nutritional data.
 * Maps to the "food_items" table.
 *
 * Foreign key to [CatalogEntity] ensures referential integrity.
 * Macros are stored per [baseServingG] grams (the reference serving size).
 * Soft-delete support via [deletedAt].
 */
@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = CatalogEntity::class,
            parentColumns = ["id"],
            childColumns = ["catalog_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["catalog_id"])]
)
data class FoodItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "catalog_id")
    val catalogId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "brand")
    val brand: String? = null,

    @ColumnInfo(name = "base_serving_g")
    val baseServingG: Double,

    @ColumnInfo(name = "base_calories")
    val baseCalories: Double,

    @ColumnInfo(name = "base_protein")
    val baseProtein: Double,

    @ColumnInfo(name = "base_carbs")
    val baseCarbs: Double,

    @ColumnInfo(name = "base_fat")
    val baseFat: Double,

    @ColumnInfo(name = "external_api_id")
    val externalApiId: String? = null,

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
