# AK1 UI — work state (handoff)

This file is the resume point for any new session/model. Read it first, then `git log`
on the `ui/` branches. Business logic is unchanged by the UI passes; every pass must
end with a green `Android Build` run and a downloadable debug APK.

## Current branch

- `ui/polish` — branched from `main`, merged with `main` after the profile-pack upload.
- Version: `versionName = "1.5.0"`, `versionCode = 6`.
- Build: CI only (no local JDK/Android SDK on the current machine).

## Pass 1 (committed)

- Vazirmatn bundled in `res/font` + `ui/theme/Typography.kt` (Persian line heights, 0 tracking).
- `ui/theme/Motion.kt`: shared durations, easings, springs.
- `AK1App.kt`: `AnimatedContent` tab transitions (root = cross-fade, sub-page = directional slide).
- Press-scale on gradient buttons and clickable glass cards.
- Animated counters for the ring summary; staggered home shortcut rows.

## Pass 2 (this pass)

- Floating pill bottom bar with animated tint/pill/icon scale (replaces `NavigationBar`).
- Home hero: single gradient card with ring avatar glow, offline/date chips and Jalali day tile.
- Animated aurora background in `AK1Theme` (three drifting radial glows).
- Summary numbers use `headlineMedium`; version marker in the Home footer.
- Uploaded profile packs are surfaced in the store's **جوایز (Rewards)** tab as free items.

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

1. **Level 2 remainder:** shimmer/skeleton for first paint, animate list item add/remove,
   animated XP progress ring, pull-to-refresh.
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
