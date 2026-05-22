package com.app.nutriai.data.local.mapper

import com.app.nutriai.data.local.entity.CatalogEntity
import com.app.nutriai.domain.model.Catalog

/**
 * Extension functions mapping between [CatalogEntity] (data layer) and [Catalog] (domain layer).
 */

fun CatalogEntity.toDomain(): Catalog = Catalog(
    id = id,
    userId = userId,
    name = name,
    lastModifiedAt = lastModifiedAt,
    deletedAt = deletedAt
)

fun Catalog.toEntity(isSynced: Boolean = false): CatalogEntity = CatalogEntity(
    id = id,
    userId = userId,
    name = name,
    lastModifiedAt = lastModifiedAt,
    isSynced = isSynced,
    deletedAt = deletedAt
)
