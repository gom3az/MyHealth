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

private val Context.appPrefs: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

data class AppPreferences(
    val isFirstRun: Boolean = true,
    val showMigrationPrompt: Boolean = true,
    val hasRunMigration: Boolean = false
)

@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.appPrefs

    private val IsFirstRun = booleanPreferencesKey("is_first_run")
    private val ShowMigrationPrompt = booleanPreferencesKey("show_migration_prompt")
    private val HasRunMigration = booleanPreferencesKey("has_run_migration")

    val preferencesFlow: Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            isFirstRun = prefs[IsFirstRun] ?: true,
            showMigrationPrompt = prefs[ShowMigrationPrompt] ?: true,
            hasRunMigration = prefs[HasRunMigration] ?: false
        )
    }

    suspend fun getPreferences(): AppPreferences {
        return preferencesFlow.first()
    }

    suspend fun setFirstRunComplete() {
        dataStore.edit { prefs ->
            prefs[IsFirstRun] = false
        }
    }

    suspend fun setMigrationComplete() {
        dataStore.edit { prefs ->
            prefs[HasRunMigration] = true
            prefs[ShowMigrationPrompt] = false
        }
    }

    suspend fun skipMigration() {
        dataStore.edit { prefs ->
            prefs[ShowMigrationPrompt] = false
        }
    }
}