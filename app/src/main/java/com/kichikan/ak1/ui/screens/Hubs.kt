package com.kichikan.ak1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kichikan.ak1.domain.model.*
import androidx.compose.ui.unit.dp
import com.kichikan.ak1.R
import com.kichikan.ak1.ui.theme.AppIcon
import com.kichikan.ak1.ui.theme.Neon
import com.kichikan.ak1.ui.HubEntry
import com.kichikan.ak1.ui.HubScreen
import com.kichikan.ak1.data.AppRepository

@Composable
internal fun ServicesScreen(padding: PaddingValues, open: (Tab) -> Unit) {
    HubScreen(
        padding, "خدمات", "ابزارهای مدیریت حلقه در یک‌جا",
        listOf(
            HubEntry("تراشکاری", "مأموریت‌ها، جوایز، آواتار و قاب", Neon.Cyan, { AppIcon(R.drawable.icon_services_workshop, 62.dp) }) { open(Tab.WORKSHOP) },
            HubEntry("جلسات", "فهرست جلسه‌ها و حضور و غیاب", Neon.Coral, { AppIcon(R.drawable.icon_services_sessions, 62.dp) }) { open(Tab.SESSIONS) },
            HubEntry("رقابت", "رتبه‌بندی اعضا و گروه‌ها", Neon.Violet, { AppIcon(R.drawable.icon_services_competition, 62.dp) }) { open(Tab.RANKING) },
            HubEntry("فروشگاه", "آواتارها، قاب‌ها و جوایز", Neon.Gold, { AppIcon(R.drawable.icon_services_store, 62.dp) }) { open(Tab.STORE) },
            HubEntry("گردونه", "گردونه‌ی شانس و جایزه", Neon.Sky, { AppIcon(R.drawable.icon_services_wheel, 62.dp) }) { open(Tab.WHEEL) }
        )
    )
}

@Composable
internal fun WorkshopHub(
    padding: PaddingValues,
    repo: AppRepository,
    changed: () -> Unit,
    open: (Tab) -> Unit,
    back: () -> Unit
) {
    var identityOpen by remember { mutableStateOf(false) }
    HubScreen(
        padding, "تراشکاری", "ساخت و مدیریت مأموریت، جوایز و شخصی‌سازی",
        listOf(
            HubEntry("شناسنامه", "ثبت امتیاز، الماس، XP و سطح", Neon.Green, { AppIcon(R.drawable.icon_workshop_identity, 62.dp) }) { identityOpen = true },
            HubEntry("مأموریت‌ها", "ساخت و ثبت انجام مأموریت", Neon.Cyan, { AppIcon(R.drawable.icon_workshop_missions, 62.dp) }) { open(Tab.WORKSHOP_MISSIONS) },
            HubEntry("جوایز فروشگاه", "ساخت و ویرایش آیتم‌ها", Neon.Gold, { AppIcon(R.drawable.icon_workshop_store_rewards, 62.dp) }) { open(Tab.WORKSHOP_PRIZES) },
            HubEntry("آواتار و قاب", "افزودن تصویر سفارشی", Neon.Violet, { AppIcon(R.drawable.icon_workshop_avatar_frame, 62.dp) }) { open(Tab.WORKSHOP_ASSETS) },
            HubEntry("تنظیم گردونه", "جوایز، وزن و هزینه", Neon.Sky, { AppIcon(R.drawable.icon_workshop_wheel_settings, 62.dp) }) { open(Tab.WORKSHOP_WHEEL) }
        ),
        back
    )
    if (identityOpen) EconomyDialog(repo, changed) { identityOpen = false }
}

@Composable
internal fun RankingHub(padding: PaddingValues, open: (Tab) -> Unit, back: () -> Unit) {
    HubScreen(
        padding, "رقابت", "رتبه‌ی اعضا بر اساس سطح و XP؛ رتبه‌ی گروه‌ها با امتیاز مستقل گروه",
        listOf(
            HubEntry("رتبه اعضا", "با آواتار و قاب هر عضو", Neon.Gold, { AppIcon(R.drawable.icon_competition_member_ranking, 62.dp) }) { open(Tab.RANK_MEMBERS) },
            HubEntry("رتبه گروه‌ها", "امتیاز و الماس گروه", Neon.Violet, { AppIcon(R.drawable.icon_competition_group_ranking, 62.dp) }) { open(Tab.RANK_GROUPS) }
        ),
        back
    )
}

@Composable
internal fun StoreHub(padding: PaddingValues, open: (Tab) -> Unit, back: () -> Unit) {
    HubScreen(
        padding, "فروشگاه", "آواتار، قاب و جایزه",
        listOf(
            HubEntry("آواتارها", "دریافت و فعال‌سازی آواتار", Neon.Cyan, { AppIcon(R.drawable.icon_store_avatars, 62.dp) }) { open(Tab.STORE_AVATARS) },
            HubEntry("قاب‌ها", "دریافت و فعال‌سازی قاب", Neon.Violet, { AppIcon(R.drawable.icon_store_frames, 62.dp) }) { open(Tab.STORE_FRAMES) },
            HubEntry("جوایز", "جوایز قابل دریافت", Neon.Gold, { AppIcon(R.drawable.icon_store_rewards, 62.dp) }) { open(Tab.STORE_REWARDS) }
        ),
        back
    )
}

@Composable
internal fun SessionsHub(padding: PaddingValues, open: (Tab) -> Unit, back: () -> Unit) {
    HubScreen(
        padding, "جلسات", "جلسه‌ها و حضور و غیاب مستقل از هم‌اند",
        listOf(
            HubEntry("فهرست جلسات", "ثبت و مشاهده جلسه‌ها", Neon.Coral, { AppIcon(R.drawable.icon_sessions_list, 62.dp) }) { open(Tab.SESSIONS_LIST) },
            HubEntry("حضور و غیاب", "تقویم هفتگی و ماهانه", Neon.Green, { AppIcon(R.drawable.icon_sessions_attendance, 62.dp) }) { open(Tab.SESSIONS_ATTENDANCE) }
        ),
        back
    )
}
