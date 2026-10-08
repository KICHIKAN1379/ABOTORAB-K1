package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.AcquisitionMethod
import com.kichikan.ak1.domain.model.Member
import com.kichikan.ak1.domain.model.ShopItem

object ShopService {
    fun isEventActive(item: ShopItem, now: Long): Boolean {
        val startOk = item.eventStartEpochMillis == null || now >= item.eventStartEpochMillis
        val endOk = item.eventEndEpochMillis == null || now <= item.eventEndEpochMillis
        return startOk && endOk
    }

    fun canAcquire(item: ShopItem, member: Member, now: Long): String? {
        if (!item.active) return "این آیتم فعال نیست"
        item.minimumLevel?.let {
            if (member.economy.level < it) return "برای این آیتم حداقل سطح $it لازم است"
        }
        if (AcquisitionMethod.WHEEL_ONLY in item.methods) return "این آیتم فقط از طریق گردونه قابل دریافت است"
        if (AcquisitionMethod.EVENT in item.methods && !isEventActive(item, now)) {
            return "زمان دریافت این آیتم فعال نیست"
        }
        if (AcquisitionMethod.MANUAL in item.methods && AcquisitionMethod.DIRECT_PURCHASE !in item.methods &&
            AcquisitionMethod.LEVEL_UNLOCK !in item.methods) {
            return "این آیتم فقط توسط مربی قابل اعطا است"
        }
        if (AcquisitionMethod.MISSION in item.methods && AcquisitionMethod.DIRECT_PURCHASE !in item.methods &&
            AcquisitionMethod.LEVEL_UNLOCK !in item.methods) {
            return "این آیتم از طریق مأموریت قابل دریافت است"
        }
        return null
    }

    fun isFreeLevelUnlock(item: ShopItem): Boolean =
        AcquisitionMethod.LEVEL_UNLOCK in item.methods &&
            AcquisitionMethod.DIRECT_PURCHASE !in item.methods

    fun hasDirectPurchase(item: ShopItem): Boolean =
        AcquisitionMethod.DIRECT_PURCHASE in item.methods || item.methods.isEmpty()
}
