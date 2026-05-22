package com.app.nutriai.domain.model

/**
 * Represents the current authentication state of the user.
 *
 * [Loading]         — Initial state while the persisted session is being
 *                     read from DataStore on app start.
 * [Unauthenticated] — No active Supabase session. App operates in offline
 *                     mode using [com.app.nutriai.util.Constants.LOCAL_USER_ID].
 * [Authenticated]   — A valid Supabase session exists. Cloud sync is enabled.
 *                     [userId] is the Supabase UUID; [email] is the account email.
 */
sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String
    ) : AuthState()
}
