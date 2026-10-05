package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.WheelConfig
import com.kichikan.ak1.domain.model.WheelItem
import kotlin.random.Random

object WheelService {
    fun spin(config: WheelConfig, previousWinnerIds: Set<String> = emptySet(), random: Random = Random.Default): WheelItem? {
        val candidates = config.items.filter { it.active && (config.allowRepeatAfterWin || it.id !in previousWinnerIds) && it.weight > 0 }
        if (candidates.isEmpty()) return null
        val total = candidates.sumOf { it.weight }
        var pick = random.nextInt(total)
        for (item in candidates) {
            pick -= item.weight
            if (pick < 0) return item
        }
        return candidates.last()
    }
}
