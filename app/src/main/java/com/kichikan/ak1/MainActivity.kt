package com.kichikan.ak1

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.model.*

private enum class Tab { HOME, MEMBERS, WORKSHOP, SESSIONS, RANKING, STORE, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AK1Theme { AK1App() } }
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

    if (repo.ring == null) {
        SetupScreen(repo) { refresh++ }
        return
    }

    Scaffold(bottomBar = {
        NavigationBar {
            listOf(
                Tab.HOME to "خانه", Tab.MEMBERS to "اعضا", Tab.WORKSHOP to "تراشکاری",
                Tab.SESSIONS to "جلسات", Tab.RANKING to "رقابت", Tab.STORE to "فروشگاه", Tab.SETTINGS to "تنظیمات"
            ).forEach { (t, label) ->
                NavigationBarItem(
                    selected = tab == t, onClick = { tab = t },
                    icon = { Text(label.take(1)) }, label = { Text(label) }
                )
            }
        }
    }) { padding ->
        key(refresh) {
            when (tab) {
                Tab.HOME -> HomeScreen(repo, padding, { tab = Tab.MEMBERS }, { changed() })
                Tab.MEMBERS -> MembersScreen(repo, padding, { changed() })
                Tab.WORKSHOP -> WorkshopScreen(repo, padding, { changed() })
                Tab.SESSIONS -> SessionsScreen(padding, repo, { changed() })
                Tab.RANKING -> RankingScreen(repo, padding)
                Tab.STORE -> StoreScreen(repo, padding)
                Tab.SETTINGS -> SettingsScreen(repo, padding) { changed() }
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
private fun HomeScreen(repo: AppRepository, padding: PaddingValues, openMembers: () -> Unit, changed: () -> Unit) {
    var action by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(repo.ring?.ringName ?: "حلقه", style = MaterialTheme.typography.headlineMedium)
        Text("پنل مربی", color = MaterialTheme.colorScheme.primary)
        SummaryCard(repo)
        Button({ action = true }, Modifier.fillMaxWidth()) { Text("ثبت امتیاز / XP") }
        OutlinedButton(openMembers, Modifier.fillMaxWidth()) { Text("مدیریت اعضا") }
        Text("میانبرها", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ action = true }, Modifier.weight(1f)) { Text("ثبت XP") }
            OutlinedButton({ action = true }, Modifier.weight(1f)) { Text("الماس") }
        }
    }
    if (action) EconomyDialog(repo, changed) { action = false }
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
    var add by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Member?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("اعضا", style = MaterialTheme.typography.headlineMedium)
            Button({ add = true }) { Text("+ عضو") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(repo.members, key = { it.id }) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(m.name, style = MaterialTheme.typography.titleLarge)
                            Text("سطح ${m.economy.level}  •  XP ${m.economy.xp}")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("🪙 ${m.economy.spendablePoints}")
                            Text("💎 ${m.economy.diamonds}")
                            TextButton({ selected = m }) { Text("تاریخچه") }
                        }
                    }
                }
            }
        }
    }
    if (add) AddMemberDialog(repo, changed) { add = false }
    selected?.let { HistoryDialog(repo, it) { selected = null } }
}

@Composable
private fun WorkshopScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    var economy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تراشکاری", style = MaterialTheme.typography.headlineMedium)
        Text("ابزارهای تربیتی و اقتصادی حلقه")
        listOf("ثبت امتیاز و XP", "ماموریت‌های فردی", "ماموریت‌های گروهی", "دفترچه تربیتی", "توشه کمال", "نقشه کمال").forEach {
            OutlinedButton({ economy = true }, Modifier.fillMaxWidth()) { Text(it) }
        }
    }
    if (economy) EconomyDialog(repo, changed) { economy = false }
}

@Composable
private fun SessionsScreen(padding: PaddingValues, repo: AppRepository, changed: () -> Unit) {
    var addSession by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<Session?>(null) }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("جلسات", style = MaterialTheme.typography.headlineMedium)
            Button({ addSession = true }) { Text("+ جلسه") }
        }

        if (repo.sessions.isEmpty()) {
            Text("هنوز جلسه‌ای ثبت نشده است.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repo.sessions.sortedByDescending { it.startsAt }, key = { it.id }) { session ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(session.title, style = MaterialTheme.typography.titleLarge)
                            if (session.description.isNotBlank()) Text(session.description)
                            session.location?.let { Text("محل: " + it) }
                            Text("زمان: " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(session.startsAt)))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton({ selectedSession = session }) {
                                    Text("حضور و غیاب")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (addSession) AddSessionDialog(repo, changed) { addSession = false }
    selectedSession?.let { session ->
        AttendanceDialog(repo, session, changed) { selectedSession = null }
    }
}

@Composable
private fun RankingScreen(repo: AppRepository, padding: PaddingValues) {
    val ranked = repo.members.sortedWith(compareByDescending<Member> { it.economy.level }.thenByDescending { it.economy.xp })
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("رقابت", style = MaterialTheme.typography.headlineMedium)
        Text("رتبه فقط بر اساس XP و سطح است؛ امتیاز قابل خرج دخالتی ندارد.")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ranked) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${ranked.indexOf(m) + 1}  ${m.name}")
                        Text("سطح ${m.economy.level} • XP ${m.economy.xp}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreScreen(repo: AppRepository, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("فروشگاه", style = MaterialTheme.typography.headlineMedium)
        Text("آواتارها • قاب‌ها • جوایز • گردونه")
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("آواتارها") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("قاب‌ها") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("جوایز") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("گردونه رایگان") }
        Text("گردونه هزینه ندارد و اقلام آن توسط مربی تعریف می‌شوند.")
    }
}

