package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.ShopItemType

/**
 * The "profile pack": avatar and frame PNGs the mentor drops into `assets/art/profiles` on GitHub.
 *
 * A file called `avatar_<key>.png` becomes a free avatar and `frame_<key>.png` a free frame, shown in
 * the shop and in every member's treasure. The key is lowercase latin letters and digits, with
 * single underscores between words (`frame_ghadir.png`, `avatar_my_hero.png`). This object only
 * holds the naming rules, so they can be unit-tested without Android.
 */
object ProfilePack {
    /** Folder inside the APK's assets where the PNGs live. */
    const val ASSET_DIR = "art/profiles"

    data class Entry(val type: ShopItemType, val key: String) {
        private val prefix get() = if (type == ShopItemType.FRAME) "frame" else "avatar"

        /** Stable shop-item id; the same file name always maps to the same item. */
        val itemId: String get() = "profile-$prefix-$key"

        /** Name shown in the app, following the existing "آواتار رهرو" / "قاب طلایی" pattern. */
        val displayName: String
            get() = (if (type == ShopItemType.FRAME) "قاب " else "آواتار ") + (NAMES[key] ?: key.replace('_', ' '))

        val description: String
            get() = if (type == ShopItemType.FRAME) "قاب ویژه؛ آواتار عضو داخل آن قرار می‌گیرد."
            else "آواتار ویژه؛ عضو می‌تواند آن را برای پروفایل خود انتخاب کند."
    }

    /** Persian names for the known keys. An unknown key still works and shows the key itself. */
    val NAMES: Map<String, String> = mapOf(
        "leader" to "رهبر شهید",
        "soleimani" to "شهید قاسم سلیمانی",
        "zulfiqar" to "ذوالفقار",
        "ramadan" to "ماه رمضان",
        "ghadir" to "غدیر",
        "fatemiyeh" to "فاطمیه",
        "commando" to "تکاور",
        "army" to "ارتشی"
    )

    private val FILE = Regex("^(avatar|frame)_([a-z0-9]+(?:_[a-z0-9]+)*)\\.png$", RegexOption.IGNORE_CASE)

    /** Reads a file name; returns null for anything that is not `avatar_<key>.png` / `frame_<key>.png`. */
    fun parse(fileName: String): Entry? {
        val match = FILE.matchEntire(fileName) ?: return null
        val type = if (match.groupValues[1].lowercase() == "frame") ShopItemType.FRAME else ShopItemType.AVATAR
        return Entry(type, match.groupValues[2].lowercase())
    }
}
