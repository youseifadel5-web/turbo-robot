# 3D Assets — What you have vs what to replace

## Runtime procedural (NOW — no FBX required)
| Piece | Script |
|-------|--------|
| Supercar body + neon + wheels | `ProceduralCarBuilder` |
| Wet road + dashes + neon curbs | `NeonCityBuilder` |
| Buildings + neon strips + lamps + billboards | `NeonCityBuilder` |
| Night sky / fog / moon / fill lights | `SkyLightingBootstrap` |
| Tire smoke, nitro flame, shockwave, rain | `VfxFactory` + `CarVfxBinder` |
| One-click assemble | `SceneAssembler` |

**Play:** empty scene → add empty GameObject → `SceneAssembler` → Play.

## Replace later with licensed / original art
Cars: assign FBX under same root; keep `ArcadeCarController` on root.
Road kit: modular meshes with same segment length as `TrackBuilder`.
City: kitbash night city, keep emissive materials cyan/magenta.
Skybox: HDRI night or URP sky volume.
VFX: replace particle materials with soft smoke textures + additive flame sheets.

## Recommended free starting points (check current license)
- Unity Asset Store: search "URP racing road", "simple supercar", "particle pack"
- Kenney / CC0 vehicle packs for traffic placeholders
- Always verify commercial license before shipping