@Composable
private fun SettingsScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(repo.exportBackup().toByteArray()) }
            Toast.makeText(context, "پشتیبان ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!raw.isNullOrBlank()) {
                try {
                    repo.importBackup(raw)
                    changed()
                    Toast.makeText(context, "پشتیبان بازیابی شد", Toast.LENGTH_SHORT).show()
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(context, e.message ?: "پشتیبان نامعتبر است", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        Text("حلقه: ${repo.ring?.ringName ?: "-"}")
        Text("نام کاربری حلقه: ${repo.ring?.ringUsername ?: "-"}")
        OutlinedButton({ export.launch("AK1-backup.json") }, Modifier.fillMaxWidth()) { Text("خروجی کامل اطلاعات") }
        OutlinedButton({ import.launch(arrayOf("application/json", "text/plain")) }, Modifier.fillMaxWidth()) { Text("ورود اطلاعات پشتیبان") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("تنظیم میانبرهای خانه") }
        Text("این نسخه کاملاً آفلاین است. پشتیبان شامل حلقه، اعضا، اقتصاد، تاریخچه، فروشگاه، گردونه، مأموریت‌ها، جلسات، حضور و غیاب و دارایی‌های ثبت‌شده است.")
    }
}


@Composable
private fun AddSessionDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("جلسه جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان جلسه") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("توضیحات") })
                OutlinedTextField(location, { location = it }, label = { Text("محل") }, singleLine = true)
                Text("زمان جلسه فعلاً زمان ثبت است؛ تقویم دقیق را در مرحله بعد اضافه می‌کنیم.")
            }
        },
        confirmButton = {
            TextButton({
                if (title.isNotBlank()) {
                    repo.addSession(title, System.currentTimeMillis(), description, location)
                    changed()
                    close()
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(close) { Text("انصراف") } }
    )
}

@Composable
private fun AttendanceDialog(
    repo: AppRepository,
    session: Session,
    changed: () -> Unit,
    close: () -> Unit
) {
    AlertDialog(
        onDismissRequest = close,
        title = { Text("حضور و غیاب: " + session.title) },
        text = {
            LazyColumn(
                Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(repo.members, key = { it.id }) { member ->
                    val current = repo.attendance.firstOrNull {
                        it.memberId == member.id && it.sessionId == session.id
                    }
                    var status by remember(current?.status) {
                        mutableStateOf(current?.status ?: AttendanceStatus.PRESENT)
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    AttendanceStatus.PRESENT to "حاضر",
                                    AttendanceStatus.ABSENT to "غایب",
                                    AttendanceStatus.LATE to "تاخیر",
                                    AttendanceStatus.EXCUSED to "موجه"
                                ).forEach { (candidate, label) ->
                                    FilterChip(
                                        selected = status == candidate,
                                        onClick = { status = candidate },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Button({
                                repo.recordAttendance(
                                    Attendance(
                                        id = "attendance-" + member.id + "-" + session.id,
                                        memberId = member.id,
                                        sessionId = session.id,
                                        status = status,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                changed()
                            }) {
                                Text("ثبت وضعیت")
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
private fun AddMemberDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = close, title = { Text("عضو جدید") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true) },
        confirmButton = {
            TextButton({ if (name.isNotBlank()) { repo.addMember(name); changed() }; close() }) { Text("ثبت") }
        },
        dismissButton = { TextButton(close) { Text("انصراف") } }
    )
}

@Composable
private fun EconomyDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var memberId by remember { mutableStateOf(repo.members.firstOrNull()?.id ?: "") }
    var type by remember { mutableStateOf("XP") }
    var amount by remember { mutableStateOf("10") }
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = close, title = { Text("ثبت رویداد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repo.members.forEach { m ->
                    FilterChip(memberId == m.id, { memberId = m.id }, label = { Text(m.name) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(type == "XP", { type = "XP" }, label = { Text("XP") })
                    FilterChip(type == "POINTS", { type = "POINTS" }, label = { Text("امتیاز") })
                    FilterChip(type == "DIAMONDS", { type = "DIAMONDS" }, label = { Text("الماس") })
                }
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("مقدار") }, singleLine = true)
                OutlinedTextField(reason, { reason = it }, label = { Text("دلیل (اجباری)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton({
                val n = amount.toIntOrNull() ?: 0
                if (memberId.isNotBlank() && n > 0 && reason.isNotBlank()) {
                    when (type) {
                        "XP" -> repo.recordXp(memberId, n, reason, "mentor")
                        "POINTS" -> repo.recordPoints(memberId, n, reason, "mentor")
                        else -> repo.recordDiamonds(memberId, n, reason, "mentor")
                    }
                    changed()
                }
                close()
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(close) { Text("انصراف") } }
    )
}

@Composable
private fun HistoryDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    val events = repo.history.filter { it.memberId == member.id }.sortedByDescending { it.createdAtEpochMillis }
    AlertDialog(
        onDismissRequest = close, title = { Text("تاریخچه ${member.name}") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) item { Text("هنوز رویدادی ثبت نشده است.") }
                items(events) { e ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(e.title)
                            e.amount?.let { Text("مقدار: ${it}") }
                            e.reason?.let { Text("دلیل: ${it}") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(close) { Text("بستن") } }
    )
}
