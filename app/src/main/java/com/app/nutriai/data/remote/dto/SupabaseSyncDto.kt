package com.app.nutriai.data.remote.dto

import com.app.nutriai.data.local.entity.CatalogEntity
import com.app.nutriai.data.local.entity.DailyLogEntity
import com.app.nutriai.data.local.entity.FoodItemEntity
import com.app.nutriai.data.local.entity.UserPreferencesEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
//  Remote DTOs (Supabase PostgREST schema)
//  last_modified_at — epoch milliseconds (Long) set by the client.
//  updated_at       — ISO 8601 TIMESTAMPTZ string set by a Postgres trigger
//                     on every INSERT/UPDATE; used as the incremental pull cursor
//                     to avoid client clock skew.  Nullable (null for rows written
//                     before the server-side trigger was deployed).
//  Column names match Supabase Postgres schema exactly.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Remote representation of the users table.
 * Posted once when the user first syncs to create a Postgres profile row
 * linked to their auth.users UUID.
 */
@Serializable
data class RemoteUserDto(
    val id: String,           // Supabase auth UUID
    val email: String,
    @SerialName("created_at")
    val createdAt: Long
)

/**
 * Remote representation of the catalogs table.
 *
 * ID format in Supabase: "{supabaseUserId}_{localCatalogId}"
 * This prefix avoids PRIMARY KEY collisions when multiple users all have
 * the same local fixed catalog IDs ("local_user_recipes", etc.).
 * [userId] is the Supabase auth UUID — always stored as UUID in remote.
 */
@Serializable
data class RemoteCatalogDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("last_modified_at") val lastModifiedAt: Long,
    @SerialName("is_synced") val isSynced: Boolean = true,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    /** Server-set timestamp from Postgres trigger. ISO 8601 TIMESTAMPTZ. */
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Remote representation of the food_items table.
 *
 * Food item IDs are UUIDs generated locally — no prefix needed.
 * [catalogId] MUST be the prefixed remote catalog ID (see [RemoteCatalogDto]).
 */
@Serializable
data class RemoteFoodItemDto(
    val id: String,
    @SerialName("catalog_id") val catalogId: String,
    val name: String,
    val brand: String? = null,
    @SerialName("base_serving_g") val baseServingG: Double,
    @SerialName("base_calories") val baseCalories: Double,
    @SerialName("base_protein") val baseProtein: Double,
    @SerialName("base_carbs") val baseCarbs: Double,
    @SerialName("base_fat") val baseFat: Double,
    @SerialName("external_api_id") val externalApiId: String? = null,
    @SerialName("last_modified_at") val lastModifiedAt: Long,
    @SerialName("is_synced") val isSynced: Boolean = true,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    /** Server-set timestamp from Postgres trigger. ISO 8601 TIMESTAMPTZ. */
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Remote representation of the daily_logs table.
 *
 * Daily log IDs are UUIDs generated locally — no prefix needed.
 * [userId] is the Supabase auth UUID.
 * [foodItemId] matches the local food item UUID (no prefix).
 * Nullable locally after SET_NULL purge, but always non-null in Supabase.
 */
@Serializable
data class RemoteDailyLogDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("food_item_id") val foodItemId: String? = null,
    /** Snapshotted food name — defaults to "" for rows synced before this field was added. */
    @SerialName("food_name") val foodName: String = "",
    @SerialName("date_timestamp") val dateTimestamp: Long,
    @SerialName("consumed_qty") val consumedQty: Double,
    @SerialName("consumed_unit") val consumedUnit: String,
    @SerialName("total_calories") val totalCalories: Double,
    @SerialName("total_protein") val totalProtein: Double,
    @SerialName("total_carbs") val totalCarbs: Double,
    @SerialName("total_fat") val totalFat: Double,
    /** Meal category: "breakfast", "snack", "lunch", "dinner". Null for legacy rows. */
    @SerialName("meal_type") val mealType: String? = null,
    @SerialName("last_modified_at") val lastModifiedAt: Long,
    @SerialName("is_synced") val isSynced: Boolean = true,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    /** Server-set timestamp from Postgres trigger. ISO 8601 TIMESTAMPTZ. */
    @SerialName("updated_at") val updatedAt: String? = null
)

// ─────────────────────────────────────────────
//  Mapper helpers — entity ↔ remote DTO
// ─────────────────────────────────────────────

