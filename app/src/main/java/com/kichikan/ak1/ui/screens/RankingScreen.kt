package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.Exports
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.ui.PageHeader
import com.kichikan.ak1.ui.RankRow
import com.kichikan.ak1.data.AppRepository

@Composable
internal fun RankingScreen(repo: AppRepository, padding: PaddingValues, groups: Boolean, back: () -> Unit) {
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
