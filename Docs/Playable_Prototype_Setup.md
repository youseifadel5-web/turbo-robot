# Playable Prototype Setup — with procedural 3D

## Fastest path (no art)
1. Unity 2022/2023 LTS + URP project.
2. Copy `Assets/Scripts` + `Docs` into the project.
3. New empty scene.
4. Create empty GameObject → add **SceneAssembler**.
5. Press Play.

You get:
- Neon night corridor (buildings, lamps, wet road)
- Procedural black/cyan supercar
- Chase camera
- Tire smoke on drift, nitro flames, trails
- Night lighting + fog

Controls: WASD, Shift drift, Ctrl nitro, Space brake.

## Manual hierarchy (optional)
```
SceneAssembler
SkyLighting
NeonCity          → NeonCityBuilder.Build()
TrackRoot         → TrackBuilder
PlayerCar         → from ProceduralCarBuilder
Main Camera       → ChaseCamera
RaceManager
Weather
```

## Replacing the procedural car
1. Parent your FBX under PlayerCar root that has Rigidbody + ArcadeCarController.
2. Delete or hide procedural mesh children.
3. Keep TrailEmitL/R points and CarVfxBinder.
