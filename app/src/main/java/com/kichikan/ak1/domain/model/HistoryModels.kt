package com.kichikan.ak1.domain.model

enum class HistoryType {
    XP_EARNED, XP_DECREASED, POINTS_EARNED, POINTS_DECREASED, POINTS_SPENT,
    DIAMONDS_EARNED, DIAMONDS_DECREASED, DIAMONDS_SPENT, LEVEL_CHANGED,
    MISSION_COMPLETED, WHEEL_REWARD, REWARD_RECEIVED, AVATAR_ACQUIRED, FRAME_ACQUIRED,
    ATTENDANCE, SESSION_NOTE, BIRTHDAY_REWARD
}

data class HistoryEvent(
    val id: String,
    val memberId: String,
    val type: HistoryType,
    val amount: Int? = null,
    val title: String,
    val reason: String? = null,
    val createdAtEpochMillis: Long,
    val createdBy: String? = null,
    val metadata: Map<String, String> = emptyMap()
)