package com.kichikan.ak1.data

import android.content.Context
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.EconomyChange
import com.kichikan.ak1.domain.service.EconomyService

class AppRepository(context: Context) {
    private var sequence = 0L
    private val store = LocalStore(context)
    var ring: RingAccount? = null
        private set
    val members = mutableListOf<Member>()
    val history = mutableListOf<HistoryEvent>()
    val shop = mutableListOf<ShopItem>()
    var wheel = WheelConfig()
        private set

    init {
        store.load()?.let {
            ring = it.ring
            members += it.members
            history += it.history
        }
    }

    fun createRing(name: String, username: String, passwordRequired: Boolean = true): RingAccount {
        val account = RingAccount("ring-" + (++sequence), name.trim(), username.trim(), passwordRequired)
        ring = account
        persist()
        return account
    }

    fun addMember(name: String): Member {
        check(ring != null) { "ابتدا حلقه را ایجاد کنید" }
        val member = Member("member-" + (++sequence), ring!!.ringId, name.trim())
        members += member
        persist()
        return member
    }

    fun updateMember(member: Member) {
        val i = members.indexOfFirst { it.id == member.id }
        if (i >= 0) members[i] = member
        persist()
    }

    fun recordXp(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) { EconomyService.addXp(it, amount, reason, actor, System.currentTimeMillis()) }

    fun recordPoints(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) { EconomyService.addPoints(it, amount, reason, actor, System.currentTimeMillis()) }

    fun recordDiamonds(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) { EconomyService.addDiamonds(it, amount, reason, actor, System.currentTimeMillis()) }

    fun decreaseLevel(memberId: String, targetLevel: Int, reason: String, actor: String? = null) =
        mutate(memberId) { EconomyService.decreaseLevel(it, targetLevel, reason, actor, System.currentTimeMillis()) }

    private fun mutate(memberId: String, operation: (MemberEconomy) -> EconomyChange): Member {
        val member = members.first { it.id == memberId }
        val change = operation(member.economy)
        history += change.events.map { it.copy(memberId = memberId) }
        val updated = member.copy(economy = change.economy)
        updateMember(updated)
        return updated
    }

    fun setWheel(config: WheelConfig) { wheel = config }
    fun persist() = store.save(ring, members, history)
}
