package com.kichikan.ak1.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.R
import com.kichikan.ak1.data.AppRepository
import com.kichikan.ak1.domain.model.*
import com.kichikan.ak1.ui.GrowthMapPage
import com.kichikan.ak1.ui.MemberArchivePage
import com.kichikan.ak1.ui.SpinWheelPage
import com.kichikan.ak1.ui.theme.AppIcon
import com.kichikan.ak1.ui.theme.Motion
import com.kichikan.ak1.ui.theme.Neon

@Composable
internal fun AK1App(repo: AppRepository) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    var ringReady by remember { mutableStateOf(repo.ring != null) }
    var growthMemberId by remember { mutableStateOf<String?>(null) }
    fun changed() { refresh++ }
    if (!ringReady) { WelcomeScreen(repo) { ringReady = true }; return }
    androidx.activity.compose.BackHandler(enabled = tab != Tab.HOME) { tab = parentOf(tab) }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { AK1BottomBar(current = tab, onSelect = { tab = it }) }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { tabTransition(initialState, targetState) },
            label = "ak1-screen"
        ) { targetTab ->
            key(refresh) {
                when (targetTab) {
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
}

/**
 * Root tabs cross-fade; deeper pages slide in the reading direction (RTL: forward
 * comes from the left, back goes out to the right) with a short travel distance so
 * the animation never feels like a full carousel.
 */
private fun tabTransition(from: Tab, to: Tab): ContentTransform {
    val rootToRoot = from.depth == 0 && to.depth == 0
    return if (rootToRoot) {
        (fadeIn(tween(Motion.Medium, easing = Motion.Standard)) +
            scaleIn(initialScale = 0.985f, animationSpec = tween(Motion.Medium, easing = Motion.Emphasized))) togetherWith
            fadeOut(tween(Motion.Fast))
    } else {
        val forward = to.depth >= from.depth
        val enterSign = if (forward) -1 else 1
        val exitSign = -enterSign
        (fadeIn(tween(Motion.Medium, easing = Motion.Standard)) +
            slideInHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { width -> width * enterSign / 6 }) togetherWith
            (fadeOut(tween(Motion.Fast)) +
                slideOutHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { width -> width * exitSign / 8 })
    }
}

/** 0 = root tab, 1 = section, 2 = sub-page of a section. */
private val Tab.depth: Int
    get() = when (this) {
        Tab.HOME, Tab.MEMBERS, Tab.SERVICES, Tab.SETTINGS -> 0
        Tab.GROWTH, Tab.ARCHIVE -> 1
        Tab.WORKSHOP_MISSIONS, Tab.WORKSHOP_PRIZES, Tab.WORKSHOP_ASSETS, Tab.WORKSHOP_WHEEL,
        Tab.RANK_MEMBERS, Tab.RANK_GROUPS,
        Tab.STORE_AVATARS, Tab.STORE_FRAMES, Tab.STORE_REWARDS,
        Tab.SESSIONS_LIST, Tab.SESSIONS_ATTENDANCE -> 2
        else -> 1
    }

@Composable
private fun AK1BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val items = listOf(
        Tab.HOME to "خانه",
        Tab.MEMBERS to "اعضا",
        Tab.SERVICES to "خدمات",
        Tab.SETTINGS to "تنظیمات"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    18.dp,
                    RoundedCornerShape(28.dp),
                    ambientColor = Neon.Cyan.copy(alpha = 0.20f),
                    spotColor = Neon.Violet.copy(alpha = 0.24f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = Neon.Navy.copy(alpha = 0.92f),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        Neon.Cyan.copy(alpha = 0.22f),
                        Neon.Violet.copy(alpha = 0.18f)
                    )
                )
            )
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
                items.forEach { (target, label) ->
                    val selected = current == target ||
                        (target == Tab.SERVICES && current !in listOf(Tab.HOME, Tab.MEMBERS, Tab.SETTINGS))
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.92f,
                        animationSpec = Motion.Selection,
                        label = "nav-scale-$label"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (selected) Neon.Cyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(Motion.Medium, easing = Motion.Standard),
                        label = "nav-tint-$label"
                    )
                    val pill by animateColorAsState(
                        targetValue = if (selected) Neon.Cyan.copy(alpha = 0.13f) else Color.Transparent,
                        animationSpec = tween(Motion.Medium, easing = Motion.Standard),
                        label = "nav-pill-$label"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pill)
                            .clickable(role = Role.Tab) { onSelect(target) }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppIcon(
                            when (target) {
                                Tab.HOME -> R.drawable.icon_nav_home
                                Tab.MEMBERS -> R.drawable.icon_nav_members
                                Tab.SERVICES -> R.drawable.icon_nav_services
                                else -> R.drawable.icon_nav_settings
                            },
                            size = 26.dp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
