package com.kichikan.ak1.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Theme presets. All three stay dark so the neon accent colours keep their contrast;
 * they differ in how deep the backdrop and the card surfaces are.
 */
enum class ThemeMode(val label: String) {
    NEON("نئون"),
    AMOLED("مشکی خالص"),
    MIDNIGHT("نیمه‌شب")
}

/**
 * Process-wide theme selection, persisted in the same preferences file the rest of the
 * settings screen uses. [mode] is Compose state, so switching it re-themes the whole app.
 */
object ThemeState {
    private const val PREFS = "ak1_settings"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.NEON)
        private set

    fun load(context: Context) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode = runCatching { ThemeMode.valueOf(raw.orEmpty()) }.getOrDefault(ThemeMode.NEON)
    }

    fun set(context: Context, value: ThemeMode) {
        mode = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, value.name)
            .apply()
    }
}
