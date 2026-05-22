package com.app.nutriai.util

/**
 * Result of a pull-to-refresh or foreground sync attempt.
 *
 * [SYNCED]        — sync ran (success or network failure — will retry later).
 * [THROTTLED]     — last sync was less than 5 minutes ago; no network call made.
 * [NOT_SIGNED_IN] — user is not authenticated; sync was skipped.
 * [NO_INTERNET]   — device has no validated internet connection.
 * [ERROR]         — sync ran but returned an unexpected error message.
 */
enum class SyncTriggerResult(val message: String) {
    SYNCED("Sync complete"),
    THROTTLED("Already up to date"),
    NOT_SIGNED_IN("Sign in to enable sync"),
    NO_INTERNET("No internet connection"),
    ERROR("Sync failed — will retry later")
}
