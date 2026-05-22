package com.app.nutriai.data.remote.api

import com.app.nutriai.data.remote.dto.FdcSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the USDA FoodData Central (FDC) REST API.
 *
 * Base URL: https://api.nal.usda.gov/
 * Docs: https://app.swaggerhub.com/apis/fdcnal/food-data_central_api/1.0.1
 *
 * Phase 5.5: Primary online nutrition source for the app.
 * Requires a free API key from https://fdc.nal.usda.gov/api-key-signup.html
 * stored as USDA_FDC_API_KEY in local.properties.
 *
 * Data types searched (in order of preference for generic queries):
 * - Foundation: USDA lab-measured reference data
 * - SR Legacy: Standard Reference legacy (pre-2019 USDA standard)
 * - Branded: Manufacturer-submitted branded food data
 *
 * All nutrient values in FDC are expressed per 100g of food.
 */
interface FoodDataCentralApiService {

    @GET("fdc/v1/foods/search")
    suspend fun searchFood(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy,Branded",
        @Query("pageSize") pageSize: Int = 5,
        @Query("pageNumber") pageNumber: Int = 1
    ): FdcSearchResponse
}
