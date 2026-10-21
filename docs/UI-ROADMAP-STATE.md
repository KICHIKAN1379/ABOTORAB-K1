# AK1 UI — work state (handoff)

This file is the resume point for any new session/model. Read it first, then `git log`
on the `ui/` branches. Business logic is unchanged by the UI passes; every pass must
end with a green `Android Build` run and a downloadable debug APK.

## Current branch

- `ui/polish` — branched from `main`, merged with `main` after the profile-pack upload.
- Version: `versionName = "1.16.0"`, `versionCode = 17`.
- Build: CI only (no local JDK/Android SDK on the current machine).

## Pass 1 (committed)

- Vazirmatn bundled in `res/font` + `ui/theme/Typography.kt` (Persian line heights, 0 tracking).
- `ui/theme/Motion.kt`: shared durations, easings, springs.
- `AK1App.kt`: `AnimatedContent` tab transitions (root = cross-fade, sub-page = directional slide).
- Press-scale on gradient buttons and clickable glass cards.
- Animated counters for the ring summary; staggered home shortcut rows.

## Pass 2 (committed)

- Floating pill bottom bar with animated tint/pill/icon scale (replaces `NavigationBar`).
- Home hero: single gradient card with ring avatar glow, offline/date chips and Jalali day tile.
- Animated aurora background in `AK1Theme` (three drifting radial glows).
- Summary numbers use `headlineMedium`; version marker in the Home footer.
- Uploaded profile packs are surfaced in the store's **جوایز (Rewards)** tab as free items.

## Pass 3 (committed)

- `ui/components/LevelProgress.kt`: animated gradient XP bar driven by `LevelRules`,
  shown under every member's header with the remaining XP to the next level.
- Member and group cards use `Modifier.animateItem()`, so adds/removes/reorders animate.
- `syncProfilePack()` now keeps name/description/type/art in sync with the file name and
  `ProfilePack.NAMES`, while leaving price/currency/methods untouched (in-app price edits survive).
- Version 1.6.0.

## Pass 4 (committed)

- Members screen: glass header card with ring stats (members/groups/total XP), live search by
  name or responsibility, "N of M" count, glass member cards instead of flat surfaces.
- Growth map ("نقشه کمال"): hero card with avatar, level/XP chips and the XP bar; the stage
  progress bar is now a gradient bar animated with `animateFloatAsState`; stage dots animate
  their fill/edge when toggled.
- Version 1.7.0.

## Pass 5 (committed)

- `ui/theme/ThemeState.kt`: `ThemeMode` presets persisted in `ak1_settings` (`theme_mode`).
- `AK1Theme`: three dark presets (Neon / Amoled / Midnight) with per-mode background brushes;
  AMOLED drops the aurora glows and uses near-black surfaces.
- `LocalAk1Background` composition local so full-screen pages (growth map, treasure) follow the
  selected preset instead of hard-coding the navy gradient.
- Settings screen: "ظاهر برنامه" filter chips, applied instantly and saved.
- Bottom bar now uses the theme surface colour, so it matches AMOLED/Midnight.
- Version 1.8.0.

## Pass 6 (committed)

- Sessions: summary card (total/upcoming/member counts); each session is a timeline card with a
  Jalali date badge, member, time and upcoming marker; list items animate with `animateItem()`.
- Attendance: per-member glass card with avatar, "N of M present" line, animated percentage chip
  and status-tinted day buttons; status colours animate with `animateColorAsState`; cards for a
  fully-present week get a green edge.
- Store: horizontal item rows with framed preview, name, type badge, free/owned/price badge,
  compact action button; list items animate; catalog shows "N items • M free".
- Version 1.9.0.

## Pass 7 (committed)

- Ranking ("رقابت"): animated podium card for the top three (2nd–1st–3rd arrangement, crown,
  medal colours, scaled entrance) plus a "rest of the table" list.
- Every remaining row shows an animated gradient bar scaled to the leader's XP/points and
  animates on reorder via `animateItem()`.
- Groups ranking reuses the same podium and bars with the group initial as avatar.
- Version 1.10.0.

## Pass 8 (committed)

- Wheel: rim glow now brightens while spinning (`animateFloatAsState`).
- After a successful spin a celebration dialog appears: falling confetti (deterministic Canvas
  particles driven by an `Animatable`), prize name, custom note, winner and a close button.
