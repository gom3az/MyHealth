package com.gomaa.healthy.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPreferencesTest {

    @Test
    fun `default values are all enabled`() {
        val prefs = SyncPreferences()
        assertTrue(prefs.masterSyncEnabled)
        assertTrue(prefs.syncStepsEnabled)
        assertTrue(prefs.syncExerciseEnabled)
        assertTrue(prefs.syncHeartRateEnabled)
    }

    @Test
    fun `custom values are applied correctly`() {
        val prefs = SyncPreferences(
            masterSyncEnabled = false,
            syncStepsEnabled = false,
            syncExerciseEnabled = true,
            syncHeartRateEnabled = true
        )
        assertFalse(prefs.masterSyncEnabled)
        assertFalse(prefs.syncStepsEnabled)
        assertTrue(prefs.syncExerciseEnabled)
        assertTrue(prefs.syncHeartRateEnabled)
    }

    @Test
    fun `copy creates new instance with updated values`() {
        val original = SyncPreferences()
        val updated = original.copy(masterSyncEnabled = false)

        assertFalse(updated.masterSyncEnabled)
        assertTrue(updated.syncStepsEnabled)
        assertTrue(original.masterSyncEnabled)
    }
}