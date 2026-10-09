package com.kichikan.ak1

import android.os.Bundle
import android.Manifest
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.birthday.BirthdayReminderScheduler
import com.kichikan.ak1.notification.DailyDateNotification
import com.kichikan.ak1.domain.calendar.BirthdayRules
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.HistoryCategory
import com.kichikan.ak1.domain.service.HistoryService

private enum class Tab { HOME, MEMBERS, SERVICES, WORKSHOP, WHEEL, SESSIONS, RANKING, STORE, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BirthdayReminderScheduler.schedule(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4107)
        }
        DailyDateNotification.start(this)
        setContent { AK1Theme { AK1App() } }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4107 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            DailyDateNotification.start(this)
        }
    }
}

@Composable
private fun AK1Theme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFFFD700),
        secondary = androidx.compose.ui.graphics.Color(0xFF38EF7D),
        background = androidx.compose.ui.graphics.Color(0xFF16213E),
        surface = androidx.compose.ui.graphics.Color(0xFF101827)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun AK1App() {
    val context = LocalContext.current
    val repo = remember { AppRepository(context.applicationContext) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    fun changed() { refresh++ }
    if (repo.ring == null) { SetupScreen(repo) { refresh++ }; return }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf(Tab.HOME to "خانه", Tab.MEMBERS to "اعضا", Tab.SERVICES to "خدمات", Tab.SETTINGS to "تنظیمات").forEach { (t, label) ->
                NavigationBarItem(selected = tab == t || (tab in listOf(Tab.WORKSHOP, Tab.SESSIONS, Tab.RANKING, Tab.STORE) && t == Tab.SERVICES),
                    onClick = { tab = t }, icon = { Text(when(t) { Tab.HOME -> "⌂"; Tab.MEMBERS -> "♙"; Tab.SERVICES -> "▦"; else -> "⚙" }) }, label = { Text(label) })
            }
        }
    }) { padding ->
        key(refresh) {
            when (tab) {
                Tab.HOME -> HomeScreen(repo, padding, { tab = Tab.MEMBERS }, { changed() }) { tab = it }
                Tab.MEMBERS -> MembersScreen(repo, padding, { changed() })
                Tab.SERVICES -> ServicesScreen(padding) { tab = it }
                Tab.WORKSHOP -> WorkshopScreen(repo, padding, { changed() })
                Tab.WHEEL -> WheelScreen(repo, padding, { changed() })
                Tab.SESSIONS -> SessionsScreen(padding, repo, { changed() })
                Tab.RANKING -> RankingScreen(repo, padding)
                Tab.STORE -> StoreScreen(repo, padding) { changed() }
                Tab.SETTINGS -> SettingsScreen(repo, padding) { changed() }
            }
        }
    }
}

