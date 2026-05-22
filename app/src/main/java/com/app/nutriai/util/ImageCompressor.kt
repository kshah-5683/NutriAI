package com.app.nutriai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.app.nutriai.data.local.dao.LabelPhotoDao
import com.app.nutriai.data.local.entity.LabelPhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for compressing, base64-encoding, and persisting nutrition label photos.
 *
 * Responsibilities:
 * 1. Load the image from the provided URI via ContentResolver
 * 2. Scale down to at most [MAX_DIMENSION]px on the longest side (preserving aspect ratio)
 * 3. Compress as JPEG at [JPEG_QUALITY]% quality → ~100–200KB
 * 4. Base64-encode the bytes for inline transmission to the Gemini API
 * 5. Save the compressed JPEG to [filesDir]/label_photos/{uuid}.jpg
 * 6. Insert a [LabelPhotoEntity] row into Room for TTL-based cleanup
 *
 * Uses [ApplicationContext] (injected by Hilt) to avoid Activity Context leaks.
 *
 * Phase 11: Nutrition Label Scanner.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val labelPhotoDao: LabelPhotoDao
) {

    companion object {
        /** Maximum pixel dimension (width or height) after scaling. */
        private const val MAX_DIMENSION = 1024

        /** JPEG compression quality (0–100). 80% yields ~100–200KB for typical label photos. */
        private const val JPEG_QUALITY = 80

        /** MIME type used in the Gemini API `inlineData` part. */
        const val MIME_TYPE = "image/jpeg"

        /** Subdirectory inside filesDir for storing compressed label photos. */
        private const val PHOTO_DIR = "label_photos"
    }

    /**
     * Loads, compresses, and saves a label photo from [uri].
     *
     * @param uri         URI of the selected/captured image (content:// or file://)
     * @param sourceType  How the photo was obtained: "gallery" or "camera"
     * @return [CompressedPhoto] with base64 bytes and the saved [LabelPhotoEntity]
     * @throws IllegalStateException if the bitmap cannot be decoded from the URI
     */
    suspend fun compressAndSave(uri: Uri, sourceType: String): CompressedPhoto =
        withContext(Dispatchers.IO) {
            // 1. Decode to Bitmap from URI
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: throw IllegalStateException("Could not open image from URI: $uri")

            // 2. Scale down if needed (preserve aspect ratio)
            val scaled = scaleBitmap(bitmap)

            // 3. Compress to JPEG bytes
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
            val jpegBytes = baos.toByteArray()

            // 4. Base64-encode
            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            // 5. Save to filesDir/label_photos/{uuid}.jpg
            val photoDir = File(context.filesDir, PHOTO_DIR).also { it.mkdirs() }
            val uuid = UUID.randomUUID().toString()
            val photoFile = File(photoDir, "$uuid.jpg")
            FileOutputStream(photoFile).use { it.write(jpegBytes) }

            // 6. Insert metadata row into Room
            val entity = LabelPhotoEntity(
                id = uuid,
                filePath = photoFile.absolutePath,
                createdAt = System.currentTimeMillis(),
                sourceType = sourceType
            )
            labelPhotoDao.insertPhoto(entity)

            CompressedPhoto(
                base64 = base64,
                mimeType = MIME_TYPE,
                entity = entity
            )
        }

    /** Scale [bitmap] so its longest side is at most [MAX_DIMENSION] px. No-op if already smaller. */
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return bitmap

        val scale = MAX_DIMENSION.toFloat() / maxOf(w, h)
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}

/**
 * Result of a successful [ImageCompressor.compressAndSave] call.
 *
 * @param base64   Base64-encoded JPEG bytes (no data-URI prefix) — ready for Gemini API
 * @param mimeType Always "image/jpeg"
 * @param entity   The [LabelPhotoEntity] row inserted into Room
 */
data class CompressedPhoto(
    val base64: String,
    val mimeType: String,
    val entity: LabelPhotoEntity
)
