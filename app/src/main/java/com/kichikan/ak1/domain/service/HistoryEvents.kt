package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.HistoryEvent

/**
 * Identity rules for member history events.
 *
 * Event ids used to be derived from the current millisecond (`"xp-$now"`) or from a member and
 * a calendar day (attendance). Both schemes can produce the same id twice: two operations in one
 * millisecond, or the mentor correcting the same class day. The member ledger renders events with
 * those ids as `LazyColumn` keys, and duplicate keys make Compose throw, so ids must stay unique
 * for a member's history.
 */
object HistoryEvents {

    /**
     * Keeps only the newest record per id (a later edit wins) and preserves the original
     * chronological order. Older builds could persist duplicate ids, so this also repairs data
     * that is already on disk.
     */
    fun dedupeNewest(events: List<HistoryEvent>): List<HistoryEvent> =
        events.reversed().distinctBy { it.id }.reversed()

    /**
     * Returns [event] unchanged when its id is free, otherwise a copy carrying a stable
     * `"#2"`, `"#3"`, ... suffix. The chosen id is added to [usedIds].
     */
    fun unique(event: HistoryEvent, usedIds: MutableSet<String>): HistoryEvent {
        var id = event.id
        var suffix = 1
        while (id in usedIds) {
            suffix++
            id = "${event.id}#$suffix"
        }
        usedIds += id
        return if (id == event.id) event else event.copy(id = id)
    }
}
