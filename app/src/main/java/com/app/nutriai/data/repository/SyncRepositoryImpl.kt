package com.app.nutriai.data.repository

import android.util.Log
import com.app.nutriai.data.local.dao.CatalogDao
import com.app.nutriai.data.local.dao.DailyLogDao
import com.app.nutriai.data.local.dao.FoodItemDao
import com.app.nutriai.data.local.dao.UserDao
import com.app.nutriai.data.local.dao.UserPreferencesDao
import com.app.nutriai.data.local.entity.UserEntity
import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.data.remote.api.SupabaseDbApiService
import com.app.nutriai.data.remote.dto.RemoteCatalogDto
import com.app.nutriai.data.remote.dto.RemoteDailyLogDto
import com.app.nutriai.data.remote.dto.RemoteFoodItemDto
import com.app.nutriai.data.remote.dto.RemoteUserDto
import com.app.nutriai.data.remote.dto.RemoteUserPreferencesDto
import com.app.nutriai.data.remote.dto.toEntity
import com.app.nutriai.data.remote.dto.toRemoteDto
import com.app.nutriai.domain.repository.SyncRepository
import com.app.nutriai.util.Constants
import com.app.nutriai.util.Resource
import kotlinx.coroutines.flow.first
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncRepositoryImpl"
private const val PAGE_SIZE = 500 // conservative — well below PostgREST's 1,000-row default

