package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Contract for Supabase authentication operations.
 *
 * All mutations return [Resource] so the caller can react to success/error
 * without catching exceptions.  [getAuthStateFlow] is a cold observable stream
 * that emits whenever the session changes (app launch, sign-in, sign-out,
 * background token refresh).
 *
 * Implementations:
 *  - [com.app.nutriai.data.repository.AuthRepositoryImpl]
 *    → GoTrue REST API + [com.app.nutriai.data.local.preferences.AuthPreferences] (DataStore)
 */
interface AuthRepository {

    /**
     * Cold [Flow] of the current [AuthState].
     *
     * Emissions:
     *  - [AuthState.Loading]         on app start while the DataStore session is read.
     *  - [AuthState.Unauthenticated] when no valid session is persisted.
     *  - [AuthState.Authenticated]   when a valid session exists.
     *
     * Collectors stay active for the lifetime of the subscriber — the flow
     * never completes on its own.
     */
    fun getAuthStateFlow(): Flow<AuthState>

    /**
     * Register a new user with [email] and [password].
     * On success, the session is persisted and [getAuthStateFlow] transitions
     * to [AuthState.Authenticated].
     */
    suspend fun signUp(email: String, password: String): Resource<AuthState>

    /**
     * Sign in an existing user with [email] and [password].
     * On success, the session is persisted and [getAuthStateFlow] transitions
     * to [AuthState.Authenticated].
     */
    suspend fun signIn(email: String, password: String): Resource<AuthState>

    /**
     * Revoke the current session on Supabase and clear the local token.
     * [getAuthStateFlow] transitions to [AuthState.Unauthenticated] on success.
     * The local Room data is NOT wiped — the app continues in offline mode.
     */
    suspend fun signOut(): Resource<Unit>

    /**
     * Exchange the stored refresh token for a new access token.
     * Called automatically by the OkHttp authenticator on 401 responses;
     * may also be called proactively on app foreground if the token is near expiry.
     */
    suspend fun refreshSession(): Resource<Unit>

    /**
     * Refresh the access token only if it is about to expire (within [bufferMs] ms).
     * Returns [Resource.Success] immediately if the token is still fresh or if no
     * session exists (unauthenticated — nothing to refresh).
     * Returns [Resource.Error] only if a refresh was attempted and failed, which
     * signals the caller to stop and prompt the user to sign in again.
     *
     * @param bufferMs how far before expiry to proactively refresh (default 5 min)
     */
    suspend fun refreshSessionIfNeeded(bufferMs: Long = 5 * 60 * 1_000L): Resource<Unit>
}
