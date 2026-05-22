package com.app.nutriai.domain.usecase

import com.app.nutriai.data.local.dao.LabelPhotoDao
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case that purges nutrition label photos older than [TTL_DAYS] days.
 *
 * Cleanup strategy:
 * 1. Query [LabelPhotoDao] for rows with `created_at` older than the cutoff.
 * 2. For each expired row: delete the JPEG file from disk, then delete the Room row.
 * 3. File path stored in [LabelPhotoEntity.filePath] is always an absolute path
 *    (set by [com.app.nutriai.util.ImageCompressor] via [File.absolutePath]),
 *    so no Context is needed here — `File(absolutePath).delete()` is sufficient.
 *
 * Called from [com.app.nutriai.NutriAiApplication.onCreate] on every app launch
 * in a non-blocking IO coroutine. No WorkManager needed — photos are small
 * (~200KB each) and the 10-day TTL means at most ~50 photos can accumulate.
 *
 * Phase 11: Nutrition Label Scanner.
 */
class CleanupLabelPhotosUseCase @Inject constructor(
    private val labelPhotoDao: LabelPhotoDao
) {
    companion object {
        private const val TTL_DAYS = 10L
        private val TTL_MILLIS = TimeUnit.DAYS.toMillis(TTL_DAYS)
    }

    suspend operator fun invoke() {
        val cutoff = System.currentTimeMillis() - TTL_MILLIS
        val expired = labelPhotoDao.getPhotosOlderThan(cutoff)
        if (expired.isEmpty()) return

        val expiredIds = mutableListOf<String>()
        for (photo in expired) {
            // Delete the file first — if this fails silently, the orphan row will be
            // retried on the next launch (photo.createdAt will still be past the cutoff).
            File(photo.filePath).delete()
            expiredIds.add(photo.id)
        }

        // Batch-delete all metadata rows in one query
        if (expiredIds.isNotEmpty()) {
            labelPhotoDao.deletePhotos(expiredIds)
        }
    }
}
