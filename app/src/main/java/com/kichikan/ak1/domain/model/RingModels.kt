package com.kichikan.ak1.domain.model

data class RingAccount(
    val ringId: String,
    val ringName: String,
    val ringUsername: String,
    val passwordRequired: Boolean = false
)

data class Group(
    val id: String,
    val ringId: String,
    val name: String,
    val leaderMemberId: String? = null,
    val memberIds: List<String> = emptyList(),
    val economy: MemberEconomy = MemberEconomy()
)

data class Member(
    val id: String,
    val ringId: String,
    val name: String,
    val economy: MemberEconomy = MemberEconomy(),
    val avatarItemId: String? = null,
    val frameItemId: String? = null,
    val birthDate: String? = null,
    val groupId: String? = null,
    val privateNotes: String = ""
)