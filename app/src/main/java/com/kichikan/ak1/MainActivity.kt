package com.kichikan.ak1

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kichikan.ak1.ui.theme.AK1Theme
import com.kichikan.ak1.ui.theme.ThemeState
import com.kichikan.ak1.audio.BackgroundMusic
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.data.AutoArchive
import com.kichikan.ak1.birthday.BirthdayReminderScheduler
import com.kichikan.ak1.notification.DailyDateNotification
import com.kichikan.ak1.ui.screens.AK1App

class MainActivity : ComponentActivity() {
    /**
     * One repository for the whole process. The Activity owns it (instead of a composable) so the
     * automatic `MRT` archive can read the latest state from [onStop].
     */
    private val repo: AppRepository by lazy { AppRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BirthdayReminderScheduler.schedule(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4107)
        }
        // Only needed on Android 8-9, where the archive folder lives at the root of shared storage.
        if (android.os.Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 4108)
        }
        DailyDateNotification.start(this)
        ThemeState.load(this)
        setContent { AK1Theme { AK1App(repo) } }
    }

    override fun onStart() {
        super.onStart()
        BackgroundMusic.start(this)
    }

    override fun onStop() {
        super.onStop()
        BackgroundMusic.pause()
        // Leaving the app (not a rotation) writes a dated snapshot into the MRT folder.
        if (!isChangingConfigurations) {
            runCatching { repo.exportBackup() }.getOrNull()?.let { AutoArchive.save(this, it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) BackgroundMusic.stop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4107 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            DailyDateNotification.start(this)
        }
    }
}
