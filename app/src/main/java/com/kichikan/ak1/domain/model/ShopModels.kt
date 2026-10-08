package com.kichikan.ak1.domain.model

enum class ShopItemType { AVATAR, FRAME, REWARD }

enum class AcquisitionMethod { DIRECT_PURCHASE, LEVEL_UNLOCK, WHEEL_ONLY, MANUAL, EVENT, MISSION }

enum class Currency { POINTS, DIAMONDS, NONE }

data class ShopItem(
    val id: String,
    val name: String,
    val type: ShopItemType,
    val imagePath: String? = null,
    val price: Int = 0,
    val currency: Currency = Currency.NONE,
    val minimumLevel: Int? = null,
    val methods: Set<AcquisitionMethod> = emptySet(),
    val eventStartEpochMillis: Long? = null,
    val eventEndEpochMillis: Long? = null,
    val stock: Int? = null,
    val active: Boolean = true,
    val description: String = ""
)

data class CatalogEntry(
    val itemId: String,
    val title: String,
    val description: String,
    val acquisitionText: String,
    val imagePath: String? = null
)