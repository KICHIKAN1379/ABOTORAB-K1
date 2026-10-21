package com.kichikan.ak1.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.calendar.JalaliCalendar
import com.kichikan.ak1.domain.model.Member
import com.kichikan.ak1.domain.model.ShopItem
import com.kichikan.ak1.domain.model.ShopItemType
import com.kichikan.ak1.domain.service.ShopService
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.LocalAk1Background
import com.kichikan.ak1.ui.theme.Neon

/**
 * Member treasure ("گنجینه"): balances, history, and — new — free choice of avatar and frame.
 * Every avatar/frame the member owns (or any free one) can be picked at any time, in any
 * combination; the preview always shows the avatar *inside* the chosen frame.
 */
@Composable
fun TreasureDialog(repo: AppRepository, memberId: String, changed: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(1) }
    val member: Member = remember(tick, repo.members.size) { repo.members.firstOrNull { it.id == memberId } } ?: run { close(); return }
    val events = remember(tick, repo.history.size) { repo.history.filter { it.memberId == memberId }.sortedByDescending { it.createdAtEpochMillis } }
    var category by remember { mutableIntStateOf(0) }
    val rewardEvents = events.filter {
        it.type == com.kichikan.ak1.domain.model.HistoryType.REWARD_RECEIVED ||
            it.type == com.kichikan.ak1.domain.model.HistoryType.WHEEL_REWARD ||
            it.type == com.kichikan.ak1.domain.model.HistoryType.BIRTHDAY_REWARD
    }
    val filteredEvents = events.filter { event ->
        when (category) {
            1 -> event.type.name.startsWith("XP_") || event.type.name.startsWith("POINTS_") ||
                event.type.name.startsWith("DIAMONDS_") || event.type.name == "LEVEL_CHANGED"
            2 -> event.type.name == "ATTENDANCE" || event.type.name == "SESSION_NOTE"
            3 -> event.type.name == "MISSION_COMPLETED"
            4 -> event.type.name == "REWARD_RECEIVED" || event.type.name == "WHEEL_REWARD" || event.type.name == "BIRTHDAY_REWARD"
            5 -> event.type.name == "AVATAR_ACQUIRED" || event.type.name == "FRAME_ACQUIRED"
            else -> true
        }
    }
    val now = System.currentTimeMillis()
    val ownedIds = remember(tick, repo.history.size) {
        (events.mapNotNull { it.metadata["itemId"] } + listOfNotNull(member.avatarItemId, member.frameItemId)).toSet()
    }
    fun available(type: ShopItemType): List<ShopItem> = repo.shop.filter { item ->
        item.active && item.type == type && (
            item.id in ownedIds ||
                (item.price == 0 && ShopService.hasDirectPurchase(item) && ShopService.canAcquire(item, member, now) == null)
            )
    }
    val avatars = available(ShopItemType.AVATAR)
    val frames = available(ShopItemType.FRAME)
    val avatarPath = repo.shop.firstOrNull { it.id == member.avatarItemId }?.imagePath
    val framePath = repo.shop.firstOrNull { it.id == member.frameItemId }?.imagePath

    fun pick(type: ShopItemType, id: String?) {
        try {
            repo.chooseLook(memberId, type, id)
            tick++
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "انتخاب انجام نشد", Toast.LENGTH_SHORT).show()
        }
    }
    fun finish() { changed(); close() }

    Dialog(onDismissRequest = { finish() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            Column(Modifier.fillMaxSize().background(LocalAk1Background.current).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { finish() }) { Text("→ بازگشت") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MemberAvatar(avatarPath, framePath, 104.dp, member.name)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("گنجینه ${member.name}", style = MaterialTheme.typography.headlineSmall)
                        Text("سطح ${member.economy.level} • XP ${member.economy.xp}", style = MaterialTheme.typography.bodyMedium)
                        Text("امتیاز ${member.economy.spendablePoints} • 💎 ${member.economy.diamonds}", style = MaterialTheme.typography.bodyMedium, color = Neon.Gold)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(tab == 1, { tab = 1 }, label = { Text("آواتارها (${avatars.size})") })
                    FilterChip(tab == 2, { tab = 2 }, label = { Text("قاب‌ها (${frames.size})") })
                    FilterChip(tab == 3, { tab = 3 }, label = { Text("جوایز دریافتی (${rewardEvents.size})") })
                    FilterChip(tab == 0, { tab = 0 }, label = { Text("سوابق") })
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        0 -> {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("همه", "اقتصاد", "حضور", "مأموریت", "جوایز", "ظاهر").forEachIndexed { index, label ->
                                    FilterChip(category == index, { category = index }, label = { Text(label) })
                                }
                            }
                            if (filteredEvents.isEmpty()) Text("برای این دسته رویدادی ثبت نشده است.")
                            else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredEvents, key = { it.id }) { event ->
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                                            Text(JalaliCalendar.formatDateTime(event.createdAtEpochMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            event.reason?.takeIf { it.isNotBlank() }?.let { Text("دلیل: $it", style = MaterialTheme.typography.bodySmall) }
                                            event.amount?.let { Text("مقدار: $it", style = MaterialTheme.typography.bodySmall) }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (rewardEvents.isEmpty()) Text("هنوز جایزه‌ای دریافت نشده است.")
                            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(rewardEvents, key = { it.id }) { event ->
                                    val item = event.metadata["itemId"]?.let { id -> repo.shop.firstOrNull { it.id == id } }
                                    Card(Modifier.fillMaxWidth(), accent = Neon.Gold) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            item?.let { found ->
                                                val path = found.imagePath
                                                if (path != null) {
                                                    MemberAvatar(
                                                        if (found.type == ShopItemType.AVATAR) path else null,
                                                        if (found.type == ShopItemType.FRAME) path else null,
                                                        56.dp,
                                                        event.title
                                                    )
                                                }
                                            }
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(item?.name ?: event.title, style = MaterialTheme.typography.titleSmall)
                                                Text(JalaliCalendar.formatDateTime(event.createdAtEpochMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                event.reason?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Neon.Gold.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, Neon.Gold.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    "دریافت شد",
                                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Neon.Gold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            val type = if (tab == 1) ShopItemType.AVATAR else ShopItemType.FRAME
                            val list = if (tab == 1) avatars else frames
                            val activeId = if (tab == 1) member.avatarItemId else member.frameItemId
                            // First tile is "none"; the rest are the choosable items, three per row.
                            val tiles: List<ShopItem?> = listOf<ShopItem?>(null) + list
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Text(
                                        if (tab == 1) "آواتار دلخواه را انتخاب کن؛ دریافت هر آواتار فقط آن را به گنجینه اضافه می‌کند و انتخاب با خودت است."
                                        else "قاب دلخواه را انتخاب کن؛ آواتار همیشه داخل قاب می‌ماند و دریافت قاب به‌صورت خودکار آن را فعال نمی‌کند.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(tiles.chunked(3)) { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { item ->
                                            val selected = item?.id == activeId
                                            val previewAvatar = if (tab == 1) item?.imagePath else avatarPath
                                            val previewFrame = if (tab == 1) framePath else item?.imagePath
                                            Surface(
                                                onClick = { pick(type, item?.id) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(18.dp),
                                                color = if (selected) Neon.Cyan.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f),
                                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Neon.Cyan else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f))
                                            ) {
                                                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    MemberAvatar(previewAvatar, previewFrame, 84.dp, member.name)
                                                    Text(
                                                        item?.name ?: if (tab == 1) "بدون آواتار" else "بدون قاب",
                                                        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1
                                                    )
                                                    Text(
                                                        if (selected) "فعال ✓" else "انتخاب",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (selected) Neon.Cyan else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
