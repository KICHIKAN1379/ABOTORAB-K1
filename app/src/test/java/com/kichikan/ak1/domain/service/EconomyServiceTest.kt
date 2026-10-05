package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.MemberEconomy
import org.junit.Assert.assertEquals
import org.junit.Test

class EconomyServiceTest {
    @Test fun xpCanCrossMultipleLevelsAndAwardsDiamonds() {
        val result = EconomyService.addXp(MemberEconomy(xp = 19), 61, "آزمون", now = 1L)
        assertEquals(80, result.economy.xp)
        assertEquals(3, result.economy.level)
        assertEquals(30, result.economy.diamonds)
    }

    @Test fun levelDecreaseRemovesExactlyThirtyXpPerLevelStep() {
        val result = EconomyService.decreaseLevel(MemberEconomy(xp = 158), 4, "اصلاح تربیتی", now = 2L)
        assertEquals(128, result.economy.xp)
        assertEquals(4, result.economy.level)
    }

    @Test fun pointsDoNotChangeLevel() {
        val result = EconomyService.addPoints(MemberEconomy(xp = 20), 1000, "جایزه", now = 3L)
        assertEquals(20, result.economy.xp)
        assertEquals(1, result.economy.level)
        assertEquals(1000, result.economy.spendablePoints)
    }
}
