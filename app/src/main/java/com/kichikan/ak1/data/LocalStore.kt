package com.kichikan.ak1.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.kichikan.ak1.domain.model.*

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("ak1_local", Context.MODE_PRIVATE)

    fun save(ring: RingAccount?, members: List<Member>, history: List<HistoryEvent>) {
        val root = JSONObject()
        ring?.let {
            root.put("ringId", it.ringId).put("ringName", it.ringName)
                .put("ringUsername", it.ringUsername).put("passwordRequired", it.passwordRequired)
        }
        val ms = JSONArray()
        members.forEach {
            ms.put(JSONObject().put("id", it.id).put("ringId", it.ringId).put("name", it.name)
                .put("xp", it.economy.xp).put("points", it.economy.spendablePoints)
                .put("diamonds", it.economy.diamonds).put("avatar", it.avatarItemId ?: JSONObject.NULL)
                .put("frame", it.frameItemId ?: JSONObject.NULL))
        }
        root.put("members", ms)
        val hs = JSONArray()
        history.forEach {
            hs.put(JSONObject().put("id", it.id).put("memberId", it.memberId).put("type", it.type.name)
                .put("amount", it.amount ?: JSONObject.NULL).put("title", it.title)
                .put("reason", it.reason ?: JSONObject.NULL).put("createdAt", it.createdAtEpochMillis)
                .put("createdBy", it.createdBy ?: JSONObject.NULL))
        }
        root.put("history", hs)
        prefs.edit().putString("backup", root.toString()).apply()
    }

    fun load(): LoadedState? {
        val raw = prefs.getString("backup", null) ?: return null
        val root = JSONObject(raw)
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
                MemberEconomy(o.optInt("xp"), o.optInt("points"), o.optInt("diamonds")),
                o.optString("avatar").takeIf { it.isNotEmpty() && it != "null" },
                o.optString("frame").takeIf { it.isNotEmpty() && it != "null" }
            )
        }
        val history = mutableListOf<HistoryEvent>()
        val hs = root.optJSONArray("history") ?: JSONArray()
        for (i in 0 until hs.length()) {
            val o = hs.getJSONObject(i)
            history += HistoryEvent(
                o.getString("id"), o.getString("memberId"), HistoryType.valueOf(o.getString("type")),
                if (o.isNull("amount")) null else o.getInt("amount"), o.getString("title"),
                if (o.isNull("reason")) null else o.getString("reason"), o.getLong("createdAt"),
                if (o.isNull("createdBy")) null else o.getString("createdBy")
            )
        }
        return LoadedState(ring, members, history)
    }
}
data class LoadedState(val ring: RingAccount?, val members: List<Member>, val history: List<HistoryEvent>)
