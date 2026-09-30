# MASTER PROMPT — Complete everything still missing in NEON RUSH RACING

Copy-paste this prompt to continue development (Unity human team or AI assistant with Unity access).

---

You are continuing **NEON RUSH RACING**, a premium Android 3D racing game (Unity + URP).

## Already done (do NOT rebuild from zero)
- Architecture folders + ~48 C# systems
- Arcade car physics, drift, nitro, trails, drift score
- Procedural car + neon city + wet road + lamps (runtime)
- SceneAssembler one-click playable gray-box
- RaceManager lifecycle, checkpoints hooks, results data
- AI waypoints, traffic pool skeletons
- Save v2, career chapter 1, upgrades affecting physics
- Gamepad Input System bridge + connect toast
- Engine audio layers (procedural tones)
- Runtime Main Menu → Race HUD → Results loop
- UI SVG art bible + URP look docs + APK checklist
- Weather grip + day/night controllers

## Your job: finish everything still MISSING for a shippable premium feel

### A) Real 3D art (highest visual priority)
1. Replace procedural car with 1–5 original/licensed FBX cars (no ripped brands).
2. Modular neon city road kit: asphalt, barriers, sidewalks, tunnels, bridges, checkpoints gates.
3. Skybox / URP sky volume for night city + optional sunset highway.
4. Particle textures: soft tire smoke, nitro flame sheets, rain, sparks.
5. Garage studio scene: reflective floor, neon rings, orbit camera, car carousel thumbnails.
6. Traffic car LODs (simple meshes).

### B) Unity project wiring (must be real .unity assets)
1. Create scenes: `MainMenu`, `Garage`, `Race_NeonCity`, `Results` (or single scene with additive load).
2. URP Asset + Renderer + Global Volume (Bloom/Vignette/Color per Docs/URP_Look_Settings.md).
3. Prefabs: PlayerCar, AICar, TrafficCar, Checkpoint, HUD Canvas, Menu Canvas.
4. Addressables or Resources for cars/tracks.
5. Build Settings scenes ordered; Android Player Settings from Docs/Android_APK_Checklist.md.

### C) Audio (production)
1. Real multi-layer engine WAV/OGG per car class (idle/low/mid/high/redline + turbo + nitro).
2. AudioMixer groups: Master, Music, SFX, Engine, Environment, UI + ducking snapshots.
3. Tire scrub, brake, collision, UI clicks, race start/finish, crowd/city ambience.
4. Spatial audio on opponents/traffic with mild Doppler.
5. Device Music: Android MediaStore/SAF picker, play/pause/next, in-car radio UI chip, no break Bluetooth headphone routing.

### D) Gameplay completion
1. Auto-generate AI racing-line waypoints from TrackBuilder segments.
2. Accurate race position (progress along spline, not Z compare).
3. Full checkpoint validation + wrong-way detection.
4. Minimap render texture bound to HUD.
5. Damage modes: Off / Visual / Full (scratches, glass, smoke).
6. Loading screen async with tips + progress.
7. Haptics on crash/nitro/gear/UI (toggle).
8. Ghost race architecture stub ready for replay path.

### E) Meta systems
1. Wire Garage UI to real car list + owned save data + 360 orbit.
2. Customize: paint/rims/neon apply real materials; locks + prices.
3. Career chapters 2–N events + star thresholds + rewards screen polish.
4. Settings screen functional: graphics tier, FPS, volumes, controls, vibration, language stub.
5. Economy soft balance (prices vs race rewards).

### F) Mobile polish
1. Touch targets ≥ 48dp; safe area notches.
2. Performance: LOD, occlusion, pooling for traffic/VFX/trails, 30/60 FPS tiers on mid Android.
3. Controller remap UI; test Bluetooth gamepads.
4. Clean console: no missing scripts, no pink materials, no spam logs.

### G) Delivery
1. Development APK + optional AAB.
2. README: Unity version, how to open, controls, known limits.
3. License note: only original/licensed assets.

## Constraints
- Offline-first core.
- No copyrighted cars/logos/music from other games.
- Prefer data-driven ScriptableObjects for cars/tracks/events.
- Keep existing namespaces `NeonRush.*` and SceneAssembler entry path working.

## Success criteria
Player installs APK → Main Menu → Garage sees real car → Race on neon track → HUD/minimap/nitro/drift feel premium → Results + coins saved → survives mid-range Android at 30–60 FPS.

---
