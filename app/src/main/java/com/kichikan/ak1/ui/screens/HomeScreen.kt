package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.R
import com.kichikan.ak1.ui.theme.AppIcon
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.AvatarRender
import com.kichikan.ak1.ui.theme.GlassIcon
import com.kichikan.ak1.ui.theme.GlassKind
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.ui.components.AnimatedCounter
import com.kichikan.ak1.ui.components.StaggeredItem

@Composable
internal fun HomeScreen(repo: AppRepository, padding: PaddingValues, openMembers: () -> Unit, changed: () -> Unit, open: (Tab) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
    val ringPhotoUri = remember { prefs.getString("ring_profile_image", null) }
    val ringPhoto = remember(ringPhotoUri) {
        ringPhotoUri?.let { raw ->
            AvatarRender.decodeSampled(context, android.net.Uri.parse(raw), 256)?.asImageBitmap()
        }
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); tick++ } }
    var action by remember { mutableStateOf(false) }
    var assistantOpen by remember { mutableStateOf(false) }
    val now = remember(tick) { java.time.LocalDateTime.now() }
    val jalali = remember(tick) { JalaliCalendar.fromGregorian(now.toLocalDate()) }
    val shortcuts = listOf(
        Triple("شناسنامه", "home_identity", "◉"),
        Triple("مدیریت اعضا", "home_members", "♙"),
        Triple("دستیار مربی", "home_assistant", "✦"),
        Triple("تراشکاری", "home_workshop", "⚒"),
        Triple("جلسات", "home_sessions", "▦"),
        Triple("رقابت", "home_ranking", "🏆"),
        Triple("فروشگاه", "home_store", "🎁"),
        Triple("گردونه", "home_wheel", "◎")
    )
    fun openShortcut(key: String) {
        when (key) {
            "home_identity" -> action = true
            "home_members" -> openMembers()
            "home_assistant" -> assistantOpen = true
            "home_workshop" -> open(Tab.WORKSHOP)
            "home_sessions" -> open(Tab.SESSIONS)
            "home_ranking" -> open(Tab.RANKING)
            "home_store" -> open(Tab.STORE)
            "home_wheel" -> open(Tab.WHEEL)
        }
    }
    val visibleShortcuts = shortcuts.filter { (_, key, _) ->
        prefs.getBoolean(key, key in listOf("home_identity", "home_members", "home_assistant"))
    }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Neon.Cyan.copy(alpha = 0.30f)),
            accent = Neon.Cyan
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF06255A), Color(0xFF0A3C86), Color(0xFF062A63)))
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(78.dp).background(
                                    Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.35f), Color.Transparent)),
                                    CircleShape
                                )
                            )
                            Surface(
                                modifier = Modifier.size(62.dp),
                                shape = CircleShape,
                                color = Neon.Navy,
                                border = BorderStroke(2.dp, Neon.Cyan)
                            ) {
                                if (ringPhoto != null) {
                                    Image(bitmap = ringPhoto, contentDescription = "تصویر پروفایل حلقه", modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("K1", color = Neon.Cyan, style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("سلام، ${repo.ring?.ringName ?: "مربی"}", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "پنل مربی • مدیریت حلقه",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HeroChip("● آفلاین", Neon.Green)
                                HeroChip("${JalaliCalendar.MONTH_NAMES[jalali.month - 1]} ${jalali.year}", Neon.Cyan)
                            }
                        }
                        Surface(
                            modifier = Modifier.size(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Neon.Cyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Neon.Cyan.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${jalali.day}", color = Neon.Cyan, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                    Text(
                        "روز تازه، فرصت تازه برای رشد",
                        style = MaterialTheme.typography.labelMedium,
                        color = Neon.Ice.copy(alpha = 0.78f)
                    )
                }
            }
        }
        SummaryCard(repo)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("دسترسی سریع", style = MaterialTheme.typography.titleLarge)
            Text("${visibleShortcuts.size} میانبر", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (visibleShortcuts.isEmpty()) {
            Card(
                        Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                Text("هنوز میانبری انتخاب نشده؛ از تنظیمات، میانبرهای خانه را فعال کن.", Modifier.padding(16.dp))
            }
        } else {
            visibleShortcuts.chunked(2).forEachIndexed { rowIndex, rowItems ->
                StaggeredItem(index = rowIndex + 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { (label, key, icon) ->
                        Card(
                            onClick = { openShortcut(key) },
                            modifier = Modifier.weight(1f).heightIn(min = 118.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                when (key) {
                                    "home_workshop" -> AppIcon(R.drawable.icon_services_workshop, 52.dp)
                                    "home_sessions" -> AppIcon(R.drawable.icon_services_sessions, 52.dp)
                                    "home_ranking" -> AppIcon(R.drawable.icon_services_competition, 52.dp)
                                    "home_store" -> AppIcon(R.drawable.icon_services_store, 52.dp)
                                    "home_identity" -> AppIcon(R.drawable.icon_home_identity, 52.dp)
                                    "home_members" -> AppIcon(R.drawable.icon_home_member_management, 52.dp)
                                    "home_assistant" -> AppIcon(R.drawable.icon_home_assistant, 52.dp)
                                    else -> AppIcon(R.drawable.icon_services_wheel, 52.dp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(label, style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
                }
            }
        }
        Text("چیدمان میانبرها از بخش تنظیمات قابل تغییر است • نسخهٔ ۱.۷", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (action) EconomyDialog(repo, changed) { action = false }
    if (assistantOpen) MentorAssistantDialog(repo) { assistantOpen = false }
}

@Composable
private fun HeroChip(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}

@Composable
private fun MentorAssistantDialog(repo: AppRepository, close: () -> Unit) {
    val report = remember(repo.members.size, repo.history.size, repo.attendance.size, repo.missionCompletions.size) {
        com.kichikan.ak1.domain.assistant.MentorAssistantAnalyzer.analyze(
            members = repo.members.toList(),
            history = repo.history.toList(),
            attendance = repo.attendance.toList(),
            sessions = repo.sessions.toList(),
            missionCompletions = repo.missionCompletions.toList()
        )
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("دستیار مربی") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تحلیل کاملاً آفلاین و بر اساس داده‌های همین حلقه است.")
                Text("موارد نیازمند توجه: ${report.insights.size}")
                Text("اولویت بالا: ${report.highPriorityCount}")
                if (!report.hasAttentionItems) {
                    Text("فعلاً نشانه مشخصی برای پیگیری پیدا نشد.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(report.insights, key = { it.memberId + "-" + it.type + "-" + it.priority }) { insight ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(insight.memberName + " — " + insight.title, style = MaterialTheme.typography.titleMedium)
                                    Text(insight.explanation)
                                    Text("پیشنهاد: " + insight.suggestedAction)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(close) { Text("بستن") } }
    )
}

@Composable
private fun SummaryCard(repo: AppRepository) {
    val metrics = listOf(
        Triple("اعضای حلقه", repo.members.size, "♙"),
        Triple("XP اعضا", repo.members.sumOf { it.economy.xp }, "↗"),
        Triple("امتیاز قابل خرج", repo.members.sumOf { it.economy.spendablePoints }, "✦"),
        Triple("الماس اعضا", repo.members.sumOf { it.economy.diamonds }, "◆")
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("خلاصه حلقه", style = MaterialTheme.typography.titleLarge)
                Text("نمای کلی", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            metrics.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { (label, value, icon) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassIcon(
                                        when (icon) { "♙" -> GlassKind.MEMBERS; "↗" -> GlassKind.XP; "✦" -> GlassKind.POINTS; else -> GlassKind.DIAMOND },
                                        size = 28.dp,
                                        tint = when (icon) { "♙" -> Neon.Violet; "↗" -> Neon.Green; "✦" -> Neon.Gold; else -> Neon.Cyan }
                                    )
                                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                AnimatedCounter(
                                    value = value,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
