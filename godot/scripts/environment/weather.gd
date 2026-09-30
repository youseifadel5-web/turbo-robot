class_name Weather
## Port of WeatherController — weather affects grip AND visuals.

enum Type { CLEAR, CLOUDY, RAIN, HEAVY_RAIN, FOG, SNOW }

static var current: int = Type.CLEAR

var _rain: GPUParticles3D = null
var _env: WorldEnvironment = null
var _cars: Array = []


func setup(env: WorldEnvironment, follow_target: Node3D, cars: Array) -> void:
	_env = env
	_cars = cars
	_rain = _make_rain()
	follow_target.add_child(_rain)


static func grip_for(type: int) -> float:
	match type:
		Type.CLEAR: return 1.0
		Type.CLOUDY: return 0.98
		Type.RAIN: return 0.82
		Type.HEAVY_RAIN: return 0.68
		Type.FOG: return 0.9
		Type.SNOW: return 0.55
	return 1.0


func set_weather(type: int) -> void:
	current = clampi(type, 0, 5)
	apply()


func apply() -> void:
	var grip := grip_for(current)
	var rain := current == Type.RAIN or current == Type.HEAVY_RAIN
	if _rain:
		_rain.emitting = rain
		_rain.amount_ratio = 1.0 if current == Type.HEAVY_RAIN else 0.6

	if _env and _env.environment:
		var e := _env.environment
		match current:
			Type.CLEAR:
				e.fog_enabled = false
			Type.CLOUDY:
				e.fog_enabled = true
				e.fog_density = 0.005
				e.ambient_light_energy = 1.0
			Type.RAIN:
				e.fog_enabled = true
				e.fog_density = 0.012
				e.ambient_light_energy = 1.0
			Type.HEAVY_RAIN:
				e.fog_enabled = true
				e.fog_density = 0.017
				e.ambient_light_energy = 0.8
			Type.FOG:
				e.fog_enabled = true
				e.fog_density = 0.035
				e.ambient_light_energy = 0.9
			Type.SNOW:
				e.fog_enabled = true
				e.fog_density = 0.02
				e.ambient_light_energy = 1.1

	for car in _cars:
		if car != null and "set_grip_multiplier" in car:
			car.set_grip_multiplier(grip)


func _make_rain() -> GPUParticles3D:
	var ps := GPUParticles3D.new()
	ps.amount = 500
	ps.lifetime = 0.6
	ps.local_coords = false
	ps.emitting = false
	ps.position = Vector3(0, 12, 20)
	var mat := ParticleProcessMaterial.new()
	mat.direction = Vector3(0.1, -1, 0.05)
	mat.spread = 5.0
	mat.initial_velocity_min = 16.0
	mat.initial_velocity_max = 20.0
	mat.scale_min = 1.0
	mat.scale_max = 1.0
	mat.color = Color(0.7, 0.8, 0.95, 0.35)
	ps.process_material = mat
	var quad := QuadMesh.new()
	quad.size = Vector2(0.03, 0.5)
	var sm := StandardMaterial3D.new()
	sm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	sm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	sm.billboard_mode = BaseMaterial3D.BILLBOARD_PARTICLES
	if ResourceLoader.exists("res://assets/vfx/tex_rain.png"):
		sm.albedo_texture = load("res://assets/vfx/tex_rain.png")
	quad.material = sm
	ps.draw_pass_1 = quad
	return ps
