package com.kichikan.ak1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelRulesTest {
    @Test fun levels_follow_20_then_30_rule() {
        assertEquals(0, LevelRules.levelForXp(19))
        assertEquals(1, LevelRules.levelForXp(20))
        assertEquals(1, LevelRules.levelForXp(49))
        assertEquals(2, LevelRules.levelForXp(50))
        assertEquals(5, LevelRules.levelForXp(140))
        assertEquals(5, LevelRules.levelForXp(169))
        assertEquals(6, LevelRules.levelForXp(170))
    }

    @Test fun decreasing_level_5_to_4_removes_30_xp() {
        assertEquals(128, LevelRules.xpAfterLevelDecrease(158, 5, 4))
    }

    @Test fun decreasing_level_1_to_0_removes_20_xp() {
        assertEquals(0, LevelRules.xpAfterLevelDecrease(20, 1, 0))
    }
}
