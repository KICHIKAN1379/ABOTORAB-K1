package com.kichikan.ak1.data

import android.content.Context
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.EconomyChange
import com.kichikan.ak1.domain.service.EconomyService
import com.kichikan.ak1.domain.service.MissionService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
        store.load()?.let { state ->
            ring = state.ring
            members += state.members
            history += state.history
            shop += state.shop
            wheel = state.wheel
            missions += state.missions
            missionCompletions += state.missionCompletions
            sessions += state.sessions
            attendance += state.attendance
            assets += state.assets
        }
    }

    fun createRing(name: String, username: String): RingAccount {
        require(name.isNotBlank()) { "نام حلقه الزامی است" }
        require(username.isNotBlank()) { "نام کاربری حلقه الزامی است" }
        check(ring == null) { "حلقه قبلاً ایجاد شده است" }

        val account = RingAccount(
            "ring-" + (++sequence),
            name.trim(),
            username.trim(),
            false
        )
        ring = account
        persist()
        return account
    }

    fun addMember(name: String, birthDate: String? = null): Member {
        check(ring != null) { "ابتدا حلقه را ایجاد کنید" }
        require(name.isNotBlank()) { "نام عضو الزامی است" }
        val normalizedBirthDate = birthDate?.trim()?.takeIf { it.isNotEmpty() }?.also { validateBirthDate(it) }

        val member = Member(
            "member-" + (++sequence),
            ring!!.ringId,
            name.trim(),
            birthDate = normalizedBirthDate
        )
        members += member
        persist()
        return member
    }

    /** Processes the annual 30-diamond birthday reward exactly once per calendar year. */
    fun processBirthdayRewards(now: Long = System.currentTimeMillis()): Int {
        val today = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        var granted = 0
        members.forEach { member ->
            val birthDate = member.birthDate?.let { parseBirthDate(it) } ?: return@forEach
            val birthdayDay = if (birthDate.month == java.time.Month.FEBRUARY && birthDate.dayOfMonth == 29 && !java.time.Year.isLeap(today.year.toLong())) 28 else birthDate.dayOfMonth
            if (birthDate.month != today.month || birthdayDay != today.dayOfMonth) return@forEach
            val alreadyGranted = history.any {
                it.memberId == member.id && it.type == HistoryType.BIRTHDAY_REWARD && it.metadata["year"] == today.year.toString()
            }
            if (alreadyGranted) return@forEach
            recordDiamonds(member.id, 30, "پاداش تولد سال ${today.year}", "system")
            history += HistoryEvent(
                id = "birthday-${member.id}-${today.year}", memberId = member.id,
                type = HistoryType.BIRTHDAY_REWARD, amount = 30,
                title = "پاداش تولد: ۳۰ الماس", reason = "روز تولد عضو",
                createdAtEpochMillis = now, createdBy = "system",
                metadata = mapOf("year" to today.year.toString())
            )
            granted++
        }
        if (granted > 0) persist()
        return granted
    }

    private fun validateBirthDate(value: String) {
        require(value.length == 10 && value[4] == '-' && value[7] == '-') { "تاریخ تولد باید به شکل YYYY-MM-DD باشد" }
        parseBirthDate(value)
    }

    private fun parseBirthDate(value: String): LocalDate = try {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("تاریخ تولد نامعتبر است؛ فرمت YYYY-MM-DD")
    }

    fun updateMember(member: Member) {
        val index = members.indexOfFirst { it.id == member.id }
        if (index >= 0) {
            members[index] = member
            persist()
        }
    }

    fun recordXp(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) {
            EconomyService.addXp(it, amount, reason, actor, System.currentTimeMillis())
        }

    fun recordPoints(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) {
            EconomyService.adjustPoints(it, amount, reason, actor, System.currentTimeMillis())
        }

    fun recordDiamonds(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) {
            EconomyService.adjustDiamonds(it, amount, reason, actor, System.currentTimeMillis())
        }

    fun decreaseXp(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) {
            EconomyService.decreaseXp(it, amount, reason, actor, System.currentTimeMillis())
        }

    fun spendPoints(memberId: String, amount: Int, reason: String, actor: String? = null) =
        mutate(memberId) {
            EconomyService.spendPoints(it, amount, reason, actor, System.currentTimeMillis())
        }

    fun decreaseLevel(memberId: String, targetLevel: Int, reason: String, actor: String? = null) =
        mutate(memberId) {
            EconomyService.decreaseLevel(it, targetLevel, reason, actor, System.currentTimeMillis())
        }

    private fun mutate(
        memberId: String,
        operation: (MemberEconomy) -> EconomyChange
    ): Member {
        val member = members.first { it.id == memberId }
        val change = operation(member.economy)

        history += change.events.map { it.copy(memberId = memberId) }

        val updated = member.copy(economy = change.economy)
        updateMember(updated)
        return updated
    }

    fun setWheel(config: WheelConfig) {
        wheel = config
        persist()
    }

    fun addMission(mission: Mission) {
        require(mission.xpReward >= 0 && mission.pointsReward >= 0 && mission.diamondReward >= 0)
        missions.removeAll { it.id == mission.id }
        missions += mission
        persist()
    }

    fun completeMission(
        mission: Mission,
        memberIds: List<String>,
        reason: String? = null
    ): MissionCompletion {
        check(MissionService.canComplete(mission, System.currentTimeMillis())) {
            "این مأموریت در حال حاضر قابل ثبت نیست"
        }

        val validIds = memberIds.distinct()
        check(validIds.isNotEmpty()) { "حداقل یک عضو لازم است" }
        validIds.forEach { id ->
            check(members.any { it.id == id }) { "عضو نامعتبر است" }
        }

        val completion = MissionService.completion(
            mission,
            validIds,
            System.currentTimeMillis(),
            reason
        )
        missionCompletions += completion

        validIds.forEach { memberId ->
            if (mission.xpReward > 0) {
                recordXp(memberId, mission.xpReward, "ماموریت: " + mission.title, "mentor")
            }
            if (mission.pointsReward > 0) {
                recordPoints(memberId, mission.pointsReward, "ماموریت: " + mission.title, "mentor")
            }
            if (mission.diamondReward > 0) {
                recordDiamonds(memberId, mission.diamondReward, "ماموریت: " + mission.title, "mentor")
            }

            history += HistoryEvent(
                "mission-" + completion.id + "-" + memberId,
                memberId,
                HistoryType.MISSION_COMPLETED,
                null,
                mission.title,
                reason,
                completion.completedAt,
                "mentor"
            )
        }

        persist()
        return completion
    }

    fun addSession(
        title: String,
        startsAt: Long,
        description: String = "",
        location: String? = null
    ): Session {
        require(title.isNotBlank()) { "عنوان جلسه الزامی است" }

        val session = Session(
            id = "session-" + (++sequence),
            title = title.trim(),
            description = description.trim(),
            startsAt = startsAt,
            location = location?.trim()?.takeIf { it.isNotEmpty() }
        )
        sessions += session
        persist()
        return session
    }

    fun addSession(session: Session) {
        sessions.removeAll { it.id == session.id }
        sessions += session
        persist()
    }

    fun recordAttendance(item: Attendance) {
        check(members.any { it.id == item.memberId }) { "عضو نامعتبر است" }
        check(sessions.any { it.id == item.sessionId }) { "جلسه نامعتبر است" }

        attendance.removeAll {
            it.memberId == item.memberId && it.sessionId == item.sessionId
        }
        attendance += item

        history += HistoryEvent(
            "attendance-" + item.id,
            item.memberId,
            HistoryType.ATTENDANCE,
            null,
            "حضور و غیاب",
            item.note,
            item.createdAt,
            "mentor",
            mapOf(
                "status" to item.status.name,
                "sessionId" to item.sessionId
            )
        )

        persist()
    }

    fun addShopItem(item: ShopItem) {
        require(item.price >= 0) { "قیمت نمی‌تواند منفی باشد" }
        shop.removeAll { it.id == item.id }
        shop += item
        persist()
    }

    fun addAsset(asset: CustomAsset) {
        assets.removeAll { it.id == asset.id }
        assets += asset
        persist()
    }

    fun persist() {
        store.save(
            ring,
            members,
            history,
            shop,
            wheel,
            missions,
            missionCompletions,
            sessions,
            attendance,
            assets
        )
    }

    fun exportBackup(): String {
        return store.exportJson() ?: run {
            persist()
            store.exportJson() ?: "{}"
        }
    }

    fun importBackup(raw: String) {
        store.importJson(raw)

        members.clear()
        history.clear()
        shop.clear()
        missions.clear()
        missionCompletions.clear()
        sessions.clear()
        attendance.clear()
        assets.clear()
        wheel = WheelConfig()

        store.load()?.let { state ->
            ring = state.ring
            members += state.members
            history += state.history
            shop += state.shop
            wheel = state.wheel
            missions += state.missions
            missionCompletions += state.missionCompletions
            sessions += state.sessions
            attendance += state.attendance
            assets += state.assets
        }
    }
}