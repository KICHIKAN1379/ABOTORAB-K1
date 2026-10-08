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
import com.kichikan.ak1.domain.calendar.BirthdayRules
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.HistoryCategory
import com.kichikan.ak1.domain.service.HistoryService

private enum class Tab { HOME, MEMBERS, WORKSHOP, SESSIONS, RANKING, STORE, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BirthdayReminderScheduler.schedule(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4107)
        }
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
                Tab.STORE -> StoreScreen(repo, padding) { changed() }
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
    var action by remember { mutableStateOf<EconomyOp?>(null) }
    var assistantOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(repo.ring?.ringName ?: "حلقه", style = MaterialTheme.typography.headlineMedium)
        Text("پنل مربی", color = MaterialTheme.colorScheme.primary)
        SummaryCard(repo)
        Button({ action = EconomyOp.XP_ADD }, Modifier.fillMaxWidth()) { Text("ثبت امتیاز / XP") }
        OutlinedButton(openMembers, Modifier.fillMaxWidth()) { Text("مدیریت اعضا") }
        OutlinedButton({ assistantOpen = true }, Modifier.fillMaxWidth()) { Text("دستیار مربی") }
        Text("میانبرها", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ action = EconomyOp.XP_ADD }, Modifier.weight(1f)) { Text("ثبت XP") }
            OutlinedButton({ action = EconomyOp.DIAMONDS_ADD }, Modifier.weight(1f)) { Text("الماس") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ action = EconomyOp.XP_SUB }, Modifier.weight(1f)) { Text("کاهش XP") }
            OutlinedButton({ action = EconomyOp.LEVEL_DOWN }, Modifier.weight(1f)) { Text("کاهش سطح") }
        }
    }
    action?.let { op -> EconomyDialog(repo, changed, op) { action = null } }
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
                            BirthdayRules.parseStored(m.birthDate)?.let {
                                Text("تولد: ${JalaliCalendar.format(it)}", style = MaterialTheme.typography.bodySmall)
                            }
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
    val context = LocalContext.current
    var addMission by remember { mutableStateOf(false) }
    var addWheelItem by remember { mutableStateOf(false) }
    var addShopItem by remember { mutableStateOf(false) }
    var completeMission by remember { mutableStateOf<Mission?>(null) }
    var assetKind by remember { mutableStateOf<String?>(null) }
    var assetMessage by remember { mutableStateOf<String?>(null) }
    val importAsset = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/png"
                require(mime.startsWith("image/")) { "فقط فایل تصویری قابل ثبت است" }
                val options = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: error("فایل خوانده نشد")
                val isFrame = assetKind == "FRAME"
                val required = if (isFrame) 288 else 256
                require(options.outWidth == required && options.outHeight == required) { "ابعاد ${if (isFrame) "قاب" else "آواتار"} باید دقیقاً ${required}×${required} پیکسل باشد" }
                val id = "asset-" + System.currentTimeMillis()
                val ext = when { mime.equals("image/jpeg", true) -> ".jpg"; mime.equals("image/webp", true) -> ".webp"; mime.equals("image/gif", true) -> ".gif"; else -> ".png" }
                val target = java.io.File(context.filesDir, "custom_assets/${id}${ext}")
                target.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                repo.addAsset(CustomAsset(id, if (isFrame) "قاب سفارشی" else "آواتار سفارشی", if (isFrame) AssetType.FRAME else AssetType.AVATAR, target.absolutePath, mime, required, required))
                assetMessage = "ثبت شد: ${target.name}"
                changed()
            } catch (e: Exception) { assetMessage = e.message ?: "ثبت فایل انجام نشد" }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("تراشکاری", style = MaterialTheme.typography.headlineMedium)
        Text("مأموریت، گردونه و ساخت آیتم‌های فروشگاه")
        OutlinedButton({ addMission = true }, Modifier.fillMaxWidth()) { Text("ساخت مأموریت") }
        if (repo.missions.isEmpty()) Text("هنوز مأموریتی تعریف نشده است.") else LazyColumn(Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(repo.missions.filter { it.active }, key = { it.id }) { mission ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(mission.title, style = MaterialTheme.typography.titleMedium)
                        Text("پاداش: XP ${mission.xpReward} • امتیاز ${mission.pointsReward} • الماس ${mission.diamondReward}")
                        Button({ completeMission = mission }, enabled = repo.members.isNotEmpty()) { Text("ثبت انجام مأموریت") }
                    }
                }
            }
        }
        HorizontalDivider()
        Text("فروشگاه و شخصی‌سازی", style = MaterialTheme.typography.titleLarge)
        Text("آواتار: 256×256 • قاب: 288×288 • PNG/JPEG/WebP/GIF")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ assetKind = "AVATAR"; importAsset.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")) }) { Text("افزودن آواتار") }
            OutlinedButton({ assetKind = "FRAME"; importAsset.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/gif")) }) { Text("افزودن قاب") }
        }
        OutlinedButton({ addShopItem = true }, Modifier.fillMaxWidth()) { Text("ساخت آیتم فروشگاه") }
        repo.shop.takeLast(6).forEach { item -> Text("• ${item.name} | ${item.type.name} | ${item.currency.name} ${item.price}") }
        assetMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        HorizontalDivider()
        Text("گردونه", style = MaterialTheme.typography.titleLarge)
        Text("تعداد آیتم‌ها: ${repo.wheel.items.size}")
        OutlinedButton({ addWheelItem = true }, Modifier.fillMaxWidth()) { Text("افزودن آیتم به گردونه") }
        repo.wheel.items.forEach { item -> Text("• " + item.title + " | شانس " + item.weight) }
    }
    if (addMission) AddMissionDialog(repo, changed) { addMission = false }
    if (addWheelItem) AddWheelItemDialog(repo, changed) { addWheelItem = false }
    if (addShopItem) AddShopItemDialog(repo, changed) { addShopItem = false }
    completeMission?.let { mission -> CompleteMissionDialog(repo, mission, changed) { completeMission = null } }
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
    AlertDialog(
        onDismissRequest = close,
        title = { Text("ثبت مأموریت: " + mission.title) },
        text = { LazyColumn { items(repo.members, key = { it.id }) { member ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(member.id in selected, { selected = if (it) selected + member.id else selected - member.id })
                Text(member.name)
            }
        } } },
        confirmButton = { TextButton({
            if (selected.isNotEmpty()) {
                try { repo.completeMission(mission, selected.toList()); changed(); close() } catch (_: IllegalArgumentException) { }
            }
        }) { Text("ثبت") } },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
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
                            Text("زمان: " + JalaliCalendar.formatDateTime(session.startsAt))
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
private fun StoreScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    var selectedMemberId by remember { mutableStateOf(repo.members.firstOrNull()?.id ?: "") }
    var result by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("فروشگاه", style = MaterialTheme.typography.headlineMedium)
        Text("جوایز، آواتار و قاب")
        if (repo.members.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repo.members.take(4).forEach { member ->
                    FilterChip(selected = selectedMemberId == member.id, onClick = { selectedMemberId = member.id }, label = { Text(member.name) })
                }
            }
        }
        if (repo.shop.isEmpty()) {
            Text("هنوز آیتمی در فروشگاه تعریف نشده است.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(repo.shop.filter { it.active }, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text("قیمت: ${item.price} ${item.currency.name}")
                            item.minimumLevel?.let { Text("حداقل سطح: $it") }
                            Button(onClick = {
                                try {
                                    repo.purchaseShopItem(selectedMemberId, item.id)
                                    result = "«${item.name}» ثبت شد."
                                    changed()
                                } catch (e: IllegalArgumentException) {
                                    result = e.message
                                }
                            }, enabled = selectedMemberId.isNotBlank()) { Text("دریافت") }
                        }
                    }
                }
            }
        }
        Text("گردونه", style = MaterialTheme.typography.titleLarge)
        Text("اقلام: ${repo.wheel.items.size} • تکرار بعد از برد: ${if (repo.wheel.allowRepeatAfterWin) "فعال" else "غیرفعال"}")
        Button(onClick = {
            try {
                val winner = repo.spinWheel(selectedMemberId)
                result = winner?.title ?: "گردونه آیتم قابل دریافت ندارد."
                changed()
            } catch (e: IllegalArgumentException) { result = e.message }
        }, enabled = selectedMemberId.isNotBlank() && repo.wheel.items.isNotEmpty()) {
            Text("چرخاندن گردونه")
        }
        result?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
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

