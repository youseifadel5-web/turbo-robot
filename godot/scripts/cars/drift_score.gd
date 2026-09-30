extends Node
## DriftScore — port of DriftScoreManager: combo multipliers + near-miss bonuses.

var car: RigidBody3D = null
var world_racers: Array = []   # nodes considered traffic/AI for near-miss

var total_score := 0.0
var current_combo := 0
var max_combo := 0
var best_angle := 0.0

var _combo_timer := 0.0
var _near_miss_timer := 0.0
const NEAR_MISS_RADIUS := 2.6
const NEAR_MISS_COOLDOWN := 0.8

signal score_changed(score: float, combo: int)
signal near_miss(bonus: int)


func setup(target_car: RigidBody3D, racers: Array) -> void:
	car = target_car
	world_racers = racers
	if car.has_signal("drift_score_tick"):
		car.connect("drift_score_tick", Callable(self, "_on_drift_tick"))


func reset_score() -> void:
	total_score = 0.0
	current_combo = 0
	max_combo = 0
	best_angle = 0.0
	score_changed.emit(0.0, 0)


func _process(delta: float) -> void:
	if _combo_timer > 0.0:
		_combo_timer -= delta
		if _combo_timer <= 0.0:
			current_combo = 0
	_near_miss_timer -= delta
	if car == null:
		return
	var speed_kmh: float = car.get("speed_kmh")
	if speed_kmh > 40.0 and _near_miss_timer <= 0.0:
		_check_near_miss()


func _on_drift_tick(tick: float) -> void:
	var mult := 1.0 + current_combo * 0.18
	total_score += tick * mult * 10.0
	current_combo = mini(current_combo + 1, 12)
	max_combo = maxi(max_combo, current_combo)
	_combo_timer = 1.25
	var angle: float = absf(car.get("drift_angle"))
	if angle > best_angle:
		best_angle = angle
	score_changed.emit(total_score, current_combo)


func _check_near_miss() -> void:
	for other in world_racers:
		if other == car or not is_instance_valid(other):
			continue
		if other is RigidBody3D and car.global_position.distance_to(other.global_position) < NEAR_MISS_RADIUS:
			var bonus := 350 + current_combo * 40
			total_score += bonus
			_near_miss_timer = NEAR_MISS_COOLDOWN
			near_miss.emit(bonus)
			score_changed.emit(total_score, current_combo)
			Sfx.play("SFX/sfx_overtake", 0.5)
			return
