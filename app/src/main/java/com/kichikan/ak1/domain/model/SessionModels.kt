package com.kichikan.ak1.domain.model

enum class AttendanceStatus { UNMARKED, PRESENT, ABSENT, LATE, EXCUSED }

data class Attendance(
    val id: String,
    val memberId: String,
    val dateEpochMillis: Long,
    val status: AttendanceStatus,
    val sessionId: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Session(
    val id: String,
    val memberId: String,
    val title: String,
    val topic: String = "",
    val startsAt: Long,
    val location: String? = null
)