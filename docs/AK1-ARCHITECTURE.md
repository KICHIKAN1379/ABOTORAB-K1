# AK1 Architecture — Foundation

## 1. Scope

AK1 is a mentor application. Each mentor has access only to their own ring.

```
Mentor
  └── Ring
      ├── Members
      ├── Sessions
      ├── Attendance
      ├── Missions
      ├── Economy
      ├── Shop / Catalog
      └── History
```

The ring name is user-defined at registration. No ring name such as "Abotorab 3" is hard-coded into the general UI.

## 2. Three separate values

### XP
Permanent progression value.
- Earned through mentor-recorded positive/negative events and defined activities.
- Determines level and member ranking.
- Spending points or diamonds never reduces XP.
- Manual level decrease also decreases XP so the member remains consistent with the requested level.

### Spendable points
Currency for mentor-defined real/educational rewards.
- Can be spent in the shop.
- Spending does not change XP, level, or ranking.

### Diamonds
Currency for personalization.
- Used for avatars and frames.
- Not deducted from XP or spendable points.
- Default rewards:
  - 10 diamonds for every newly reached level.
  - 30 diamonds for birthday.
  - configurable diamonds for individual/group missions.
  - configurable manual diamond grants with a required reason.
  - optional wheel rewards.

## 3. Level formula

- Level 0: 0–19 XP
- Level 1: 20–49 XP
- Level 2: 50–79 XP
- Level 3: 80–109 XP
- Level 4: 110–139 XP
- Level 5: 140–169 XP
- etc.

Formula:
`level = 0` for XP < 20; otherwise `1 + floor((XP - 20) / 30)`.

Manual level decrease must record:
- previous level
- new level
- XP removed
- required reason
- timestamp
- mentor

## 4. Shop

Main categories:
- Avatars
- Frames
- Rewards
- Wheel

Avatars and frames support a "personal creation" area for mentor-uploaded assets.

Recommended source size:
- Avatar PNG: 256×256 px, transparent background preferred.
- Frame PNG: 256×256 px, transparent center/background preferred.

Future:
- Animated GIF frames are planned.
- The data model must not assume frames are static PNG only.

Each shop item can independently define:
- price
- currency
- minimum level
- acquisition method
- event start/end
- stock
- active state

Acquisition methods include direct purchase, level unlock, wheel-only, manual, event, and mission.

## 5. Wheel

The wheel is free to spin.

The mentor defines its contents manually, for example:
- +10 points
- +5 diamonds
- free trip
- half-price tuition
- avatar
- frame
- custom reward

Wheel outcomes are recorded in member history.

The wheel does not require points or diamonds to spin.

## 6. Catalog

Every reward/shop item can have:
- image
- title
- description
- acquisition instructions

The catalog can later be exported as JPEG for sharing with members. The exported image should show both the reward image and how it can be obtained.

## 7. History

Every member has a permanent history.

History must be grouped:
- Year
  - Month
    - Events

It must support filtering by event type (XP, points, diamonds, wheel, missions, purchases, etc.) so the mentor does not need to scroll through a long unstructured timeline.

Each event stores date/time, amount when applicable, reason when applicable, source, mentor, and metadata.

## 8. Main navigation

- Home
- Members
- تراشکاری
- Sessions
- Competition (ranking only)
- Store
- Settings

Home contains mentor-selectable shortcuts.

## 9. Offline / online

Offline:
- no password required after local setup
- export ring data from Settings
- import the versioned export later to restore the ring

Online:
- ring name + ring username + password are used for authentication
- mentor sees only their own ring

Exports must be versioned for future migrations.
