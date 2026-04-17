package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.data.local.dao.BriefDao
import com.gomaa.healthy.domain.model.HomeScreenData
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val briefDao: BriefDao
) {
    suspend operator fun invoke(date: LocalDate): HomeScreenData? {
        val epochDay = date.toEpochDay()
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay =
            date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return briefDao.getHomeScreenData(epochDay, startOfDay, endOfDay)

    }
}