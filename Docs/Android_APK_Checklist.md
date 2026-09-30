# Android APK Checklist — Neon Rush Racing

## 1. Unity project
- [ ] Unity 2022 LTS or 2023 LTS
- [ ] Module: Android Build Support + OpenJDK + SDK + NDK
- [ ] URP template or add URP package
- [ ] Input System package active (Project Settings → Active Input Handling: Both or Input System)
- [ ] Copy this repo `Assets/Scripts`, `Assets/UI`, `Docs` into the project

## 2. Scene
- [ ] New scene `NeonCity_Race`
- [ ] Empty object + `SceneAssembler` (builds city, car, HUD, audio)
- [ ] Save scene, add to File → Build Settings (index 0)

## 3. Player Settings (Android)
- [ ] Company / Product name: Neon Rush Racing
- [ ] Package Name: `com.yourstudio.neonrush`
- [ ] Minimum API: 24 (or 23), Target API: highest installed
- [ ] Scripting Backend: IL2CPP
- [ ] Target Architectures: ARM64 (required on Play), ARMv7 optional
- [ ] Graphics APIs: Vulkan + OpenGLES3
- [ ] Orientation: Landscape Left/Right
- [ ] Internet: Not required for offline core
- [ ] Write Permission: only if Device Music uses old external storage paths

## 4. Permissions (only as needed)
- Device Music (Android 13+): `READ_MEDIA_AUDIO` via MediaStore / picker — do not request broad storage if using SAF
- Bluetooth Gamepad: usually no extra permission for standard game controllers on modern Android
- Do not force audio route away from Bluetooth headphones

## 5. Quality
- [ ] Attach `UrpLookApplier` (Low/Med/High)
- [ ] Default target 60 FPS (`Application.targetFrameRate`)
- [ ] Disable heavy shadows on Low tier

## 6. Build
- [ ] Build Settings → Android → Build APK (or Build App Bundle for Play)
- [ ] First test: Development Build + Autoconnect Profiler on one mid-range phone
- [ ] Verify: drive, nitro sound layers, HUD buttons, no pink materials

## 7. Known prototype limits
- Engine audio is procedural tones until you assign real WAV layers
- Device music is still a bridge placeholder
- Procedural car/city are placeholders for licensed art
- No online services required

## 8. Store hygiene
- Original or licensed art/audio only
- Privacy policy if you later add ads/analytics
- Content rating questionnaire
