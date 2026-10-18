package com.kichikan.ak1.domain.service

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural artwork for the built-in avatars and frames.
 *
 * Avatars are square illustrations; frames are transparent rings whose hole is detected
 * automatically by [com.kichikan.ak1.ui.AvatarRender], so any avatar is clipped *inside* any frame.
 */
object AssetArt {
    const val ART_VERSION = 2
    val avatarNames = listOf("رهرو", "پیش‌قدم", "پژوهشگر", "قله‌نورد", "ستاره", "سفیر")
    val frameNames = listOf("فیروزه‌ای", "طلایی", "ارغوانی", "مرجانی", "زمردی", "ستاره‌ای", "الماسی", "قله")

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun stroke(color: Int, width: Float, cap: Paint.Cap = Paint.Cap.ROUND) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width; strokeCap = cap }
    private fun shaded(shader: Shader) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
    private fun lin(x0: Float, y0: Float, x1: Float, y1: Float, vararg colors: Int) =
        LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP)
    private fun rad(cx: Float, cy: Float, r: Float, vararg colors: Int) =
        RadialGradient(cx, cy, r, colors, null, Shader.TileMode.CLAMP)
    private fun a(alpha: Int, color: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun star(cx: Float, cy: Float, outer: Float, inner: Float, points: Int = 5, rotation: Float = -90f): Path = Path().apply {
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val ang = (rotation + i * 180f / points) * PI / 180.0
            val x = cx + (r * cos(ang)).toFloat()
            val y = cy + (r * sin(ang)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun sparkle(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val path = Path().apply {
            moveTo(cx, cy - r); quadTo(cx, cy, cx + r, cy); quadTo(cx, cy, cx, cy + r)
            quadTo(cx, cy, cx - r, cy); quadTo(cx, cy, cx, cy - r); close()
        }
        canvas.drawPath(path, fill(color))
    }

    // ───────────────────────── avatars ─────────────────────────

    private class AvatarLook(
        val bgLight: Int, val bgDark: Int, val clothLight: Int, val clothDark: Int,
        val hair: Int, val skinLight: Int, val skinDark: Int
    )

    private val looks = listOf(
        AvatarLook(0xFF6FE3DC.toInt(), 0xFF087F8C.toInt(), 0xFF4C5E86.toInt(), 0xFF1E2A47.toInt(), 0xFF2A1F1C.toInt(), 0xFFF3CFA9.toInt(), 0xFFD9A078.toInt()),
        AvatarLook(0xFFFFB090.toInt(), 0xFFCA5B3F.toInt(), 0xFF35C2B3.toInt(), 0xFF0B6F78.toInt(), 0xFF15110F.toInt(), 0xFFE6B48C.toInt(), 0xFFC48A62.toInt()),
        AvatarLook(0xFFC7B7FF.toInt(), 0xFF5A48A8.toInt(), 0xFFFFFFFF.toInt(), 0xFFCDD3E6.toInt(), 0xFF4A3426.toInt(), 0xFFF5D7B8.toInt(), 0xFFDCA880.toInt()),
        AvatarLook(0xFF8DEBB5.toInt(), 0xFF2B8458.toInt(), 0xFFFFC15A.toInt(), 0xFFE0782A.toInt(), 0xFF241A16.toInt(), 0xFFE9BE96.toInt(), 0xFFCB956C.toInt()),
        AvatarLook(0xFF5B7CFF.toInt(), 0xFF0A1B5C.toInt(), 0xFF3DF6E6.toInt(), 0xFF0E8FA3.toInt(), 0xFF1B1412.toInt(), 0xFFD9A47C.toInt(), 0xFFB5805A.toInt()),
        AvatarLook(0xFFFFE08A.toInt(), 0xFFC98A1E.toInt(), 0xFF2E8B63.toInt(), 0xFF145A3E.toInt(), 0xFF2B2018.toInt(), 0xFFEFC59E.toInt(), 0xFFD19A70.toInt())
    )

    fun avatarBitmap(variant: Int, size: Int = 512): Bitmap {
        val look = looks[variant.mod(looks.size)]
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(size / 512f, size / 512f)
        drawAvatar(c, variant.mod(looks.size), look)
        return bmp
    }

    private fun drawAvatar(c: Canvas, variant: Int, look: AvatarLook) {
        // background
        c.drawRoundRect(RectF(0f, 0f, 512f, 512f), 120f, 120f, shaded(rad(180f, 140f, 560f, look.bgLight, look.bgDark)))
        c.drawCircle(86f, 92f, 52f, fill(a(55, Color.WHITE)))
        c.drawCircle(440f, 140f, 28f, fill(a(45, Color.WHITE)))
        c.drawCircle(420f, 420f, 70f, fill(a(28, Color.WHITE)))
        if (variant == 4) {
            for ((x, y, r) in listOf(Triple(70f, 70f, 12f), Triple(430f, 90f, 16f), Triple(110f, 190f, 8f), Triple(455f, 230f, 9f), Triple(60f, 300f, 7f))) sparkle(c, x, y, r, a(230, Color.WHITE))
        }
        // shoulders & neck
        val shoulders = Path().apply {
            moveTo(40f, 520f); cubicTo(48f, 400f, 150f, 352f, 256f, 352f)
            cubicTo(362f, 352f, 464f, 400f, 472f, 520f); close()
        }
        c.drawPath(shoulders, shaded(lin(0f, 352f, 0f, 520f, look.clothLight, look.clothDark)))
        c.drawRoundRect(RectF(220f, 292f, 292f, 392f), 34f, 34f, shaded(lin(0f, 292f, 0f, 392f, look.skinDark, look.skinDark)))
        val collar = Path().apply { moveTo(206f, 356f); lineTo(256f, 430f); lineTo(306f, 356f); close() }
        c.drawPath(collar, fill(look.skinDark))
        c.drawPath(Path().apply { moveTo(196f, 354f); lineTo(250f, 436f); lineTo(232f, 372f); close() }, fill(a(90, Color.WHITE)))
        c.drawPath(Path().apply { moveTo(316f, 354f); lineTo(262f, 436f); lineTo(280f, 372f); close() }, fill(a(60, Color.BLACK)))
        // back hair for curly look
        if (variant == 4) {
            for (k in 0..10) {
                val ang = (195 + k * 15) * PI / 180.0
                c.drawCircle(256f + (108 * cos(ang)).toFloat(), 232f + (112 * sin(ang)).toFloat(), 34f, fill(look.hair))
            }
        }
        // ears + head
        c.drawCircle(160f, 252f, 20f, fill(look.skinDark))
        c.drawCircle(352f, 252f, 20f, fill(look.skinDark))
        c.drawOval(RectF(164f, 124f, 348f, 346f), shaded(rad(228f, 214f, 190f, look.skinLight, look.skinDark)))
        // hair / hats
        val hair = fill(look.hair)
        when (variant) {
            0, 2 -> {
                val fringe = Path().apply {
                    moveTo(160f, 238f); cubicTo(150f, 100f, 362f, 96f, 352f, 238f)
                    cubicTo(330f, 178f, 190f, 176f, 160f, 238f); close()
                }
                c.drawPath(fringe, hair)
                c.drawPath(Path().apply { moveTo(190f, 150f); cubicTo(230f, 118f, 300f, 118f, 330f, 150f); cubicTo(290f, 136f, 232f, 138f, 190f, 150f); close() }, fill(a(70, Color.WHITE)))
            }
            1 -> {
                val sweep = Path().apply {
                    moveTo(160f, 244f); cubicTo(142f, 90f, 372f, 80f, 352f, 232f)
                    cubicTo(300f, 206f, 236f, 168f, 206f, 196f); cubicTo(196f, 216f, 180f, 238f, 160f, 244f); close()
                }
                c.drawPath(sweep, hair)
            }
            3 -> {
                c.drawArc(RectF(156f, 96f, 356f, 284f), 180f, 180f, true, shaded(lin(0f, 96f, 0f, 240f, 0xFFFF6A5C.toInt(), 0xFFC23A33.toInt())))
                c.drawRoundRect(RectF(150f, 204f, 362f, 246f), 20f, 20f, shaded(lin(0f, 204f, 0f, 246f, 0xFFE8E2D8.toInt(), 0xFFB8B0A2.toInt())))
                c.drawCircle(256f, 98f, 24f, fill(0xFFFFF1E0.toInt()))
            }
            4 -> {
                val fringe = Path().apply {
                    moveTo(166f, 226f); cubicTo(160f, 130f, 352f, 128f, 346f, 226f)
                    cubicTo(326f, 184f, 190f, 184f, 166f, 226f); close()
                }
                c.drawPath(fringe, hair)
                c.drawPath(Path().apply { addArc(RectF(160f, 128f, 352f, 280f), 196f, 148f) }, stroke(0xFFFFD25A.toInt(), 14f))
            }
            5 -> {
                c.drawArc(RectF(168f, 112f, 344f, 262f), 180f, 180f, true, shaded(lin(0f, 112f, 0f, 220f, 0xFFFFFFFF.toInt(), 0xFFD8DEEA.toInt())))
                c.drawRoundRect(RectF(164f, 200f, 348f, 232f), 14f, 14f, fill(0xFFC9A24A.toInt()))
                for (k in 0..4) c.drawCircle(196f + k * 30f, 216f, 5f, fill(0xFFFFF0B8.toInt()))
            }
        }
        // face
        val eyeY = 262f
        for (ex in listOf(214f, 298f)) {
            c.drawOval(RectF(ex - 19f, eyeY - 15f, ex + 19f, eyeY + 15f), fill(Color.WHITE))
            c.drawCircle(ex, eyeY, 10.5f, fill(0xFF3B2A22.toInt()))
            c.drawCircle(ex, eyeY, 5.5f, fill(0xFF0E0A09.toInt()))
            c.drawCircle(ex + 4f, eyeY - 4f, 3f, fill(Color.WHITE))
        }
        val brow = stroke(look.hair, 8f)
        c.drawArc(RectF(190f, 224f, 238f, 250f), 200f, 130f, false, brow)
        c.drawArc(RectF(274f, 224f, 322f, 250f), 210f, 130f, false, brow)
        c.drawArc(RectF(240f, 262f, 272f, 306f), 20f, 140f, false, stroke(a(120, look.skinDark), 6f))
        c.drawArc(RectF(216f, 286f, 296f, 336f), 20f, 140f, false, stroke(0xFFA24B40.toInt(), 7f))
        c.drawCircle(188f, 306f, 17f, fill(a(70, 0xFFFF7A7A.toInt())))
        c.drawCircle(324f, 306f, 17f, fill(a(70, 0xFFFF7A7A.toInt())))
        if (variant == 2) {
            val glass = stroke(0xFF2B2F45.toInt(), 7f)
            c.drawCircle(214f, eyeY, 32f, glass); c.drawCircle(298f, eyeY, 32f, glass)
            c.drawLine(246f, eyeY - 2f, 266f, eyeY - 2f, glass)
        }
        // soft gloss
        c.drawOval(RectF(20f, 20f, 300f, 150f), shaded(lin(0f, 20f, 0f, 150f, a(70, Color.WHITE), a(0, Color.WHITE))))
    }

    /** Level avatar: the matching character plus tier colour, crown and level badge. */
    fun levelAvatarBitmap(level: Int, size: Int = 512): Bitmap {
        val tier = (level / 5 - 1).coerceAtLeast(0)
        val bmp = avatarBitmap(tier, size)
        val c = Canvas(bmp)
        c.scale(size / 512f, size / 512f)
        val tierColors = listOf(0xFF3DF6E6.toInt(), 0xFF5B9CFF.toInt(), 0xFFB197FF.toInt(), 0xFFFFC857.toInt(), 0xFF58E39A.toInt())
        val accent = tierColors[tier % tierColors.size]
        c.drawRoundRect(RectF(10f, 10f, 502f, 502f), 112f, 112f, stroke(accent, if (level >= 20) 20f else 12f))
        if (level >= 25) {
            val crown = Path().apply {
                moveTo(176f, 112f); lineTo(160f, 40f); lineTo(214f, 80f); lineTo(256f, 22f)
                lineTo(298f, 80f); lineTo(352f, 40f); lineTo(336f, 112f); close()
            }
            c.drawPath(crown, shaded(lin(0f, 22f, 0f, 112f, 0xFFFFE9A0.toInt(), 0xFFD9A12E.toInt())))
        } else if (level >= 10) {
            c.drawPath(star(256f, 62f, 30f, 13f), shaded(lin(0f, 30f, 0f, 90f, 0xFFFFE9A0.toInt(), 0xFFD9A12E.toInt())))
        }
        c.drawCircle(420f, 420f, 66f, shaded(lin(0f, 354f, 0f, 486f, 0xFFFFE9A0.toInt(), 0xFFD9A12E.toInt())))
        c.drawCircle(420f, 420f, 66f, stroke(0xFF8B5E10.toInt(), 6f))
        val t = fill(0xFF3A2A06.toInt()).apply { textSize = 60f; textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        c.drawText(level.toString(), 420f, 442f, t)
        return bmp
    }

    // ───────────────────────── frames ─────────────────────────

    /** Frames keep a transparent circular hole of radius [HOLE] (of 256) for the avatar. */
    private const val HOLE = 190f
    private const val OUTER = 238f

    private fun annulus(c: Canvas, inner: Float, outer: Float, paint: Paint) {
        val p = Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = outer - inner }
        c.drawCircle(256f, 256f, (inner + outer) / 2f, p)
    }

    fun frameBitmap(style: Int, size: Int = 512): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(size / 512f, size / 512f)
        when (style.mod(8)) {
            0 -> { // neon
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = a(200, 0xFF1EFCE5.toInt()); maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL) }
                annulus(c, HOLE + 6, OUTER - 4, glow)
                val sweep = SweepGradient(256f, 256f, intArrayOf(0xFF1EFCE5.toInt(), 0xFF3D7BFF.toInt(), 0xFFB197FF.toInt(), 0xFF1EFCE5.toInt()), null)
                annulus(c, HOLE + 4, OUTER - 8, shaded(sweep))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(a(230, Color.WHITE), 4f))
                c.drawCircle(256f, 256f, OUTER - 6, stroke(a(150, Color.WHITE), 3f))
                for ((x, y, r) in listOf(Triple(256f, 30f, 20f), Triple(482f, 256f, 14f), Triple(256f, 482f, 20f), Triple(30f, 256f, 14f))) sparkle(c, x, y, r, Color.WHITE)
            }
            1 -> { // gold
                annulus(c, HOLE, OUTER - 10, shaded(lin(60f, 40f, 450f, 470f, 0xFFFFF0B0.toInt(), 0xFFE0A630.toInt(), 0xFF8B5E10.toInt())))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(0xFFFFF6CF.toInt(), 5f))
                c.drawCircle(256f, 256f, OUTER - 12, stroke(0xFF7A4F0A.toInt(), 5f))
                c.drawCircle(256f, 256f, (HOLE + OUTER) / 2f - 4f, stroke(a(120, 0xFFFFFFFF.toInt()), 3f))
                for (ang in listOf(0.0, 90.0, 180.0, 270.0)) {
                    val x = 256f + ((HOLE + OUTER) / 2f * cos(ang * PI / 180)).toFloat(); val y = 256f + ((HOLE + OUTER) / 2f * sin(ang * PI / 180)).toFloat()
                    c.drawCircle(x, y, 17f, shaded(rad(x - 5, y - 5, 24f, 0xFFFF8A94.toInt(), 0xFFB5182B.toInt())))
                    c.drawCircle(x, y, 17f, stroke(0xFFFFE9A0.toInt(), 4f))
                }
                val crown = Path().apply { moveTo(204f, 50f); lineTo(194f, 4f); lineTo(228f, 30f); lineTo(256f, 0f); lineTo(284f, 30f); lineTo(318f, 4f); lineTo(308f, 50f); close() }
                c.drawPath(crown, shaded(lin(0f, 0f, 0f, 50f, 0xFFFFF0B0.toInt(), 0xFFD9A12E.toInt())))
            }
            2 -> { // violet glass
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = a(170, 0xFFB197FF.toInt()); maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL) }
                annulus(c, HOLE + 8, OUTER - 6, glow)
                annulus(c, HOLE, OUTER - 8, shaded(lin(60f, 40f, 450f, 470f, a(235, 0xFFE9E0FF.toInt()), a(190, 0xFF8E72E8.toInt()), a(235, 0xFF4B3A9E.toInt()))))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(a(240, Color.WHITE), 4f))
                c.drawCircle(256f, 256f, OUTER - 10, stroke(a(190, Color.WHITE), 3f))
                c.drawArc(RectF(30f, 30f, 482f, 482f), 200f, 70f, false, stroke(a(220, Color.WHITE), 8f))
                for ((x, y, r) in listOf(Triple(110f, 96f, 16f), Triple(420f, 150f, 12f), Triple(400f, 430f, 18f), Triple(90f, 380f, 10f))) sparkle(c, x, y, r, Color.WHITE)
            }
            3 -> { // coral flame
                for (k in 0 until 24) {
                    val ang = (k * 15.0 - 90) * PI / 180
                    val long = if (k % 2 == 0) 250f else 236f
                    val base = (HOLE + OUTER) / 2f
                    val bx = 256f + (base * cos(ang)).toFloat(); val by = 256f + (base * sin(ang)).toFloat()
                    val tx = 256f + (long * cos(ang)).toFloat(); val ty = 256f + (long * sin(ang)).toFloat()
                    val px = (-sin(ang) * 16).toFloat(); val py = (cos(ang) * 16).toFloat()
                    val flame = Path().apply { moveTo(bx + px, by + py); lineTo(tx, ty); lineTo(bx - px, by - py); close() }
                    c.drawPath(flame, shaded(lin(bx, by, tx, ty, 0xFFFF8A3D.toInt(), 0xFFFFE066.toInt())))
                }
                annulus(c, HOLE, OUTER - 18, shaded(lin(60f, 40f, 450f, 470f, 0xFFFFC4AF.toInt(), 0xFFCA5B3F.toInt(), 0xFF8C2F1A.toInt())))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(0xFFFFE3D6.toInt(), 4f))
            }
            4 -> { // emerald vine
                annulus(c, HOLE, OUTER - 14, shaded(lin(60f, 40f, 450f, 470f, 0xFF5BD39A.toInt(), 0xFF1F7A52.toInt(), 0xFF0E4630.toInt())))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(0xFFC8FFE3.toInt(), 4f))
                val base = (HOLE + OUTER) / 2f
                for (k in 0 until 24) {
                    val ang = k * 15.0
                    val rr = ang * PI / 180
                    val x = 256f + (base * cos(rr)).toFloat(); val y = 256f + (base * sin(rr)).toFloat()
                    c.save(); c.rotate((ang + 90 + if (k % 2 == 0) 35 else -35).toFloat(), x, y)
                    c.drawOval(RectF(x - 11f, y - 30f, x + 11f, y + 6f), shaded(lin(x, y - 30f, x, y + 6f, if (k % 2 == 0) 0xFF9BF0B8.toInt() else 0xFF3FB36F.toInt(), 0xFF176B45.toInt())))
                    c.restore()
                }
                for (ang in listOf(45.0, 135.0, 225.0, 315.0)) {
                    val x = 256f + (base * cos(ang * PI / 180)).toFloat(); val y = 256f + (base * sin(ang * PI / 180)).toFloat()
                    c.drawCircle(x, y, 13f, fill(0xFFFF9EC4.toInt())); c.drawCircle(x, y, 5f, fill(0xFFFFE066.toInt()))
                }
            }
            5 -> { // starry night
                annulus(c, HOLE, OUTER - 6, shaded(lin(60f, 40f, 450f, 470f, 0xFF2B3C9A.toInt(), 0xFF141C5E.toInt(), 0xFF090D36.toInt())))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(0xFFFFD25A.toInt(), 4f))
                c.drawCircle(256f, 256f, OUTER - 8, stroke(0xFFFFD25A.toInt(), 4f))
                val base = (HOLE + OUTER) / 2f - 2f
                for (k in 0 until 12) {
                    val rr = (k * 30.0 - 90) * PI / 180
                    val x = 256f + (base * cos(rr)).toFloat(); val y = 256f + (base * sin(rr)).toFloat()
                    c.drawPath(star(x, y, if (k % 3 == 0) 17f else 12f, if (k % 3 == 0) 7f else 5f), shaded(lin(x, y - 17f, x, y + 17f, 0xFFFFF0B0.toInt(), 0xFFE0A630.toInt())))
                }
            }
            6 -> { // diamond
                annulus(c, HOLE, OUTER - 6, shaded(lin(60f, 40f, 450f, 470f, 0xFFFFFFFF.toInt(), 0xFF9BE8FF.toInt(), 0xFF4BB6E8.toInt())))
                for (k in 0 until 16) {
                    val a0 = (k * 22.5 - 90) * PI / 180; val a1 = ((k + 1) * 22.5 - 90) * PI / 180
                    val facet = Path().apply {
                        moveTo(256f + (HOLE * cos(a0)).toFloat(), 256f + (HOLE * sin(a0)).toFloat())
                        lineTo(256f + ((OUTER - 6) * cos(a0)).toFloat(), 256f + ((OUTER - 6) * sin(a0)).toFloat())
                        lineTo(256f + ((OUTER - 6) * cos(a1)).toFloat(), 256f + ((OUTER - 6) * sin(a1)).toFloat())
                        lineTo(256f + (HOLE * cos(a1)).toFloat(), 256f + (HOLE * sin(a1)).toFloat()); close()
                    }
                    c.drawPath(facet, fill(if (k % 2 == 0) a(90, Color.WHITE) else a(70, 0xFF2A7FB8.toInt())))
                    c.drawPath(facet, stroke(a(160, Color.WHITE), 2f))
                }
                c.drawCircle(256f, 256f, HOLE + 2, stroke(Color.WHITE, 4f))
                for ((x, y, r) in listOf(Triple(256f, 28f, 22f), Triple(484f, 256f, 15f), Triple(256f, 484f, 22f), Triple(28f, 256f, 15f))) sparkle(c, x, y, r, Color.WHITE)
            }
            else -> { // summit
                annulus(c, HOLE, OUTER - 12, shaded(lin(60f, 40f, 450f, 470f, 0xFF39C0B6.toInt(), 0xFF0F6C8C.toInt(), 0xFF0B2E5C.toInt())))
                c.drawCircle(256f, 256f, HOLE + 2, stroke(0xFFBFFFF6.toInt(), 4f))
                val peak = Path().apply { moveTo(196f, 64f); lineTo(256f, 6f); lineTo(316f, 64f); close() }
                c.drawPath(peak, shaded(lin(0f, 6f, 0f, 64f, 0xFF5FA8E0.toInt(), 0xFF0F4C86.toInt())))
                c.drawPath(Path().apply { moveTo(238f, 24f); lineTo(256f, 6f); lineTo(274f, 24f); lineTo(264f, 30f); lineTo(256f, 22f); lineTo(248f, 30f); close() }, fill(Color.WHITE))
                c.drawLine(256f, 6f, 256f, -2f, stroke(0xFFE0684F.toInt(), 3f))
            }
        }
        return bmp
    }
}
