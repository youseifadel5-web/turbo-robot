extends Node
## GhostRecorder + GhostPlayer port — records the player's line and plays
## it back as a translucent rival. Data: JSON in user://ghosts/<trackId>.json.
## IMPROVEMENT over Unity: ghost races are now surfaced as a menu mode.

const SAMPLE_INTERVAL := 0.1

var track_id := "neon_city_01"
var _car: RigidBody3D = null
var _times: PackedFloat32Array = []
var _positions: PackedVector3Array = []
var _rotations: Array[Quaternion] = []
var _timer := 0.0
var _elapsed := 0.0
var _recording := false

var ghost_mesh: Node3D = null
var _play_data: Dictionary = {}
var _play_time := 0.0
var _play_index := 0


func begin(car: RigidBody3D, id: String) -> void:
	_car = car
	if not id.is_empty():
		track_id = id
	_times = PackedFloat32Array()
	_positions = PackedVector3Array()
	_rotations = []
	_elapsed = 0.0
	_timer = 0.0
	_recording = car != null


func stop() -> void:
	_recording = false


func update_recording(delta: float) -> void:
	if not _recording or _car == null:
		return
	_elapsed += delta
	_timer += delta
	if _timer < SAMPLE_INTERVAL:
		return
	_timer = 0.0
	_times.append(_elapsed)
	_positions.append(_car.global_position)
	_rotations.append(_car.global_transform.basis.get_rotation_quaternion())


func get_data(car_id: String) -> Dictionary:
	return {
		"trackId": track_id,
		"carId": car_id,
		"lapTime": _elapsed,
		"times": _times,
		"positions": _positions,
		"rotations": _rotations.map(func(q: Quaternion) -> Array: return [q.x, q.y, q.z, q.w]),
	}


func save_ghost(car_id: String) -> void:
	if _times.size() < 2:
		return
	var data := get_data(car_id)
	var dir := "user://ghosts"
	DirAccess.make_dir_recursive_absolute(dir)
	var path := "%s/%s.json" % [dir, track_id]
	var better := true
	if FileAccess.file_exists(path):
		var f := FileAccess.open(path, FileAccess.READ)
		var existing = JSON.parse_string(f.get_as_text()) if f else null
		f.close()
		if existing is Dictionary:
			better = _elapsed < float(existing.get("lapTime", 1e9))
	if better:
		var f := FileAccess.open(path, FileAccess.WRITE)
		f.store_string(JSON.stringify(data))
		f.close()


static func load_ghost(id: String) -> Dictionary:
	var path := "user://ghosts/%s.json" % id
	if not FileAccess.file_exists(path):
		return {}
	var f := FileAccess.open(path, FileAccess.READ)
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	return parsed if parsed is Dictionary else {}


static func has_ghost(id: String) -> bool:
	return FileAccess.file_exists("user://ghosts/%s.json" % id)


## ---- playback ----

func start_playback(data: Dictionary, car_scene: Node3D) -> void:
	_play_data = data
	ghost_mesh = car_scene
	_play_time = 0.0
	_play_index = 0
	if ghost_mesh:
		ghost_mesh.visible = true


func stop_playback() -> void:
	ghost_mesh = null
	_play_data = {}


func _process(delta: float) -> void:
	if ghost_mesh == null or _play_data.is_empty():
		return
	var times: PackedFloat32Array = _play_data.get("times", PackedFloat32Array())
	var positions: PackedVector3Array = _play_data.get("positions", PackedVector3Array())
	var rotations: Array = _play_data.get("rotations", [])
	if _play_index >= times.size() - 1:
		return
	_play_time += delta
	while _play_index < times.size() - 1 and times[_play_index + 1] < _play_time:
		_play_index += 1
	var next_i := mini(_play_index + 1, times.size() - 1)
	var t0: float = times[_play_index]
	var t1: float = maxf(t0 + 0.0001, times[next_i])
	var k := clampf((_play_time - t0) / (t1 - t0), 0.0, 1.0)
	ghost_mesh.global_position = positions[_play_index].lerp(positions[next_i], k)
	var q0 := _quat_at(rotations, _play_index)
	var q1 := _quat_at(rotations, next_i)
	ghost_mesh.global_transform.basis = Basis(q0.slerp(q1, k))


func _quat_at(rotations: Array, i: int) -> Quaternion:
	if i >= rotations.size():
		return Quaternion.IDENTITY
	var r: Array = rotations[i]
	return Quaternion(r[0], r[1], r[2], r[3])
