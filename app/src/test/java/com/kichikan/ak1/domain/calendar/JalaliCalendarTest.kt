package com.kichikan.ak1.domain.calendar

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliCalendarTest {
    // Expected values come from ICU's Persian calendar (Intl.DateTimeFormat, ca=persian).
    private val known = listOf(
        LocalDate.of(1970, 1, 1) to JalaliDate(1348, 10, 11),
        LocalDate.of(2000, 1, 1) to JalaliDate(1378, 10, 11),
        LocalDate.of(2006, 10, 7) to JalaliDate(1385, 7, 15),
        LocalDate.of(2024, 3, 20) to JalaliDate(1403, 1, 1),
        LocalDate.of(2025, 3, 20) to JalaliDate(1403, 12, 30),
        LocalDate.of(2025, 3, 21) to JalaliDate(1404, 1, 1),
        LocalDate.of(2026, 3, 20) to JalaliDate(1404, 12, 29),
        LocalDate.of(2026, 3, 21) to JalaliDate(1405, 1, 1),
        LocalDate.of(2026, 10, 8) to JalaliDate(1405, 7, 16)
    )

    @Test fun convertsKnownDatesBothWays() {
        known.forEach { (gregorian, jalali) ->
            assertEquals("to jalali $gregorian", jalali, JalaliCalendar.fromGregorian(gregorian))
            assertEquals("to gregorian $jalali", gregorian, JalaliCalendar.toGregorian(jalali))
        }
    }

    @Test fun roundTripsEveryDayFor200Years() {
        var day = LocalDate.of(1921, 3, 21).toEpochDay()
        val end = LocalDate.of(2122, 3, 20).toEpochDay()
        var previous: JalaliDate? = null
        while (day <= end) {
            val date = LocalDate.ofEpochDay(day)
            val jalali = JalaliCalendar.fromGregorian(date)
            assertTrue("valid $jalali for $date", JalaliCalendar.isValid(jalali.year, jalali.month, jalali.day))
            assertEquals(date, JalaliCalendar.toGregorian(jalali))
            previous?.let { assertTrue("$it must come before $jalali", compare(it, jalali) < 0) }
            previous = jalali
            day++
        }
    }

    private fun compare(a: JalaliDate, b: JalaliDate): Int =
        compareValuesBy(a, b, { it.year }, { it.month }, { it.day })

    @Test fun knowsLeapYears() {
        listOf(1395, 1399, 1403, 1408, 1412).forEach { assertTrue("$it", JalaliCalendar.isLeapYear(it)) }
        listOf(1400, 1401, 1402, 1404, 1405, 1406, 1407).forEach { assertFalse("$it", JalaliCalendar.isLeapYear(it)) }
    }

    @Test fun monthLengths() {
        assertEquals(31, JalaliCalendar.daysInMonth(1405, 1))
        assertEquals(31, JalaliCalendar.daysInMonth(1405, 6))
        assertEquals(30, JalaliCalendar.daysInMonth(1405, 7))
        assertEquals(30, JalaliCalendar.daysInMonth(1405, 11))
        assertEquals(29, JalaliCalendar.daysInMonth(1404, 12))
        assertEquals(30, JalaliCalendar.daysInMonth(1403, 12))
    }

    @Test fun parsesAnyDigitsAndSeparators() {
        val expected = JalaliDate(1385, 7, 15)
        assertEquals(expected, JalaliCalendar.parse("1385/07/15"))
        assertEquals(expected, JalaliCalendar.parse("13850715"))
        assertEquals(expected, JalaliCalendar.parse("۱۳۸۵/۰۷/۱۵"))
        assertEquals(expected, JalaliCalendar.parse("۱۳۸۵۰۷۱۵"))
        assertEquals(expected, JalaliCalendar.parse("1385-7-15"))
        assertEquals(expected, JalaliCalendar.parse(" 1385.07.15 "))
        assertEquals(JalaliDate(1385, 7, 5), JalaliCalendar.parse("1385/7/5"))
    }

    @Test fun readsOldGregorianValuesAsJalali() {
        assertEquals(JalaliDate(1385, 7, 15), JalaliCalendar.parse("2006-10-07"))
    }

    @Test fun rejectsImpossibleDates() {
        listOf(
            "", "1385", "1385/07", "1385/13/01", "1385/00/10", "1385/07/00",
            "1385/07/31", "1404/12/30", "1385/07/15/1", "abcd", "138507150"
        ).forEach { assertNull("'$it'", JalaliCalendar.parse(it)) }
        assertEquals(JalaliDate(1403, 12, 30), JalaliCalendar.parse("1403/12/30"))
    }

    @Test fun formatsWithAsciiDigitsAndPadding() {
        assertEquals("1385/07/05", JalaliCalendar.format(JalaliDate(1385, 7, 5)))
        assertEquals("1385/07/05", JalaliDate(1385, 7, 5).toString())
    }

    @Test fun formatsDateTimeInGivenZone() {
        val millis = LocalDate.of(2026, 10, 8).atTime(9, 5).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("1405/07/16 09:05", JalaliCalendar.formatDateTime(millis, ZoneOffset.UTC))
    }
}
