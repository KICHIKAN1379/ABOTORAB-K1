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
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.R
import com.kichikan.ak1.ui.theme.AK1Theme
import com.kichikan.ak1.ui.theme.AppIcon
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.AvatarRender
import com.kichikan.ak1.ui.Exports
import com.kichikan.ak1.ui.GrowthMapDialog
import com.kichikan.ak1.ui.HubEntry
import com.kichikan.ak1.ui.HubScreen
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.ui.PageHeader
import com.kichikan.ak1.ui.RankRow
import com.kichikan.ak1.ui.SpinWheelPage
import com.kichikan.ak1.ui.TreasureDialog
import com.kichikan.ak1.ui.theme.GlassIcon
import com.kichikan.ak1.ui.theme.GlassKind
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.birthday.BirthdayReminderScheduler
import com.kichikan.ak1.notification.DailyDateNotification
import com.kichikan.ak1.domain.calendar.BirthdayRules
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.domain.service.HistoryCategory
import com.kichikan.ak1.domain.service.HistoryService

private enum class Tab {
    HOME, MEMBERS, SERVICES, WORKSHOP, WHEEL, SESSIONS, RANKING, STORE, SETTINGS,
    WORKSHOP_MISSIONS, WORKSHOP_PRIZES, WORKSHOP_ASSETS, WORKSHOP_WHEEL,
    RANK_MEMBERS, RANK_GROUPS,
    STORE_AVATARS, STORE_FRAMES, STORE_REWARDS,
    SESSIONS_LIST, SESSIONS_ATTENDANCE
}

private fun parentOf(tab: Tab): Tab = when (tab) {
    Tab.WORKSHOP_MISSIONS, Tab.WORKSHOP_PRIZES, Tab.WORKSHOP_ASSETS, Tab.WORKSHOP_WHEEL -> Tab.WORKSHOP
    Tab.RANK_MEMBERS, Tab.RANK_GROUPS -> Tab.RANKING
    Tab.STORE_AVATARS, Tab.STORE_FRAMES, Tab.STORE_REWARDS -> Tab.STORE
    Tab.SESSIONS_LIST, Tab.SESSIONS_ATTENDANCE -> Tab.SESSIONS
    Tab.WORKSHOP, Tab.RANKING, Tab.STORE, Tab.SESSIONS, Tab.WHEEL -> Tab.SERVICES
    else -> Tab.HOME
}

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
private fun AK1App() {
    val context = LocalContext.current
    val repo = remember { AppRepository(context.applicationContext) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    fun changed() { refresh++ }
    if (repo.ring == null) { SetupScreen(repo) { refresh++ }; return }
    androidx.activity.compose.BackHandler(enabled = tab != Tab.HOME) { tab = parentOf(tab) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, bottomBar = {
        NavigationBar(containerColor = Neon.Navy.copy(alpha = 0.88f)) {
            listOf(Tab.HOME to "خانه", Tab.MEMBERS to "اعضا", Tab.SERVICES to "خدمات", Tab.SETTINGS to "تنظیمات").forEach { (t, label) ->
                val selected = tab == t || (t == Tab.SERVICES && tab !in listOf(Tab.HOME, Tab.MEMBERS, Tab.SETTINGS))
                NavigationBarItem(selected = selected,
                    onClick = { tab = t },
                    icon = {
                        AppIcon(
                            when (t) {
                                Tab.HOME -> R.drawable.icon_nav_home
                                Tab.MEMBERS -> R.drawable.icon_nav_members
                                Tab.SERVICES -> R.drawable.icon_nav_services
                                else -> R.drawable.icon_nav_settings
                            },
                            28.dp
                        )
                    },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Neon.Cyan,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Neon.Cyan.copy(alpha = 0.16f)
                    ))
            }
        }
    }) { padding ->
        key(refresh) {
            when (tab) {
                Tab.HOME -> HomeScreen(repo, padding, { tab = Tab.MEMBERS }, { changed() }) { tab = it }
                Tab.MEMBERS -> MembersScreen(repo, padding, { changed() })
                Tab.SERVICES -> ServicesScreen(padding) { tab = it }
                Tab.WORKSHOP -> WorkshopHub(padding, repo, { changed() }, { tab = it }) { tab = Tab.SERVICES }
                Tab.RANKING -> RankingHub(padding, { tab = it }) { tab = Tab.SERVICES }
                Tab.STORE -> StoreHub(padding, { tab = it }) { tab = Tab.SERVICES }
                Tab.SESSIONS -> SessionsHub(padding, { tab = it }) { tab = Tab.SERVICES }
                Tab.WORKSHOP_MISSIONS -> WorkshopScreen(repo, padding, { changed() }, 0) { tab = Tab.WORKSHOP }
                Tab.WORKSHOP_PRIZES -> WorkshopScreen(repo, padding, { changed() }, 1) { tab = Tab.WORKSHOP }
                Tab.WORKSHOP_ASSETS -> WorkshopScreen(repo, padding, { changed() }, 2) { tab = Tab.WORKSHOP }
                Tab.WORKSHOP_WHEEL -> WorkshopScreen(repo, padding, { changed() }, 3) { tab = Tab.WORKSHOP }
                Tab.WHEEL -> SpinWheelPage(repo, padding)
                Tab.RANK_MEMBERS -> RankingScreen(repo, padding, false) { tab = Tab.RANKING }
                Tab.RANK_GROUPS -> RankingScreen(repo, padding, true) { tab = Tab.RANKING }
                Tab.STORE_AVATARS -> StoreScreen(repo, padding, ShopItemType.AVATAR, { changed() }) { tab = Tab.STORE }
                Tab.STORE_FRAMES -> StoreScreen(repo, padding, ShopItemType.FRAME, { changed() }) { tab = Tab.STORE }
                Tab.STORE_REWARDS -> StoreScreen(repo, padding, ShopItemType.REWARD, { changed() }) { tab = Tab.STORE }
                Tab.SESSIONS_LIST -> SessionsScreen(padding, repo, { changed() }, false) { tab = Tab.SESSIONS }
                Tab.SESSIONS_ATTENDANCE -> SessionsScreen(padding, repo, { changed() }, true) { tab = Tab.SESSIONS }
                Tab.SETTINGS -> SettingsScreen(repo, padding) { changed() }
            }
        }
    }
}

@Composable
private fun ServicesScreen(padding: PaddingValues, open: (Tab) -> Unit) {
    HubScreen(
        padding, "خدمات", "ابزارهای مدیریت حلقه در یک‌جا",
        listOf(
            HubEntry("تراشکاری", "مأموریت‌ها، جوایز، آواتار و قاب", Neon.Cyan, { AppIcon(R.drawable.icon_services_workshop, 62.dp) }) { open(Tab.WORKSHOP) },
            HubEntry("جلسات", "فهرست جلسه‌ها و حضور و غیاب", Neon.Coral, { AppIcon(R.drawable.icon_services_sessions, 62.dp) }) { open(Tab.SESSIONS) },
            HubEntry("رقابت", "رتبه‌بندی اعضا و گروه‌ها", Neon.Violet, { AppIcon(R.drawable.icon_services_competition, 62.dp) }) { open(Tab.RANKING) },
            HubEntry("فروشگاه", "آواتارها، قاب‌ها و جوایز", Neon.Gold, { AppIcon(R.drawable.icon_services_store, 62.dp) }) { open(Tab.STORE) },
            HubEntry("گردونه", "گردونه‌ی شانس و جایزه", Neon.Sky, { AppIcon(R.drawable.icon_services_wheel, 62.dp) }) { open(Tab.WHEEL) }
        )
    )
}