/**
 * Maps a local [CatalogEntity] to [RemoteCatalogDto] for Supabase upsert.
 *
 * @param supabaseUserId The authenticated user's UUID from GoTrue.
 * @param remoteId       Pre-built remote ID (e.g. "{uuid}_local_user_recipes").
 */
fun CatalogEntity.toRemoteDto(supabaseUserId: String, remoteId: String): RemoteCatalogDto =
    RemoteCatalogDto(
        id = remoteId,
        userId = supabaseUserId,
        name = name,
        lastModifiedAt = lastModifiedAt,
        isSynced = true,
        deletedAt = deletedAt
    )

/**
 * Maps a local [FoodItemEntity] to [RemoteFoodItemDto] for Supabase upsert.
 *
 * @param remoteCatalogId The prefixed catalog ID as stored in Supabase.
 */
fun FoodItemEntity.toRemoteDto(remoteCatalogId: String): RemoteFoodItemDto =
    RemoteFoodItemDto(
        id = id,
        catalogId = remoteCatalogId,
        name = name,
        brand = brand,
        baseServingG = baseServingG,
        baseCalories = baseCalories,
        baseProtein = baseProtein,
        baseCarbs = baseCarbs,
        baseFat = baseFat,
        externalApiId = externalApiId,
        lastModifiedAt = lastModifiedAt,
        isSynced = true,
        deletedAt = deletedAt
    )

/**
 * Maps a local [DailyLogEntity] to [RemoteDailyLogDto] for Supabase upsert.
 *
 * @param supabaseUserId The authenticated user's UUID from GoTrue.
 */
fun DailyLogEntity.toRemoteDto(supabaseUserId: String): RemoteDailyLogDto =
    RemoteDailyLogDto(
        id = id,
        userId = supabaseUserId,
        foodItemId = foodItemId,
        foodName = foodName,
        dateTimestamp = dateTimestamp,
        consumedQty = consumedQty,
        consumedUnit = consumedUnit,
        totalCalories = totalCalories,
        totalProtein = totalProtein,
        totalCarbs = totalCarbs,
        totalFat = totalFat,
        mealType = mealType,
        lastModifiedAt = lastModifiedAt,
        isSynced = true,
        deletedAt = deletedAt
    )

// ─────────────────────────────────────────────
//  Remote DTO → local entity (pull direction)
// ─────────────────────────────────────────────

/**
 * Converts a pulled [RemoteCatalogDto] back into a local [CatalogEntity].
 *
 * [localId] strips the UUID prefix from the remote ID so the local catalog ID
 * (e.g. "local_user_recipes") is preserved in Room.
 * [localUserId] is always [com.app.nutriai.util.Constants.LOCAL_USER_ID].
 */
fun RemoteCatalogDto.toEntity(localId: String, localUserId: String): CatalogEntity =
    CatalogEntity(
        id = localId,
        userId = localUserId,
        name = name,
        lastModifiedAt = lastModifiedAt,
        isSynced = true,
        deletedAt = deletedAt
    )

/**
 * Converts a pulled [RemoteFoodItemDto] back into a local [FoodItemEntity].
 *
 * [localCatalogId] is the local catalog ID (prefix stripped).
 */
fun RemoteFoodItemDto.toEntity(localCatalogId: String): FoodItemEntity =
    FoodItemEntity(
        id = id,
        catalogId = localCatalogId,
        name = name,
        brand = brand,
        baseServingG = baseServingG,
        baseCalories = baseCalories,
        baseProtein = baseProtein,
        baseCarbs = baseCarbs,
        baseFat = baseFat,
        externalApiId = externalApiId,
        lastModifiedAt = lastModifiedAt,
        isSynced = true,
        deletedAt = deletedAt
    )

/**
 * Converts a pulled [RemoteDailyLogDto] back into a local [DailyLogEntity].
 *
 * [localUserId] is always [com.app.nutriai.util.Constants.LOCAL_USER_ID].
 */
fun RemoteDailyLogDto.toEntity(localUserId: String): DailyLogEntity =
    DailyLogEntity(
        id = id,
        userId = localUserId,
        foodItemId = foodItemId,
        foodName = foodName,
        dateTimestamp = dateTimestamp,
        consumedQty = consumedQty,
        consumedUnit = consumedUnit,
        totalCalories = totalCalories,
        totalProtein = totalProtein,
        totalCarbs = totalCarbs,
        totalFat = totalFat,
        mealType = mealType,
        lastModifiedAt = lastModifiedAt,
        isSynced = true,
        deletedAt = deletedAt
    )

// ─────────────────────────────────────────────
//  User Preferences (macro goals sync — Phase 14)
// ─────────────────────────────────────────────

