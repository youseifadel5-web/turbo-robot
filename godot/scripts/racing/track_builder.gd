class_name TrackBuilder
## Port of NeonRush.Tracks.TrackBuilder: modular route toward a target distance.
## Builds road slabs (StaticBody3D), neon curbs, checkpoints (Area3D) and start/finish.
## IMPROVEMENT over Unity: the city now decorates THIS route instead of an
## overlapping straight corridor (the Unity build had both overlapping).

enum SegmentType { START, STRAIGHT, CURVE_LEFT, CURVE_RIGHT, HAIRPIN, TUNNEL, BRIDGE, JUMP, CHECKPOINT, FINISH }

var segment_length := 50.0
var road_width := 14.0

var root: Node3D = null
var segments: Array[Node3D] = []
var checkpoints: Array[Node3D] = []
var start_point: Node3D = null
var finish_point: Node3D = null
var built_distance := 0.0
var route_points: Array[Vector3] = []   # for city decoration + racing line


func clear() -> void:
	for s in segments:
		if is_instance_valid(s):
			s.queue_free()
	segments.clear()
	checkpoints.clear()
	start_point = null
	finish_point = null
	built_distance = 0.0
	route_points.clear()


func build_from_config(config: Dictionary) -> void:
	clear()
	if root == null:
		root = Node3D.new()
		root.name = "TrackRoot"
	var target_m := Session.target_km(int(config.get("lengthPreset", 1))) * 1000.0
	var cp_count: int = config.get("checkpointCount", 8)
	var sequence := _generate_sequence(target_m, cp_count)

	var pos := Vector3.ZERO
	var heading := 0.0
	for type in sequence:
		var seg := _spawn_segment(type, pos, heading)
		segments.append(seg)
		if type == SegmentType.START:
			start_point = seg
		if type == SegmentType.FINISH:
			finish_point = seg
		if type == SegmentType.CHECKPOINT:
			checkpoints.append(seg)
		var advance := _advance(type)
		built_distance += advance
		route_points.append(pos)
		match type:
			SegmentType.CURVE_LEFT: heading -= 25.0
			SegmentType.CURVE_RIGHT: heading += 25.0
			SegmentType.HAIRPIN: heading += 140.0
		var dir := Vector3(sin(deg_to_rad(heading)), 0, cos(deg_to_rad(heading)))
		pos += dir * advance

	_add_ground()


## Big collision + visual ground plane under the whole track so cars that
## leave the road drive on ground instead of falling (Unity had a ground cube).
func _add_ground() -> void:
	if route_points.is_empty():
		return
	var mn := route_points[0]
	var mx := route_points[0]
	for p in route_points:
		mn = mn.min(p)
		mx = mx.max(p)
	var centroid := (mn + mx) * 0.5
	var size := maxf(mx.x - mn.x, mx.z - mn.z) + 600.0
	var ground := StaticBody3D.new()
	ground.name = "Ground"
	var col := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(size, 0.4, size)
	col.shape = shape
	col.position = Vector3(0, -0.8, 0)
	ground.add_child(col)
	root.add_child(ground)
	ground.global_position = Vector3(centroid.x, 0, centroid.z)
	var mesh := MeshInstance3D.new()
	mesh.name = "GroundMesh"
	var bm := BoxMesh.new()
	bm.size = shape.size
	mesh.mesh = bm
	mesh.position = col.position
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.04, 0.05, 0.06)
	mat.roughness = 0.9
	mesh.material_override = mat
	ground.add_child(mesh)


func _generate_sequence(target_m: float, cp_count: int) -> Array:
	var list: Array = [SegmentType.START]
	var acc := segment_length
	var cp_placed := 0
	var next_cp_at := target_m / maxf(1.0, cp_count + 1.0)
	var safety := 0
	while acc < target_m - segment_length * 1.5 and safety < 400:
		safety += 1
		var r := randf()
		var t: int
		if r < 0.45: t = SegmentType.STRAIGHT
		elif r < 0.62: t = SegmentType.CURVE_LEFT
		elif r < 0.79: t = SegmentType.CURVE_RIGHT
		elif r < 0.86: t = SegmentType.TUNNEL
		elif r < 0.92: t = SegmentType.BRIDGE
		elif r < 0.96: t = SegmentType.JUMP
		else: t = SegmentType.HAIRPIN
		if t == SegmentType.HAIRPIN and list[list.size() - 1] == SegmentType.HAIRPIN:
			t = SegmentType.STRAIGHT
		list.append(t)
		acc += _advance(t)
		if cp_placed < cp_count and acc >= next_cp_at * (cp_placed + 1):
			list.append(SegmentType.CHECKPOINT)
			cp_placed += 1
	list.append(SegmentType.FINISH)
	return list


func _advance(t: int) -> float:
	match t:
		SegmentType.HAIRPIN: return segment_length * 0.7
		SegmentType.JUMP: return segment_length * 1.1
		SegmentType.CHECKPOINT, SegmentType.START, SegmentType.FINISH: return segment_length * 0.5
		_: return segment_length


