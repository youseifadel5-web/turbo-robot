extends Node3D
## NeonTrail — pooled neon trail quads while nitro is active (port of NeonTrailSystem).
## Cyan → magenta fade, additive.

var car: RigidBody3D = null

const SPAWN_INTERVAL := 0.04
const TRAIL_LIFETIME := 1.8
const TRAIL_WIDTH := 0.35
const POOL_SIZE := 48
const START_COLOR := Color(0.0, 0.94, 1.0, 0.85)
const END_COLOR := Color(1.0, 0.17, 0.84, 0.0)

var _pool: Array[MeshInstance3D] = []
var _active: Array = []   # [MeshInstance3D, age]
var _timer := 0.0


func _ready() -> void:
	var mat := StandardMaterial3D.new()
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.emission_enabled = true
	mat.emission = START_COLOR
	mat.emission_energy_multiplier = 2.0
	mat.billboard_mode = BaseMaterial3D.BILLBOARD_DISABLED
	for i in POOL_SIZE:
		var mi := MeshInstance3D.new()
		var quad := QuadMesh.new()
		quad.size = Vector2(TRAIL_WIDTH, TRAIL_WIDTH * 2.2)
		quad.material = mat.duplicate()
		mi.mesh = quad
		add_child(mi)
		mi.visible = false
		_pool.append(mi)


func _process(delta: float) -> void:
	if car == null:
		return
	for i in range(_active.size() - 1, -1, -1):
		var entry: Array = _active[i]
		entry[1] += delta
		var t: float = float(entry[1]) / TRAIL_LIFETIME
		if t >= 1.0:
			entry[0].visible = false
			_active.remove_at(i)
			continue
		var mi: MeshInstance3D = entry[0]
		var c: Color = START_COLOR.lerp(END_COLOR, t)
		var m: StandardMaterial3D = mi.mesh.material
		m.albedo_color = c
		m.emission = c

	var nitro_active: bool = car.get("is_nitro_active")
	if not nitro_active:
		return
	_timer -= delta
	if _timer > 0.0:
		return
	_timer = SPAWN_INTERVAL
	var basis: Basis = car.global_transform.basis
	var pos: Vector3 = car.global_position
	_spawn(pos + basis.x * -0.7, basis)
	_spawn(pos + basis.x * 0.7, basis)


func _spawn(pos: Vector3, basis: Basis) -> void:
	var mi: MeshInstance3D = null
	for p in _pool:
		if not p.visible:
			mi = p
			break
	if mi == null and _pool.size() > 0:
		mi = _pool[randi() % _pool.size()]
	if mi == null:
		return
	mi.global_position = pos + Vector3(0, 0.05, 0)
	mi.look_at(pos + basis.z, Vector3.UP)
	mi.visible = true
	_active.append([mi, 0.0])
