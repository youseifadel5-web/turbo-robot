# NEON RUSH — Distinctive Features (Not Generic)

These features make the game feel unique while staying in the premium mobile racing genre shown in the references.

## 1. Neon Trail System
While Nitro is active the car emits a short-lived cyan → magenta light trail on the asphalt.
- Object-pooled ribbon / decal trail
- Automatically fades after 1.5–2.5 s
- Intensity scales with nitro level
- Can be disabled in Graphics → Effects for low-end devices

## 2. Shockwave Nitro
On nitro activation:
- Brief radial energy pulse under the car
- Small camera punch-in (very light)
- Exhaust flame intensity spike
- Engine audio layer crossfade to high-RPM + turbo layer
- Optional screen edge cyan vignette for 0.3 s

## 3. Drift Heat & Score
Real drift (not animation only):
- Drift angle calculated from velocity vs forward
- Tire smoke color shifts with sustained drift (white → cyan → magenta)
- Score = angle × speed × time × combo multiplier
- Near-miss bonus when passing traffic/AI within 2.5 m
- Combo resets after 1.2 s of no drift
- HUD shows: SCORE / COMBO / ANGLE / SPEED bonus

## 4. Modular Track Length Selector
Player chooses before race:
- SHORT   (~2–4 km)
- MEDIUM  (~5–8 km)
- LONG    (~10–20 km)
- EXTREME (map maximum)
- CUSTOM  (laps + approximate distance)

TrackBuilder uses segments (Straight, Curve L/R, Hairpin, Tunnel, Bridge, Jump, Checkpoint, Start, Finish) and validates connectivity so impossible routes cannot be generated.

## 5. Car Class Identity
Cars have classes that affect visual treatment:
- Class S → soft underglow ring + higher base stats
- Class A → stronger neon accents
- Class B → cleaner look
Stats are data-driven (ScriptableObject) and upgrades modify real physics values (acceleration, grip, brake torque, nitro capacity, mass).

## 6. Live Garage Presentation
- 360° orbit + pinch zoom
- Real-time reflection probe / planar reflection on floor
- Stats bars animate when upgrades or parts change
- Car idle animation (subtle suspension / light pulse)
- Instant switch between owned cars via bottom carousel

## 7. Device Music as Car Radio
- Music from phone plays as “in-car radio”
- Small elegant now-playing card appears for 3–4 s then fades
- Ducking: music lowers slightly on heavy crash or nitro start, never silences engine
- Works with Bluetooth headphones / car audio (system route, no forced override)

## 8. Smart Controller Support
- Unity Input System (no custom Bluetooth API required for standard gamepads)
- Toast: “CONTROLLER CONNECTED” / “CONTROLLER DISCONNECTED”
- Auto-rebind friendly mapping screen
- Separate from Bluetooth audio devices

## 9. Weather That Matters
- Rain / Heavy Rain → reduced lateral grip + longer braking distance
- Wet road reflections + tire spray
- Fog reduces far visibility
- Snow (future) → different handling profile
Not just a color filter.

## 10. Race Feedback Polish
- Checkpoint gate flash + soft sound
- Position change animation
- Drift popup (“GOOD DRIFT 12M”, “PERFECT”, “NEAR MISS +500”)
- Finish cinematic camera + result screen with position, time, best lap, drift score, nitro used, overtakes, rewards

## What we deliberately avoid
- Fake stats that do not affect physics
- Placeholder buttons that do nothing
- Copying logos / exact car models from other games
- Forcing internet for core gameplay
- Overly busy HUD that covers the road
