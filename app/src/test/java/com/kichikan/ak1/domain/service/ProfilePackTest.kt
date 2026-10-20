package com.kichikan.ak1.domain.service

import com.kichikan.ak1.domain.model.ShopItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfilePackTest {
    @Test fun frameAndAvatarFilesAreRecognised() {
        val frame = ProfilePack.parse("frame_ghadir.png")
        assertEquals(ShopItemType.FRAME, frame?.type)
        assertEquals("ghadir", frame?.key)
        val avatar = ProfilePack.parse("avatar_commando.png")
        assertEquals(ShopItemType.AVATAR, avatar?.type)
        assertEquals("commando", avatar?.key)
    }

    @Test fun casingFromWindowsIsNormalised() {
        val entry = ProfilePack.parse("Frame_Zulfiqar.PNG")
        assertEquals(ShopItemType.FRAME, entry?.type)
        assertEquals("zulfiqar", entry?.key)
        assertEquals("profile-frame-zulfiqar", entry?.itemId)
    }

    @Test fun multiWordKeysAreAllowed() {
        val entry = ProfilePack.parse("avatar_my_hero_2.png")
        assertEquals("my_hero_2", entry?.key)
    }

    @Test fun otherFilesAreIgnored() {
        listOf(
            "README.md", "frame.png", "frame_.png", "avatar_x.jpg", "avatar_x.png.txt",
            "frame_ghadir (1).png", "frame__ghadir.png", "frame_ghadir_.png", "icon_nav_home.png", ".gitkeep", "تکاور.png"
        ).forEach { assertNull("should be ignored: $it", ProfilePack.parse(it)) }
    }

    @Test fun knownKeysGetPersianNamesInTheExistingPattern() {
        assertEquals("قاب ذوالفقار", ProfilePack.parse("frame_zulfiqar.png")?.displayName)
        assertEquals("آواتار تکاور", ProfilePack.parse("avatar_commando.png")?.displayName)
        assertEquals("آواتار شهید قاسم سلیمانی", ProfilePack.parse("avatar_soleimani.png")?.displayName)
    }

    @Test fun everyListedNameHasAUsableKey() {
        ProfilePack.NAMES.forEach { (key, name) ->
            assertNotNull("avatar_$key.png", ProfilePack.parse("avatar_$key.png"))
            assertNotNull("frame_$key.png", ProfilePack.parse("frame_$key.png"))
            assertEquals(false, name.isBlank())
        }
        assertEquals(8, ProfilePack.NAMES.size)
    }

    @Test fun unknownKeyFallsBackToTheKey() {
        assertEquals("قاب my hero", ProfilePack.parse("frame_my_hero.png")?.displayName)
    }

    @Test fun sameNameAsAvatarAndFrameGetsDifferentIds() {
        val avatar = ProfilePack.parse("avatar_army.png")!!
        val frame = ProfilePack.parse("frame_army.png")!!
        assertNotEquals(avatar.itemId, frame.itemId)
        assertEquals("profile-avatar-army", avatar.itemId)
        assertEquals("profile-frame-army", frame.itemId)
    }
}
