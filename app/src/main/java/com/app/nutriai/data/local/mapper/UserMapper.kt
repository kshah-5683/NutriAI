package com.app.nutriai.data.local.mapper

import com.app.nutriai.data.local.entity.UserEntity
import com.app.nutriai.domain.model.User

/**
 * Extension functions mapping between [UserEntity] (data layer) and [User] (domain layer).
 */

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    createdAt = createdAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    createdAt = createdAt
)
