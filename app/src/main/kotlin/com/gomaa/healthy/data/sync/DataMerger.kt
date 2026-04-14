package com.gomaa.healthy.data.sync

import com.gomaa.healthy.data.local.entity.DailyStepsEntity
import com.gomaa.healthy.data.local.entity.ExerciseSessionEntity
import com.gomaa.healthy.data.local.entity.HeartRateEntity
import com.gomaa.healthy.data.mapper.SOURCE_MY_HEALTH
import javax.inject.Inject
import javax.inject.Singleton

interface DataMerger {
    fun mergeSteps(
        hcData: List<DailyStepsEntity>, localData: List<DailyStepsEntity>
    ): List<DailyStepsEntity>

    fun mergeHeartRates(
        hcData: List<HeartRateEntity>, localData: List<HeartRateEntity>
    ): List<HeartRateEntity>

    fun mergeExerciseSessions(
        hcData: List<ExerciseSessionEntity>, localData: List<ExerciseSessionEntity>
    ): List<ExerciseSessionEntity>
}

@Singleton
class DataMergerImpl @Inject constructor() : DataMerger {

    companion object {
        private const val HEART_RATE_BIN_SIZE_MS = 5000L
    }

    override fun mergeSteps(
        hcData: List<DailyStepsEntity>, localData: List<DailyStepsEntity>
    ): List<DailyStepsEntity> {
        if (hcData.isEmpty()) return localData
        if (localData.isEmpty()) return hcData

        val hcByDate = hcData.associateBy { it.date }
        val localByDate = localData.associateBy { it.date }

        val allDates = (hcData.map { it.date } + localData.map { it.date }).toSet()

        return allDates.map { date ->
            val hc = hcByDate[date]
            val local = localByDate[date]

            when {
                hc != null && local == null -> hc
                local != null && hc == null -> local
                hc != null && local != null -> {
                    val hcPrecision =
                        DataOriginConstants.getPrecision(hc.dataOrigin, hc.source).priority
                    val localPrecision =
                        DataOriginConstants.getPrecision(local.dataOrigin, local.source).priority

                    when {
                        localPrecision > hcPrecision -> local
                        hcPrecision > localPrecision -> hc
                        else -> {
                            if (local.totalSteps >= hc.totalSteps) local else hc
                        }
                    }
                }

                else -> throw IllegalStateException("Unexpected state in mergeSteps")
            }
        }.sortedBy { it.date }
    }

    override fun mergeHeartRates(
        hcData: List<HeartRateEntity>, localData: List<HeartRateEntity>
    ): List<HeartRateEntity> {
        if (hcData.isEmpty()) return localData
        if (localData.isEmpty()) return hcData

        fun roundToBin(timestamp: Long): Long {
            return (timestamp / HEART_RATE_BIN_SIZE_MS) * HEART_RATE_BIN_SIZE_MS
        }

        val merged = mutableMapOf<Long, HeartRateEntity>()

        hcData.forEach { hr ->
            val bin = roundToBin(hr.timestamp)
            val existing = merged[bin]
            if (existing == null) {
                merged[bin] = hr.copy(timestamp = bin)
            } else {
                val existingPrecision = DataOriginConstants.getPrecision(
                    existing.dataOrigin, existing.source
                ).priority
                val newPrecision = DataOriginConstants.getPrecision(
                    hr.dataOrigin, hr.source
                ).priority

                val winner = when {
                    newPrecision > existingPrecision -> hr
                    existingPrecision > newPrecision -> existing
                    else -> {
                        if (hr.bpm >= existing.bpm) hr else existing
                    }
                }
                merged[bin] = winner.copy(timestamp = bin)
            }
        }

        localData.forEach { local ->
            val bin = roundToBin(local.timestamp)
            val existing = merged[bin]

            val shouldReplace = when {
                existing == null -> true
                local.source == SOURCE_MY_HEALTH -> true
                existing.source == SOURCE_MY_HEALTH -> false
                else -> {
                    val existingPrecision = DataOriginConstants.getPrecision(
                        existing.dataOrigin, existing.source
                    ).priority
                    val localPrecision =
                        DataOriginConstants.getPrecision(local.dataOrigin, local.source).priority

                    when {
                        localPrecision > existingPrecision -> true
                        existingPrecision > localPrecision -> false
                        else -> local.bpm >= existing.bpm
                    }
                }
            }

            if (shouldReplace) {
                merged[bin] = local.copy(timestamp = bin)
            }
        }

        return merged.values.toList().sortedBy { it.timestamp }
    }

    override fun mergeExerciseSessions(
        hcData: List<ExerciseSessionEntity>, localData: List<ExerciseSessionEntity>
    ): List<ExerciseSessionEntity> {
        if (hcData.isEmpty()) return localData
        if (localData.isEmpty()) return hcData

        val sortedHc = hcData.sortedBy { it.startTime }
        val sortedLocal = localData.sortedBy { it.startTime }

        val merged = mutableListOf<ExerciseSessionEntity>()
        var hcIndex = 0
        var localIndex = 0

        while (hcIndex < sortedHc.size && localIndex < sortedLocal.size) {
            val hc = sortedHc[hcIndex]
            val local = sortedLocal[localIndex]

            val hcStart = hc.startTime
            val localStart = local.startTime

            when {
                hc.endTime <= localStart -> {
                    merged.add(hc)
                    hcIndex++
                }

                local.endTime <= hcStart -> {
                    merged.add(local)
                    localIndex++
                }

                else -> {
                    val overlapping = if (hcStart < local.endTime && hc.endTime > localStart) {
                        val hcPrecision =
                            DataOriginConstants.getPrecision(hc.dataOrigin, hc.source).priority
                        val localPrecision = DataOriginConstants.getPrecision(
                            local.dataOrigin, local.source
                        ).priority

                        when {
                            localPrecision > hcPrecision -> local
                            hcPrecision > localPrecision -> hc
                            else -> {
                                val hcDuration = hc.endTime - hc.startTime
                                val localDuration = local.endTime - local.startTime
                                if (localDuration >= hcDuration) local else hc
                            }
                        }
                    } else {
                        null
                    }

                    if (overlapping != null) {
                        merged.add(overlapping)
                        hcIndex++
                        localIndex++
                    } else if (hcStart < localStart) {
                        merged.add(hc)
                        hcIndex++
                    } else {
                        merged.add(local)
                        localIndex++
                    }
                }
            }
        }

        while (hcIndex < sortedHc.size) {
            merged.add(sortedHc[hcIndex])
            hcIndex++
        }

        while (localIndex < sortedLocal.size) {
            merged.add(sortedLocal[localIndex])
            localIndex++
        }

        return merged.sortedBy { it.startTime }
    }
}
