package com.kichikan.ak1.birthday

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kichikan.ak1.data.AppRepository
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

object BirthdayReminderScheduler {
    private const val REQUEST_CODE = 4107
    private const val CHANNEL_ID = "ak1_birthday"

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, BirthdayReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = java.util.Calendar.getInstance()
        next.add(java.util.Calendar.DAY_OF_YEAR, 1)
        next.set(java.util.Calendar.HOUR_OF_DAY, 9)
        next.set(java.util.Calendar.MINUTE, 0)
        next.set(java.util.Calendar.SECOND, 0)
        next.set(java.util.Calendar.MILLISECOND, 0)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآوری تولد", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun notify(context: Context, title: String, text: String, id: Int) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }
}

class BirthdayBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BirthdayReminderScheduler.ensureChannel(context.applicationContext)
        BirthdayReminderScheduler.schedule(context.applicationContext)
    }
}

class BirthdayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        BirthdayReminderScheduler.ensureChannel(appContext)
        val repo = AppRepository(appContext)
        if (repo.ring == null) {
            BirthdayReminderScheduler.schedule(appContext)
            return
        }

        val today = LocalDate.now(ZoneId.systemDefault())
        repo.processBirthdayRewards()

        repo.members.forEach { member ->
            val date = member.birthDate?.let {
                runCatching { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
            } ?: return@forEach
            when (daysUntilBirthday(date, today)) {
                0L -> BirthdayReminderScheduler.notify(
                    appContext,
                    "امروز تولد " + member.name + " است 🎂",
                    "۳۰ الماس پاداش تولد امروز به‌صورت خودکار ثبت شد.",
                    member.id.hashCode()
                )
                3L -> BirthdayReminderScheduler.notify(
                    appContext,
                    "تولد " + member.name + " نزدیک است",
                    "۳ روز تا تولد این عضو باقی مانده است.",
                    member.id.hashCode() xor 3
                )
            }
        }
        BirthdayReminderScheduler.schedule(appContext)
    }

    private fun daysUntilBirthday(birthDate: LocalDate, today: LocalDate): Long {
        var year = today.year
        var candidate = safeBirthday(birthDate, year)
        if (candidate.isBefore(today)) {
            year++
            candidate = safeBirthday(birthDate, year)
        }
        return ChronoUnit.DAYS.between(today, candidate)
    }

    private fun safeBirthday(birthDate: LocalDate, year: Int): LocalDate =
        if (birthDate.monthValue == 2 && birthDate.dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, birthDate.monthValue, birthDate.dayOfMonth)
        }
}
