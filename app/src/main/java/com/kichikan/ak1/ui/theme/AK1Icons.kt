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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Helper shared by the glass icons below.

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
