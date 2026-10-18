package com.kichikan.ak1.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.model.WheelItem
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon
import kotlinx.coroutines.launch
import java.time.LocalTime

private val SEGMENT_COLORS = listOf(
    Color(0xFF1EFCE5), Color(0xFF5BB5FF), Color(0xFFB197FF), Color(0xFFFEC856), Color(0xFFFF8A6B), Color(0xFF3CAEA3)
)

/** Spin recipe derived from the phone clock's seconds at the moment of the spin. */
private data class SpinPlan(val seconds: Int, val turns: Int, val durationMs: Int)

private fun planFromClock(): SpinPlan {
    val s = LocalTime.now().second              // 0..59, read at spin time
    val turns = 4 + s % 9                       // 4..12 full turns
    val duration = 3800 + s * 45                // 3.8s .. 6.4s
    return SpinPlan(s, turns, duration)
}

/**
 * The main wheel: a real, spinnable wheel. The prize is drawn by the repository (weighted, with
 * all its rules) *before* the animation, then the wheel lands exactly on that prize. How fast it
 * spins depends on the phone's current seconds, so every spin feels different.
 */
@Composable
fun SpinWheelPage(repo: AppRepository, padding: PaddingValues) {
    var tick by remember { mutableIntStateOf(0) }
    var memberId by remember(repo.members.size) { mutableStateOf(repo.members.firstOrNull()?.id ?: "") }
    var result by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var spinning by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val member = remember(tick, memberId) { repo.members.firstOrNull { it.id == memberId } }
    val segments: List<WheelItem> = repo.wheel.items.filter { it.active }

    fun spin() {
        if (spinning || member == null) return
        result = null
        val prize = try { repo.spinWheel(memberId) } catch (e: Exception) { result = e.message ?: "چرخاندن گردونه انجام نشد."; return }
        if (prize == null) { result = "جایزه قابل انتخابی وجود ندارد یا جوایز این عضو تمام شده‌اند."; return }
        val index = segments.indexOfFirst { it.id == prize.id }.coerceAtLeast(0)
        val plan = planFromClock()
        val seg = 360f / segments.size.coerceAtLeast(1)
        val jitter = ((System.nanoTime() % 1000L) / 1000f - 0.5f) * seg * 0.6f
        val current = ((rotation.value % 360f) + 360f) % 360f
        val target = ((-(index + 0.5f) * seg + jitter - current) % 360f + 360f) % 360f
        val end = rotation.value + plan.turns * 360f + target
        info = "ثانیه‌ی ساعت: ${plan.seconds} • ${plan.turns} دور در ${"%.1f".format(plan.durationMs / 1000f)} ثانیه"
        spinning = true
        scope.launch {
            rotation.animateTo(end, tween(plan.durationMs, easing = CubicBezierEasing(0.12f, 0.7f, 0.1f, 1f)))
            result = "🎉 نتیجه: ${prize.title}" + (prize.customText?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
            spinning = false
            tick++
        }
    }

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("گردونه شانس", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        Text(
            "عضو را انتخاب کن و گردونه را بچرخان. جایزه روی گردونه ثبت می‌شود و پاداش در سابقه عضو می‌نشیند.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth()
        )
        if (repo.members.isEmpty()) {
            Text("برای استفاده از گردونه ابتدا یک عضو بساز.", modifier = Modifier.fillMaxWidth())
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repo.members.forEach { m ->
                    FilterChip(selected = memberId == m.id, onClick = { if (!spinning) { memberId = m.id; result = null } }, label = { Text(m.name) })
                }
            }
            member?.let {
                Text("سطح ${it.economy.level} • امتیاز ${it.economy.spendablePoints} • الماس ${it.economy.diamonds}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
            }
            if (segments.isEmpty()) {
                Text("هنوز جایزه‌ی فعالی برای گردونه تعریف نشده است؛ از «تراشکاری ← تنظیم گردونه» جایزه اضافه کن.", modifier = Modifier.fillMaxWidth())
            } else {
                Box(Modifier.fillMaxWidth(0.92f).aspectRatio(1f), contentAlignment = Alignment.TopCenter) {
                    Canvas(Modifier.fillMaxSize().padding(top = 14.dp)) {
                        val d = size.minDimension
                        val radius = d / 2f
                        val center = Offset(size.width / 2f, radius)
                        val seg = 360f / segments.size
                        // outer neon glow + rim
                        drawCircle(Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.35f), Color.Transparent), center, radius * 1.18f), radius * 1.18f, center)
                        drawCircle(Color.White.copy(alpha = 0.18f), radius, center)
                        val r = radius * 0.94f
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(2, 30, 67)
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = r * (if (segments.size > 8) 0.085f else 0.11f)
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        }
                        val spun = rotation.value
                        segments.forEachIndexed { i, item ->
                            val start = -90f + i * seg + spun
                            drawArc(SEGMENT_COLORS[i % SEGMENT_COLORS.size], start, seg, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2))
                            drawArc(Color.White.copy(alpha = 0.55f), start, seg, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(2f))
                            val mid = (start + seg / 2f)
                            drawContext.canvas.nativeCanvas.apply {
                                save()
                                rotate(mid, center.x, center.y)
                                drawText(item.title.take(if (segments.size > 8) 9 else 13), center.x + r * 0.60f, center.y + paint.textSize * 0.35f, paint)
                                restore()
                            }
                        }
                        // glossy highlight + hub
                        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(center.x - r * 0.3f, center.y - r * 0.4f), r), r, center)
                        drawCircle(Neon.Navy, r * 0.16f, center)
                        drawCircle(Neon.Gold, r * 0.11f, center)
                        // pointer (fixed, top)
                        val pointer = Path().apply {
                            moveTo(center.x - r * 0.07f, center.y - radius - 8f)
                            lineTo(center.x + r * 0.07f, center.y - radius - 8f)
                            lineTo(center.x, center.y - r * 0.80f)
                            close()
                        }
                        drawPath(pointer, Color(0xFFFF5A4D))
                        drawPath(pointer, Color.White, style = Stroke(3f))
                    }
                }
                Button(
                    onClick = { spin() },
                    enabled = member != null && !spinning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (spinning) "در حال چرخش…" else "چرخاندن گردونه") }
                info?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                result?.let {
                    Card(Modifier.fillMaxWidth(), accent = Neon.Gold) {
                        Text(it, Modifier.padding(16.dp), color = Neon.Gold, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text("هزینه: " + when (repo.wheel.mode.name) { "FREE" -> "رایگان"; "POINTS" -> "${repo.wheel.spinCostPoints} امتیاز"; else -> "${repo.wheel.spinCostPoints} امتیاز + ${repo.wheel.spinCostDiamonds} الماس" },
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
