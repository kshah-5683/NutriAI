package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.repository.AuthRepository
import com.app.nutriai.util.Resource
import javax.inject.Inject

/**
 * Validates credentials and delegates sign-in to [AuthRepository].
 *
 * Input validation:
 *  - Email must be non-blank and contain '@'.
 *  - Password must be at least 6 characters (Supabase minimum).
 *
 * Returns [Resource.Error] for invalid input before making any network call.
 */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<AuthState> {
        if (email.isBlank()) return Resource.Error("Email cannot be empty")
        if (!email.contains('@')) return Resource.Error("Please enter a valid email address")
        if (password.isBlank()) return Resource.Error("Password cannot be empty")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters")
        return authRepository.signIn(email.trim(), password)
    }
}
