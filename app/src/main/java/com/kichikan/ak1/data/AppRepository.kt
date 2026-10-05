package com.kichikan.ak1.data

import android.content.Context
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.EconomyChange
import com.kichikan.ak1.domain.service.EconomyService
import com.kichikan.ak1.domain.service.MissionService

class AppRepository(context: Context) {
    private var sequence = 0L
    private val store = LocalStore(context)
    var ring: RingAccount? = null
        private set
    val members = mutableListOf<Member>()
    val history = mutableListOf<HistoryEvent>()
    val shop = mutableListOf<ShopItem>()
    val missions = mutableListOf<Mission>()
    val missionCompletions = mutableListOf<MissionCompletion>()
    val sessions = mutableListOf<Session>()
    val attendance = mutableListOf<Attendance>()
    val assets = mutableListOf<CustomAsset>()
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

    fun addMission(mission: Mission) { missions += mission }
    fun completeMission(mission: Mission, memberIds: List<String>, reason: String? = null): MissionCompletion {
        check(MissionService.canComplete(mission, System.currentTimeMillis()))
        val completion = MissionService.completion(mission, memberIds, System.currentTimeMillis(), reason)
        missionCompletions += completion
        memberIds.distinct().forEach { memberId ->
            if (mission.xpReward > 0) recordXp(memberId, mission.xpReward, "ماموریت: ${mission.title}", "mentor")
            if (mission.pointsReward > 0) recordPoints(memberId, mission.pointsReward, "ماموریت: ${mission.title}", "mentor")
            if (mission.diamondReward > 0) recordDiamonds(memberId, mission.diamondReward, "ماموریت: ${mission.title}", "mentor")
            history += HistoryEvent("mission-${completion.id}-$memberId", memberId, HistoryType.MISSION_COMPLETED, null, mission.title, reason, completion.completedAt, "mentor")
        }
        persist()
        return completion
    }

    fun addSession(session: Session) { sessions += session }
    fun recordAttendance(item: Attendance) { attendance += item; history += HistoryEvent("attendance-${item.id}", item.memberId, HistoryType.ATTENDANCE, null, "حضور و غیاب", item.note, item.createdAt, "mentor"); persist() }
    fun addAsset(asset: CustomAsset) { assets += asset; persist() }
    fun persist() = store.save(ring, members, history)\n    fun exportBackup(): String = store.exportJson() ?: "{}"\n    fun importBackup(raw: String) {\n        store.importJson(raw)\n        members.clear()\n        history.clear()\n        store.load()?.let {\n            ring = it.ring\n            members += it.members\n            history += it.history\n        }\n    }
}
