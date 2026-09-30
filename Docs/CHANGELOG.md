# CHANGELOG — v0.10 (from v0.9)

## Fixed
- Real scene files now ship in-repo: Assets/Scenes/{MainMenu, Garage, Race_NeonCity}.unity
  (+ stable .meta GUIDs for all scripts/folders; the Setup Wizard no longer
  overwrites existing scenes — it registers them in Build Settings instead).

## Added
- Cockpit / first-person: CAM button on the race HUD cycles 6 camera modes;
  procedural CockpitInterior (dashboard, steering wheel that turns with input,
  red neon strips) shown in Cockpit/Hood modes
- Time-of-day presets: TrackConfig.timeOfDay (Night/Sunset/Dawn/Day) drives
  sun, ambient, fog and camera background through SkyLightingBootstrap
- HUD reference look: radial speedometer (cyan ring + digital readout + gear),
  magenta nitro ring, live leaderboard (positions + names, YOU highlighted)
- Garage reference look: colored class badges (S/A/B), coins pill, car
  carousel strip, RACE / CUSTOMIZE buttons, live PAINT-RIMS-NEON cycling
  (persisted incl. new OwnedCarSave.neonId)
- Localization skeleton: L10n string table EN/AR + LANGUAGE setting
  (uGUI Arabic shaping limitation documented — TMP needed for production)
- Services layer: IAdService / IIapService / ILeaderboardService with
  No-Op stubs (editor-simulated) + REAL LocalLeaderboard (per-track best
  times auto-submitted on race finish, persisted)
- ArcadeCarController.SteeringInput exposed for cockpit wheel / HUD

---

# CHANGELOG — v0.9 (from v0.8 prototype)

## Added — assets (all original, procedurally authored)
- 5 low-poly cars (OBJ + LOD1): falcon_s, vortex_gt, titan_x, aurora_r, nomad_x
- 2 traffic meshes (sedan, van)
- VFX textures: tire smoke, 4-frame flame sheet, rain streak, spark, glow, skid mark
- Night-city skybox panorama (1k)
- 37 WAV audio bank: engine layers per class (B/A/S idle/low/mid/high/redline),
  turbo, nitro, 20+ SFX, 4 ambience loops
- URP additive shader "NeonRush/AdditiveUnlit" for underglow/neon quads

## Added — gameplay
- RacingLineBuilder: AI racing line + progress + tangents from TrackBuilder
- RacePositionTracker: accurate positions/laps (replaces Z-compare)
- WrongWayDetector with HUD + sound
- MinimapController (top-down RT → HUD)
- DamageSystem (Off/Visual/Full)
- GhostRecorder/GhostPlayer (ghost race architecture + best-line persistence)
- SceneLoader (async, tips, progress) + GameSession (cross-scene race requests)
- Haptics (amplitude-aware, settings-gated)
- AI rivals spawned by SceneAssembler (3 cars, waypoint-driven)
- AdaptiveFpsGuard (auto quality downgrade)

## Added — audio
- AudioClipRegistry (Resources WAV loading)
- SfxPlayer (pooled one-shots, bus-routed, UI sounds)
- DeviceMusicBridge (full rewrite): MediaStore library, permission flow, playback
- DeviceMusicPlugin.java (Android): MediaStore query, MediaPlayer, audio focus,
  auto-advance callbacks; no audio routing forced (BT-safe)
- InGameRadio UI (browser + song chip)

## Added — meta
- CarCatalog (data-driven cars/paints/rims/neon, prices, stats factory)
- CustomizeApplier (material-level customization, persisted)
- CareerCatalog chapters 1–3 + CareerScreen browser
- SettingsService + SettingsScreen (fully functional)
- GarageStudioBuilder + GarageOrbitCamera + GarageScreen (real 3D garage loop)
- SafeAreaFitter

## Added — editor
- NeonRushSetupWizard: one-click URP assets, materials, prefabs, scenes,
  Build Settings, Android Player Settings; optional READ_MEDIA_AUDIO manifest item

## Changed
- SceneAssembler: player car from CarModelLoader (selected car, saved paint);
  consumes GameSession; wires all advanced systems; garage toggle
- RaceManager: position tracker integration, countdown beeps + haptics, best-lap
  tracking, finish fanfare, ghost save on finish
- MainMenuFlow: GARAGE/EVENTS/SETTINGS/DEVICE MUSIC buttons now functional
- EngineAudioController: real WAV layers per car class (Start-time resolution)
- CareerSystem: catalog-driven chapters 1–3, career-complete clamp
- SaveData: +engineVolume field

## Preserved (per constraints)
- All NeonRush.* namespaces, SceneAssembler entry path, offline-first core,
  v0.8 systems untouched except the integrations listed above.
