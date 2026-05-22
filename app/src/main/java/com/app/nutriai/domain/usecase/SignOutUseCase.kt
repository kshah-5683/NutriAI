package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.repository.AuthRepository
import com.app.nutriai.domain.repository.SyncRepository
import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.util.Resource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Signs the user out and cancels any pending background sync.
 *
 * Before signing out, a final sync push is attempted so the user's latest
 * changes are not lost. If the push fails, sign-out proceeds anyway (the
 * unsynced data remains in Room for the next session).
 *
 * Local Room data is preserved — the app continues to work offline after
 * sign-out.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        // Best-effort final push — don't block sign-out on failure
        val currentState = authRepository.getAuthStateFlow().first()
        if (currentState is AuthState.Authenticated) {
            runCatching {
                syncRepository.pushLocalChanges(currentState.userId)
            }
        }
        return authRepository.signOut()
    }
}
