extends Node
## DamageSystem port — visual scuffed paint + smoke, optional performance penalty.
## Mode: 0 Off, 1 Visual, 2 Full.

var mode := 1
var max_damage := 100.0
const IMPACT_THRESHOLD := 3.0
const VISUAL_START := 15.0
const SMOKE_START := 45.0

var damage := 0.0
var _car: RigidBody3D = null
var _body_mesh: MeshInstance3D = null
var _original_paint := Color.WHITE
var _smoke: GPUParticles3D = null
var _base_accel := 0.0
var _base_max_speed := 0.0
var _base_grip := 0.0
var _stored_base := false


func setup(target_car: RigidBody3D) -> void:
	_car = target_car
	_car.connect("collided", Callable(self, "_on_impact"))
	_body_mesh = _car.get_node_or_null("CarMesh")
	if _body_mesh and _body_mesh.material_override is StandardMaterial3D:
		_original_paint = _body_mesh.material_override.albedo_color


func _on_impact(impact: float) -> void:
	if mode == 0 or _car == null:
		return
	if impact < IMPACT_THRESHOLD:
		return
	damage = minf(max_damage, damage + impact * 2.2)
	Sfx.play("SFX/sfx_collision_soft", clampf(impact / 20.0, 0.1, 1.0))
	Bus.pulse_crash_duck()
	if impact > 12.0:
		Sfx.play("SFX/sfx_crash", clampf(impact / 30.0, 0.2, 1.0))
	_apply_visual()
	_apply_performance()


func _apply_visual() -> void:
	if mode == 0 or damage < VISUAL_START or _body_mesh == null:
		return
	var t := clampf((damage - VISUAL_START) / max_damage, 0.0, 1.0)
	if _body_mesh.material_override is StandardMaterial3D:
		_body_mesh.material_override.albedo_color = _original_paint.lerp(Color(0.16, 0.16, 0.17), t * 0.7)
	if damage >= SMOKE_START and _smoke == null:
		_smoke = _make_smoke()
		_car.add_child(_smoke)


func _apply_performance() -> void:
	if mode != 2 or _car == null:
		return
	if not _stored_base:
		_base_accel = _car.acceleration
		_base_max_speed = _car.max_speed
		_base_grip = _car.lateral_grip
		_stored_base = true
	var health := 1.0 - damage / maxf(1.0, max_damage)
	_car.acceleration = _base_accel * lerpf(0.65, 1.0, health)
	_car.max_speed = _base_max_speed * lerpf(0.8, 1.0, health)
	_car.lateral_grip = _base_grip * lerpf(0.85, 1.0, health)


func _make_smoke() -> GPUParticles3D:
	var ps := GPUParticles3D.new()
	ps.amount = 40
	ps.lifetime = 1.2
	ps.position = Vector3(0, 0.7, -2.0)
	ps.local_coords = false
	var mat := ParticleProcessMaterial.new()
	mat.direction = Vector3(0, 1, 0)
	mat.spread = 18.0
	mat.initial_velocity_min = 0.4
	mat.initial_velocity_max = 1.2
	mat.scale_min = 0.6
	mat.scale_max = 1.4
	mat.color = Color(0.75, 0.75, 0.78, 0.5)
	ps.process_material = mat
	var quad := QuadMesh.new()
	quad.size = Vector2(0.9, 0.9)
	var sm := StandardMaterial3D.new()
	sm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	sm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	sm.billboard_mode = BaseMaterial3D.BILLBOARD_PARTICLES
	if ResourceLoader.exists("res://assets/vfx/tex_tiresmoke.png"):
		sm.albedo_texture = load("res://assets/vfx/tex_tiresmoke.png")
	sm.albedo_color = Color(0.8, 0.85, 0.9, 0.4)
	quad.material = sm
	ps.draw_pass_1 = quad
	return ps


func reset_damage() -> void:
	damage = 0.0
	if _body_mesh and _body_mesh.material_override is StandardMaterial3D:
		_body_mesh.material_override.albedo_color = _original_paint
	if _smoke:
		_smoke.queue_free()
		_smoke = null
	if _stored_base and _car != null:
		_car.acceleration = _base_accel
		_car.max_speed = _base_max_speed
		_car.lateral_grip = _base_grip
