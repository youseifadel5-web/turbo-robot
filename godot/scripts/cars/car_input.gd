extends Node
## CarInput — port of MobileCarInput + GamepadInputBridge:
## touch (from HUD buttons) + keyboard + gamepad + tilt, with steering assist.

var car: RigidBody3D = null
var hud: Node = null

var steering := 0.0
var throttle := 0.0
var brake := 0.0
var drift := false
var nitro := false

const STEERING_ASSIST := 0.15


func setup(target_car: RigidBody3D, race_hud: Node) -> void:
	car = target_car
	hud = race_hud


func _physics_process(_delta: float) -> void:
	if car == null:
		return
	var kb_steer := Input.get_axis("car_right", "car_left")
	var kb_vert := 0.0
	if Input.is_key_pressed(KEY_UP) or Input.is_key_pressed(KEY_W):
		kb_vert = 1.0
	elif Input.is_key_pressed(KEY_DOWN) or Input.is_key_pressed(KEY_S):
		kb_vert = -1.0
	var kb_throttle := maxf(0.0, kb_vert)
	var kb_brake := maxf(0.0, -kb_vert)
	if Input.is_key_pressed(KEY_SPACE):
		kb_brake = 1.0
	var kb_drift := Input.is_key_pressed(KEY_SHIFT)
	var kb_nitro := Input.is_key_pressed(KEY_CTRL)

	# Gamepad axes (left stick steer, triggers gas/brake)
	var joy_steer := Input.get_joy_axis(0, JOY_AXIS_LEFT_X) if Input.get_connected_joypads().size() > 0 else 0.0
	var joy_gas := 0.0
	var joy_brake := 0.0
	if Input.get_connected_joypads().size() > 0:
		joy_gas = maxf(0.0, Input.get_joy_axis(0, JOY_AXIS_TRIGGER_RIGHT))
		joy_brake = maxf(0.0, Input.get_joy_axis(0, JOY_AXIS_TRIGGER_LEFT))
		if Input.is_joy_button_pressed(0, JOY_BUTTON_A):
			joy_gas = 1.0
		if Input.is_joy_button_pressed(0, JOY_BUTTON_B):
			joy_brake = 1.0
		var abs_steer := absf(joy_steer)
		if abs_steer < 0.15:
			joy_steer = 0.0
		elif abs_steer > 1.0:
			joy_steer = signf(joy_steer)

	# Touch (HUD buttons)
	var touch := {}
	if hud and hud.has_method("touch_state"):
		touch = hud.touch_state()
	var touch_steer := 0.0
	if touch.get("left", false):
		touch_steer -= 1.0
	if touch.get("right", false):
		touch_steer += 1.0

	# Tilt scheme
	var tilt_steer := 0.0
	if Save.control_scheme == 2 and Input.get_accelerometer() != Vector3.ZERO:
		tilt_steer = clampf(Input.get_accelerometer().x * 0.7, -1.0, 1.0)

	var final_steer := touch_steer + tilt_steer + kb_steer + joy_steer
	final_steer = clampf(final_steer * Save.steering_sensitivity, -1.0, 1.0)

	# Steering assist toward velocity direction at speed
	if STEERING_ASSIST > 0.0 and car.get("speed_kmh") > 30.0:
		var vel: Vector3 = car.linear_velocity
		if vel.length_squared() > 1.0:
			var forward := -car.global_transform.basis.z
			var angle := rad_to_deg(atan2(forward.cross(vel.normalized()).y, forward.dot(vel.normalized())))
			final_steer = lerpf(final_steer, clampf(angle / 40.0, -1.0, 1.0), STEERING_ASSIST * 0.25)

	var final_throttle := maxf(maxf(float(touch.get("gas", false)), kb_throttle), maxf(joy_gas, 0.0))
	var final_brake := maxf(maxf(float(touch.get("brake", false)), kb_brake), joy_brake)
	var final_drift: bool = touch.get("drift", false) or kb_drift or Input.is_joy_button_pressed(0, JOY_BUTTON_X)
	var final_nitro: bool = touch.get("nitro", false) or kb_nitro or Input.is_joy_button_pressed(0, JOY_BUTTON_RIGHT_SHOULDER)

	car.set_input(final_steer, final_throttle, final_brake, final_drift, final_nitro)
