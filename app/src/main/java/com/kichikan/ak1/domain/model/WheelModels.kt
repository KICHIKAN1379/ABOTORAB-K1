package com.kichikan.ak1.domain.model

enum class WheelMode { FREE, POINTS, POINTS_AND_DIAMONDS }

enum class WheelRewardType {
    POINTS,
    DIAMONDS,
    AVATAR,
    FRAME,
    REWARD,
    CUSTOM
}

data class WheelItem(
    val id: String,
    val title: String,
    val type: WheelRewardType,
    val amount: Int? = null,
    val shopItemId: String? = null,
    val customText: String? = null,
    val weight: Int = 1,
    val active: Boolean = true
)

data class WheelConfig(
    val mode: WheelMode = WheelMode.FREE,
    val spinCostPoints: Int = 0,
    val spinCostDiamonds: Int = 0,
    val freeSpin: Boolean = true,
    val items: List<WheelItem> = emptyList(),
    val allowRepeatAfterWin: Boolean = false
)
