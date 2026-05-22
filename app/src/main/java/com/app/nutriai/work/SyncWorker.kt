package com.app.nutriai.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.nutriai.domain.usecase.SyncDataUseCase
import com.app.nutriai.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "SyncWorker"

/**
 * WorkManager [CoroutineWorker] that performs background cloud sync.
 *
 * Scheduled periodically (every [com.app.nutriai.util.Constants.SYNC_INTERVAL_HOURS] hours)
 * with a [androidx.work.NetworkType.CONNECTED] constraint — only runs when the device
 * has any network connection.
 *
 * Uses [SyncDataUseCase] which resolves the current auth state and delegates to
 * [com.app.nutriai.domain.repository.SyncRepository.syncAll].
 *
 * Returns [Result.retry()] on transient network failures (HTTP 503, timeout, etc.)
 * so WorkManager applies exponential back-off. Returns [Result.failure()] on
 * permanent errors (not signed in, invalid credentials) so WorkManager stops retrying.
 *
 * Annotated with [@HiltWorker] + [@AssistedInject] — Hilt generates the
 * worker factory automatically (registered in [SupabaseModule]).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDataUseCase: SyncDataUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Background sync started (attempt ${runAttemptCount + 1})")

        return when (val result = syncDataUseCase()) {
            is Resource.Success -> {
                Log.i(TAG, "Background sync completed successfully")
                Result.success()
            }
            is Resource.Error -> {
                val message = result.message ?: "Unknown sync error"
                Log.w(TAG, "Sync error: $message")

                // Don't retry for auth errors — user must sign in again
                if (message.contains("Not signed in", ignoreCase = true) ||
                    message.contains("Session expired", ignoreCase = true)) {
                    Log.i(TAG, "Sync skipped — not authenticated")
                    Result.failure()
                } else {
                    // Transient error — let WorkManager retry with back-off
                    Log.w(TAG, "Sync failed — will retry: $message")
                    Result.retry()
                }
            }
            is Resource.Loading -> Result.retry()
        }
    }
}
