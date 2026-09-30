extends Node
## RacingAI — port of NeonRush.AI.RacingAI: waypoint following, corner braking,
## mistakes, overtakes, nitro logic, difficulty presets.
## IMPROVEMENT over Unity: per-rival lateral offset on the racing line so
## AI cars don't drive in one perfect single file (Unity known-limit fixed).

var difficulty := 1   # 0 Easy 1 Normal 2 Hard 3 Expert
var aggression := 0.5
var mistake_rate := 0.14

var car: RigidBody3D = null
var waypoints: PackedVector3Array = PackedVector3Array()
var lane_offset := 0.0

var _wp_index := 0
var _mistake_timer := 0.0
var _mistake_steer := 0.0
var _nitro_hold := 0.0
var _reverse_timer := 0.0
const WAYPOINT_REACH := 8.0
const LOOK_AHEAD := 18.0


func set_difficulty(value: int) -> void:
	difficulty = clampi(value, 0, 3)
	match difficulty:
		0: aggression = 0.25; mistake_rate = 0.28
		1: aggression = 0.5; mistake_rate = 0.14
		2: aggression = 0.75; mistake_rate = 0.06
		3: aggression = 0.92; mistake_rate = 0.02


func setup(target_car: RigidBody3D, wp: PackedVector3Array, diff: int, offset: float) -> void:
	car = target_car
	waypoints = wp
	lane_offset = offset
	set_difficulty(diff)
	_wp_index = mini(2, wp.size() - 1)


func _physics_process(delta: float) -> void:
	if car == null or waypoints.is_empty():
		return
	if car.freeze:
		return

	_advance_waypoint()
	var target_point := _look_ahead_point()
	var to_target := target_point - car.global_position
	to_target.y = 0.0
	if to_target.length() < 0.5:
		to_target = -car.global_transform.basis.z

	var forward := -car.global_transform.basis.z
	var steer := rad_to_deg(atan2(forward.cross(to_target.normalized()).y, forward.dot(to_target.normalized()))) / 35.0
	steer = clampf(steer, -1.0, 1.0)

	# Mistakes: brief wrong steer
	_mistake_timer -= delta
	if _mistake_timer <= 0.0 and randf() < mistake_rate * delta * 8.0:
		_mistake_steer = randf_range(-0.55, 0.55)
		_mistake_timer = randf_range(0.2, 0.6)
	if _mistake_timer > 0.0:
		steer = clampf(steer + _mistake_steer, -1.0, 1.0)

	var corner_factor := 1.0 - clampf(absf(steer), 0.0, 1.0)
	var throttle: float = lerpf(0.35, 1.0, corner_factor)
	var brake := 0.45 if absf(steer) > 0.7 and car.get("speed_kmh") > 90.0 else 0.0

	# Skill cap
	var skill_cap := 0.82 if difficulty == 0 else (0.92 if difficulty == 1 else 1.0)
	throttle *= skill_cap

	# Overtake nudge
	if aggression > 0.45 and randf() < 0.02:
		steer += (0.25 if randf() > 0.5 else -0.25) * aggression
		throttle = maxf(throttle, 0.9)

	# Nitro on straights
	_nitro_hold -= delta
	var use_nitro := false
	if _nitro_hold <= 0.0 and corner_factor > 0.85 and car.get("speed_kmh") > 60.0 and aggression > 0.4:
		use_nitro = car.get("nitro_normalized") > 0.25
		if use_nitro:
			_nitro_hold = 1.4

	var drift: bool = absf(steer) > 0.55 and float(car.get("speed_kmh")) > 50.0 and aggression > 0.5

	# Stuck detection: reverse out
	var speed: float = car.get("speed_kmh")
	if speed < 6.0:
		_reverse_timer += delta
	else:
		_reverse_timer = 0.0
	if _reverse_timer > 1.5:
		steer = -steer
		throttle = 0.0
		brake = 1.0
		if _reverse_timer > 3.0:
			_reverse_timer = 0.0

	car.set_input(steer, throttle, brake, drift, use_nitro)


func _advance_waypoint() -> void:
	var wp: Vector3 = waypoints[_wp_index]
	var flat := Vector3(wp.x, car.global_position.y, wp.z)
	if flat.distance_squared_to(car.global_position) < WAYPOINT_REACH * WAYPOINT_REACH:
		_wp_index = (_wp_index + 1) % waypoints.size()


func _look_ahead_point() -> Vector3:
	var next := (_wp_index + 1) % waypoints.size()
	var a: Vector3 = waypoints[_wp_index]
	var b: Vector3 = waypoints[next]
	var dir := (b - a).normalized() if a.distance_to(b) > 0.1 else Vector3.FORWARD
	# lateral offset so rivals use different lines
	var side := dir.cross(Vector3.UP)
	return a + dir * LOOK_AHEAD + side * lane_offset