@Composable
private fun WorkshopHub(
    padding: PaddingValues,
    repo: AppRepository,
    changed: () -> Unit,
    open: (Tab) -> Unit,
    back: () -> Unit
) {
    var identityOpen by remember { mutableStateOf(false) }
    HubScreen(
        padding, "تراشکاری", "ساخت و مدیریت مأموریت، جوایز و شخصی‌سازی",
        listOf(
            HubEntry("شناسنامه", "ثبت امتیاز، الماس، XP و سطح", Neon.Green, { AppIcon(R.drawable.icon_workshop_identity, 62.dp) }) { identityOpen = true },
            HubEntry("مأموریت‌ها", "ساخت و ثبت انجام مأموریت", Neon.Cyan, { AppIcon(R.drawable.icon_workshop_missions, 62.dp) }) { open(Tab.WORKSHOP_MISSIONS) },
            HubEntry("جوایز فروشگاه", "ساخت و ویرایش آیتم‌ها", Neon.Gold, { AppIcon(R.drawable.icon_workshop_store_rewards, 62.dp) }) { open(Tab.WORKSHOP_PRIZES) },
            HubEntry("آواتار و قاب", "افزودن تصویر سفارشی", Neon.Violet, { AppIcon(R.drawable.icon_workshop_avatar_frame, 62.dp) }) { open(Tab.WORKSHOP_ASSETS) },
            HubEntry("تنظیم گردونه", "جوایز، وزن و هزینه", Neon.Sky, { AppIcon(R.drawable.icon_workshop_wheel_settings, 62.dp) }) { open(Tab.WORKSHOP_WHEEL) }
        ),
        back
    )
    if (identityOpen) EconomyDialog(repo, changed) { identityOpen = false }
}

@Composable
private fun RankingHub(padding: PaddingValues, open: (Tab) -> Unit, back: () -> Unit) {
    HubScreen(
        padding, "رقابت", "رتبه‌ی اعضا بر اساس سطح و XP؛ رتبه‌ی گروه‌ها با امتیاز مستقل گروه",
        listOf(
            HubEntry("رتبه اعضا", "با آواتار و قاب هر عضو", Neon.Gold, { AppIcon(R.drawable.icon_competition_member_ranking, 62.dp) }) { open(Tab.RANK_MEMBERS) },
            HubEntry("رتبه گروه‌ها", "امتیاز و الماس گروه", Neon.Violet, { AppIcon(R.drawable.icon_competition_group_ranking, 62.dp) }) { open(Tab.RANK_GROUPS) }
        ),
        back
    )
}

@Composable
private fun StoreHub(padding: PaddingValues, open: (Tab) -> Unit, back: () -> Unit) {
    HubScreen(
        padding, "فروشگاه", "آواتار، قاب و جایزه",
        listOf(
            HubEntry("آواتارها", "دریافت و فعال‌سازی آواتار", Neon.Cyan, { AppIcon(R.drawable.icon_store_avatars, 62.dp) }) { open(Tab.STORE_AVATARS) },
            HubEntry("قاب‌ها", "دریافت و فعال‌سازی قاب", Neon.Violet, { AppIcon(R.drawable.icon_store_frames, 62.dp) }) { open(Tab.STORE_FRAMES) },
            HubEntry("جوایز", "جوایز قابل دریافت", Neon.Gold, { AppIcon(R.drawable.icon_store_rewards, 62.dp) }) { open(Tab.STORE_REWARDS) }
        ),
        back
    )
}

