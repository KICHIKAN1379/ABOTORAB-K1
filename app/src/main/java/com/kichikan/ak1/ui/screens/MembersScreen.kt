package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.Exports
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.ui.TreasureDialog
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.calendar.JalaliCalendar

@Composable
internal fun MembersScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit, openGrowth: (Member) -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var add by remember { mutableStateOf(false) }
    var addGroup by remember { mutableStateOf(false) }
    var actionMember by remember { mutableStateOf<Member?>(null) }
    var selected by remember { mutableStateOf<Member?>(null) }
    var editing by remember { mutableStateOf<Member?>(null) }
    var groupEditing by remember { mutableStateOf<Group?>(null) }
    var scoreGroup by remember { mutableStateOf<Group?>(null) }
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
                    TextButton({ actionMember = null; openGrowth(m) }) { Text("نقشه کمال") }
                }
            },
            dismissButton = { TextButton({ actionMember = null }) { Text("بستن") } }
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
    dismissButton = { Row { TextButton({ repo.archiveMember(member.id); changed(); close() }) { Text("حذف (به بایگانی)") }; TextButton(close) { Text("لغو") } } })
}

@Composable private fun MemberNotesDialog(repo: AppRepository, member: Member, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text("توضیحات ${member.name}") }, text = { Text(if (member.privateNotes.isBlank()) "برای این عضو توضیحی ثبت نشده است." else member.privateNotes) }, confirmButton = { TextButton(close) { Text("بستن") } })
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
