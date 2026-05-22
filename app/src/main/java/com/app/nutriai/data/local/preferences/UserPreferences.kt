package com.app.nutriai.data.local.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.app.nutriai.data.local.dao.UserPreferencesDao
import com.app.nutriai.data.local.entity.UserPreferencesEntity
import com.app.nutriai.data.sync.SyncEntityType
import com.app.nutriai.data.sync.SyncPushManager
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserPreferences"

/**
 * Persists user macro goal preferences.
 *
 * **Phase 14 migration:** Backing store changed from DataStore-only to Room
 * ([UserPreferencesDao]) with Supabase bidirectional sync. Room is the single
 * read source — all UI consumers observe [macroGoalsFlow] which reads from Room.
 *
 * On first launch after the Phase 14 update, existing DataStore values (if any)
 * are migrated to Room once, then DataStore goal keys become dead code.
 *
 * Separated from [AuthPreferences] so that ViewModels that only need macro goals
 * (e.g., HomeViewModel, InsightsViewModel) don't depend on auth-scoped classes.
 */
@Singleton
class UserPreferences @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao,
    private val syncPushManager: SyncPushManager,
    private val dataStore: DataStore<Preferences>
) {

    /** Legacy DataStore keys — used only for one-time migration. */
    private object LegacyKeys {
        val CALORIE_GOAL = floatPreferencesKey("calorie_goal")
        val PROTEIN_GOAL = floatPreferencesKey("protein_goal")
        val CARBS_GOAL   = floatPreferencesKey("carbs_goal")
        val FAT_GOAL     = floatPreferencesKey("fat_goal")
    }

    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // One-time migration: DataStore → Room
        migrationScope.launch { migrateFromDataStoreIfNeeded() }
    }

    /**
     * Emits the user's current daily macro goals from Room.
     * Returns [MacroGoals] defaults when no row exists (first launch).
     */
    val macroGoalsFlow: Flow<MacroGoals> =
        userPreferencesDao.getPreferencesFlow(Constants.LOCAL_USER_ID).map { entity ->
            entity?.toMacroGoals() ?: MacroGoals()
        }

    /**
     * Persist updated macro goals to Room and trigger a debounced push to Supabase.
     */
    suspend fun saveMacroGoals(goals: MacroGoals) {
        val entity = UserPreferencesEntity(
            userId = Constants.LOCAL_USER_ID,
            calorieGoal = goals.calorieGoal,
            proteinGoal = goals.proteinGoal,
            carbsGoal = goals.carbsGoal,
            fatGoal = goals.fatGoal,
            isSynced = false,
            lastModifiedAt = System.currentTimeMillis()
        )
        userPreferencesDao.upsertPreferences(entity)
        syncPushManager.schedulePush(SyncEntityType.USER_PREFERENCES, listOf(Constants.LOCAL_USER_ID))
    }

    // ─── One-time DataStore → Room migration ────────────────────────────

    /**
     * If Room has no preferences row but DataStore has non-default goals,
     * seed Room from DataStore. This preserves goals set before Phase 14.
     * After migration, DataStore goal keys are cleared (dead code cleanup).
     */
    private suspend fun migrateFromDataStoreIfNeeded() {
        try {
            val existing = userPreferencesDao.getPreferences(Constants.LOCAL_USER_ID)
            if (existing != null) return  // Room already has data — nothing to migrate

            val defaults = MacroGoals()
            val prefs = dataStore.data.first()
            val dsCalorie = prefs[LegacyKeys.CALORIE_GOAL]
            val dsProtein = prefs[LegacyKeys.PROTEIN_GOAL]
            val dsCarbs   = prefs[LegacyKeys.CARBS_GOAL]
            val dsFat     = prefs[LegacyKeys.FAT_GOAL]

            // Only migrate if at least one value was explicitly set (non-null)
            if (dsCalorie != null || dsProtein != null || dsCarbs != null || dsFat != null) {
                val entity = UserPreferencesEntity(
                    userId = Constants.LOCAL_USER_ID,
                    calorieGoal = dsCalorie?.toDouble() ?: defaults.calorieGoal,
                    proteinGoal = dsProtein?.toDouble() ?: defaults.proteinGoal,
                    carbsGoal = dsCarbs?.toDouble() ?: defaults.carbsGoal,
                    fatGoal = dsFat?.toDouble() ?: defaults.fatGoal,
                    isSynced = false,  // push to Supabase on next sync
                    lastModifiedAt = System.currentTimeMillis()
                )
                userPreferencesDao.upsertPreferences(entity)
                Log.i(TAG, "Migrated macro goals from DataStore to Room")

                // Clean up legacy DataStore keys
                dataStore.edit { mutablePrefs ->
                    mutablePrefs.remove(LegacyKeys.CALORIE_GOAL)
                    mutablePrefs.remove(LegacyKeys.PROTEIN_GOAL)
                    mutablePrefs.remove(LegacyKeys.CARBS_GOAL)
                    mutablePrefs.remove(LegacyKeys.FAT_GOAL)
                }
                Log.d(TAG, "Cleared legacy DataStore goal keys")
            }
        } catch (e: Exception) {
            Log.w(TAG, "DataStore → Room migration failed (non-fatal): ${e.message}")
        }
    }
}
