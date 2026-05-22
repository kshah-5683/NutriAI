package com.app.nutriai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.nutriai.data.local.entity.LabelPhotoEntity

/**
 * DAO for [LabelPhotoEntity] — local nutrition label photo metadata.
 *
 * Used by:
 * - [com.app.nutriai.util.ImageCompressor] to insert a new photo record after saving the file.
 * - [com.app.nutriai.domain.usecase.CleanupLabelPhotosUseCase] to find and delete expired photos.
 *
 * Phase 11: Nutrition Label Scanner.
 */
@Dao
interface LabelPhotoDao {

    /** Insert a new photo metadata record. REPLACE on conflict (shouldn't happen with UUID PKs). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: LabelPhotoEntity)

    /**
     * Retrieve all photos created before [cutoffTimestamp] (epoch millis).
     * Used by the cleanup job to find photos older than the 10-day TTL.
     */
    @Query("SELECT * FROM label_photos WHERE created_at < :cutoffTimestamp")
    suspend fun getPhotosOlderThan(cutoffTimestamp: Long): List<LabelPhotoEntity>

    /** Delete a single photo metadata record by ID. */
    @Query("DELETE FROM label_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)

    /** Batch-delete multiple photo metadata records. */
    @Query("DELETE FROM label_photos WHERE id IN (:ids)")
    suspend fun deletePhotos(ids: List<String>)
}
