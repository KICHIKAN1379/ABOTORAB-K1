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

enum class ClayKind { WORKSHOP, SESSIONS, RANKING, STORE, MISSION, PRIZE, FRAME, WHEEL, PODIUM, GROUPS, AVATAR, CLIPBOARD }

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
                    ClayKind.MISSION -> drawMission()
                    ClayKind.PRIZE -> drawPrize()
                    ClayKind.FRAME -> drawFrameIcon()
                    ClayKind.WHEEL -> drawWheelIcon()
                    ClayKind.PODIUM -> drawPodium()
                    ClayKind.GROUPS -> drawGroups()
                    ClayKind.AVATAR -> drawAvatarIcon()
                    ClayKind.CLIPBOARD -> drawClipboard()
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

private val goldClay get() = clay(Color(0xFFFFE08A), Color(0xFFFEC856), Color(0xFFD99A1E), Offset(38f, 30f), 60f)

private fun DrawScope.drawMission() {
    contactShadow(50f, 87f, 66f, 9f)
    val mountain = Path().apply { moveTo(8f, 84f); lineTo(44f, 30f); lineTo(58f, 50f); lineTo(70f, 38f); lineTo(94f, 84f); close() }
    drawPath(mountain, clay(Color(0xFF7DB8E6), Color(0xFF2B74AC), Color(0xFF186AA4), Offset(40f, 42f), 72f))
    val snow = Path().apply { moveTo(44f, 30f); lineTo(35f, 45f); lineTo(41f, 43f); lineTo(46f, 49f); lineTo(51f, 42f); lineTo(55f, 46f); close() }
    drawPath(snow, Color(0xFFFDFDFD))
    val route = Path().apply { moveTo(52f, 82f); cubicTo(26f, 74f, 72f, 64f, 46f, 52f) }
    drawPath(route, Color(0xFFFEC856), style = Stroke(6.5f, cap = StrokeCap.Round))
    drawRoundRect(Color(0xFF8D6E63), Offset(43.2f, 12f), Size(2.8f, 20f), CornerRadius(1.4f))
    drawPath(Path().apply { moveTo(46f, 12.5f); lineTo(63f, 17.5f); lineTo(46f, 23f); close() }, clay(Color(0xFFFF9A78), Color(0xFFE0684F), Color(0xFFB4452F), Offset(52f, 16f), 14f))
}

private fun DrawScope.drawPrize() {
    contactShadow(50f, 89f, 44f, 7f)
    rotate(-14f, pivot = Offset(50f, 30f)) { drawRoundRect(clay(Color(0xFF6FB0E0), Color(0xFF2B74AC), Color(0xFF186AA4), Offset(40f, 20f), 40f), Offset(31f, 8f), Size(17f, 44f), CornerRadius(4f)) }
    rotate(14f, pivot = Offset(50f, 30f)) { drawRoundRect(clay(Color(0xFFFF9A78), Color(0xFFE0684F), Color(0xFFB4452F), Offset(60f, 20f), 40f), Offset(52f, 8f), Size(17f, 44f), CornerRadius(4f)) }
    drawCircle(goldClay, 30f, Offset(50f, 62f))
    drawCircle(Color(0xFFFFE9A8), 21f, Offset(50f, 62f))
    drawPath(starPath(50f, 62f, 15f, 6.5f), clay(Color(0xFFFFC94D), Color(0xFFFEC856), Color(0xFFD99A1E), Offset(46f, 56f), 20f))
    gloss(32f, 42f, 18f, 8f, 0.55f)
}

private fun DrawScope.drawFrameIcon() {
    contactShadow(50f, 88f, 60f, 8f)
    drawRoundRect(goldClay, Offset(13f, 13f), Size(74f, 74f), CornerRadius(20f))
    drawRoundRect(clay(Color(0xFFB34A2B), Color(0xFFF3E7DF), Color(0xFFFFFFFF), Offset(52f, 56f), 44f), Offset(24f, 24f), Size(52f, 52f), CornerRadius(13f))
    drawCircle(Color(0xFFFFD0AE), 9f, Offset(50f, 44f))
    drawArc(Color(0xFF2B74AC), 180f, 180f, true, Offset(32f, 52f), Size(36f, 40f))
    gloss(20f, 17f, 26f, 8f, 0.5f)
}