@Composable
private fun ServicesScreen(padding: PaddingValues, open: (Tab) -> Unit) {
    val services = listOf(
        Triple("تراشکاری", "مدیریت مأموریت‌ها، جوایز و گردونه", Tab.WORKSHOP),
        Triple("جلسات", "جلسه‌ها و حضور و غیاب گروهی", Tab.SESSIONS),
        Triple("رقابت", "رتبه‌بندی اعضا و گروه‌ها", Tab.RANKING),
        Triple("فروشگاه", "آواتار، قاب و جوایز", Tab.STORE)
    )
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("خدمات", style = MaterialTheme.typography.headlineMedium)
        services.forEachIndexed { i, item ->
            Card(onClick = { open(item.third) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(listOf("⚒", "▦", "🏆", "🎁")[i], style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.first, style = MaterialTheme.typography.titleLarge)
                        Text(item.second, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("‹", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(repo: AppRepository, changed: () -> Unit) {
    var ringName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ابوتراب K1", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("راه‌اندازی آفلاین مربی", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Text("در این مرحله هیچ اتصال آنلاین یا حساب ابری فعال نیست.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(ringName, { ringName = it }, label = { Text("نام حلقه") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(username, { username = it }, label = { Text("نام کاربری محلی مربی") }, singleLine = true)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(18.dp))
        Button({
            try {
                repo.createRing(ringName, username)
                changed()
            } catch (e: IllegalArgumentException) {
                error = e.message
            }
        }, enabled = ringName.isNotBlank() && username.isNotBlank()) {
            Text("شروع کار آفلاین")
        }
    }
}

@Composable
private fun HomeScreen(repo: AppRepository, padding: PaddingValues, openMembers: () -> Unit, changed: () -> Unit, open: (Tab) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
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
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(repo.ring?.ringName ?: "حلقه", style = MaterialTheme.typography.headlineMedium)
        Text("پنل مربی", color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("امروز • ${jalali.day} ${JalaliCalendar.MONTH_NAMES[jalali.month - 1]} ${jalali.year}", style = MaterialTheme.typography.titleMedium)
                Text(String.format(java.util.Locale("fa", "IR"), "%02d:%02d", now.hour, now.minute), style = MaterialTheme.typography.headlineMedium)
            }
        }
        SummaryCard(repo)
        shortcuts.forEach { (label, key, icon) ->
            if (prefs.getBoolean(key, key in listOf("home_identity", "home_members", "home_assistant"))) {
                OutlinedButton(onClick = {
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
                }, modifier = Modifier.fillMaxWidth()) { Text("$icon   $label", modifier = Modifier.fillMaxWidth()) }
            }
        }
        Text("نمایش هر میانبر را از تنظیمات انتخاب کن.", style = MaterialTheme.typography.bodySmall)
    }
    if (action) EconomyDialog(repo, changed) { action = false }
    if (assistantOpen) MentorAssistantDialog(repo) { assistantOpen = false }
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("خلاصه حلقه", style = MaterialTheme.typography.titleLarge)
            Text("اعضا: ${repo.members.size}")
            Text("XP کل: ${repo.members.sumOf { it.economy.xp }}")
            Text("امتیاز قابل خرج: ${repo.members.sumOf { it.economy.spendablePoints }}")
            Text("الماس: ${repo.members.sumOf { it.economy.diamonds }}")
        }
    }
}

@Composable
private fun MembersScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var add by remember { mutableStateOf(false) }
    var addGroup by remember { mutableStateOf(false) }
    var actionMember by remember { mutableStateOf<Member?>(null) }
    var selected by remember { mutableStateOf<Member?>(null) }
    var editing by remember { mutableStateOf<Member?>(null) }
    var groupEditing by remember { mutableStateOf<Group?>(null) }
    var scoreGroup by remember { mutableStateOf<Group?>(null) }
    var mapMember by remember { mutableStateOf<Member?>(null) }
    var exportMember by remember { mutableStateOf<Member?>(null) }
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val m = exportMember
        if (uri != null && m != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { out -> buildMemberCard(repo, m).compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, out) } }
                .onSuccess { Toast.makeText(context, "خروجی JPEG آماده شد", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "ذخیره تصویر ناموفق بود", Toast.LENGTH_SHORT).show() }
        }
        exportMember = null
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("مدیریت اعضا و گروه‌ها", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(page == 0, { page = 0 }, label = { Text("اعضا") })
            FilterChip(page == 1, { page = 1 }, label = { Text("گروه‌ها") })
        }
        if (page == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button({ add = true }) { Text("+ عضو") } }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repo.members, key = { it.id }) { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            TextButton(onClick = { actionMember = m }) { Text(m.name, style = MaterialTheme.typography.titleLarge) }
                            Text("سطح ${m.economy.level} • XP ${m.economy.xp} • امتیاز ${m.economy.spendablePoints} • 💎 ${m.economy.diamonds}")
                            repo.groups.firstOrNull { it.id == m.groupId }?.let { Text("گروه: ${it.name}", style = MaterialTheme.typography.bodySmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                OutlinedButton({ actionMember = m }) { Text("گزینه‌ها") }
                                OutlinedButton({ exportMember = m; exporter.launch("${m.name}-AK1.jpg") }) { Text("JPEG") }
                            }
                        }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button({ addGroup = true }) { Text("+ گروه") } }
            if (repo.groups.isEmpty()) Text("هنوز گروهی ساخته نشده است.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repo.groups, key = { it.id }) { g ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(g.name, style = MaterialTheme.typography.titleLarge)
                            Text("سرگروه: ${repo.members.firstOrNull { it.id == g.leaderMemberId }?.name ?: "تعیین نشده"}")
                            Text("اعضا: ${repo.members.count { it.groupId == g.id }}")
                            Text("سطح ${g.economy.level} • XP مستقل ${g.economy.xp} • امتیاز ${g.economy.spendablePoints} • 💎 ${g.economy.diamonds}")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton({ groupEditing = g }) { Text("ویرایش گروه") }
                                Button({ scoreGroup = g }) { Text("تغییر امتیاز") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (add) AddMemberDialog(repo, changed) { add = false }
    if (addGroup) GroupDialog(repo, null, changed) { addGroup = false }
    groupEditing?.let { GroupDialog(repo, it, changed) { groupEditing = null } }
    scoreGroup?.let { GroupScoreDialog(repo, it, changed) { scoreGroup = null } }
    editing?.let { MemberEditDialog(repo, it, changed) { editing = null } }
    selected?.let { HistoryDialog(repo, it) { selected = null } }
    actionMember?.let { m ->
        AlertDialog(
            onDismissRequest = { actionMember = null },
            title = { Text(m.name) },
            text = { Text("انتخاب کن چه کاری می‌خواهی انجام بدهی.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton({ actionMember = null; editing = m }) { Text("ویرایش") }
                    TextButton({ actionMember = null; selected = m }) { Text("گنجینه") }
                    TextButton({ actionMember = null; mapMember = m }) { Text("نقشه کمال") }
                }
            },
            dismissButton = { TextButton({ actionMember = null }) { Text("بستن") } }
        )
    }
    mapMember?.let { m -> GrowthMapDialog(repo, m) { mapMember = null } }
}

@Composable
private fun GrowthMapDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    val memberHistory = repo.history.filter { it.memberId == member.id }
    val memberAttendance = repo.attendance.filter { it.memberId == member.id }
    val presentCount = memberAttendance.count { it.status == AttendanceStatus.PRESENT }
    val completions = repo.missionCompletions.filter { it.memberIds.contains(member.id) }
    val evidence = listOf(
        Triple("شروع مسیر", true, "با عضویت در حلقه، مسیر رشد آغاز می‌شود."),
        Triple("قدم اول", memberHistory.isNotEmpty(), "با ثبت نخستین رویداد در گنجینه باز می‌شود."),
        Triple("پشتکار", completions.size >= 2 || memberHistory.count { it.type == HistoryType.XP_EARNED } >= 5, "با تکمیل ۲ مأموریت یا ثبت ۵ رویداد دریافت XP باز می‌شود."),
        Triple("مسئولیت‌پذیری", memberAttendance.count { it.status != AttendanceStatus.UNMARKED } >= 3, "با ثبت وضعیت در دست‌کم ۳ نوبت حضور و غیاب باز می‌شود."),
        Triple("حضور منظم", presentCount >= 5, "با ثبت ۵ حضور باز می‌شود."),
        Triple("صداقت", memberHistory.any { "صداقت" in it.title || "صداقت" in (it.reason ?: "") }, "وقتی مربی رویدادی درباره صداقت ثبت کند، این نشانه باز می‌شود."),
        Triple("خدمت و اثرگذاری", completions.any { completion -> repo.missions.firstOrNull { it.id == completion.missionId }?.type == MissionType.GROUP } || memberHistory.any { "خدمت" in it.title || "خدمت" in (it.reason ?: "") }, "با تکمیل مأموریت گروهی یا ثبت رویدادی درباره خدمت باز می‌شود."),
        Triple("ثبات قدم", member.economy.level >= 5, "با رسیدن به سطح ۵، نشانه ثبات قدم روشن می‌شود.")
    )
    val unlocked = evidence.count { it.second }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("نقشه کمال — ${member.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مسیر رشد از سوابق واقعی عضو ساخته می‌شود؛ مراحل به‌صورت خودکار باز می‌شوند.")
                Text("پیشرفت مسیر: $unlocked از ${evidence.size}")
                LinearProgressIndicator(progress = unlocked.toFloat() / evidence.size.toFloat(), modifier = Modifier.fillMaxWidth())
                Text("سطح ${member.economy.level} • XP ${member.economy.xp} • امتیاز ${member.economy.spendablePoints} • 💎 ${member.economy.diamonds}")
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(evidence.size) { index ->
                        val item = evidence[index]
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (item.second) "●" else "○", color = if (item.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                                if (index < evidence.lastIndex) Text("│", color = MaterialTheme.colorScheme.outline)
                            }
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.first, style = MaterialTheme.typography.titleMedium)
                                    Text(if (item.second) "به‌دست‌آمده" else "در انتظار", style = MaterialTheme.typography.labelMedium, color = if (item.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.third, style = MaterialTheme.typography.bodySmall)
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

private fun buildMemberCard(repo: AppRepository, member: Member): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(1000, 620, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap); canvas.drawColor(android.graphics.Color.rgb(22, 33, 62))
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 34f }
    val small = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.LTGRAY; textSize = 25f }
    repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath?.let { android.graphics.BitmapFactory.decodeFile(it) }?.let { canvas.drawBitmap(it, null, android.graphics.Rect(390, 50, 610, 270), paint) }
    repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath?.let { android.graphics.BitmapFactory.decodeFile(it) }?.let { canvas.drawBitmap(it, null, android.graphics.Rect(370, 30, 630, 290), paint) }
    canvas.drawText(member.name, 50f, 360f, paint)
    canvas.drawText("سطح ${member.economy.level}    XP ${member.economy.xp}", 50f, 415f, small)
    canvas.drawText("امتیاز ${member.economy.spendablePoints}    الماس ${member.economy.diamonds}", 50f, 455f, small)
    canvas.drawText(repo.ring?.ringName ?: "ابوتراب K1", 50f, 535f, small)
    return bitmap
}@Composable
private fun HistoryDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    val events = repo.history.filter { it.memberId == member.id }.sortedByDescending { it.createdAtEpochMillis }
    var category by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }
    val acquiredIds = (events.mapNotNull { it.metadata["itemId"] } + listOfNotNull(member.avatarItemId, member.frameItemId)).toSet()
    val avatars = repo.shop.filter { it.id in acquiredIds && it.type == ShopItemType.AVATAR }.distinctBy { it.id }
    val frames = repo.shop.filter { it.id in acquiredIds && it.type == ShopItemType.FRAME }.distinctBy { it.id }
    val chosenItems = when (category) { 1 -> avatars; 2 -> frames; else -> emptyList() }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("گنجینه — ${member.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("امتیاز: ${member.economy.spendablePoints}   •   💎 الماس: ${member.economy.diamonds}", style = MaterialTheme.typography.titleMedium)
                Text("سطح ${member.economy.level} • XP ${member.economy.xp}")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = category == 0, onClick = { category = 0 }, label = { Text("سوابق") })
                    FilterChip(selected = category == 1, onClick = { category = 1 }, label = { Text("آواتارها (${avatars.size})") })
                    FilterChip(selected = category == 2, onClick = { category = 2 }, label = { Text("قاب‌ها (${frames.size})") })
                }
                when (category) {
                    0 -> {
                        if (events.isEmpty()) Text("هنوز رویدادی در گنجینه ثبت نشده است.")
                        else LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(events, key = { it.id }) { event ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(event.title, style = MaterialTheme.typography.titleSmall)
                                        Text(JalaliCalendar.formatDateTime(event.createdAtEpochMillis), style = MaterialTheme.typography.bodySmall)
                                        event.reason?.takeIf { it.isNotBlank() }?.let { Text("دلیل: $it") }
                                        Text("نوع: ${event.type.name}" + (event.amount?.let { " • مقدار: $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    1, 2 -> {
                        if (chosenItems.isEmpty()) Text(if (category == 1) "هنوز آواتاری در سوابق این عضو پیدا نشد." else "هنوز قابی در سوابق این عضو پیدا نشد.")
                        else LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(chosenItems, key = { it.id }) { item ->
                                Card(onClick = { selectedItem = item }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                                        Text(if (item.id == member.avatarItemId) "آواتار فعال" else if (item.id == member.frameItemId) "قاب فعال" else "دریافت‌شده")
                                        Text("برای مشاهده روش دریافت لمس کن.", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(close) { Text("بستن") } }
    )
    selectedItem?.let { item ->
        val acquisition = events.firstOrNull { it.metadata["itemId"] == item.id }
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = { Text(item.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (item.type == ShopItemType.AVATAR) "دسته: آواتار" else "دسته: قاب")
                    if (item.description.isNotBlank()) Text(item.description)
                    Text("روش دریافت: " + (acquisition?.reason ?: when {
                        item.methods.contains(AcquisitionMethod.LEVEL_UNLOCK) -> "بازشدن با سطح"
                        item.methods.contains(AcquisitionMethod.WHEEL_ONLY) -> "گردونه"
                        item.methods.contains(AcquisitionMethod.MISSION) -> "مأموریت"
                        item.methods.contains(AcquisitionMethod.EVENT) -> "رویداد"
                        item.methods.contains(AcquisitionMethod.MANUAL) -> "هدیه مربی"
                        else -> "فروشگاه یا تجهیز دستی"
                    }))
                    acquisition?.let { Text("زمان دریافت: ${JalaliCalendar.formatDateTime(it.createdAtEpochMillis)}") }
                }
            },
            confirmButton = { TextButton({ selectedItem = null }) { Text("بازگشت") } }
        )
    }
}

@Composable private fun GroupDialog(repo: AppRepository, group: Group?, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    val initialMembers = remember(group?.id, repo.members.size) {
        (group?.memberIds?.takeIf { it.isNotEmpty() }
            ?: repo.members.filter { it.groupId == group?.id && group != null }.map { it.id }).toSet()
    }
    var memberIds by remember(group?.id, repo.members.size) { mutableStateOf(initialMembers) }
    var leaderId by remember(group?.id, repo.members.size) {
        mutableStateOf(group?.leaderMemberId ?: repo.members.firstOrNull { it.groupId == group?.id && group != null }?.id.orEmpty())
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (group == null) "ساخت گروه" else "ویرایش گروه") },
        text = {
            Column(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام گروه") }, singleLine = true)
                Text("سرگروه")
                if (repo.members.isEmpty()) Text("برای انتخاب سرگروه ابتدا عضو بساز.")
                else LazyColumn(Modifier.heightIn(max = 100.dp)) {
                    items(repo.members, key = { it.id }) { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = leaderId == m.id, onClick = { leaderId = m.id; memberIds = memberIds + m.id })
                            Text(m.name)
                        }
                    }
                }
                Text("اعضای گروه")
                LazyColumn(Modifier.heightIn(max = 180.dp)) {
                    items(repo.members, key = { it.id }) { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = memberIds.contains(m.id), onCheckedChange = { checked ->
                                memberIds = if (checked) memberIds + m.id else memberIds - m.id
                                if (!checked && leaderId == m.id) leaderId = ""
                            })
                            Text(m.name)
                        }
                    }
                }
                Text("امتیاز گروه جدا از امتیاز اعضاست.")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && (repo.members.isEmpty() || (leaderId.isNotBlank() && memberIds.contains(leaderId)))) {
                    val saved = if (group == null) repo.addGroup(name.trim()) else group.copy(name = name.trim())
                    repo.updateGroup(saved.copy(name = name.trim(), leaderMemberId = leaderId.takeIf { it.isNotBlank() }, memberIds = memberIds.toList()))
                    changed()
                    close()
                }
            }, enabled = name.isNotBlank() && (repo.members.isEmpty() || (leaderId.isNotBlank() && memberIds.contains(leaderId)))) { Text("ثبت") }
        },
        dismissButton = {
            Row {
                if (group != null) TextButton({ repo.deleteGroup(group.id); changed(); close() }) { Text("حذف") }
                TextButton(close) { Text("لغو") }
            }
        }
    )
}

