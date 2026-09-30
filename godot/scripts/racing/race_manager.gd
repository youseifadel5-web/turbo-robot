extends Node
## RaceManager — port of NeonRush.Racing.RaceManager: countdown → laps → results.

signal race_finished(results: Dictionary)

enum State { IDLE, COUNTDOWN, RACING, FINISHED }

var track_builder: TrackBuilder = null
var track_config: Dictionary = {}
var countdown_seconds := 3

var player_car: RigidBody3D = null
var ai_cars: Array = []
var hud: Node = null
var drift_score: Node = null
var position_tracker: PositionTracker = null
var ghost_recorder: Node = null
var minimap: Node = null

var state: State = State.IDLE
var race_time := 0.0
var current_lap := 1
var player_position := 1
var _countdown_timer := 0.0
var _total_laps := 3
var _last_countdown_shown := 0
var _lap_start_time := 0.0
var _best_lap_time := 0.0
var _top_speed := 0.0


func start_race() -> void:
	_total_laps = maxi(1, int(track_config.get("laps", 3)))
	current_lap = 1
	race_time = 0.0
	_last_countdown_shown = countdown_seconds + 1
	player_position = 1
	_top_speed = 0.0
	_best_lap_time = 0.0
	_lap_start_time = 0.0
	if drift_score and drift_score.has_method("reset_score"):
		drift_score.reset_score()
	if hud:
		hud.set_lap(1, _total_laps)
		hud.set_position(1, 1 + ai_cars.size())
	state = State.COUNTDOWN
	_countdown_timer = countdown_seconds
	_set_cars_frozen(true)


func _process(delta: float) -> void:
	match state:
		State.COUNTDOWN:
			_countdown_timer -= delta
			if _countdown_timer > 0.0 and hud:
				hud.flash_checkpoint(str(ceil(_countdown_timer)))
			var shown := int(ceil(maxf(0.0, _countdown_timer)))
			if shown != _last_countdown_shown and _countdown_timer > 0.0:
				_last_countdown_shown = shown
				Sfx.play("SFX/sfx_countdown", 0.8)
			if _countdown_timer <= 0.0:
				state = State.RACING
				_set_cars_frozen(false)
				_lap_start_time = race_time
				if hud:
					hud.flash_checkpoint("GO!")
				Sfx.play("SFX/sfx_race_start", 0.9)
				if ghost_recorder and ghost_recorder.has_method("begin"):
					ghost_recorder.begin(player_car, String(track_config.get("trackId", "race")))
		State.RACING:
			race_time += delta
			if position_tracker:
				position_tracker.update()
				player_position = position_tracker.player_position
			if player_car:
				var kmh: float = player_car.get("speed_kmh")
				if kmh > _top_speed:
					_top_speed = kmh
			if hud:
				hud.set_progress(player_total_progress_normalized())
				hud.set_position(player_position, 1 + ai_cars.size())
			if ghost_recorder and ghost_recorder.has_method("update_recording"):
				ghost_recorder.update_recording(delta)


func player_total_progress_normalized() -> float:
	if position_tracker:
		return clampf(position_tracker.player_total_progress / maxf(1.0, _total_laps), 0.0, 1.0)
	return clampf(race_time / 120.0, 0.0, 1.0)


## Called by checkpoint Area3D signals (index order).
func on_player_checkpoint(index: int) -> void:
	if state != State.RACING:
		return
	if hud:
		hud.flash_checkpoint("CHECKPOINT")


## Called when the player crosses the finish line area.
func on_player_finish_line() -> void:
	if state != State.RACING:
		return
	if current_lap < _total_laps:
		var lap_time := race_time - _lap_start_time
		if lap_time > 5.0 and (_best_lap_time <= 0.0 or lap_time < _best_lap_time):
			_best_lap_time = lap_time
		_lap_start_time = race_time
		current_lap += 1
		if hud:
			hud.set_lap(current_lap, _total_laps)
			hud.flash_checkpoint("LAP %d" % current_lap)
		Sfx.play("SFX/sfx_checkpoint", 0.7)
	else:
		finish_race()


func finish_race() -> void:
	state = State.FINISHED
	_set_cars_frozen(true)
	Sfx.play("SFX/sfx_finish", 0.9)

	if ghost_recorder and player_car:
		if ghost_recorder.has_method("stop"):
			ghost_recorder.stop()
		if ghost_recorder.has_method("save_ghost"):
			ghost_recorder.save_ghost(Save.selected_car_id)

	Save.submit_time(String(track_config.get("trackId", "race")), race_time)
	var drift_total: float = drift_score.total_score if drift_score else 0.0
	var max_combo: int = drift_score.max_combo if drift_score else 0
	Save.record_race(race_time, drift_total)

	var results := {
		"position": player_position,
		"totalRacers": 1 + ai_cars.size(),
		"totalTime": race_time,
		"bestLap": _best_lap_time if _best_lap_time > 0.0 else race_time / maxf(1, _total_laps),
		"topSpeedKmh": _top_speed,
		"driftScore": drift_total,
		"maxCombo": max_combo,
	}
	race_finished.emit(results)


func _set_cars_frozen(frozen: bool) -> void:
	if player_car:
		player_car.freeze = frozen
		if frozen:
			player_car.linear_velocity = Vector3.ZERO
			player_car.angular_velocity = Vector3.ZERO
	for ai in ai_cars:
		if ai is RigidBody3D:
			ai.freeze = frozen
			if frozen:
				ai.linear_velocity = Vector3.ZERO
				ai.angular_velocity = Vector3.ZERO
