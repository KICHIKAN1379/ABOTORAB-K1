package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.HistoryEvent
import java.util.Calendar

data class HistoryYear(val year: Int, val months: List<HistoryMonth>)
data class HistoryMonth(val month: Int, val events: List<HistoryEvent>)

object HistoryService {
    fun group(events: List<HistoryEvent>): List<HistoryYear> {
        val cal = Calendar.getInstance()
        return events.groupBy { event ->
            cal.timeInMillis = event.createdAtEpochMillis
            cal.get(Calendar.YEAR)
        }.map { (year, yearEvents) ->
            HistoryYear(year, yearEvents.groupBy { event ->
                cal.timeInMillis = event.createdAtEpochMillis
                cal.get(Calendar.MONTH) + 1
            }.map { (month, monthEvents) ->
                HistoryMonth(month, monthEvents.sortedByDescending { it.createdAtEpochMillis })
            }.sortedByDescending { it.month })
        }.sortedByDescending { it.year }
    }
}
