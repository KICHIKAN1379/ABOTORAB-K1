package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kichikan.ak1.ui.theme.Button
import com.kichikan.ak1.ui.theme.Card
import com.kichikan.ak1.ui.PageHeader
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.calendar.JalaliCalendar

@Composable
internal fun WorkshopScreen(repo: AppRepository, padding: PaddingValues, changed: () -> Unit, section: Int, back: () -> Unit) {
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
}

@Composable private fun MissionEditDialog(repo: AppRepository, item: Mission?, changed: () -> Unit, close: () -> Unit) {
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

@Composable internal fun EconomyDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }; var op by remember { mutableStateOf<EconomyOp?>(null) }; var memberId by remember { mutableStateOf(repo.members.firstOrNull()?.id?:"") }; var amount by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; val member=repo.members.firstOrNull{it.id==memberId}
    fun submit(){val m=member?:run{error="عضو انتخاب نشده";return};val n=amount.toIntOrNull()?:run{error="مقدار را وارد کنید";return};if(n<=0){error="مقدار باید بیشتر از صفر باشد";return};if(op==EconomyOp.LEVEL_DOWN&&n>=m.economy.level){error="سطح مقصد باید کمتر از سطح فعلی باشد";return};if(reason.isBlank()){error="دلیل اجباری است";return};try{when(op){EconomyOp.XP_ADD->repo.recordXp(m.id,n,reason,"mentor");EconomyOp.XP_SUB->repo.decreaseXp(m.id,n,reason,"mentor");EconomyOp.POINTS_ADD->repo.recordPoints(m.id,n,reason,"mentor");EconomyOp.POINTS_SUB->repo.recordPoints(m.id,-n,reason,"mentor");EconomyOp.DIAMONDS_ADD->repo.recordDiamonds(m.id,n,reason,"mentor");EconomyOp.DIAMONDS_SUB->repo.recordDiamonds(m.id,-n,reason,"mentor");EconomyOp.LEVEL_DOWN->repo.decreaseLevel(m.id,n,reason,"mentor");null->Unit};changed();close()}catch(ex:IllegalArgumentException){error=ex.message}}
    AlertDialog(onDismissRequest=close,title={Text(if(step==0)"شناسنامه" else "ثبت ${op?.label}")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(step==0){Text("ابتدا نوع تغییر را انتخاب کنید");listOf(EconomyOp.POINTS_ADD to "امتیاز",EconomyOp.DIAMONDS_ADD to "الماس",EconomyOp.XP_ADD to "XP",EconomyOp.LEVEL_DOWN to "سطح").forEach{pair->Button({op=pair.first;step=1},Modifier.fillMaxWidth()){Text(pair.second)}}}
            else { Text("عضو");Row(Modifier.horizontalScroll(rememberScrollState())){repo.members.forEach{m->FilterChip(memberId==m.id,{memberId=m.id},label={Text(m.name)})}};Text("نوع عملیات");Row(Modifier.horizontalScroll(rememberScrollState())){op?.let{base->val options=when(base){EconomyOp.POINTS_ADD->listOf(EconomyOp.POINTS_ADD,EconomyOp.POINTS_SUB);EconomyOp.DIAMONDS_ADD->listOf(EconomyOp.DIAMONDS_ADD,EconomyOp.DIAMONDS_SUB);EconomyOp.XP_ADD->listOf(EconomyOp.XP_ADD,EconomyOp.XP_SUB);else->listOf(EconomyOp.LEVEL_DOWN)};options.forEach{o->FilterChip(op==o,{op=o},label={Text(o.label)})}}};OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text(if(op==EconomyOp.LEVEL_DOWN)"سطح مقصد" else "مقدار")},singleLine=true);OutlinedTextField(reason,{reason=it},label={Text("دلیل (اجباری)")},singleLine=true);error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}
        } },confirmButton={if(step==0)TextButton(close){Text("انصراف")}else Row{TextButton({step=0;error=null}){Text("مرحله قبل")};TextButton({submit()}){Text("تأیید")}}},dismissButton={if(step==1)TextButton(close){Text("لغو")}})
}

@Composable private fun WheelConfigDialog(repo: AppRepository, changed: () -> Unit, close: () -> Unit) {
    var mode by remember { mutableStateOf(repo.wheel.mode) }; var points by remember { mutableStateOf(repo.wheel.spinCostPoints.toString()) }; var diamonds by remember { mutableStateOf(repo.wheel.spinCostDiamonds.toString()) }; var repeat by remember { mutableStateOf(repo.wheel.allowRepeatAfterWin) }
    AlertDialog(onDismissRequest=close,title={Text("تنظیمات گردونه")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.horizontalScroll(rememberScrollState())){WheelMode.values().forEach{m->FilterChip(mode==m,{mode=m},label={Text(m.name)})}};OutlinedTextField(points,{points=it.filter(Char::isDigit)},label={Text("هزینه امتیاز")});OutlinedTextField(diamonds,{diamonds=it.filter(Char::isDigit)},label={Text("هزینه الماس")});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(repeat,{repeat=it});Text("تکرار جایزه بعد از برد")}}},confirmButton={TextButton({repo.setWheel(repo.wheel.copy(mode=mode,spinCostPoints=points.toIntOrNull()?:0,spinCostDiamonds=diamonds.toIntOrNull()?:0,freeSpin=mode==WheelMode.FREE,allowRepeatAfterWin=repeat));changed();close()}){Text("ذخیره")}},dismissButton={TextButton(close){Text("لغو")}})
}
