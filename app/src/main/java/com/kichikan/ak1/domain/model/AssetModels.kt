package com.kichikan.ak1.domain.model

enum class AssetType { AVATAR, FRAME, REWARD_IMAGE }

data class CustomAsset(
    val id: String,
    val name: String,
    val type: AssetType,
    val path: String,
    val mimeType: String = "image/png",
    val width: Int? = null,
    val height: Int? = null,
    val active: Boolean = true
)
