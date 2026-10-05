package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.*

data class EconomyChange(val economy: MemberEconomy, val events: List<HistoryEvent>)

object EconomyService {
    fun addXp(economy: MemberEconomy, amount: Int, reason: String, actor: String? = null, now: Long): EconomyChange {
        require(amount > 0)
        val oldLevel = economy.level
        val next = economy.copy(xp = economy.xp + amount)
        val gainedLevels = (oldLevel + 1)..next.level
        val events = mutableListOf(HistoryEvent("xp-$now", "", HistoryType.XP_EARNED, amount, "کسب XP", reason, now, actor))
        for (level in gainedLevels) {
            events += HistoryEvent("level-$now-$level", "", HistoryType.LEVEL_CHANGED, level, "رسیدن به سطح $level", reason, now, actor)
            events += HistoryEvent("diamond-level-$now-$level", "", HistoryType.DIAMONDS_EARNED, 10, "پاداش رسیدن به سطح $level", reason, now, actor)
        }
        return EconomyChange(next.copy(diamonds = next.diamonds + gainedLevels.count() * 10), events)
    }

    fun addPoints(economy: MemberEconomy, amount: Int, reason: String, actor: String? = null, now: Long): EconomyChange {
        require(amount > 0)
        return EconomyChange(economy.copy(spendablePoints = economy.spendablePoints + amount),
            listOf(HistoryEvent("points-$now", "", HistoryType.POINTS_EARNED, amount, "کسب امتیاز", reason, now, actor)))
    }

    fun addDiamonds(economy: MemberEconomy, amount: Int, reason: String, actor: String? = null, now: Long): EconomyChange {
        require(amount > 0)
        return EconomyChange(economy.copy(diamonds = economy.diamonds + amount),
            listOf(HistoryEvent("diamond-$now", "", HistoryType.DIAMONDS_EARNED, amount, "کسب الماس", reason, now, actor)))
    }

    fun spendPoints(economy: MemberEconomy, amount: Int, reason: String, actor: String? = null, now: Long): EconomyChange {
        require(amount > 0 && economy.spendablePoints >= amount)
        return EconomyChange(economy.copy(spendablePoints = economy.spendablePoints - amount),
            listOf(HistoryEvent("spend-$now", "", HistoryType.POINTS_SPENT, amount, "خرج امتیاز", reason, now, actor)))
    }

    fun decreaseLevel(economy: MemberEconomy, targetLevel: Int, reason: String, actor: String? = null, now: Long): EconomyChange {
        val oldLevel = economy.level
        require(targetLevel in 0 until oldLevel)
        val nextXp = LevelRules.xpAfterLevelDecrease(economy.xp, oldLevel, targetLevel)
        val removed = economy.xp - nextXp
        return EconomyChange(economy.copy(xp = nextXp), listOf(
            HistoryEvent("xp-down-$now", "", HistoryType.XP_DECREASED, removed, "کاهش XP", reason, now, actor,
                mapOf("previousLevel" to oldLevel.toString(), "newLevel" to targetLevel.toString())),
            HistoryEvent("level-down-$now", "", HistoryType.LEVEL_CHANGED, targetLevel, "کاهش سطح به $targetLevel", reason, now, actor,
                mapOf("previousLevel" to oldLevel.toString(), "newLevel" to targetLevel.toString(), "xpRemoved" to removed.toString()))
        ))
    }
}