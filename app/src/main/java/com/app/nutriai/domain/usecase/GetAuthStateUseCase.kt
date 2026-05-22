package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Exposes the [AuthRepository.getAuthStateFlow] observable to the presentation layer.
 *
 * ViewModels should collect this flow to react to sign-in / sign-out events
 * without holding a direct reference to the repository.
 */
class GetAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> = authRepository.getAuthStateFlow()
}
