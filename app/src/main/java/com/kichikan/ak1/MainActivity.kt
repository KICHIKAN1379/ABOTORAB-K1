package com.kichikan.ak1

import android.os.Bundle
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
    val repo = remember {
        AppRepository(applicationContext).also {
            if (it.ring == null) it.createRing("حلقه من", "mentor")
            if (it.members.isEmpty()) { it.addMember("محمدعلی")
            it.addMember("علی")
            it.addMember("رضا")
            it.addMember("محمد")
        }
    }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    fun changed() { refresh++ }

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
                Tab.SESSIONS -> SessionsScreen(padding)
                Tab.RANKING -> RankingScreen(repo, padding)
                Tab.STORE -> StoreScreen(repo, padding)
                Tab.SETTINGS -> SettingsScreen(repo, padding)
            }
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
private fun SessionsScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("جلسات", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("حضور و غیاب") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("جلسه جدید") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("اردو") }
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
private fun SettingsScreen(repo: AppRepository, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        Text("حلقه: ${repo.ring?.ringName ?: "-"}")
        Text("نام کاربری حلقه: ${repo.ring?.ringUsername ?: "-"}")
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("خروجی کامل اطلاعات") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("ورود اطلاعات پشتیبان") }
        OutlinedButton({}, Modifier.fillMaxWidth()) { Text("تنظیم میانبرهای خانه") }
    }
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
