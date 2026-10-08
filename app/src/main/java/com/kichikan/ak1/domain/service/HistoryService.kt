package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.HistoryEvent
import com.kichikan.ak1.domain.model.HistoryType
import java.time.ZoneId

data class HistoryYear(val year: Int, val months: List<HistoryMonth>)
data class HistoryMonth(val month: Int, val events: List<HistoryEvent>)

/** User-facing filter groups for the member history screen. */
enum class HistoryCategory(val label: String, val types: Set<HistoryType>) {
    XP("XP", setOf(HistoryType.XP_EARNED, HistoryType.XP_DECREASED)),
    POINTS(
        "امتیاز",
        setOf(HistoryType.POINTS_EARNED, HistoryType.POINTS_DECREASED, HistoryType.POINTS_SPENT)
    ),
    DIAMONDS(
        "الماس",
        setOf(
            HistoryType.DIAMONDS_EARNED, HistoryType.DIAMONDS_DECREASED,
            HistoryType.DIAMONDS_SPENT, HistoryType.BIRTHDAY_REWARD
        )
    ),
    LEVEL("سطح", setOf(HistoryType.LEVEL_CHANGED)),
    MISSION("ماموریت", setOf(HistoryType.MISSION_COMPLETED)),
    WHEEL("گردونه", setOf(HistoryType.WHEEL_REWARD)),
    SHOP(
        "فروشگاه",
        setOf(HistoryType.REWARD_RECEIVED, HistoryType.AVATAR_ACQUIRED, HistoryType.FRAME_ACQUIRED)
    ),
    ATTENDANCE("حضور", setOf(HistoryType.ATTENDANCE, HistoryType.SESSION_NOTE))
}

object HistoryService {
    /** Returns only events of [category]; null means no filtering. */
    fun filter(events: List<HistoryEvent>, category: HistoryCategory?): List<HistoryEvent> =
        if (category == null) events else events.filter { it.type in category.types }

    /**
     * Groups events as Jalali Year > Month > Events, newest first.
     * [HistoryYear.year] is the Jalali year and [HistoryMonth.month] is 1..12 (1 = Farvardin).
     */
    fun group(events: List<HistoryEvent>, zone: ZoneId = ZoneId.systemDefault()): List<HistoryYear> {
        val dated = events.map { it to JalaliCalendar.fromEpochMillis(it.createdAtEpochMillis, zone) }
        return dated.groupBy { it.second.year }.map { (year, inYear) ->
            HistoryYear(
                year,
                inYear.groupBy { it.second.month }.map { (month, inMonth) ->
                    HistoryMonth(month, inMonth.map { it.first }.sortedByDescending { it.createdAtEpochMillis })
                }.sortedByDescending { it.month }
            )
        }.sortedByDescending { it.year }
    }
}
