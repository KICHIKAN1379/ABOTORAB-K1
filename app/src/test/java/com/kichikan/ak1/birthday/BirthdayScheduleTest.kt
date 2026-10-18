package com.kichikan.ak1.birthday

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BirthdayScheduleTest {
    private val tehran = ZoneId.of("Asia/Tehran")

    private fun at(hour: Int, minute: Int, day: Int = 16) =
        ZonedDateTime.of(LocalDate.of(2026, 7, day), LocalTime.of(hour, minute), tehran)

    @Test fun aTimeStillAheadTargetsToday() {
        assertEquals(at(9, 0), BirthdaySchedule.nextNineAm(at(8, 59)))
    }

    @Test fun exactlyNineTargetsTomorrow() {
        assertEquals(at(9, 0, day = 17), BirthdaySchedule.nextNineAm(at(9, 0)))
    }

    @Test fun lateEveningTargetsTomorrowMorning() {
        assertEquals(at(9, 0, day = 17), BirthdaySchedule.nextNineAm(at(23, 59)))
    }

    @Test fun crossingMidnightRollsTheDateForward() {
        val next = BirthdaySchedule.nextNineAm(at(23, 30))
        assertEquals(LocalDate.of(2026, 7, 17), next.toLocalDate())
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }

    @Test fun worksOnTheSpringForwardDayWhereNineAmExists() {
        val newYork = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(LocalDate.of(2026, 3, 8), LocalTime.of(8, 30), newYork)

        val next = BirthdaySchedule.nextNineAm(now)

        assertEquals(LocalDate.of(2026, 3, 8), next.toLocalDate())
        assertEquals(LocalTime.of(9, 0), next.toLocalTime())
    }
}
