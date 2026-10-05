package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.Mission
import com.kichikan.ak1.domain.model.MissionCompletion

object MissionService {
    fun canComplete(mission: Mission, now: Long): Boolean {
        if (!mission.active) return false
        if (mission.startAt != null && now < mission.startAt) return false
        if (mission.endAt != null && now > mission.endAt) return false
        return mission.xpReward >= 0 && mission.pointsReward >= 0 && mission.diamondReward >= 0
    }

    fun completion(mission: Mission, memberIds: List<String>, now: Long, reason: String? = null): MissionCompletion {
        require(memberIds.isNotEmpty())
        return MissionCompletion("completion-" + now + "-" + mission.id, mission.id, memberIds.distinct(), now, reason)
    }
}
