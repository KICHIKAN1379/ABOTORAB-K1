package com.kichikan.ak1.domain.model

/**
 * AK1 economy:
 * XP = permanent progression and ranking.
 * POINTS = spendable currency for mentor-defined rewards.
 * DIAMONDS = spendable currency for personalization.
 */
data class MemberEconomy(
    val xp: Int = 0,
    val spendablePoints: Int = 0,
    val diamonds: Int = 0,
    val levelOverride: Int? = null
) {
    val calculatedLevel: Int
        get() = LevelRules.levelForXp(xp)

    val effectiveLevel: Int
        get() = levelOverride ?: calculatedLevel

    /**
     * Compatibility alias used by domain services.
     * Level is derived from XP unless a future feature explicitly introduces an override.
     */
    val level: Int
        get() = effectiveLevel
}

object LevelRules {
    const val FIRST_LEVEL_XP = 20
    const val XP_PER_NEXT_LEVEL = 30

    fun levelForXp(xp: Int): Int =
        when {
            xp < FIRST_LEVEL_XP -> 0
            else -> 1 + (xp - FIRST_LEVEL_XP) / XP_PER_NEXT_LEVEL
        }

    fun xpForLevel(level: Int): Int =
        when {
            level <= 0 -> 0
            else -> FIRST_LEVEL_XP + (level - 1) * XP_PER_NEXT_LEVEL
        }

    /**
     * Lowering a level removes one 30-XP tier per level,
     * except crossing from level 1 to 0 removes the initial 20 XP.
     */
    fun xpAfterLevelDecrease(currentXp: Int, currentLevel: Int, targetLevel: Int): Int {
        require(targetLevel in 0 until currentLevel)
        return (currentXp - xpForLevel(currentLevel) + xpForLevel(targetLevel))
            .coerceAtLeast(0)
    }
}

data class DiamondPolicy(
    val missionIndividual: Int = 0,
    val missionGroup: Int = 0,
    val diamondsPerNewLevel: Int = 10,
    val birthday: Int = 30
)

data class MissionReward(
    val points: Int = 0,
    val diamonds: Int = 0
)
