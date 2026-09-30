extends Node3D
## GarageStudio — 360° showroom platform floating above the race world.
## show_car(carId, paintId, rimId, neonId) swaps the displayed car.

var _car_root: Node3D = null
var _car_id := ""


func _ready() -> void:
	# Round platform
	var floor_mesh := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 9.0
	cyl.bottom_radius = 9.0
	cyl.height = 0.4
	floor_mesh.mesh = cyl
	floor_mesh.position = Vector3(0, -0.2, 0)
	var fm := StandardMaterial3D.new()
	fm.albedo_color = Color(0.05, 0.06, 0.1)
	fm.metallic = 0.4
	fm.roughness = 0.2
	floor_mesh.material_override = fm
	add_child(floor_mesh)

	# Neon ring
	var ring := MeshInstance3D.new()
	var torus := TorusMesh.new()
	torus.inner_radius = 9.0
	torus.outer_radius = 9.5
	ring.mesh = torus
	ring.position = Vector3(0, 0.02, 0)
	ring.material_override = CarBuilder.make_neon_mat(Color(0.0, 0.94, 1.0), 3.0)
	add_child(ring)

	# Studio lights
	var key := DirectionalLight3D.new()
	key.rotation_degrees = Vector3(-45, 30, 0)
	key.light_energy = 1.2
	key.light_color = Color(0.8, 0.9, 1.0)
	add_child(key)
	for side in [-1.0, 1.0]:
		var l := OmniLight3D.new()
		l.light_color = Color(0.0, 0.94, 1.0) if side < 0 else Color(1.0, 0.17, 0.84)
		l.omni_range = 14.0
		l.light_energy = 2.0
		l.position = Vector3(side * 5.0, 3.5, 3.0)
		add_child(l)


func show_car(car_id: String, paint_id: String, rim_id: String, neon_id: String) -> void:
	if car_id == _car_id and _car_root != null:
		return
	_car_id = car_id
	if _car_root:
		_car_root.queue_free()
	_car_root = CarBuilder.build_car(car_id,
		CarCatalog.paint_hex(paint_id),
		CarCatalog.rim_hex(rim_id),
		CarCatalog.neon_hex(neon_id), false)
	_car_root.position = Vector3(0, 0.05, 0)
	add_child(_car_root)


func get_car_node() -> Node3D:
	return _car_root