@Composable
private fun EconomyDialog(
    repo: AppRepository,
    changed: () -> Unit,
    initialOp: EconomyOp = EconomyOp.XP_ADD,
    close: () -> Unit
) {
    var memberId by remember { mutableStateOf(repo.members.firstOrNull()?.id ?: "") }
    var op by remember { mutableStateOf(initialOp) }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val member = repo.members.firstOrNull { it.id == memberId }

    fun submit() {
        val m = member
        val n = amount.toIntOrNull()
        if (m == null) { error = "ابتدا یک عضو انتخاب کنید"; return }
        if (n == null) { error = if (op.isLevelTarget) "سطح مقصد را وارد کنید" else "مقدار را وارد کنید"; return }
        if (!op.isLevelTarget && n <= 0) { error = "مقدار باید بزرگ‌تر از صفر باشد"; return }
        if (op.isLevelTarget && n >= m.economy.level) {
            error = "سطح مقصد باید کمتر از سطح فعلی (${m.economy.level}) باشد"; return
        }
        if (reason.isBlank()) { error = "نوشتن دلیل اجباری است"; return }
        val why = reason.trim()
        try {
            when (op) {
                EconomyOp.XP_ADD -> repo.recordXp(m.id, n, why, "mentor")
                EconomyOp.XP_SUB -> repo.decreaseXp(m.id, n, why, "mentor")
                EconomyOp.POINTS_ADD -> repo.recordPoints(m.id, n, why, "mentor")
                EconomyOp.POINTS_SUB -> repo.recordPoints(m.id, -n, why, "mentor")
                EconomyOp.DIAMONDS_ADD -> repo.recordDiamonds(m.id, n, why, "mentor")
                EconomyOp.DIAMONDS_SUB -> repo.recordDiamonds(m.id, -n, why, "mentor")
                EconomyOp.LEVEL_DOWN -> repo.decreaseLevel(m.id, n, why, "mentor")
            }
            changed()
            close()
        } catch (e: IllegalArgumentException) {
            error = e.message ?: "عملیات انجام نشد"
        }
    }

    AlertDialog(
        onDismissRequest = close, title = { Text("ثبت رویداد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (repo.members.isEmpty()) Text("ابتدا یک عضو اضافه کنید.")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repo.members.forEach { m ->
                        FilterChip(memberId == m.id, { memberId = m.id; error = null }, label = { Text(m.name) })
                    }
                }
                member?.let { m ->
                    Text(
                        "سطح ${m.economy.level} • XP ${m.economy.xp} • امتیاز ${m.economy.spendablePoints} • الماس ${m.economy.diamonds}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EconomyOp.entries.forEach { o ->
                        FilterChip(op == o, { op = o; error = null }, label = { Text(o.label) })
                    }
                }
                OutlinedTextField(
                    amount, { amount = it.filter(Char::isDigit) },
                    label = { Text(if (op.isLevelTarget) "سطح مقصد" else "مقدار") }, singleLine = true
                )
                OutlinedTextField(reason, { reason = it }, label = { Text("دلیل (اجباری)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton({ submit() }) { Text("ثبت") } },
        dismissButton = { TextButton(close) { Text("انصراف") } }
    )
}

@Composable
private fun HistoryDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    var category by remember { mutableStateOf<HistoryCategory?>(null) }
    val all = repo.history.filter { it.memberId == member.id }
    val years = HistoryService.group(HistoryService.filter(all, category))
    AlertDialog(
        onDismissRequest = close, title = { Text("تاریخچه ${member.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(category == null, { category = null }, label = { Text("همه") })
                    HistoryCategory.entries.forEach { c ->
                        FilterChip(category == c, { category = c }, label = { Text(c.label) })
                    }
                }
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (years.isEmpty()) {
                        item { Text(if (all.isEmpty()) "هنوز رویدادی ثبت نشده است." else "رویدادی با این فیلتر وجود ندارد.") }
                    }
                    years.forEach { y ->
                        item { Text("${y.year}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                        y.months.forEach { mo ->
                            item {
                                Text(
                                    "${JalaliCalendar.MONTH_NAMES[mo.month - 1]} ${y.year} • ${mo.events.size} رویداد",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            // No stable key: event ids can repeat when two events share the same millisecond.
                            items(mo.events) { e ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(e.title)
                                        Text(JalaliCalendar.formatDateTime(e.createdAtEpochMillis), style = MaterialTheme.typography.bodySmall)
                                        e.amount?.let { Text("مقدار: $it") }
                                        e.reason?.let { Text("دلیل: $it") }
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
}
