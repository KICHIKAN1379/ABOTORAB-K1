package com.kichikan.ak1.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ───────────────────────── Style 1: minimal vector (menus) ─────────────────────────

enum class MinimalKind { HOME, MEMBERS, SERVICES, SETTINGS }

/** Flat, geometric, two-tone icon (24-unit grid): body colour plus one accent detail. */
@Composable
fun MinimalIcon(kind: MinimalKind, selected: Boolean, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val body = if (selected) Neon.Cyan else Color(0xFFC3D3F2)
    val accent = if (selected) Neon.Gold else Color(0xFF8FA8D6)
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            when (kind) {
                MinimalKind.HOME -> {
                    val roof = Path().apply {
                        moveTo(12f, 2.8f); lineTo(22.2f, 11.6f); lineTo(19.6f, 11.6f)
                        lineTo(19.6f, 20.8f); lineTo(4.4f, 20.8f); lineTo(4.4f, 11.6f)
                        lineTo(1.8f, 11.6f); close()
                    }
                    drawPath(roof, body)
                    drawRoundRect(accent, Offset(9.6f, 13.8f), Size(4.8f, 7f), CornerRadius(1.4f))
                }
                MinimalKind.MEMBERS -> {
                    drawCircle(body, 3.4f, Offset(9f, 7.6f))
                    drawArc(body, 180f, 180f, true, Offset(2.6f, 13f), Size(12.8f, 15f))
                    drawCircle(accent, 2.7f, Offset(17.4f, 9f))
                    drawArc(accent, 180f, 180f, true, Offset(13.6f, 14.4f), Size(8.8f, 11f))
                }
                MinimalKind.SERVICES -> {
                    val r = CornerRadius(2.4f)
                    drawRoundRect(body, Offset(3f, 3f), Size(8f, 8f), r)
                    drawRoundRect(accent, Offset(13f, 3f), Size(8f, 8f), r)
                    drawRoundRect(body, Offset(3f, 13f), Size(8f, 8f), r)
                    drawRoundRect(body, Offset(13f, 13f), Size(8f, 8f), r)
                }
                MinimalKind.SETTINGS -> {
                    val r = CornerRadius(1f)
                    drawRoundRect(body, Offset(2.5f, 5f), Size(19f, 2f), r)
                    drawRoundRect(body, Offset(2.5f, 11f), Size(19f, 2f), r)
                    drawRoundRect(body, Offset(2.5f, 17f), Size(19f, 2f), r)
                    drawCircle(accent, 3f, Offset(8f, 6f))
                    drawCircle(accent, 3f, Offset(16f, 12f))
                    drawCircle(accent, 3f, Offset(10f, 18f))
                }
            }
        }
    }
}

// ───────────────────────── Style 3: clay 3D (services) ─────────────────────────

enum class ClayKind { WORKSHOP, SESSIONS, RANKING, STORE }

private fun clay(light: Color, base: Color, dark: Color, center: Offset, radius: Float) =
    Brush.radialGradient(listOf(light, base, dark), center = center, radius = radius)

private fun DrawScope.contactShadow(cx: Float, cy: Float, w: Float, h: Float) {
    for (i in 0..3) {
        val k = 1f + i * 0.16f
        drawOval(
            Color(0xFF3A2A3A).copy(alpha = 0.10f),
            Offset(cx - w * k / 2f, cy - h * k / 2f),
            Size(w * k, h * k)
        )
    }
}