func _spawn_segment(type: int, pos: Vector3, heading_deg: float) -> Node3D:
	var holder := Node3D.new()
	holder.name = SegmentType.keys()[type]
	holder.position = pos
	holder.rotation.y = deg_to_rad(heading_deg)
	root.add_child(holder)

	var body := StaticBody3D.new()
	body.name = "Road"
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(road_width, 0.3, _advance(type))
	shape.shape = box
	shape.position = Vector3(0, -0.15, _advance(type) * 0.5)
	body.add_child(shape)
	holder.add_child(body)

	var mesh := MeshInstance3D.new()
	mesh.name = "RoadMesh"
	var bm := BoxMesh.new()
	bm.size = Vector3(road_width, 0.3, _advance(type))
	mesh.mesh = bm
	mesh.position = Vector3(0, -0.15, _advance(type) * 0.5)
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.08, 0.09, 0.11)
	mat.metallic = 0.05
	mat.roughness = 0.12   # wet asphalt look
	mesh.material_override = mat
	holder.add_child(mesh)

	# Neon curbs both sides
	var cyan := CarBuilder.make_neon_mat(Color(0.0, 0.94, 1.0), 3.0)
	var magenta := CarBuilder.make_neon_mat(Color(1.0, 0.17, 0.84), 3.0)
	for side in [-1.0, 1.0]:
		var curb := MeshInstance3D.new()
		var cb := BoxMesh.new()
		cb.size = Vector3(0.2, 0.16, _advance(type))
		curb.mesh = cb
		curb.position = Vector3(side * (road_width * 0.5 + 0.15), 0.02, _advance(type) * 0.5)
		curb.material_override = cyan if side < 0 else magenta
		holder.add_child(curb)

	# Center dashes
	if type == SegmentType.STRAIGHT:
		var dash_mat := StandardMaterial3D.new()
		dash_mat.albedo_color = Color(0.95, 0.95, 0.85)
		dash_mat.emission_enabled = true
		dash_mat.emission = Color(0.95, 0.95, 0.85)
		dash_mat.emission_energy_multiplier = 0.25
		var dashes := int(_advance(type) / 8.0)
		for i in dashes:
			if i % 2 == 1:
				continue
			var d := MeshInstance3D.new()
			var db := BoxMesh.new()
			db.size = Vector3(0.25, 0.02, 3.5)
			d.mesh = db
			d.position = Vector3(0, 0.02, 4.0 + i * 8.0)
			d.material_override = dash_mat
			holder.add_child(d)

	# Checkpoint gate
	if type == SegmentType.CHECKPOINT:
		var gate := _make_gate(Color(0.0, 1.0, 0.8), heading_deg)
		gate.position = Vector3(0, 0, _advance(type) * 0.5)
		holder.add_child(gate)
	# Finish gate
	if type == SegmentType.FINISH:
		var gate := _make_gate(Color(1.0, 0.2, 0.6), heading_deg)
		gate.position = Vector3(0, 0, _advance(type) * 0.5)
		holder.add_child(gate)
	# Start banner
	if type == SegmentType.START:
		var gate := _make_gate(Color(0.2, 0.8, 1.0), heading_deg)
		gate.position = Vector3(0, 0, _advance(type) * 0.5)
		holder.add_child(gate)
	# Tunnel walls
	if type == SegmentType.TUNNEL:
		for side in [-1.0, 1.0]:
			var wall := StaticBody3D.new()
			var ws := CollisionShape3D.new()
			var wb := BoxShape3D.new()
			wb.size = Vector3(0.5, 6.0, _advance(type))
			ws.shape = wb
			ws.position = Vector3(side * (road_width * 0.5 + 0.4), 3.0, _advance(type) * 0.5)
			wall.add_child(ws)
			holder.add_child(wall)
			var wm := MeshInstance3D.new()
			var wbm := BoxMesh.new()
			wbm.size = wb.size
			wm.mesh = wbm
			wm.position = ws.position
			var wall_mat := CarBuilder.make_neon_mat(Color(0.4, 0.7, 1.0), 1.2)
			wall_mat.albedo_color = Color(0.1, 0.11, 0.15)
			wm.material_override = wall_mat
			holder.add_child(wm)

	return holder


func _make_gate(color: Color, _heading: float) -> Node3D:
	var gate := Node3D.new()
	gate.name = "Gate"
	var area := Area3D.new()
	var col := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(road_width, 8.0, 1.5)
	col.shape = box
	col.position = Vector3(0, 4.0, 0)
	area.add_child(col)
	gate.add_child(area)
	for side in [-1.0, 1.0]:
		var p := MeshInstance3D.new()
		var pm := BoxMesh.new()
		pm.size = Vector3(0.4, 8.0, 0.4)
		p.mesh = pm
		p.position = Vector3(side * (road_width * 0.5), 4.0, 0)
		p.material_override = CarBuilder.make_neon_mat(color, 3.0)
		gate.add_child(p)
	var top := MeshInstance3D.new()
	var tm := BoxMesh.new()
	tm.size = Vector3(road_width + 1.0, 0.4, 0.4)
	top.mesh = tm
	top.position = Vector3(0, 8.0, 0)
	top.material_override = CarBuilder.make_neon_mat(color, 3.0)
	gate.add_child(top)
	var light := OmniLight3D.new()
	light.light_color = color
	light.omni_range = 14.0
	light.light_energy = 1.6
	light.position = Vector3(0, 5.0, 0)
	gate.add_child(light)
	return gate
