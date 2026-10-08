package com.kichikan.ak1.data

import android.content.Context
import com.kichikan.ak1.domain.calendar.BirthdayRules
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.EconomyChange
import com.kichikan.ak1.domain.service.EconomyService
import com.kichikan.ak1.domain.service.MissionService
import com.kichikan.ak1.domain.service.WheelService
import java.time.Instant
import java.time.ZoneId

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
        val normalizedBirthDate = birthDate?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { BirthdayRules.normalize(it, java.time.LocalDate.now()) }

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

    /**
     * Processes the annual 30-diamond birthday reward exactly once per Jalali year.
     * Birthdays are Jalali dates (see [BirthdayRules]).
     */
    fun processBirthdayRewards(now: Long = System.currentTimeMillis()): Int {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val jalaliYear = JalaliCalendar.fromGregorian(today).year
        var granted = 0
        members.forEach { member ->
            val birth = BirthdayRules.parseStored(member.birthDate) ?: return@forEach
            if (!BirthdayRules.isBirthday(birth, today)) return@forEach
            // Judged by the event date, so rewards recorded by older versions still count.
            val alreadyGranted = history.any {
                it.memberId == member.id && it.type == HistoryType.BIRTHDAY_REWARD &&
                    JalaliCalendar.fromEpochMillis(it.createdAtEpochMillis, zone).year == jalaliYear
            }
            if (alreadyGranted) return@forEach
            recordDiamonds(member.id, 30, "پاداش تولد سال $jalaliYear", "system")
            history += HistoryEvent(
                id = "birthday-${member.id}-$jalaliYear", memberId = member.id,
                type = HistoryType.BIRTHDAY_REWARD, amount = 30,
                title = "پاداش تولد: ۳۰ الماس", reason = "روز تولد عضو",
                createdAtEpochMillis = now, createdBy = "system",
                metadata = mapOf("year" to jalaliYear.toString(), "calendar" to "jalali")
            )
            granted++
        }
        if (granted > 0) persist()
        return granted
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


    fun purchaseShopItem(memberId: String, itemId: String): Boolean {
        val item = shop.firstOrNull { it.id == itemId && it.active } ?: return false
        val member = members.firstOrNull { it.id == memberId } ?: return false
        item.minimumLevel?.let { require(member.economy.level >= it) { "سطح عضو کافی نیست" } }
        if (item.stock != null) require(item.stock > 0) { "موجودی جایزه تمام شده است" }
        when (item.currency) {
            Currency.POINTS -> spendPoints(memberId, item.price, "خرید ${item.name}", "mentor")
            Currency.DIAMONDS -> recordDiamonds(memberId, -item.price, "خرید ${item.name}", "mentor")
            Currency.NONE -> Unit
        }
        val index = shop.indexOfFirst { it.id == item.id }
        if (index >= 0 && item.stock != null) shop[index] = item.copy(stock = item.stock - 1)
        history += HistoryEvent(
            id = "purchase-${System.currentTimeMillis()}-$memberId", memberId = memberId,
            type = HistoryType.REWARD_RECEIVED, amount = item.price,
            title = "دریافت: ${item.name}", reason = "خرید از فروشگاه",
            createdAtEpochMillis = System.currentTimeMillis(), createdBy = "mentor",
            metadata = mapOf("itemId" to item.id, "currency" to item.currency.name)
        )
        persist()
        return true
    }

    fun spinWheel(memberId: String): WheelItem? {
        check(members.any { it.id == memberId }) { "عضو نامعتبر است" }
        when (wheel.mode) {
            WheelMode.FREE -> Unit
            WheelMode.POINTS -> if (wheel.spinCostPoints > 0) spendPoints(memberId, wheel.spinCostPoints, "هزینه گردونه", "mentor")
            WheelMode.POINTS_AND_DIAMONDS -> {
                if (wheel.spinCostPoints > 0) spendPoints(memberId, wheel.spinCostPoints, "هزینه گردونه", "mentor")
                if (wheel.spinCostDiamonds > 0) recordDiamonds(memberId, -wheel.spinCostDiamonds, "هزینه گردونه", "mentor")
            }
        }
        val previousWinnerIds = history.filter { it.memberId == memberId && it.type == HistoryType.WHEEL_REWARD }
            .mapNotNull { it.metadata["itemId"] }.toSet()
        val item = WheelService.spin(wheel, previousWinnerIds) ?: return null
        when (item.type) {
            WheelRewardType.POINTS -> item.amount?.takeIf { it > 0 }?.let { recordPoints(memberId, it, "گردونه: ${item.title}", "mentor") }
            WheelRewardType.DIAMONDS -> item.amount?.takeIf { it > 0 }?.let { recordDiamonds(memberId, it, "گردونه: ${item.title}", "mentor") }
            else -> Unit
        }
        history += HistoryEvent(
            id = "wheel-${System.currentTimeMillis()}-$memberId", memberId = memberId,
            type = HistoryType.WHEEL_REWARD, amount = item.amount, title = item.title,
            reason = item.customText ?: "پاداش گردونه", createdAtEpochMillis = System.currentTimeMillis(),
            createdBy = "mentor", metadata = mapOf("itemId" to item.id, "type" to item.type.name)
        )
        persist()
        return item
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