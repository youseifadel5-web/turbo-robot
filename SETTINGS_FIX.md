# Settings screen layout fix (v25.4)

## Symptom
Opening the **Settings** tab looked "shrunk" and out of line with the rest of the
player: the header buttons (menu on the left, theme button on the right) sat
further inside the screen than the cards below, and the top rows looked
separated/misaligned compared with Home / Channels / Films / Radio.

## Root cause
`ui/settings/SettingsScreen.kt` put the `PlayerTopBar` **inside** the screen's
`LazyColumn`, and that `LazyColumn` had `Modifier.padding(horizontal = 20.dp)`.
`PlayerTopBar` already applies its own `18.dp` inner padding, so the header got
`18 + 20 = 38.dp` on each side instead of `18.dp`. Every other screen places
`PlayerTopBar` full-bleed, so only Settings was indented an extra 20.dp per side
— the header looked narrower and its buttons did not line up with the cards.

## Fix
1. `SettingsScreen.kt`
   - Moved `PlayerTopBar` **out** of the `LazyColumn` into a wrapping `Column`,
     so it spans the full width with only its own 18.dp padding (same as every
     other screen).
   - The `LazyColumn` now uses `Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)`
     and `contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)`, so the
     cards keep their 20.dp side padding and the title sits snug under the header
     (4.dp) instead of being pushed away.
2. `SettingsScreen.kt` — the header menu button was drawn but **disabled**
   (`onMenu = null`), so it looked active but did nothing. Added an `onOpenMenu`
   parameter and wired it to the same quick drawer used on Home.
3. `MainScreen.kt` — passed `onOpenMenu = { showQuickDrawer = true }` into
   `SettingsScreen`.

No feature, screen, or setting was removed — only the Settings top area layout
and the menu button wiring changed.

## Rebuild
```
bash build_release.sh
# or
./gradlew clean :app:assembleRelease
```
