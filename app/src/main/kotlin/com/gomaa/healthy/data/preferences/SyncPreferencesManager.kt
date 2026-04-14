package com.gomaa.healthy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instance
private val Context.syncPrefs: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

/**
 * Data class holding sync preferences.
 */
data class SyncPreferences(
    val masterSyncEnabled: Boolean = true,
    val syncStepsEnabled: Boolean = true,
    val syncExerciseEnabled: Boolean = true,
    val syncHeartRateEnabled: Boolean = true
)

/**
 * Manages sync preferences using DataStore.
 */
@Singleton
class SyncPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.syncPrefs

    // Preference keys
    private val MasterSyncEnabled = booleanPreferencesKey("master_sync_enabled")
    private val StepsSyncEnabled = booleanPreferencesKey("sync_steps_enabled")
    private val ExerciseSyncEnabled = booleanPreferencesKey("sync_exercise_enabled")
    private val HeartRateSyncEnabled = booleanPreferencesKey("sync_heart_rate_enabled")

    /**
     * Flow of sync preferences. Emits whenever preferences change.
     */
    val preferencesFlow: Flow<SyncPreferences> = dataStore.data.map { prefs ->
        SyncPreferences(
            masterSyncEnabled = prefs[MasterSyncEnabled] ?: true,
            syncStepsEnabled = prefs[StepsSyncEnabled] ?: true,
            syncExerciseEnabled = prefs[ExerciseSyncEnabled] ?: true,
            syncHeartRateEnabled = prefs[HeartRateSyncEnabled] ?: true
        )
    }

    /**
     * Get current preferences (one-shot read).
     */
    suspend fun getPreferences(): SyncPreferences {
        return preferencesFlow.first()
    }

    /**
     * Update master sync toggle.
     */
    suspend fun setMasterSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MasterSyncEnabled] = enabled
        }
    }

    /**
     * Update steps sync toggle.
     */
    suspend fun setStepsSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[StepsSyncEnabled] = enabled
        }
    }

    /**
     * Update exercise sync toggle.
     */
    suspend fun setExerciseSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ExerciseSyncEnabled] = enabled
        }
    }

    /**
     * Update heart rate sync toggle.
     */
    suspend fun setHeartRateSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[HeartRateSyncEnabled] = enabled
        }
    }
}