@Composable
private fun SessionsHub(padding: PaddingValues, open: (Tab) -> Unit, back: () -> Unit) {
    HubScreen(
        padding, "جلسات", "جلسه‌ها و حضور و غیاب مستقل از هم‌اند",
        listOf(
            HubEntry("فهرست جلسات", "ثبت و مشاهده جلسه‌ها", Neon.Coral, { AppIcon(R.drawable.icon_sessions_list, 62.dp) }) { open(Tab.SESSIONS_LIST) },
            HubEntry("حضور و غیاب", "تقویم هفتگی و ماهانه", Neon.Green, { AppIcon(R.drawable.icon_sessions_attendance, 62.dp) }) { open(Tab.SESSIONS_ATTENDANCE) }
        ),
        back
    )
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    if (ringPhoto != null) {
                        Image(bitmap = ringPhoto, contentDescription = "تصویر پروفایل حلقه", modifier = Modifier.fillMaxSize())
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("K1", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("سلام، ${repo.ring?.ringName ?: "مربی"}", style = MaterialTheme.typography.headlineSmall)
                    Text("پنل مربی • مدیریت حلقه", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "●  حالت آفلاین",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${jalali.day}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("امروز", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${JalaliCalendar.MONTH_NAMES[jalali.month - 1]} ${jalali.year}", style = MaterialTheme.typography.titleLarge)
                    Text("روز تازه، فرصت تازه برای رشد", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            visibleShortcuts.chunked(2).forEach { rowItems ->
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
        Text("چیدمان میانبرها از بخش تنظیمات قابل تغییر است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val metrics = listOf(
        Triple("اعضای حلقه", repo.members.size.toString(), "♙"),
        Triple("XP اعضا", repo.members.sumOf { it.economy.xp }.toString(), "↗"),
        Triple("امتیاز قابل خرج", repo.members.sumOf { it.economy.spendablePoints }.toString(), "✦"),
        Triple("الماس اعضا", repo.members.sumOf { it.economy.diamonds }.toString(), "◆")
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
                                Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
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
            runCatching { context.contentResolver.openOutputStream(uri)?.use { out -> Exports.memberCard(repo, m).compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, out) } }
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
                    val avatarPath = repo.shop.firstOrNull { it.id == m.avatarItemId }?.imagePath
                    val framePath = repo.shop.firstOrNull { it.id == m.frameItemId }?.imagePath
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MemberAvatar(avatarPath, framePath, 84.dp, m.name)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { actionMember = m }, contentPadding = PaddingValues(0.dp)) {
                                        Text(m.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    repo.groups.firstOrNull { it.id == m.groupId }?.let {
                                        Text(it.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    m.responsibility.takeIf { it.isNotBlank() }?.let {
                                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text("مسئولیت: $it", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(
                                    Triple("سطح", m.economy.level.toString(), MaterialTheme.colorScheme.primaryContainer),
                                    Triple("XP", m.economy.xp.toString(), MaterialTheme.colorScheme.secondaryContainer),
                                    Triple("امتیاز", m.economy.spendablePoints.toString(), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f)),
                                    Triple("💎", m.economy.diamonds.toString(), MaterialTheme.colorScheme.surfaceVariant)
                                ).forEach { (label, value, tint) ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                        color = tint
                                    ) {
                                        Column(Modifier.padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { actionMember = m }, modifier = Modifier.weight(1f)) { Text("گزینه‌های عضو") }
                                OutlinedButton(onClick = { exportMember = m; exporter.launch("${m.name}-AK1.jpg") }, modifier = Modifier.weight(1f)) { Text("خروجی کارت") }
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
                            Text("امتیاز گروه ${g.economy.spendablePoints} • 💎 الماس ${g.economy.diamonds}")
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
    selected?.let { TreasureDialog(repo, it.id, changed) { selected = null } }
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
                    repo.adjustGroupScore(group.id, points.toIntOrNull() ?: 0, diamonds.toIntOrNull() ?: 0, reason)
                    changed()
                    close()
                } catch (e: Exception) { error = e.message ?: "تغییر امتیاز انجام نشد." }
            }, enabled = reason.isNotBlank()) { Text("ثبت تغییر") }
        },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
}
@Composable private fun MemberEditDialog(repo: AppRepository, member: Member, changed: () -> Unit, close: () -> Unit) {
    var name by remember { mutableStateOf(member.name) }; var groupId by remember { mutableStateOf(member.groupId) }; var notes by remember { mutableStateOf(member.privateNotes) }; var responsibility by remember { mutableStateOf(member.responsibility) }
    AlertDialog(onDismissRequest = close, title = { Text("ویرایش عضو") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true); Text("گروه")
        Row(Modifier.horizontalScroll(rememberScrollState())) { FilterChip(groupId == null, { groupId = null }, label = { Text("بدون گروه") }); repo.groups.forEach { g -> FilterChip(groupId == g.id, { groupId = g.id }, label = { Text(g.name) }) } }
        OutlinedTextField(responsibility, { responsibility = it }, label = { Text("مسئولیت در حلقه (نمایش در اعضا، نه رتبه‌بندی)") }, singleLine = true)
        OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات محرمانه تربیتی") })
    } }, confirmButton = { TextButton({ if (name.isNotBlank()) { repo.updateMember(member.copy(name = name.trim(), groupId = groupId, privateNotes = notes, responsibility = responsibility.trim())); changed(); close() } }) { Text("ذخیره") } },
    dismissButton = { Row { TextButton({ repo.deleteMember(member.id); changed(); close() }) { Text("حذف عضو") }; TextButton(close) { Text("لغو") } } })
}
@Composable private fun MemberNotesDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text("توضیحات ${member.name}") }, text = { Text(if (member.privateNotes.isBlank()) "برای این عضو توضیحی ثبت نشده است." else member.privateNotes) }, confirmButton = { TextButton(close) { Text("بستن") } })
}
@Composable
private fun WorkshopScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit, section: Int, back: () -> Unit) {
    val context = LocalContext.current
    var addMission by remember { mutableStateOf(false) }; var editMission by remember { mutableStateOf<Mission?>(null) }; var completeMission by remember { mutableStateOf<Mission?>(null) }; var addWheel by remember { mutableStateOf(false) }; var editWheelConfig by remember { mutableStateOf(false) }; var editWheel by remember { mutableStateOf<WheelItem?>(null) }; var addShop by remember { mutableStateOf(false) }; var editShop by remember { mutableStateOf<ShopItem?>(null) }
    var assetKind by remember { mutableStateOf<String?>(null) }; var assetMessage by remember { mutableStateOf<String?>(null) }
    val importAsset = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) try { val mime=context.contentResolver.getType(uri)?:"image/png"; require(mime.startsWith("image/")){"فقط فایل تصویری قابل ثبت است"}; val o=BitmapFactory.Options().also{it.inJustDecodeBounds=true}; context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,o)}; val frame=assetKind=="FRAME"; val size=if(frame)288 else 256; require(o.outWidth==size&&o.outHeight==size){"ابعاد ${if(frame)"قاب" else "آواتار"} باید ${size}×${size} باشد"}; val id="asset-"+System.currentTimeMillis(); val ext=when{mime.equals("image/jpeg",true)->".jpg";mime.equals("image/webp",true)->".webp";mime.equals("image/gif",true)->".gif";else->".png"}; val target=java.io.File(context.filesDir,"custom_assets/$id$ext");target.parentFile?.mkdirs();context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{out->input.copyTo(out)}};val assetName=if(frame)"قاب سفارشی" else "آواتار سفارشی";repo.addAsset(CustomAsset(id,assetName,if(frame)AssetType.FRAME else AssetType.AVATAR,target.absolutePath,mime,size,size));repo.addShopItem(ShopItem(id=id,name=assetName,type=if(frame)ShopItemType.FRAME else ShopItemType.AVATAR,imagePath=target.absolutePath,currency=Currency.NONE,methods=setOf(AcquisitionMethod.MANUAL),description="دارایی سفارشی مربی؛ برای دریافت باید مربی آن را هدیه کند."));assetMessage="ثبت شد و به فهرست آواتار/قاب اضافه شد";changed()}catch(ex:Exception){assetMessage=ex.message} }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        PageHeader(when (section) { 0 -> "مأموریت‌ها"; 1 -> "جوایز فروشگاه"; 2 -> "آواتار و قاب"; else -> "تنظیم گردونه" }, back, "ساخت و مدیریت در تراشکاری")
        if(section==0){
        OutlinedButton({addMission=true},Modifier.fillMaxWidth()){Text("+ مأموریت")}
        repo.missions.forEach{m->Card(Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(8.dp)){Text(m.title,style=MaterialTheme.typography.titleMedium);Text("XP ${m.xpReward} • امتیاز ${m.pointsReward} • 💎 ${m.diamondReward}");Text(if(m.active)"فعال" else "غیرفعال");Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){Button({completeMission=m},enabled=m.active){Text("ثبت انجام")};TextButton({editMission=m}){Text("ویرایش")};TextButton({repo.deleteMission(m.id);changed()}){Text("حذف")}}}}}
        }
        if(section==1){
        Text("جوایز / فروشگاه",style=MaterialTheme.typography.titleLarge)
        OutlinedButton({addShop=true},Modifier.fillMaxWidth()){Text("+ ساخت جایزه / آواتار / قاب")}
        repo.shop.forEach{item->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(item.name);Text("${item.type.name} • ${item.price} ${item.currency.name}")};Row{TextButton({editShop=item}){Text("ویرایش")};TextButton({repo.deleteShopItem(item.id);changed()}){Text("حذف")}}}}}
        }
        if(section==2){
        Text("ساخت شخصی قاب و آواتار",style=MaterialTheme.typography.titleLarge);Text("آواتار 256×256 و قاب 288×288 — PNG/JPEG/WebP/GIF")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton({assetKind="AVATAR";importAsset.launch(arrayOf("image/png","image/jpeg","image/webp","image/gif"))}){Text("+ آواتار")};OutlinedButton({assetKind="FRAME";importAsset.launch(arrayOf("image/png","image/jpeg","image/webp","image/gif"))}){Text("+ قاب")}}
        assetMessage?.let{Text(it)}
        }
        if(section==3){
        Text("گردونه",style=MaterialTheme.typography.titleLarge);Text("حالت: ${repo.wheel.mode.name} • هزینه امتیاز ${repo.wheel.spinCostPoints} • هزینه الماس ${repo.wheel.spinCostDiamonds}");OutlinedButton({editWheelConfig=true},Modifier.fillMaxWidth()){Text("ویرایش تنظیمات گردونه")};OutlinedButton({addWheel=true},Modifier.fillMaxWidth()){Text("+ آیتم گردونه")};repo.wheel.items.forEach{item->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${item.title} • وزن ${item.weight}");Row{TextButton({editWheel=item}){Text("ویرایش")};TextButton({repo.deleteWheelItem(item.id);changed()}){Text("حذف")}}}}
        }
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
@Composable
private fun ItemPreview(repo: AppRepository, type: ShopItemType, path: String) {
    // Frames are always previewed with an avatar inside, avatars inside a frame: nothing sticks out.
    val sampleAvatar = repo.shop.firstOrNull { it.type == ShopItemType.AVATAR && it.imagePath != null }?.imagePath
    val sampleFrame = repo.shop.firstOrNull { it.type == ShopItemType.FRAME && it.imagePath != null }?.imagePath
    when (type) {
        ShopItemType.AVATAR -> MemberAvatar(path, sampleFrame, 112.dp)
        ShopItemType.FRAME -> MemberAvatar(sampleAvatar, path, 112.dp)
        ShopItemType.REWARD -> {
            val bitmap = remember(path) { AvatarRender.decodeSampled(path, 384)?.asImageBitmap() }
            bitmap?.let { Image(bitmap = it, contentDescription = "تصویر آیتم فروشگاه", modifier = Modifier.size(112.dp)) }
        }
    }
}

@Composable
private fun ShopItemEditDialog(repo: AppRepository, item: ShopItem, changed: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(item.name) }
    var price by remember { mutableStateOf(item.price.toString()) }
    var description by remember { mutableStateOf(item.description) }
    var level by remember { mutableStateOf(item.minimumLevel?.toString() ?: "") }
    var currency by remember { mutableStateOf(item.currency) }
    var imagePath by remember { mutableStateOf(item.imagePath) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { copyShopImage(context, uri) }
            .onSuccess { imagePath = it }
            .onFailure { Toast.makeText(context, it.message ?: "انتخاب تصویر ناموفق بود", Toast.LENGTH_LONG).show() }
    }
    AlertDialog(onDismissRequest = close, title = { Text("ویرایش ${item.name}") }, text = {
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (imagePath == null) "افزودن تصویر" else "تغییر تصویر") }
            imagePath?.let { raw -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ItemPreview(repo, item.type, raw) } }
            OutlinedTextField(description, { description = it }, label = { Text("توضیحات جایزه") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("قیمت") }, singleLine = true)
            OutlinedTextField(level, { level = it.filter(Char::isDigit) }, label = { Text("حداقل سطح") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Currency.entries.forEach { cur -> FilterChip(currency == cur, { currency = cur }, label = { Text(when(cur) { Currency.POINTS -> "امتیاز"; Currency.DIAMONDS -> "الماس"; Currency.NONE -> "رایگان" }) }) } }
        }
    }, confirmButton = { TextButton({ if (name.isNotBlank()) {
        repo.updateShopItem(item.copy(name = name.trim(), imagePath = imagePath, price = price.toIntOrNull() ?: 0, description = description.trim(), minimumLevel = level.toIntOrNull(), currency = currency))
        changed(); close()
    } }) { Text("ذخیره") } }, dismissButton = { Row { TextButton({ repo.deleteShopItem(item.id); changed(); close() }) { Text("حذف") }; TextButton(close) { Text("لغو") } } })
}
@Composable
private fun AddShopItemDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
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
    var imagePath by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { copyShopImage(context, uri) }
            .onSuccess { imagePath = it }
            .onFailure { Toast.makeText(context, it.message ?: "انتخاب تصویر ناموفق بود", Toast.LENGTH_LONG).show() }
    }
    val compatibleAssets = repo.assets.filter { when (type) { ShopItemType.AVATAR -> it.type == AssetType.AVATAR; ShopItemType.FRAME -> it.type == AssetType.FRAME; ShopItemType.REWARD -> it.type == AssetType.REWARD_IMAGE } }
    AlertDialog(onDismissRequest = close, title = { Text("آیتم فروشگاه") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { ShopItemType.entries.forEach { candidate -> FilterChip(type == candidate, { type = candidate; assetId = null }, label = { Text(when(candidate){ ShopItemType.AVATAR -> "آواتار"; ShopItemType.FRAME -> "قاب"; ShopItemType.REWARD -> "جایزه" }) }) } }
            OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (imagePath == null) "انتخاب تصویر برای نمایش در فروشگاه" else "تغییر تصویر انتخاب‌شده") }
            imagePath?.let { raw -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ItemPreview(repo, type, raw) } }
            if (compatibleAssets.isNotEmpty()) {
                Text("یا از دارایی‌های ثبت‌شده انتخاب کن")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { compatibleAssets.forEach { asset ->
                    val thumb = remember(asset.path) { AvatarRender.decodeSampled(asset.path, 256)?.asImageBitmap() }
                    Card(onClick = { assetId = asset.id; imagePath = null }) { Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) { thumb?.let { Image(bitmap = it, contentDescription = asset.name, modifier = Modifier.size(54.dp)) }; Text(asset.name, style = MaterialTheme.typography.labelSmall) } }
                } }
            }
            OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("قیمت") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Currency.entries.forEach { candidate -> FilterChip(currency == candidate, { currency = candidate }, label = { Text(when(candidate){ Currency.POINTS -> "امتیاز"; Currency.DIAMONDS -> "الماس"; Currency.NONE -> "رایگان" }) }) } }
            OutlinedTextField(level, { level = it.filter(Char::isDigit) }, label = { Text("حداقل سطح (اختیاری)") }, singleLine = true)
            Text("شرایط دریافت")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(direct, { direct = !direct }, label = { Text("خرید مستقیم") })
                FilterChip(levelUnlock, { levelUnlock = !levelUnlock }, label = { Text("بازشدن با سطح") })
                FilterChip(wheelOnly, { wheelOnly = !wheelOnly }, label = { Text("فقط گردونه") })
                FilterChip(event, { event = !event }, label = { Text("رویداد") })
            }
        }
    }, confirmButton = { TextButton({ if (name.isNotBlank()) {
        val methods = buildSet { if (direct) add(AcquisitionMethod.DIRECT_PURCHASE); if (levelUnlock) add(AcquisitionMethod.LEVEL_UNLOCK); if (wheelOnly) add(AcquisitionMethod.WHEEL_ONLY); if (event) add(AcquisitionMethod.EVENT) }
        val selectedImage = imagePath ?: assetId?.let { id -> repo.assets.firstOrNull { it.id == id }?.path }
        repo.addShopItem(ShopItem("shop-" + System.currentTimeMillis(), name.trim(), type, selectedImage, price.toIntOrNull() ?: 0, currency, level.toIntOrNull(), methods))
        changed(); close()
    } }) { Text("ثبت") } }, dismissButton = { TextButton(close) { Text("لغو") } })
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
private fun SessionsScreen(padding: PaddingValues, repo: AppRepository, changed: () -> Unit, attendance: Boolean, back: () -> Unit) {
    var add by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Session?>(null) }
    var detail by remember { mutableStateOf<Session?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader(if (attendance) "حضور و غیاب" else "فهرست جلسات", back,
            if (attendance) "تقویم هفتگی و ماهانه" else "برای دیدن جزئیات، روی جلسه بزن")
        if (attendance) AttendanceCalendarScreen(repo, changed)
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${repo.sessions.size} جلسه ثبت‌شده", style = MaterialTheme.typography.labelLarge)
                Button(onClick = { add = true }, enabled = repo.members.isNotEmpty()) { Text("+ جلسه") }
            }
            if (repo.sessions.isEmpty()) Text("هنوز جلسه‌ای ثبت نشده است.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(repo.sessions.sortedByDescending { it.startsAt }, key = { it.id }) { session ->
                    Card(onClick = { detail = session }, modifier = Modifier.fillMaxWidth(), accent = Neon.Coral) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Only the essentials here: topic, member, date and time. The rest lives in the detail view.
                            Text(session.title, style = MaterialTheme.typography.titleLarge)
                            Text(repo.members.firstOrNull { it.id == session.memberId }?.name ?: "عضو حذف‌شده", style = MaterialTheme.typography.titleSmall, color = Neon.Cyan)
                            Text(JalaliCalendar.formatDateTime(session.startsAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if (add) SessionEditDialog(repo, null, changed) { add = false }
    editing?.let { SessionEditDialog(repo, it, changed) { editing = null } }
    detail?.let { session ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(session.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عضو: " + (repo.members.firstOrNull { it.id == session.memberId }?.name ?: "عضو حذف‌شده"))
                    Text("زمان: " + JalaliCalendar.formatDateTime(session.startsAt))
                    session.location?.takeIf { it.isNotBlank() }?.let { Text("مکان: $it") }
                    Text(if (session.topic.isBlank()) "توضیحاتی ثبت نشده است." else "توضیحات:\n" + session.topic)
                }
            },
            confirmButton = { TextButton({ val s0 = session; detail = null; editing = s0 }) { Text("ویرایش") } },
            dismissButton = {
                Row {
                    TextButton({ repo.deleteSession(session.id); detail = null; changed() }) { Text("حذف") }
                    TextButton({ detail = null }) { Text("بستن") }
                }
            }
        )
    }
}

