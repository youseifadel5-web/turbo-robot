# Cinematic Look — Neon Rush (NOW)

Use these generated frames as the art bible when dressing Unity scenes.

## Must-have look
- Night-first. Wet asphalt. Neon cyan + magenta. Bloom, not clutter.
- HUD never covers the vanishing point of the road.
- Garage = dark hangar + floor reflection + rim light on an ORIGINAL supercar.
- No real-world brand logos or copied body silhouettes from other games.

## Unity scene dressing checklist
Garage:
- Reflection probe on floor plane
- 2 neon strip lights (cyan L, magenta R)
- Soft spotlight from above-front
- Car pivot yaw

Race:
- URP Bloom threshold ~0.9, intensity 0.4–0.7
- Planar reflection or wet smoothness 0.85 on road
- Street lamps + emissive sign quads
- Tire smoke only when drifting
- Nitro: additive trail + exhaust

## Camera
Default chase: offset (0, 3.2, -7.5), FOV 55–60 on phone.
