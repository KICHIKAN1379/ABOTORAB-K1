package com.kichikan.ak1.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import com.kichikan.ak1.R

/**
 * Low-volume background loop that plays while the app is on screen.
 *
 * The track is `res/raw/ak1_theme.wav` (a calm 32 s pad). Replace that file with any audio you
 * like and it keeps working; nothing else refers to the track.
 */
object BackgroundMusic {
    private const val PREFS = "ak1_settings"
    private const val KEY = "background_music"
    private const val VOLUME = 0.30f

    private var player: MediaPlayer? = null

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).apply()
        if (enabled) start(context) else pause()
    }

    /** Starts (or resumes) the loop. Does nothing when the mentor turned it off or the phone is silent. */
    fun start(context: Context) {
        if (!enabled(context)) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audio?.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        player?.let { existing ->
            runCatching { if (!existing.isPlaying) existing.start() }
            return
        }
        runCatching {
            val created = MediaPlayer.create(context.applicationContext, R.raw.ak1_theme) ?: return
            created.isLooping = true
            created.setVolume(VOLUME, VOLUME)
            created.start()
            player = created
        }
    }

    /** Pauses without releasing, so returning to the app resumes where it stopped. */
    fun pause() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    fun stop() {
        runCatching { player?.release() }
        player = null
    }
}