@Composable
private fun AttendanceCalendarScreen(repo: AppRepository, changed: () -> Unit) {
    val context = LocalContext.current
    var attendanceEditTarget by remember { mutableStateOf<Pair<Member, java.time.LocalDate>?>(null) }
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
    val rawSchedule = prefs.getString("class_schedule_periods", "[]") ?: "[]"
    val schedulePeriods: List<Triple<java.time.LocalDate, java.time.LocalDate?, Set<Int>>> = remember(rawSchedule) {
        runCatching {
            val array = org.json.JSONArray(rawSchedule)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                val dayArray = item.getJSONArray("days")
                val periodDays = (0 until dayArray.length()).map { dayArray.getInt(it) }.toSet()
                val endText = item.optString("end")
                Triple(java.time.LocalDate.parse(item.getString("start")),
                    if (endText.isBlank()) null else java.time.LocalDate.parse(endText), periodDays)
            }
        }.getOrDefault(emptyList())
    }
    val scheduledDays = (0L..6L).map { saturday.plusDays(it) }.filter { day ->
        if (schedulePeriods.isEmpty()) activeDays.contains(day.dayOfWeek.value % 7)
        else schedulePeriods.lastOrNull { period -> !day.isBefore(period.first) && (period.second == null || !day.isAfter(period.second)) }
            ?.third?.contains(day.dayOfWeek.value % 7) == true
    }
    var closedDates by remember { mutableStateOf(prefs.getStringSet("closed_class_dates", emptySet()) ?: emptySet()) }
    val days = scheduledDays.filterNot { it.toString() in closedDates }
    val jalaliWeek = JalaliCalendar.fromGregorian(saturday)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
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
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("وضعیت حضور هر روز", style = MaterialTheme.typography.titleSmall)
                Text("ح = حاضر  •  م = غیبت موجه  •  غ = غیبت غیرموجه", style = MaterialTheme.typography.bodySmall)
                Text("ت = تأخیر  •  — = بدون ثبت", style = MaterialTheme.typography.bodySmall)
                Text("با لمس خانه هر روز، وضعیت همان عضو تغییر می‌کند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (scheduledDays.isNotEmpty()) {
            Text("برای تعطیلی موردی، روز را لمس کن؛ روز تعطیل از جدول حضور و غیاب کنار گذاشته می‌شود.", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                scheduledDays.forEach { day ->
                    val dateKey = day.toString()
                    val isClosed = dateKey in closedDates
                    val j = JalaliCalendar.fromGregorian(day)
                    val weekday = listOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")[day.dayOfWeek.value % 7]
                    FilterChip(selected = isClosed, onClick = {
                        closedDates = if (isClosed) closedDates - dateKey else closedDates + dateKey
                        prefs.edit().putStringSet("closed_class_dates", closedDates).apply()
                    }, label = { Text("$weekday ${j.day}" + if (isClosed) " • تعطیل" else "") })
                }
            }
        }
        if (scheduledDays.isEmpty()) Text("برای این هفته روز کلاسی تنظیم نشده است؛ روزها را در تنظیمات مشخص کن.")
        else if (days.isEmpty()) Text("تمام روزهای برنامه‌ریزی‌شده این هفته تعطیل شده‌اند.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(repo.members, key = { it.id }) { member ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(member.name, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.forEach { day ->
                                val epoch = day.atStartOfDay(zone).toInstant().toEpochMilli()
                                val current = repo.attendance.firstOrNull { it.memberId == member.id && it.sessionId == null && java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == day }
                                val mark = when (current?.status) {
                                    AttendanceStatus.PRESENT -> "ح"
                                    AttendanceStatus.EXCUSED -> "م"
                                    AttendanceStatus.ABSENT -> "غ"
                                    AttendanceStatus.LATE -> "ت"
                                    else -> "—"
                                }
                                val statusLabel = when (current?.status) {
                                    AttendanceStatus.PRESENT -> "حاضر"
                                    AttendanceStatus.EXCUSED -> "موجه"
                                    AttendanceStatus.ABSENT -> "غایب"
                                    AttendanceStatus.LATE -> "تأخیر"
                                    else -> "بدون ثبت"
                                }
                                val jd = JalaliCalendar.fromGregorian(day)
                                OutlinedButton(
                                    onClick = { attendanceEditTarget = member to day },
                                    modifier = Modifier.width(84.dp).height(76.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = when (current?.status) {
                                            AttendanceStatus.PRESENT -> androidx.compose.ui.graphics.Color(0xFF3CAEA3).copy(alpha = 0.28f)
                                            AttendanceStatus.EXCUSED -> androidx.compose.ui.graphics.Color(0xFFFEC856).copy(alpha = 0.28f)
                                            AttendanceStatus.ABSENT -> androidx.compose.ui.graphics.Color(0xFFFF8A80).copy(alpha = 0.28f)
                                            AttendanceStatus.LATE -> androidx.compose.ui.graphics.Color(0xFFB197FF).copy(alpha = 0.28f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text("${jd.day}", style = MaterialTheme.typography.labelLarge)
                                        Text(mark, style = MaterialTheme.typography.titleMedium, color = if (current?.status == null || current.status == AttendanceStatus.UNMARKED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                                        Text(statusLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
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
            export.launch("حضور-غیاب-$year-$month.csv")
        }, modifier = Modifier.fillMaxWidth()) { Text("خروجی ماه شمسی (CSV سازگار با Excel)") }
    }
    attendanceEditTarget?.let { target ->
        val (member, day) = target
        val current = repo.attendance.firstOrNull {
            it.memberId == member.id && it.sessionId == null &&
                java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == day
        }
        val options = listOf(
            AttendanceStatus.PRESENT to "حاضر • ح",
            AttendanceStatus.EXCUSED to "غیبت موجه • م",
            AttendanceStatus.ABSENT to "غیبت غیرموجه • غ",
            AttendanceStatus.LATE to "تأخیر • ت",
            AttendanceStatus.UNMARKED to "بدون ثبت • —"
        )
        val jalaliDate = JalaliCalendar.fromGregorian(day)
        AlertDialog(
            onDismissRequest = { attendanceEditTarget = null },
            title = { Text("حضور و غیاب ${member.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${jalaliDate.day} ${JalaliCalendar.MONTH_NAMES[jalaliDate.month - 1]} ${jalaliDate.year}")
                    options.forEach { (status, label) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = current?.status == status || (current == null && status == AttendanceStatus.UNMARKED),
                                onClick = {
                                    val epoch = day.atStartOfDay(zone).toInstant().toEpochMilli()
                                    repo.recordWeeklyAttendance(member.id, epoch, status)
                                    changed()
                                    attendanceEditTarget = null
                                }
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ attendanceEditTarget = null }) { Text("بستن") } }
        )
    }
}

@Composable
private fun AttendanceCalendarDialog(repo: AppRepository, session: Session, changed: () -> Unit, close: () -> Unit) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
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
                    Button({ export.launch("AK1-attendance-${java.time.YearMonth.now()}.csv") }) { Text("خروجی ماه (CSV سازگار با Excel)") }
                    TextButton(close) { Text("بستن") }
                }
            }
        },
        confirmButton = {}
    )
}

