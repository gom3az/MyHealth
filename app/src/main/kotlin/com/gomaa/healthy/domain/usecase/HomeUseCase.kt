package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.data.local.dao.BriefDao
import com.gomaa.healthy.domain.model.HomeScreenData
import java.time.LocalDate
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val briefDao: BriefDao
) {
    suspend operator fun invoke(date: LocalDate): HomeScreenData? {
        val epoch = date.toEpochDay()
        val epochMillis = epoch * 86400000
        return briefDao.getHomeScreenData(epoch, epochMillis)
    }
}