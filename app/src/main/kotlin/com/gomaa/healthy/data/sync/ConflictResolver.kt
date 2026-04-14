package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH

/**
 * Conflict resolution strategy for bidirectional sync between local database and Health Connect.
 *
 * Strategy: Local-first (local data always wins in conflicts)
 * - Local-only data (source = "myhealth") is never overwritten by Health Connect data
 * - Health Connect data is only inserted if no local equivalent exists
 * - For steps: Local daily steps take precedence; HC steps are only added for dates with no local data
 * - For exercise sessions: Local sessions are never modified; HC sessions with same recordId are skipped
 * - For heart rate: Local readings take precedence; HC readings with same timestamp are skipped
 */
object ConflictResolver {

    /**
     * Resolve conflict for steps data.
     * Local-first: Only skip if MY_HEALTH data already exists for this date.
     * HC data can coexist with existing HC data from same source.
     * @return true if HC data should be applied, false if local MY_HEALTH data should be kept
     */
    fun shouldApplySteps(localStepsForDate: DailyStepsEntity?): Boolean {
        // Apply HC data only if no MY_HEALTH data exists for this date
        // (HC data can coexist with existing HC data)
        return localStepsForDate?.source != SOURCE_MY_HEALTH
    }

    /**
     * Resolve conflict for exercise session.
     * Combines deduplication and local-first checks.
     * @param hcRecordId The HC record ID being synced
     * @param existingByHcId Existing session with same HC recordId (dedup check)
     * @param existingLocal Existing MY_HEALTH session at same time range (conflict check)
     * @return true if HC session should be applied
     */
    fun shouldApplyExercise(
        hcRecordId: String,
        existingByHcId: ExerciseSessionEntity?,
        existingLocal: ExerciseSessionEntity?
    ): Boolean {
        // Deduplication: same HC record already exists
        if (existingByHcId != null) return false
        // Local-first: MY_HEALTH session exists at same time
        if (existingLocal != null && existingLocal.source == SOURCE_MY_HEALTH) return false
        return true
    }

    /**
     * Resolve conflict for heart rate data.
     * Combines deduplication and local-first checks.
     * @param hcRecordId The HC record ID being synced
     * @param existingRecordIds Set of already-synced HC record IDs
     * @param existingLocal MY_HEALTH reading at same timestamp (conflict check)
     * @return true if HC reading should be applied
     */
    fun shouldApplyHeartRate(
        hcRecordId: String,
        existingRecordIds: Set<String>,
        existingLocal: HeartRateEntity?
    ): Boolean {
        // Deduplication: same HC record already synced
        if (hcRecordId in existingRecordIds) return false
        // Local-first: MY_HEALTH reading exists at same timestamp
        if (existingLocal != null && existingLocal.source == SOURCE_MY_HEALTH) return false
        return true
    }
}