private fun DrawScope.drawWheelIcon() {
    contactShadow(50f, 90f, 54f, 7f)
    val colors = listOf(0xFFE0684F, 0xFFFEC856, 0xFF3CAEA3, 0xFF2B74AC, 0xFFB197FF, 0xFFFF9A78).map { Color(it) }
    for (i in colors.indices) drawArc(colors[i], -90f + i * 60f, 60f, true, Offset(14f, 14f), Size(72f, 72f))
    drawCircle(Color.White.copy(alpha = 0.22f), 36f, Offset(50f, 50f), style = Stroke(5f))
    drawCircle(goldClay, 11f, Offset(50f, 50f))
    drawPath(Path().apply { moveTo(43f, 6f); lineTo(57f, 6f); lineTo(50f, 22f); close() }, Color(0xFFB4452F))
    gloss(24f, 20f, 22f, 9f, 0.4f)
}

private fun DrawScope.drawPodium() {
    contactShadow(50f, 88f, 70f, 8f)
    drawRoundRect(clay(Color(0xFF6FB0E0), Color(0xFF2B74AC), Color(0xFF186AA4), Offset(24f, 60f), 40f), Offset(12f, 56f), Size(26f, 32f), CornerRadius(7f))
    drawRoundRect(clay(Color(0xFFFF9A78), Color(0xFFE0684F), Color(0xFFB4452F), Offset(76f, 66f), 36f), Offset(62f, 66f), Size(26f, 22f), CornerRadius(7f))
    drawRoundRect(goldClay, Offset(37f, 40f), Size(26f, 48f), CornerRadius(7f))
    drawPath(starPath(50f, 24f, 13f, 5.6f), goldClay)
    gloss(41f, 44f, 8f, 18f, 0.4f)
}

private fun DrawScope.drawGroups() {
    contactShadow(50f, 88f, 66f, 8f)
    fun person(cx: Float, top: Float, r: Float, body: Color, bodyDark: Color) {
        drawCircle(clay(Color(0xFFFFE2C8), Color(0xFFFFC9A0), Color(0xFFE5A67C), Offset(cx - r * 0.3f, top - r * 0.3f), r * 2f), r, Offset(cx, top))
        drawArc(clay(Color.White, body, bodyDark, Offset(cx - r, top + r * 2f), r * 5f), 180f, 180f, true, Offset(cx - r * 1.9f, top + r * 1.3f), Size(r * 3.8f, r * 3.6f))
    }
    person(24f, 46f, 9f, Color(0xFFB197FF), Color(0xFF7B64C3))
    person(76f, 46f, 9f, Color(0xFFE0684F), Color(0xFFB4452F))
    person(50f, 38f, 12f, Color(0xFF2B74AC), Color(0xFF186AA4))
}

private fun DrawScope.drawAvatarIcon() {
    contactShadow(50f, 89f, 56f, 8f)
    drawArc(clay(Color(0xFF7DB8E6), Color(0xFF2B74AC), Color(0xFF186AA4), Offset(40f, 70f), 50f), 180f, 180f, true, Offset(16f, 58f), Size(68f, 62f))
    drawCircle(clay(Color(0xFFFFE2C8), Color(0xFFFFC9A0), Color(0xFFE5A67C), Offset(42f, 30f), 36f), 21f, Offset(50f, 40f))
    drawArc(Color(0xFF5D4037), 180f, 180f, true, Offset(28f, 18f), Size(44f, 36f))
    drawOval(Color(0xFFFFC9A0), Offset(32f, 36f), Size(36f, 26f))
    gloss(36f, 22f, 12f, 6f, 0.5f)
}

private fun DrawScope.drawClipboard() {
    contactShadow(50f, 89f, 56f, 8f)
    drawRoundRect(clay(Color(0xFFD9976A), Color(0xFFB8744B), Color(0xFF8D5535), Offset(36f, 28f), 80f), Offset(18f, 12f), Size(64f, 76f), CornerRadius(14f))
    drawRoundRect(clay(Color.White, Color(0xFFF7EEE6), Color(0xFFD9C9BC), Offset(40f, 36f), 70f), Offset(25f, 24f), Size(50f, 58f), CornerRadius(8f))
    for ((i, w) in listOf(34f, 28f, 34f).withIndex()) {
        drawCircle(Color(0xFF3CAEA3), 3.4f, Offset(33f, 40f + i * 14f))
        drawRoundRect(Color(0xFFBFB3A9), Offset(41f, 38f + i * 14f), Size(w, 5f), CornerRadius(2.5f))
    }
    drawRoundRect(goldClay, Offset(36f, 6f), Size(28f, 14f), CornerRadius(7f))
}

