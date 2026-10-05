package com.kichikan.ak1.domain.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.kichikan.ak1.domain.model.CatalogEntry

object CatalogService {
    fun renderJpeg(entry: CatalogEntry, image: Bitmap?, width: Int = 1200, height: Int = 1500): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
        image?.let {
            val side = 760
            val left = (width - side) / 2
            canvas.drawBitmap(it, null, Rect(left, 70, left + side, 70 + side), paint)
        }
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 58f
        canvas.drawText(entry.title, 60f, 930f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 34f
        drawWrapped(canvas, entry.description, 60f, 1010f, width - 120, paint)
        drawWrapped(canvas, "نحوه دریافت: " + entry.acquisitionText, 60f, 1200f, width - 120, paint)
        return out
    }

    private fun drawWrapped(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Int, paint: Paint) {
        var cursorY = y
        var line = ""
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line, x, cursorY, paint)
                cursorY += paint.textSize * 1.35f
                line = word
            } else line = candidate
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, cursorY, paint)
    }
}
