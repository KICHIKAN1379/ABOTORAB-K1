package com.kichikan.ak1.domain.model

data class RingAccount(
    val ringId: String,
    val ringName: String,
    val ringUsername: String,
    val passwordRequired: Boolean = false
)

data class Member(
    val id: String,
    val ringId: String,
    val name: String,
    val economy: MemberEconomy = MemberEconomy(),
    val avatarItemId: String? = null,
    val frameItemId: String? = null,
    /** Jalali (Shamsi) birth date as "YYYY/MM/DD". Older saves may hold Gregorian "yyyy-MM-dd"; both are read. */
    val birthDate: String? = null
)
