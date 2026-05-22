package com.app.nutriai.domain.model

/**
 * Domain model representing a user's food catalog.
 * A catalog groups related food items (e.g., "Personal Catalog", "Favorites").
 */
data class Catalog(
    val id: String,
    val userId: String,
    val name: String,
    val lastModifiedAt: Long,
    val deletedAt: Long? = null
)
