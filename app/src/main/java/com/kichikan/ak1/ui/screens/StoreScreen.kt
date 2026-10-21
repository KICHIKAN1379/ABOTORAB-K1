package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.AvatarRender
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.ui.PageHeader
import com.kichikan.ak1.data.AppRepository

private var lastStoreMemberId: String = ""

@Composable
internal fun StoreScreen(repo: AppRepository, padding: PaddingValues, filter: ShopItemType, changed: () -> Unit, back: () -> Unit) {
    var selectedMemberId by remember { mutableStateOf(lastStoreMemberId.takeIf { id -> repo.members.any { it.id == id } } ?: repo.members.firstOrNull()?.id ?: "") }
    var result by remember { mutableStateOf<String?>(null) }
    var manualGrantItem by remember { mutableStateOf<ShopItem?>(null) }
    val member = repo.members.firstOrNull { it.id == selectedMemberId }
    val now = System.currentTimeMillis()
    val memberAvatarPath = member?.let { m -> repo.shop.firstOrNull { it.id == m.avatarItemId }?.imagePath }
    val memberFramePath = member?.let { m -> repo.shop.firstOrNull { it.id == m.frameItemId }?.imagePath }
    val title = when (filter) { ShopItemType.AVATAR -> "آواتارها"; ShopItemType.FRAME -> "قاب‌ها"; ShopItemType.REWARD -> "جوایز" }
    // The mentor's uploaded profile pack (assets/art/profiles) is free, so it is also
    // surfaced in the rewards tab where gifts are expected to live.
    val shown = repo.shop.filter {
        it.active && (it.type == filter || (filter == ShopItemType.REWARD && it.id.startsWith("profile-")))
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader(title, back, "فروشگاه")
        member?.let { Text("${it.name} • سطح ${it.economy.level} • امتیاز ${it.economy.spendablePoints} • الماس ${it.economy.diamonds}", style = MaterialTheme.typography.bodyMedium) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repo.members.forEach { m -> FilterChip(selectedMemberId == m.id, { selectedMemberId = m.id; lastStoreMemberId = m.id; result = null }, label = { Text(m.name) }) }
        }
        result?.let { Text(it, color = Neon.Cyan) }
        if (filter == ShopItemType.REWARD) {
            Text(
                "آواتارها و قاب‌های تازهٔ آپلودشده هم به‌صورت رایگان در همین بخش هستند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${shown.size} مورد • ${shown.count { it.currency == Currency.NONE || it.price == 0 }} رایگان",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (shown.isEmpty()) Text("هنوز آیتمی در این بخش تعریف نشده است.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(shown, key = { it.id }) { item ->
                val itemBitmap = remember(item.imagePath) { item.imagePath?.let { AvatarRender.decodeSampled(it, 384)?.asImageBitmap() } }
                val reason = member?.let { com.kichikan.ak1.domain.service.ShopService.canAcquire(item, it, now) }
                val owned = member?.let { m -> historyOwned(repo, m.id, item.id) } == true
                val free = item.currency == Currency.NONE || item.price == 0
                val accent = when (item.type) {
                    ShopItemType.AVATAR -> Neon.Cyan
                    ShopItemType.FRAME -> Neon.Violet
                    ShopItemType.REWARD -> Neon.Gold
                }
                val typeLabel = when (item.type) {
                    ShopItemType.AVATAR -> "آواتار"
                    ShopItemType.FRAME -> "قاب تصویر"
                    ShopItemType.REWARD -> "جایزه"
                }
                Card(Modifier.fillMaxWidth().animateItem(), accent = accent) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (item.type) {
                                ShopItemType.FRAME -> MemberAvatar(memberAvatarPath, item.imagePath, 88.dp, member?.name ?: "")
                                ShopItemType.AVATAR -> MemberAvatar(item.imagePath, memberFramePath, 88.dp, member?.name ?: "")
                                ShopItemType.REWARD -> itemBitmap?.let { Image(bitmap = it, contentDescription = item.name, modifier = Modifier.size(84.dp)) }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StoreBadge(
                                    text = when {
                                        owned -> "در اختیار"
                                        free -> "رایگان"
                                        else -> "${item.price} ${item.currency.name}"
                                    },
                                    accent = if (owned || free) Neon.Green else Neon.Gold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                StoreBadge(typeLabel, accent)
                                item.minimumLevel?.let {
                                    Text(
                                        "از سطح $it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(if (item.methods.isEmpty()) "دریافت مستقیم" else item.methods.joinToString(" • ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        }
                    }
                }
            }
        }
    }
    manualGrantItem?.let { item ->
        ManualGrantDialog(repo, selectedMemberId, item, changed) { manualGrantItem = null }
    }
}

@Composable
private fun StoreBadge(text: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
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
