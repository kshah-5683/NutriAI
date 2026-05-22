package com.app.nutriai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity tracking metadata for locally-stored nutrition label photos.
 *
 * Photos themselves are stored as JPEG files in [android.content.Context.filesDir]/label_photos/.
 * This table only holds metadata so the cleanup job can efficiently query rows
 * older than the 10-day TTL without scanning the filesystem.
 *
 * Key constraints:
 * - Photos are NEVER synced to Supabase — purely local transient artifacts.
 * - [filePath] stores the absolute path to the compressed JPEG file.
 * - Rows (and their files) are purged when [createdAt] < now - 10 days.
 *
 * Phase 11: Nutrition Label Scanner.
 */
@Entity(tableName = "label_photos")
data class LabelPhotoEntity(
    @PrimaryKey
    val id: String,

    /** Absolute path to the JPEG file on disk, e.g., /data/user/0/.../files/label_photos/uuid.jpg */
    @ColumnInfo(name = "file_path")
    val filePath: String,

    /** Epoch millis when the photo was saved — used for TTL cleanup. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** How the photo was obtained: "camera" or "gallery". Informational only. */
    @ColumnInfo(name = "source_type")
    val sourceType: String
)
