package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.WheelConfig
import com.kichikan.ak1.domain.model.WheelItem
import com.kichikan.ak1.domain.model.WheelRewardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class WheelServiceTest {
    private fun item(id: String, active: Boolean = true, weight: Int = 1) =
        WheelItem(id, id, WheelRewardType.CUSTOM, weight = weight, active = active)

    @Test fun emptyWheelHasNoWinner() {
        assertNull(WheelService.spin(WheelConfig(items = emptyList())))
    }

    @Test fun inactiveItemsAreNeverSelected() {
        val result = WheelService.spin(WheelConfig(items = listOf(item("off", active = false), item("on"))))
        assertEquals("on", result?.id)
    }

    @Test fun previousWinnerIsExcludedWhenRepeatIsDisabled() {
        val config = WheelConfig(items = listOf(item("first"), item("second")), allowRepeatAfterWin = false)
        val result = WheelService.spin(config, previousWinnerIds = setOf("first"))
        assertEquals("second", result?.id)
    }

    @Test fun allPreviousWinnersReturnNoPrizeWhenRepeatIsDisabled() {
        val config = WheelConfig(items = listOf(item("first"), item("second")), allowRepeatAfterWin = false)
        assertNull(WheelService.spin(config, previousWinnerIds = setOf("first", "second")))
    }

    @Test fun repeatCanBeAllowed() {
        val config = WheelConfig(items = listOf(item("first")), allowRepeatAfterWin = true)
        assertNotNull(WheelService.spin(config, previousWinnerIds = setOf("first")))
    }

    @Test fun nonPositiveWeightsAreIgnored() {
        val config = WheelConfig(items = listOf(item("bad", weight = 0), item("good", weight = 2)))
        assertEquals("good", WheelService.spin(config)?.id)
    }
}
