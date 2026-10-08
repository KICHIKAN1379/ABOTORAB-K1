package com.kichikan.ak1.domain.calendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Birthdays are kept on the Jalali calendar: a member born on 15 Mehr celebrates on 15 Mehr
 * every year. New values are stored as "YYYY/MM/DD"; older Gregorian "yyyy-MM-dd" values are
 * still understood and are converted when read.
 */
object BirthdayRules {
    private const val OLDEST_YEAR = 1300

    fun parseStored(value: String?): JalaliDate? =
        value?.takeIf { it.isNotBlank() }?.let { JalaliCalendar.parse(it) }

    /**
     * Validates user input and returns the canonical stored text "YYYY/MM/DD".
     * Throws [IllegalArgumentException] with a message that can be shown to the user.
     */
    fun normalize(input: String, today: LocalDate): String {
        val date = JalaliCalendar.parse(input, allowGregorian = false)
            ?: throw IllegalArgumentException("تاریخ تولد معتبر نیست؛ مثال: ۱۳۸۵/۰۷/۱۵ (سال ۴ رقم، ماه و روز ۲ رقم)")
        require(date.year >= OLDEST_YEAR) { "سال تولد باید شمسی و از ۱۳۰۰ به بعد باشد" }
        require(!JalaliCalendar.toGregorian(date).isAfter(today)) { "تاریخ تولد نمی‌تواند در آینده باشد" }
        return JalaliCalendar.format(date)
    }

    /** The birthday inside [year]. Esfand 30 falls back to Esfand 29 in non-leap years. */
    fun birthdayInYear(birth: JalaliDate, year: Int): JalaliDate {
        val day = if (birth.month == 12 && birth.day == 30 && !JalaliCalendar.isLeapYear(year)) 29 else birth.day
        return JalaliDate(year, birth.month, day)
    }

    fun isBirthday(birth: JalaliDate, today: LocalDate): Boolean {
        val now = JalaliCalendar.fromGregorian(today)
        return birthdayInYear(birth, now.year) == now
    }

    /** Days from [today] to the next birthday (0 when it is today). */
    fun daysUntilBirthday(birth: JalaliDate, today: LocalDate): Long {
        val year = JalaliCalendar.fromGregorian(today).year
        var next = JalaliCalendar.toGregorian(birthdayInYear(birth, year))
        if (next.isBefore(today)) next = JalaliCalendar.toGregorian(birthdayInYear(birth, year + 1))
        return ChronoUnit.DAYS.between(today, next)
    }
}
