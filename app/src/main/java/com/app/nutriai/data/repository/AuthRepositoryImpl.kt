package com.app.nutriai.data.repository

import android.util.Log
import com.app.nutriai.data.local.dao.CatalogDao
import com.app.nutriai.data.local.dao.DailyLogDao
import com.app.nutriai.data.local.dao.FoodItemDao
import com.app.nutriai.data.local.dao.UserDao
import com.app.nutriai.data.local.dao.UserPreferencesDao
import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.data.local.preferences.AuthSession
import com.app.nutriai.data.remote.api.SupabaseAuthApiService
import com.app.nutriai.data.remote.dto.GoTrueError
import com.app.nutriai.data.remote.dto.GoTrueResponse
import com.app.nutriai.data.remote.dto.IdTokenSignInRequest
import com.app.nutriai.data.remote.dto.RefreshTokenRequest
import com.app.nutriai.data.remote.dto.SignInRequest
import com.app.nutriai.data.remote.dto.SignUpRequest
import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.repository.AuthRepository
import com.app.nutriai.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepositoryImpl"

/**
 * Implementation of [AuthRepository] backed by the Supabase GoTrue REST API
 * and [AuthPreferences] (DataStore) for session persistence.
 *
 * Session lifecycle:
 *  1. On app start, [getAuthStateFlow] reads the persisted token from DataStore.
 *  2. On sign-in / sign-up, tokens are saved to DataStore and cached in memory.
 *  3. On sign-out, the server session is revoked and local tokens are wiped.
 *  4. On 401 responses, [refreshSession] is called by [SupabaseAuthenticator]
 *     (OkHttp Authenticator) to obtain a new access token transparently.
 *
 * If [BuildConfig.SUPABASE_URL] or [BuildConfig.SUPABASE_ANON_KEY] is blank
 * (i.e. not configured in local.properties), all operations return
 * [Resource.Error] with a clear setup message instead of crashing.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: SupabaseAuthApiService,
    private val authPreferences: AuthPreferences,
    private val json: Json,
    // User-data DAOs for sign-out wipe (ifct_foods intentionally excluded — static seed data)
    private val userDao: UserDao,
    private val catalogDao: CatalogDao,
    private val foodItemDao: FoodItemDao,
    private val dailyLogDao: DailyLogDao,
    private val userPreferencesDao: UserPreferencesDao
) : AuthRepository {

    // ─── Auth state ───────────────────────────────────────────────────────

    override fun getAuthStateFlow(): Flow<AuthState> =
        authPreferences.sessionFlow.map { session ->
            when {
                session == null -> AuthState.Unauthenticated
                else -> AuthState.Authenticated(
                    userId = session.userId,
                    email  = session.email
                )
            }
        }

    // ─── Sign Up ─────────────────────────────────────────────────────────

    override suspend fun signUp(email: String, password: String): Resource<AuthState> {
        return try {
            val response = authApiService.signUp(SignUpRequest(email, password))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up network error", e)
            Resource.Error("Sign-up failed: ${e.localizedMessage ?: "Network error"}", e)
        }
    }

    // ─── Sign In ─────────────────────────────────────────────────────────

    override suspend fun signIn(email: String, password: String): Resource<AuthState> {
        return try {
            val response = authApiService.signIn(body = SignInRequest(email, password))
            handleAuthResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in network error", e)
            Resource.Error("Sign-in failed: ${e.localizedMessage ?: "Network error"}", e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<AuthState> {
        return try {
            val response = authApiService.signInWithIdToken(
                body = IdTokenSignInRequest(idToken = idToken)
            )
            handleAuthResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in network error", e)
            Resource.Error("Google sign-in failed: ${e.localizedMessage ?: "Network error"}", e)
        }
    }

    // ─── Sign Out ────────────────────────────────────────────────────────

    override suspend fun signOut(): Resource<Unit> {
        return try {
            val session = authPreferences.getSession()
            if (session != null) {
                // Best-effort server revocation — ignore failure (token expired, offline, etc.)
                runCatching {
                    authApiService.signOut("Bearer ${session.accessToken}")
                }
            }
            authPreferences.clearSession()

            // Wipe all user-specific local data so a different account signing in on
            // the same device cannot inherit food items, logs, or catalogs from a
            // previous session. FK order: daily_logs → food_items → catalogs → users.
            // ifct_foods is intentionally excluded — it is static seed data, not user data.
            withContext(Dispatchers.IO) {
                dailyLogDao.clearAll()
                foodItemDao.clearAll()
                catalogDao.clearAll()
                userDao.clearAll()
                userPreferencesDao.clearAll()
            }

            Log.d(TAG, "Signed out — session, sync cursor, and local user data cleared")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out error", e)
            // Always clear local session even if server call fails
            runCatching { authPreferences.clearSession() }
            Resource.Error("Sign-out error: ${e.localizedMessage}", e)
        }
    }

    // ─── Token Refresh ───────────────────────────────────────────────────

    override suspend fun refreshSession(): Resource<Unit> {
        val refreshToken = authPreferences.getRefreshToken()
            ?: return Resource.Error("No refresh token available — please sign in again")

        return try {
            val response = authApiService.refreshToken(
                body = RefreshTokenRequest(refreshToken)
            )
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null && body.accessToken.isNotBlank()) {
                        // Update full session (refresh token rotates on each refresh)
                        val existing = authPreferences.getSession()
                        if (existing != null) {
                            val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1_000L)
                            authPreferences.saveSession(
                                existing.copy(
                                    accessToken  = body.accessToken,
                                    refreshToken = body.refreshToken.ifBlank { existing.refreshToken },
                                    expiresAt    = expiresAt
                                )
                            )
                        }
                        Log.d(TAG, "Token refreshed successfully")
                        Resource.Success(Unit)
                    } else {
                        Resource.Error("Token refresh returned empty body")
                    }
                }
                response.code() == 400 || response.code() == 401 -> {
                    // Refresh token invalid/expired — force sign-out
                    Log.w(TAG, "Refresh token expired — clearing session")
                    authPreferences.clearSession()
                    Resource.Error("Session expired — please sign in again")
                }
                else -> {
                    Resource.Error("Token refresh failed: HTTP ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh network error", e)
            Resource.Error("Token refresh failed: ${e.localizedMessage}", e)
        }
    }

    // ─── Pre-emptive token refresh ───────────────────────────────────────

    override suspend fun refreshSessionIfNeeded(bufferMs: Long): Resource<Unit> {
        val session = authPreferences.getSession()
            ?: return Resource.Success(Unit) // Not signed in — nothing to refresh

        val msUntilExpiry = session.expiresAt - System.currentTimeMillis()
        return if (msUntilExpiry <= bufferMs) {
            Log.d(TAG, "Token expires in ${msUntilExpiry / 1_000}s — refreshing proactively")
            refreshSession()
        } else {
            Log.d(TAG, "Token still fresh (${msUntilExpiry / 60_000} min remaining) — skipping refresh")
            Resource.Success(Unit)
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    /**
     * Shared logic for handling GoTrue auth responses (sign-up + sign-in).
     * Persists the session to DataStore on success.
     */
    private suspend fun handleAuthResponse(
        response: Response<GoTrueResponse>
    ): Resource<AuthState> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                val user = body?.user
                if (body != null && body.accessToken.isNotBlank() && user != null) {
                    val expiresAt = System.currentTimeMillis() + (body.expiresIn * 1_000L)
                    val session = AuthSession(
                        accessToken  = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId       = user.id,
                        email        = user.email.orEmpty(),
                        expiresAt    = expiresAt
                    )
                    authPreferences.saveSession(session)
                    Log.d(TAG, "Auth successful — userId=${user.id}")
                    Resource.Success(AuthState.Authenticated(user.id, user.email.orEmpty()))
                } else {
                    Resource.Error("Unexpected empty response from Supabase")
                }
            }
            response.code() == 400 -> {
                val errMsg = parseGoTrueError(response) ?: "Invalid credentials"
                Resource.Error(errMsg)
            }
            response.code() == 422 -> {
                val errMsg = parseGoTrueError(response) ?: "Email already registered"
                Resource.Error(errMsg)
            }
            response.code() == 429 -> {
                Resource.Error("Too many requests — please wait a moment before trying again")
            }
            else -> {
                Resource.Error("Supabase auth error: HTTP ${response.code()}")
            }
        }
    }

    /** Attempts to deserialise GoTrue error JSON from the response body. */
    private fun parseGoTrueError(response: Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string() ?: return null
            val err = json.decodeFromString(GoTrueError.serializer(), errorBody)
            err.message.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
