package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.repository.AuthRepository
import com.app.nutriai.util.Resource
import javax.inject.Inject

/**
 * Delegates sign-in with Google ID Token to [AuthRepository].
 *
 * Returns [Resource.Error] if the ID Token is empty.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Resource<AuthState> {
        if (idToken.isBlank()) return Resource.Error("Google ID Token is empty")
        return authRepository.signInWithGoogle(idToken)
    }
}
