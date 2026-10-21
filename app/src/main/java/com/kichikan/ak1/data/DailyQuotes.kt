package com.kichikan.ak1.data

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate

/**
 * Mentor-written sentences that appear at the top of the home screen.
 *
 * Every sentence is stored in `ak1_settings` as a JSON array, so the mentor can keep as many
 * as they like. One is picked per day deterministically: the same sentence stays all day and
 * changes automatically at midnight without any background work.
 */
object DailyQuotes {
    private const val PREFS = "ak1_settings"
    private const val KEY = "daily_quotes"

    fun list(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).trim().takeIf { it.isNotEmpty() }
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        save(context, list(context) + value)
    }

    fun remove(context: Context, text: String) {
        save(context, list(context) - text)
    }

    fun save(context: Context, quotes: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, JSONArray(quotes).toString())
            .apply()
    }

    /** The sentence of today; stable for the whole day and different tomorrow. */
    fun today(context: Context): String? {
        val quotes = list(context)
        if (quotes.isEmpty()) return null
        val index = (LocalDate.now().toEpochDay() % quotes.size).toInt()
        return quotes[index]
    }
}
