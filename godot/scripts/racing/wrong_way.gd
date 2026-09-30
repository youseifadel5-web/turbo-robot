extends Node
## WrongWayDetector port — warns when driving against the track direction.

var line: RacingLine = null
var car: RigidBody3D = null
var hud: Node = null
const MIN_SPEED_KMH := 20.0
const TRIGGER_DOT := -0.35
const HOLD_SECONDS := 0.9

var is_wrong_way := false
var _hold_timer := 0.0
var _beep_cooldown := 0.0


func setup(racing_line: RacingLine, target_car: RigidBody3D, race_hud: Node) -> void:
	line = racing_line
	car = target_car
	hud = race_hud


func _process(delta: float) -> void:
	if line == null or not line.is_valid() or car == null:
		return
	var speed_kmh: float = car.get("speed_kmh")
	if speed_kmh < MIN_SPEED_KMH:
		_hold_timer = 0.0
		_set_state(false)
		return
	var tangent := line.get_tangent(car.global_position)
	var forward := -car.global_transform.basis.z
	var dot := forward.dot(tangent)
	if dot < TRIGGER_DOT:
		_hold_timer += delta
	else:
		_hold_timer = 0.0
	_set_state(_hold_timer >= HOLD_SECONDS)
	if is_wrong_way:
		_beep_cooldown -= delta
		if _beep_cooldown <= 0.0:
			Sfx.play("SFX/sfx_wrongway", 0.7)
			_beep_cooldown = 1.6


func _set_state(wrong: bool) -> void:
	if wrong == is_wrong_way:
		return
	is_wrong_way = wrong
	if hud and hud.has_method("show_wrong_way"):
		hud.show_wrong_way(wrong)
