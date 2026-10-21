package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.R
import com.kichikan.ak1.ui.theme.AppIcon
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.theme.ThemeMode
import com.kichikan.ak1.ui.theme.ThemeState
import com.kichikan.ak1.ui.AvatarRender
import com.kichikan.ak1.audio.BackgroundMusic
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.data.AutoArchive
import com.kichikan.ak1.data.DailyQuotes
import com.kichikan.ak1.domain.calendar.JalaliCalendar

/** Icon + label used inside the wide settings buttons. */
@Composable
private fun ActionLabel(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppIcon(icon, 22.dp)
        Text(text)
    }
}

private data class SettingsTool(val icon: Int, val label: String, val onClick: () -> Unit)

/** Phone-menu style tile: big icon with the label underneath. */
@Composable
private fun SettingsTile(tool: SettingsTool, modifier: Modifier = Modifier) {
    Surface(
        onClick = tool.onClick,
        modifier = modifier.heightIn(min = 96.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Neon.Cyan.copy(alpha = 0.25f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppIcon(tool.icon, 34.dp)
            Text(
                tool.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
internal fun SettingsScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit, openArchive: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ak1_settings", android.content.Context.MODE_PRIVATE) }
    var musicEnabled by remember { mutableStateOf(BackgroundMusic.enabled(context)) }
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
    var quotesOpen by remember { mutableStateOf(false) }
    var newQuote by remember { mutableStateOf("") }
    val quotes = remember { mutableStateListOf<String>().apply { addAll(DailyQuotes.list(context)) } }
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
        Card(Modifier.fillMaxWidth(), accent = Neon.Cyan) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)
                Text("حلقه: ${repo.ring?.ringName ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "نام کاربری مربی: ${repo.ring?.ringUsername ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(musicEnabled, { on ->
                musicEnabled = on
                BackgroundMusic.setEnabled(context, on)
            })
            Text("پخش موسیقی پس‌زمینه")
        }
        Text("ظاهر برنامه", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = ThemeState.mode == mode,
                    onClick = { ThemeState.set(context, mode) },
                    label = { Text(mode.label) }
                )
            }
        }
        Text(
            "تم انتخابی بلافاصله اعمال و ذخیره می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "هر بار که از برنامه خارج می‌شوی، یک نسخه‌ی پشتیبان با تاریخ شمسی در پوشه‌ی ${AutoArchive.folderLabel()} ذخیره می‌شود (۱۲ نسخه‌ی آخر نگه داشته می‌شود). اگر اجازهٔ دسترسی به حافظه داده نشود، پوشه در Download/MRT ساخته می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (android.os.Build.VERSION.SDK_INT >= 30 && !android.os.Environment.isExternalStorageManager()) {
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                    )
                }.onFailure {
                    runCatching {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                ActionLabel(R.drawable.icon_settings_backup, "اجازهٔ دسترسی به حافظهٔ اصلی (پوشه MRT)")
            }
        }
        Text("ابزارها", style = MaterialTheme.typography.titleMedium)
        val tools = listOf(
            SettingsTool(R.drawable.icon_settings_backup, "خروجی کامل") { export.launch("AK1-backup.json") },
            SettingsTool(R.drawable.icon_settings_restore, "ورود پشتیبان") { import.launch(arrayOf("application/json", "text/plain")) },
            SettingsTool(R.drawable.icon_settings_edit_profile, "ویرایش پروفایل") { profileOpen = true },
            SettingsTool(R.drawable.icon_settings_growth_map, "نقشه کمال") { growthManagerOpen = true },
            SettingsTool(R.drawable.icon_home_member_management, "بایگانی") { openArchive() },
            SettingsTool(R.drawable.icon_settings_shortcuts, "میانبرها") { shortcutsOpen = true }
        )
        tools.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tool -> SettingsTile(tool, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Card(Modifier.fillMaxWidth(), accent = Neon.Violet) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("جملهٔ روز", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (quotes.isEmpty()) "هنوز جمله‌ای ثبت نشده است. هر تعداد جمله بنویس تا هر روز یکی بالای صفحهٔ خانه بیاید."
                    else "${quotes.size} جمله ثبت شده • امروز: «${DailyQuotes.today(context) ?: ""}»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { quotesOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("مدیریت جمله‌های روز") }
            }
        }
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
    if (quotesOpen) {
        AlertDialog(
            onDismissRequest = { quotesOpen = false },
            title = { Text("جمله‌های روز") },
            text = {
                Column(
                    Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "هر تعداد جمله که می‌خواهی بنویس؛ هر روز یکی از آن‌ها به‌صورت خودکار بالای صفحهٔ خانه نمایش داده می‌شود.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newQuote,
                        onValueChange = { newQuote = it },
                        label = { Text("جملهٔ جدید") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            DailyQuotes.add(context, newQuote)
                            newQuote = ""
                            quotes.clear()
                            quotes.addAll(DailyQuotes.list(context))
                        },
                        enabled = newQuote.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("افزودن جمله") }
                    Text("${quotes.size} جمله ثبت‌شده", style = MaterialTheme.typography.labelMedium)
                    quotes.toList().forEach { quote ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(quote, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                DailyQuotes.remove(context, quote)
                                quotes.remove(quote)
                            }) { Text("حذف") }
                        }
                    }
                    if (quotes.isEmpty()) Text("فهرست خالی است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { quotesOpen = false }) { Text("بستن") } }
        )
    }
}