@Composable
private fun GroupScoreDialog(repo: AppRepository, group: Group, changed: () -> Unit, close: () -> Unit) {
    var points by remember { mutableStateOf("0") }
    var diamonds by remember { mutableStateOf("0") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("تغییر امتیاز مستقل گروه ${group.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("امتیاز گروه: ${group.economy.spendablePoints} • الماس: ${group.economy.diamonds}")
                OutlinedTextField(points, { points = it.filter { c -> c.isDigit() || c == '-' } }, label = { Text("تغییر امتیاز (+/-)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(diamonds, { diamonds = it.filter { c -> c.isDigit() || c == '-' } }, label = { Text("تغییر الماس (+/-)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(reason, { reason = it }, label = { Text("دلیل تغییر (اجباری)") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    repo.adjustGroupScore(group.id, 0, points.toIntOrNull() ?: 0, diamonds.toIntOrNull() ?: 0, reason)
                    changed()
                    close()
                } catch (e: Exception) { error = e.message ?: "تغییر امتیاز انجام نشد." }
            }, enabled = reason.isNotBlank()) { Text("ثبت تغییر") }
        },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
}
@Composable private fun MemberEditDialog(repo: AppRepository, member: Member, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf(member.name) }; var groupId by remember { mutableStateOf(member.groupId) }; var notes by remember { mutableStateOf(member.privateNotes) }
    AlertDialog(onDismissRequest = close, title = { Text("ویرایش عضو") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true); Text("گروه")
        Row(Modifier.horizontalScroll(rememberScrollState())) { FilterChip(groupId == null, { groupId = null }, label = { Text("بدون گروه") }); repo.groups.forEach { g -> FilterChip(groupId == g.id, { groupId = g.id }, label = { Text(g.name) }) } }
        OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات محرمانه تربیتی") })
    } }, confirmButton = { TextButton({ if (name.isNotBlank()) { repo.updateMember(member.copy(name = name.trim(), groupId = groupId, privateNotes = notes)); changed(); close() } }) { Text("ذخیره") } },
    dismissButton = { Row { TextButton({ repo.deleteMember(member.id); changed(); close() }) { Text("حذف عضو") }; TextButton(close) { Text("لغو") } } })
}
@Composable private fun MemberNotesDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text("توضیحات ${member.name}") }, text = { Text(if (member.privateNotes.isBlank()) "برای این عضو توضیحی ثبت نشده است." else member.privateNotes) }, confirmButton = { TextButton(close) { Text("بستن") } })
}
@Composable
private fun WorkshopScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    val context = LocalContext.current
    var addMission by remember { mutableStateOf(false) }; var editMission by remember { mutableStateOf<Mission?>(null) }; var completeMission by remember { mutableStateOf<Mission?>(null) }; var addWheel by remember { mutableStateOf(false) }; var editWheelConfig by remember { mutableStateOf(false) }; var editWheel by remember { mutableStateOf<WheelItem?>(null) }; var addShop by remember { mutableStateOf(false) }; var editShop by remember { mutableStateOf<ShopItem?>(null) }
    var assetKind by remember { mutableStateOf<String?>(null) }; var assetMessage by remember { mutableStateOf<String?>(null) }
    val importAsset = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) try { val mime=context.contentResolver.getType(uri)?:"image/png"; require(mime.startsWith("image/")){"فقط فایل تصویری قابل ثبت است"}; val o=BitmapFactory.Options().also{it.inJustDecodeBounds=true}; context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,o)}; val frame=assetKind=="FRAME"; val size=if(frame)288 else 256; require(o.outWidth==size&&o.outHeight==size){"ابعاد ${if(frame)"قاب" else "آواتار"} باید ${size}×${size} باشد"}; val id="asset-"+System.currentTimeMillis(); val ext=when{mime.equals("image/jpeg",true)->".jpg";mime.equals("image/webp",true)->".webp";mime.equals("image/gif",true)->".gif";else->".png"}; val target=java.io.File(context.filesDir,"custom_assets/$id$ext");target.parentFile?.mkdirs();context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{out->input.copyTo(out)}};repo.addAsset(CustomAsset(id,if(frame)"قاب سفارشی" else "آواتار سفارشی",if(frame)AssetType.FRAME else AssetType.AVATAR,target.absolutePath,mime,size,size));assetMessage="ثبت شد";changed()}catch(ex:Exception){assetMessage=ex.message} }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("تراشکاری",style=MaterialTheme.typography.headlineMedium)
        Text("ساخت و مدیریت مأموریت، جوایز، گردونه و شخصی‌سازی")
        OutlinedButton({addMission=true},Modifier.fillMaxWidth()){Text("+ مأموریت")}
        repo.missions.forEach{m->Card(Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(8.dp)){Text(m.title,style=MaterialTheme.typography.titleMedium);Text("XP ${m.xpReward} • امتیاز ${m.pointsReward} • 💎 ${m.diamondReward}");Text(if(m.active)"فعال" else "غیرفعال");Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){Button({completeMission=m},enabled=m.active){Text("ثبت انجام")};TextButton({editMission=m}){Text("ویرایش")};TextButton({repo.deleteMission(m.id);changed()}){Text("حذف")}}}}}
        HorizontalDivider(); Text("جوایز / فروشگاه",style=MaterialTheme.typography.titleLarge)
        OutlinedButton({addShop=true},Modifier.fillMaxWidth()){Text("+ ساخت جایزه / آواتار / قاب")}
        repo.shop.forEach{item->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(item.name);Text("${item.type.name} • ${item.price} ${item.currency.name}")};Row{TextButton({editShop=item}){Text("ویرایش")};TextButton({repo.deleteShopItem(item.id);changed()}){Text("حذف")}}}}}
        HorizontalDivider();Text("ساخت شخصی قاب و آواتار",style=MaterialTheme.typography.titleLarge);Text("آواتار 256×256 و قاب 288×288 — PNG/JPEG/WebP/GIF")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton({assetKind="AVATAR";importAsset.launch(arrayOf("image/png","image/jpeg","image/webp","image/gif"))}){Text("+ آواتار")};OutlinedButton({assetKind="FRAME";importAsset.launch(arrayOf("image/png","image/jpeg","image/webp","image/gif"))}){Text("+ قاب")}}
        assetMessage?.let{Text(it)}
        HorizontalDivider();Text("گردونه",style=MaterialTheme.typography.titleLarge);Text("حالت: ${repo.wheel.mode.name} • هزینه امتیاز ${repo.wheel.spinCostPoints} • هزینه الماس ${repo.wheel.spinCostDiamonds}");OutlinedButton({editWheelConfig=true},Modifier.fillMaxWidth()){Text("ویرایش تنظیمات گردونه")};OutlinedButton({addWheel=true},Modifier.fillMaxWidth()){Text("+ آیتم گردونه")};repo.wheel.items.forEach{item->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${item.title} • وزن ${item.weight}");Row{TextButton({editWheel=item}){Text("ویرایش")};TextButton({repo.deleteWheelItem(item.id);changed()}){Text("حذف")}}}}
    }
    if(editWheelConfig) WheelConfigDialog(repo,changed){editWheelConfig=false};if(addMission) MissionEditDialog(repo,null,changed){addMission=false};editMission?.let{MissionEditDialog(repo,it,changed){editMission=null}};completeMission?.let{CompleteMissionDialog(repo,it,changed){completeMission=null}};if(addWheel)WheelEditDialog(repo,null,changed){addWheel=false};editWheel?.let{WheelEditDialog(repo,it,changed){editWheel=null}};if(addShop)AddShopItemDialog(repo,changed){addShop=false};editShop?.let{ShopItemEditDialog(repo,it,changed){editShop=null}}
}@Composable private fun MissionEditDialog(repo: AppRepository, item: Mission?, changed: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(item?.title ?: "") }
    var desc by remember { mutableStateOf(item?.description ?: "") }
    var xp by remember { mutableStateOf((item?.xpReward ?: 0).toString()) }
    var pts by remember { mutableStateOf((item?.pointsReward ?: 0).toString()) }
    var dia by remember { mutableStateOf((item?.diamondReward ?: 0).toString()) }
    var group by remember { mutableStateOf(item?.type == MissionType.GROUP) }
    var active by remember { mutableStateOf(item?.active ?: true) }
    var startAt by remember { mutableStateOf(item?.startAt) }
    var endAt by remember { mutableStateOf(item?.endAt) }
    var error by remember { mutableStateOf<String?>(null) }
    fun pickDate(initial: Long?, isStart: Boolean) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = initial ?: System.currentTimeMillis() }
        android.app.DatePickerDialog(context, { _, year, month, day ->
            val selected = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month)
                set(java.util.Calendar.DAY_OF_MONTH, day)
                set(java.util.Calendar.HOUR_OF_DAY, if (isStart) 0 else 23)
                set(java.util.Calendar.MINUTE, if (isStart) 0 else 59)
                set(java.util.Calendar.SECOND, if (isStart) 0 else 59)
                set(java.util.Calendar.MILLISECOND, if (isStart) 0 else 999)
            }.timeInMillis
            if (isStart) startAt = selected else endAt = selected
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if(item==null) "ساخت مأموریت" else "ویرایش مأموریت") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(title,{title=it},label={Text("عنوان")},singleLine=true)
                OutlinedTextField(desc,{desc=it},label={Text("توضیحات")})
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(group,{group=it}); Text("مأموریت گروهی") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active,{active=it}); Text("مأموریت فعال است") }
                OutlinedTextField(xp,{xp=it.filter(Char::isDigit)},label={Text("پاداش XP")},singleLine=true)
                OutlinedTextField(pts,{pts=it.filter(Char::isDigit)},label={Text("پاداش امتیاز")},singleLine=true)
                OutlinedTextField(dia,{dia=it.filter(Char::isDigit)},label={Text("پاداش الماس")},singleLine=true)
                Text("محدودیت زمانی (اختیاری)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ pickDate(startAt, true) }) { Text("شروع: ${startAt?.let { JalaliCalendar.formatDateTime(it) } ?: "بدون محدودیت"}") }
                    if (startAt != null) TextButton({ startAt = null }) { Text("پاک‌کردن") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ pickDate(endAt, false) }) { Text("پایان: ${endAt?.let { JalaliCalendar.formatDateTime(it) } ?: "بدون محدودیت"}") }
                    if (endAt != null) TextButton({ endAt = null }) { Text("پاک‌کردن") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton({
                try {
                    require(title.isNotBlank()) { "عنوان مأموریت الزامی است" }
                    require((xp.toIntOrNull() ?: 0) >= 0 && (pts.toIntOrNull() ?: 0) >= 0 && (dia.toIntOrNull() ?: 0) >= 0) { "پاداش نمی‌تواند منفی باشد" }
                    require(startAt == null || endAt == null || startAt!! <= endAt!!) { "تاریخ شروع باید قبل از پایان باشد" }
                    val mission = Mission(item?.id ?: "mission-${java.util.UUID.randomUUID()}", title.trim(), desc.trim(), if(group) MissionType.GROUP else MissionType.INDIVIDUAL, xp.toIntOrNull() ?: 0, pts.toIntOrNull() ?: 0, dia.toIntOrNull() ?: 0, active, startAt, endAt)
                    if (item == null) repo.addMission(mission) else repo.updateMission(mission)
                    changed()
                    close()
                } catch (e: Exception) { error = e.message ?: "ذخیره مأموریت ناموفق بود" }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                if(item!=null) TextButton({repo.deleteMission(item.id);changed();close()}) { Text("حذف") }
                TextButton(close) { Text("لغو") }
            }
        }
    )
}
@Composable private fun WheelEditDialog(repo: AppRepository, item: WheelItem?, changed: () -> Unit, close: () -> Unit) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var amount by remember { mutableStateOf((item?.amount ?: 0).toString()) }
    var weight by remember { mutableStateOf((item?.weight ?: 1).toString()) }
    var type by remember { mutableStateOf(item?.type ?: WheelRewardType.REWARD) }
    var text by remember { mutableStateOf(item?.customText ?: "") }
    var shopItemId by remember { mutableStateOf(item?.shopItemId) }
    var active by remember { mutableStateOf(item?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    val compatible = repo.shop.filter { shop ->
        shop.active && when (type) {
            WheelRewardType.AVATAR -> shop.type == ShopItemType.AVATAR
            WheelRewardType.FRAME -> shop.type == ShopItemType.FRAME
            WheelRewardType.REWARD -> shop.type == ShopItemType.REWARD
            else -> false
        }
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if(item==null) "آیتم گردونه" else "ویرایش آیتم گردونه") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(title,{title=it},label={Text("عنوان")},singleLine=true)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    WheelRewardType.values().forEach { candidate ->
                        FilterChip(type==candidate, {
                            type=candidate
                            shopItemId=null
                            error=null
                        }, label={Text(candidate.name)})
                    }
                }
                if (type in listOf(WheelRewardType.AVATAR, WheelRewardType.FRAME, WheelRewardType.REWARD)) {
                    Text("جایزهٔ متصل از فروشگاه")
                    if (compatible.isEmpty()) Text("ابتدا یک آیتم فعال و هم‌نوع در فروشگاه بساز.")
                    compatible.forEach { shop ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(shopItemId == shop.id, { shopItemId = shop.id; if (title.isBlank()) title = shop.name })
                            Text(shop.name + if (shop.stock != null) " • موجودی ${shop.stock}" else "")
                        }
                    }
                } else {
                    OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text("مقدار")},singleLine=true)
                }
                OutlinedTextField(weight,{weight=it.filter(Char::isDigit)},label={Text("وزن/احتمال")},singleLine=true)
                OutlinedTextField(text,{text=it},label={Text("توضیح جایزه")})
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active,{active=it}); Text("فعال") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton({
                try {
                    require(title.isNotBlank()) { "عنوان جایزه الزامی است" }
                    require(weight.toIntOrNull() != null && weight.toInt() > 0) { "وزن باید بیشتر از صفر باشد" }
                    if (type in listOf(WheelRewardType.AVATAR, WheelRewardType.FRAME, WheelRewardType.REWARD)) {
                        require(shopItemId != null && compatible.any { it.id == shopItemId }) { "برای این نوع جایزه، یک آیتم معتبر از فروشگاه انتخاب کن" }
                    } else shopItemId = null
                    val wheelItem = WheelItem(item?.id ?: "wheel-${java.util.UUID.randomUUID()}", title.trim(), type, amount.toIntOrNull(), shopItemId, text.takeIf { it.isNotBlank() }, weight.toInt(), active)
                    if (item == null) repo.setWheel(repo.wheel.copy(items = repo.wheel.items + wheelItem)) else repo.updateWheelItem(wheelItem)
                    changed()
                    close()
                } catch (e: Exception) { error = e.message ?: "ذخیره آیتم گردونه ناموفق بود" }
            }) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                if(item!=null) TextButton({repo.deleteWheelItem(item.id);changed();close()}) { Text("حذف") }
                TextButton(close) { Text("لغو") }
            }
        }
    )
}
@Composable private fun ShopItemEditDialog(repo: AppRepository, item: ShopItem, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf(item.name) };var price by remember { mutableStateOf(item.price.toString()) };var description by remember { mutableStateOf(item.description) };var level by remember { mutableStateOf(item.minimumLevel?.toString()?:"") };var currency by remember { mutableStateOf(item.currency) }
    AlertDialog(onDismissRequest=close,title={Text("ویرایش ${item.name}")},text={Column(verticalArrangement=Arrangement.spacedBy(5.dp)){OutlinedTextField(name,{name=it},label={Text("نام")});OutlinedTextField(description,{description=it},label={Text("توضیحات جایزه")});OutlinedTextField(price,{price=it.filter(Char::isDigit)},label={Text("قیمت")});OutlinedTextField(level,{level=it.filter(Char::isDigit)},label={Text("حداقل سطح")});Row{Currency.values().forEach{cur->FilterChip(currency==cur,{currency=cur},label={Text(cur.name)})}}}},confirmButton={TextButton({if(name.isNotBlank()){repo.updateShopItem(item.copy(name=name.trim(),price=price.toIntOrNull()?:0,description=description.trim(),minimumLevel=level.toIntOrNull(),currency=currency));changed();close()}}){Text("ذخیره")}},dismissButton={Row{TextButton({repo.deleteShopItem(item.id);changed();close()}){Text("حذف")};TextButton(close){Text("لغو")}}})
}
@Composable
private fun AddShopItemDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ShopItemType.AVATAR) }
    var price by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf(Currency.POINTS) }
    var level by remember { mutableStateOf("") }
    var direct by remember { mutableStateOf(true) }
    var levelUnlock by remember { mutableStateOf(false) }
    var wheelOnly by remember { mutableStateOf(false) }
    var event by remember { mutableStateOf(false) }
    var assetId by remember { mutableStateOf<String?>(null) }
    val compatibleAssets = repo.assets.filter { when (type) { ShopItemType.AVATAR -> it.type == AssetType.AVATAR; ShopItemType.FRAME -> it.type == AssetType.FRAME; ShopItemType.REWARD -> it.type == AssetType.REWARD_IMAGE } }
    AlertDialog(onDismissRequest = close, title = { Text("آیتم فروشگاه") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 520.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { ShopItemType.entries.forEach { candidate -> FilterChip(type == candidate, { type = candidate; assetId = null }, label = { Text(candidate.name) }) } }
            OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("قیمت") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Currency.entries.forEach { candidate -> FilterChip(currency == candidate, { currency = candidate }, label = { Text(candidate.name) }) } }
            OutlinedTextField(level, { level = it.filter(Char::isDigit) }, label = { Text("حداقل سطح (اختیاری)") }, singleLine = true)
            Text("شرایط دریافت")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(direct, { direct = !direct }, label = { Text("خرید مستقیم") })
                FilterChip(levelUnlock, { levelUnlock = !levelUnlock }, label = { Text("بازشدن با سطح") })
                FilterChip(wheelOnly, { wheelOnly = !wheelOnly }, label = { Text("فقط گردونه") })
                FilterChip(event, { event = !event }, label = { Text("رویداد") })
            }
            if (compatibleAssets.isNotEmpty()) { Text("تصویر"); Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { compatibleAssets.forEach { asset -> FilterChip(assetId == asset.id, { assetId = asset.id }, label = { Text(asset.name) }) } } }
        }
    },
    confirmButton = { TextButton({ if (name.isNotBlank()) { val methods = buildSet { if (direct) add(AcquisitionMethod.DIRECT_PURCHASE); if (levelUnlock) add(AcquisitionMethod.LEVEL_UNLOCK); if (wheelOnly) add(AcquisitionMethod.WHEEL_ONLY); if (event) add(AcquisitionMethod.EVENT) }; repo.addShopItem(ShopItem("shop-" + System.currentTimeMillis(), name.trim(), type, assetId?.let { id -> repo.assets.firstOrNull { it.id == id }?.path }, price.toIntOrNull() ?: 0, currency, level.toIntOrNull(), methods)); changed(); close() } }) { Text("ثبت") } },
    dismissButton = { TextButton(close) { Text("لغو") } })
}@Composable
private fun AddMissionDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var xp by remember { mutableStateOf("0") }
    var points by remember { mutableStateOf("0") }
    var diamonds by remember { mutableStateOf("0") }
    var group by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("مأموریت جدید") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("توضیحات") })
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(group, { group = it }); Text("مأموریت گروهی") }
            OutlinedTextField(xp, { xp = it.filter(Char::isDigit) }, label = { Text("XP") }, singleLine = true)
            OutlinedTextField(points, { points = it.filter(Char::isDigit) }, label = { Text("امتیاز") }, singleLine = true)
            OutlinedTextField(diamonds, { diamonds = it.filter(Char::isDigit) }, label = { Text("الماس") }, singleLine = true)
        } },
        confirmButton = { TextButton({
            if (title.isNotBlank()) {
                repo.addMission(Mission("mission-" + System.currentTimeMillis(), title.trim(), description.trim(), if (group) MissionType.GROUP else MissionType.INDIVIDUAL, xp.toIntOrNull() ?: 0, points.toIntOrNull() ?: 0, diamonds.toIntOrNull() ?: 0))
                changed(); close()
            }
        }) { Text("ثبت") } },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
}

