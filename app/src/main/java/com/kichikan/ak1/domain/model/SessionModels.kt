package com.kichikan.ak1.domain.model

enum class AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED }

data class Attendance(
    val id: String,
    val memberId: String,
    val sessionId: String,
    val status: AttendanceStatus,
    val note: String? = null,
    val createdAt: Long
)

data class Session(
    val id: String,
    val title: String,
    val description: String = "",
    val startsAt: Long,
    val location: String? = null
)
