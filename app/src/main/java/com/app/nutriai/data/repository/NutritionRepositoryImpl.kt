package com.app.nutriai.data.repository

import android.util.Log
import com.app.nutriai.data.local.dao.IfctFoodDao
import com.app.nutriai.data.local.entity.toNutritionInfo
import com.app.nutriai.data.remote.api.FoodDataCentralApiService
import com.app.nutriai.data.remote.dto.toNutritionInfo
import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.domain.repository.NutritionRepository
import com.app.nutriai.util.IfctCsvLoader
import com.app.nutriai.util.Resource
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementation of [NutritionRepository] using a two-tier lookup chain:
 *
 * **Tier 1 — USDA FoodData Central (FDC):** Online REST API backed by USDA's
 * authoritative database of Foundation, SR Legacy, and Branded foods.
 * Requires [fdcApiKey] from BuildConfig (local.properties: USDA_FDC_API_KEY).
 * Returns up to 5 ranked results; Foundation/SR Legacy preferred over Branded.
 *
 * **Tier 2 — IFCT 2017 (offline):** ~120 common Indian foods from the National
 * Institute of Nutrition (NIN) ICMR IFCT 2017 tables, bundled as a CSV asset
 * and seeded into Room on first launch via [IfctCsvLoader]. Activated when FDC
 * is unreachable (VPN, no internet) or returns no usable results.
 *
 * **Tier 3 — null:** Both tiers produced nothing. [LookupNutritionUseCase] will
 * return [Resource.Success] with null, and the UI prompts manual entry.
 *
 * Phase 5.5: Two-tier USDA FDC implementation.
 * Domain interface [NutritionRepository] is unchanged.
 */