private fun buildAttendanceExcel(repo: AppRepository, monthOffset: Int = 0): String {
    // CSV is intentionally used instead of HTML content disguised as .xls.
    // UTF-8 BOM makes Persian names and headers display correctly in desktop Excel.
    val zone = java.time.ZoneId.systemDefault()
    val todayJ = JalaliCalendar.fromGregorian(java.time.LocalDate.now(zone))
    val index = todayJ.month - 1 + monthOffset
    val year = todayJ.year + Math.floorDiv(index, 12)
    val month = Math.floorMod(index, 12) + 1
    val first = JalaliCalendar.toGregorian(com.kichikan.ak1.domain.calendar.JalaliDate(year, month, 1))
    val last = JalaliCalendar.toGregorian(com.kichikan.ak1.domain.calendar.JalaliDate(year, month, JalaliCalendar.daysInMonth(year, month)))
    fun csvCell(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
    val rows = mutableListOf<String>()
    val headers = mutableListOf("عضو")
    var day = first
    while (!day.isAfter(last)) {
        val j = JalaliCalendar.fromGregorian(day)
        headers += "${j.year}/${j.month.toString().padStart(2, '0')}/${j.day.toString().padStart(2, '0')}"
        day = day.plusDays(1)
    }
    rows += headers.joinToString(",") { csvCell(it) }
    repo.members.forEach { member ->
        val cells = mutableListOf(member.name)
        var date = first
        while (!date.isAfter(last)) {
            val attendance = repo.attendance.firstOrNull {
                it.memberId == member.id && it.sessionId == null &&
                    java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == date
            }
            cells += when (attendance?.status) {
                AttendanceStatus.PRESENT -> "حضور"
                AttendanceStatus.EXCUSED -> "موجه"
                AttendanceStatus.ABSENT -> "غیرموجه"
                AttendanceStatus.LATE -> "تأخیر"
                else -> ""
            }
            date = date.plusDays(1)
        }
        rows += cells.joinToString(",") { csvCell(it) }
    }
    return "\uFEFF" + rows.joinToString("\r\n")
}

@Composable private fun SessionEditDialog(repo: AppRepository, session: Session?, changed: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var memberId by remember { mutableStateOf(session?.memberId ?: repo.members.firstOrNull()?.id ?: "") }
    var topic by remember { mutableStateOf(session?.title ?: "") }
    var description by remember { mutableStateOf(session?.topic ?: "") }
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
            OutlinedTextField(topic,{topic=it},label={Text("موضوع جلسه")},singleLine=true)
            OutlinedTextField(description,{description=it},label={Text("توضیحات جلسه")},minLines=3,maxLines=5)
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
        if(topic.isNotBlank()&&memberId.isNotBlank()){
            val saved = Session(session?.id ?: "session-${java.util.UUID.randomUUID()}", memberId, topic.trim(), description.trim(), startsAt, session?.location)
            repo.addSession(saved)
            changed()
            close()
        }
    }){Text("ذخیره")}},
    dismissButton={Row{if(session!=null)TextButton({repo.deleteSession(session.id);changed();close()}){Text("حذف")};TextButton(close){Text("لغو")}}})
}@Composable
private fun RankingScreen(repo: AppRepository, padding: PaddingValues, groups: Boolean, back: () -> Unit) {
    val context = LocalContext.current
    val avatarOf = { m: Member -> repo.shop.firstOrNull { it.id == m.avatarItemId }?.imagePath }
    val frameOf = { m: Member -> repo.shop.firstOrNull { it.id == m.frameItemId }?.imagePath }
    val rankedMembers = repo.members.sortedWith(compareByDescending<Member> { it.economy.level }.thenByDescending { it.economy.xp })
    val rankedGroups = repo.groups.sortedByDescending { it.economy.spendablePoints }
    val title = if (groups) "رتبه‌بندی گروه‌ها" else "رتبه‌بندی اعضا"
    val rankingExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri != null) {
            val rows = if (groups) rankedGroups.map { g -> RankRow(g.name, "امتیاز ${g.economy.spendablePoints} • الماس ${g.economy.diamonds}", letter = g.name) }
            else rankedMembers.map { m -> RankRow(m.name, "سطح ${m.economy.level} • XP ${m.economy.xp} • امتیاز ${m.economy.spendablePoints} • الماس ${m.economy.diamonds}", avatarOf(m), frameOf(m), m.name) }
            runCatching { context.contentResolver.openOutputStream(uri)?.use { out -> Exports.rankingCard(title, rows).compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, out) } }
                .onSuccess { Toast.makeText(context, "تصویر رتبه‌بندی ذخیره شد", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "ذخیره تصویر ناموفق بود: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader(title, back, if (groups) "فقط با امتیاز و الماس مستقل گروه محاسبه می‌شود." else "بر اساس سطح و XP؛ خروجی تصویر، آواتار و قاب هر عضو را هم دارد.")
        Button(onClick = { rankingExporter.launch(if (groups) "رتبه‌بندی-گروه‌ها.jpg" else "رتبه‌بندی-اعضا.jpg") }, modifier = Modifier.fillMaxWidth()) { Text("خروجی JPEG رتبه‌بندی") }
        if (!groups) {
            if (rankedMembers.isEmpty()) Text("هنوز عضوی ثبت نشده است.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(rankedMembers, key = { _, m -> m.id }) { index, m ->
                    Card(Modifier.fillMaxWidth(), accent = if (index < 3) Neon.Gold else null) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${index + 1}", style = MaterialTheme.typography.headlineSmall, color = if (index < 3) Neon.Gold else Neon.Cyan)
                            MemberAvatar(avatarOf(m), frameOf(m), 56.dp, m.name)
                            Column(Modifier.weight(1f)) {
                                Text(m.name, style = MaterialTheme.typography.titleMedium)
                                Text("سطح ${m.economy.level} • XP ${m.economy.xp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            if (rankedGroups.isEmpty()) Text("هنوز گروهی ساخته نشده است.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(rankedGroups, key = { _, g -> g.id }) { index, g ->
                    Card(Modifier.fillMaxWidth(), accent = if (index < 3) Neon.Gold else null) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${index + 1}", style = MaterialTheme.typography.headlineSmall, color = if (index < 3) Neon.Gold else Neon.Cyan)
                            Column(Modifier.weight(1f)) {
                                Text(g.name, style = MaterialTheme.typography.titleMedium)
                                Text("امتیاز ${g.economy.spendablePoints} • الماس ${g.economy.diamonds}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private var lastStoreMemberId: String = ""

@Composable
private fun StoreScreen(repo: AppRepository, padding: PaddingValues, filter: ShopItemType, changed: () -> Unit, back: () -> Unit) {
    var selectedMemberId by remember { mutableStateOf(lastStoreMemberId.takeIf { id -> repo.members.any { it.id == id } } ?: repo.members.firstOrNull()?.id ?: "") }
    var result by remember { mutableStateOf<String?>(null) }
    var manualGrantItem by remember { mutableStateOf<ShopItem?>(null) }
    val member = repo.members.firstOrNull { it.id == selectedMemberId }
    val now = System.currentTimeMillis()
    val memberAvatarPath = member?.let { m -> repo.shop.firstOrNull { it.id == m.avatarItemId }?.imagePath }
    val memberFramePath = member?.let { m -> repo.shop.firstOrNull { it.id == m.frameItemId }?.imagePath }
    val title = when (filter) { ShopItemType.AVATAR -> "آواتارها"; ShopItemType.FRAME -> "قاب‌ها"; ShopItemType.REWARD -> "جوایز" }
    val shown = repo.shop.filter { it.active && it.type == filter }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader(title, back, "فروشگاه")
        member?.let { Text("${it.name} • سطح ${it.economy.level} • امتیاز ${it.economy.spendablePoints} • الماس ${it.economy.diamonds}", style = MaterialTheme.typography.bodyMedium) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repo.members.forEach { m -> FilterChip(selectedMemberId == m.id, { selectedMemberId = m.id; lastStoreMemberId = m.id; result = null }, label = { Text(m.name) }) }
        }
        result?.let { Text(it, color = Neon.Cyan) }
        if (shown.isEmpty()) Text("هنوز آیتمی در این بخش تعریف نشده است.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(shown, key = { it.id }) { item ->
                val itemBitmap = remember(item.imagePath) { item.imagePath?.let { AvatarRender.decodeSampled(it, 384)?.asImageBitmap() } }
                val reason = member?.let { com.kichikan.ak1.domain.service.ShopService.canAcquire(item, it, now) }
                val owned = member?.let { m -> historyOwned(repo, m.id, item.id) } == true
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (item.type) {
                        ShopItemType.FRAME -> MemberAvatar(memberAvatarPath, item.imagePath, 112.dp, member?.name ?: "")
                        ShopItemType.AVATAR -> MemberAvatar(item.imagePath, memberFramePath, 112.dp, member?.name ?: "")
                        ShopItemType.REWARD -> itemBitmap?.let { Image(bitmap = it, contentDescription = item.name, modifier = Modifier.size(104.dp)) }
                    }
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    item.minimumLevel?.let { Text("حداقل سطح: $it", style = MaterialTheme.typography.bodySmall) }
                    Text(if (item.methods.isEmpty()) "دریافت مستقیم" else item.methods.joinToString(" • ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (owned) "قبلاً دریافت شده" else if (item.currency == Currency.NONE || item.price == 0) "رایگان" else "قیمت: ${item.price} ${item.currency.name}")
                    val manualOnly = AcquisitionMethod.MANUAL in item.methods &&
                        AcquisitionMethod.DIRECT_PURCHASE !in item.methods &&
                        AcquisitionMethod.LEVEL_UNLOCK !in item.methods
                    Button(onClick = {
                        try {
                            when {
                                // A REWARD is consumed, not equipped: tapping "use" again must never
                                // charge the member a second time.
                                owned && item.type == ShopItemType.REWARD ->
                                    result = "«${item.name}» قبلاً دریافت شده است."
                                owned -> {
                                    repo.equipShopItem(selectedMemberId, item.id)
                                    result = "«${item.name}» برای عضو فعال شد."
                                    changed()
                                }
                                manualOnly -> manualGrantItem = item
                                else -> {
                                    repo.purchaseShopItem(selectedMemberId, item.id)
                                    result = "«${item.name}» دریافت شد."
                                    changed()
                                }
                            }
                        } catch (ex: Exception) { result = ex.message }
                    }, enabled = member != null && (manualOnly || reason == null)) {
                        Text(
                            when {
                                owned && item.type == ShopItemType.REWARD -> "قبلاً دریافت شد"
                                owned -> "استفاده"
                                manualOnly -> "هدیه مربی"
                                else -> "دریافت"
                            }
                        )
                    }
                    reason?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                } }
            }
        }
    }
    manualGrantItem?.let { item ->
        ManualGrantDialog(repo, selectedMemberId, item, changed) { manualGrantItem = null }
    }
}

@Composable
private fun ManualGrantDialog(repo: AppRepository, memberId: String, item: ShopItem, changed: () -> Unit, close: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("هدیه دادن «${item.name}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("این هدیه بدون کسر امتیاز یا الماس به عضو داده می‌شود.")
                OutlinedTextField(reason, { reason = it; error = null }, label = { Text("دلیل هدیه") }, singleLine = false)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    repo.grantShopItem(memberId, item.id, reason)
                    changed()
                    close()
                } catch (e: Exception) { error = e.message ?: "هدیه ثبت نشد" }
            }, enabled = reason.isNotBlank()) { Text("ثبت هدیه") }
        },
        dismissButton = { TextButton(close) { Text("لغو") } }
    )
}

private fun historyOwned(repo: AppRepository, memberId: String, itemId: String): Boolean = repo.history.any { it.memberId == memberId && it.type == HistoryType.REWARD_RECEIVED && it.metadata["itemId"] == itemId }
/** Icon + label used inside the wide settings buttons. */
@Composable
private fun ActionLabel(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppIcon(icon, 22.dp)
        Text(text)
    }
}

@Composable
private fun SettingsScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
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
    var profileOpen by remember { mutableStateOf(false) }
    var growthManagerOpen by remember { mutableStateOf(false) }
    var growthStageName by remember { mutableStateOf("") }
    val defaultGrowthStages = listOf("شروع مسیر", "خدمت‌گزار حلقه", "مسئولیت‌پذیری", "اعتمادسازی", "پیش‌قدم در خیر", "قله کمال")
    val growthStages = remember { mutableStateListOf<String>().apply {
        val raw = prefs.getString("growth_stages", null)
        if (raw == null) addAll(defaultGrowthStages) else runCatching {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    } }
    var profileImageUri by remember { mutableStateOf(prefs.getString("ring_profile_image", null)) }
    val profileImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            profileImageUri = uri.toString()
            prefs.edit().putString("ring_profile_image", uri.toString()).apply()
            changed()
            Toast.makeText(context, "تصویر پروفایل حلقه ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }
    val shortcutPrefs = prefs
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
    var scheduleOpen by remember { mutableStateOf(false) }
    var startDigits by remember { mutableStateOf("") }
    var endDigits by remember { mutableStateOf("") }
    var periodError by remember { mutableStateOf<String?>(null) }
    val periodDays = remember { mutableStateListOf<Int>().apply { addAll(activeWeekdays) } }
    var scheduleRefresh by remember { mutableIntStateOf(0) }
    val schedulePeriods = remember(scheduleRefresh) {
        mutableStateListOf<org.json.JSONObject>().apply {
            runCatching {
                val array = org.json.JSONArray(shortcutPrefs.getString("class_schedule_periods", "[]"))
                for (i in 0 until array.length()) add(array.getJSONObject(i))
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineMedium)
        Text("حلقه: ${repo.ring?.ringName ?: "-"}")
        Text("نام کاربری حلقه: ${repo.ring?.ringUsername ?: "-"}")
        OutlinedButton({ export.launch("AK1-backup.json") }, Modifier.fillMaxWidth()) { ActionLabel(R.drawable.icon_settings_backup, "خروجی کامل اطلاعات") }
        OutlinedButton({ import.launch(arrayOf("application/json", "text/plain")) }, Modifier.fillMaxWidth()) { ActionLabel(R.drawable.icon_settings_restore, "ورود اطلاعات پشتیبان") }
        OutlinedButton({ profileOpen = true }, Modifier.fillMaxWidth()) { ActionLabel(R.drawable.icon_settings_edit_profile, "ویرایش پروفایل") }
        OutlinedButton({ growthManagerOpen = true }, Modifier.fillMaxWidth()) { ActionLabel(R.drawable.icon_settings_growth_map, "مدیریت نقشه کمال") }
        OutlinedButton({ shortcutsOpen = true }, Modifier.fillMaxWidth()) { ActionLabel(R.drawable.icon_settings_shortcuts, "تنظیم میانبرهای خانه") }
        HorizontalDivider()
        Text("روزهای برگزاری کلاس", style = MaterialTheme.typography.titleMedium)
        Text("هر زمان برنامه تغییر کرد، روزهای کلاس را از اینجا به‌روزرسانی کن. روز حذف‌شده در حضور و غیاب هفتگی نمایش داده نمی‌شود.")
        OutlinedButton(onClick = { scheduleOpen = true; periodError = null; startDigits = JalaliCalendar.fromGregorian(java.time.LocalDate.now()).let { "%04d%02d%02d".format(it.year, it.month, it.day) }; endDigits = ""; periodDays.clear(); periodDays.addAll(activeWeekdays) }, modifier = Modifier.fillMaxWidth()) { Text("مدیریت بازه‌های زمانی برنامه هفتگی") }
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
    if (growthManagerOpen) {
        AlertDialog(
            onDismissRequest = { growthManagerOpen = false },
            title = { Text("طراحی مسیر صعود به قله کمال") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مراحل را خودت تعریف کن؛ سپس در نقشه هر عضو، رسیدن او به هر مرحله را دستی ثبت می‌کنی.")
                    OutlinedTextField(growthStageName, { growthStageName = it }, label = { Text("نام مقام یا مرحله جدید") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val value = growthStageName.trim()
                        if (value.isNotBlank() && value !in growthStages) {
                            growthStages.add(value)
                            prefs.edit().putString("growth_stages", org.json.JSONArray(growthStages).toString()).apply()
                            growthStageName = ""
                            changed()
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("افزودن مرحله به مسیر") }
                    growthStages.toList().forEachIndexed { index, stage ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                                Text("${index + 1}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary)
                            }
                            Text(stage, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = {
                                growthStages.remove(stage)
                                prefs.edit().putString("growth_stages", org.json.JSONArray(growthStages).toString()).apply()
                                changed()
                            }) { Text("حذف") }
                        }
                    }
                    if (growthStages.isEmpty()) Text("مسیر خالی است؛ مرحله‌های دلخواهت را اضافه کن.")
                }
            },
            confirmButton = { TextButton(onClick = { growthManagerOpen = false }) { Text("تمام") } }
        )
    }
    if (profileOpen) {
        val preview = remember(profileImageUri) {
            profileImageUri?.let { raw ->
                AvatarRender.decodeSampled(context, android.net.Uri.parse(raw), 256)?.asImageBitmap()
            }
        }
        AlertDialog(
            onDismissRequest = { profileOpen = false },
            title = { Text("ویرایش پروفایل حلقه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(112.dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
                        if (preview != null) Image(bitmap = preview, contentDescription = "پیش‌نمایش تصویر حلقه", modifier = Modifier.fillMaxSize())
                        else Box(contentAlignment = Alignment.Center) { Text("K1", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }
                    }
                    Text(repo.ring?.ringName ?: "حلقه", style = MaterialTheme.typography.titleMedium)
                    Text("تصویر در بالای صفحه خانه کنار نام حلقه نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { profileImagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("انتخاب تصویر") }
                }
            },
            confirmButton = { TextButton(onClick = { profileOpen = false }) { Text("انجام شد") } },
            dismissButton = { TextButton(onClick = {
                profileImageUri = null
                prefs.edit().remove("ring_profile_image").apply()
                changed()
                profileOpen = false
            }) { Text("حذف تصویر") } }
        )
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
    if (scheduleOpen) {
        AlertDialog(
            onDismissRequest = { scheduleOpen = false },
            title = { Text("بازه‌های زمانی برنامه کلاس") },
            text = {
                Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("برای هر بازه، تاریخ شروع، پایان اختیاری و روزهای کلاس را ثبت کن. بازه‌ها نباید هم‌پوشانی داشته باشند.")
                    if (schedulePeriods.isNotEmpty()) {
                        Text("بازه‌های ثبت‌شده", style = MaterialTheme.typography.titleMedium)
                        schedulePeriods.forEach { period ->
                            val start = period.optString("start")
                            val finish = period.optString("end")
                            val dayValues = runCatching {
                                val a = period.getJSONArray("days")
                                (0 until a.length()).map { a.getInt(it) }
                            }.getOrDefault(emptyList())
                            val startLabel = runCatching { JalaliCalendar.format(JalaliCalendar.fromGregorian(java.time.LocalDate.parse(start))) }.getOrDefault(start)
                            val endLabel = if (finish.isBlank()) "بدون پایان" else runCatching { JalaliCalendar.format(JalaliCalendar.fromGregorian(java.time.LocalDate.parse(finish))) }.getOrDefault(finish)
                            val daysLabel = weekdayLabels.filter { it.first in dayValues }.joinToString("، ") { it.second }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("$startLabel تا $endLabel", style = MaterialTheme.typography.titleSmall)
                                    Text(daysLabel.ifBlank { "بدون روز کلاس" }, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = {
                                    val raw = org.json.JSONArray(shortcutPrefs.getString("class_schedule_periods", "[]"))
                                    val updated = org.json.JSONArray()
                                    for (i in 0 until raw.length()) {
                                        val item = raw.getJSONObject(i)
                                        if (item.optString("start") != start || item.optString("end") != finish) updated.put(item)
                                    }
                                    shortcutPrefs.edit().putString("class_schedule_periods", updated.toString()).apply()
                                    scheduleRefresh++
                                    changed()
                                }) { Text("حذف") }
                            }
                        }
                    }
                    HorizontalDivider()
                    Text("افزودن بازه", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = startDigits, onValueChange = { startDigits = it.filter(Char::isDigit).take(8); periodError = null },
                        label = { Text("تاریخ شروع شمسی") }, placeholder = { Text("14050101") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = JalaliDateTransformation,
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
                    OutlinedTextField(value = endDigits, onValueChange = { endDigits = it.filter(Char::isDigit).take(8); periodError = null },
                        label = { Text("تاریخ پایان شمسی (اختیاری)") }, placeholder = { Text("خالی = بدون پایان") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = JalaliDateTransformation,
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
                    weekdayLabels.forEach { (day, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = periodDays.contains(day), onCheckedChange = { checked ->
                                if (checked && day !in periodDays) periodDays.add(day)
                                else if (!checked) periodDays.remove(day)
                            })
                            Text(label)
                        }
                    }
                    periodError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val startJ = JalaliCalendar.parse(startDigits, allowGregorian = false)
                            ?: throw IllegalArgumentException("تاریخ شروع شمسی معتبر نیست")
                        val endJ = if (endDigits.isBlank()) null else JalaliCalendar.parse(endDigits, allowGregorian = false)
                            ?: throw IllegalArgumentException("تاریخ پایان شمسی معتبر نیست")
                        val startDate = JalaliCalendar.toGregorian(startJ)
                        val endDate = endJ?.let { JalaliCalendar.toGregorian(it) }
                        require(endDate == null || !endDate.isBefore(startDate)) { "تاریخ پایان نباید قبل از شروع باشد" }
                        require(periodDays.isNotEmpty()) { "حداقل یک روز کلاس انتخاب کن" }
                        val existing = org.json.JSONArray(shortcutPrefs.getString("class_schedule_periods", "[]"))
                        for (i in 0 until existing.length()) {
                            val item = existing.getJSONObject(i)
                            val oldStart = java.time.LocalDate.parse(item.getString("start"))
                            val oldEndText = item.optString("end")
                            val oldEnd = if (oldEndText.isBlank()) java.time.LocalDate.MAX else java.time.LocalDate.parse(oldEndText)
                            val newEnd = endDate ?: java.time.LocalDate.MAX
                            require(newEnd.isBefore(oldStart) || startDate.isAfter(oldEnd)) { "این بازه با یک بازه ثبت‌شده هم‌پوشانی دارد" }
                        }
                        val dayArray = org.json.JSONArray()
                        periodDays.sorted().forEach { dayArray.put(it) }
                        existing.put(org.json.JSONObject().put("start", startDate.toString()).put("end", endDate?.toString() ?: "").put("days", dayArray))
                        shortcutPrefs.edit().putString("class_schedule_periods", existing.toString()).apply()
                        scheduleRefresh++
                        scheduleOpen = false
                        changed()
                        Toast.makeText(context, "بازه برنامه ثبت شد", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        periodError = e.message ?: "ثبت بازه ناموفق بود"
                    }
                }) { Text("ثبت بازه") }
            },
            dismissButton = { TextButton(onClick = { scheduleOpen = false }) { Text("بستن") } }
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



private fun copyShopImage(context: android.content.Context, uri: android.net.Uri): String {
    val folder = java.io.File(context.filesDir, "shop_images")
    if (!folder.exists() && !folder.mkdirs()) error("ساخت پوشه تصاویر ممکن نشد")
    val target = java.io.File(folder, "shop-${java.util.UUID.randomUUID()}.img")
    val input = context.contentResolver.openInputStream(uri) ?: error("خواندن تصویر انتخاب‌شده ممکن نشد")
    input.use { stream ->
        target.outputStream().use { output -> stream.copyTo(output) }
    }
    return target.absolutePath
}
