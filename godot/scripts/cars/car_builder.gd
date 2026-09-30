class_name CarBuilder
## Builds a car Node3D from the original OBJ meshes + applies customization.
## Port of CarModelLoader + ProceduralCarBuilder + CustomizeApplier.

const CAR_SCRIPT := preload("res://scripts/cars/arcade_car.gd")
const ENGINE_AUDIO_SCRIPT := preload("res://scripts/cars/engine_audio.gd")
const NEON_TRAIL_SCRIPT := preload("res://scripts/cars/neon_trail.gd")


static func make_paint(color: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = color
	m.metallic = 0.65
	m.roughness = 0.25
	return m


static func make_glass() -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = Color(0.15, 0.2, 0.28, 0.55)
	m.metallic = 0.1
	m.roughness = 0.05
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	return m


static func make_neon_mat(color: Color, intensity := 4.0) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = color
	m.emission_enabled = true
	m.emission = color
	m.emission_energy_multiplier = intensity
	return m


## Build a full car (mesh + physics + optional audio/vfx). paint/rim/neon hex colors.
static func build_car(car_id: String, paint_hex: String, rim_hex: String, neon_hex: String,
		with_audio := false) -> Node3D:
	var root := Node3D.new()
	root.name = "Car_" + car_id

	var car := RigidBody3D.new()
	car.set_script(CAR_SCRIPT)
	car.name = "Body"
	root.add_child(car)

	var def := CarCatalog.get_car(car_id)

	# Mesh — original OBJ assets, LOD via visibility_range
	var mesh := _load_car_mesh(car_id)
	if mesh != null:
		var mi := MeshInstance3D.new()
		mi.name = "CarMesh"
		mi.mesh = mesh
		mi.material_override = make_paint(CarCatalog.hex_color(paint_hex))
		mi.layers = 1
		car.add_child(mi)

		var lod_mesh := _load_car_mesh(car_id + "_lod1")
		if lod_mesh != null:
			var lod := MeshInstance3D.new()
			lod.name = "CarMeshLOD"
			lod.mesh = lod_mesh
			lod.material_override = mi.material_override
			lod.visibility_range_end = 70.0
			car.add_child(lod)

	# Collider (matches Unity BoxCollider 1.9 x 1.0 x 4.4, center y 0.5)
	var col := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(1.9, 1.0, 4.4)
	col.shape = shape
	col.position = Vector3(0, 0.5, 0)
	car.add_child(col)
	# Unity rigidbodies have no implicit friction — arcade forces only.
	var pm := PhysicsMaterial.new()
	pm.friction = 0.0
	pm.bounce = 0.0
	car.physics_material_override = pm

	# Neon underglow quad + light
	var neon_color := CarCatalog.hex_color(neon_hex)
	var glow := MeshInstance3D.new()
	var quad := QuadMesh.new()
	quad.size = Vector2(2.4, 4.8)
	quad.orientation = PlaneMesh.FACE_Y
	var gm := StandardMaterial3D.new()
	gm.albedo_color = Color(neon_color.r, neon_color.g, neon_color.b, 0.55)
	gm.emission_enabled = true
	gm.emission = neon_color
	gm.emission_energy_multiplier = 2.5
	gm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	gm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	gm.billboard_mode = BaseMaterial3D.BILLBOARD_DISABLED
	quad.material = gm
	glow.mesh = quad
	glow.position = Vector3(0, 0.06, 0)
	glow.name = "Underglow"
	car.add_child(glow)

	var light := OmniLight3D.new()
	light.light_color = neon_color
	light.omni_range = 7.0
	light.light_energy = 1.4
	light.position = Vector3(0, 0.3, 0)
	light.shadow_enabled = false
	light.name = "UnderglowLight"
	car.add_child(light)

	# Headlights
	var hl := SpotLight3D.new()
	hl.spot_range = 30.0
	hl.spot_angle = 32.0
	hl.light_energy = 2.0
	hl.light_color = Color(0.8, 0.9, 1.0)
	hl.position = Vector3(0, 0.7, 2.2)
	hl.name = "Headlights"
	car.add_child(hl)

	# Rim glow strip (visual hint of rim color)
	var rim_light := OmniLight3D.new()
	rim_light.light_color = CarCatalog.hex_color(rim_hex)
	rim_light.omni_range = 2.5
	rim_light.light_energy = 0.5
	rim_light.position = Vector3(0, 0.35, -2.1)
	car.add_child(rim_light)

	if with_audio:
		var eng := Node3D.new()
		eng.set_script(ENGINE_AUDIO_SCRIPT)
		eng.name = "EngineAudio"
		car.add_child(eng)

	var trail := Node3D.new()
	trail.set_script(NEON_TRAIL_SCRIPT)
	trail.name = "NeonTrail"
	car.add_child(trail)

	return root


static func build_traffic(variant: int) -> Node3D:
	var root := Node3D.new()
	root.name = "TrafficCar"
	var mesh: Mesh = null
	if variant % 2 == 0:
		mesh = _load_car_mesh("traffic_sedan")
	else:
		mesh = _load_car_mesh("traffic_van")
	var mi := MeshInstance3D.new()
	if mesh != null:
		mi.mesh = mesh
		var m := StandardMaterial3D.new()
		m.albedo_color = Color(randf() * 0.4 + 0.1, randf() * 0.3, randf() * 0.5 + 0.2)
		mi.material_override = m
	else:
		var box := BoxMesh.new()
		box.size = Vector3(1.8, 1.0, 4.2)
		mi.mesh = box
	mi.position = Vector3(0, 0.5, 0)
	root.add_child(mi)
	# Rear red lights for night traffic look
	var tail := OmniLight3D.new()
	tail.light_color = Color(1.0, 0.15, 0.1)
	tail.omni_range = 3.0
	tail.light_energy = 0.7
	tail.position = Vector3(0, 0.7, -2.0)
	root.add_child(tail)
	return root


static func _load_car_mesh(car_id: String) -> Mesh:
	var path := "res://assets/cars/%s.obj" % car_id
	if ResourceLoader.exists(path):
		var res = load(path)
		if res is Mesh:
			return res
		if res is PackedScene:
			var inst = res.instantiate()
			for child in inst.get_children():
				if child is MeshInstance3D:
					return child.mesh
			if inst is MeshInstance3D:
				return inst.mesh
	return null
