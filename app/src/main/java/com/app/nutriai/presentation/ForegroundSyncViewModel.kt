package com.app.nutriai.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.data.sync.SyncThrottleManager
import com.app.nutriai.util.SyncTriggerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ForegroundSyncViewModel"

/**
 * Handles foreground sync triggered when the app returns to the foreground.
 *
 * Delegates throttle logic and sync execution to [SyncThrottleManager], which is
 * a shared singleton also used by [com.app.nutriai.presentation.screens.home.HomeViewModel]
 * for pull-to-refresh — ensuring both call sites respect the same 5-minute window.
 *
 * ## Usage
 * Call [pullIfThrottled] from [MainActivity.onResume] (via a LifecycleObserver).
 */
@HiltViewModel
class ForegroundSyncViewModel @Inject constructor(
    private val syncThrottleManager: SyncThrottleManager
) : ViewModel() {

    /**
     * Fire-and-forget foreground pull. Used by [MainActivity.onResume].
     * Delegates entirely to [SyncThrottleManager.triggerSync] — throttle check,
     * execution, and logging are all handled there.
     */
    fun pullIfThrottled() {
        viewModelScope.launch {
            val result = syncThrottleManager.triggerSync()
            if (result == SyncTriggerResult.THROTTLED) {
                Log.v(TAG, "Foreground pull skipped (throttled)")
            } else {
                Log.d(TAG, "Foreground pull result: $result")
            }
        }
    }
}
