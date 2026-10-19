package com.kichikan.ak1.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/** Jalali seasons, each with the frame colour used by the daily date badge. */
enum class JalaliSeason(val label: String, val frame: Int, val edge: Int) {
    SPRING("بهار", 0xFF1F7A54.toInt(), 0xFF9BE7BE.toInt()),
    SUMMER("تابستان", 0xFFC48412.toInt(), 0xFFFFD98A.toInt()),
    AUTUMN("پاییز", 0xFFB4551F.toInt(), 0xFFFFB27A.toInt()),
    WINTER("زمستان", 0xFF27588F.toInt(), 0xFF9DC7F5.toInt());

    companion object {
        /** Farvardin..Khordad = spring, Tir..Shahrivar = summer, Mehr..Azar = autumn, rest = winter. */
        fun forMonth(month: Int): JalaliSeason = when (month) {
            in 1..3 -> SPRING
            in 4..6 -> SUMMER
            in 7..9 -> AUTUMN
            else -> WINTER
        }
    }
}

/**
 * Draws the notification badge: the Jalali day number inside a coloured frame whose colour
 * depends on the season.
 *
 * Two variants exist because Android renders the *small* icon as an alpha silhouette in the
 * status bar: a filled square would turn into a solid block and hide the number. So the status
 * bar gets an outlined badge (white ring + white number) and the expanded notification gets the
 * filled, season-coloured badge as its large icon.
 */
object NotificationBadge {

    fun dayIcon(day: Int, season: JalaliSeason, size: Int, filled: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = size * 0.055f
        val rect = RectF(inset, inset, size - inset, size - inset)
        val radius = size * 0.24f

        val frame = Paint(Paint.ANTI_ALIAS_FLAG)
        if (filled) {
            frame.shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                season.frame, darken(season.frame, 0.7f), Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, radius, radius, frame)
            val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.035f
                color = season.edge
            }
            canvas.drawRoundRect(rect, radius, radius, edge)
        } else {
            frame.style = Paint.Style.STROKE
            frame.strokeWidth = size * 0.085f
            frame.color = Color.WHITE
            canvas.drawRoundRect(rect, radius, radius, frame)
        }

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = size * if (day >= 10) 0.44f else 0.52f
        }
        val metrics = text.fontMetrics
        val baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(day.toString(), size / 2f, baseline, text)
        return bitmap
    }

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )
}
