package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.HistoryEvent
import com.kichikan.ak1.domain.model.HistoryType
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryServiceTest {
    private fun at(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun event(id: String, type: HistoryType, time: Long) =
        HistoryEvent(id, "m1", type, 1, "t", null, time)

    // Jalali dates (ICU): a=1404/10/10, b=1404/10/15, c=1404/12/18, d=1404/12/29, e=1405/01/02
    private val sample = listOf(
        event("a", HistoryType.XP_EARNED, at(2025, 12, 31)),
        event("b", HistoryType.POINTS_EARNED, at(2026, 1, 5)),
        event("c", HistoryType.DIAMONDS_EARNED, at(2026, 3, 9)),
        event("d", HistoryType.XP_DECREASED, at(2026, 3, 20)),
        event("e", HistoryType.XP_EARNED, at(2026, 3, 22))
    )

    @Test fun groupsByJalaliYearThenMonthNewestFirst() {
        val years = HistoryService.group(sample, ZoneOffset.UTC)
        // 2026-03-22 is already 1405 (Nowruz), the earlier events are still 1404.
        assertEquals(listOf(1405, 1404), years.map { it.year })
        assertEquals(listOf(1), years[0].months.map { it.month })
        assertEquals(listOf(12, 10), years[1].months.map { it.month })
        // Within a month the newest event comes first.
        assertEquals(listOf("d", "c"), years[1].months[0].events.map { it.id })
        assertEquals(listOf("b", "a"), years[1].months[1].events.map { it.id })
    }

    @Test fun filtersByCategory() {
        assertEquals(listOf("a", "d", "e"), HistoryService.filter(sample, HistoryCategory.XP).map { it.id })
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