@Composable
private fun AddWheelItemDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WheelRewardType.CUSTOM) }
    var amount by remember { mutableStateOf("0") }
    var weight by remember { mutableStateOf("1") }
    var customText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("آیتم جدید گردونه") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
            Text("نوع: " + type.name)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WheelRewardType.values().take(4).forEach { t -> FilterChip(type == t, { type = t }, label = { Text(t.name) }) }
            }
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("مقدار") }, singleLine = true)
            OutlinedTextField(weight, { weight = it.filter(Char::isDigit) }, label = { Text("وزن/شانس") }, singleLine = true)
            OutlinedTextField(customText, { customText = it }, label = { Text("توضیح") })
        } },
        confirmButton = { TextButton({
            if (title.isNotBlank()) {
                val item = WheelItem("wheel-" + System.currentTimeMillis(), title.trim(), type, amount.toIntOrNull(), null, customText.trim().takeIf { it.isNotEmpty() }, (weight.toIntOrNull() ?: 1).coerceAtLeast(1), true)
                repo.setWheel(repo.wheel.copy(items = repo.wheel.items + item)); changed(); close()
            }
        }) { Text("ثبت") } },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
}

@Composable
private fun CompleteMissionDialog(repo: AppRepository, mission: Mission, changed: () -> Unit, close: () -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("ثبت مأموریت: " + mission.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 480.dp)) {
                Text(if (mission.type == MissionType.GROUP) "اعضای شرکت‌کننده در مأموریت گروهی را انتخاب کن:" else "اعضای انجام‌دهنده را انتخاب کن:")
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(repo.members, key = { it.id }) { member ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(member.id in selected, { selected = if (it) selected + member.id else selected - member.id })
                            Text(member.name)
                        }
                    }
                }
                OutlinedTextField(reason, { reason = it }, label = { Text("توضیح ثبت (اختیاری)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton({
                try {
                    require(selected.isNotEmpty()) { "حداقل یک عضو را انتخاب کن" }
                    repo.completeMission(mission, selected.toList(), reason.trim().takeIf { it.isNotEmpty() })
                    changed()
                    close()
                } catch (e: Exception) {
                    error = e.message ?: "ثبت مأموریت انجام نشد"
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
}

@Composable
private fun SessionsScreen(padding: PaddingValues, repo: AppRepository, changed: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var add by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Session?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("جلسات", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = page == 0, onClick = { page = 0 }, label = { Text("حضور و غیاب") })
            FilterChip(selected = page == 1, onClick = { page = 1 }, label = { Text("جلسه‌ها") })
        }
        if (page == 0) AttendanceCalendarScreen(repo, changed)
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("جلسات ثبت‌شده")
                Button(onClick = { add = true }, enabled = repo.members.isNotEmpty()) { Text("+ جلسه") }
            }
            Text("ثبت و مدیریت جلسه‌ها مستقل از حضور و غیاب است.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(repo.sessions.sortedByDescending { it.startsAt }, key = { it.id }) { session ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(repo.members.firstOrNull { it.id == session.memberId }?.name ?: "عضو حذف‌شده", style = MaterialTheme.typography.titleSmall)
                            Text(session.title, style = MaterialTheme.typography.titleLarge)
                            if (session.topic.isNotBlank()) Text("موضوع: " + session.topic)
                            Text(JalaliCalendar.formatDateTime(session.startsAt))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                OutlinedButton(onClick = { editing = session }) { Text("ویرایش") }
                                OutlinedButton(onClick = { repo.deleteSession(session.id); changed() }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (add) SessionEditDialog(repo, null, changed) { add = false }
    editing?.let { SessionEditDialog(repo, it, changed) { editing = null } }
}

@Composable
private fun AttendanceCalendarScreen(repo: AppRepository, changed: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var monthOffset by remember { mutableIntStateOf(0) }
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone).plusWeeks(weekOffset.toLong())
    val saturday = today.minusDays(((today.dayOfWeek.value + 1) % 7).toLong())
    val activeDays = remember {
        mutableStateListOf<Int>().apply {
            addAll((prefs.getStringSet("class_weekdays", setOf("6", "1", "3")) ?: setOf("6", "1", "3")).mapNotNull { it.toIntOrNull() })
        }
    }
    val days = (0L..6L).map { saturday.plusDays(it) }.filter { activeDays.contains(it.dayOfWeek.value % 7) }
    val jalaliWeek = JalaliCalendar.fromGregorian(saturday)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.ms-excel")) { uri ->
        if (uri != null) runCatching {
            val raw = buildAttendanceExcel(repo, monthOffset)
            context.contentResolver.openOutputStream(uri)?.use { it.write(raw.toByteArray(Charsets.UTF_8)) }
                ?: throw IllegalStateException("ذخیره فایل ممکن نشد")
        }.onSuccess { Toast.makeText(context, "خروجی ماه شمسی ذخیره شد", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, it.message ?: "ساخت خروجی ناموفق بود", Toast.LENGTH_LONG).show() }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { weekOffset-- }) { Text("‹ هفته قبل") }
            Text("هفته ${jalaliWeek.day} ${JalaliCalendar.MONTH_NAMES[jalaliWeek.month - 1]} ${jalaliWeek.year}")
            TextButton(onClick = { weekOffset++ }) { Text("هفته بعد ›") }
        }
        Text("روزهای کلاس از تنظیمات خوانده می‌شوند. ✓ حضور، م موجه، غ غیرموجه، ت تأخیر، _ ثبت‌نشده", style = MaterialTheme.typography.bodySmall)
        if (days.isEmpty()) Text("برای این هفته روز کلاسی انتخاب نشده است؛ روزها را در تنظیمات مشخص کن.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(repo.members, key = { it.id }) { member ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(member.name, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.forEach { day ->
                                val epoch = day.atStartOfDay(zone).toInstant().toEpochMilli()
                                val current = repo.attendance.firstOrNull { it.memberId == member.id && java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == day }
                                val mark = when (current?.status) {
                                    AttendanceStatus.PRESENT -> "✓"
                                    AttendanceStatus.EXCUSED -> "م"
                                    AttendanceStatus.ABSENT -> "غ"
                                    AttendanceStatus.LATE -> "ت"
                                    else -> "_"
                                }
                                val jd = JalaliCalendar.fromGregorian(day)
                                OutlinedButton(onClick = {
                                    val next = when (current?.status) {
                                        null, AttendanceStatus.UNMARKED -> AttendanceStatus.PRESENT
                                        AttendanceStatus.PRESENT -> AttendanceStatus.EXCUSED
                                        AttendanceStatus.EXCUSED -> AttendanceStatus.ABSENT
                                        AttendanceStatus.ABSENT -> AttendanceStatus.LATE
                                        AttendanceStatus.LATE -> AttendanceStatus.UNMARKED
                                    }
                                    repo.recordWeeklyAttendance(member.id, epoch, next)
                                    changed()
                                }, modifier = Modifier.width(58.dp).height(58.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${jd.day}"); Text(mark) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthOffset-- }) { Text("‹ ماه قبل") }
            val nowJ = JalaliCalendar.fromGregorian(java.time.LocalDate.now(zone))
            val monthIndex = nowJ.month - 1 + monthOffset
            val year = nowJ.year + Math.floorDiv(monthIndex, 12)
            val month = Math.floorMod(monthIndex, 12) + 1
            Text("${JalaliCalendar.MONTH_NAMES[month - 1]} $year")
            TextButton(onClick = { monthOffset++ }) { Text("ماه بعد ›") }
        }
        OutlinedButton(onClick = {
            val nowJ = JalaliCalendar.fromGregorian(java.time.LocalDate.now(zone))
            val index = nowJ.month - 1 + monthOffset
            val year = nowJ.year + Math.floorDiv(index, 12)
            val month = Math.floorMod(index, 12) + 1
            export.launch("حضور-غیاب-$year-$month.xls")
        }, modifier = Modifier.fillMaxWidth()) { Text("خروجی Excel ماه شمسی") }
    }
}

@Composable
private fun AttendanceCalendarDialog(repo: AppRepository, session: Session, changed: () -> Unit, close: () -> Unit) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.ms-excel")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(buildAttendanceExcel(repo).toByteArray()) }
            Toast.makeText(context, "خروجی Excel آماده شد", Toast.LENGTH_SHORT).show()
        }
    }
    val today = java.time.LocalDate.now().plusWeeks(weekOffset.toLong())
    val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    val days = (0L..6L).map { monday.plusDays(it) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("تقویم حضور و غیاب") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ weekOffset-- }) { Text("‹ هفته قبل") }
                    Text("هفته ${monday}")
                    TextButton({ weekOffset++ }) { Text("هفته بعد ›") }
                }
                Text("م = موجه   غ = غیرموجه   _ = ثبت‌نشده   ت = تأخیر", style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.heightIn(max = 520.dp)) {
                    items(repo.members, key = { it.id }) { member ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(6.dp)) {
                                Text(member.name)
                                Row(Modifier.horizontalScroll(rememberScrollState())) {
                                    days.forEach { day ->
                                        val epoch = day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        val current = repo.attendance.firstOrNull {
                                            it.memberId == member.id &&
                                                java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == day
                                        }
                                        val label = when (current?.status) {
                                            AttendanceStatus.EXCUSED -> "م"
                                            AttendanceStatus.ABSENT -> "غ"
                                            AttendanceStatus.LATE -> "ت"
                                            else -> "_"
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val next = when (current?.status) {
                                                    AttendanceStatus.UNMARKED, AttendanceStatus.PRESENT, null -> AttendanceStatus.EXCUSED
                                                    AttendanceStatus.EXCUSED -> AttendanceStatus.ABSENT
                                                    AttendanceStatus.ABSENT -> AttendanceStatus.LATE
                                                    AttendanceStatus.LATE -> AttendanceStatus.UNMARKED
                                                }
                                                repo.recordWeeklyAttendance(member.id, epoch, next)
                                                changed()
                                            },
                                            modifier = Modifier.width(52.dp).height(50.dp)
                                        ) { Text("${day.dayOfMonth}\\n$label") }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button({ export.launch("AK1-attendance-${java.time.YearMonth.now()}.xls") }) { Text("خروجی Excel ماه") }
                    TextButton(close) { Text("بستن") }
                }
            }
        },
        confirmButton = {}
    )
}