// ───────────────────────── Style 4: layered glass (everything else) ─────────────────────────

enum class GlassKind { IDENTITY, MEMBERS, ASSISTANT, WHEEL, XP, POINTS, DIAMOND }

/** Translucent glass tile with a coloured glow behind a frosted symbol (100-unit grid). */
@Composable
fun GlassIcon(kind: GlassKind, modifier: Modifier = Modifier, size: Dp = 48.dp, tint: Color = Neon.Cyan) {
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))))
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.0f), tint.copy(alpha = 0.10f)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            scale(this.size.width / 100f, pivot = Offset.Zero) {
                // glow ("sun" behind the glass)
                drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.75f), Color.Transparent), Offset(66f, 34f), 34f), 34f, Offset(66f, 34f))
                val fill = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.30f)),
                    start = Offset(20f, 15f), end = Offset(80f, 85f)
                )
                val edge = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val edgeColor = Color.White.copy(alpha = 0.95f)
                fun path(p: Path) { drawPath(p, fill); drawPath(p, edgeColor, style = edge) }
                when (kind) {
                    GlassKind.IDENTITY -> {
                        path(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(16f, 26f, 84f, 74f, CornerRadius(10f))) })
                        drawCircle(tint.copy(alpha = 0.9f), 8f, Offset(34f, 45f))
                        drawRoundRect(tint.copy(alpha = 0.9f), Offset(48f, 40f), Size(26f, 5f), CornerRadius(2.5f))
                        drawRoundRect(tint.copy(alpha = 0.6f), Offset(48f, 52f), Size(20f, 5f), CornerRadius(2.5f))
                        drawRoundRect(tint.copy(alpha = 0.6f), Offset(26f, 62f), Size(40f, 4f), CornerRadius(2f))
                    }
                    GlassKind.MEMBERS -> {
                        path(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(36f, 22f), Size(28f, 28f))) })
                        path(Path().apply { moveTo(20f, 80f); cubicTo(20f, 58f, 80f, 58f, 80f, 80f); close() })
                    }
                    GlassKind.ASSISTANT -> {
                        path(Path().apply {
                            moveTo(50f, 14f); cubicTo(52f, 38f, 62f, 48f, 86f, 50f)
                            cubicTo(62f, 52f, 52f, 62f, 50f, 86f)
                            cubicTo(48f, 62f, 38f, 52f, 14f, 50f)
                            cubicTo(38f, 48f, 48f, 38f, 50f, 14f); close()
                        })
                    }
                    GlassKind.WHEEL -> {
                        val c = Offset(50f, 50f)
                        drawCircle(fill, 32f, c); drawCircle(edgeColor, 32f, c, style = edge)
                        for (i in 0 until 6) rotate(i * 60f, pivot = c) {
                            drawLine(tint.copy(alpha = 0.85f), Offset(50f, 50f), Offset(50f, 20f), 3f, StrokeCap.Round)
                        }
                        drawCircle(tint, 7f, c)
                    }
                    GlassKind.XP -> {
                        path(Path().apply {
                            moveTo(50f, 16f); lineTo(80f, 50f); lineTo(60f, 50f)
                            lineTo(60f, 82f); lineTo(40f, 82f); lineTo(40f, 50f); lineTo(20f, 50f); close()
                        })
                    }
                    GlassKind.POINTS -> path(starPath(50f, 52f, 36f, 15f))
                    GlassKind.DIAMOND -> {
                        path(Path().apply {
                            moveTo(30f, 22f); lineTo(70f, 22f); lineTo(88f, 42f); lineTo(50f, 84f); lineTo(12f, 42f); close()
                        })
                        drawLine(edgeColor, Offset(12f, 42f), Offset(88f, 42f), 2f)
                        drawLine(edgeColor, Offset(36f, 22f), Offset(50f, 42f), 2f)
                        drawLine(edgeColor, Offset(64f, 22f), Offset(50f, 42f), 2f)
                    }
                }
            }
        }
    }
}
