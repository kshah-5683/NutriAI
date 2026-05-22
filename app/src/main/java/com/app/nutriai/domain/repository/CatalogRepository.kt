package com.app.nutriai.domain.repository

import com.app.nutriai.domain.model.Catalog
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for catalog operations.
 * Implementations live in the data layer (data/repository/).
 */
interface CatalogRepository {
    fun getCatalogsByUserId(userId: String): Flow<List<Catalog>>
    suspend fun getCatalogById(id: String): Catalog?
    suspend fun insertCatalog(catalog: Catalog)
    suspend fun updateCatalog(catalog: Catalog)
    suspend fun softDeleteCatalog(id: String)
}
