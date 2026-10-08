package com.kichikan.ak1.domain.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A date in the Jalali (Shamsi / Persian) calendar. */
data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    override fun toString(): String = JalaliCalendar.format(this)
}

/**
 * Pure-Kotlin Jalali calendar (no Android or ICU dependency, so it runs in plain unit tests).
 *
 * The conversion follows the Borkowski 33-year-cycle algorithm (as used by jalaali-js). It was
 * cross-checked day by day against ICU's Persian calendar for every day from 1299/12/24
 * to 1501/01/10, with no differences and identical leap years.
 *
 * Valid for Jalali years 1 until 3178.
 */
object JalaliCalendar {
    val MONTH_NAMES: List<String> = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private const val MIN_YEAR = 1
    private const val MAX_YEAR = 3000

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private class YearInfo(val leap: Int, val gregorianYear: Int, val march: Int)

    private fun yearInfo(jy: Int): YearInfo {
        require(jy in MIN_YEAR..MAX_YEAR) { "سال شمسی خارج از محدوده است: $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return YearInfo(leap, gy, march)
    }

    private fun gregorianToDay(gy: Int, gm: Int, gd: Int): Int {
        var d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4 + (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4 + 752
        return d
    }

    private fun dayToGregorian(jdn: Int): LocalDate {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3 / 4) * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = (i % 153) / 5 + 1
        val gm = (i / 153) % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return LocalDate.of(gy, gm, gd)
    }

    private fun jalaliToDay(jy: Int, jm: Int, jd: Int): Int {
        val info = yearInfo(jy)
        return gregorianToDay(info.gregorianYear, 3, info.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    fun isLeapYear(year: Int): Boolean = yearInfo(year).leap == 0

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isLeapYear(year)) 30 else 29
        else -> throw IllegalArgumentException("ماه نامعتبر: $month")
    }

    fun isValid(year: Int, month: Int, day: Int): Boolean =
        year in MIN_YEAR..MAX_YEAR && month in 1..12 && day in 1..daysInMonth(year, month)

    fun fromGregorian(date: LocalDate): JalaliDate {
        val gy = date.year
        val jdn = gregorianToDay(gy, date.monthValue, date.dayOfMonth)
        var jy = gy - 621
        val info = yearInfo(jy)
        var k = jdn - gregorianToDay(gy, 3, info.march)
        if (k >= 0) {
            if (k <= 185) return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (info.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun toGregorian(date: JalaliDate): LocalDate {
        require(isValid(date.year, date.month, date.day)) { "تاریخ شمسی نامعتبر: $date" }
        return dayToGregorian(jalaliToDay(date.year, date.month, date.day))
    }

    fun fromEpochMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): JalaliDate =
        fromGregorian(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

    /** "1385/07/05" — always ASCII digits, independent of the device locale. */
    fun format(date: JalaliDate): String =
        date.year.toString().padStart(4, '0') + "/" +
            date.month.toString().padStart(2, '0') + "/" +
            date.day.toString().padStart(2, '0')

    /** "1405/07/16 09:05" in [zone]. */
    fun formatDateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone)
        return format(fromGregorian(time.toLocalDate())) + " " +
            time.hour.toString().padStart(2, '0') + ":" + time.minute.toString().padStart(2, '0')
    }

    /**
     * Parses user or stored text. Accepts Persian/Arabic or Latin digits and any separator
     * ("/", "-", ".", space) or none at all ("13850715").
     *
     * A first number of 1700 or more is read as a Gregorian date when [allowGregorian] is true,
     * because older versions of the app stored birth dates as Gregorian "yyyy-MM-dd". With
     * [allowGregorian] false (user input) such a number is rejected instead.
     *
     * Returns null when the text is not a real date.
     */
    fun parse(text: String, allowGregorian: Boolean = true): JalaliDate? {
        val ascii = text.trim().map { c -> c.digitToIntOrNull()?.let { '0' + it } ?: c }.joinToString("")
        val numbers = Regex("[0-9]+").findAll(ascii).map { it.value }.toList()
        val parts: List<String> = when {
            numbers.size == 1 && numbers[0].length == 8 ->
                listOf(numbers[0].substring(0, 4), numbers[0].substring(4, 6), numbers[0].substring(6, 8))
            numbers.size == 3 -> numbers
            else -> return null
        }
        if (parts.any { it.length > 4 }) return null
        val a = parts[0].toInt()
        val b = parts[1].toInt()
        val c = parts[2].toInt()
        if (a >= 1700) {
            if (!allowGregorian) return null
            return try {
                fromGregorian(LocalDate.of(a, b, c))
            } catch (_: Exception) {
                null
            }
        }
        return if (isValid(a, b, c)) JalaliDate(a, b, c) else null
    }
}
