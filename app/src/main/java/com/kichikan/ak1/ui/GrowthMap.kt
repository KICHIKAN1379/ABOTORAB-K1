package com.kichikan.ak1.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.model.Member
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon

private val DEFAULT_STAGES = listOf("شروع مسیر", "خدمت‌گزار حلقه", "مسئولیت‌پذیری", "اعتمادسازی", "پیش‌قدم در خیر", "قله کمال")

private fun readStages(prefs: android.content.SharedPreferences): List<String> {
    val raw = runCatching { prefs.getString("growth_stages", null) }.getOrNull() ?: return DEFAULT_STAGES
    return runCatching {
        val arr = org.json.JSONArray(raw)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(DEFAULT_STAGES)
}

private fun readReached(prefs: android.content.SharedPreferences, key: String): Set<String> =
    runCatching { prefs.getStringSet(key, emptySet())?.toSet() }.getOrNull() ?: emptySet()

/**
 * Growth map ("نقشه کمال") as a full-screen page. It deliberately uses a plain scrolling column
 * (no lazy list inside an alert dialog) so it always opens, whatever the number of stages.
 */
@Composable
fun GrowthMapDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
    val stages = remember(member.id) { readStages(prefs) }
    val reachedKey = "growth_reached_${member.id}"
    var reached by remember(member.id) { mutableStateOf(readReached(prefs, reachedKey)) }
    val count = stages.count { it in reached }
    val progress = if (stages.isEmpty()) 0f else count.toFloat() / stages.size.toFloat()
    val avatarPath = repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath
    val framePath = repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Neon.Navy) {
            Column(
                Modifier.fillMaxSize().background(Neon.BackgroundBrush).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(close) { Text("→ بازگشت") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MemberAvatar(avatarPath, framePath, 72.dp, member.name)
                    Column {
                        Text("نقشه کمال", style = MaterialTheme.typography.headlineSmall)
                        Text("صعود ${member.name}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                            val w = size.width
                            val h = size.height
                            val mountain = Path().apply {
                                moveTo(0f, h); lineTo(w * 0.22f, h * 0.46f); lineTo(w * 0.36f, h * 0.67f)
                                lineTo(w * 0.60f, h * 0.16f); lineTo(w * 0.78f, h * 0.53f); lineTo(w, h * 0.24f); lineTo(w, h); close()
                            }
                            drawPath(mountain, Brush.verticalGradient(listOf(Neon.Sky.copy(alpha = 0.55f), Neon.Indigo.copy(alpha = 0.35f))))
                            val route = Path().apply {
                                moveTo(w * 0.08f, h * 0.92f); lineTo(w * 0.22f, h * 0.62f); lineTo(w * 0.36f, h * 0.76f); lineTo(w * 0.60f, h * 0.30f)
                            }
                            drawPath(route, Neon.Cyan, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawCircle(Neon.Gold, 8f, Offset(w * 0.60f, h * 0.28f))
                        }
                        Text("$count از ${stages.size} مقام ثبت شده", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("سطح ${member.economy.level} • XP ${member.economy.xp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (stages.isEmpty()) {
                    Text("هنوز مرحله‌ای تعریف نشده؛ از تنظیمات، مسیر نقشه کمال را بساز.")
                }
                stages.forEachIndexed { index, stage ->
                    val isReached = stage in reached
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (isReached) Neon.Cyan else Color.Transparent,
                            border = BorderStroke(1.5.dp, if (isReached) Neon.Cyan else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                "${index + 1}", Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                color = if (isReached) Neon.Navy else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Card(Modifier.weight(1f), accent = if (isReached) Neon.Cyan else null) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stage, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (isReached) "مقام کسب‌شده" else "در انتظار صعود",
                                    color = if (isReached) Neon.Cyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                OutlinedButton(onClick = {
                                    reached = if (isReached) reached - stage else reached + stage
                                    prefs.edit().putStringSet(reachedKey, reached.toSet()).apply()
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (isReached) "لغو ثبت این مقام" else "ثبت رسیدن عضو به این مقام")
                                }
                            }
                        }
                    }
                }
                Text("ثبت مقام‌ها دستی است و فقط مربی آن را تغییر می‌دهد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
