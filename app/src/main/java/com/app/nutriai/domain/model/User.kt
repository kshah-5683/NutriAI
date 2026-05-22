package com.app.nutriai.domain.model

/**
 * Domain model representing a user.
 * Pure Kotlin data class - no framework dependencies.
 */
data class User(
    val id: String,
    val email: String,
    val createdAt: Long
)
