package com.kichikan.ak1.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.ui.Exports
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.ui.PageHeader
import com.kichikan.ak1.ui.RankRow
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Motion
import com.kichikan.ak1.ui.theme.Neon

private data class PodiumEntry(
    val name: String,
    val caption: String,
    val fraction: Float,
    val avatar: String?,
    val frame: String?,
    val rank: Int
)

@Composable
internal fun RankingScreen(repo: AppRepository, padding: PaddingValues, groups: Boolean, back: () -> Unit) {
    val context = LocalContext.current
    val avatarOf = { m: Member -> repo.shop.firstOrNull { it.id == m.avatarItemId }?.imagePath }
    val frameOf = { m: Member -> repo.shop.firstOrNull { it.id == m.frameItemId }?.imagePath }
    val rankedMembers = repo.members.sortedWith(compareByDescending<Member> { it.economy.level }.thenByDescending { it.economy.xp })
    val rankedGroups = repo.groups.sortedByDescending { it.economy.spendablePoints }
    val maxMemberXp = (rankedMembers.maxOfOrNull { it.economy.xp } ?: 0).coerceAtLeast(1)
    val maxGroupPoints = (rankedGroups.maxOfOrNull { it.economy.spendablePoints } ?: 0).coerceAtLeast(1)
    val title = if (groups) "رتبه‌بندی گروه‌ها" else "رتبه‌بندی اعضا"
    val rankingExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri != null) {
            val rows = if (groups) rankedGroups.map { g -> RankRow(g.name, "امتیاز ${g.economy.spendablePoints} • الماس ${g.economy.diamonds}", letter = g.name) }
            else rankedMembers.map { m -> RankRow(m.name, "سطح ${m.economy.level} • XP ${m.economy.xp} • امتیاز ${m.economy.spendablePoints} • الماس ${m.economy.diamonds}", avatarOf(m), frameOf(m), m.name) }
            runCatching { context.contentResolver.openOutputStream(uri)?.use { out -> Exports.rankingCard(title, rows).compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out) } }
                .onSuccess { Toast.makeText(context, "تصویر رتبه‌بندی ذخیره شد", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "ذخیره تصویر ناموفق بود: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader(title, back, if (groups) "فقط با امتیاز و الماس مستقل گروه محاسبه می‌شود." else "بر اساس سطح و XP؛ خروجی تصویر، آواتار و قاب هر عضو را هم دارد.")
        Button(onClick = { rankingExporter.launch(if (groups) "رتبه‌بندی-گروه‌ها.jpg" else "رتبه‌بندی-اعضا.jpg") }, modifier = Modifier.fillMaxWidth()) { Text("خروجی JPEG رتبه‌بندی") }
        if (!groups) {
            if (rankedMembers.isEmpty()) {
                Text("هنوز عضوی ثبت نشده است.")
            } else {
                PodiumCard(
                    entries = rankedMembers.take(3).mapIndexed { index, m ->
                        PodiumEntry(
                            name = m.name,
                            caption = "سطح ${m.economy.level} • ${m.economy.xp} XP",
                            fraction = m.economy.xp.toFloat() / maxMemberXp,
                            avatar = avatarOf(m),
                            frame = frameOf(m),
                            rank = index + 1
                        )
                    }
                )
                val rest = rankedMembers.drop(3)
                if (rest.isNotEmpty()) {
                    Text("ادامهٔ جدول", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(rest, key = { _, m -> m.id }) { index, m ->
                            val fraction by animateFloatAsState(
                                targetValue = m.economy.xp.toFloat() / maxMemberXp,
                                animationSpec = tween(700, easing = Motion.Decelerate),
                                label = "member-rank-bar"
                            )
                            RankRowCard(
                                rank = index + 4,
                                title = m.name,
                                caption = "سطح ${m.economy.level} • XP ${m.economy.xp}",
                                fraction = fraction,
                                accent = Neon.Cyan,
                                avatar = avatarOf(m),
                                frame = frameOf(m),
                                letter = m.name,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        } else {
            if (rankedGroups.isEmpty()) {
                Text("هنوز گروهی ساخته نشده است.")
            } else {
                PodiumCard(
                    entries = rankedGroups.take(3).mapIndexed { index, g ->
                        PodiumEntry(
                            name = g.name,
                            caption = "${g.economy.spendablePoints} امتیاز",
                            fraction = g.economy.spendablePoints.toFloat() / maxGroupPoints,
                            avatar = null,
                            frame = null,
                            rank = index + 1
                        )
                    }
                )
                val rest = rankedGroups.drop(3)
                if (rest.isNotEmpty()) {
                    Text("ادامهٔ جدول", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(rest, key = { _, g -> g.id }) { index, g ->
                            val fraction by animateFloatAsState(
                                targetValue = g.economy.spendablePoints.toFloat() / maxGroupPoints,
                                animationSpec = tween(700, easing = Motion.Decelerate),
                                label = "group-rank-bar"
                            )
                            RankRowCard(
                                rank = index + 4,
                                title = g.name,
                                caption = "امتیاز ${g.economy.spendablePoints} • الماس ${g.economy.diamonds}",
                                fraction = fraction,
                                accent = Neon.Violet,
                                avatar = null,
                                frame = null,
                                letter = g.name,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(entries: List<PodiumEntry>) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.92f,
        animationSpec = tween(Motion.Slow, easing = Motion.Emphasized),
        label = "podium-enter"
    )
    Card(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale
            },
        accent = Neon.Gold
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("سکوی افتخار", style = MaterialTheme.typography.titleMedium)
            val order = listOfNotNull(entries.getOrNull(1), entries.getOrNull(0), entries.getOrNull(2))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                order.forEach { entry -> PodiumColumn(entry, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PodiumColumn(entry: PodiumEntry, modifier: Modifier = Modifier) {
    val accent = when (entry.rank) {
        1 -> Neon.Gold
        2 -> Color(0xFFC9D6E8)
        else -> Color(0xFFE0A074)
    }
    val avatarSize = if (entry.rank == 1) 74.dp else 60.dp
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (entry.rank == 1) Text("👑", style = MaterialTheme.typography.titleLarge)
        Box(contentAlignment = Alignment.BottomCenter) {
            MemberAvatar(entry.avatar, entry.frame, avatarSize, entry.name)
            Surface(shape = CircleShape, color = accent, modifier = Modifier.offset(y = 6.dp)) {
                Text(
                    "${entry.rank}",
                    Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Neon.Navy
                )
            }
        }
        Text(
            entry.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            entry.caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        RankBar(entry.fraction, accent, Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RankRowCard(
    rank: Int,
    title: String,
    caption: String,
    fraction: Float,
    accent: Color,
    avatar: String?,
    frame: String?,
    letter: String,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), accent = if (rank <= 3) Neon.Gold else null) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                ) {
                    Text(
                        "$rank",
                        Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
                MemberAvatar(avatar, frame, 46.dp, letter)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RankBar(fraction, accent)
        }
    }
}

@Composable
private fun RankBar(fraction: Float, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(accent, Neon.Sky)))
        )
    }
}
