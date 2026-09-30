# NEON RUSH RACING — v0.10

Premium Android 3D neon racing game. Unity + URP, offline-first, original assets only.

v0.10 closes the v0.9 audit gaps: real scene files, cockpit view, time-of-day
presets, reference-look HUD/garage, EN/AR localization skeleton and a
services layer (ads/IAP/leaderboards) with a real local leaderboard.
See `Docs/V10_Notes.md` and `Docs/V09_Completion_Guide.md`.

---

## Requirements
- Unity 6 (6000.x) — the car physics uses `Rigidbody.linearVelocity` (Unity 6 API).
- Android module installed (Build Support: Android SDK/NDK, OpenJDK).
- Target device: Android 7.0 (API 24) or newer, mid-range friendly.

> URP note: `Packages/manifest.json` pins URP 14.0.11 from the prototype. If Unity 6
> warns about the package version, upgrade it via Window > Package Manager
> (Universal RP). All scripts use standard URP APIs (Lit shader, Volume, URP asset).

## Open the project
1. Unity Hub → Add → select this folder → open with Unity 6.
2. Wait for import (first import compiles ~80 scripts + assets).
3. The three scenes exist in-repo: `Assets/Scenes/{MainMenu,Garage,Race_NeonCity}.unity`.

## One-click setup (REQUIRED on first open)
Menu: **NeonRush → Setup Project (one click)**. It keeps the shipped scenes and creates the rest of the project assets:
- URP pipeline asset + renderer + mobile-friendly settings (if none assigned)
- Night-city skybox material
- Shared car materials (body / glass / tires / rims / lights / additive glow)
- Car prefabs from the original OBJ meshes (LOD0 + LOD1 via LODGroup) → `Assets/Resources/Prefabs`
- Traffic prefabs
- `TrackConfig` + `CarStats` data assets
- Scenes: `MainMenu`, `Garage`, `Race_NeonCity` (added to Build Settings)
- Android Player Settings: IL2CPP, ARM64, API 24+, landscape.

Optional: **NeonRush → Add READ_MEDIA_AUDIO to Android manifest** — needed on
Android 13+ for device-music browsing in release builds.

## Play (editor)
Open `Assets/Scenes/MainMenu.unity` → Play.
- PLAY → 3-2-1 countdown → race with 3 AI rivals, leaderboard, radial speedo + nitro ring, minimap, drift score, nitro, CAM button (6 views incl. cockpit with dashboard + steering wheel).
- GARAGE → 360° studio, class badges, coins pill, carousel, RACE/CUSTOMIZE, live paint/rims/neon.
- EVENTS → career chapters 1–3, event list with stars and rewards.
- SETTINGS → graphics, FPS, volumes, vibration, controls, sensitivity, language (EN/AR).
- DEVICE MUSIC → device library browser + in-race song chip (Android only).

You can also use the original one-scene path: empty scene → add `SceneAssembler` → Play.

## Build APK / AAB
1. File → Build Settings → Android → Switch Platform.
2. Scenes are already ordered (MainMenu, Garage, Race_NeonCity).
3. Build → `NeonRushRacing.apk` (development build) — or Build App Bundle for AAB.
4. Verify Player Settings per `Docs/Android_APK_Checklist.md` (the wizard applies the critical ones).

## Controls
- Touch: on-screen steering/gas/brake (from v0.8 HUD).
- Gamepad (Input System, Bluetooth or wired): left stick = steer, RT/A = gas, LT/B = brake, X = drift, RB/L1 = nitro. Connect/disconnect toast is automatic (v0.8 bridge).
- Tilt: select in SETTINGS → CONTROLS.
- Camera: tap CAM on the race HUD to cycle Chase / Close / Far / Hood / Cockpit / Bumper.

## Audio
- 37 original synthesized WAVs (engine idle/low/mid/high per car class B/A/S, turbo,
  nitro, 20+ SFX, city/rain/wind/tunnel ambience) under `Assets/Resources/Audio`.
- Engine layers auto-assigned per car class; procedural tone fallback if missing.
- `AudioBus` runtime mixing with crash-ducking. The AudioMixer asset is optional —
  to add one: create via Window → Audio → Audio Mixer, then route the pools in
  `SfxPlayer`/`EngineAudioController`. The AudioBus already covers volumes/ducking.
- Device music streams from MediaStore URIs (no copying) via the bundled Java plugin
  `Assets/Plugins/Android/DeviceMusicPlugin.java`. Bluetooth audio routing is never
  forced — the plugin requests transient audio focus only.

## Device music permission flow
- First open of DEVICE MUSIC triggers the runtime permission request.
- If denied: message "Music access is required to select songs from your device."
  with a TRY AGAIN button. Nothing crashes; the rest of the game is unaffected.

## Performance (mid-range Android)
- URP asset: HDR off, 2x MSAA, 60 m shadow distance (wizard).
- LODs on all cars; traffic pooled (8–28 cars by density); particle count tuned.
- `AdaptiveFpsGuard` drops the look tier automatically if FPS < ~72% of target.
- FPS cap: SETTINGS → FRAME RATE (30/60/90/120).

## Folder map (new in v0.9)
- `Assets/Art/Cars` — 5 original car OBJs + LODs + 2 traffic meshes (procedurally authored, CC0-equivalent, no brands)
- `Assets/Art/VFX` — smoke / flame sheet / rain / spark / glow / skid textures
- `Assets/Art/Skybox` — generated night-city panorama
- `Assets/Resources/Audio` — full WAV bank (Engine/SFX/Ambience)
- `Assets/Shaders` — `NeonRush/AdditiveUnlit` (underglow / neon quads)
- `Assets/Plugins/Android` — DeviceMusicPlugin.java
- `Assets/Editor` — `NeonRushSetupWizard`
- New systems in `Assets/Scripts/{Racing,Core,Audio,Cars,Career,Settings,UI,Environment,Performance}`

## Known limits (honest list)
- AI rivals use the shared racing line with per-car offsets — no per-car line optimization yet.
- Engine audio layers are crossfaded synth WAVs — good, not sampled-recording quality.
- Ghost race: recorder + playback implemented, not yet surfaced as a game mode UI.
- Arabic UI: strings + switcher exist, but uGUI Text does not join Arabic letters —
  production Arabic requires a TextMeshPro + Arabic font swap (see Docs/V10_Notes.md).
- Ads / IAP / online leaderboards: architecture + local leaderboard are real;
  SDK wiring needs packages/accounts (see Docs/V10_Notes.md).
- Multiplayer/online: out of scope (architecture stays offline-first, GameSession ready).

## License
All meshes, textures, sounds and code in this project are original and procedurally
generated for NEON RUSH RACING. No third-party brands, ripped car models, or
copyrighted audio. Safe for commercial use as-is.
