package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.nutriai.domain.model.NutritionInfo

/**
 * Room entity for IFCT 2017 (Indian Food Composition Tables) data.
 *
 * Seeded once on first launch from assets/ifct2017.csv by [com.app.nutriai.util.IfctCsvLoader].
 * Provides offline nutrition lookup for ~120 common Indian foods when the
 * USDA FoodData Central network call fails or returns no results.
 *
 * Source: National Institute of Nutrition (NIN), ICMR, Hyderabad — IFCT 2017.
 * All values are per 100g of edible portion.
 *
 * Phase 5.5: Offline fallback in the FDC → IFCT → null lookup chain.
 */
@Entity(tableName = "ifct_foods")
data class IfctFoodEntity(

    /** NIN food code (e.g. "G001", "P002", "V015"). Primary key from CSV. */
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    /** Full food name as it appears in IFCT 2017 (may contain Indian names). */
    @ColumnInfo(name = "name")
    val name: String,

    /** Energy in kcal per 100g. */
    @ColumnInfo(name = "energy_kcal")
    val energyKcal: Double,

    /** Protein in grams per 100g. */
    @ColumnInfo(name = "protein_g")
    val proteinG: Double,

    /** Total fat in grams per 100g. */
    @ColumnInfo(name = "fat_g")
    val fatG: Double,

    /** Total carbohydrates in grams per 100g. */
    @ColumnInfo(name = "carbs_g")
    val carbsG: Double,

    /** Dietary fiber in grams per 100g. 0.0 means not significant or not reported. */
    @ColumnInfo(name = "fiber_g")
    val fiberG: Double
)

// ─── Entity → Domain mapper ─────────────────────────────────────────────────

fun IfctFoodEntity.toNutritionInfo(): NutritionInfo = NutritionInfo(
    productName = name,
    brand = null,
    caloriesPer100g = energyKcal,
    proteinPer100g = proteinG,
    carbsPer100g = carbsG,
    fatPer100g = fatG,
    fiberPer100g = if (fiberG > 0.0) fiberG else null,
    source = "IFCT 2017 (Offline)",
    externalId = code
)
