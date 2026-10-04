# Youseif Player Pro — v25.4 fixes (settings layout · Cast picker · Cast proxy)

## 1) Settings screen looked "shrunk" / out of line with the rest of the app
`app/src/main/java/com/example/ui/settings/SettingsScreen.kt` (+ `ui/MainScreen.kt`)

Root cause: `PlayerTopBar` was placed INSIDE the screen's `LazyColumn`, which had
`Modifier.padding(horizontal = 20.dp)`. `PlayerTopBar` already applies its own 18.dp
inner padding, so the header got 18 + 20 = 38.dp per side instead of 18.dp. Every
other screen places the top bar full-bleed, so only Settings was indented an extra
20.dp per side and its buttons did not line up with the cards below.
Fix: moved the top bar out into a wrapping `Column` (full-bleed), list now uses
`fillMaxWidth().weight(1f).padding(horizontal = 20.dp)` with
`contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)`; also wired the header
menu button (`onOpenMenu`) to the quick drawer instead of leaving it disabled.

## 2) Cast picker never opened — "background can not be translucent: #0"
`app/src/main/java/com/example/player/CastHelper.kt`, `app/src/main/res/values/themes.xml`

`MediaRouteChooserDialog` is an AppCompatDialog and `MediaRouterThemeHelper` reads the
theme's `colorPrimary`; the app's base theme (`android:Theme.Material.Light.NoActionBar`)
is not AppCompat and defines no appcompat `colorPrimary`, so it read `#0` and
`ColorUtils.calculateContrast` threw. Fix: added an AppCompat dialog theme with an
OPAQUE `colorPrimary` (`Theme.Youseif.CastDialog`) and pass it to the chooser.

## 3) Casting didn't send the video to the TV (works only on the phone)
Files: NEW `app/src/main/java/com/example/player/CastProxy.kt`,
`CastHelper.kt`, `player/YouseifPlayerController.kt`,
`ui/components/VideoPlayerView.kt`

Root cause: the app handed the raw stream URL to the TV. The TV fetches the URL itself
and sends NO `Referer` / `User-Agent`, so IPTV streams that need those headers returned
403 on the TV while working on the phone (the phone player DOES send them —
`playUrl(..., referer, userAgent)`).

Fix: added a small **local HTTP proxy** (`CastProxy`) that runs on the phone while
casting:
- It fetches the upstream stream WITH the same `Referer` / `User-Agent` the phone uses.
- It re-serves it to the TV on the LAN: `http://<phone-lan-ip>:<port>/stream?u=…&r=…&ua=…`
- HLS `.m3u8` playlists are rewritten so segments, keys (`EXT-X-KEY`) and maps
  (`EXT-X-MAP`) also go through the proxy (otherwise the TV would fetch them directly).
- Port auto-picks 8899–8910; LAN IP prefers Wi‑Fi/Ethernet over VPN tunnels.
- `CastHelper` now builds the proxy URL and forwards referer/user-agent; the proxy is
  stopped when the cast session ends. Falls back to the raw URL if proxying is impossible.

Wiring: `YouseifPlayerController.getCurrentReferer()/getCurrentUserAgent()` expose the
current stream's headers; the cast button in `VideoPlayerView` passes them in.

### Notes / limits
- Phone and TV must be on the same Wi‑Fi, and a VPN can block device discovery (mDNS).
  Turn the VPN off while casting if no devices appear.
- The proxy forwards headers but not DRM; streams that need a login cookie will still fail.
- Rebuild and test on a real device.

## Rebuild
```
bash build_release.sh
# or
./gradlew clean :app:assembleRelease
```
