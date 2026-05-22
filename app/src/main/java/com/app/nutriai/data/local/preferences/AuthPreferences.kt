package com.app.nutriai.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the Supabase auth session to [DataStore].
 *
 * Provides:
 *  - [sessionFlow] — observable auth session (emits null when signed out).
 *  - [saveSession] — persist a new session after sign-in / token refresh.
 *  - [clearSession] — wipe all stored tokens (sign-out).
 *  - [getCachedToken] — synchronous in-memory token read used by the
 *    OkHttp interceptor, which cannot call suspend functions.
 *
 * The [cachedToken] is an in-memory copy of the access token, updated
 * whenever [saveSession] or [clearSession] is called.  On app restart it
 * is populated lazily when the OkHttp interceptor first calls
 * [getCachedToken] while the coroutine-based warm-up in
 * [AuthRepositoryImpl] is still running — in that window the interceptor
 * will get `null` and proceed without a Bearer token, which is acceptable
 * for unauthenticated requests.
 */
@Singleton
class AuthPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val ACCESS_TOKEN    = stringPreferencesKey("access_token")
        val REFRESH_TOKEN   = stringPreferencesKey("refresh_token")
        val USER_ID         = stringPreferencesKey("user_id")
        val USER_EMAIL      = stringPreferencesKey("user_email")
        val TOKEN_EXPIRY    = longPreferencesKey("token_expiry")
        /** ISO 8601 timestamp from the Supabase server clock (updated_at column). */
        val LAST_SYNC_AT    = stringPreferencesKey("last_sync_at_iso")
    }

    // ─── In-memory token cache (for OkHttp interceptor) ─────────────────

    @Volatile
    private var cachedToken: String? = null

    init {
        // On cold start the in-memory cachedToken is null, but the access token is
        // persisted in DataStore from the previous session.  Warm up the cache
        // immediately so the OkHttp interceptor always has a token ready before the
        // first network call (e.g. push-on-write 500 ms after the first mutation).
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            dataStore.data.firstOrNull()?.get(Keys.ACCESS_TOKEN)?.let {
                cachedToken = it
            }
        }
    }

    /** Returns the current access token without suspending. May be null. */
    fun getCachedToken(): String? = cachedToken

    // ─── DataStore flows ─────────────────────────────────────────────────

    /**
     * Emits the persisted [AuthSession] whenever it changes.
     * Emits `null` when the user is signed out.
     */
    val sessionFlow: Flow<AuthSession?> = dataStore.data.map { prefs ->
        val token   = prefs[Keys.ACCESS_TOKEN]  ?: return@map null
        val refresh = prefs[Keys.REFRESH_TOKEN] ?: return@map null
        val userId  = prefs[Keys.USER_ID]       ?: return@map null
        val email   = prefs[Keys.USER_EMAIL]    ?: return@map null
        val expiry  = prefs[Keys.TOKEN_EXPIRY]  ?: 0L
        AuthSession(token, refresh, userId, email, expiry)
    }

    /**
     * ISO 8601 timestamp of the most recent successful sync, as set by the Supabase server clock.
     * Used as the incremental pull cursor — passed as "gt.{value}" to the updated_at filter.
     * Null on first launch or after a data wipe (triggers a full paginated pull).
     */
    val lastSyncAtFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_AT]
    }

    // ─── Mutations ───────────────────────────────────────────────────────

    /** Persist a new auth session and update the in-memory token cache. */
    suspend fun saveSession(session: AuthSession) {
        cachedToken = session.accessToken
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN]  = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.USER_ID]       = session.userId
            prefs[Keys.USER_EMAIL]    = session.email
            prefs[Keys.TOKEN_EXPIRY]  = session.expiresAt
        }
    }

    /** Clear all stored tokens, wipe the in-memory cache, and reset the sync cursor. */
    suspend fun clearSession() {
        cachedToken = null
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.TOKEN_EXPIRY)
            // Reset the incremental pull cursor so the next user gets a full pull
            // instead of inheriting the previous session's sync timestamp.
            prefs.remove(Keys.LAST_SYNC_AT)
        }
    }

    /** Returns the current session, or null if not signed in. */
    suspend fun getSession(): AuthSession? = sessionFlow.firstOrNull()

    /** Returns the refresh token for token-renewal, or null if no session. */
    suspend fun getRefreshToken(): String? =
        dataStore.data.map { it[Keys.REFRESH_TOKEN] }.firstOrNull()

    /** Persist the access token returned by a refresh — keeps other fields intact. */
    suspend fun updateAccessToken(accessToken: String, expiresAt: Long) {
        cachedToken = accessToken
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.TOKEN_EXPIRY] = expiresAt
        }
    }

    /**
     * Records the server-side ISO 8601 timestamp of the last successful sync.
     *
     * [isoTimestamp] should come from the [updated_at] field of the most recently
     * pulled row (the maximum `updated_at` across all entities), not from the device clock.
     * This keeps the pull cursor aligned to the Supabase server clock, preventing clock-skew
     * gaps where some remote changes could be missed.
     */
    suspend fun setLastSyncAt(isoTimestamp: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_AT] = isoTimestamp
        }
    }

}

/**
 * An authenticated Supabase session.
 *
 * @param expiresAt Epoch milliseconds at which the [accessToken] expires.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAt: Long
)
