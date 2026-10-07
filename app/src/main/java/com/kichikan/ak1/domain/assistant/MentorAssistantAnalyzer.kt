package com.kichikan.ak1.domain.assistant

import com.kichikan.ak1.domain.model.*

object MentorAssistantAnalyzer {
    fun analyze(
        members: List<Member>,
        history: List<HistoryEvent>,
        attendance: List<Attendance>,
        sessions: List<Session>,
        missionCompletions: List<MissionCompletion>,
        now: Long = System.currentTimeMillis()
    ): MentorAssistantReport {
        val insights = members.flatMap { member ->
            analyzeMember(member, history, attendance, missionCompletions, now)
        }.sortedByDescending { it.priority }
        return MentorAssistantReport(now, insights)
    }

    private fun analyzeMember(
        member: Member,
        history: List<HistoryEvent>,
        attendance: List<Attendance>,
        missionCompletions: List<MissionCompletion>,
        now: Long
    ): List<MentorInsight> {
        val result = mutableListOf<MentorInsight>()
        val memberAttendance = attendance.filter { it.memberId == member.id }
            .sortedByDescending { it.createdAt }
        val recentAttendance = memberAttendance.take(5)
        val absences = recentAttendance.count { it.status == AttendanceStatus.ABSENT }
        val recentAbsence = recentAttendance.firstOrNull()?.status == AttendanceStatus.ABSENT

        if (absences >= 3) {
            result += MentorInsight(
                member.id, member.name, MentorInsightType.ATTENDANCE_DROP, 90,
                "نیاز به توجه در حضور",
                "در ۵ جلسه اخیر، $absences غیبت ثبت شده است.",
                "با یک گفت‌وگوی کوتاه و غیرقضاوتی، علت غیبت‌ها را بررسی کن."
            )
        } else if (recentAbsence) {
            result += MentorInsight(
                member.id, member.name, MentorInsightType.RECENT_ABSENCE, 65,
                "غیبت اخیر",
                "آخرین وضعیت حضور این عضو غیبت بوده است.",
                "قبل از جلسه بعدی یک پیگیری کوتاه انجام بده."
            )
        }

        val day = 24L * 60L * 60L * 1000L
        val recentStart = now - 14L * day
        val previousStart = recentStart - 14L * day

        val recentActivity = history.any {
            it.memberId == member.id && it.createdAtEpochMillis >= recentStart
        }
        if (!recentActivity) {
            result += MentorInsight(
                member.id, member.name, MentorInsightType.NO_RECENT_ACTIVITY, 70,
                "فعالیت اخیر کم است",
                "در ۱۴ روز اخیر رویداد تربیتی یا امتیازی برای این عضو ثبت نشده است.",
                "یک تعامل ساده یا مأموریت کوچک برای او در نظر بگیر."
            )
        }

        val recentXp = history.filter {
            it.memberId == member.id &&
                it.type == HistoryType.XP_EARNED &&
                it.createdAtEpochMillis >= recentStart
        }.sumOf { it.amount ?: 0 }

        val previousXp = history.filter {
            it.memberId == member.id &&
                it.type == HistoryType.XP_EARNED &&
                it.createdAtEpochMillis in previousStart until recentStart
        }.sumOf { it.amount ?: 0 }

        if (previousXp >= 10 && recentXp * 2 < previousXp) {
            result += MentorInsight(
                member.id, member.name, MentorInsightType.ACTIVITY_DROP, 80,
                "کاهش فعالیت",
                "XP ثبت‌شده نسبت به بازه قبل به شکل محسوسی کاهش یافته است.",
                "علت تغییر را بررسی کن و از مقایسه مستقیم با دیگران پرهیز کن."
            )
        }

        val missionCount = missionCompletions.count {
            member.id in it.memberIds && it.completedAt >= recentStart
        }
        if (missionCount == 0 && recentActivity) {
            result += MentorInsight(
                member.id, member.name, MentorInsightType.MISSION_STAGNATION, 50,
                "مأموریت اخیر ندارد",
                "فعالیت ثبت شده، اما در ۱۴ روز اخیر مأموریتی تکمیل نشده است.",
                "یک مأموریت متناسب با سطح فعلی او پیشنهاد کن."
            )
        }
        return result
    }
}
