package com.app.nutriai.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.repository.AuthRepository
import com.app.nutriai.domain.repository.SyncRepository
import com.app.nutriai.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Triggers a full bidirectional sync with Supabase.
 *
 * Returns [Resource.Error] immediately if:
 *  - The device has no active internet connection.
 *  - The user is not authenticated.
 *
 * Delegates to [SyncRepository.syncAll] for the actual network operations.
 *
 * Called by:
 *  - [com.app.nutriai.work.SyncWorker] — periodic background sync (every 24 h on Wi-Fi).
 *  - [com.app.nutriai.presentation.screens.auth.AuthViewModel] — manual sync from Profile screen.
 *  - [SignInUseCase] (via AuthViewModel) — first sync after sign-in.
 */
class SyncDataUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): Resource<Unit> {
        // Fast-fail if there is no active internet connection.
        // WorkManager already enforces NetworkType.CONNECTED for background sync;
        // this check covers the manual "Sync Now" button when offline.
        if (!isNetworkAvailable()) {
            return Resource.Error("No internet connection")
        }

        val authState = authRepository.getAuthStateFlow().first()
        if (authState !is AuthState.Authenticated) {
            return when (authState) {
                is AuthState.Unauthenticated -> Resource.Error("Not signed in — sync skipped")
                else -> Resource.Error("Auth state still loading — sync skipped")
            }
        }

        // Pre-emptively refresh JWT if it expires within 5 minutes.
        // This prevents the first post-expiry sync from failing silently.
        val refreshResult = authRepository.refreshSessionIfNeeded()
        if (refreshResult is Resource.Error) {
            // Refresh token itself expired — session has been cleared, force re-auth
            return Resource.Error(refreshResult.message ?: "Session expired — please sign in again")
        }

        return syncRepository.syncAll(authState.userId)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
