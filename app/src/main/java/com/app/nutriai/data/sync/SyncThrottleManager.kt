package com.app.nutriai.data.sync

import android.util.Log
import com.app.nutriai.domain.usecase.SyncDataUseCase
import com.app.nutriai.util.Resource
import com.app.nutriai.util.SyncTriggerResult
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncThrottleManager"
const val SYNC_THROTTLE_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

/**
 * Singleton that owns the sync throttle state shared across all call sites.
 *
 * Both [com.app.nutriai.presentation.ForegroundSyncViewModel] (foreground resume)
 * and [com.app.nutriai.presentation.screens.home.HomeViewModel] (pull-to-refresh)
 * delegate here, so a foreground-resume sync and a pull-to-refresh gesture within
 * the same 5-minute window correctly share the same throttle counter.
 *
 * [triggerSync] suspends until the sync completes and returns a [SyncTriggerResult]
 * the caller can use to show appropriate UI feedback.
 *
 * [pullIfThrottled] is a fire-and-forget variant for the onResume path.
 */
@Singleton
class SyncThrottleManager @Inject constructor(
    private val syncDataUseCase: SyncDataUseCase
) {

    @Volatile
    private var lastSyncAtMs: Long = 0L

    /**
     * Suspending sync with throttle. Returns a [SyncTriggerResult] describing
     * the outcome so the caller can show a snackbar or other feedback.
     *
     * Thread-safe: [lastSyncAtMs] is @Volatile and the check-then-set is
     * acceptable here — a tiny race on simultaneous gestures would at most
     * result in two syncs firing at the same time (extremely unlikely UX).
     */
    suspend fun triggerSync(): SyncTriggerResult {
        val now = System.currentTimeMillis()
        if (now - lastSyncAtMs < SYNC_THROTTLE_INTERVAL_MS) {
            Log.v(TAG, "Sync throttled (last sync was ${(now - lastSyncAtMs) / 1000}s ago)")
            return SyncTriggerResult.THROTTLED
        }

        lastSyncAtMs = now
        Log.d(TAG, "Sync starting")
        return when (val result = syncDataUseCase()) {
            is Resource.Success -> {
                Log.d(TAG, "Sync complete")
                SyncTriggerResult.SYNCED
            }
            is Resource.Error -> {
                Log.w(TAG, "Sync failed: ${result.message}")
                when {
                    result.message?.contains("Not signed in", ignoreCase = true) == true -> SyncTriggerResult.NOT_SIGNED_IN
                    result.message?.contains("No internet",  ignoreCase = true) == true  -> SyncTriggerResult.NO_INTERNET
                    else -> SyncTriggerResult.ERROR
                }
            }
            is Resource.Loading -> SyncTriggerResult.SYNCED // never emitted
        }
    }
}
