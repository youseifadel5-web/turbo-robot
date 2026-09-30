# NEON RUSH RACING — Premium Visual & UX Spec
Inspired by modern mobile racing games, with original Neon Rush identity.

## Core Identity
- **Dark Premium** base (near-black / carbon)
- **Electric Cyan + Magenta Neon** accents (not pure cyan only)
- Wet reflective asphalt at night
- Strong bloom on lights, controlled motion blur
- Cinematic garage lighting (spot + rim + floor reflection)
- UI that feels like a racing game, never like a phone app

## Color Tokens
| Token | Hex | Use |
|-------|-----|-----|
| BG Deep | #05070C | Full-screen backgrounds |
| Panel | #0C1018CC | Translucent cards (80% opacity) |
| Neon Primary | #00F0FF | Active buttons, speed, nitro |
| Neon Secondary | #FF2BD6 | Drift, special events, highlights |
| Neon Gold | #FFC manifest | Coins, rewards |
| Text Primary | #F2F5FF | Titles |
| Text Secondary | #9AA3B5 | Labels |
| Danger | #FF3355 | Wrong way / damage |
| Success | #2EFF8A | Checkpoint / finish |

## Garage Screen (Reference style)
- Full 3D car on reflective floor with neon rings / light strips
- Horizontal car carousel at bottom (thumbnails + arrows)
- Left: stats bars (Top Speed / Accel / Handling / Brake / Drift / Nitro) with animated fill
- Top-right: Coins + Premium currency
- Floating action: Customize (gear icon), Upgrade, Race / Next
- Soft rotation of car + subtle idle animation
- Camera can orbit 360° + pinch zoom

## Customize Screen
Categories (bottom bar, horizontal scroll):
ENGINE · BRAKE · AGILITY · TURBO · NOS · RIMS · PAINT · BODY · SPOILER · NEON · LIGHTS · WRAP

- Left panel: live stat bars that update when part changes
- Center: 3D car with real-time material / mesh swap
- Locked parts show lock icon + price
- Confirm button with cost

## Race HUD (Premium Mobile)
**Top bar**
- Pause
- Position (e.g. 2/8)
- Distance / Progress % or Lap
- Race timer

**Right side**
- Large speed (KM/H) with neon outline
- Nitro meter + “NITRO SHOCKWAVE” label when active
- Optional drift score / combo

**Bottom**
- Touch controls: steering zones or virtual wheel / buttons
- Nitro button (glowing)
- Drift / Handbrake
- Optional minimap (corner, semi-transparent)

**Center feedback**
- Drift score popup (“GOOD DRIFT 10M”)
- Checkpoint flash
- Wrong way warning
- Near-miss bonus

Keep center of screen as clear as possible for the road.

## Distinctive Neon Rush Features
1. **Neon Trail Mode** — car leaves temporary cyan/magenta light trails while nitro is active
2. **Shockwave Nitro** — short radial pulse + camera punch when nitro starts
3. **Drift Heat Meter** — longer drifts change tire smoke color (white → cyan → magenta)
4. **Night Pulse City** — environment neon signs pulse to a subtle beat (optional, off by default for performance)
5. **Car Class Glow** — S-class cars get a soft underglow ring in garage
6. **Dynamic Weather Grip** — rain reduces lateral grip and lengthens braking (real physics, not just filter)
7. **Ghost Replay** — after race, optional ghost of best run (architecture ready)
8. **Controller Toast** — clean “CONTROLLER CONNECTED / DISCONNECTED” banner matching neon style

## Environments (from references + original)
1. City Night (wet streets, neon signs)
2. Mountain Coastal Highway (sunset / night)
3. Tunnel (echo, strong reflections)
4. Bridge / Overpass
5. Industrial Port
6. Desert Canyon (day + heat haze optional)
7. Volcanic / Lava edge (future)
8. Orbital / Sci-fi elevated track (future event)

## Camera Modes
- Chase (default)
- Close Chase
- Far Chase
- Hood
- Cockpit (first-person with steering wheel optional)
- Bumper
Light camera shake only on: collision, nitro start, landing from jump.

## Quality Tiers (Mobile)
| Setting | Shadows | Post | Particles | Reflections |
|---------|---------|------|-----------|-------------|
| Low | Off / baked | Minimal | Low | Cubemap only |
| Medium | Soft | Bloom + AO light | Medium | Planar selective |
| High | Soft + contact | Full stack | High | Planar + SSR lite |
| Ultra | High | Full + motion blur | Max | Full |

FPS targets: 30 / 60 (90/120 if device supports and user enables).
