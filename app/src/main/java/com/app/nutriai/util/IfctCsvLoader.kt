package com.app.nutriai.util

import android.content.Context
import android.util.Log
import com.app.nutriai.data.local.dao.IfctFoodDao
import com.app.nutriai.data.local.entity.IfctFoodEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent seeder that loads [ifct2017.csv] from the app's assets folder
 * into the [ifct_foods] Room table on first launch.
 *
 * Call [seedIfNeeded] once at startup (e.g. from [NutritionRepositoryImpl] or
 * [Application.onCreate]). If the table already has rows, the method returns
 * immediately without reading the file.
 *
 * CSV format (header row + data rows):
 * ```
 * code,name,energy_kcal,protein_g,fat_g,carbs_g,fiber_g
 * G001,Rice raw,345,7.9,1.0,78.2,0.6
 * ```
 *
 * Malformed rows are skipped with a warning log — they never crash the app.
 *
 * Phase 5.5: Part of the offline IFCT 2017 fallback nutrition source.
 */
@Singleton
class IfctCsvLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ifctFoodDao: IfctFoodDao
) {

    /**
     * Seeds the database if it is empty.
     * Safe to call multiple times — no-ops after the first successful seed.
     * Must be called from a coroutine; runs on [Dispatchers.IO].
     */
    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val existingCount = ifctFoodDao.count()
        if (existingCount > 0) {
            Log.d(TAG, "IFCT already seeded ($existingCount rows). Skipping.")
            return@withContext
        }

        Log.d(TAG, "Seeding IFCT 2017 from assets/$CSV_FILE_NAME …")
        val entities = parsecsv()
        if (entities.isEmpty()) {
            Log.w(TAG, "No valid rows parsed from $CSV_FILE_NAME — table not seeded.")
            return@withContext
        }

        ifctFoodDao.insertAll(entities)
        Log.d(TAG, "IFCT seeded: ${entities.size} foods inserted.")
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun parsecsv(): List<IfctFoodEntity> {
        val entities = mutableListOf<IfctFoodEntity>()
        var lineNumber = 0

        try {
            context.assets.open(CSV_FILE_NAME).bufferedReader().use { reader ->
                // Skip header
                reader.readLine()
                lineNumber = 1

                reader.forEachLine { line ->
                    lineNumber++
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@forEachLine

                    val entity = parseLine(trimmed, lineNumber)
                    if (entity != null) entities.add(entity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open/read $CSV_FILE_NAME: ${e.message}", e)
        }

        return entities
    }

    private fun parseLine(line: String, lineNumber: Int): IfctFoodEntity? {
        val parts = line.split(",")
        if (parts.size < 7) {
            Log.w(TAG, "Line $lineNumber has ${parts.size} columns (expected 7) — skipped: $line")
            return null
        }

        return try {
            IfctFoodEntity(
                code       = parts[0].trim(),
                name       = parts[1].trim(),
                energyKcal = parts[2].trim().toDouble(),
                proteinG   = parts[3].trim().toDouble(),
                fatG       = parts[4].trim().toDouble(),
                carbsG     = parts[5].trim().toDouble(),
                fiberG     = parts[6].trim().toDouble()
            )
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Line $lineNumber number parse error — skipped: $line", e)
            null
        }
    }

    companion object {
        private const val TAG = "IfctCsvLoader"
        private const val CSV_FILE_NAME = "ifct2017.csv"
    }
}
