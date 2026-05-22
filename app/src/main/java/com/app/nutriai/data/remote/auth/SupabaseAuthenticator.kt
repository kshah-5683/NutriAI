package com.app.nutriai.data.remote.auth

import android.util.Log
import com.app.nutriai.BuildConfig
import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.data.remote.dto.GoTrueResponse
import com.app.nutriai.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.net.Proxy
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupabaseAuthenticator"

/**
 * OkHttp [Authenticator] that transparently refreshes the Supabase JWT
 * when the server responds with HTTP 401 (Unauthorized).
 *
 * Flow:
 *  1. OkHttp receives a 401 response → calls [authenticate].
 *  2. Reads the persisted refresh token from [AuthPreferences].
 *  3. Makes a **synchronous** raw HTTP call to GoTrue's token endpoint
 *     (bypasses Retrofit to avoid interceptor/authenticator recursion).
 *  4. On success: updates [AuthPreferences] with the new access + refresh tokens,
 *     then retries the original request with the new Bearer token.
 *  5. On failure (400/401 from GoTrue): clears the session — the user must sign in again.
 *
 * The `X-Auth-Retry` header prevents infinite retry loops: if the retried request
 * also returns 401, [authenticate] returns null and the 401 propagates to the caller.
 *
 * [runBlocking] is acceptable here because OkHttp's Authenticator already executes
 * on a background I/O thread, and [AuthPreferences] DataStore reads are fast.
 */
@Singleton
class SupabaseAuthenticator @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val json: Json
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops — if we already retried once, give up.
        if (response.request.header("X-Auth-Retry") != null) {
            Log.w(TAG, "Already retried once — giving up on 401")
            return null
        }

        // Read refresh token from DataStore.
        val refreshToken = runBlocking { authPreferences.getRefreshToken() }
        if (refreshToken == null) {
            Log.w(TAG, "No refresh token available — cannot refresh")
            return null
        }

        Log.d(TAG, "Access token expired — attempting refresh")

        // Build a raw OkHttp request to GoTrue (avoids Retrofit recursion).
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val requestBody = json.encodeToString(RefreshTokenRequest.serializer(), RefreshTokenRequest(refreshToken))
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
            .post(requestBody)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Content-Type", "application/json")
            .build()

        // Use a bare OkHttpClient — no interceptors, no authenticator (prevents recursion).
        val refreshClient = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .build()

        val refreshResponse = try {
            refreshClient.newCall(refreshRequest).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh network error", e)
            return null
        }

        return refreshResponse.use { resp ->
            if (!resp.isSuccessful) {
                if (resp.code == 400 || resp.code == 401) {
                    // Refresh token is invalid/expired — force sign-out.
                    Log.w(TAG, "Refresh token expired (HTTP ${resp.code}) — clearing session")
                    runBlocking { authPreferences.clearSession() }
                } else {
                    Log.w(TAG, "Token refresh failed: HTTP ${resp.code}")
                }
                return@use null
            }

            val body = try {
                val bodyString = resp.body?.string() ?: return@use null
                json.decodeFromString(GoTrueResponse.serializer(), bodyString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse refresh response", e)
                return@use null
            }

            if (body.accessToken.isBlank()) {
                Log.w(TAG, "Refresh response had empty access token")
                return@use null
            }

            // Update persisted session with new tokens.
            runBlocking {
                val existing = authPreferences.getSession()
                if (existing != null) {
                    val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1_000L)
                    authPreferences.saveSession(
                        existing.copy(
                            accessToken  = body.accessToken,
                            // GoTrue rotates the refresh token on each refresh.
                            // Keep the old one if the response doesn't include a new one.
                            refreshToken = body.refreshToken.ifBlank { existing.refreshToken },
                            expiresAt    = expiresAt
                        )
                    )
                }
            }

            Log.d(TAG, "Token refreshed successfully — retrying original request")

            // Retry the original request with the new access token.
            response.request.newBuilder()
                .header("Authorization", "Bearer ${body.accessToken}")
                .header("X-Auth-Retry", "1")
                .build()
        }
    }
}
