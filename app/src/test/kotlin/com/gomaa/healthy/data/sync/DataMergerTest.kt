package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateBucketEntity
import com.gomaa.healthy.data.mapper.SOURCE_HEALTH_CONNECT
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataMergerTest {

    private lateinit var dataMerger: DataMerger

    @Before
    fun setup() {
        dataMerger = DataMergerImpl()
    }

    @Test
    fun `mergeSteps returns local when both have data for same date`() {
        val hcData = listOf(
            DailyStepsEntity(
                date = 20000L,
                totalSteps = 5000,
                totalDistanceMeters = 0.0,
                activeMinutes = 0,
                lightActivityMinutes = 0,
                moderateActivityMinutes = 0,
                vigorousActivityMinutes = 0,
                source = SOURCE_HEALTH_CONNECT,
                dataOrigin = "android"
            )
        )
        val localData = listOf(
            DailyStepsEntity(
                date = 20000L,
                totalSteps = 8000,
                totalDistanceMeters = 0.0,
                activeMinutes = 0,
                lightActivityMinutes = 0,
                moderateActivityMinutes = 0,
                vigorousActivityMinutes = 0,
                source = SOURCE_MY_HEALTH,
                dataOrigin = null
            )
        )

        val result = dataMerger.mergeSteps(hcData, localData)

        assertEquals(1, result.size)
        assertEquals(8000, result[0].totalSteps)
        assertEquals(SOURCE_MY_HEALTH, result[0].source)
    }

    @Test
    fun `mergeSteps returns hc when no local data exists`() {
        val hcData = listOf(
            DailyStepsEntity(
                date = 20000L,
                totalSteps = 5000,
                totalDistanceMeters = 0.0,
                activeMinutes = 0,
                lightActivityMinutes = 0,
                moderateActivityMinutes = 0,
                vigorousActivityMinutes = 0,
                source = SOURCE_HEALTH_CONNECT,
                dataOrigin = "com.huawei.health"
            )
        )
        val localData = emptyList<DailyStepsEntity>()

        val result = dataMerger.mergeSteps(hcData, localData)

        assertEquals(1, result.size)
        assertEquals(5000, result[0].totalSteps)
    }

    @Test
    fun `mergeSteps returns local when no hc data exists`() {
        val hcData = emptyList<DailyStepsEntity>()
        val localData = listOf(
            DailyStepsEntity(
                date = 20000L,
                totalSteps = 8000,
                totalDistanceMeters = 0.0,
                activeMinutes = 0,
                lightActivityMinutes = 0,
                moderateActivityMinutes = 0,
                vigorousActivityMinutes = 0,
                source = SOURCE_MY_HEALTH,
                dataOrigin = null
            )
        )

        val result = dataMerger.mergeSteps(hcData, localData)

        assertEquals(1, result.size)
        assertEquals(8000, result[0].totalSteps)
    }

    @Test
    fun `mergeSteps wearable (Huawei) has higher priority than phone (android)`() {
        val hcData = listOf(
            DailyStepsEntity(
                date = 20000L,
                totalSteps = 3000,
                totalDistanceMeters = 0.0,
                activeMinutes = 0,
                lightActivityMinutes = 0,
                moderateActivityMinutes = 0,
                vigorousActivityMinutes = 0,
                source = SOURCE_HEALTH_CONNECT,
                dataOrigin = "android"
            )
        )
        val localData = emptyList<DailyStepsEntity>()

        val result = dataMerger.mergeSteps(hcData, localData)

        assertEquals(1, result.size)
        assertEquals(3000, result[0].totalSteps)
    }

    @Test
    fun `mergeHeartRates returns local when both have data for same bucketId`() {
        val hcData = listOf(
            HeartRateBucketEntity(
                bucketId = "2026-04-15-14",
                source = SOURCE_HEALTH_CONNECT,
                dayTimestamp = 1704067200000,
                minBpm = 70,
                avgBpm = 75,
                maxBpm = 80,
                count = 10,
                samplesJson = "[]",
                healthConnectRecordId = "hc-1"
            )
        )
        val localData = listOf(
            HeartRateBucketEntity(
                bucketId = "2026-04-15-14",
                source = SOURCE_MY_HEALTH,
                dayTimestamp = 1704067200000,
                minBpm = 75,
                avgBpm = 80,
                maxBpm = 85,
                count = 5,
                samplesJson = "[]",
                healthConnectRecordId = ""
            )
        )

        val result = dataMerger.mergeHeartRates(hcData, localData)

        assertEquals(1, result.size)
        assertEquals(80, result[0].avgBpm)
        assertEquals(SOURCE_MY_HEALTH, result[0].source)
    }

    @Test
    fun `mergeHeartRates returns merged list when no conflicts`() {
        val hcData = listOf(
            HeartRateBucketEntity(
                bucketId = "2026-04-15-14",
                source = SOURCE_HEALTH_CONNECT,
                dayTimestamp = 1704067200000,
                minBpm = 70,
                avgBpm = 75,
                maxBpm = 80,
                count = 10,
                samplesJson = "[]",
                healthConnectRecordId = "hc-1"
            ),
            HeartRateBucketEntity(
                bucketId = "2026-04-15-15",
                source = SOURCE_HEALTH_CONNECT,
                dayTimestamp = 1704067200000,
                minBpm = 75,
                avgBpm = 80,
                maxBpm = 85,
                count = 8,
                samplesJson = "[]",
                healthConnectRecordId = "hc-2"
            )
        )
        val localData = listOf(
            HeartRateBucketEntity(
                bucketId = "2026-04-15-16",
                source = SOURCE_MY_HEALTH,
                dayTimestamp = 1704067200000,
                minBpm = 80,
                avgBpm = 85,
                maxBpm = 90,
                count = 5,
                samplesJson = "[]",
                healthConnectRecordId = ""
            )
        )

        val result = dataMerger.mergeHeartRates(hcData, localData)

        assertEquals(3, result.size)
    }

    @Test
    fun `mergeExerciseSessions returns local when overlapping time range exists`() {
        val hcData = listOf(
            ExerciseSessionEntity(
                id = "hc-1",
                startTime = 1000L,
                endTime = 2000L,
                avgHeartRate = 120,
                maxHeartRate = 150,
                minHeartRate = 80,
                deviceBrand = "Huawei",
                source = SOURCE_HEALTH_CONNECT,
                healthConnectRecordId = "hc-rec-1"
            )
        )
        val localData = listOf(
            ExerciseSessionEntity(
                id = "local-1",
                startTime = 1000L,
                endTime = 2000L,
                avgHeartRate = 130,
                maxHeartRate = 160,
                minHeartRate = 90,
                deviceBrand = "Huawei",
                source = SOURCE_MY_HEALTH
            )
        )

        val result = dataMerger.mergeExerciseSessions(hcData, localData)

        assertTrue(result.any { it.id == "local-1" })
        assertTrue(result.none { it.id == "hc-1" })
    }

    @Test
    fun `mergeExerciseSessions returns hc when no local overlapping sessions`() {
        val hcData = listOf(
            ExerciseSessionEntity(
                id = "hc-1",
                startTime = 1000L,
                endTime = 2000L,
                avgHeartRate = 120,
                maxHeartRate = 150,
                minHeartRate = 80,
                deviceBrand = "Huawei",
                source = SOURCE_HEALTH_CONNECT,
                healthConnectRecordId = "hc-rec-1"
            )
        )
        val localData = listOf(
            ExerciseSessionEntity(
                id = "local-1",
                startTime = 3000L,
                endTime = 4000L,
                avgHeartRate = 130,
                maxHeartRate = 160,
                minHeartRate = 90,
                deviceBrand = "Huawei",
                source = SOURCE_MY_HEALTH
            )
        )

        val result = dataMerger.mergeExerciseSessions(hcData, localData)

        assertEquals(2, result.size)
    }
}