/**
 * Remote representation of the user_preferences table (migration 006).
 *
 * The Supabase table has NO `last_modified_at` or `is_synced` columns — those
 * are local-only in Room. `updated_at` is a server-managed trigger timestamp
 * (ISO 8601 TIMESTAMPTZ) used for pull freshness checks.
 */
@Serializable
data class RemoteUserPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("calorie_goal") val calorieGoal: Double = 2000.0,
    @SerialName("protein_goal") val proteinGoal: Double = 150.0,
    @SerialName("carbs_goal") val carbsGoal: Double = 250.0,
    @SerialName("fat_goal") val fatGoal: Double = 65.0,
    // ── Profile fields (Phase R4: AI Recommendations) ──
    val age: Int? = null,
    val gender: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("weight_goal") val weightGoal: String? = null,
    @SerialName("diet_type") val dietType: String? = null,
    /** Supabase TEXT[] deserializes as List<String> via kotlinx.serialization. */
    @SerialName("cuisine_preferences") val cuisinePreferences: List<String> = emptyList(),
    /** Supabase TEXT[] deserializes as List<String>. */
    val allergies: List<String> = emptyList(),
    @SerialName("recommendations_enabled") val recommendationsEnabled: Boolean = false,
    // ── End profile fields ──
    /** Server-set timestamp from Postgres trigger. ISO 8601 TIMESTAMPTZ. */
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Push-only DTO for user_preferences upsert.
 *
 * Excludes `updated_at` because the Supabase column is `NOT NULL DEFAULT now()`
 * with a BEFORE UPDATE trigger. Sending explicit `null` (which `explicitNulls = true`
 * would do for [RemoteUserPreferencesDto.updatedAt]) violates the NOT NULL constraint
 * and returns HTTP 400.
 */
@Serializable
data class RemoteUserPreferencesPushDto(
    @SerialName("user_id") val userId: String,
    @SerialName("calorie_goal") val calorieGoal: Double,
    @SerialName("protein_goal") val proteinGoal: Double,
    @SerialName("carbs_goal") val carbsGoal: Double,
    @SerialName("fat_goal") val fatGoal: Double,
    // ── Profile fields (Phase R4: AI Recommendations) ──
    val age: Int? = null,
    val gender: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("weight_goal") val weightGoal: String? = null,
    @SerialName("diet_type") val dietType: String? = null,
    /** Supabase TEXT[] — kotlinx.serialization serializes List<String> as JSON array,
     *  which PostgREST accepts for TEXT[] columns. */
    @SerialName("cuisine_preferences") val cuisinePreferences: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    @SerialName("recommendations_enabled") val recommendationsEnabled: Boolean = false
)

/** Maps a local [UserPreferencesEntity] to [RemoteUserPreferencesPushDto] for Supabase upsert. */
fun UserPreferencesEntity.toRemoteDto(supabaseUserId: String): RemoteUserPreferencesPushDto =
    RemoteUserPreferencesPushDto(
        userId = supabaseUserId,
        calorieGoal = calorieGoal,
        proteinGoal = proteinGoal,
        carbsGoal = carbsGoal,
        fatGoal = fatGoal,
        // Profile fields — CSV string → List<String> for Supabase TEXT[]
        age = age,
        gender = gender,
        weightKg = weightKg,
        weightGoal = weightGoal,
        dietType = dietType,
        cuisinePreferences = cuisinePreferences
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        allergies = allergies
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        recommendationsEnabled = recommendationsEnabled
    )

/** Converts a pulled [RemoteUserPreferencesDto] into a local [UserPreferencesEntity]. */
fun RemoteUserPreferencesDto.toEntity(localUserId: String): UserPreferencesEntity =
    UserPreferencesEntity(
        userId = localUserId,
        calorieGoal = calorieGoal,
        proteinGoal = proteinGoal,
        carbsGoal = carbsGoal,
        fatGoal = fatGoal,
        // Profile fields — List<String> from Supabase TEXT[] → CSV string for Room
        age = age,
        gender = gender,
        weightKg = weightKg,
        weightGoal = weightGoal,
        dietType = dietType,
        cuisinePreferences = cuisinePreferences
            .filter { it.isNotBlank() }
            .joinToString(","),
        allergies = allergies
            .filter { it.isNotBlank() }
            .joinToString(","),
        recommendationsEnabled = recommendationsEnabled,
        isSynced = true,
        lastModifiedAt = System.currentTimeMillis()
    )