private fun buildAttendanceExcel(repo: AppRepository, monthOffset: Int = 0): String {
    val zone = java.time.ZoneId.systemDefault()
    val todayJ = JalaliCalendar.fromGregorian(java.time.LocalDate.now(zone))
    val index = todayJ.month - 1 + monthOffset
    val year = todayJ.year + Math.floorDiv(index, 12)
    val month = Math.floorMod(index, 12) + 1
    val first = JalaliCalendar.toGregorian(com.kichikan.ak1.domain.calendar.JalaliDate(year, month, 1))
    val last = JalaliCalendar.toGregorian(com.kichikan.ak1.domain.calendar.JalaliDate(year, month, JalaliCalendar.daysInMonth(year, month)))
    val sb = StringBuilder("<html><meta charset=\"UTF-8\"><table border=\"1\"><tr><th>عضو</th>")
    var day = first
    while (!day.isAfter(last)) {
        val j = JalaliCalendar.fromGregorian(day)
        sb.append("<th>").append(j.year).append("/").append(j.month.toString().padStart(2, '0')).append("/").append(j.day.toString().padStart(2, '0')).append("</th>")
        day = day.plusDays(1)
    }
    sb.append("</tr>")
    repo.members.forEach { member ->
        sb.append("<tr><td>").append(member.name).append("</td>")
        var date = first
        while (!date.isAfter(last)) {
            val a = repo.attendance.firstOrNull { it.memberId == member.id && java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == date }
            sb.append("<td>").append(when (a?.status) {
                AttendanceStatus.PRESENT -> "حضور"
                AttendanceStatus.EXCUSED -> "موجه"
                AttendanceStatus.ABSENT -> "غیرموجه"
                AttendanceStatus.LATE -> "تأخیر"
                else -> ""
            }).append("</td>")
            date = date.plusDays(1)
        }
        sb.append("</tr>")
    }
    return sb.append("</table></html>").toString()
}
@Composable private fun SessionEditDialog(repo: AppRepository, session: Session?, changed: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var memberId by remember { mutableStateOf(session?.memberId ?: repo.members.firstOrNull()?.id ?: "") }
    var title by remember { mutableStateOf(session?.title ?: "") }
    var topic by remember { mutableStateOf(session?.topic ?: "") }
    var startsAt by remember { mutableLongStateOf(session?.startsAt ?: System.currentTimeMillis()) }
    fun showDatePicker() {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = startsAt }
        android.app.DatePickerDialog(context, { _, year, month, day ->
            val updated = java.util.Calendar.getInstance().apply {
                timeInMillis = startsAt
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month)
                set(java.util.Calendar.DAY_OF_MONTH, day)
            }
            startsAt = updated.timeInMillis
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }
    fun showTimePicker() {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = startsAt }
        android.app.TimePickerDialog(context, { _, hour, minute ->
            val updated = java.util.Calendar.getInstance().apply {
                timeInMillis = startsAt
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            startsAt = updated.timeInMillis
        }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show()
    }
    AlertDialog(onDismissRequest = close, title = { Text(if(session==null)"جلسه جدید" else "ویرایش جلسه") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("عضو")
            Row(Modifier.horizontalScroll(rememberScrollState())) { repo.members.forEach { m -> FilterChip(memberId==m.id,{memberId=m.id},label={Text(m.name)}) } }
            OutlinedTextField(title,{title=it},label={Text("عنوان")},singleLine=true)
            OutlinedTextField(topic,{topic=it},label={Text("موضوع")},singleLine=true)
            Text("تاریخ و ساعت جلسه")
            Text(JalaliCalendar.formatDateTime(startsAt))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker() }) { Text("انتخاب تاریخ") }
                OutlinedButton(onClick = { showTimePicker() }) { Text("انتخاب ساعت") }
            }
            Text("برای ثبت جلسه‌های گذشته هم می‌توانی تاریخ قبلی را انتخاب کنی.", style = MaterialTheme.typography.bodySmall)
        }
    },
    confirmButton={TextButton({
        if(title.isNotBlank()&&memberId.isNotBlank()){
            val saved = Session(session?.id ?: "session-${java.util.UUID.randomUUID()}", memberId, title.trim(), topic.trim(), startsAt, session?.location)
            repo.addSession(saved)
            changed()
            close()
        }
    }){Text("ذخیره")}},
    dismissButton={Row{if(session!=null)TextButton({repo.deleteSession(session.id);changed();close()}){Text("حذف")};TextButton(close){Text("لغو")}}})
}@Composable
private fun RankingScreen(repo: AppRepository, padding: PaddingValues) {
    var mode by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var exportRows by remember { mutableStateOf<List<String>>(emptyList()) }
    var exportTitle by remember { mutableStateOf("رتبه‌بندی") }
    val rankingExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { out -> buildRankingCard(exportTitle, exportRows).compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, out) } }
            .onSuccess { Toast.makeText(context, "تصویر رتبه‌بندی ذخیره شد", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(context, "ذخیره تصویر ناموفق بود: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("رقابت", style = MaterialTheme.typography.headlineMedium)
        Text("سطح‌بندی و رتبه‌بندی فقط بر اساس XP است؛ خرج‌کردن امتیاز سطح را کم نمی‌کند.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(mode == 0, { mode = 0 }, label = { Text("اعضا") }); FilterChip(mode == 1, { mode = 1 }, label = { Text("گروه‌ها") }) }
        OutlinedButton(onClick = {
            if (mode == 0) {
                val rows = repo.members.sortedWith(compareByDescending<Member> { it.economy.level }.thenByDescending { it.economy.xp })
                exportTitle = "رتبه‌بندی اعضا"
                exportRows = rows.mapIndexed { i, m -> "${i + 1}. ${m.name} | سطح ${m.economy.level} | XP ${m.economy.xp} | امتیاز ${m.economy.spendablePoints} | الماس ${m.economy.diamonds}" }
            } else {
                val rows = repo.groups.sortedByDescending { it.economy.spendablePoints }
                exportTitle = "رتبه‌بندی گروه‌ها"
                exportRows = rows.mapIndexed { i, group -> "${i + 1}. ${group.name} | امتیاز ${group.economy.spendablePoints} | الماس ${group.economy.diamonds}" }
            }
            rankingExporter.launch(if (mode == 0) "رتبه‌بندی-اعضا.jpg" else "رتبه‌بندی-گروه‌ها.jpg")
        }, modifier = Modifier.fillMaxWidth()) { Text("خروجی JPEG رتبه‌بندی") }
        if (mode == 0) {
            val ranked = repo.members.sortedWith(compareByDescending<Member> { it.economy.level }.thenByDescending { it.economy.xp })
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(ranked, key = { it.id }) { m -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("#${ranked.indexOf(m)+1} ${m.name}"); Text("سطح ${m.economy.level} • XP ${m.economy.xp}") } } } }
        } else {
            val rankedGroups = repo.groups.sortedByDescending { it.economy.spendablePoints }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rankedGroups, key = { it.id }) { group -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("#${rankedGroups.indexOfFirst { it.id == group.id }+1} ${group.name}"); Text("امتیاز ${group.economy.spendablePoints} • الماس ${group.economy.diamonds}") } } } }
        }
    }
}

