package com.app.nutriai.data.repository

import com.app.nutriai.data.local.dao.CatalogDao
import com.app.nutriai.data.local.mapper.toDomain
import com.app.nutriai.data.local.mapper.toEntity
import com.app.nutriai.data.sync.SyncEntityType
import com.app.nutriai.data.sync.SyncPushManager
import com.app.nutriai.domain.model.Catalog
import com.app.nutriai.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CatalogRepository] backed by Room via [CatalogDao].
 * Injected by Hilt as a singleton.
 *
 * Flow-based queries emit automatically when the underlying table changes.
 * Every mutation schedules a debounced push-on-write via [SyncPushManager].
 */
@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val catalogDao: CatalogDao,
    private val syncPushManager: SyncPushManager
) : CatalogRepository {

    override fun getCatalogsByUserId(userId: String): Flow<List<Catalog>> {
        return catalogDao.getCatalogsByUserId(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCatalogById(id: String): Catalog? {
        return catalogDao.getCatalogById(id)?.toDomain()
    }

    override suspend fun insertCatalog(catalog: Catalog) {
        catalogDao.insertCatalog(catalog.toEntity())
        syncPushManager.schedulePush(SyncEntityType.CATALOG, listOf(catalog.id))
    }

    override suspend fun updateCatalog(catalog: Catalog) {
        catalogDao.updateCatalog(catalog.toEntity())
        syncPushManager.schedulePush(SyncEntityType.CATALOG, listOf(catalog.id))
    }

    override suspend fun softDeleteCatalog(id: String) {
        catalogDao.softDeleteCatalog(id)
        syncPushManager.schedulePush(SyncEntityType.CATALOG, listOf(id))
    }
}
