package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.*

data class EconomyChange(val economy: MemberEconomy, val events: List<HistoryEvent>)

object EconomyService {

    fun addXp(
        economy: MemberEconomy,
        amount: Int,
        reason: String,
        actor: String? = null,
        now: Long
    ): EconomyChange {
        require(amount > 0)
        val oldLevel = economy.level
        val next = economy.copy(xp = economy.xp + amount)
        val gainedLevels = if (next.level > oldLevel) (oldLevel + 1)..next.level else IntRange.EMPTY

        val events = mutableListOf(
            HistoryEvent("xp-$now", "", HistoryType.XP_EARNED, amount, "کسب XP", reason, now, actor)
        )

        for (level in gainedLevels) {
            events += HistoryEvent(
                "level-$now-$level", "", HistoryType.LEVEL_CHANGED, level,
                "رسیدن به سطح $level", reason, now, actor
            )
            events += HistoryEvent(
                "diamond-level-$now-$level", "", HistoryType.DIAMONDS_EARNED, 10,
                "پاداش رسیدن به سطح $level", reason, now, actor
            )
        }

        return EconomyChange(
            next.copy(diamonds = next.diamonds + gainedLevels.count() * 10),
            events
        )
    }

    /**
     * Positive points also grant the same amount of XP.
     * Negative points only reverse the spendable point balance.
     * XP is reversed separately through decreaseXp().
     */
    fun adjustPoints(
        economy: MemberEconomy,
        amount: Int,
        reason: String,
        actor: String? = null,
        now: Long
    ): EconomyChange {
        require(amount != 0)
        if (amount > 0) {
            val xpChange = addXp(economy, amount, reason, actor, now)
            val pointEvent = HistoryEvent(
                "points-$now", "", HistoryType.POINTS_EARNED, amount,
                "افزایش امتیاز", reason, now, actor
            )
            return EconomyChange(
                xpChange.economy.copy(
                    spendablePoints = economy.spendablePoints + amount
                ),
                xpChange.events + pointEvent
            )
        }

        val decrease = -amount
        require(economy.spendablePoints >= decrease) { "امتیاز کافی نیست" }
        return EconomyChange(
            economy.copy(spendablePoints = economy.spendablePoints - decrease),
            listOf(
                HistoryEvent(
                    "points-down-$now", "", HistoryType.POINTS_DECREASED, decrease,
                    "کاهش امتیاز", reason, now, actor
                )
            )
        )
    }

    fun adjustDiamonds(
        economy: MemberEconomy,
        amount: Int,
        reason: String,
        actor: String? = null,
        now: Long
    ): EconomyChange {
        require(amount != 0)
        if (amount > 0) {
            return EconomyChange(
                economy.copy(diamonds = economy.diamonds + amount),
                listOf(
                    HistoryEvent(
                        "diamond-$now", "", HistoryType.DIAMONDS_EARNED, amount,
                        "افزایش الماس", reason, now, actor
                    )
                )
            )
        }

        val decrease = -amount
        require(economy.diamonds >= decrease) { "الماس کافی نیست" }
        return EconomyChange(
            economy.copy(diamonds = economy.diamonds - decrease),
            listOf(
                HistoryEvent(
                    "diamond-down-$now", "", HistoryType.DIAMONDS_DECREASED, decrease,
                    "کاهش الماس", reason, now, actor
                )
            )
        )
    }

    fun decreaseXp(
        economy: MemberEconomy,
        amount: Int,
        reason: String,
        actor: String? = null,
        now: Long
    ): EconomyChange {
        require(amount > 0)
        require(amount <= economy.xp) { "XP کافی نیست" }

        val oldLevel = economy.level
        val nextXp = economy.xp - amount
        val nextLevel = LevelRules.levelForXp(nextXp)

        val events = mutableListOf(
            HistoryEvent(
                "xp-down-$now", "", HistoryType.XP_DECREASED, amount,
                "کاهش XP", reason, now, actor,
                mapOf("previousLevel" to oldLevel.toString(), "newLevel" to nextLevel.toString())
            )
        )

        if (nextLevel != oldLevel) {
            events += HistoryEvent(
                "level-down-xp-$now", "", HistoryType.LEVEL_CHANGED, nextLevel,
                "کاهش سطح به $nextLevel", reason, now, actor,
                mapOf(
                    "previousLevel" to oldLevel.toString(),
                    "newLevel" to nextLevel.toString()
                )
            )
        }

        return EconomyChange(economy.copy(xp = nextXp), events)
    }

    fun spendPoints(
        economy: MemberEconomy,
        amount: Int,
        reason: String,
        actor: String? = null,
        now: Long
    ): EconomyChange {
        require(amount > 0 && economy.spendablePoints >= amount)
        return EconomyChange(
            economy.copy(spendablePoints = economy.spendablePoints - amount),
            listOf(
                HistoryEvent(
                    "spend-$now", "", HistoryType.POINTS_SPENT, amount,
                    "خرج امتیاز", reason, now, actor
                )
            )
        )
    }

    fun decreaseLevel(
        economy: MemberEconomy,
        targetLevel: Int,
        reason: String,
        actor: String? = null,
        now: Long
    ): EconomyChange {
        val oldLevel = economy.level
        require(targetLevel in 0 until oldLevel)
        val nextXp = LevelRules.xpAfterLevelDecrease(economy.xp, oldLevel, targetLevel)
        val removed = economy.xp - nextXp

        return EconomyChange(
            economy.copy(xp = nextXp),
            listOf(
                HistoryEvent(
                    "xp-down-$now", "", HistoryType.XP_DECREASED, removed,
                    "کاهش XP", reason, now, actor,
                    mapOf("previousLevel" to oldLevel.toString(), "newLevel" to targetLevel.toString())
                ),
                HistoryEvent(
                    "level-down-$now", "", HistoryType.LEVEL_CHANGED, targetLevel,
                    "کاهش سطح به $targetLevel", reason, now, actor,
                    mapOf(
                        "previousLevel" to oldLevel.toString(),
                        "newLevel" to targetLevel.toString(),
                        "xpRemoved" to removed.toString()
                    )
                )
            )
        )
    }
}