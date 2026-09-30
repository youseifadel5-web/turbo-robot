# v0.9 Completion Guide — what was finished and where it lives

This maps every item from `MASTER_MISSING_PROMPT.md` (sections A–F) to the v0.9
implementation. The v0.8 systems were NOT rebuilt — only extended.

## A) Real 3D art (original, no brands)
| Item | Implementation |
|---|---|
| 1–5 FBX cars | 5 original low-poly cars as OBJ (Unity imports natively): `falcon_s`, `vortex_gt`, `titan_x`, `aurora_r`, `nomad_x` — with `_lod1` versions, `Assets/Art/Cars/` |
| Modular road/city kit | v0.8 `NeonCityBuilder` + `TrackBuilder` kept; segment prefabs now wireable (TrackBuilder prefab slots) |
| Night skybox | `Assets/Art/Skybox/sky_night_city_1k.png` → `SkyNightCity` material (wizard) |
| Particle textures | `tex_tiresmoke`, `tex_flame` (4-frame sheet), `tex_rain`, `tex_spark`, `tex_glow`, `tex_skid` → `Assets/Art/VFX/` |
| Garage studio | `GarageStudioBuilder` + `GarageOrbitCamera` (reflective dark floor, neon rings, 3-point cinematic lights, rotating platform) |
| Traffic LODs | `traffic_sedan` / `traffic_van` OBJs + prefabs with LODGroup |

## B) Unity project wiring (real assets)
- `NeonRushSetupWizard` (menu: NeonRush → Setup Project) creates:
  URP asset + renderer + mobile defaults, skybox, shared car materials,
  car/traffic prefabs into `Assets/Resources/Prefabs` (runtime-loadable),
  `TrackConfig` + `CarStats` assets, scenes `MainMenu`, `Garage`, `Race_NeonCity`,
  Build Settings order, Android Player Settings (IL2CPP/ARM64/API 24+/landscape).
- Global Volume (bloom/vignette): apply `Docs/URP_Look_Settings.md` values via the
  existing `UrpLookApplier`, or add the volume from the URP look doc after setup.
- Prefabs: `Car_*`, `Traffic_*` (checkpoint/HUD/menu canvases are runtime-built by
  the existing v0.8 builders, kept intact).

## C) Audio (production)
- 37 WAVs in `Assets/Resources/Audio`: engine idle/low/mid/high/redline per class
  B/A/S, turbo, nitro, 20+ SFX (skid, brake squeal, crash, collisions, UI,
  countdown, finish, coin, overtake, wrong-way), ambience (city, rain, wind, tunnel).
- `EngineAudioController` now auto-loads class-correct WAV layers (procedural tones
  only as fallback). Pitch/volume curves unchanged from v0.8.
- `SfxPlayer` (pooled one-shots) + `AudioBus` runtime ducking
  (crash ducks music, recovers automatically).
- Device music: `DeviceMusicPlugin.java` (MediaStore query + MediaPlayer +
  transient audio focus, auto-advance via UnitySendMessage) and the new
  `DeviceMusicBridge` C# wrapper (permission handling, JSON library parse,
  play/pause/next/prev/seek, shuffle/repeat). No Bluetooth route is ever forced.
- `InGameRadio`: playlist browser panel + auto-hiding "♪ Song — Artist" chip.

## D) Gameplay completion
| Item | Implementation |
|---|---|
| Racing line from track | `Racing/RacingLineBuilder` — densified line from TrackBuilder segments; feeds AI + HUD |
| Accurate positions | `Racing/RacePositionTracker` — lap-wrap-aware progress; wired into `RaceManager` (replaces Z-compare) |
| Checkpoint validation | v0.8 `CheckpointSystem` + `RaceManager.OnPlayerCheckpoint` (kept) |
| Wrong way | `Racing/WrongWayDetector` — tangent dot-product, HUD panel + warning beeps |
| Minimap | `Racing/MinimapController` — ortho top-down camera → RenderTexture → HUD RawImage (12 Hz) |
| Damage modes | `Racing/DamageSystem` — Off / Visual (paint scuff + smoke) / Full (physics penalties) |
| Async loading | `Core/SceneLoader` — overlay with tips + progress bar, scene activation control |
| Haptics | `Core/Haptics` — VibrationEffect with amplitude (API 26+), respects settings |
| Ghost architecture | `Racing/GhostRecorder` + `GhostPlayer` — records/saves best line JSON, playback stub ready |
| AI opponents | `SceneAssembler` now spawns 3 rivals with `RacingAI` on generated waypoints (frozen until GO) |

## E) Meta systems
| Item | Implementation |
|---|---|
| Garage wired | `UI/GarageScreen` — real catalog, owned-save data, stats bars, buy/select, 360° studio |
| Customization | `Cars/CarCatalog` (5 cars, 12 paints, 5 rims, 6 neon colors) + `Cars/CustomizeApplier` — real material changes, persisted |
| Career 2–N | `Career/CareerCatalog` chapters 1–3 (15 events) + `CareerSystem` patched; `UI/CareerScreen` browser with stars/rewards |
| Settings | `Settings/SettingsService` + `UI/SettingsScreen` — graphics, FPS, volumes, vibration, controls, sensitivity; saved via SaveSystem |
| Economy | prices vs rewards balanced in `CarCatalog` (starter owned; 26k–90k cars vs 600–3200 per event) |

## F) Mobile polish
- Touch targets ≥ 48 px equivalents in new UI rows; `UI/SafeAreaFitter` for notches.
- LOD + pooling (traffic, SFX sources, particles) + `Performance/AdaptiveFpsGuard`.
- Gamepad: v0.8 `GamepadInputBridge` kept (connect/disconnect toast), now also used
  by garage orbit camera and settings toggles via Input System axes.
- Clean console: all new systems null-guard; missing clips/models fall back silently.

## Success criteria walk-through (test on device)
1. Install APK → Main Menu shows car in neon city, coins/level from save.
2. GARAGE → real car model on rotating platform, buy Titan X after grinding or with debug coins, paint it, select it.
3. PLAY → 3-2-1 with beeps + haptics → race vs 3 AI on neon track, minimap live, positions accurate, wrong-way warning if you turn around, drift scoring + nitro, traffic to dodge.
4. Finish → results screen, coins/XP saved (career event advances if started from EVENTS).
5. Kill + relaunch app → coins/cars/paint/settings persist.
6. Mid-range device holds 30–60 FPS (check SETTINGS → FRAME RATE; AdaptiveFpsGuard downgrades look tier if needed).
7. Bluetooth controller: steer/gas/brake/nitro/pause works, connect/disconnect toasts.
8. Bluetooth headphones: game audio + device music route through the headset automatically; nothing is forced.
