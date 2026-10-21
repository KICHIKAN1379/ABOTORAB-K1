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
import com.kichikan.ak1.domain.model.Currency
import com.kichikan.ak1.domain.model.Member
import com.kichikan.ak1.domain.model.ShopItem
import com.kichikan.ak1.domain.model.ShopItemType

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
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0xFF02101F.toInt(), 0xFF062A63.toInt(), 0xFF01132E.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = RadialGradient(w * 0.9f, h * 0.04f, w * 0.85f, 0x551EFCE5, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = RadialGradient(w * 0.05f, h * 0.95f, w * 0.9f, 0x44B197FF, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        // Fine dot grid for a printed-poster feel.
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18FFFFFF }
        var y = 40f
        while (y < h) {
            var x = 40f
            while (x < w) {
                canvas.drawCircle(x, y, 2.4f, dot)
                x += 56f
            }
            y += 56f
        }
    }

    private fun drawAvatar(canvas: Canvas, avatarPath: String?, framePath: String?, letter: String, left: Float, top: Float, size: Int) {
        val bitmap = AvatarRender.compose(avatarPath, framePath, size, letter)
        canvas.drawBitmap(bitmap, left, top, null)
    }

    private fun wrap(canvas: Canvas, text: String, paint: Paint, centerX: Float, startY: Float, maxWidth: Float, lineHeight: Float) {
        val words = text.trim().split(" ")
        var line = ""
        var y = startY
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, centerX, y, paint)
                line = word
                y += lineHeight
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, centerX, y, paint)
    }

    fun memberCard(repo: AppRepository, member: Member): Bitmap {
        val w = 1200
        val h = 840
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, w, h)
        canvas.drawRoundRect(RectF(44f, 44f, w - 44f, h - 44f), 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF })
        canvas.drawRoundRect(RectF(44f, 44f, w - 44f, h - 44f), 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x66FFFFFF
        })
        val avatarPath = repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath
        val framePath = repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath
        val size = 400
        drawAvatar(canvas, avatarPath, framePath, member.name, (w - size) / 2f, 80f, size)
        canvas.drawText(member.name, w / 2f, 570f, paint(Color.WHITE, 64f, bold = true))
        canvas.drawText("سطح ${member.economy.level}   •   XP ${member.economy.xp}", w / 2f, 642f, paint(0xFF1EFCE5.toInt(), 40f, bold = true))
        canvas.drawText("امتیاز ${member.economy.spendablePoints}   •   الماس ${member.economy.diamonds}", w / 2f, 700f, paint(0xFFC3D3F2.toInt(), 34f))
        canvas.drawText(repo.ring?.ringName ?: "آکادمی K1", w / 2f, 780f, paint(0xFFFEC856.toInt(), 32f))
        return bitmap
    }

    fun rankingCard(title: String, rows: List<RankRow>): Bitmap {
        val width = 1600
        val rowHeight = 156
        val header = 300
        val height = (header + rows.size.coerceAtLeast(1) * rowHeight + 100).coerceAtMost(8000)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, width, height)
        canvas.drawText(title, width / 2f, 122f, paint(0xFFFEC856.toInt(), 74f, bold = true))
        canvas.drawText("مرتب‌سازی بر اساس تلاش و رشد اعضای حلقه", width / 2f, 190f, paint(0xFFC3D3F2.toInt(), 34f))
        canvas.drawLine(width * 0.22f, 226f, width * 0.78f, 226f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(width * 0.22f, 0f, width * 0.78f, 0f, intArrayOf(Color.TRANSPARENT, 0xFF1EFCE5.toInt(), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            strokeWidth = 3f
        })
        if (rows.isEmpty()) {
            canvas.drawText("هنوز اعضایی برای نمایش در این فهرست نیستند.", width / 2f, header + 80f, paint(Color.WHITE, 38f))
        }
        val rankColors = listOf(0xFFFEC856.toInt(), 0xFFD6DEEB.toInt(), 0xFFE0955F.toInt())
        rows.forEachIndexed { i, row ->
            val top = header + i * rowHeight
            val card = RectF(60f, top + 8f, width - 60f, top + rowHeight - 8f)
            canvas.drawRoundRect(card, 38f, 38f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1FFFFFFF })
            canvas.drawRoundRect(card, 38f, 38f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2f; color = 0x33FFFFFF
            })
            val rankColor = rankColors.getOrElse(i) { 0xFF1EFCE5.toInt() }
            val badgeX = 128f
            val badgeY = top + rowHeight / 2f
            canvas.drawCircle(badgeX, badgeY, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (rankColor and 0x00FFFFFF) or 0x33000000 })
            canvas.drawCircle(badgeX, badgeY, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 3f; color = rankColor
            })
            canvas.drawText("${i + 1}", badgeX, badgeY + 18f, paint(rankColor, 44f, bold = true))
            val avatarSize = 120
            drawAvatar(canvas, row.avatarPath, row.framePath, row.letter, 200f, top + (rowHeight - avatarSize) / 2f, avatarSize)
            val textLeft = 352f
            canvas.drawText(row.title, textLeft, top + 70f, paint(Color.WHITE, 44f, bold = true, align = Paint.Align.LEFT))
            canvas.drawText(row.subtitle, textLeft, top + 118f, paint(0xFFC3D3F2.toInt(), 32f, align = Paint.Align.LEFT))
        }
        canvas.drawText("AK1 • ABOTORAB-K1", width / 2f, height - 40f, paint(0x881EFCE5.toInt(), 26f))
        return bitmap
    }

    /** Poster-style card for a shop item: picture, how to get it, price and description. */
    fun shopItemCard(repo: AppRepository, item: ShopItem): Bitmap {
        val w = 1200
        val h = 1560
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background(canvas, w, h)
        canvas.drawRoundRect(RectF(54f, 54f, w - 54f, h - 54f), 60f, 60f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF })
        canvas.drawRoundRect(RectF(54f, 54f, w - 54f, h - 54f), 60f, 60f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x55FFFFFF
        })
        val typeLabel = when (item.type) {
            ShopItemType.AVATAR -> "آواتار"
            ShopItemType.FRAME -> "قاب تصویر"
            ShopItemType.REWARD -> "جایزه"
        }
        val typeColor = when (item.type) {
            ShopItemType.AVATAR -> 0xFF1EFCE5.toInt()
            ShopItemType.FRAME -> 0xFFB197FF.toInt()
            ShopItemType.REWARD -> 0xFFFEC856.toInt()
        }
        val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (typeColor and 0x00FFFFFF) or 0x22000000 }
        canvas.drawRoundRect(RectF(w / 2f - 130f, 96f, w / 2f + 130f, 164f), 34f, 34f, tag)
        canvas.drawText(typeLabel, w / 2f, 145f, paint(typeColor, 34f, bold = true))

        val imageSize = 700
        val left = (w - imageSize) / 2f
        val top = 200f
        canvas.drawRoundRect(RectF(left - 24f, top - 24f, left + imageSize + 24f, top + imageSize + 24f), 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x10FFFFFF })
        if (item.imagePath != null) {
            when (item.type) {
                ShopItemType.AVATAR -> drawAvatar(canvas, item.imagePath, null, item.name, left, top, imageSize)
                ShopItemType.FRAME -> drawAvatar(canvas, null, item.imagePath, item.name, left, top, imageSize)
                ShopItemType.REWARD -> {
                    val art = AvatarRender.decodeSampled(item.imagePath, imageSize)
                    if (art != null) {
                        val side = minOf(art.width, art.height)
                        val src = android.graphics.Rect((art.width - side) / 2, (art.height - side) / 2, (art.width + side) / 2, (art.height + side) / 2)
                        canvas.drawBitmap(art, src, RectF(left, top, left + imageSize, top + imageSize), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    }
                }
            }
        } else {
            canvas.drawCircle(left + imageSize / 2f, top + imageSize / 2f, imageSize / 2.4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = typeColor and 0x33FFFFFF })
            canvas.drawText(item.name.take(1), left + imageSize / 2f, top + imageSize / 2f + 80f, paint(Color.WHITE, 220f, bold = true))
        }

        canvas.drawText(item.name, w / 2f, 1030f, paint(Color.WHITE, 66f, bold = true))
        val description = item.description.ifBlank { "بدون توضیح" }
        wrap(canvas, description, paint(0xFFC3D3F2.toInt(), 32f), w / 2f, 1096f, w - 240f, 46f)

        val acquisition = when {
            item.currency == Currency.NONE || item.price == 0 -> "رایگان / هدیه"
            item.currency == Currency.POINTS -> "${item.price} امتیاز"
            else -> "${item.price} الماس"
        }
        canvas.drawText("روش دریافت: $acquisition", w / 2f, 1260f, paint(0xFF1EFCE5.toInt(), 36f, bold = true))
        if (item.methods.isNotEmpty()) {
            canvas.drawText(item.methods.joinToString(" • ") { it.name }, w / 2f, 1316f, paint(0xFFC3D3F2.toInt(), 26f))
        }
        canvas.drawText("حلقه: ${repo.ring?.ringName ?: "AK1"}", w / 2f, 1410f, paint(0xFFFEC856.toInt(), 32f))
        canvas.drawText("AK1 • ABOTORAB-K1", w / 2f, 1490f, paint(0x881EFCE5.toInt(), 24f))
        return bitmap
    }
}
