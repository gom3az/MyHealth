package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictResolverTest {

    @Test
    fun `shouldApplySteps returns true when no local data exists`() {
        val result = ConflictResolver.shouldApplySteps(null)
        assertTrue(result)
    }

    @Test
    fun `shouldApplySteps returns false when local MY_HEALTH data exists`() {
        val localSteps = DailyStepsEntity(
            date = 0,
            totalSteps = 1000,
            totalDistanceMeters = 0.0,
            activeMinutes = 0,
            lightActivityMinutes = 0,
            moderateActivityMinutes = 0,
            vigorousActivityMinutes = 0,
            source = SOURCE_MY_HEALTH,
            syncedToHc = 1
        )
        val result = ConflictResolver.shouldApplySteps(localSteps)
        assertFalse(result)
    }

    @Test
    fun `shouldApplySteps returns true when HC data exists from different source`() {
        // HC data can coexist with existing HC data from same source
        val hcSteps = DailyStepsEntity(
            date = 0,
            totalSteps = 500,
            totalDistanceMeters = 0.0,
            activeMinutes = 0,
            lightActivityMinutes = 0,
            moderateActivityMinutes = 0,
            vigorousActivityMinutes = 0,
            source = SOURCE_HEALTH_CONNECT,
            syncedToHc = 1
        )
        val result = ConflictResolver.shouldApplySteps(hcSteps)
        assertTrue(result)  // HC data can coexist
    }

    @Test
    fun `shouldApplyExercise returns false when HC record already synced`() {
        val existingByHcId = ExerciseSessionEntity(
            id = "existing-id",
            startTime = 1000L,
            endTime = 2000L,
            avgHeartRate = 120,
            maxHeartRate = 150,
            minHeartRate = 80,
            deviceBrand = "Huawei",
            source = SOURCE_HEALTH_CONNECT,
            healthConnectRecordId = "hc-record-id",
            syncedToHc = 1
        )
        val result = ConflictResolver.shouldApplyExercise(
            hcRecordId = "hc-record-id",
            existingByHcId = existingByHcId,
            existingLocal = null
        )
        assertFalse(result)
    }

    @Test
    fun `shouldApplyExercise returns false when local MY_HEALTH session exists at same time`() {
        val existingLocal = ExerciseSessionEntity(
            id = "local-id",
            startTime = 1000L,
            endTime = 2000L,
            avgHeartRate = 120,
            maxHeartRate = 150,
            minHeartRate = 80,
            deviceBrand = "Huawei",
            source = SOURCE_MY_HEALTH,
            syncedToHc = 1
        )
        val result = ConflictResolver.shouldApplyExercise(
            hcRecordId = "new-hc-id",
            existingByHcId = null,
            existingLocal = existingLocal
        )
        assertFalse(result)
    }

    @Test
    fun `shouldApplyExercise returns true when no conflicts`() {
        val result = ConflictResolver.shouldApplyExercise(
            hcRecordId = "new-hc-id",
            existingByHcId = null,
            existingLocal = null
        )
        assertTrue(result)
    }

    @Test
    fun `shouldApplyHeartRate returns false when HC record already synced`() {
        val existingRecordIds = setOf("hc-record-id-1", "hc-record-id-2")
        val result = ConflictResolver.shouldApplyHeartRate(
            hcRecordId = "hc-record-id-1",
            existingRecordIds = existingRecordIds,
            existingLocal = null
        )
        assertFalse(result)
    }

    @Test
    fun `shouldApplyHeartRate returns false when local MY_HEALTH reading exists`() {
        val existingLocal = HeartRateEntity(
            timestamp = 1000L,
            source = SOURCE_MY_HEALTH,
            bpm = 72,
            healthConnectRecordId = null,
            syncedToHc = 1
        )
        val result = ConflictResolver.shouldApplyHeartRate(
            hcRecordId = "new-hc-id",
            existingRecordIds = emptySet(),
            existingLocal = existingLocal
        )
        assertFalse(result)
    }

    @Test
    fun `shouldApplyHeartRate returns true when no conflicts`() {
        val result = ConflictResolver.shouldApplyHeartRate(
            hcRecordId = "new-hc-id",
            existingRecordIds = emptySet(),
            existingLocal = null
        )
        assertTrue(result)
    }
}