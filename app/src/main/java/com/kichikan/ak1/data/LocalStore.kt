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
        groups: List<Group>,
        deletedMembers: List<Member> = emptyList()
    ) {
        val root = JSONObject().put("schemaVersion", 6)
        ring?.let {
            root.put("ringId", it.ringId).put("ringName", it.ringName)
                .put("ringUsername", it.ringUsername).put("passwordRequired", it.passwordRequired)
        }

        fun memberJson(member: Member) = JSONObject()
            .put("id", member.id).put("ringId", member.ringId).put("name", member.name)
            .put("xp", member.economy.xp).put("points", member.economy.spendablePoints)
            .put("diamonds", member.economy.diamonds)
            .put("halfDiamondUnits", member.economy.halfDiamondUnits)
            .put("avatar", member.avatarItemId ?: JSONObject.NULL)
            .put("frame", member.frameItemId ?: JSONObject.NULL)
            .put("birthDate", member.birthDate ?: JSONObject.NULL)
            .put("groupId", member.groupId ?: JSONObject.NULL)
            .put("privateNotes", member.privateNotes)
            .put("responsibility", member.responsibility)

        val ms = JSONArray()
        members.forEach { ms.put(memberJson(it)) }
        root.put("members", ms)
        val deletedMembersJson = JSONArray()
        deletedMembers.forEach { deletedMembersJson.put(memberJson(it)) }
        root.put("deletedMembers", deletedMembersJson)
        addAppSettingsToBackup(root)
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
        shop.forEach { item ->
            val itemJson = JSONObject().put("id", item.id).put("name", item.name).put("type", item.type.name)
                .put("imagePath", item.imagePath ?: JSONObject.NULL).put("price", item.price)
                .put("currency", item.currency.name).put("minimumLevel", item.minimumLevel ?: JSONObject.NULL)
                .put("methods", JSONArray(item.methods.map { method -> method.name }))
                .put("eventStart", item.eventStartEpochMillis ?: JSONObject.NULL)
                .put("eventEnd", item.eventEndEpochMillis ?: JSONObject.NULL)
                .put("stock", item.stock ?: JSONObject.NULL).put("active", item.active).put("description", item.description)
            item.imagePath?.let { imagePath ->
                readAssetBytes(imagePath)?.let { bytes ->
                    if (bytes.size <= 10 * 1024 * 1024) itemJson.put("imageBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
            }
            shopJson.put(itemJson)
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

    /**
     * Settings are part of a complete local backup too. The ring profile URI is intentionally
     * omitted because a content URI is device/provider-specific and may not work after restore.
     */
    private fun addAppSettingsToBackup(root: JSONObject) {
        val all = context.getSharedPreferences("ak1_settings", Context.MODE_PRIVATE).all
        val settings = JSONObject()
        val stringSetKeys = JSONArray()
        all.forEach { (key, value) ->
            if (key == "ring_profile_image" || value == null) return@forEach
            when (value) {
                is Set<*> -> {
                    settings.put(key, JSONArray(value.mapNotNull { it?.toString() }))
                    stringSetKeys.put(key)
                }
                is String, is Boolean, is Int, is Long, is Float, is Double -> settings.put(key, value)
            }
        }
        root.put("appSettings", settings)
        root.put("appSettingsStringSets", stringSetKeys)
    }

    private fun restoreAppSettings(root: JSONObject) {
        val settings = root.optJSONObject("appSettings") ?: return
        val setKeysJson = root.optJSONArray("appSettingsStringSets") ?: JSONArray()
        val setKeys = (0 until setKeysJson.length()).map { setKeysJson.getString(it) }.toSet()
        val prefs = context.getSharedPreferences("ak1_settings", Context.MODE_PRIVATE)
        val existingProfileImage = prefs.getString("ring_profile_image", null)
        val editor = prefs.edit().clear()
        if (existingProfileImage != null) editor.putString("ring_profile_image", existingProfileImage)
        val keys = settings.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "ring_profile_image") continue
            val value = settings.get(key)
            when {
                key in setKeys && value is JSONArray -> {
                    val values = (0 until value.length()).map { value.getString(it) }.toSet()
                    editor.putStringSet(key, values.toMutableSet())
                }
                value is Boolean -> editor.putBoolean(key, value)
                value is Number -> {
                    val number = value.toDouble()
                    if (number % 1.0 != 0.0) editor.putFloat(key, number.toFloat())
                    else if (number in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) editor.putInt(key, number.toInt())
                    else editor.putLong(key, number.toLong())
                }
                value is String -> editor.putString(key, value)
                value is JSONArray -> editor.putString(key, value.toString())
            }
        }
        check(editor.commit()) { "ذخیره تنظیمات بازیابی‌شده ناموفق بود" }
    }

    private fun restoreShopImageIfNeeded(base64: String?, id: String, originalPath: String?): String? {
        if (base64.isNullOrBlank() || originalPath.isNullOrBlank()) return originalPath
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val directory = File(context.filesDir, "custom_assets/backup_shop").apply { mkdirs() }
            val extension = originalPath.substringAfterLast('.', "img")
                .lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: "img"
            val safeId = id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { "shop" }
            val target = File(directory, "$safeId.$extension").canonicalFile
            require(target.parentFile == directory.canonicalFile) { "مسیر تصویر فروشگاه نامعتبر است" }
            if (!target.isFile || !target.readBytes().contentEquals(bytes)) target.writeBytes(bytes)
            target.absolutePath
        }.getOrDefault(originalPath)
    }

    private fun readAssetBytes(path: String): ByteArray? = runCatching {
        if (path.startsWith("content://")) context.contentResolver.openInputStream(Uri.parse(path))?.use { it.readBytes() }
        else File(path).takeIf { it.isFile }?.readBytes()
    }.getOrNull()

    private fun restoreAssetIfNeeded(base64: String?, id: String, originalPath: String, mimeType: String): String {
        if (base64.isNullOrBlank()) return originalPath
        return runCatching {
            val dir = File(context.filesDir, "custom_assets").apply { mkdirs() }
            val extension = when {
                mimeType.equals("image/jpeg", true) -> ".jpg"
                mimeType.equals("image/webp", true) -> ".webp"
                mimeType.equals("image/gif", true) -> ".gif"
                else -> ".png"
            }
            // Never use an imported ID as a path: backups are user-selected files and IDs
            // must not be able to escape the app's private asset directory.
            val safeId = id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { "asset" }
            val target = File(dir, "$safeId$extension").canonicalFile
            require(target.parentFile == dir.canonicalFile) { "مسیر فایل دارایی نامعتبر است" }
            // Prefer the bytes embedded in the backup even when a file already exists at
            // the old absolute path; that path may refer to unrelated content on this device.
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
        require(version in 1..6) { "نسخه پشتیبان پشتیبانی نمی‌شود" }

        // Validate before accepting the backup. Validation deliberately skips asset writes so
        // malformed data later in the file cannot overwrite an existing image on this device.
        val previous = prefs.getString("backup", null)
        prefs.edit().putString("backup", raw).commit()
        try {
            load(restoreAssets = false)
            restoreAppSettings(root)
        } catch (e: Exception) {
            val editor = prefs.edit()
            if (previous == null) editor.remove("backup") else editor.putString("backup", previous)
            editor.commit()
            throw IllegalArgumentException("فایل پشتیبان ناقص یا ناسازگار است؛ اطلاعات قبلی حفظ شد.", e)
        }
    }

    fun load(restoreAssets: Boolean = true): LoadedState? {
        val raw = prefs.getString("backup", null) ?: return null
        val root = JSONObject(raw)
        val version = root.optInt("schemaVersion", 0)
        require(version in 1..6) { "نسخه پشتیبان پشتیبانی نمی‌شود" }

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
                o.optString("privateNotes", ""),
                o.optString("responsibility", "")
            )
        }

        val deletedMembers = mutableListOf<Member>()
        val deletedMembersJson = root.optJSONArray("deletedMembers") ?: JSONArray()
        for (i in 0 until deletedMembersJson.length()) {
            val o = deletedMembersJson.getJSONObject(i)
            deletedMembers += Member(
                o.getString("id"), o.getString("ringId"), o.getString("name"),
                o.optInt("halfDiamondUnits", 0).let { units ->
                    MemberEconomy(o.optInt("xp"), o.optInt("points"), o.optInt("diamonds") + units / 2, units % 2)
                },
                o.optString("avatar").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("frame").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("birthDate").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("groupId").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("privateNotes", ""),
                o.optString("responsibility", "")
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
            val originalImagePath = o.optString("imagePath").takeIf { it.isNotEmpty() && it != "null" }
            val restoredImagePath = if (restoreAssets) {
                restoreShopImageIfNeeded(o.optString("imageBase64").takeIf { it.isNotBlank() }, o.getString("id"), originalImagePath)
            } else originalImagePath
            shop += ShopItem(
                o.getString("id"), o.getString("name"), ShopItemType.valueOf(o.getString("type")),
                restoredImagePath, o.optInt("price"),
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
            val restoredPath = if (restoreAssets) {
                restoreAssetIfNeeded(o.optString("dataBase64").takeIf { it.isNotBlank() }, o.getString("id"), originalPath, o.optString("mimeType", "image/png"))
            } else originalPath
            assets += CustomAsset(
                o.getString("id"), o.getString("name"), AssetType.valueOf(o.getString("type")),
                restoredPath, o.optString("mimeType", "image/png"),
                if (o.isNull("width")) null else o.getInt("width"),
                if (o.isNull("height")) null else o.getInt("height"), o.optBoolean("active", true)
            )
        }

        return LoadedState(ring, members, history, shop, wheel, missions, completions, sessions, attendance, assets, groups, deletedMembers)
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
    val groups: List<Group> = emptyList(),
    val deletedMembers: List<Member> = emptyList()
)
