package com.app.nutriai.data.remote.api

import com.app.nutriai.data.remote.dto.ParseFoodEdgeResponse
import com.app.nutriai.data.remote.dto.ParseFoodRequest
import com.app.nutriai.data.remote.dto.PrefetchRequest
import com.app.nutriai.data.remote.dto.RecommendMealsRequest
import com.app.nutriai.data.remote.dto.RecommendMealsResponse
import com.app.nutriai.data.remote.dto.ScanLabelEdgeResponse
import com.app.nutriai.data.remote.dto.ScanLabelRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for Supabase Edge Functions.
 *
 * Uses the same `@Named("supabase")` Retrofit instance as [SupabaseDbApiService] —
 * Edge Functions live at `/functions/v1/{name}` on the same Supabase base URL.
 * The OkHttpClient already injects `apikey` and `Authorization: Bearer <jwt>` headers.
 *
 * Separated from [SupabaseDbApiService] because Edge Functions are RPC-style
 * endpoints (POST with JSON body → JSON response), not PostgREST CRUD operations.
 *
 * Phase R2: Android Home Screen Recommendations.
 */
interface SupabaseEdgeFunctionService {

    /**
     * Call the `parse-food` Edge Function.
     *
     * Replaces direct Gemini API calls for food entity extraction.
     * The Edge Function handles catalog name fetching, prompt building,
     * Gemma 4 inference, JSON extraction, and catalog cache resolution
     * — all server-side with shared prompts from `_shared/prompts.ts`.
     *
     * Returns parsed foods with pre-resolved catalog matches.
     */
    @POST("functions/v1/parse-food")
    suspend fun parseFoodViaEdge(
        @Body body: ParseFoodRequest
    ): Response<ParseFoodEdgeResponse>

    /**
     * Call the `scan-label` Edge Function.
     *
     * Replaces direct Gemini API calls for nutrition label scanning.
     * The Edge Function handles Gemma 4 Vision inference, JSON extraction,
     * and per-serving → per-100g conversion — all server-side.
     *
     * Returns both raw per-serving and converted per-100g values,
     * plus suggested quantity/unit for form pre-fill.
     */
    @POST("functions/v1/scan-label")
    suspend fun scanLabelViaEdge(
        @Body body: ScanLabelRequest
    ): Response<ScanLabelEdgeResponse>

    /**
     * Call the `recommend-meals` Edge Function.
     *
     * Supports two modes:
     * - `time_based` — automatic recommendations based on time of day + remaining macros.
     * - `query` — on-demand search with a user-provided query string.
     *
     * The Edge Function handles catalog pre-filtering, frequency ranking,
     * optional profile-based personalization, and Gemma 4 inference.
     */
    @POST("functions/v1/recommend-meals")
    suspend fun recommendMeals(
        @Body body: RecommendMealsRequest
    ): Response<RecommendMealsResponse>

    /**
     * Fire-and-forget trigger for the `prefetch-recommendations` Edge Function.
     *
     * Called after every food log, edit, or delete to proactively refresh the
     * recommendation cache for the next meal slot. Passes client-local timestamps
     * so the server uses the correct timezone for cache keys and meal progression.
     *
     * Returns raw [ResponseBody] — the response is not parsed; we only care
     * about whether the call succeeded (caller swallows errors).
     *
     * Phase R2.1: Cache-first Android recommendations.
     */
    @POST("functions/v1/prefetch-recommendations")
    suspend fun triggerPrefetch(
        @Body body: PrefetchRequest
    ): Response<ResponseBody>
}
