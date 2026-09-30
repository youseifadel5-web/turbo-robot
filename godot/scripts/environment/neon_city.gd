class_name NeonCity
## Port of NeonCityBuilder — procedural neon city. IMPROVEMENT: buildings,
## lamps and billboards now line the generated racing route (Unity built a
## separate straight corridor that overlapped the track).

var root: Node3D = null
var building_spacing := 28.0
var rows := 2


func build(parent: Node3D, line: RacingLine, world_root: Node3D) -> void:
	root = world_root
	var cyan := CarBuilder.make_neon_mat(Color(0.0, 0.94, 1.0), 4.0)
	var magenta := CarBuilder.make_neon_mat(Color(1.0, 0.17, 0.84), 4.0)
	var building_dark := _building_mat(Color(0.07, 0.08, 0.11))
	var building_mid := _building_mat(Color(0.1, 0.11, 0.15))

	if not line.is_valid():
		return
	var pts := line.points
	var idx := 0
	# Walk the line; decorate both sides every ~28 m
	var i := 0
	while i < pts.size():
		var p: Vector3 = pts[i]
		var next: Vector3 = pts[min(i + 1, pts.size() - 1)]
		var dir := (next - p).normalized() if next != p else Vector3.FORWARD
		var side := dir.cross(Vector3.UP)
		for s in [-1.0, 1.0]:
			for r in rows:
				var x: float = s * (14.0 + 8.0 + r * 14.0 + randf() * 3.0)
				var h := randf_range(12.0, 42.0)
				var w := randf_range(6.0, 12.0)
				var d := randf_range(6.0, 14.0)
				var b := MeshInstance3D.new()
				b.name = "Bld_%d" % idx
				idx += 1
				var bm := BoxMesh.new()
				bm.size = Vector3(w, h, d)
				b.mesh = bm
				b.position = p + side * x + Vector3(0, h * 0.5, 0) + dir * randf_range(-4.0, 4.0)
				b.material_override = building_dark if randf() > 0.5 else building_mid
				parent.add_child(b)
				# Neon vertical strip
				var strip := MeshInstance3D.new()
				var sb := BoxMesh.new()
				sb.size = Vector3(0.06, h * 0.7, 0.08)
				strip.mesh = sb
				strip.position = b.position - side * s * (w * 0.5 + 0.1)
				strip.material_override = cyan if randf() > 0.45 else magenta
				parent.add_child(strip)
				# Window glow
				if h > 18.0 and randf() > 0.4:
					var win := MeshInstance3D.new()
					var wb := BoxMesh.new()
					wb.size = Vector3(0.03, h * 0.55, d * 0.7)
					win.mesh = wb
					win.position = b.position - side * s * (w * 0.5 + 0.15)
					win.material_override = CarBuilder.make_neon_mat(Color(0.4, 0.7, 1.0), 1.2)
					parent.add_child(win)
		# Lamps
		_spawn_lamp(parent, p + side * (-9.2), cyan)
		_spawn_lamp(parent, p + side * (9.2), magenta)
		i += maxi(1, int(building_spacing / 10.0))  # sampling step ~ spacing

	# Billboards
	for k in 8:
		if pts.is_empty():
			break
		var bp: Vector3 = pts[randi() % pts.size()]
		var board := MeshInstance3D.new()
		var bb := BoxMesh.new()
		bb.size = Vector3(0.3, 4.0, 8.0)
		board.mesh = bb
		board.position = bp + Vector3(randf_range(-18, 18), 8.0 + randf() * 6.0, randf_range(-18, 18))
		board.material_override = cyan if randf() > 0.5 else magenta
		parent.add_child(board)


func _spawn_lamp(parent: Node3D, pos: Vector3, neon_mat: StandardMaterial3D) -> void:
	var pole := MeshInstance3D.new()
	var pm := CylinderMesh.new()
	pm.top_radius = 0.08
	pm.bottom_radius = 0.08
	pm.height = 6.0
	pole.mesh = pm
	pole.position = pos + Vector3(0, 3.0, 0)
	pole.material_override = _building_mat(Color(0.12, 0.12, 0.14))
	parent.add_child(pole)

	var head := MeshInstance3D.new()
	var hm := BoxMesh.new()
	hm.size = Vector3(0.8, 0.15, 0.4)
	head.mesh = hm
	head.position = pos + Vector3(0, 6.1, 0)
	head.material_override = neon_mat
	parent.add_child(head)

	var light := OmniLight3D.new()
	light.omni_range = 18.0
	light.light_energy = 1.6
	light.light_color = neon_mat.emission
	light.shadow_enabled = false
	light.position = pos + Vector3(0, 5.5, 0)
	parent.add_child(light)


func _building_mat(c: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.metallic = 0.15
	m.roughness = 0.65
	return m
