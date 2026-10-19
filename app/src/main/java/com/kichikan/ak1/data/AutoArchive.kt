package com.kichikan.ak1.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import java.io.File
import java.time.ZonedDateTime
import java.util.concurrent.Executors

/**
 * Writes a dated JSON snapshot into a folder named `MRT` every time the mentor leaves the app.
 *
 * On Android 10+ direct writes to the root of shared storage are not allowed, so the folder is
 * created through MediaStore inside `Download/MRT` (visible in any file manager, no permission
 * needed). Android 8-9 still writes `MRT/` at the root of shared storage with the legacy
 * WRITE_EXTERNAL_STORAGE permission. Only the newest [KEEP] snapshots are kept.
 */
object AutoArchive {
    const val FOLDER = "MRT"
    private const val PREFIX = "AK1-"
    private const val KEEP = 12

    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ak1-autosave").apply { isDaemon = true }
    }

    /** Queues a snapshot; safe to call from `onStop`, the work happens on a background thread. */
    fun save(context: Context, json: String) {
        val app = context.applicationContext
        worker.execute { runCatching { write(app, json) } }
    }

    fun folderLabel(): String = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER"

    private fun write(context: Context, json: String) {
        val name = fileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeScoped(context, name, json)
            pruneScoped(context)
        } else {
            writeLegacy(name, json)
            pruneLegacy()
        }
    }

    private fun fileName(): String {
        val now = ZonedDateTime.now()
        val jalali = JalaliCalendar.fromGregorian(now.toLocalDate())
        val stamp = "%04d%02d%02d-%02d%02d".format(jalali.year, jalali.month, jalali.day, now.hour, now.minute)
        return "$PREFIX$stamp.json"
    }

    private fun writeScoped(context: Context, name: String, json: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        try {
            resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private fun pruneScoped(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%/$FOLDER/%", "$PREFIX%")
        val order = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        val stale = mutableListOf<Uri>()
        resolver.query(collection, projection, selection, args, order)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            var index = 0
            while (cursor.moveToNext()) {
                if (index++ < KEEP) continue
                stale += ContentUris.withAppendedId(collection, cursor.getLong(idColumn))
            }
        }
        stale.forEach { runCatching { resolver.delete(it, null, null) } }
    }

    private fun writeLegacy(name: String, json: String) {
        val folder = File(Environment.getExternalStorageDirectory(), FOLDER)
        if (!folder.exists() && !folder.mkdirs()) return
        File(folder, name).writeText(json)
    }

    private fun pruneLegacy() {
        val kept = File(Environment.getExternalStorageDirectory(), FOLDER)
            .listFiles { file -> file.name.startsWith(PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        kept.drop(KEEP).forEach { runCatching { it.delete() } }
    }
}
