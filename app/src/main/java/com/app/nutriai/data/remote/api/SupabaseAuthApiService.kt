package com.app.nutriai.data.remote.api

import com.app.nutriai.data.remote.dto.GoTrueResponse
import com.app.nutriai.data.remote.dto.RefreshTokenRequest
import com.app.nutriai.data.remote.dto.SignInRequest
import com.app.nutriai.data.remote.dto.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for Supabase GoTrue authentication REST API.
 *
 * Base URL: [BuildConfig.SUPABASE_URL] (e.g. https://xxxx.supabase.co)
 * Auth endpoints live under the /auth/v1/ path.
 *
 * The [apikey] header is added automatically by [SupabaseHeaderInterceptor]
 * (injected via the @Named("supabase") OkHttpClient in SupabaseModule).
 * These endpoints do NOT require a JWT Bearer token — only the anon key.
 */
interface SupabaseAuthApiService {

    /**
     * Register a new user with email and password.
     * Returns a session immediately if email confirmation is disabled
     * in the Supabase dashboard (recommended for this app).
     */
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body body: SignUpRequest
    ): Response<GoTrueResponse>

    /**
     * Sign in an existing user with email and password.
     * Returns access + refresh tokens on success.
     */
    @POST("auth/v1/token")
    suspend fun signIn(
        @Query("grant_type") grantType: String = "password",
        @Body body: SignInRequest
    ): Response<GoTrueResponse>

    /**
     * Exchange a refresh token for a new access token.
     * Called automatically by [SupabaseAuthenticator] on 401 responses.
     */
    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: RefreshTokenRequest
    ): Response<GoTrueResponse>

    /**
     * Revoke the current session on the Supabase server.
     * The JWT must be passed explicitly here — the interceptor may not have a
     * valid token anymore if the user is mid-sign-out.
     */
    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>
}
