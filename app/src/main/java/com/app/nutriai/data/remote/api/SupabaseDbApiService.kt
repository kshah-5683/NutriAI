package com.app.nutriai.data.remote.api

import com.app.nutriai.data.remote.dto.RemoteCatalogDto
import com.app.nutriai.data.remote.dto.RemoteDailyLogDto
import com.app.nutriai.data.remote.dto.RemoteFoodItemDto
import com.app.nutriai.data.remote.dto.RemoteUserDto
import com.app.nutriai.data.remote.dto.RemoteUserPreferencesDto
import com.app.nutriai.data.remote.dto.RemoteUserPreferencesPushDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for Supabase PostgREST data API (cloud sync).
 *
 * Base URL: [BuildConfig.SUPABASE_URL] (e.g. https://xxxx.supabase.co)
 * Database endpoints live under the /rest/v1/ path.
 *
 * Every request has the [apikey] and [Authorization: Bearer <jwt>] headers
 * injected automatically by [SupabaseHeaderInterceptor] (SupabaseModule).
 *
 * Upsert strategy: POST with [Prefer: resolution=merge-duplicates,return=minimal]
 * — inserts new rows and updates existing ones based on PRIMARY KEY.
 * Row Level Security (RLS) in Supabase ensures users can only read/write
 * their own data.
 *
 * PostgREST filter format for @Query parameters:
 *   Equality:   @Query("user_id")         = "eq.{uuid}"
 *   IN filter:  @Query("catalog_id", encoded=true) = "in.(id1,id2)"
 *   GT filter:  @Query("updated_at") = "gt.{isoTimestamp}"  (incremental pull cursor, server clock)
 *
 * Pagination: Pass [limit] and [offset] for paginated full pulls.
 * Null query params are omitted from the URL by Retrofit automatically.
 */
interface SupabaseDbApiService {

    // ─────────────────────────────────────────────
    //  Users
    // ─────────────────────────────────────────────

    /**
     * Create or update the user's profile row in public.users.
     * Called once after successful sign-in/sign-up.
     */
    @POST("rest/v1/users")
    suspend fun upsertUser(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=minimal",
        @Body body: RemoteUserDto
    ): Response<Unit>

    // ─────────────────────────────────────────────
    //  Catalogs
    // ─────────────────────────────────────────────

    /**
     * Fetch catalogs for the authenticated user.
     *
     * @param userIdFilter        PostgREST equality filter — "eq.{supabaseUserId}".
     * @param updatedAtFilter       Optional GT filter — "gt.{isoTimestamp}" for incremental pull (server clock).
     *                             Null → no time filter (full pull).
     * @param limit               Page size for paginated full pulls. Null → server default.
     * @param offset              Page offset for paginated full pulls. Null → 0.
     */
    @GET("rest/v1/catalogs")
    suspend fun getCatalogs(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<RemoteCatalogDto>>

    /** Upsert a batch of catalogs. */
    @POST("rest/v1/catalogs")
    suspend fun upsertCatalogs(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "id",
        @Body body: List<RemoteCatalogDto>
    ): Response<Void?>

    // ─────────────────────────────────────────────
    //  Food Items
    // ─────────────────────────────────────────────

    /**
     * Fetch food items whose catalog_id is in the provided list.
     *
     * @param catalogIdFilter     PostgREST IN filter — "in.(id1,id2,...)".
     *                            [encoded=true] prevents Retrofit from URL-encoding parentheses.
     * @param updatedAtFilter       Optional GT filter — "gt.{isoTimestamp}" for incremental pull (server clock).
     * @param limit               Page size for paginated full pulls. Null → server default.
     * @param offset              Page offset for paginated full pulls. Null → 0.
     */
    @GET("rest/v1/food_items")
    suspend fun getFoodItems(
        @Query(value = "catalog_id", encoded = true) catalogIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<RemoteFoodItemDto>>

    /** Upsert a batch of food items. */
    @POST("rest/v1/food_items")
    suspend fun upsertFoodItems(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "id",
        @Body body: List<RemoteFoodItemDto>
    ): Response<Void?>

    // ─────────────────────────────────────────────
    //  Daily Logs
    // ─────────────────────────────────────────────

    /**
     * Fetch daily logs for the authenticated user.
     *
     * @param userIdFilter        PostgREST equality filter — "eq.{supabaseUserId}".
     * @param updatedAtFilter       Optional GT filter — "gt.{isoTimestamp}" for incremental pull (server clock).
     * @param limit               Page size for paginated full pulls. Null → server default.
     * @param offset              Page offset for paginated full pulls. Null → 0.
     */
    @GET("rest/v1/daily_logs")
    suspend fun getDailyLogs(
        @Query("user_id") userIdFilter: String,
        @Query("updated_at") updatedAtFilter: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<RemoteDailyLogDto>>

    /** Upsert a batch of daily logs. */
    @POST("rest/v1/daily_logs")
    suspend fun upsertDailyLogs(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "id",
        @Body body: List<RemoteDailyLogDto>
    ): Response<Void?>

    // ─────────────────────────────────────────────
    //  User Preferences (macro goals — Phase 14)
    // ─────────────────────────────────────────────

    /**
     * Fetch the user's macro goal preferences.
     * Returns a list with 0 or 1 rows (single row per user, filtered by RLS).
     */
    @GET("rest/v1/user_preferences")
    suspend fun getUserPreferences(
        @Query("user_id") userIdFilter: String
    ): Response<List<RemoteUserPreferencesDto>>

    /**
     * Upsert the user's macro goal preferences.
     * Uses `on_conflict=user_id` since the PK is `user_id`, not `id`.
     */
    @POST("rest/v1/user_preferences")
    suspend fun upsertUserPreferences(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "user_id",
        @Body body: List<RemoteUserPreferencesPushDto>
    ): Response<Void?>
}
