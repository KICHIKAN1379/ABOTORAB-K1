package com.kichikan.ak1.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import com.kichikan.ak1.domain.model.*

class LocalStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("ak1_local", Context.MODE_PRIVATE)

    fun save(
        ring: RingAccount?,
        members: List<Member>,
        history: List<HistoryEvent>,
        shop: List<ShopItem>,
        wheel: WheelConfig,
        missions: List<Mission>,
        missionCompletions: List<MissionCompletion>,
        sessions: List<Session>,
        attendance: List<Attendance>,
        assets: List<CustomAsset>,
        groups: List<Group>
    ) {
        val root = JSONObject().put("schemaVersion", 5)
        ring?.let {
            root.put("ringId", it.ringId).put("ringName", it.ringName)
                .put("ringUsername", it.ringUsername).put("passwordRequired", it.passwordRequired)
        }

        val ms = JSONArray()
        members.forEach {
            ms.put(JSONObject().put("id", it.id).put("ringId", it.ringId).put("name", it.name)
                .put("xp", it.economy.xp).put("points", it.economy.spendablePoints)
                .put("diamonds", it.economy.diamonds)
                .put("halfDiamondUnits", it.economy.halfDiamondUnits)
                .put("avatar", it.avatarItemId ?: JSONObject.NULL)
                .put("frame", it.frameItemId ?: JSONObject.NULL)
                .put("birthDate", it.birthDate ?: JSONObject.NULL).put("groupId", it.groupId ?: JSONObject.NULL).put("privateNotes", it.privateNotes))
        }
        root.put("members", ms)
        val groupsJson = JSONArray()
        groups.forEach {
            groupsJson.put(JSONObject().put("id", it.id).put("ringId", it.ringId).put("name", it.name)
                .put("leaderMemberId", it.leaderMemberId ?: JSONObject.NULL)
                .put("memberIds", JSONArray(it.memberIds))
                .put("xp", it.economy.xp).put("points", it.economy.spendablePoints)
                .put("diamonds", it.economy.diamonds).put("halfDiamondUnits", it.economy.halfDiamondUnits)
                .put("levelOverride", it.economy.levelOverride ?: JSONObject.NULL))
        }
        root.put("groups", groupsJson)

        val hs = JSONArray()
        history.forEach {
            hs.put(JSONObject().put("id", it.id).put("memberId", it.memberId).put("type", it.type.name)
                .put("amount", it.amount ?: JSONObject.NULL).put("title", it.title)
                .put("reason", it.reason ?: JSONObject.NULL).put("createdAt", it.createdAtEpochMillis)
                .put("createdBy", it.createdBy ?: JSONObject.NULL)
                .put("metadata", JSONObject(it.metadata)))
        }
        root.put("history", hs)

        val shopJson = JSONArray()
        shop.forEach {
            shopJson.put(JSONObject().put("id", it.id).put("name", it.name).put("type", it.type.name)
                .put("imagePath", it.imagePath ?: JSONObject.NULL).put("price", it.price)
                .put("currency", it.currency.name).put("minimumLevel", it.minimumLevel ?: JSONObject.NULL)
                .put("methods", JSONArray(it.methods.map { method -> method.name }))
                .put("eventStart", it.eventStartEpochMillis ?: JSONObject.NULL)
                .put("eventEnd", it.eventEndEpochMillis ?: JSONObject.NULL)
                .put("stock", it.stock ?: JSONObject.NULL).put("active", it.active).put("description", it.description))
        }
        root.put("shop", shopJson)

        val wheelJson = JSONObject()
            .put("mode", wheel.mode.name)
            .put("spinCostPoints", wheel.spinCostPoints)
            .put("spinCostDiamonds", wheel.spinCostDiamonds)
            .put("freeSpin", wheel.freeSpin)
            .put("allowRepeatAfterWin", wheel.allowRepeatAfterWin)
        val wheelItems = JSONArray()
        wheel.items.forEach {
            wheelItems.put(JSONObject().put("id", it.id).put("title", it.title).put("type", it.type.name)
                .put("amount", it.amount ?: JSONObject.NULL).put("shopItemId", it.shopItemId ?: JSONObject.NULL)
                .put("customText", it.customText ?: JSONObject.NULL).put("weight", it.weight).put("active", it.active))
        }
        wheelJson.put("items", wheelItems)
        root.put("wheel", wheelJson)

        val missionsJson = JSONArray()
        missions.forEach {
            missionsJson.put(JSONObject().put("id", it.id).put("title", it.title).put("description", it.description)
                .put("type", it.type.name).put("xpReward", it.xpReward).put("pointsReward", it.pointsReward)
                .put("diamondReward", it.diamondReward).put("active", it.active)
                .put("startAt", it.startAt ?: JSONObject.NULL).put("endAt", it.endAt ?: JSONObject.NULL))
        }
        root.put("missions", missionsJson)

        val completionsJson = JSONArray()
        missionCompletions.forEach {
            completionsJson.put(JSONObject().put("id", it.id).put("missionId", it.missionId)
                .put("memberIds", JSONArray(it.memberIds)).put("completedAt", it.completedAt)
                .put("reason", it.reason ?: JSONObject.NULL))
        }
        root.put("missionCompletions", completionsJson)

        val sessionsJson = JSONArray()
        sessions.forEach {
            sessionsJson.put(JSONObject().put("id", it.id).put("title", it.title)
                .put("topic", it.topic).put("memberId", it.memberId).put("startsAt", it.startsAt)
                .put("location", it.location ?: JSONObject.NULL))
        }
        root.put("sessions", sessionsJson)

        val attendanceJson = JSONArray()
        attendance.forEach {
            attendanceJson.put(JSONObject().put("id", it.id).put("memberId", it.memberId)
                .put("date", it.dateEpochMillis).put("sessionId", it.sessionId ?: JSONObject.NULL).put("status", it.status.name)
                .put("note", it.note ?: JSONObject.NULL).put("createdAt", it.createdAt))
        }
        root.put("attendance", attendanceJson)

        val assetsJson = JSONArray()
        assets.forEach {
            val assetJson = JSONObject().put("id", it.id).put("name", it.name).put("type", it.type.name)
                .put("path", it.path).put("mimeType", it.mimeType)
                .put("width", it.width ?: JSONObject.NULL).put("height", it.height ?: JSONObject.NULL)
                .put("active", it.active)
            readAssetBytes(it.path)?.let { bytes ->
                if (bytes.size <= 10 * 1024 * 1024) assetJson.put("dataBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
            }
            assetsJson.put(assetJson)
        }
        root.put("assets", assetsJson)

        prefs.edit().putString("backup", root.toString()).apply()
    }

    private fun readAssetBytes(path: String): ByteArray? = runCatching {
        if (path.startsWith("content://")) context.contentResolver.openInputStream(Uri.parse(path))?.use { it.readBytes() }
        else File(path).takeIf { it.isFile }?.readBytes()
    }.getOrNull()

    private fun restoreAssetIfNeeded(base64: String?, id: String, originalPath: String, mimeType: String): String {
        if (base64.isNullOrBlank()) return originalPath
        val existing = runCatching { if (originalPath.startsWith("content://")) null else File(originalPath).takeIf { it.isFile }?.absolutePath }.getOrNull()
        if (existing != null) return existing
        return runCatching {
            val dir = File(context.filesDir, "custom_assets").apply { mkdirs() }
            val extension = when { mimeType.equals("image/jpeg", true) -> ".jpg"; mimeType.equals("image/webp", true) -> ".webp"; mimeType.equals("image/gif", true) -> ".gif"; else -> ".png" }
            val target = File(dir, id + extension)
            target.writeBytes(Base64.decode(base64, Base64.DEFAULT))
            target.absolutePath
        }.getOrDefault(originalPath)
    }
    fun exportJson(): String? = prefs.getString("backup", null)

    fun importJson(raw: String) {
        val root = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("ساختار فایل پشتیبان معتبر نیست", e)
        }
        val version = root.optInt("schemaVersion", 0)
        require(version in 1..5) { "نسخه پشتیبان پشتیبانی نمی‌شود" }

        // Validate the entire backup before accepting it. If parsing any nested record fails,
        // restore the previous saved data instead of leaving the app with a broken backup.
        val previous = prefs.getString("backup", null)
        prefs.edit().putString("backup", raw).commit()
        try {
            load()
        } catch (e: Exception) {
            val editor = prefs.edit()
            if (previous == null) editor.remove("backup") else editor.putString("backup", previous)
            editor.commit()
            throw IllegalArgumentException("فایل پشتیبان ناقص یا ناسازگار است؛ اطلاعات قبلی حفظ شد.", e)
        }
    }

    fun load(): LoadedState? {
        val raw = prefs.getString("backup", null) ?: return null
        val root = JSONObject(raw)
        val version = root.optInt("schemaVersion", 0)
        require(version in 1..5) { "نسخه پشتیبان پشتیبانی نمی‌شود" }

        val ring = if (root.has("ringId")) RingAccount(
            root.getString("ringId"), root.getString("ringName"),
            root.getString("ringUsername"), root.optBoolean("passwordRequired", false)
        ) else null

        val members = mutableListOf<Member>()
        val ms = root.optJSONArray("members") ?: JSONArray()
        for (i in 0 until ms.length()) {
            val o = ms.getJSONObject(i)
            members += Member(
                o.getString("id"), o.getString("ringId"), o.getString("name"),
                // Older saves kept every half-diamond unit pending; carry pairs into whole diamonds.
                o.optInt("halfDiamondUnits", 0).let { units ->
                    MemberEconomy(o.optInt("xp"), o.optInt("points"), o.optInt("diamonds") + units / 2, units % 2)
                },
                o.optString("avatar").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("frame").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("birthDate").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("groupId").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("privateNotes", "")
            )
        }

        val groups = mutableListOf<Group>()
        val groupsJson = root.optJSONArray("groups") ?: JSONArray()
        for (i in 0 until groupsJson.length()) {
            val o = groupsJson.getJSONObject(i)
            val memberIdsJson = o.optJSONArray("memberIds") ?: JSONArray()
            val memberIds = (0 until memberIdsJson.length()).map { memberIdsJson.getString(it) }
            groups += Group(
                o.getString("id"), o.getString("ringId"), o.getString("name"),
                o.optString("leaderMemberId").takeIf { it.isNotBlank() && it != "null" },
                memberIds,
                MemberEconomy(
                    xp = o.optInt("xp", 0), spendablePoints = o.optInt("points", 0),
                    diamonds = o.optInt("diamonds", 0), halfDiamondUnits = o.optInt("halfDiamondUnits", 0),
                    levelOverride = if (o.isNull("levelOverride")) null else o.optInt("levelOverride")
                )
            )
        }

        val history = mutableListOf<HistoryEvent>()
        val hs = root.optJSONArray("history") ?: JSONArray()
        for (i in 0 until hs.length()) {
            val o = hs.getJSONObject(i)
            val metadata = mutableMapOf<String, String>()
            o.optJSONObject("metadata")?.keys()?.forEach { key -> metadata[key] = o.getJSONObject("metadata").optString(key) }
            history += HistoryEvent(
                o.getString("id"), o.getString("memberId"), HistoryType.valueOf(o.getString("type")),
                if (o.isNull("amount")) null else o.getInt("amount"), o.getString("title"),
                if (o.isNull("reason")) null else o.getString("reason"), o.getLong("createdAt"),
                if (o.isNull("createdBy")) null else o.getString("createdBy"), metadata
            )
        }

        val shop = mutableListOf<ShopItem>()
        val shopJson = root.optJSONArray("shop") ?: JSONArray()
        for (i in 0 until shopJson.length()) {
            val o = shopJson.getJSONObject(i)
            val methods = mutableSetOf<AcquisitionMethod>()
            val methodsJson = o.optJSONArray("methods") ?: JSONArray()
            for (j in 0 until methodsJson.length()) methods += AcquisitionMethod.valueOf(methodsJson.getString(j))
            shop += ShopItem(
                o.getString("id"), o.getString("name"), ShopItemType.valueOf(o.getString("type")),
                o.optString("imagePath").takeIf { it.isNotEmpty() && it != "null" }, o.optInt("price"),
                Currency.valueOf(o.optString("currency", Currency.NONE.name)),
                if (o.isNull("minimumLevel")) null else o.getInt("minimumLevel"), methods,
                if (o.isNull("eventStart")) null else o.getLong("eventStart"),
                if (o.isNull("eventEnd")) null else o.getLong("eventEnd"),
                if (o.isNull("stock")) null else o.getInt("stock"), o.optBoolean("active", true), o.optString("description", "")
            )
        }

        val wheelObj = root.optJSONObject("wheel")
        val wheelItems = mutableListOf<WheelItem>()
        wheelObj?.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                wheelItems += WheelItem(
                    o.getString("id"), o.getString("title"), WheelRewardType.valueOf(o.getString("type")),
                    if (o.isNull("amount")) null else o.getInt("amount"),
                    o.optString("shopItemId").takeIf { it.isNotEmpty() && it != "null" },
                    o.optString("customText").takeIf { it.isNotEmpty() && it != "null" },
                    o.optInt("weight", 1), o.optBoolean("active", true)
                )
            }
        }
        val wheel = WheelConfig(
            mode = runCatching { WheelMode.valueOf(wheelObj?.optString("mode", WheelMode.FREE.name) ?: WheelMode.FREE.name) }.getOrDefault(WheelMode.FREE),
            spinCostPoints = wheelObj?.optInt("spinCostPoints", 0) ?: 0,
            spinCostDiamonds = wheelObj?.optInt("spinCostDiamonds", 0) ?: 0,
            freeSpin = wheelObj?.optBoolean("freeSpin", true) ?: true,
            wheelItems,
            wheelObj?.optBoolean("allowRepeatAfterWin", false) ?: false
        )

        val missions = mutableListOf<Mission>()
        val missionsJson = root.optJSONArray("missions") ?: JSONArray()
        for (i in 0 until missionsJson.length()) {
            val o = missionsJson.getJSONObject(i)
            missions += Mission(
                o.getString("id"), o.getString("title"), o.optString("description"),
                MissionType.valueOf(o.optString("type", MissionType.INDIVIDUAL.name)),
                o.optInt("xpReward"), o.optInt("pointsReward"), o.optInt("diamondReward"),
                o.optBoolean("active", true),
                if (o.isNull("startAt")) null else o.getLong("startAt"),
                if (o.isNull("endAt")) null else o.getLong("endAt")
            )
        }

        val completions = mutableListOf<MissionCompletion>()
        val completionsJson = root.optJSONArray("missionCompletions") ?: JSONArray()
        for (i in 0 until completionsJson.length()) {
            val o = completionsJson.getJSONObject(i)
            val ids = mutableListOf<String>()
            val idsJson = o.optJSONArray("memberIds") ?: JSONArray()
            for (j in 0 until idsJson.length()) ids += idsJson.getString(j)
            completions += MissionCompletion(
                o.getString("id"), o.getString("missionId"), ids, o.getLong("completedAt"),
                if (o.isNull("reason")) null else o.getString("reason")
            )
        }

        val sessions = mutableListOf<Session>()
        val sessionsJson = root.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sessionsJson.length()) {
            val o = sessionsJson.getJSONObject(i)
            sessions += Session(
                o.getString("id"), o.optString("memberId", ""), o.getString("title"), o.optString("topic", o.optString("description")),
                o.getLong("startsAt"), o.optString("location").takeIf { it.isNotEmpty() && it != "null" }
            )
        }

        val attendance = mutableListOf<Attendance>()
        val attendanceJson = root.optJSONArray("attendance") ?: JSONArray()
        for (i in 0 until attendanceJson.length()) {
            val o = attendanceJson.getJSONObject(i)
            attendance += Attendance(
                o.getString("id"), o.getString("memberId"), o.optLong("date", o.optLong("createdAt")), 
                runCatching { AttendanceStatus.valueOf(o.getString("status")) }.getOrDefault(AttendanceStatus.UNMARKED),
                o.optString("sessionId").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("note").takeIf { it.isNotEmpty() && it != "null" }, o.optLong("createdAt")
            )
        }

        val assets = mutableListOf<CustomAsset>()
        val assetsJson = root.optJSONArray("assets") ?: JSONArray()
        for (i in 0 until assetsJson.length()) {
            val o = assetsJson.getJSONObject(i)
            val originalPath = o.getString("path")
            val restoredPath = restoreAssetIfNeeded(o.optString("dataBase64").takeIf { it.isNotBlank() }, o.getString("id"), originalPath, o.optString("mimeType", "image/png"))
            assets += CustomAsset(
                o.getString("id"), o.getString("name"), AssetType.valueOf(o.getString("type")),
                restoredPath, o.optString("mimeType", "image/png"),
                if (o.isNull("width")) null else o.getInt("width"),
                if (o.isNull("height")) null else o.getInt("height"), o.optBoolean("active", true)
            )
        }

        return LoadedState(ring, members, history, shop, wheel, missions, completions, sessions, attendance, assets, groups)
    }
}

data class LoadedState(
    val ring: RingAccount?,
    val members: List<Member>,
    val history: List<HistoryEvent>,
    val shop: List<ShopItem> = emptyList(),
    val wheel: WheelConfig = WheelConfig(),
    val missions: List<Mission> = emptyList(),
    val missionCompletions: List<MissionCompletion> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val attendance: List<Attendance> = emptyList(),
    val assets: List<CustomAsset> = emptyList(),
    val groups: List<Group> = emptyList()
)
