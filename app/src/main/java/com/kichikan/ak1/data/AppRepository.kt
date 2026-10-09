package com.kichikan.ak1.data

import android.content.Context
import com.kichikan.ak1.domain.calendar.BirthdayRules
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.EconomyChange
import com.kichikan.ak1.domain.service.EconomyService
import com.kichikan.ak1.domain.service.MissionService
import com.kichikan.ak1.domain.service.ShopService
import com.kichikan.ak1.domain.service.WheelService
import java.time.Instant
import java.time.ZoneId

class AppRepository(context: Context) {
    private fun newId(prefix: String): String = "$prefix-${java.util.UUID.randomUUID()}"
    private val appContext = context.applicationContext
    private val store = LocalStore(appContext)

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
    val groups = mutableListOf<Group>()

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
            groups += state.groups
        }
        var unlockedAny = false
        members.toList().forEach { member ->
            val updated = grantLevelAvatars(member, (5..member.economy.level step 5).toList())
            if (updated != member) {
                val index = members.indexOfFirst { it.id == member.id }
                if (index >= 0) members[index] = updated
                unlockedAny = true
            }
        }
        if (unlockedAny) persist()
    }

    private fun grantLevelAvatars(member: Member, levels: List<Int>): Member {
        var updated = member
        levels.forEach { level ->
            val item = ensureLevelAvatarItem(level)
            val alreadyOwned = history.any { it.memberId == member.id && it.metadata["itemId"] == item.id }
            if (!alreadyOwned) {
                val now = System.currentTimeMillis()
                history += HistoryEvent(
                    id = "level-avatar-${member.id}-$level",
                    memberId = member.id,
                    type = HistoryType.REWARD_RECEIVED,
                    amount = null,
                    title = "هدیه آواتار سطح $level",
                    reason = "هدیه خودکار رسیدن به هر پنج سطح",
                    createdAtEpochMillis = now,
                    createdBy = "system",
                    metadata = mapOf("itemId" to item.id, "acquisition" to "LEVEL_UNLOCK", "level" to level.toString())
                )
                updated = updated.copy(avatarItemId = item.id)
            }
        }
        return updated
    }

    private fun ensureLevelAvatarItem(level: Int): ShopItem {
        val id = "level-avatar-$level"
        shop.firstOrNull { it.id == id }?.let { return it }
        val directory = java.io.File(appContext.filesDir, "custom_assets/level_avatars")
        directory.mkdirs()
        val file = java.io.File(directory, "$id.png")
        if (!file.exists()) createLevelAvatarBitmap(level).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, file.outputStream())
        val item = ShopItem(
            id = id,
            name = "آواتار ویژه سطح $level",
            type = ShopItemType.AVATAR,
            imagePath = file.absolutePath,
            price = 0,
            currency = Currency.NONE,
            minimumLevel = level,
            methods = setOf(AcquisitionMethod.LEVEL_UNLOCK),
            active = true,
            description = "آواتار هدیه‌ای که با رسیدن به سطح $level آزاد می‌شود."
        )
        shop += item
        return item
    }

    private fun createLevelAvatarBitmap(level: Int): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val palette = listOf(
            android.graphics.Color.rgb(48, 145, 160),
            android.graphics.Color.rgb(55, 105, 190),
            android.graphics.Color.rgb(120, 76, 180),
            android.graphics.Color.rgb(190, 125, 35),
            android.graphics.Color.rgb(45, 145, 90)
        )
        val tier = ((level / 5 - 1) / 2).coerceAtLeast(0)
        val accent = palette[tier % palette.size]
        fun paint(color: Int, style: android.graphics.Paint.Style = android.graphics.Paint.Style.FILL, width: Float = 1f) =
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.style = style; strokeWidth = width }
        canvas.drawCircle(128f, 128f, 120f, paint(android.graphics.Color.rgb(22, 33, 62)))
        canvas.drawCircle(128f, 128f, 111f, paint(accent, android.graphics.Paint.Style.STROKE, if (level >= 20) 9f else 5f))
        if (level >= 15) {
            canvas.drawCircle(128f, 128f, 98f, paint(android.graphics.Color.rgb(255, 215, 0), android.graphics.Paint.Style.STROKE, 3f))
        }
        canvas.drawOval(48f, 150f, 208f, 255f, paint(accent))
        canvas.drawRoundRect(108f, 135f, 148f, 180f, 14f, 14f, paint(android.graphics.Color.rgb(190, 125, 92)))
        canvas.drawCircle(128f, 100f, 53f, paint(android.graphics.Color.rgb(218, 166, 128)))
        canvas.drawArc(74f, 42f, 182f, 128f, 180f, 180f, true, paint(android.graphics.Color.rgb(35, 30, 32)))
        canvas.drawCircle(108f, 103f, 5f, paint(android.graphics.Color.rgb(25, 25, 25)))
        canvas.drawCircle(148f, 103f, 5f, paint(android.graphics.Color.rgb(25, 25, 25)))
        canvas.drawArc(104f, 112f, 152f, 139f, 10f, 160f, false, paint(android.graphics.Color.rgb(90, 45, 40), android.graphics.Paint.Style.STROKE, 4f))
        if (level >= 25) {
            val gold = paint(android.graphics.Color.rgb(255, 215, 0))
            val path = android.graphics.Path().apply {
                moveTo(90f, 48f); lineTo(80f, 18f); lineTo(112f, 37f); lineTo(128f, 8f)
                lineTo(145f, 37f); lineTo(176f, 18f); lineTo(166f, 48f); close()
            }
            canvas.drawPath(path, gold)
        } else if (level >= 10) {
            canvas.drawCircle(128f, 43f, 11f, paint(android.graphics.Color.rgb(255, 215, 0)))
        }
        val badge = paint(android.graphics.Color.rgb(255, 215, 0))
        canvas.drawCircle(190f, 190f, 24f, badge)
        val textPaint = paint(android.graphics.Color.rgb(22, 33, 62)).apply {
            textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        canvas.drawText(level.toString(), 190f, 197f, textPaint)
        return bitmap
    }

    fun createRing(name: String, username: String): RingAccount {
        require(name.isNotBlank()) { "نام حلقه الزامی است" }
        require(username.isNotBlank()) { "نام کاربری حلقه الزامی است" }
        check(ring == null) { "حلقه قبلاً ایجاد شده است" }

        val account = RingAccount(
            newId("ring"),
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
            newId("member"), ring!!.ringId, name.trim(), birthDate = normalizedBirthDate
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

    fun addGroup(name: String): Group {
        require(ring != null) { "ابتدا حلقه را ایجاد کنید" }
        require(name.isNotBlank()) { "نام گروه الزامی است" }
        val group = Group(newId("group"), ring!!.ringId, name.trim())
        groups += group; persist(); return group
    }

    fun updateGroup(group: Group) {
        val i = groups.indexOfFirst { it.id == group.id }
        if (i >= 0) {
            val validIds = group.memberIds.filter { id -> members.any { it.id == id } }.distinct()
            groups[i] = group.copy(memberIds = validIds, leaderMemberId = group.leaderMemberId?.takeIf { id -> validIds.contains(id) })
            members.indices.forEach { index ->
                val member = members[index]
                if (validIds.contains(member.id)) members[index] = member.copy(groupId = group.id)
                else if (member.groupId == group.id) members[index] = member.copy(groupId = null)
            }
            groups.indices.forEach { gi ->
                if (groups[gi].id != group.id && groups[gi].memberIds.any { validIds.contains(it) }) {
                    groups[gi] = groups[gi].copy(memberIds = groups[gi].memberIds.filterNot { validIds.contains(it) },
                        leaderMemberId = groups[gi].leaderMemberId?.takeUnless { validIds.contains(it) })
                }
            }
            persist()
        }
    }
    fun deleteGroup(groupId: String) {
        groups.removeAll { it.id == groupId }
        members.indices.reversed().forEach { i -> if (members[i].groupId == groupId) members[i] = members[i].copy(groupId = null) }
        persist()
    }
    fun assignMemberToGroup(memberId: String, groupId: String?) {
        val i = members.indexOfFirst { it.id == memberId }
        if (i >= 0) {
            members[i] = members[i].copy(groupId = groupId)
            groups.indices.forEach { gi ->
                val g = groups[gi]
                groups[gi] = if (g.id == groupId) g.copy(memberIds = (g.memberIds + memberId).distinct())
                else g.copy(memberIds = g.memberIds - memberId, leaderMemberId = g.leaderMemberId?.takeUnless { it == memberId })
            }
            persist()
        }
    }

    fun adjustGroupScore(groupId: String, xpDelta: Int, pointsDelta: Int, diamondsDelta: Int, reason: String) {
        require(reason.isNotBlank()) { "دلیل تغییر امتیاز گروه الزامی است" }
        val index = groups.indexOfFirst { it.id == groupId }
        require(index >= 0) { "گروه پیدا نشد" }
        val group = groups[index]
        val economy = group.economy
        require(economy.xp + xpDelta >= 0) { "XP گروه نمی‌تواند منفی شود" }
        require(economy.spendablePoints + pointsDelta >= 0) { "امتیاز گروه نمی‌تواند منفی شود" }
        require(economy.diamonds + diamondsDelta >= 0) { "الماس گروه نمی‌تواند منفی شود" }
        groups[index] = group.copy(economy = economy.copy(
            xp = economy.xp + xpDelta,
            spendablePoints = economy.spendablePoints + pointsDelta,
            diamonds = economy.diamonds + diamondsDelta
        ))
        val now = System.currentTimeMillis()
        fun log(delta: Int, type: HistoryType, label: String) {
            if (delta != 0) history += HistoryEvent(
                id = "group-${now}-${groupId}-${type.name}", memberId = groupId, type = type,
                amount = kotlin.math.abs(delta), title = "${label} گروه ${group.name}",
                reason = reason, createdAtEpochMillis = now, createdBy = "mentor",
                metadata = mapOf("entityType" to "GROUP", "groupId" to groupId, "delta" to delta.toString())
            )
        }
        log(xpDelta, if (xpDelta >= 0) HistoryType.XP_EARNED else HistoryType.XP_DECREASED, "XP")
        log(pointsDelta, if (pointsDelta >= 0) HistoryType.POINTS_EARNED else HistoryType.POINTS_DECREASED, "امتیاز")
        log(diamondsDelta, if (diamondsDelta >= 0) HistoryType.DIAMONDS_EARNED else HistoryType.DIAMONDS_DECREASED, "الماس")
        persist()
    }

    fun deleteMember(memberId: String) {
        members.removeAll { it.id == memberId }
        history.removeAll { it.memberId == memberId }
        attendance.removeAll { it.memberId == memberId }
        sessions.removeAll { it.memberId == memberId }
        missionCompletions.indices.reversed().forEach { index ->
            val completion = missionCompletions[index]
            val remainingMemberIds = completion.memberIds.filterNot { it == memberId }
            if (remainingMemberIds.isEmpty()) missionCompletions.removeAt(index)
            else if (remainingMemberIds.size != completion.memberIds.size) {
                missionCompletions[index] = completion.copy(memberIds = remainingMemberIds)
            }
        }
        groups.indices.forEach { index ->
            val group = groups[index]
            groups[index] = group.copy(
                memberIds = group.memberIds.filterNot { it == memberId },
                leaderMemberId = group.leaderMemberId?.takeUnless { it == memberId }
            )
        }
        persist()
    }

    fun updateMember(member: Member) {
        val index = members.indexOfFirst { it.id == member.id }
        if (index >= 0) {
            members[index] = member
            groups.indices.forEach { gi ->
                val group = groups[gi]
                groups[gi] = when {
                    group.id == member.groupId -> group.copy(memberIds = (group.memberIds + member.id).distinct())
                    else -> group.copy(memberIds = group.memberIds - member.id,
                        leaderMemberId = group.leaderMemberId?.takeUnless { it == member.id })
                }
            }
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

        var updated = member.copy(economy = change.economy)
        if (change.economy.level > member.economy.level) {
            updated = grantLevelAvatars(updated, (5..change.economy.level step 5).filter { it > member.economy.level })
        }
        updateMember(updated)
        return updated
    }

    fun updateWheelItem(item: WheelItem) { setWheel(wheel.copy(items = wheel.items.map { if (it.id == item.id) item else it })) }
    fun deleteWheelItem(itemId: String) { setWheel(wheel.copy(items = wheel.items.filterNot { it.id == itemId })) }

    fun setWheel(config: WheelConfig) {
        wheel = config
        persist()
    }

    fun updateMission(mission: Mission) { val i = missions.indexOfFirst { it.id == mission.id }; if (i >= 0) { missions[i] = mission; persist() } }
    fun deleteMission(missionId: String) { missions.removeAll { it.id == missionId }; missionCompletions.removeAll { it.missionId == missionId }; persist() }

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

    fun addSession(memberId: String, title: String, topic: String = "", location: String? = null): Session {
        require(members.any { it.id == memberId }) { "عضو نامعتبر است" }
        require(title.isNotBlank()) { "عنوان جلسه الزامی است" }
        val session = Session(newId("session"), memberId, title.trim(), topic.trim(), System.currentTimeMillis(), location?.trim()?.takeIf { it.isNotEmpty() })
        sessions += session
        persist()
        return session
    }

    fun updateSession(session: Session) { val i = sessions.indexOfFirst { it.id == session.id }; if (i >= 0) { sessions[i] = session; persist() } }
    fun deleteSession(sessionId: String) { sessions.removeAll { it.id == sessionId }; attendance.removeAll { it.sessionId == sessionId }; persist() }

    fun addSession(session: Session) {
        sessions.removeAll { it.id == session.id }
        sessions += session
        persist()
    }

    fun recordAttendance(item: Attendance) {
        check(members.any { it.id == item.memberId }) { "عضو نامعتبر است" }
        attendance.removeAll { it.memberId == item.memberId && it.dateEpochMillis == item.dateEpochMillis }
        attendance += item
        history += HistoryEvent("attendance-" + item.id, item.memberId, HistoryType.ATTENDANCE, null,
            "حضور و غیاب", item.note, item.createdAt, "mentor",
            mapOf("status" to item.status.name, "date" to item.dateEpochMillis.toString()))
        persist()
    }

    fun recordWeeklyAttendance(memberId: String, dateEpochMillis: Long, status: AttendanceStatus, note: String? = null) {
        recordAttendance(Attendance("attendance-$memberId-$dateEpochMillis", memberId, dateEpochMillis, status, null, note, System.currentTimeMillis()))
    }


    fun purchaseShopItem(memberId: String, itemId: String): Boolean {
        val item = shop.firstOrNull { it.id == itemId } ?: return false
        val member = members.firstOrNull { it.id == memberId } ?: return false
        val now = System.currentTimeMillis()
        val acquisitionError = ShopService.canAcquire(item, member, now)
        require(acquisitionError == null) { acquisitionError ?: "این آیتم قابل دریافت نیست" }
        if (item.stock != null) require(item.stock > 0) { "موجودی جایزه تمام شده است" }
        val alreadyOwned = item.type != ShopItemType.REWARD && history.any { it.memberId == memberId && it.type == HistoryType.REWARD_RECEIVED && it.metadata["itemId"] == item.id }
        if (alreadyOwned) {
            equipShopItem(memberId, itemId)
            return true
        }
        val freeLevelUnlock = ShopService.isFreeLevelUnlock(item)
        if (!freeLevelUnlock && ShopService.hasDirectPurchase(item)) {
            when (item.currency) {
                Currency.POINTS -> if (item.price > 0) spendPoints(memberId, item.price, "خرید ${item.name}", "mentor")
                Currency.DIAMONDS -> if (item.price > 0) recordDiamonds(memberId, -item.price, "خرید ${item.name}", "mentor")
                Currency.NONE -> Unit
            }
        }
        val current = members.first { it.id == memberId }
        val equipped = when (item.type) {
            ShopItemType.AVATAR -> current.copy(avatarItemId = item.id)
            ShopItemType.FRAME -> current.copy(frameItemId = item.id)
            ShopItemType.REWARD -> current
        }
        members[members.indexOfFirst { it.id == memberId }] = equipped
        val index = shop.indexOfFirst { it.id == item.id }
        if (index >= 0 && item.stock != null) shop[index] = item.copy(stock = item.stock - 1)
        history += HistoryEvent(
            "purchase-${now}-${memberId}", memberId, HistoryType.REWARD_RECEIVED, item.price,
            "دریافت: ${item.name}", if (freeLevelUnlock) "بازشدن با سطح" else "دریافت از فروشگاه",
            now, "mentor", mapOf("itemId" to item.id, "currency" to item.currency.name,
                "acquisition" to if (freeLevelUnlock) "LEVEL_UNLOCK" else "PURCHASE")
        )
        persist()
        return true
    }

    fun equipShopItem(memberId: String, itemId: String) {
        val member = members.firstOrNull { it.id == memberId } ?: error("عضو نامعتبر است")
        val item = shop.firstOrNull { it.id == itemId && it.active } ?: error("آیتم پیدا نشد")
        require(item.type == ShopItemType.AVATAR || item.type == ShopItemType.FRAME) { "این آیتم قابل استفاده نیست" }
        require(ShopService.canAcquire(item, member, System.currentTimeMillis()) == null) { "این آیتم هنوز قابل استفاده نیست" }
        val updated = when (item.type) {
            ShopItemType.AVATAR -> member.copy(avatarItemId = item.id)
            ShopItemType.FRAME -> member.copy(frameItemId = item.id)
            ShopItemType.REWARD -> member
        }
        members[members.indexOfFirst { it.id == memberId }] = updated
        history += HistoryEvent("equip-${System.currentTimeMillis()}-${memberId}-${item.id}", memberId,
            HistoryType.REWARD_RECEIVED, null, "استفاده از ${item.name}", "انتخاب آواتار/قاب",
            System.currentTimeMillis(), "mentor", mapOf("itemId" to item.id, "type" to item.type.name))
        persist()
    }
    fun spinWheel(memberId: String): WheelItem? {
        val member = members.firstOrNull { it.id == memberId } ?: error("عضو نامعتبر است")
        require(wheel.spinCostPoints >= 0 && wheel.spinCostDiamonds >= 0) { "هزینه گردونه نمی‌تواند منفی باشد" }

        // Select a valid prize before charging anything. If all prizes are exhausted,
        // the member must not lose points or diamonds.
        val previousWinnerIds = history.filter { it.memberId == memberId && it.type == HistoryType.WHEEL_REWARD }
            .mapNotNull { it.metadata["itemId"] }.toSet()
        // Ignore stale or exhausted linked shop prizes before choosing, so a broken
        // catalog reference can never consume a spin or charge the member.
        val eligibleItems = wheel.items.filter { wheelItem ->
            val linked = wheelItem.shopItemId?.let { id -> shop.firstOrNull { it.id == id } }
            when (wheelItem.type) {
                WheelRewardType.POINTS, WheelRewardType.DIAMONDS, WheelRewardType.CUSTOM ->
                    wheelItem.shopItemId == null
                WheelRewardType.AVATAR -> linked?.let { it.active && (it.stock == null || it.stock > 0) && it.type == ShopItemType.AVATAR } == true
                WheelRewardType.FRAME -> linked?.let { it.active && (it.stock == null || it.stock > 0) && it.type == ShopItemType.FRAME } == true
                WheelRewardType.REWARD -> linked?.let { it.active && (it.stock == null || it.stock > 0) && it.type == ShopItemType.REWARD } == true
            }
        }
        val item = WheelService.spin(wheel.copy(items = eligibleItems), previousWinnerIds) ?: return null

        val pointsCost = when (wheel.mode) {
            WheelMode.FREE -> 0
            WheelMode.POINTS, WheelMode.POINTS_AND_DIAMONDS -> wheel.spinCostPoints
        }
        val diamondsCost = if (wheel.mode == WheelMode.POINTS_AND_DIAMONDS) wheel.spinCostDiamonds else 0
        require(member.economy.spendablePoints >= pointsCost) { "امتیاز کافی برای چرخاندن گردونه نداری" }
        require(member.economy.diamonds >= diamondsCost) { "الماس کافی برای چرخاندن گردونه نداری" }

        // Validate both balances before either mutation so a failed spin cannot partially charge.
        if (pointsCost > 0) spendPoints(memberId, pointsCost, "هزینه گردونه", "mentor")
        if (diamondsCost > 0) recordDiamonds(memberId, -diamondsCost, "هزینه گردونه", "mentor")

        when (item.type) {
            WheelRewardType.POINTS -> item.amount?.takeIf { it > 0 }?.let { recordPoints(memberId, it, "گردونه: ${item.title}", "mentor") }
            WheelRewardType.DIAMONDS -> item.amount?.takeIf { it > 0 }?.let { recordDiamonds(memberId, it, "گردونه: ${item.title}", "mentor") }
            WheelRewardType.AVATAR, WheelRewardType.FRAME, WheelRewardType.REWARD -> {
                val linked = shop.first { it.id == item.shopItemId && it.active && (it.stock == null || it.stock > 0) }
                val memberIndex = members.indexOfFirst { it.id == memberId }
                val current = members[memberIndex]
                members[memberIndex] = when (linked.type) {
                    ShopItemType.AVATAR -> current.copy(avatarItemId = linked.id)
                    ShopItemType.FRAME -> current.copy(frameItemId = linked.id)
                    ShopItemType.REWARD -> current
                }
                val shopIndex = shop.indexOfFirst { it.id == linked.id }
                if (shopIndex >= 0 && linked.stock != null) shop[shopIndex] = linked.copy(stock = linked.stock - 1)
                history += HistoryEvent(
                    id = "wheel-item-${System.currentTimeMillis()}-$memberId",
                    memberId = memberId, type = HistoryType.REWARD_RECEIVED, amount = null,
                    title = "دریافت از گردونه: ${linked.name}", reason = item.customText ?: "جایزه گردونه",
                    createdAtEpochMillis = System.currentTimeMillis(), createdBy = "mentor",
                    metadata = mapOf("itemId" to linked.id, "acquisition" to "WHEEL", "type" to linked.type.name)
                )
            }
            WheelRewardType.CUSTOM -> Unit
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
    fun updateShopItem(item: ShopItem) { val i = shop.indexOfFirst { it.id == item.id }; if (i >= 0) { shop[i] = item; persist() } }
    fun deleteShopItem(itemId: String) { shop.removeAll { it.id == itemId }; persist() }

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
            assets,
            groups
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
        groups.clear()
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
            groups += state.groups
        }
    }
}