package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.User

/**
 * Repository interface for user operations.
 * Implementations live in the data layer (data/repository/).
 */
interface UserRepository {
    suspend fun getUserById(id: String): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun insertUser(user: User)
}
