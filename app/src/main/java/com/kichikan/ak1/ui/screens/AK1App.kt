package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.R
import com.kichikan.ak1.ui.theme.AppIcon
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.GrowthMapPage
import com.kichikan.ak1.ui.MemberArchivePage
import com.kichikan.ak1.ui.SpinWheelPage
import com.kichikan.ak1.data.AppRepository

@Composable
internal fun AK1App(repo: AppRepository) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    var ringReady by remember { mutableStateOf(repo.ring != null) }
    var growthMemberId by remember { mutableStateOf<String?>(null) }
    fun changed() { refresh++ }
    if (!ringReady) { WelcomeScreen(repo) { ringReady = true }; return }
    androidx.activity.compose.BackHandler(enabled = tab != Tab.HOME) { tab = parentOf(tab) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, bottomBar = {
        NavigationBar(containerColor = Neon.Navy.copy(alpha = 0.88f)) {
            listOf(Tab.HOME to "خانه", Tab.MEMBERS to "اعضا", Tab.SERVICES to "خدمات", Tab.SETTINGS to "تنظیمات").forEach { (t, label) ->
                val selected = tab == t || (t == Tab.SERVICES && tab !in listOf(Tab.HOME, Tab.MEMBERS, Tab.SETTINGS))
                NavigationBarItem(selected = selected,
                    onClick = { tab = t },
                    icon = {
                        AppIcon(
                            when (t) {
                                Tab.HOME -> R.drawable.icon_nav_home
                                Tab.MEMBERS -> R.drawable.icon_nav_members
                                Tab.SERVICES -> R.drawable.icon_nav_services
                                else -> R.drawable.icon_nav_settings
                            },
                            28.dp
                        )
                    },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Neon.Cyan,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Neon.Cyan.copy(alpha = 0.16f)
                    ))
            }
        }
    }) { padding ->
        key(refresh) {
            when (tab) {
                Tab.HOME -> HomeScreen(repo, padding, { tab = Tab.MEMBERS }, { changed() }) { tab = it }
                Tab.MEMBERS -> MembersScreen(repo, padding, { changed() }) { member ->
                    growthMemberId = member.id
                    tab = Tab.GROWTH
                }
                Tab.GROWTH -> {
                    val growthMember = repo.members.firstOrNull { it.id == growthMemberId }
                    if (growthMember != null) {
                        GrowthMapPage(repo, growthMember, padding) { tab = Tab.MEMBERS }
                    } else {
                        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            Text("عضو پیدا نشد.")
                        }
                    }
                }
                Tab.SERVICES -> ServicesScreen(padding) { tab = it }
                Tab.WORKSHOP -> WorkshopHub(padding, repo, { changed() }, { tab = it }) { tab = Tab.SERVICES }
                Tab.RANKING -> RankingHub(padding, { tab = it }) { tab = Tab.SERVICES }
                Tab.STORE -> StoreHub(padding, { tab = it }) { tab = Tab.SERVICES }
                Tab.SESSIONS -> SessionsHub(padding, { tab = it }) { tab = Tab.SERVICES }
                Tab.WORKSHOP_MISSIONS -> WorkshopScreen(repo, padding, { changed() }, 0) { tab = Tab.WORKSHOP }
                Tab.WORKSHOP_PRIZES -> WorkshopScreen(repo, padding, { changed() }, 1) { tab = Tab.WORKSHOP }
                Tab.WORKSHOP_ASSETS -> WorkshopScreen(repo, padding, { changed() }, 2) { tab = Tab.WORKSHOP }
                Tab.WORKSHOP_WHEEL -> WorkshopScreen(repo, padding, { changed() }, 3) { tab = Tab.WORKSHOP }
                Tab.WHEEL -> SpinWheelPage(repo, padding)
                Tab.RANK_MEMBERS -> RankingScreen(repo, padding, false) { tab = Tab.RANKING }
                Tab.RANK_GROUPS -> RankingScreen(repo, padding, true) { tab = Tab.RANKING }
                Tab.STORE_AVATARS -> StoreScreen(repo, padding, ShopItemType.AVATAR, { changed() }) { tab = Tab.STORE }
                Tab.STORE_FRAMES -> StoreScreen(repo, padding, ShopItemType.FRAME, { changed() }) { tab = Tab.STORE }
                Tab.STORE_REWARDS -> StoreScreen(repo, padding, ShopItemType.REWARD, { changed() }) { tab = Tab.STORE }
                Tab.SESSIONS_LIST -> SessionsScreen(padding, repo, { changed() }, false) { tab = Tab.SESSIONS }
                Tab.SESSIONS_ATTENDANCE -> SessionsScreen(padding, repo, { changed() }, true) { tab = Tab.SESSIONS }
                Tab.SETTINGS -> SettingsScreen(repo, padding, { changed() }) { tab = Tab.ARCHIVE }
                Tab.ARCHIVE -> MemberArchivePage(repo, padding) { tab = Tab.SETTINGS }
            }
        }
    }
}
