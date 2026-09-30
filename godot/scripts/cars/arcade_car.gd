extends RigidBody3D
## ArcadeCar — port of NeonRush.Cars.ArcadeCarController.
## Same arcade model: acceleration force, speed-sensitive yaw rate,
## lateral grip lerp, nitro, drift detection and gear calc.

signal nitro_started
signal nitro_ended
signal drift_score_tick(tick: float)
signal collided(impact: float)

var stats: Dictionary = {}
var is_player := false

var acceleration := 28.0
var reverse_acceleration := 12.0
var max_speed := 55.0            # m/s
var steering_power := 78.0       # deg/s at full speed factor
var brake_power := 35.0
var lateral_grip := 8.0
var nitro_force := 22.0
var nitro_capacity := 100.0
var nitro_drain := 28.0
var nitro_recharge := 8.0
var drift_grip_multiplier := 0.35
var drift_steer_multiplier := 1.25
var min_drift_speed := 5.0

var _steering := 0.0
var _throttle := 0.0
var _brake := 0.0
var _drift_pressed := false
var _nitro_pressed := false
var _nitro := 100.0
var nitro_normalized := 1.0
var _was_nitro := false
var _grip_override := -1.0

var speed_kmh := 0.0
var is_drifting := false
var is_nitro_active := false
var drift_angle := 0.0
var current_gear := 1
var car_class := 0

@onready var _v_before := Vector3.ZERO


func _ready() -> void:
	center_of_mass_mode = RigidBody3D.CENTER_OF_MASS_MODE_CUSTOM
	center_of_mass = Vector3(0, -0.45, 0)
	angular_damp = 4.0
	linear_damp = 0.05
	contact_monitor = true
	max_contacts_reported = 4
	body_entered.connect(_on_body_entered)


func apply_stats(s: Dictionary) -> void:
	stats = s
	car_class = int(s.get("carClass", 0))
	acceleration = s.get("acceleration", 28.0)
	reverse_acceleration = s.get("reverseAcceleration", 12.0)
	max_speed = s.get("maxSpeedKmh", 265.0) / 3.6
	brake_power = s.get("brakePower", 35.0)
	steering_power = s.get("steeringPower", 78.0)
	lateral_grip = s.get("lateralGrip", 8.0)
	drift_grip_multiplier = s.get("driftGripMultiplier", 0.32)
	nitro_capacity = s.get("nitroCapacity", 100.0)
	nitro_force = s.get("nitroForce", 22.0)
	nitro_drain = s.get("nitroDrainPerSecond", 26.0)
	nitro_recharge = s.get("nitroRechargePerSecond", 8.0)
	mass = s.get("mass", 1280.0)
	_nitro = minf(_nitro, nitro_capacity)


func set_input(steer: float, gas: float, brake_in: float, drift: bool, boost: bool) -> void:
	_steering = clampf(steer, -1.0, 1.0)
	_throttle = clampf(gas, 0.0, 1.0)
	_brake = clampf(brake_in, 0.0, 1.0)
	_drift_pressed = drift
	_nitro_pressed = boost


func set_grip_multiplier(mult: float) -> void:
	_grip_override = clampf(mult, 0.4, 1.2)


func _physics_process(delta: float) -> void:
	_v_before = linear_velocity
	var forward := -global_transform.basis.z
	var speed := linear_velocity.length()
	var local_vel := global_transform.basis.inverse() * linear_velocity
	speed_kmh = speed * 3.6

	if freeze:
		return

	# Throttle / brake (Unity ForceMode.Acceleration == force ignoring mass)
	if _throttle > 0.01:
		apply_central_force(forward * acceleration * _throttle * mass)
	if _brake > 0.01:
		apply_central_force(-forward * brake_power * _brake * mass)

	# Steering: speed-sensitive target yaw rate (arcade, crisp)
	var speed_factor := clampf(speed / 6.0, 0.0, 1.0)
	var target_yaw_deg := _steering * steering_power * (0.35 + speed_factor * 0.65)
	if _drift_pressed:
		target_yaw_deg *= drift_steer_multiplier
	if local_vel.z < -0.5:
		target_yaw_deg = -target_yaw_deg   # reversing inverts steering
	var target_yaw := deg_to_rad(target_yaw_deg)
	angular_velocity.y = lerpf(angular_velocity.y, target_yaw, clampf(delta * 7.0, 0.0, 1.0))

	# Lateral grip / drift
	var grip := lateral_grip * (drift_grip_multiplier if _drift_pressed else 1.0)
	if _grip_override >= 0.0:
		grip = lateral_grip * _grip_override * (drift_grip_multiplier if _drift_pressed else 1.0)
	local_vel.x = lerpf(local_vel.x, 0.0, clampf(grip * delta, 0.0, 1.0))
	linear_velocity = global_transform.basis * local_vel

	# Nitro
	is_nitro_active = _nitro_pressed and _nitro > 0.5 and _throttle > 0.1
	if is_nitro_active:
		apply_central_force(forward * nitro_force * mass)
		_nitro -= nitro_drain * delta
		if not _was_nitro:
			nitro_started.emit()
			_was_nitro = true
	else:
		if _was_nitro:
			nitro_ended.emit()
			_was_nitro = false
		_nitro += nitro_recharge * delta
	_nitro = clampf(_nitro, 0.0, nitro_capacity)

	# Soft speed limit
	if linear_velocity.length() > max_speed:
		linear_velocity = linear_velocity.normalized() * max_speed

	# Drift detection + angle
	var side_speed := absf(local_vel.x)
	is_drifting = _drift_pressed and side_speed > 0.6 and speed > min_drift_speed
	if is_drifting and linear_velocity.length() > 0.5:
		var v_flat := Vector3(linear_velocity.x, 0, linear_velocity.z)
		drift_angle = rad_to_deg(forward.signed_angle_to(v_flat.normalized(), Vector3.UP))
		drift_score_tick.emit(absf(drift_angle) * (speed * 0.15) * delta)
	else:
		drift_angle = 0.0

	nitro_normalized = _nitro / maxf(1.0, nitro_capacity)
	current_gear = _gear(speed_kmh)


func _gear(kmh: float) -> int:
	if kmh < 20.0: return 1
	if kmh < 45.0: return 2
	if kmh < 75.0: return 3
	if kmh < 110.0: return 4
	if kmh < 150.0: return 5
	return 6


func _on_body_entered(_body: Node) -> void:
	var impact := (_v_before - linear_velocity).length()
	if impact > 1.0:
		collided.emit(impact)
