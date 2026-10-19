package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.AvatarRender
import com.kichikan.ak1.ui.MemberAvatar
import com.kichikan.ak1.data.AppRepository

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
internal fun ShopItemEditDialog(repo: AppRepository, item: ShopItem, changed: () -> Unit, close: () -> Unit) {
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
internal fun AddShopItemDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
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
