package com.kichikan.ak1.domain.assistant

enum class MentorInsightType {
    ATTENDANCE_DROP,
    RECENT_ABSENCE,
    ACTIVITY_DROP,
    NO_RECENT_ACTIVITY,
    MISSION_STAGNATION
}

data class MentorInsight(
    val memberId: String,
    val memberName: String,
    val type: MentorInsightType,
    val priority: Int,
    val title: String,
    val explanation: String,
    val suggestedAction: String
)

data class MentorAssistantReport(
    val generatedAt: Long,
    val insights: List<MentorInsight>
) {
    val highPriorityCount: Int
        get() = insights.count { it.priority >= 80 }

    val hasAttentionItems: Boolean
        get() = insights.isNotEmpty()
}
