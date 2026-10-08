package com.kichikan.ak1.domain.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayRulesTest {
    private val birth = JalaliDate(1385, 7, 15)
    private val today = LocalDate.of(2026, 10, 8) // 1405/07/16

    @Test fun birthdayHappensOnTheSameJalaliDayEachYear() {
        assertTrue(BirthdayRules.isBirthday(birth, LocalDate.of(2026, 10, 7)))   // 1405/07/15
        assertTrue(BirthdayRules.isBirthday(birth, LocalDate.of(2027, 10, 7)))   // 1406/07/15
        assertFalse(BirthdayRules.isBirthday(birth, LocalDate.of(2026, 10, 8)))
        assertFalse(BirthdayRules.isBirthday(birth, LocalDate.of(2026, 10, 6)))
    }

    @Test fun countsDaysUntilTheNextBirthday() {
        assertEquals(3L, BirthdayRules.daysUntilBirthday(birth, LocalDate.of(2026, 10, 4)))
        assertEquals(0L, BirthdayRules.daysUntilBirthday(birth, LocalDate.of(2026, 10, 7)))
        // The day after the birthday waits for next year's 1406/07/15 (= 2027-10-07).
        assertEquals(364L, BirthdayRules.daysUntilBirthday(birth, today))
    }

    @Test fun esfandThirtyFallsBackToTwentyNinthInNormalYears() {
        val leapBaby = JalaliDate(1403, 12, 30)
        assertEquals(JalaliDate(1404, 12, 29), BirthdayRules.birthdayInYear(leapBaby, 1404))
        assertEquals(JalaliDate(1408, 12, 30), BirthdayRules.birthdayInYear(leapBaby, 1408))
        // 1404/12/29 is 2026-03-20.
        assertTrue(BirthdayRules.isBirthday(leapBaby, LocalDate.of(2026, 3, 20)))
    }

    @Test fun normalizesUserInput() {
        assertEquals("1385/07/15", BirthdayRules.normalize("13850715", today))
        assertEquals("1385/07/05", BirthdayRules.normalize("۱۳۸۵/۷/۵", today))
    }

    @Test fun rejectsBadInput() {
        // "20061007" is a Gregorian date: user input must be Jalali, so it is rejected, not converted.
        listOf("1385", "13851315", "15000101", "1299/01/01", "14050717", "20061007").forEach {
            val failed = runCatching { BirthdayRules.normalize(it, today) }
            assertTrue("'$it' must be rejected", failed.exceptionOrNull() is IllegalArgumentException)
        }
        // Today itself is fine.
        assertEquals("1405/07/16", BirthdayRules.normalize("14050716", today))
    }

    @Test fun readsStoredValuesOfBothKinds() {
        assertEquals(birth, BirthdayRules.parseStored("1385/07/15"))
        assertEquals(birth, BirthdayRules.parseStored("2006-10-07"))
        assertNull(BirthdayRules.parseStored(null))
        assertNull(BirthdayRules.parseStored(""))
        assertNull(BirthdayRules.parseStored("garbage"))
    }
}