private fun buildRankingCard(title: String, rows: List<String>): android.graphics.Bitmap {
    val width = 1200
    val lineHeight = 52
    val height = (170 + rows.size.coerceAtLeast(1) * lineHeight).coerceAtMost(6000)
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(22, 33, 62))
    val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(255, 215, 0); textSize = 42f; textAlign = android.graphics.Paint.Align.RIGHT; typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD) }
    val rowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 27f; textAlign = android.graphics.Paint.Align.RIGHT }
    val dividerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(56, 239, 125); strokeWidth = 2f }
    canvas.drawText(title, width - 50f, 75f, titlePaint)
    canvas.drawText("مجموعه فرهنگی تربیتی ابوتراب علیه السلام", width - 50f, 125f, rowPaint)
    if (rows.isEmpty()) canvas.drawText("هنوز داده‌ای برای رتبه‌بندی وجود ندارد.", width - 50f, 205f, rowPaint)
    rows.forEachIndexed { i, row ->
        val y = 190f + i * lineHeight
        canvas.drawText(row.take(85), width - 50f, y, rowPaint)
        canvas.drawLine(45f, y + 15f, width - 45f, y + 15f, dividerPaint)
    }
    return bitmap
}

@Composable
private fun WheelScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    var memberId by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    val member = repo.members.firstOrNull { it.id == memberId }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("گردونه شانس", style = MaterialTheme.typography.headlineMedium)
        Text("اول عضو را انتخاب کن؛ سپس گردونه را بچرخان. نتیجه و پاداش در سابقه عضو ثبت می‌شود.")
        if (repo.members.isEmpty()) {
            Text("برای استفاده از گردونه ابتدا یک عضو بساز.")
        } else {
            Text("انتخاب عضو", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(repo.members, key = { it.id }) { m ->
                    FilterChip(selected = memberId == m.id, onClick = { memberId = m.id; result = null }, label = { Text(m.name) })
                }
            }
            member?.let { Text("سطح ${it.economy.level} • امتیاز ${it.economy.spendablePoints} • الماس ${it.economy.diamonds}") }
            Text("جوایز فعال: ${repo.wheel.items.count { it.active }} • حالت: ${repo.wheel.mode.name}")
            Button(
                onClick = {
                    result = try {
                        val prize = repo.spinWheel(memberId)
                        if (prize == null) "جایزه قابل انتخابی وجود ندارد؛ از کارگاه، آیتم فعال تعریف کن."
                        else "🎉 نتیجه: ${prize.title}" + (prize.customText?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
                    } catch (e: Exception) {
                        e.message ?: "چرخاندن گردونه انجام نشد."
                    }
                    changed()
                },
                enabled = member != null && repo.wheel.items.any { it.active },
                modifier = Modifier.fillMaxWidth()
            ) { Text("چرخاندن گردونه") }
            result?.let { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) } }
        }
    }
}