@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val fdcApiService: FoodDataCentralApiService,
    @Named("fdcApiKey") private val fdcApiKey: String,
    private val ifctFoodDao: IfctFoodDao,
    private val ifctCsvLoader: IfctCsvLoader
) : NutritionRepository {

    override suspend fun searchNutrition(foodName: String, brand: String?): Resource<List<NutritionInfo>> {
        // Ensure IFCT table is populated (no-op after first launch)
        runCatching { ifctCsvLoader.seedIfNeeded() }

        // ── Tier 1a: FDC Branded (only when brand is provided) ─────────────
        if (!brand.isNullOrBlank()) {
            val brandedResult = tryFdc("$brand $foodName", dataType = "Branded")
            if (brandedResult != null) {
                // Validate the FDC result actually belongs to the requested brand.
                // FDC search is keyword-based and may return a different brand
                // (e.g. "Amul cheese" → "Food Lion cheese" if Amul isn't in FDC).
                val topResult = (brandedResult as? Resource.Success)?.data?.firstOrNull()
                val resultBrand = (topResult?.brand ?: "").lowercase()
                val requestedBrand = brand.lowercase().trim()
                val brandMatches = resultBrand.contains(requestedBrand) ||
                    requestedBrand.split("\\s+".toRegex()).any { word ->
                        word.length > 2 && resultBrand.contains(word)
                    }

                if (brandMatches) {
                    return brandedResult.mapResults { it.copy(matchType = "branded") }
                }
                // Brand mismatch — fall through to generic tier.
                // UI will show "Brand not found, using generic" via matchType + clarification type.
                Log.d(TAG, "FDC Branded brand \"${topResult?.brand}\" doesn't match \"$brand\" — falling through to generic")
            }
        }

        // ── Tier 1b: USDA FoodData Central (all types) ────────────────────
        val fdcResult = tryFdc(foodName)
        if (fdcResult != null) {
            return fdcResult.mapResults { it.copy(matchType = "generic") }
        }

        // ── Tier 2: IFCT 2017 offline fallback ─────────────────────────────
        val ifctResult = tryIfct(foodName)
        if (ifctResult != null) {
            return ifctResult.mapResults { it.copy(matchType = "generic") }
        }

        // ── Tier 3: Nothing found ───────────────────────────────────────────
        Log.d(TAG, "No results from FDC or IFCT for \"$foodName\"")
        return Resource.Success(emptyList())
    }

    /**
     * Maps each [NutritionInfo] inside a [Resource.Success] list using the given transform.
     */
    private fun Resource<List<NutritionInfo>>.mapResults(
        transform: (NutritionInfo) -> NutritionInfo
    ): Resource<List<NutritionInfo>> = when (this) {
        is Resource.Success -> Resource.Success(data.map(transform))
        is Resource.Error -> this
        is Resource.Loading -> this
    }

    // ─── Tier 1: FDC ─────────────────────────────────────────────────────────

    private suspend fun tryFdc(
        foodName: String,
        dataType: String = "Foundation,SR Legacy,Branded"
    ): Resource<List<NutritionInfo>>? {
        if (fdcApiKey.isBlank()) {
            Log.w(TAG, "USDA_FDC_API_KEY is not set — skipping FDC lookup.")
            return null
        }

        return try {
            Log.d(TAG, "FDC search: \"$foodName\" (dataType=$dataType)")
            val response = fdcApiService.searchFood(query = foodName, apiKey = fdcApiKey, dataType = dataType)
            Log.d(TAG, "FDC returned ${response.foods.size} foods for \"$foodName\"")

            val results = response.foods
                .filter { it.hasUsableData }
                .mapNotNull { it.toNutritionInfo() }
                .sortedWith(compareBy(
                    // Priority 1: Exact matches first (case-insensitive)
                    { it.productName.lowercase().trim() != foodName.lowercase().trim() },
                    // Priority 2: Starts-with matches next
                    { !it.productName.lowercase().trim().startsWith(foodName.lowercase().trim()) },
                    // Priority 3: Macro completeness score (descending)
                    {
                        var score = 0
                        if (it.caloriesPer100g > 0) score += 4
                        if (it.proteinPer100g > 0) score += 1
                        if (it.carbsPer100g > 0) score += 1
                        if (it.fatPer100g > 0) score += 1
                        -score
                    }
                ))

            if (results.isNotEmpty()) {
                Log.d(TAG, "FDC: ${results.size} usable results for \"$foodName\"")
                Resource.Success(results)
            } else {
                Log.d(TAG, "FDC: no usable results for \"$foodName\" — trying IFCT")
                null  // Fall through to IFCT
            }

        } catch (e: retrofit2.HttpException) {
            val msg = when (e.code()) {
                403 -> "FDC API key invalid or quota exceeded."
                429 -> "FDC rate-limited — please try again shortly."
                500, 502, 503 -> "USDA FoodData Central temporarily unavailable."
                else -> "FDC lookup failed (HTTP ${e.code()})."
            }
            Log.e(TAG, "FDC HTTP ${e.code()} for \"$foodName\": ${e.message()}", e)
            // Non-fatal — fall through to IFCT unless it's a key/auth issue
            if (e.code() == 403) Resource.Error(msg, e) else null

        } catch (e: java.net.UnknownHostException) {
            Log.w(TAG, "FDC unreachable (no internet / VPN) for \"$foodName\" — trying IFCT")
            null  // Fall through to IFCT silently

        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "FDC timeout for \"$foodName\" — trying IFCT")
            null

        } catch (e: java.io.IOException) {
            Log.w(TAG, "FDC IO error for \"$foodName\": ${e::class.simpleName} — trying IFCT")
            null

        } catch (e: Exception) {
            Log.e(TAG, "FDC unexpected error for \"$foodName\": ${e::class.simpleName}", e)
            null
        }
    }

    // ─── Tier 2: IFCT ────────────────────────────────────────────────────────

    private suspend fun tryIfct(foodName: String): Resource<List<NutritionInfo>>? {
        return try {
            Log.d(TAG, "IFCT search: \"$foodName\"")

            // First try full-phrase match
            var rows = ifctFoodDao.searchByName(query = foodName)

            // If nothing, try each individual word (handles "chicken breast" → "breast")
            if (rows.isEmpty()) {
                val words = foodName.trim().split(Regex("\\s+")).filter { it.length > 2 }
                for (word in words) {
                    rows = ifctFoodDao.searchByWord(word = word)
                    if (rows.isNotEmpty()) break
                }
            }

            if (rows.isNotEmpty()) {
                Log.d(TAG, "IFCT: ${rows.size} results for \"$foodName\"")
                Resource.Success(rows.map { it.toNutritionInfo() })
            } else {
                Log.d(TAG, "IFCT: no results for \"$foodName\"")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "IFCT query error for \"$foodName\": ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "NutritionRepo"
    }
}
