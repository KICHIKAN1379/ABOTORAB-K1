package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.HistoryEvent
import com.kichikan.ak1.domain.model.HistoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryEventsTest {
    private fun event(id: String, amount: Int) = HistoryEvent(
        id = id,
        memberId = "m1",
        type = HistoryType.POINTS_EARNED,
        amount = amount,
        title = "t",
        createdAtEpochMillis = 0L
    )

    @Test fun dedupeKeepsTheNewestDuplicateAndTheOriginalOrder() {
        val first = event("a", 1)
        val stale = event("b", 1)
        val other = event("c", 3)
        val corrected = event("b", 2)

        val result = HistoryEvents.dedupeNewest(listOf(first, stale, other, corrected))

        assertEquals(listOf("a", "c", "b"), result.map { it.id })
        assertEquals(2, result.last().amount ?: -1)
    }

    @Test fun dedupeLeavesUniqueEventsUntouched() {
        val events = listOf(event("a", 1), event("b", 2))
        assertEquals(events, HistoryEvents.dedupeNewest(events))
    }

    @Test fun uniqueReturnsTheSameInstanceWhenTheIdIsFree() {
        val used = mutableSetOf<String>()
        val original = event("xp-1", 10)

        val result = HistoryEvents.unique(original, used)

        assertSame(original, result)
        assertTrue("xp-1" in used)
    }

    @Test fun uniqueAddsStableSuffixesForCollisions() {
        val used = mutableSetOf<String>()

        val first = HistoryEvents.unique(event("xp-1", 10), used)
        val second = HistoryEvents.unique(event("xp-1", 20), used)
        val third = HistoryEvents.unique(event("xp-1", 30), used)

        assertEquals("xp-1", first.id)
        assertEquals("xp-1#2", second.id)
        assertEquals("xp-1#3", third.id)
        assertEquals(setOf("xp-1", "xp-1#2", "xp-1#3"), used)
    }
}
