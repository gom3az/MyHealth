package com.gomaa.healthy.domain.model

import java.time.LocalDate

sealed class DateRangeFilter {
    data object Today : DateRangeFilter()
    data object Last7Days : DateRangeFilter()
    data object Last30Days : DateRangeFilter()
    data object All : DateRangeFilter()

    data class Custom(
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : DateRangeFilter()

    fun toDateRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (this) {
            is Today -> today to today
            is Last7Days -> today.minusDays(6) to today
            is Last30Days -> today.minusDays(29) to today
            is All -> LocalDate.of(2000, 1, 1) to today
            is Custom -> startDate to endDate
        }
    }

    fun displayName(): String {
        return when (this) {
            is Today -> "Today"
            is Last7Days -> "7 Days"
            is Last30Days -> "30 Days"
            is All -> "All"
            is Custom -> "${startDate} - ${endDate}"
        }
    }

    companion object {
        val presets = listOf(Today, Last7Days, Last30Days, All)
    }
}