@Composable
private fun StoreScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    var selectedMemberId by remember { mutableStateOf(repo.members.firstOrNull()?.id ?: "") }
    var result by remember { mutableStateOf<String?>(null) }
    val member = repo.members.firstOrNull { it.id == selectedMemberId }
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("فروشگاه", style = MaterialTheme.typography.headlineMedium)
        Text("جوایز، آواتار و قاب")
        member?.let { Text("سطح ${it.economy.level} • امتیاز ${it.economy.spendablePoints} • الماس ${it.economy.diamonds}") }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repo.members.forEach { m -> FilterChip(selectedMemberId == m.id, { selectedMemberId = m.id; result = null }, label = { Text(m.name) }) }
        }
        if (repo.shop.isEmpty()) Text("هنوز آیتمی در فروشگاه تعریف نشده است.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
            items(repo.shop.filter { it.active }, key = { it.id }) { item ->
                val reason = member?.let { com.kichikan.ak1.domain.service.ShopService.canAcquire(item, it, now) }
                val owned = member?.let { m -> historyOwned(repo, m.id, item.id) } == true
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(when (item.type) { ShopItemType.AVATAR -> "آواتار"; ShopItemType.FRAME -> "قاب"; ShopItemType.REWARD -> "جایزه" })
                    item.minimumLevel?.let { Text("حداقل سطح: $it") }
                    Text(if (item.methods.isEmpty()) "دریافت مستقیم" else item.methods.joinToString(" • ") { it.name })
                    Text(if (owned) "قبلاً دریافت شده" else if (item.currency == Currency.NONE || item.price == 0) "رایگان" else "قیمت: ${item.price} ${item.currency.name}")
                    Button(onClick = { try { repo.purchaseShopItem(selectedMemberId, item.id); result = "«${item.name}» دریافت و برای عضو فعال شد."; changed() } catch (ex: IllegalArgumentException) { result = ex.message } }, enabled = member != null && reason == null) { Text(if (owned) "استفاده" else "دریافت") }
                    reason?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                } }
            }
        }
        Text("گردونه", style = MaterialTheme.typography.titleLarge)
        Text("اقلام: ${repo.wheel.items.size} • تکرار بعد از برد: ${if (repo.wheel.allowRepeatAfterWin) "فعال" else "غیرفعال"}")
        Button(onClick = { try { val winner = repo.spinWheel(selectedMemberId); result = winner?.title ?: "گردونه آیتم قابل دریافت ندارد."; changed() } catch (ex: IllegalArgumentException) { result = ex.message } }, enabled = member != null && repo.wheel.items.isNotEmpty()) { Text("چرخاندن گردونه") }
        result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

private fun historyOwned(repo: AppRepository, memberId: String, itemId: String): Boolean = repo.history.any { it.memberId == memberId && it.type == HistoryType.REWARD_RECEIVED && it.metadata["itemId"] == itemId }
@Composable
private fun SettingsScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val raw = repo.exportBackup()
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("امکان نوشتن در فایل انتخاب‌شده وجود ندارد")
                output.use { it.write(raw.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "پشتیبان ذخیره شد", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "ذخیره پشتیبان انجام نشد", Toast.LENGTH_LONG).show()
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalArgumentException("خواندن فایل پشتیبان ممکن نشد")
                require(raw.isNotBlank()) { "فایل پشتیبان خالی است" }
                repo.importBackup(raw)
                changed()
                Toast.makeText(context, "پشتیبان بازیابی شد", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "پشتیبان نامعتبر است", Toast.LENGTH_LONG).show()
            }
        }
    }
    var shortcutsOpen by remember { mutableStateOf(false) }
    val shortcutPrefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
    val shortcutLabels = listOf(
        "home_identity" to "شناسنامه", "home_members" to "مدیریت اعضا",
        "home_assistant" to "دستیار مربی", "home_workshop" to "تراشکاری",
        "home_sessions" to "جلسات", "home_ranking" to "رقابت",
        "home_store" to "فروشگاه", "home_wheel" to "گردونه"
    )
    val shortcutStates = remember { mutableStateMapOf<String, Boolean>().apply {
        shortcutLabels.forEach { (key, _) -> put(key, shortcutPrefs.getBoolean(key, key in listOf("home_identity", "home_members", "home_assistant"))) }
    } }
    val weekdayLabels = listOf(6 to "شنبه", 0 to "یکشنبه", 1 to "دوشنبه", 2 to "سه‌شنبه", 3 to "چهارشنبه", 4 to "پنجشنبه", 5 to "جمعه")
    val activeWeekdays = remember { mutableStateListOf<Int>().apply {
        addAll((shortcutPrefs.getStringSet("class_weekdays", setOf("6", "1", "3")) ?: setOf("6", "1", "3")).mapNotNull { it.toIntOrNull() })
    } }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        Text("حلقه: ${repo.ring?.ringName ?: "-"}")
        Text("نام کاربری حلقه: ${repo.ring?.ringUsername ?: "-"}")
        OutlinedButton({ export.launch("AK1-backup.json") }, Modifier.fillMaxWidth()) { Text("خروجی کامل اطلاعات") }
        OutlinedButton({ import.launch(arrayOf("application/json", "text/plain")) }, Modifier.fillMaxWidth()) { Text("ورود اطلاعات پشتیبان") }
        OutlinedButton({ shortcutsOpen = true }, Modifier.fillMaxWidth()) { Text("تنظیم میانبرهای خانه") }
        HorizontalDivider()
        Text("روزهای برگزاری کلاس", style = MaterialTheme.typography.titleMedium)
        Text("هر زمان برنامه تغییر کرد، روزهای کلاس را از اینجا به‌روزرسانی کن. روز حذف‌شده در حضور و غیاب هفتگی نمایش داده نمی‌شود.")
        weekdayLabels.forEach { (day, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = activeWeekdays.contains(day), onCheckedChange = { checked ->
                    if (checked) { if (!activeWeekdays.contains(day)) activeWeekdays.add(day) }
                    else activeWeekdays.remove(day)
                    shortcutPrefs.edit().putStringSet("class_weekdays", activeWeekdays.map { it.toString() }.toSet()).apply()
                })
                Text(label)
            }
        }
        Text("این نسخه کاملاً آفلاین است. پشتیبان شامل حلقه، اعضا، اقتصاد، تاریخچه، فروشگاه، گردونه، مأموریت‌ها، جلسات، حضور و غیاب و دارایی‌های ثبت‌شده است.")
    }
    if (shortcutsOpen) {
        AlertDialog(
            onDismissRequest = { shortcutsOpen = false },
            title = { Text("میانبرهای خانه") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هر میانبری که تیک بخورد در خانه نمایش داده می‌شود.")
                    shortcutLabels.forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = shortcutStates[key] == true, onCheckedChange = { shortcutStates[key] = it })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    shortcutPrefs.edit().also { editor ->
                        shortcutStates.forEach { (key, enabled) -> editor.putBoolean(key, enabled) }
                    }.apply()
                    shortcutsOpen = false
                    changed()
                    Toast.makeText(context, "میانبرهای خانه ذخیره شد", Toast.LENGTH_SHORT).show()
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = {
                    shortcutLabels.forEach { (key, _) ->
                        shortcutStates[key] = shortcutPrefs.getBoolean(key, key in listOf("home_identity", "home_members", "home_assistant"))
                    }
                    shortcutsOpen = false
                }) { Text("انصراف") }
            }
        )
    }
}


