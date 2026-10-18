package com.kichikan.ak1.birthday

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * When the daily birthday check should run, kept free of Android types so it can be unit tested.
 *
 * The old scheduler always targeted "tomorrow at 09:00". Because the app rescheduled on every
 * launch with the same PendingIntent, opening the app any morning before 09:00 replaced that day's
 * alarm with the next day's, so the reminder (and the once-a-year 30-diamond reward that only the
 * alarm granted) could be postponed forever.
 */
object BirthdaySchedule {
    val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    /** The next 09:00 strictly after [now]: today when it is still ahead, otherwise tomorrow. */
    fun nextNineAm(now: ZonedDateTime): ZonedDateTime {
        val today = now.with(REMINDER_TIME)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}
