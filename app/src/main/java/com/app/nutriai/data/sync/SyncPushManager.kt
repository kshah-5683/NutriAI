package com.app.nutriai.data.sync

import android.util.Log
import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncPushManager"
private const val DEBOUNCE_MS = 500L

/**
 * Triggers an immediate (debounced) push to Supabase whenever a local mutation occurs.
 *
 * ## How it works
 * Repositories call [schedulePush] after every DAO mutation.  Multiple calls within
 * [DEBOUNCE_MS] (500 ms) are coalesced — e.g., an AI parse that inserts 8 ingredients
 * at once produces a single batch upsert instead of 8 individual HTTP calls.
 *
 * After the debounce window, [SyncRepository.pushLocalChanges] is invoked.  It reads all
 * rows with `is_synced = false`, upserts them to Supabase in a single batch per entity
 * type, and marks them synced — so the dirty-flag mechanism handles the actual batching.
 *
 * ## Failure handling
 * Network failures are swallowed silently: the dirty flag (`is_synced = false`) remains
 * set, so the next periodic sync (24 h via [SyncWorker]) or foreground pull will retry.
 *
 * The internal coroutine scope uses [SupervisorJob] so a failed push does not cancel
 * subsequent push jobs.
 */
@Singleton
class SyncPushManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authPreferences: AuthPreferences
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Internal signal channel. Each [schedulePush] call emits Unit here.
     * [extraBufferCapacity] = 1 ensures the first emission is never dropped while
     * the collector is still initializing.
     * [onBufferOverflow] = DROP_OLDEST means rapid bursts collapse to a single signal.
     */
    private val pushSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            pushSignal
                .debounce(DEBOUNCE_MS)
                .collect { performPush() }
        }
    }

    /**
     * Schedules a push for the given [entityType] and [ids].
     *
     * This call is fire-and-forget and never suspends. Multiple rapid calls are debounced
     * into a single push after [DEBOUNCE_MS] of inactivity.
     *
     * @param entityType The type of entity that was mutated (informational; logged only).
     * @param ids        IDs of the mutated rows (informational; dirty-flag handles targeting).
     */
    fun schedulePush(entityType: SyncEntityType, ids: List<String>) {
        Log.v(TAG, "schedulePush: $entityType ids=${ids.take(3)}${if (ids.size > 3) "…" else ""}")
        pushSignal.tryEmit(Unit)
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private suspend fun performPush() {
        val session = authPreferences.getSession()
        if (session == null) {
            Log.d(TAG, "Push skipped — no active session")
            return
        }
        Log.d(TAG, "Executing debounced push for user ${session.userId}")
        val result = syncRepository.pushLocalChanges(session.userId)
        if (result is com.app.nutriai.util.Resource.Error) {
            Log.w(TAG, "Debounced push failed (will retry on next sync): ${result.message}")
        }
    }
}

/**
 * Entity types that can be pushed on-write.
 * Used for logging and future routing to targeted push endpoints.
 */
enum class SyncEntityType {
    CATALOG,
    FOOD_ITEM,
    DAILY_LOG,
    USER_PREFERENCES
}
