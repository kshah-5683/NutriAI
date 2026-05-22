package com.app.nutriai.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.nutriai.util.Constants
import java.util.concurrent.TimeUnit

/**
 * Schedules and cancels the periodic background cloud sync job.
 *
 * Uses [ExistingPeriodicWorkPolicy.KEEP] so rescheduling on every app launch
 * does not reset the timer for an already-enqueued job.
 *
 * Constraints:
 *  - Network connected ([NetworkType.CONNECTED]) — sync only when online.
 *  - No battery or storage constraints to keep sync reliable.
 *
 * Back-off: linear 10-minute back-off on [SyncWorker.Result.retry] (network
 * failures), capped by WorkManager's default 5-hour maximum.
 *
 * Usage:
 *  ```kotlin
 *  // In NutriAiApplication or when the user signs in:
 *  SyncScheduler.schedule(applicationContext)
 *
 *  // When the user signs out:
 *  SyncScheduler.cancel(applicationContext)
 *  ```
 */
object SyncScheduler {

    /**
     * Enqueues the periodic sync if not already scheduled.
     * Safe to call on every app launch — [ExistingPeriodicWorkPolicy.KEEP] is a no-op
     * if a pending job already exists.
     */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = Constants.SYNC_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.LINEAR,
                backoffDelay = 10L,
                timeUnit = TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.SYNC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Cancels the periodic sync job.
     * Called when the user signs out so sync stops until they sign in again.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.SYNC_WORK_TAG)
    }
}
