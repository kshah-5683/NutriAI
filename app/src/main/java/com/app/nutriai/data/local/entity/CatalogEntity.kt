package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a food catalog in the local database.
 * Maps to the "catalogs" table.
 *
 * Foreign key to [UserEntity] ensures referential integrity.
 * Soft-delete support via [deletedAt] (null = active, non-null = deleted timestamp).
 */
@Entity(
    tableName = "catalogs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class CatalogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
