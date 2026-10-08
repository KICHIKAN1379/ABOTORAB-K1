package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.HistoryEvent
import com.kichikan.ak1.domain.model.HistoryType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryServiceTest {
    private fun at(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.timeInMillis

    private fun event(id: String, type: HistoryType, time: Long) =
        HistoryEvent(id, "m1", type, 1, "t", null, time)

    private val sample = listOf(
        event("a", HistoryType.XP_EARNED, at(2025, 12, 31)),
        event("b", HistoryType.POINTS_EARNED, at(2026, 1, 5)),
        event("c", HistoryType.DIAMONDS_EARNED, at(2026, 3, 9)),
        event("d", HistoryType.XP_DECREASED, at(2026, 3, 20))
    )

    @Test fun groupsByYearThenMonthNewestFirst() {
        val years = HistoryService.group(sample)
        assertEquals(listOf(2026, 2025), years.map { it.year })
        assertEquals(listOf(3, 1), years[0].months.map { it.month })
        // Within a month the newest event comes first.
        assertEquals(listOf("d", "c"), years[0].months[0].events.map { it.id })
    }

    @Test fun filtersByCategory() {
        assertEquals(listOf("a", "d"), HistoryService.filter(sample, HistoryCategory.XP).map { it.id })
        assertEquals(listOf("c"), HistoryService.filter(sample, HistoryCategory.DIAMONDS).map { it.id })
        assertEquals(sample, HistoryService.filter(sample, null))
    }

    @Test fun everyHistoryTypeBelongsToExactlyOneCategory() {
        HistoryType.entries.forEach { type ->
            val owners = HistoryCategory.entries.count { type in it.types }
            assertEquals("type $type", 1, owners)
        }
    }
}
