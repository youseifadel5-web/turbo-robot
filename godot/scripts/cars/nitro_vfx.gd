extends Node3D
## NitroVFX — nitro flames + shockwave + camera punch (port of NitroVFXController).

var car: RigidBody3D = null
var chase_camera: Camera3D = null

var _flames: Array[GPUParticles3D] = []
const FLAME_IDLE := 12.0
const FLAME_NITRO := 56.0


func setup(target_car: RigidBody3D, camera: Camera3D) -> void:
	car = target_car
	chase_camera = camera
	for i in 2:
		var f := _make_flame(Vector3(-0.45 if i == 0 else 0.45, 0.35, -2.1))
		add_child(f)
		_flames.append(f)
	car.connect("nitro_started", Callable(self, "_on_nitro_started"))


func _make_flame(local_pos: Vector3) -> GPUParticles3D:
	var ps := GPUParticles3D.new()
	ps.amount = 30
	ps.lifetime = 0.25
	ps.local_coords = false
	ps.emitting = true
	ps.position = local_pos
	var mat := ParticleProcessMaterial.new()
	mat.direction = Vector3(0, 0, 1)
	mat.spread = 8.0
	mat.initial_velocity_min = 4.0
	mat.initial_velocity_max = 7.0
	mat.scale_min = 0.4
	mat.scale_max = 0.9
	mat.color = Color(0.2, 0.9, 1.0, 0.9)
	ps.process_material = mat
	var quad := QuadMesh.new()
	quad.size = Vector2(0.35, 0.35)
	var sm := StandardMaterial3D.new()
	sm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	sm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	sm.billboard_mode = BaseMaterial3D.BILLBOARD_PARTICLES
	sm.albedo_texture = load("res://assets/vfx/tex_flame.png") if ResourceLoader.exists("res://assets/vfx/tex_flame.png") else null
	sm.albedo_color = Color(0.3, 0.95, 1.0, 0.85)
	quad.material = sm
	ps.draw_pass_1 = quad
	return ps


func _process(_delta: float) -> void:
	if car == null:
		return
	var nitro_active: bool = car.get("is_nitro_active")
	for f in _flames:
		f.amount_ratio = 1.0 if nitro_active else 0.22
		var mat: ParticleProcessMaterial = f.process_material
		mat.initial_velocity_min = 8.0 if nitro_active else 2.0
		mat.initial_velocity_max = 13.0 if nitro_active else 4.0


func _on_nitro_started() -> void:
	if chase_camera and chase_camera.has_method("add_shake"):
		chase_camera.add_shake(0.35)
		chase_camera.add_punch(Vector3(0, 0, 0.22))
	Sfx.play("SFX/sfx_nitro_ignite", 0.9)
