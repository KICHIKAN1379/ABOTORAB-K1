package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ShopServiceTest {
    private fun member(level: Int) = Member("m", "r", "عضو", MemberEconomy(xp = if (level == 1) 20 else 20 + (level - 1) * 30))

    @Test fun directPurchaseAllowsNormalItem() {
        val item = ShopItem("a", "آواتار", ShopItemType.AVATAR, price = 10, currency = Currency.POINTS)
        assertNull(ShopService.canAcquire(item, member(1), 1000))
        assertTrue(ShopService.hasDirectPurchase(item))
    }

    @Test fun levelUnlockIsFreeAndNeedsLevel() {
        val item = ShopItem("a", "آواتار", ShopItemType.AVATAR, minimumLevel = 3, methods = setOf(AcquisitionMethod.LEVEL_UNLOCK))
        assertNotNull(ShopService.canAcquire(item, member(1), 1000))
        assertNull(ShopService.canAcquire(item, member(3), 1000))
        assertTrue(ShopService.isFreeLevelUnlock(item))
    }

    @Test fun wheelOnlyCannotBePurchased() {
        val item = ShopItem("a", "جایزه", ShopItemType.REWARD, methods = setOf(AcquisitionMethod.WHEEL_ONLY))
        assertNotNull(ShopService.canAcquire(item, member(1), 1000))
    }
}
