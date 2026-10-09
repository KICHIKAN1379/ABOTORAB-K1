package com.kichikan.ak1.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kichikan.ak1.MainActivity
import com.kichikan.ak1.R
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalTime

object DailyDateNotification {
    private const val CHANNEL_ID = "ak1_daily_date"
    private const val NOTIFICATION_ID = 6101
    private const val REQUEST_CODE = 6101
    private const val ACTION_UPDATE = "com.kichikan.ak1.UPDATE_DAILY_DATE"

    fun start(context: Context) {
        createChannel(context)
        show(context)
        scheduleNext(context)
    }

    fun scheduleNext(context: Context) {
        val intent = Intent(context, DailyDateNotificationReceiver::class.java).setAction(ACTION_UPDATE)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val zone = ZoneId.systemDefault()
        val nextMidnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight, pending)
    }

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val now = java.time.ZonedDateTime.now(ZoneId.systemDefault())
        val date = JalaliCalendar.fromGregorian(now.toLocalDate())
        val openApp = PendingIntent.getActivity(
            context, 6102, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = IconCompat.createWithBitmap(createDayIcon(date.day))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle("امروز • ${date.day} ${JalaliCalendar.MONTH_NAMES[date.month - 1]} ${date.year}")
            .setContentText("روز ${date.day} • ساعت ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}")
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "تاریخ روز", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "نمایش تاریخ شمسی روز در اعلان پایدار برنامه"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun createDayIcon(day: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 56f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val metrics = paint.fontMetrics
        canvas.drawText(day.toString(), 36f, 36f - (metrics.ascent + metrics.descent) / 2f, paint)
        return bitmap
    }

    internal fun updateAndReschedule(context: Context) {
        show(context)
        scheduleNext(context)
    }

    internal fun updateAction(): String = ACTION_UPDATE
}

class DailyDateNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DailyDateNotification.updateAndReschedule(context)
    }
}

class DailyDateNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyDateNotification.start(context)
        }
    }
}
