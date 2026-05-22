package com.app.nutriai.domain.repository

import com.app.nutriai.util.Resource

/**
 * Contract for bidirectional cloud sync between Room (local) and Supabase (remote).
 *
 * Sync is user-scoped — [supabaseUserId] must be the authenticated user's UUID.
 * Calling sync while unauthenticated returns [Resource.Error].
 *
 * The implementation handles ID translation between the local Room schema
 * (which uses the pre-auth [com.app.nutriai.util.Constants.LOCAL_USER_ID]) and the
 * Supabase schema (which uses the real UUID from GoTrue).
 *
 * Implementation:
 *  - [com.app.nutriai.data.repository.SyncRepositoryImpl]
 */
interface SyncRepository {

    /**
     * Push all locally unsynced records (catalogs, food items, daily logs) to Supabase.
     *
     * For each entity:
     *  1. Fetch rows where [isSynced = false] from the local DAO.
     *  2. Map to remote DTOs (replace local user/catalog IDs with remote IDs).
     *  3. Upsert to Supabase (merge-duplicates strategy — safe to retry).
     *  4. On HTTP 2xx: mark the rows as synced locally.
     *
     * Soft-deleted records (tombstones) are also pushed so deletions propagate
     * to other devices.
     */
    suspend fun pushLocalChanges(supabaseUserId: String): Resource<Unit>

    /**
     * Pull remote records from Supabase and upsert them into Room.
     *
     * For each entity:
     *  1. Fetch all rows for [supabaseUserId] from Supabase.
     *  2. Apply Last-Write-Wins: only update local if remote [lastModifiedAt] is newer.
     *  3. Apply remote soft-deletes — mark local rows as deleted if [deletedAt] != null.
     *
     * Called after a successful push to propagate changes from other devices.
     */
    suspend fun pullRemoteChanges(supabaseUserId: String): Resource<Unit>

    /**
     * Full bidirectional sync: push first, then pull.
     * This is the method called by [com.app.nutriai.work.SyncWorker].
     */
    suspend fun syncAll(supabaseUserId: String): Resource<Unit>
}
