package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.HistoryType
import com.kichikan.ak1.domain.model.MemberEconomy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test fun positivePointsAddXpSpendablePointsAndHalfDiamonds() {
        // 1000 points: +1000 XP (level 1 -> 34 = 33 levels * 10 diamonds) and 1000 * 0.5 = 500 diamonds.
        val result = EconomyService.addPoints(MemberEconomy(xp = 20), 1000, "جایزه", now = 3L)
        assertEquals(1020, result.economy.xp)
        assertEquals(34, result.economy.level)
        assertEquals(1000, result.economy.spendablePoints)
        assertEquals(830, result.economy.diamonds)
        assertEquals(0, result.economy.halfDiamondUnits)
    }

    @Test fun oddPointsLeaveOneHalfDiamondPending() {
        val result = EconomyService.adjustPoints(MemberEconomy(), 3, "تست", now = 4L)
        assertEquals(1, result.economy.diamonds)
        assertEquals(1, result.economy.halfDiamondUnits)
        // The pending half plus 1 more point completes another whole diamond.
        val next = EconomyService.adjustPoints(result.economy, 1, "تست", now = 5L)
        assertEquals(2, next.economy.diamonds)
        assertEquals(0, next.economy.halfDiamondUnits)
    }

    @Test fun negativePointsDoNotReduceXpOrDiamonds() {
        val start = MemberEconomy(xp = 100, spendablePoints = 50, diamonds = 7)
        val result = EconomyService.adjustPoints(start, -20, "جریمه", now = 6L)
        assertEquals(30, result.economy.spendablePoints)
        assertEquals(100, result.economy.xp)
        assertEquals(7, result.economy.diamonds)
        assertTrue(result.events.any { it.type == HistoryType.POINTS_DECREASED })
    }

    @Test fun diamondsCanBeDecreasedOnlyWithinBalance() {
        val start = MemberEconomy(diamonds = 5)
        val result = EconomyService.adjustDiamonds(start, -3, "تنظیم دستی", now = 7L)
        assertEquals(2, result.economy.diamonds)
        val failed = runCatching { EconomyService.adjustDiamonds(result.economy, -3, "تنظیم دستی", now = 8L) }
        assertTrue(failed.isFailure)
    }

    @Test fun xpDecreaseCannotExceedXpAndKeepsPointsAndDiamonds() {
        val start = MemberEconomy(xp = 60, spendablePoints = 40, diamonds = 9)
        val result = EconomyService.decreaseXp(start, 30, "اصلاح", now = 9L)
        assertEquals(30, result.economy.xp)
        assertEquals(40, result.economy.spendablePoints)
        assertEquals(9, result.economy.diamonds)
        assertTrue(runCatching { EconomyService.decreaseXp(start, 61, "اصلاح", now = 10L) }.isFailure)
    }
}
