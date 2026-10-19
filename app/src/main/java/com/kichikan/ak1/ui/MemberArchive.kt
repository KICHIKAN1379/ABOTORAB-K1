package com.kichikan.ak1.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.ArchivedMember
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon

/**
 * Archive of deleted members. Deleting a member moves the record here instead of destroying it,
 * so the mentor can bring back a member (with the whole ledger) or remove them permanently.
 */
@Composable
fun MemberArchivePage(repo: AppRepository, padding: PaddingValues, back: () -> Unit) {
    var message by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var confirmPurge by remember { mutableStateOf<ArchivedMember?>(null) }
    val entries = remember(tick) { repo.archived.sortedByDescending { it.archivedAtEpochMillis } }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader("بایگانی اعضا", back, "اعضای حذف‌شده، با تمام سابقه")
        Text(
            "هر عضوی که حذف شود به اینجا می‌آید و اطلاعاتش (امتیاز، الماس، سابقه، حضور و جلسات) حفظ می‌شود. " +
                "تا وقتی از همین‌جا برای همیشه حذفش نکنی، قابل بازگردانی است.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        message?.let { Text(it, color = Neon.Cyan) }
        if (entries.isEmpty()) {
            Text("بایگانی خالی است.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(entries, key = { it.member.id }) { entry ->
                    ArchiveRow(
                        repo = repo,
                        entry = entry,
                        onRestore = {
                            repo.restoreArchivedMember(entry.member.id)
                            message = "«${entry.member.name}» بازگردانی شد."
                            tick++
                        },
                        onPurge = { confirmPurge = entry }
                    )
                }
            }
        }
    }

    confirmPurge?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmPurge = null },
            title = { Text("حذف برای همیشه") },
            text = { Text("«${entry.member.name}» و تمام سابقه‌اش پاک شود؟ این کار برگشت‌پذیر نیست.") },
            confirmButton = {
                TextButton({
                    repo.purgeArchivedMember(entry.member.id)
                    message = "«${entry.member.name}» برای همیشه حذف شد."
                    confirmPurge = null
                    tick++
                }) { Text("حذف برای همیشه", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmPurge = null }) { Text("لغو") } }
        )
    }
}

@Composable
private fun ArchiveRow(
    repo: AppRepository,
    entry: ArchivedMember,
    onRestore: () -> Unit,
    onPurge: () -> Unit
) {
    val member = entry.member
    val avatarPath = repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath
    val framePath = repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MemberAvatar(avatarPath, framePath, 52.dp, member.name)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "سطح ${member.economy.level} • XP ${member.economy.xp} • امتیاز ${member.economy.spendablePoints} • 💎 ${member.economy.diamonds}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "بایگانی‌شده: ${JalaliCalendar.formatDateTime(entry.archivedAtEpochMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("بازگردانی") }
                OutlinedButton(onClick = onPurge, modifier = Modifier.weight(1f)) { Text("حذف برای همیشه") }
            }
        }
    }
}
