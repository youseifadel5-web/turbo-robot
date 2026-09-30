# Track System — Modular Length

## Presets
| Preset | Target distance |
|--------|-----------------|
| SHORT | ~2–4 km |
| MEDIUM | ~5–8 km |
| LONG | ~10–20 km |
| EXTREME | map maximum (~22 km default) |
| CUSTOM | player-defined km + laps |

## Segments
Start · Straight · CurveLeft · CurveRight · Hairpin · Tunnel · Bridge · Jump · Checkpoint · Finish

`TrackBuilder.BuildFromConfig(TrackConfig)` generates a sequence weighted toward straights/curves, inserts checkpoints evenly, and ends with Finish. Double-hairpins are avoided.

## Usage
1. Create a `TrackConfig` ScriptableObject asset.
2. Set length preset + laps + environment.
3. Assign prefabs on `TrackBuilder` (or leave null for gray-box primitives).
4. Call `BuildFromConfig` from `RaceManager.StartRace()`.

## Next
- Real modular mesh pieces with snap points
- Racing line waypoints per segment
- Environment-specific props (city neon, desert rocks, tunnel lights)
