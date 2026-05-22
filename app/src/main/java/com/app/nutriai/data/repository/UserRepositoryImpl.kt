package com.app.nutriai.data.repository

import com.app.nutriai.data.local.dao.UserDao
import com.app.nutriai.data.local.mapper.toDomain
import com.app.nutriai.data.local.mapper.toEntity
import com.app.nutriai.domain.model.User
import com.app.nutriai.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [UserRepository] backed by Room via [UserDao].
 * Injected by Hilt as a singleton.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomain()
    }

    override suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)?.toDomain()
    }

    override suspend fun insertUser(user: User) {
        userDao.insertUser(user.toEntity())
    }
}
