extends Camera3D
## ChaseCamera — port with 6 modes (Chase/Close/Far/Hood/Cockpit/Bumper),
## exponential smoothing, shake and nitro punch.

enum Mode { CHASE, CLOSE, FAR, HOOD, COCKPIT, BUMPER }

var target: Node3D = null
var mode: Mode = Mode.CHASE

const OFFSETS := {
	Mode.CHASE: Vector3(0.0, 3.2, -7.5),
	Mode.CLOSE: Vector3(0.0, 2.4, -5.2),
	Mode.FAR: Vector3(0.0, 4.5, -11.0),
	Mode.HOOD: Vector3(0.0, 1.1, 0.8),
	Mode.COCKPIT: Vector3(0.0, 1.35, 0.15),
	Mode.BUMPER: Vector3(0.0, 0.7, -1.2),
}

const POSITION_SMOOTH := 8.0
const ROTATION_SMOOTH := 8.0
const LOOK_HEIGHT := 1.1

var _shake_strength := 0.0
var _runtime_punch := Vector3.ZERO


func cycle_mode() -> void:
	mode = (mode + 1) as Mode if mode < Mode.BUMPER else Mode.CHASE


func set_mode(m: Mode) -> void:
	mode = m


func add_shake(strength: float) -> void:
	_shake_strength = maxf(_shake_strength, strength)


func add_punch(local_punch: Vector3) -> void:
	_runtime_punch = local_punch


func mode_name() -> String:
	match mode:
		Mode.CHASE: return "CHASE"
		Mode.CLOSE: return "CLOSE"
		Mode.FAR: return "FAR"
		Mode.HOOD: return "HOOD"
		Mode.COCKPIT: return "COCKPIT"
		Mode.BUMPER: return "BUMPER"
	return "CHASE"


func _process(delta: float) -> void:
	if target == null:
		return
	var base_offset: Vector3 = OFFSETS[mode]
	var desired: Vector3 = target.global_transform * (base_offset + _runtime_punch)

	var shake_offset := Vector3.ZERO
	if _shake_strength > 0.001:
		shake_offset = Vector3(randf_range(-1, 1), randf_range(-1, 1), randf_range(-1, 1)) * _shake_strength * 0.15
		_shake_strength = lerpf(_shake_strength, 0.0, 1.0 - exp(-6.0 * delta))
	_runtime_punch = _runtime_punch.lerp(Vector3.ZERO, delta * 8.0)

	var pos_smooth := 14.0 if (mode == Mode.COCKPIT or mode == Mode.HOOD) else POSITION_SMOOTH
	global_position = global_position.lerp(desired + shake_offset, 1.0 - exp(-pos_smooth * delta))

	var look_point: Vector3
	if mode == Mode.COCKPIT or mode == Mode.HOOD:
		look_point = target.global_position + (-target.global_transform.basis.z) * 25.0 + target.global_transform.basis.y * 0.3
	else:
		look_point = target.global_position + target.global_transform.basis.y * LOOK_HEIGHT + (-target.global_transform.basis.z) * 8.0

	var rot_smooth := 16.0 if mode == Mode.COCKPIT else ROTATION_SMOOTH
	var xform := global_transform.looking_at(look_point, Vector3.UP)
	var q := global_transform.basis.get_rotation_quaternion().slerp(xform.basis.get_rotation_quaternion(), 1.0 - exp(-rot_smooth * delta))
	global_transform.basis = Basis(q)
