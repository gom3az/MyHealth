package com.gomaa.healthy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    val syncHeartRateEnabled: Boolean = true,
    val syncWindowDays: Int = 1  // Default: 24 hours
)

/**
 * Manages sync preferences using DataStore.
 * Includes Health Kit sync window and last sync timestamps.
 */
@Singleton
class SyncPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.syncPrefs

    // Preference keys for sync toggles
    private val MasterSyncEnabled = booleanPreferencesKey("master_sync_enabled")
    private val StepsSyncEnabled = booleanPreferencesKey("sync_steps_enabled")
    private val ExerciseSyncEnabled = booleanPreferencesKey("sync_exercise_enabled")
    private val HeartRateSyncEnabled = booleanPreferencesKey("sync_heart_rate_enabled")

    // Preference key for Health Kit sync window (in days)
    private val SyncWindowDays = intPreferencesKey("sync_window_days")

    // Preference keys for last Health Kit sync timestamps (per data type for incremental sync)
    private val LastStepsSync = longPreferencesKey("last_steps_sync")
    private val LastHeartRateSync = longPreferencesKey("last_heart_rate_sync")
    private val LastWorkoutSync = longPreferencesKey("last_workout_sync")
    private val LastHealthKitSync = longPreferencesKey("last_healthkit_sync")

    companion object {
        // Valid sync window options
        val SYNC_WINDOW_OPTIONS = listOf(1, 7, 30)  // 1 day, 7 days, 30 days

        fun getSyncWindowLabel(days: Int): String = when (days) {
            1 -> "Last 24 hours"
            7 -> "Last 7 days"
            30 -> "Last 30 days"
            else -> "Last $days days"
        }
    }

    /**
     * Flow of sync preferences. Emits whenever preferences change.
     */
    val preferencesFlow: Flow<SyncPreferences> = dataStore.data.map { prefs ->
        SyncPreferences(
            masterSyncEnabled = prefs[MasterSyncEnabled] ?: true,
            syncStepsEnabled = prefs[StepsSyncEnabled] ?: true,
            syncExerciseEnabled = prefs[ExerciseSyncEnabled] ?: true,
            syncHeartRateEnabled = prefs[HeartRateSyncEnabled] ?: true,
            syncWindowDays = prefs[SyncWindowDays] ?: 1
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

    /**
     * Get the sync window in days.
     * Default is 1 (24 hours).
     */
    suspend fun getSyncWindowDays(): Int {
        return dataStore.data.first()[SyncWindowDays] ?: 1
    }

    /**
     * Set the sync window in days.
     * Valid values: 1, 7, 30
     */
    suspend fun setSyncWindowDays(days: Int) {
        if (days !in SYNC_WINDOW_OPTIONS) {
            throw IllegalArgumentException("Invalid sync window: $days. Valid options: $SYNC_WINDOW_OPTIONS")
        }
        dataStore.edit { prefs ->
            prefs[SyncWindowDays] = days
        }
    }

    /**
     * Get last sync timestamp for steps data.
     */
    suspend fun getLastStepsSyncTime(): Long? {
        val time = dataStore.data.first()[LastStepsSync]
        return if (time == -1L) null else time
    }

    /**
     * Set last sync timestamp for steps data.
     */
    suspend fun setLastStepsSyncTime(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LastStepsSync] = timestamp
        }
    }

    /**
     * Get last sync timestamp for heart rate data.
     */
    suspend fun getLastHeartRateSyncTime(): Long? {
        val time = dataStore.data.first()[LastHeartRateSync]
        return if (time == -1L) null else time
    }

    /**
     * Set last sync timestamp for heart rate data.
     */
    suspend fun setLastHeartRateSyncTime(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LastHeartRateSync] = timestamp
        }
    }

    /**
     * Get last sync timestamp for workout data.
     */
    suspend fun getLastWorkoutSyncTime(): Long? {
        val time = dataStore.data.first()[LastWorkoutSync]
        return if (time == -1L) null else time
    }

    /**
     * Set last sync timestamp for workout data.
     */
    suspend fun setLastWorkoutSyncTime(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LastWorkoutSync] = timestamp
        }
    }

    /**
     * Get last general Health Kit sync timestamp.
     */
    suspend fun getLastHealthKitSyncTime(): Long? {
        val time = dataStore.data.first()[LastHealthKitSync]
        return if (time == -1L) null else time
    }

    /**
     * Set last general Health Kit sync timestamp.
     */
    suspend fun setLastHealthKitSyncTime(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LastHealthKitSync] = timestamp
        }
    }
}
