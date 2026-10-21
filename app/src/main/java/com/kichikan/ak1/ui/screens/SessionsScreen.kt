package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Motion
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.PageHeader
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.calendar.JalaliCalendar

@Composable
internal fun SessionsScreen(padding: PaddingValues, repo: AppRepository, changed: () -> Unit, attendance: Boolean, back: () -> Unit) {
    var add by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Session?>(null) }
    var detail by remember { mutableStateOf<Session?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader(if (attendance) "حضور و غیاب" else "فهرست جلسات", back,
            if (attendance) "تقویم هفتگی و ماهانه" else "برای دیدن جزئیات، روی جلسه بزن")
        if (attendance) AttendanceCalendarScreen(repo, changed)
        else {
            val sessions = repo.sessions.sortedByDescending { it.startsAt }
            val now = System.currentTimeMillis()
            val upcoming = sessions.count { it.startsAt > now }
            Card(Modifier.fillMaxWidth(), accent = Neon.Coral) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SessionStat("جلسات", "${sessions.size}", Neon.Coral, Modifier.weight(1f))
                    SessionStat("پیش رو", "$upcoming", Neon.Cyan, Modifier.weight(1f))
                    SessionStat("اعضا", "${repo.members.size}", Neon.Violet, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${sessions.size} جلسه ثبت‌شده", style = MaterialTheme.typography.labelLarge)
                Button(onClick = { add = true }, enabled = repo.members.isNotEmpty()) { Text("+ جلسه") }
            }
            if (sessions.isEmpty()) Text("هنوز جلسه‌ای ثبت نشده است.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { session ->
                    val zone = java.time.ZoneId.systemDefault()
                    val moment = java.time.Instant.ofEpochMilli(session.startsAt).atZone(zone)
                    val j = JalaliCalendar.fromGregorian(moment.toLocalDate())
                    val isUpcoming = session.startsAt > now
                    val accent = if (isUpcoming) Neon.Cyan else Neon.Coral
                    Card(onClick = { detail = session }, modifier = Modifier.fillMaxWidth().animateItem(), accent = accent) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = accent.copy(alpha = 0.13f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${j.day}", style = MaterialTheme.typography.headlineSmall, color = accent)
                                    Text(
                                        JalaliCalendar.MONTH_NAMES[j.month - 1],
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                // Only the essentials here: topic, member, date and time. The rest lives in the detail view.
                                Text(session.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                                Text(
                                    repo.members.firstOrNull { it.id == session.memberId }?.name ?: "عضو حذف‌شده",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Neon.Cyan
                                )
                                Text(
                                    "%02d:%02d".format(moment.hour, moment.minute) + if (isUpcoming) " • پیش رو" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                session.location?.takeIf { it.isNotBlank() }?.let {
                                    Text("مکان: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
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
private fun SessionStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
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
                val statuses = days.map { day ->
                    day to repo.attendance.firstOrNull {
                        it.memberId == member.id && it.sessionId == null &&
                            java.time.Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() == day
                    }?.status
                }
                val attended = statuses.count {
                    it.second == AttendanceStatus.PRESENT || it.second == AttendanceStatus.LATE
                }
                val ratio = if (days.isEmpty()) 0 else attended * 100 / days.size
                val cardAccent = if (days.isNotEmpty() && attended == days.size) Neon.Green else Neon.Cyan
                val avatarPath = repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath
                val framePath = repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath
                Card(Modifier.fillMaxWidth().animateItem(), accent = cardAccent) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MemberAvatar(avatarPath, framePath, 40.dp, member.name)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(member.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "$attended از ${days.size} روز حاضر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = cardAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, cardAccent.copy(alpha = 0.30f))
                            ) {
                                Text(
                                    "$ratio٪",
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cardAccent
                                )
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            statuses.forEach { (day, status) ->
                                val effective = status?.takeIf { it != AttendanceStatus.UNMARKED }
                                val dayAccent = when (effective) {
                                    AttendanceStatus.PRESENT -> Neon.Green
                                    AttendanceStatus.EXCUSED -> Neon.Gold
                                    AttendanceStatus.ABSENT -> Neon.Coral
                                    AttendanceStatus.LATE -> Neon.Violet
                                    else -> Neon.Sky
                                }
                                val container by animateColorAsState(
                                    targetValue = if (effective == null) MaterialTheme.colorScheme.surfaceVariant
                                    else dayAccent.copy(alpha = 0.22f),
                                    animationSpec = tween(Motion.Medium),
                                    label = "attendance-fill"
                                )
                                val mark = when (effective) {
                                    AttendanceStatus.PRESENT -> "ح"
                                    AttendanceStatus.EXCUSED -> "م"
                                    AttendanceStatus.ABSENT -> "غ"
                                    AttendanceStatus.LATE -> "ت"
                                    else -> "—"
                                }
                                val statusLabel = when (effective) {
                                    AttendanceStatus.PRESENT -> "حاضر"
                                    AttendanceStatus.EXCUSED -> "موجه"
                                    AttendanceStatus.ABSENT -> "غایب"
                                    AttendanceStatus.LATE -> "تأخیر"
                                    else -> "بدون ثبت"
                                }
                                val jd = JalaliCalendar.fromGregorian(day)
                                OutlinedButton(
                                    onClick = { attendanceEditTarget = member to day },
                                    modifier = Modifier.width(78.dp).height(74.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    border = BorderStroke(1.dp, if (effective == null) MaterialTheme.colorScheme.outlineVariant else dayAccent.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = container)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text("${jd.day}", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            mark,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (effective == null) MaterialTheme.colorScheme.onSurfaceVariant else dayAccent
                                        )
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
}