/**
 * Implementation of [SyncRepository] using the Supabase PostgREST REST API.
 *
 * ## ID translation
 * Local Room entities use fixed IDs for catalogs:
 *   - [Constants.INGREDIENT_CATALOG_ID] = "local_user_ingredients"
 *   - [Constants.RECIPE_CATALOG_ID]     = "local_user_recipes"
 *   - User ID                            = [Constants.LOCAL_USER_ID] = "local_user"
 *
 * Supabase uses the real Supabase user UUID and prefixed catalog IDs:
 *   - Remote catalog ID = "{supabaseUserId}_{localCatalogId}"
 *   - Remote user ID    = Supabase auth UUID
 *
 * Food item and daily log IDs are UUIDs generated locally — no translation needed.
 *
 * ## Sync strategy
 * - **Push**: Fetch unsynced rows → upsert to Supabase → mark as synced locally.
 * - **Pull**: Fetch all remote rows → upsert to Room with LWW (Last-Write-Wins).
 * - Empty batches skip the HTTP call entirely.
 * - Failures on individual entity types are logged but do not abort the entire sync —
 *   the caller receives [Resource.Error] only if ALL entity types fail.
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val dbApiService: SupabaseDbApiService,
    private val catalogDao: CatalogDao,
    private val foodItemDao: FoodItemDao,
    private val dailyLogDao: DailyLogDao,
    private val userDao: UserDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val authPreferences: AuthPreferences
) : SyncRepository {

    // ─── Public API ───────────────────────────────────────────────────────

    override suspend fun syncAll(supabaseUserId: String): Resource<Unit> {
        val pushResult = pushLocalChanges(supabaseUserId)
        val pullResult = pullRemoteChanges(supabaseUserId)

        // Post-pull push: recalculateLogsForFoodItem (called during pull) may have
        // marked daily logs as unsynced. Push them now so Supabase gets the
        // corrected totals in the same sync cycle instead of waiting for the next one.
        if (pullResult is Resource.Success) {
            val postPullResult = pushLocalChanges(supabaseUserId)
            if (postPullResult is Resource.Error) {
                Log.w(TAG, "Post-pull push failed: ${postPullResult.message}")
            }
        }

        // Advance the pull cursor ONLY when BOTH directions succeed.
        // Cursor is updated inside pullRemoteChanges() on success; syncAll just logs the outcome.
        return when {
            pushResult is Resource.Success && pullResult is Resource.Success -> {
                Log.i(TAG, "Full sync complete for user $supabaseUserId")
                Resource.Success(Unit)
            }
            pushResult is Resource.Error && pullResult is Resource.Error ->
                Resource.Error("Sync failed — push: ${pushResult.message}, pull: ${pullResult.message}")
            pushResult is Resource.Error -> {
                // Pull succeeded; push will retry on next sync (dirty rows stay isSynced=false)
                Log.w(TAG, "Push failed but pull succeeded: ${pushResult.message}")
                Resource.Success(Unit)
            }
            else -> {
                // Push succeeded; pull cursor NOT advanced — avoids missing remote changes
                Log.w(TAG, "Pull failed but push succeeded: ${(pullResult as Resource.Error).message}")
                Resource.Success(Unit)
            }
        }
    }

    // ─── Push (local → Supabase) ─────────────────────────────────────────

    override suspend fun pushLocalChanges(supabaseUserId: String): Resource<Unit> {
        Log.d(TAG, "Starting push for user $supabaseUserId")
        var anyError = false

        // 1. Ensure user profile row exists in Supabase
        upsertRemoteUser(supabaseUserId)

        // 2. Push unsynced catalogs
        val unsyncedCatalogs = catalogDao.getUnsyncedCatalogs()
        if (unsyncedCatalogs.isNotEmpty()) {
            val remoteCatalogs = unsyncedCatalogs.map { catalog ->
                val remoteId = buildRemoteCatalogId(supabaseUserId, catalog.id)
                catalog.toRemoteDto(supabaseUserId, remoteId)
            }
            val response = runCatching { dbApiService.upsertCatalogs(body = remoteCatalogs) }
                .getOrNull()
            if (response?.isSuccessful == true) {
                catalogDao.markAsSynced(unsyncedCatalogs.map { it.id })
                Log.d(TAG, "Pushed ${unsyncedCatalogs.size} catalogs")
            } else {
                Log.w(TAG, "Catalog push failed: ${response?.code()}")
                anyError = true
            }
        }

        // 3. Push unsynced food items
        val unsyncedFoods = foodItemDao.getUnsyncedFoods()
        if (unsyncedFoods.isNotEmpty()) {
            val remoteFoods = unsyncedFoods.map { food ->
                val remoteCatalogId = buildRemoteCatalogId(supabaseUserId, food.catalogId)
                food.toRemoteDto(remoteCatalogId)
            }
            val response = runCatching { dbApiService.upsertFoodItems(body = remoteFoods) }
                .getOrNull()
            if (response?.isSuccessful == true) {
                foodItemDao.markAsSynced(unsyncedFoods.map { it.id })
                Log.d(TAG, "Pushed ${unsyncedFoods.size} food items")
            } else {
                Log.w(TAG, "Food items push failed: ${response?.code()}")
                anyError = true
            }
        }

        // 4. Push unsynced daily logs (includes tombstones).
        //    Orphaned logs (food_item_id = NULL after SET_NULL purge) are filtered out —
        //    Supabase's NOT NULL constraint on food_item_id would reject them, and they
        //    have already been synced before the purge nullified their FK.
        val unsyncedLogs = dailyLogDao.getUnsyncedLogs().first()
        val pushableLogs = unsyncedLogs.filter { it.foodItemId != null }
        if (pushableLogs.isNotEmpty()) {
            val remoteLogs = pushableLogs.map { it.toRemoteDto(supabaseUserId) }
            val response = runCatching { dbApiService.upsertDailyLogs(body = remoteLogs) }
                .getOrNull()
            if (response?.isSuccessful == true) {
                dailyLogDao.markAsSynced(pushableLogs.map { it.id })
                Log.d(TAG, "Pushed ${pushableLogs.size} daily logs")
            } else {
                Log.w(TAG, "Daily logs push failed: ${response?.code()}")
                anyError = true
            }
        }
        // Mark orphaned logs as synced so they don't accumulate in the unsynced queue.
        val orphanedLogs = unsyncedLogs.filter { it.foodItemId == null }
        if (orphanedLogs.isNotEmpty()) {
            dailyLogDao.markAsSynced(orphanedLogs.map { it.id })
            Log.d(TAG, "Marked ${orphanedLogs.size} orphaned logs (null food_item_id) as synced")
        }

        // 5. Push unsynced user preferences (macro goals — Phase 14)
        val unsyncedPrefs = userPreferencesDao.getUnsyncedPreferences(Constants.LOCAL_USER_ID)
        if (unsyncedPrefs != null) {
            val remotePrefs = unsyncedPrefs.toRemoteDto(supabaseUserId)
            val response = runCatching {
                dbApiService.upsertUserPreferences(body = listOf(remotePrefs))
            }.getOrNull()
            if (response?.isSuccessful == true) {
                userPreferencesDao.markAsSynced(Constants.LOCAL_USER_ID)
                Log.d(TAG, "Pushed user preferences (macro goals)")
            } else {
                Log.w(TAG, "User preferences push failed: ${response?.code()}")
                anyError = true
            }
        }

        // 6. Purge synced tombstones from all entity types.
        //    Order matters — daily logs first (no FK children), then food items, then catalogs.
        //
        //    With the v4→v5 migration, daily_logs.food_item_id uses ON DELETE SET NULL
        //    instead of CASCADE. Hard-deleting a food item or catalog now safely nullifies
        //    the FK in daily_logs instead of cascade-deleting the log rows, preserving
        //    historical entries with their stored snapshot macros and food name.
        if (!anyError) {
            dailyLogDao.purgeSyncedTombstones()
            foodItemDao.purgeSyncedTombstones()
            catalogDao.purgeSyncedTombstones()
            Log.d(TAG, "Tombstone purge complete")
        }

        return if (anyError)
            Resource.Error("One or more entity types failed to push")
        else
            Resource.Success(Unit)
    }

    // ─── Pull (Supabase → local) ─────────────────────────────────────────

    override suspend fun pullRemoteChanges(supabaseUserId: String): Resource<Unit> {
        Log.d(TAG, "Starting pull for user $supabaseUserId")
        var anyError = false

        // Cursor is a server-side ISO 8601 timestamp (updated_at column).
        // Null on first install or after data wipe — triggers a full paginated pull.
        val lastSyncAt: String? = authPreferences.lastSyncAtFlow.first()

        // Guard: force a full pull when the local DB is empty OR partially restored.
        //
        // Two scenarios require a forced full pull despite a non-null cursor:
        //  a) Fresh install / Room wipe: catalogCount == 0 — everything is missing.
        //  b) Partial restore: catalogs exist but daily_logs are empty — a previous pull
        //     crashed after inserting catalogs/foods but before finishing daily logs.
        //     The cursor was NOT advanced (pull failed), yet incremental mode would still
        //     skip logs that haven't changed since the old cursor value.
        //
        // Food items are always fully fetched regardless (see section 2 below), so they
        // don't need a separate sentinel.
        val catalogCount = catalogDao.getCatalogCount()
        val logCount     = dailyLogDao.getNonDeletedLogCount()
        val localIsEmpty = catalogCount == 0 || logCount == 0
        val isIncremental = lastSyncAt != null && !localIsEmpty
        val updatedAtFilter = if (isIncremental) "gt.$lastSyncAt" else null
        Log.d(TAG, when {
            isIncremental          -> "Incremental pull since $lastSyncAt"
            catalogCount == 0      -> "Full pull (local DB empty — cursor ignored)"
            logCount == 0          -> "Full pull (daily logs missing — partial restore detected)"
            else                   -> "Full pull (first sync / cursor reset)"
        })

        // 1. Catalogs — always fetch all (typically 2–3 rows; not worth filtering).
        //    This ensures we always have the full catalog ID set for the food item query.
        val remoteCatalogs: List<RemoteCatalogDto> = runCatching {
            dbApiService.getCatalogs(userIdFilter = "eq.$supabaseUserId").body().orEmpty()
        }.getOrElse {
            Log.w(TAG, "Catalog pull failed: ${it.message}")
            anyError = true
            emptyList()
        }

        // 2. Food items — always fetch ALL, regardless of cursor.
        //    Food items are the FK parent of daily_logs.  Filtering by updated_at in
        //    incremental mode could leave a gap: a daily log updated since lastSyncAt
        //    may reference a food item created before lastSyncAt that is absent from
        //    the local DB (e.g. after a Room wipe or tombstone purge) — causing a FK
        //    constraint violation on insert.  A user's catalog is typically a few
        //    hundred items at most, so a full paginated pull on every sync is cheap
        //    compared to the correctness risk.
        val remoteFoods: List<RemoteFoodItemDto> = if (remoteCatalogs.isNotEmpty()) {
            val catalogIdFilter = "in.(${remoteCatalogs.joinToString(",") { it.id }})"
            runCatching {
                fetchAllPaginated { limit, offset ->
                    dbApiService.getFoodItems(
                        catalogIdFilter = catalogIdFilter,
                        limit = limit,
                        offset = offset
                    )
                }
            }.getOrElse {
                Log.w(TAG, "Food items pull failed: ${it.message}")
                anyError = true
                emptyList()
            }
        } else emptyList()

        // 3. Daily logs — always fetch ALL via paginated full pull.
        //    Like food items, daily logs must always be complete before conflict resolution
        //    runs.  An incremental filter (updated_at > cursor) can silently leave gaps
        //    when the local DB was partially wiped or a previous pull crashed mid-flight —
        //    the missing entries never appear because they haven't changed since the cursor.
        //    A user's log history is bounded (paginated at 500 rows) and cheap to re-sync.
        val remoteLogs: List<RemoteDailyLogDto> = runCatching {
            fetchAllPaginated { limit, offset ->
                dbApiService.getDailyLogs(
                    userIdFilter = "eq.$supabaseUserId",
                    limit = limit,
                    offset = offset
                )
            }
        }.getOrElse {
            Log.w(TAG, "Daily logs pull failed: ${it.message}")
            anyError = true
            emptyList()
        }

        // 4. Apply pulled data to Room using delete-wins + LWW.
        //
        //    Uses *IncludingDeleted queries so locally soft-deleted rows are visible to the
        //    conflict resolver — otherwise getCatalogById returns null for deleted rows,
        //    making them indistinguishable from "row never existed".
        //
        //    Conflict resolution rules (in priority order):
        //      a) Remote row is NEW (no local copy at all)    → insert
        //      b) Remote row is DELETED                       → delete wins; apply tombstone locally
        //      c) Local row is DELETED, remote is NOT         → skip; do NOT un-delete locally
        //      d) Incremental pull (updated_at filter applied) → always apply; the updated_at
        //         cursor guarantees this row changed since last sync. Skipping on equal
        //         last_modified_at would miss Supabase-Dashboard edits (which don't touch
        //         last_modified_at, only updated_at via Postgres trigger).
        //      e) Full pull (no cursor)                       → LWW by lastModifiedAt
        // ── Conflict resolution: INSERT for new rows, UPDATE for existing rows ──────────────
        //
        // CRITICAL: Do NOT use insertCatalog / insertFood with OnConflictStrategy.REPLACE
        // for rows that already exist.  SQLite's REPLACE strategy internally DELETEs the
        // existing row and INSERTs a new one, which fires the ON DELETE CASCADE chain:
        //
        //   catalog REPLACED → food_items cascade-deleted → daily_logs cascade-deleted
        //
        // Fix: use UPDATE (SQL UPDATE statement — no DELETE, no CASCADE trigger) for
        // existing rows.  Only use INSERT for rows that are genuinely new (local == null).
        //
        // ── Full-pull update rule ────────────────────────────────────────────────────────
        //
        // When isIncremental = false (cursor not yet set, or DB was empty), the original
        // LWW guard (remote.lastModifiedAt > local.lastModifiedAt) silently skips updates
        // whenever the timestamps are equal — e.g. after push-then-pull on the same device,
        // or after a direct Supabase Dashboard edit (which updates updated_at via Postgres
        // trigger but does NOT touch last_modified_at).
        //
        // Fix: when the local row is already synced (isSynced = true) it has no pending
        // local changes to protect.  Always overwrite it with the remote version.
        // The LWW guard is only needed when isSynced = false (a locally-edited-but-not-yet-
        // pushed row): in that case, keep local if it is newer; apply remote if remote wins.

        remoteCatalogs.forEach { remote ->
            val localId = stripRemoteCatalogIdPrefix(supabaseUserId, remote.id)
            val local = catalogDao.getCatalogByIdIncludingDeleted(localId)
            val entity = remote.toEntity(localId, Constants.LOCAL_USER_ID)
            when {
                local == null            -> catalogDao.insertCatalog(entity)    // new row: INSERT safe
                remote.deletedAt != null -> catalogDao.updateCatalog(entity)    // tombstone: UPDATE
                local.deletedAt != null  -> { /* local delete wins — skip remote edit */ }
                isIncremental            -> catalogDao.updateCatalog(entity)    // cursor guarantees change
                local.isSynced           -> catalogDao.updateCatalog(entity)    // no local unsent changes
                remote.lastModifiedAt > local.lastModifiedAt -> catalogDao.updateCatalog(entity) // LWW
            }
        }

        remoteFoods.forEach { remote ->
            val localCatalogId = stripRemoteCatalogIdPrefix(supabaseUserId, remote.catalogId)
            val local = foodItemDao.getFoodByIdIncludingDeleted(remote.id)
            val entity = remote.toEntity(localCatalogId)
            val didUpdate: Boolean = when {
                local == null            -> { foodItemDao.insertFood(entity); true  }  // new row
                remote.deletedAt != null -> { foodItemDao.updateFood(entity); false }  // tombstone — do NOT recalculate
                local.deletedAt != null  -> false                                      // local delete wins
                isIncremental            -> { foodItemDao.updateFood(entity); true  }  // cursor guarantees change
                local.isSynced           -> { foodItemDao.updateFood(entity); true  }  // no local unsent changes
                remote.lastModifiedAt > local.lastModifiedAt -> { foodItemDao.updateFood(entity); true }  // LWW
                else                     -> false
            }
            // Cascade: recalculate stored macro totals on all daily logs referencing this food
            // item so that sync push sends correct values to other devices.
            // Skipped for tombstones (food is being deleted — its macros are irrelevant) and
            // when the food item was not actually changed (local delete wins / LWW lost).
            if (didUpdate && entity.deletedAt == null) {
                dailyLogDao.recalculateLogsForFoodItem(
                    foodItemId   = entity.id,
                    baseCalories = entity.baseCalories,
                    baseProtein  = entity.baseProtein,
                    baseCarbs    = entity.baseCarbs,
                    baseFat      = entity.baseFat
                )
            }
        }

        remoteLogs.forEach { remote ->
            // FK safety guard: skip this log if the referenced food item is not in the local DB.
            // This can happen when a catalog fetch fails mid-sync (remoteFoods = emptyList) but
            // the daily-log fetch succeeds — inserting would violate food_item_id → food_items FK.
            // Also skip logs with null food_item_id (shouldn't happen from Supabase, but defensive).
            val remoteFoodId = remote.foodItemId
            if (remoteFoodId == null || foodItemDao.getFoodByIdIncludingDeleted(remoteFoodId) == null) {
                Log.w(TAG, "Skipping log ${remote.id} — food_item_id ${remote.foodItemId} not in local DB")
                return@forEach
            }

            val local = dailyLogDao.getLogByIdIncludingDeleted(remote.id)
            val entity = remote.toEntity(Constants.LOCAL_USER_ID)
            when {
                local == null            -> dailyLogDao.insertLog(entity)
                remote.deletedAt != null -> dailyLogDao.updateLog(entity)       // tombstone: UPDATE
                local.deletedAt != null  -> { /* local delete wins — skip remote edit */ }
                isIncremental            -> dailyLogDao.updateLog(entity)       // cursor guarantees change
                local.isSynced           -> dailyLogDao.updateLog(entity)       // no local unsent changes
                remote.lastModifiedAt >= local.lastModifiedAt -> dailyLogDao.updateLog(entity) // LWW
            }
        }

        // 5. Pull user preferences (macro goals — Phase 14).
        //    Single row per user. Simple overwrite when no local unsent changes exist.
        val remotePrefs: RemoteUserPreferencesDto? = runCatching {
            dbApiService.getUserPreferences(userIdFilter = "eq.$supabaseUserId")
                .body()?.firstOrNull()
        }.getOrElse {
            Log.w(TAG, "User preferences pull failed: ${it.message}")
            anyError = true
            null
        }

        if (remotePrefs != null) {
            val localPrefs = userPreferencesDao.getPreferences(Constants.LOCAL_USER_ID)
            when {
                localPrefs == null -> {
                    // No local row — insert the remote version
                    userPreferencesDao.upsertPreferences(remotePrefs.toEntity(Constants.LOCAL_USER_ID))
                    Log.d(TAG, "Pulled user preferences (first sync — inserted)")
                }
                localPrefs.isSynced -> {
                    // No pending local changes — always accept remote
                    userPreferencesDao.upsertPreferences(remotePrefs.toEntity(Constants.LOCAL_USER_ID))
                    Log.d(TAG, "Pulled user preferences (overwrite — no local changes)")
                }
                else -> {
                    // Local has unsent changes — keep local (push will send them next cycle)
                    Log.d(TAG, "Pulled user preferences — skipped (local has unsent changes)")
                }
            }
        }

        // 6. Advance the pull cursor using only daily log updated_at values.
        //
        //    Catalogs and food items are always fetched in full (no cursor filter), so
        //    their updated_at values may be OLDER than the current cursor — including them
        //    would regress the cursor and cause repeated re-fetches of already-seen data.
        //
        //    The cursor should only move forward.  If no logs were returned (empty result
        //    or all logs predated the cursor) the cursor stays at its current value.
        //
        //    Only advance when all entity types succeeded.
        if (!anyError) {
            val latestLogUpdatedAt = remoteLogs.mapNotNull { it.updatedAt }.maxOrNull()

            if (latestLogUpdatedAt != null) {
                // Only write if it's genuinely newer than the current cursor.
                val current = lastSyncAt
                if (current == null || latestLogUpdatedAt > current) {
                    authPreferences.setLastSyncAt(latestLogUpdatedAt)
                    Log.d(TAG, "Pull cursor advanced to $latestLogUpdatedAt")
                } else {
                    Log.d(TAG, "Pull cursor unchanged — no newer logs (latest=$latestLogUpdatedAt, cursor=$current)")
                }
            } else {
                Log.d(TAG, "Pull cursor unchanged — no log updated_at values in response")
            }
        }

        Log.d(TAG, "Pull complete — ${remoteCatalogs.size} catalogs, ${remoteFoods.size} foods, ${remoteLogs.size} logs")
        return if (anyError)
            Resource.Error("One or more entity types failed to pull")
        else
            Resource.Success(Unit)
    }

    /**
     * Fetches all pages from a paginated PostgREST endpoint using LIMIT/OFFSET.
     *
     * Stops when a page returns fewer rows than [pageSize] (signals last page).
     * Throws on HTTP error — callers should wrap in [runCatching].
     *
     * @param pageSize Rows per page. Defaults to [PAGE_SIZE] (500), safely below
     *                 PostgREST's 1,000-row default limit.
     */
    private suspend fun <T> fetchAllPaginated(
        pageSize: Int = PAGE_SIZE,
        fetcher: suspend (limit: Int, offset: Int) -> Response<List<T>>
    ): List<T> {
        val all = mutableListOf<T>()
        var offset = 0
        do {
            val response = fetcher(pageSize, offset)
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
            val page = response.body().orEmpty()
            all.addAll(page)
            offset += pageSize
        } while (page.size == pageSize)
        return all
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    /**
     * Builds the remote catalog ID by prefixing the local ID with the Supabase user UUID.
     * Prevents PRIMARY KEY collisions when multiple users share the same local catalog IDs.
     */
    private fun buildRemoteCatalogId(supabaseUserId: String, localCatalogId: String): String =
        "${supabaseUserId}${Constants.REMOTE_CATALOG_ID_SEPARATOR}${localCatalogId}"

    /**
     * Strips the UUID prefix from a remote catalog ID to recover the local catalog ID.
     * Falls back to the full remote ID if parsing fails (defensive).
     */
    private fun stripRemoteCatalogIdPrefix(supabaseUserId: String, remoteCatalogId: String): String {
        val prefix = "${supabaseUserId}${Constants.REMOTE_CATALOG_ID_SEPARATOR}"
        return if (remoteCatalogId.startsWith(prefix))
            remoteCatalogId.removePrefix(prefix)
        else
            remoteCatalogId // fallback — shouldn't happen with correct RLS
    }

    /** Best-effort upsert of the user's profile row in Supabase. */
    private suspend fun upsertRemoteUser(supabaseUserId: String) {
        val localUser = userDao.getUserById(Constants.LOCAL_USER_ID) ?: return
        runCatching {
            dbApiService.upsertUser(
                body = RemoteUserDto(
                    id        = supabaseUserId,
                    email     = localUser.email,
                    createdAt = localUser.createdAt
                )
            )
        }.onFailure { Log.w(TAG, "User upsert failed (non-fatal): ${it.message}") }
    }
}