- Version 1.11.0.

## Pass 9 (committed)

- Home: "حضور هفت روز گذشته" card with seven animated bars (today highlighted), per-day counts
  and a weekly total, fed from weekly attendance records.
- Home shortcut tiles now use the shared glass Card with per-tile accent colours instead of
  opaque surfaces.
- Version 1.12.0.

## Pass 10 (committed)

- Member archive: coral summary card (count + latest archive date), live name search with
  empty-result message, glass rows with level/XP line, points/diamond chips and animated items.
- Version 1.13.0.

## Pass 11 (committed)

- Shared `PageHeader` is now a pill back button with an accent edge plus title/subtitle, so every
  inner page gets the same compact header.
- Welcome flow: enter animation on each step (fade + rise), three feature chips on the welcome
  step, and the ring setup fields grouped in a glass card.
- Version 1.14.0.

## Pass 12 (this pass)

- Settings: header card with ring name/mentor, theme chips scroll horizontally on narrow screens
  and explain that the choice is saved instantly.
- Workshop management lists: section summary card (missions active/inactive, shop items/free/
  frames, wheel prizes/active/total weight), accent-edged cards for missions and shop items,
  coloured activity status, roomier rows.
- Version 1.15.0.

## Pass 13 (this pass) — requested feature round

- Group profiles: each group card opens a profile with the leader's framed avatar, group economy,
  total XP and every member's avatar, plus shortcuts to edit/score.
- Attendance: excused/absent now require a written reason (enforced in the repository and shown
  in a stateful edit dialog).
- Treasure: history gets category chips (economy/attendance/missions/rewards/looks) and a separate
  "received rewards" tab with item art, date and reason.
- Frames: the frame hole measurement now has a 0.74 floor, so the uploaded profile frames
  (leader, Soleimani, Ramadan, Fatemiyeh, Ghadir, commando) show the avatar at the same size as
  the built-in frames instead of shrinking it.
- Shop: workshop prizes are grouped into avatars/frames/rewards; every store/workshop item has a
  poster-style high-resolution JPEG card export.
- Looks are no longer auto-equipped on gift, purchase or wheel win; they wait in the treasure.
- Ranking/member exports redrawn at higher resolution (1600px / 1200px) and saved at JPEG 100.
- Home: offline chip removed, daily quote replaces the fixed greeting line, compact summary card,
  larger shortcut icons. Settings can manage any number of daily quotes.
- Settings: phone-menu style icon grid for tools, all-files-access shortcut, archive path hint.
- Auto archive prefers `MRT/` at the root of shared storage (next to Download/DCIM) and falls back
  to `Download/MRT` when access is not granted.
- Version 1.16.0.

## Profile pack (uploaded assets)

`app/src/main/assets/art/profiles/*.png` — `avatar_<key>.png` / `frame_<key>.png`.

- `ProfilePack` parses the name; `AppRepository.syncProfilePack()` registers each file once as a
  free `ShopItem` (`price = 0`, `Currency.NONE`, `DIRECT_PURCHASE`).
- Replacing a PNG with the same name updates the art on the next app start (SHA-1 check).
- Renaming creates a new item; the old one stays until removed in the shop.
- Persian display names live in `ProfilePack.NAMES`; unknown keys fall back to the file key.
- Store → Avatars/Frames shows them in their natural tabs; Store → Rewards also lists every
  `profile-*` item (see `StoreScreen.kt`).

## Next steps

1. **Level 2 remainder:** animated ranking bars, attendance check-in animations,
   empty-state illustrations, optional shimmer.
2. **Level 3:** redesign Members/profile/sessions/store layouts, light + AMOLED themes,
   theme choice stored in `ak1_settings`.
3. **Level 4:** shared-element transitions, predictive back, M3 Expressive components.
4. Optional: configure `AK1_KEYSTORE_BASE64` / `AK1_KEYSTORE_PASSWORD` secrets so CI APKs
   install over previous builds and keep local data. Until then, export a backup, uninstall,
   install, import.

## Verify a build

```sh
git push origin <branch>
# GitHub → Actions → Android Build → Artifacts → ABOTORAB-K1-debug-apk
```

APK signing: without the secrets above, CI uses a per-run debug key.