private fun DrawScope.gloss(x: Float, y: Float, w: Float, h: Float, a: Float = 0.5f) =
    drawOval(Color.White.copy(alpha = a), Offset(x, y), Size(w, h))

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Soft, matte, rounded 3D icon on a warm clay tile (100-unit grid). */
@Composable
fun ClayIcon(kind: ClayKind, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(Brush.linearGradient(listOf(Color(0xFFEFE4DE), Color(0xFFD7C9C3)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            scale(this.size.width / 100f, pivot = Offset.Zero) {
                when (kind) {
                    ClayKind.WORKSHOP -> drawGear()
                    ClayKind.SESSIONS -> drawCalendar()
                    ClayKind.RANKING -> drawTrophy()
                    ClayKind.STORE -> drawGift()
                }
            }
        }
    }
}

private fun DrawScope.drawGear() {
    contactShadow(50f, 83f, 52f, 9f)
    val c = Offset(50f, 48f)
    val toothBrush = clay(Color(0xFFFF9A78), Color(0xFFDC6941), Color(0xFFB34A2B), Offset(40f, 30f), 60f)
    for (i in 0 until 8) {
        rotate(i * 45f, pivot = c) {
            drawRoundRect(toothBrush, Offset(43f, 12f), Size(14f, 76f), CornerRadius(5f))
        }
    }
    drawCircle(clay(Color(0xFFFFA583), Color(0xFFDC6941), Color(0xFFB34A2B), Offset(40f, 34f), 48f), 30f, c)
    drawCircle(clay(Color(0xFFB34A2B), Color(0xFFD7C9C3), Color(0xFFEFE4DE), Offset(52f, 56f), 16f), 12f, c)
    drawCircle(Color(0xFF2B74AC), 5.5f, c)
    gloss(32f, 26f, 22f, 10f)
}

private fun DrawScope.drawCalendar() {
    contactShadow(50f, 85f, 56f, 8f)
    drawRoundRect(
        clay(Color(0xFFFFFFFF), Color(0xFFF7EEE6), Color(0xFFD9C9BC), Offset(36f, 30f), 80f),
        Offset(15f, 20f), Size(70f, 62f), CornerRadius(14f)
    )
    drawRoundRect(
        clay(Color(0xFF5FA2D6), Color(0xFF2B74AC), Color(0xFF186AA4), Offset(30f, 20f), 60f),
        Offset(15f, 20f), Size(70f, 20f), CornerRadius(14f)
    )
    drawRect(Color(0xFF2B74AC), Offset(15f, 30f), Size(70f, 10f))
    for (x in listOf(33f, 67f)) {
        drawRoundRect(
            clay(Color(0xFFFFE08A), Color(0xFFFEC856), Color(0xFFD99A1E), Offset(x - 2f, 12f), 14f),
            Offset(x - 4.5f, 11f), Size(9f, 18f), CornerRadius(4.5f)
        )
    }
    val check = Path().apply { moveTo(34f, 61f); lineTo(46f, 72f); lineTo(68f, 50f) }
    drawPath(check, Color(0xFF3CAEA3), style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    gloss(21f, 44f, 14f, 6f, 0.6f)
}

private fun DrawScope.drawTrophy() {
    contactShadow(50f, 89f, 50f, 8f)
    val gold = clay(Color(0xFFFFE08A), Color(0xFFFEC856), Color(0xFFD99A1E), Offset(38f, 26f), 52f)
    // handles
    drawArc(Color(0xFFE8A92E), 90f, 180f, false, Offset(10f, 23f), Size(30f, 30f), style = Stroke(7f, cap = StrokeCap.Round))
    drawArc(Color(0xFFE8A92E), -90f, 180f, false, Offset(60f, 23f), Size(30f, 30f), style = Stroke(7f, cap = StrokeCap.Round))
    // stem + base
    drawRoundRect(gold, Offset(43f, 60f), Size(14f, 16f), CornerRadius(4f))
    drawRoundRect(
        clay(Color(0xFF3F8FD0), Color(0xFF0560AB), Color(0xFF034A87), Offset(36f, 74f), 40f),
        Offset(28f, 74f), Size(44f, 14f), CornerRadius(6f)
    )
    drawRoundRect(Color(0xFFFEC856), Offset(42f, 78f), Size(16f, 5f), CornerRadius(2.5f))
    // cup
    val cup = Path().apply {
        moveTo(26f, 14f); lineTo(74f, 14f); lineTo(74f, 38f)
        cubicTo(74f, 54f, 63f, 64f, 50f, 64f)
        cubicTo(37f, 64f, 26f, 54f, 26f, 38f); close()
    }
    drawPath(cup, gold)
    drawRoundRect(Color(0xFFFFE9A8), Offset(24f, 11f), Size(52f, 8f), CornerRadius(4f))
    drawPath(starPath(50f, 38f, 12f, 5.2f), clay(Color(0xFFFFF4D2), Color(0xFFFFE9A8), Color(0xFFF2C25A), Offset(46f, 33f), 16f))
    gloss(32f, 20f, 9f, 22f, 0.45f)
}

private fun DrawScope.drawGift() {
    contactShadow(50f, 88f, 62f, 9f)
    drawRoundRect(
        clay(Color(0xFFFF8D74), Color(0xFFE0684F), Color(0xFFB4452F), Offset(36f, 50f), 70f),
        Offset(18f, 44f), Size(64f, 42f), CornerRadius(10f)
    )
    drawRoundRect(
        clay(Color(0xFFFFA08A), Color(0xFFEB7A62), Color(0xFFC4543B), Offset(34f, 30f), 70f),
        Offset(13f, 32f), Size(74f, 18f), CornerRadius(9f)
    )
    val ribbon = clay(Color(0xFFFFE08A), Color(0xFFFEC856), Color(0xFFD99A1E), Offset(46f, 40f), 40f)
    drawRoundRect(ribbon, Offset(43f, 32f), Size(14f, 54f), CornerRadius(4f))
    rotate(-28f, pivot = Offset(42f, 26f)) { drawOval(ribbon, Offset(24f, 14f), Size(20f, 26f)) }
    rotate(28f, pivot = Offset(58f, 26f)) { drawOval(ribbon, Offset(56f, 14f), Size(20f, 26f)) }
    drawCircle(clay(Color(0xFFFFE9A8), Color(0xFFFEC856), Color(0xFFD99A1E), Offset(47f, 26f), 14f), 8f, Offset(50f, 30f))
    gloss(22f, 52f, 10f, 18f, 0.35f)
}