/**
 * Shows the typed digits as YYYY/MM/DD. The slashes exist only on screen: the text the user
 * edits (and the state) holds digits only, so nobody has to type "/" or "-".
 */
private object JalaliDateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val shown = StringBuilder()
        digits.forEachIndexed { index, c ->
            if (index == 4 || index == 6) shown.append('/')
            shown.append(c)
        }
        val offsets = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 4 -> offset
                offset <= 6 -> offset + 1
                else -> offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 4 -> offset
                offset <= 7 -> offset - 1
                else -> offset - 2
            }.coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(shown.toString()), offsets)
    }
}

@Composable
private fun AddMemberDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var birthDigits by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val parsedBirth = if (birthDigits.length == 8) JalaliCalendar.parse(birthDigits, allowGregorian = false) else null
    AlertDialog(
        onDismissRequest = close, title = { Text("عضو جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true)
                OutlinedTextField(
                    value = birthDigits,
                    onValueChange = { raw ->
                        // Persian, Arabic and Latin digits all work; everything else is dropped.
                        birthDigits = raw.mapNotNull { it.digitToIntOrNull() }.joinToString("").take(8)
                        error = null
                    },
                    label = { Text("تاریخ تولد شمسی (اختیاری)") },
                    placeholder = { Text("1385/07/15") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = JalaliDateTransformation,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                )
                Text(
                    "فقط عدد بنویسید (مثلاً 13850715)؛ علامت / خودکار اضافه می‌شود. برای یادآوری و پاداش سالانهٔ تولد استفاده می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (birthDigits.length == 8) {
                    if (parsedBirth != null) {
                        Text(
                            "✓ ${parsedBirth.day} ${JalaliCalendar.MONTH_NAMES[parsedBirth.month - 1]} ${parsedBirth.year}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("این تاریخ شمسی وجود ندارد.", color = MaterialTheme.colorScheme.error)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton({
                if (name.isNotBlank()) {
                    try {
                        repo.addMember(name, birthDigits)
                        changed()
                        close()
                    } catch (e: IllegalArgumentException) {
                        error = e.message
                    }
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(close) { Text("انصراف") } }
    )
}

/** Manual mentor adjustments. Every operation requires a reason and is written to the member history. */
private enum class EconomyOp(val label: String, val isLevelTarget: Boolean = false) {
    XP_ADD("XP +"),
    XP_SUB("XP −"),
    POINTS_ADD("امتیاز +"),
    POINTS_SUB("امتیاز −"),
    DIAMONDS_ADD("الماس +"),
    DIAMONDS_SUB("الماس −"),
    LEVEL_DOWN("کاهش سطح", isLevelTarget = true)
}

@Composable private fun EconomyDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }; var op by remember { mutableStateOf<EconomyOp?>(null) }; var memberId by remember { mutableStateOf(repo.members.firstOrNull()?.id?:"") }; var amount by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; val member=repo.members.firstOrNull{it.id==memberId}
    fun submit(){val m=member?:run{error="عضو انتخاب نشده";return};val n=amount.toIntOrNull()?:run{error="مقدار را وارد کنید";return};if(n<=0){error="مقدار باید بیشتر از صفر باشد";return};if(op==EconomyOp.LEVEL_DOWN&&n>=m.economy.level){error="سطح مقصد باید کمتر از سطح فعلی باشد";return};if(reason.isBlank()){error="دلیل اجباری است";return};try{when(op){EconomyOp.XP_ADD->repo.recordXp(m.id,n,reason,"mentor");EconomyOp.XP_SUB->repo.decreaseXp(m.id,n,reason,"mentor");EconomyOp.POINTS_ADD->repo.recordPoints(m.id,n,reason,"mentor");EconomyOp.POINTS_SUB->repo.recordPoints(m.id,-n,reason,"mentor");EconomyOp.DIAMONDS_ADD->repo.recordDiamonds(m.id,n,reason,"mentor");EconomyOp.DIAMONDS_SUB->repo.recordDiamonds(m.id,-n,reason,"mentor");EconomyOp.LEVEL_DOWN->repo.decreaseLevel(m.id,n,reason,"mentor");null->Unit};changed();close()}catch(ex:IllegalArgumentException){error=ex.message}}
    AlertDialog(onDismissRequest=close,title={Text(if(step==0)"شناسنامه" else "ثبت ${op?.label}")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(step==0){Text("ابتدا نوع تغییر را انتخاب کنید");listOf(EconomyOp.POINTS_ADD to "امتیاز",EconomyOp.DIAMONDS_ADD to "الماس",EconomyOp.XP_ADD to "XP",EconomyOp.LEVEL_DOWN to "سطح").forEach{pair->Button({op=pair.first;step=1},Modifier.fillMaxWidth()){Text(pair.second)}}}
            else { Text("عضو");Row(Modifier.horizontalScroll(rememberScrollState())){repo.members.forEach{m->FilterChip(memberId==m.id,{memberId=m.id},label={Text(m.name)})}};Text("نوع عملیات");Row(Modifier.horizontalScroll(rememberScrollState())){op?.let{base->val options=when(base){EconomyOp.POINTS_ADD->listOf(EconomyOp.POINTS_ADD,EconomyOp.POINTS_SUB);EconomyOp.DIAMONDS_ADD->listOf(EconomyOp.DIAMONDS_ADD,EconomyOp.DIAMONDS_SUB);EconomyOp.XP_ADD->listOf(EconomyOp.XP_ADD,EconomyOp.XP_SUB);else->listOf(EconomyOp.LEVEL_DOWN)};options.forEach{o->FilterChip(op==o,{op=o},label={Text(o.label)})}}};OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text(if(op==EconomyOp.LEVEL_DOWN)"سطح مقصد" else "مقدار")},singleLine=true);OutlinedTextField(reason,{reason=it},label={Text("دلیل (اجباری)")},singleLine=true);error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}
        } },confirmButton={if(step==0)TextButton(close){Text("انصراف")}else Row{TextButton({step=0;error=null}){Text("مرحله قبل")};TextButton({submit()}){Text("تأیید")}}},dismissButton={if(step==1)TextButton(close){Text("لغو")}})
}@Composable private fun WheelConfigDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var mode by remember { mutableStateOf(repo.wheel.mode) }; var points by remember { mutableStateOf(repo.wheel.spinCostPoints.toString()) }; var diamonds by remember { mutableStateOf(repo.wheel.spinCostDiamonds.toString()) }; var repeat by remember { mutableStateOf(repo.wheel.allowRepeatAfterWin) }
    AlertDialog(onDismissRequest=close,title={Text("تنظیمات گردونه")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.horizontalScroll(rememberScrollState())){WheelMode.values().forEach{m->FilterChip(mode==m,{mode=m},label={Text(m.name)})}};OutlinedTextField(points,{points=it.filter(Char::isDigit)},label={Text("هزینه امتیاز")});OutlinedTextField(diamonds,{diamonds=it.filter(Char::isDigit)},label={Text("هزینه الماس")});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(repeat,{repeat=it});Text("تکرار جایزه بعد از برد")}}},confirmButton={TextButton({repo.setWheel(repo.wheel.copy(mode=mode,spinCostPoints=points.toIntOrNull()?:0,spinCostDiamonds=diamonds.toIntOrNull()?:0,freeSpin=mode==WheelMode.FREE,allowRepeatAfterWin=repeat));changed();close()}){Text("ذخیره")}},dismissButton={TextButton(close){Text("لغو")}})
}

