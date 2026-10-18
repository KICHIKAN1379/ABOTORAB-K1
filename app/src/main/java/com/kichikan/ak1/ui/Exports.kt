package com.kichikan.ak1.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.model.Member

/** One line of a ranking export; [avatarPath]/[framePath] are drawn together (avatar inside frame). */
data class RankRow(
    val title: String,
    val subtitle: String,
    val avatarPath: String? = null,
    val framePath: String? = null,
    val letter: String = ""
)

object Exports {
    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.CENTER) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = size; textAlign = align
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun background(canvas: Canvas, w: Int, h: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0xFF021E43.toInt(), 0xFF012C71.toInt(), 0xFF011D59.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = RadialGradient(w * 0.92f, h * 0.05f, w * 0.7f, 0x331EFCE5, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = RadialGradient(w * 0.05f, h * 0.95f, w * 0.8f, 0x33B197FF, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    }

    private fun drawAvatar(canvas: Canvas, avatarPath: String?, framePath: String?, letter: String, left: Float, top: Float, size: Int) {
        val bitmap = AvatarRender.compose(avatarPath, framePath, size, letter)
        canvas.drawBitmap(bitmap, left, top, null)
    }

    fun memberCard(repo: AppRepository, member: Member): Bitmap {
        val w = 1000
        val h = 700
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, w, h)
        val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
        canvas.drawRoundRect(RectF(36f, 36f, w - 36f, h - 36f), 48f, 48f, glass)
        canvas.drawRoundRect(RectF(36f, 36f, w - 36f, h - 36f), 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x66FFFFFF
        })
        val avatarPath = repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath
        val framePath = repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath
        val size = 360
        drawAvatar(canvas, avatarPath, framePath, member.name, (w - size) / 2f, 52f, size)
        canvas.drawText(member.name, w / 2f, 465f, paint(Color.WHITE, 54f, bold = true))
        canvas.drawText("سطح ${member.economy.level}   •   XP ${member.economy.xp}", w / 2f, 525f, paint(0xFF1EFCE5.toInt(), 34f, bold = true))
        canvas.drawText("امتیاز ${member.economy.spendablePoints}   •   الماس ${member.economy.diamonds}", w / 2f, 575f, paint(0xFFC3D3F2.toInt(), 30f))
        canvas.drawText(repo.ring?.ringName ?: "ابوتراب K1", w / 2f, 640f, paint(0xFFFEC856.toInt(), 28f))
        return bitmap
    }

    fun rankingCard(title: String, rows: List<RankRow>): Bitmap {
        val width = 1200
        val rowHeight = 112
        val header = 200
        val height = (header + rows.size.coerceAtLeast(1) * rowHeight + 60).coerceAtMost(8000)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, width, height)
        canvas.drawText(title, width / 2f, 90f, paint(0xFFFEC856.toInt(), 52f, bold = true))
        canvas.drawText("مجموعه فرهنگی تربیتی ابوتراب علیه السلام", width / 2f, 145f, paint(0xFFC3D3F2.toInt(), 28f))
        if (rows.isEmpty()) canvas.drawText("هنوز داده‌ای برای رتبه‌بندی وجود ندارد.", width / 2f, header + 60f, paint(Color.WHITE, 30f))
        val rankColors = listOf(0xFFFEC856.toInt(), 0xFFD6DEEB.toInt(), 0xFFE0955F.toInt())
        rows.forEachIndexed { i, row ->
            val top = header + i * rowHeight
            canvas.drawRoundRect(RectF(40f, top + 6f, width - 40f, top + rowHeight - 6f), 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x17FFFFFF })
            val rankColor = rankColors.getOrElse(i) { 0xFF1EFCE5.toInt() }
            canvas.drawText("${i + 1}", width - 92f, top + rowHeight / 2f + 14f, paint(rankColor, 40f, bold = true))
            val avatarSize = 92
            drawAvatar(canvas, row.avatarPath, row.framePath, row.letter, width - 150f - avatarSize, top + (rowHeight - avatarSize) / 2f, avatarSize)
            val textRight = width - 160f - avatarSize
            canvas.drawText(row.title, textRight, top + 48f, paint(Color.WHITE, 34f, bold = true, align = Paint.Align.RIGHT))
            canvas.drawText(row.subtitle, textRight, top + 90f, paint(0xFFC3D3F2.toInt(), 26f, align = Paint.Align.RIGHT))
        }
        return bitmap
    }
}
