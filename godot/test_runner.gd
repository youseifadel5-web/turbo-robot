extends Node
## Headless smoke test: loads the game, starts a race, simulates frames,
## checks physics / drift / nitro / positions / finish / save / garage.
## To enable, add this line under [autoload] in project.godot:
##   TestRunner="*res://test_runner.gd"
## then run:  godot --headless --quit-after 2500

var game: Node = null
var errors: Array = []


func _ready() -> void:
	await get_tree().process_frame
	await get_tree().process_frame
	await _run()


func _run() -> void:
	var packed: PackedScene = load("res://main.tscn")
	if packed == null:
		_fail("main.tscn failed to load")
		return
	game = packed.instantiate()
	get_tree().root.add_child(game)
	for i in 5:
		await get_tree().process_frame
	if game.player_car == null:
		_fail("player car missing")
		return
	if game.track_builder == null or game.track_builder.segments.is_empty():
		_fail("track not built")
		return
	if not game.line.is_valid():
		_fail("racing line invalid")
		return
	print("TEST track segments: %d, line points: %d, AI cars: %d" % [
		game.track_builder.segments.size(), game.line.points.size(), game.ai_cars.size()])

	# start the race via manager
	game.race_manager.start_race()
	if game.race_manager.state != 1:  # COUNTDOWN
		_fail("race not in countdown, state=%d" % game.race_manager.state)
		return
	for i in 240:  # 4 s countdown
		await get_tree().process_frame
	if game.race_manager.state != 2:  # RACING
		_fail("race not racing after countdown, state=%d" % game.race_manager.state)
		return
	print("TEST race is RACING after countdown")

	# take direct control of the player car (disable the input bridge)
	game.input_bridge.set_physics_process(false)
	# simulate player input: full throttle for 5 s
	for i in 300:
		game.player_car.set_input(0.0, 1.0, 0.0, false, false)
		await get_tree().process_frame
		if i % 60 == 0:
			print("DBG t=%.1f pos=%s vel=%s spd=%.1f onfloor=%s" % [i/60.0,
				game.player_car.global_position, game.player_car.linear_velocity,
				game.player_car.get("speed_kmh"), game.player_car.get_colliding_bodies() ])
	var kmh: float = game.player_car.get("speed_kmh")
	print("TEST speed after 5s throttle: %.1f km/h" % kmh)
	if kmh < 30.0:
		_fail("car not moving: %.1f km/h" % kmh)
		return
	game.position_tracker.update()
	var prog: float = game.position_tracker.player_total_progress
	print("TEST tracker progress after straight drive: %.3f (pos z=%.0f, total_dist=%.0f)" % [
		prog, game.player_car.global_position.z, game.line.total_distance])
	if prog < 0.02:
		_fail("progress not advancing along racing line: %.3f" % prog)
		return

	# drift test right away at speed
	game.player_car.set_input(1.0, 1.0, 0.0, true, false)
	for i in 120:
		await get_tree().process_frame
	print("TEST drift state: %s, angle: %.0f, score: %.0f" % [
		game.player_car.get("is_drifting"), game.player_car.get("drift_angle"), game.drift_score.total_score])

	# nitro test
	for i in 60:
		game.player_car.set_input(0.0, 1.0, 0.0, false, true)
		await get_tree().process_frame
	if not game.player_car.get("is_nitro_active"):
		_fail("nitro not activating")
		return
	print("TEST nitro OK, speed: %.1f km/h" % game.player_car.get("speed_kmh"))

	# position tracker sanity
	game.position_tracker.update()
	print("TEST player position: P%d, progress: %.2f, traffic active: %s" % [
		game.position_tracker.player_position, game.position_tracker.player_total_progress,
		game.traffic != null and game.traffic._active.size() > 0])

	# force-finish race and check results flow
	game.race_manager.current_lap = game.race_manager._total_laps
	game.race_manager.on_player_finish_line()
	for i in 30:
		await get_tree().process_frame
	if game.race_manager.state != 3:
		_fail("race not finished after final lap, state=%d" % game.race_manager.state)
		return
	print("TEST race finished, stars/rewards screen shown: %s" % game.results_screen._root.visible)

	# save persisted
	if Save.best_time <= 0.0:
		_fail("best time not recorded")
		return
	print("TEST save best time: %.2f" % Save.best_time)

	# garage flow
	game._open_garage()
	for i in 10:
		await get_tree().process_frame
	print("TEST garage open: %s, stage car: %s" % [game.garage_screen.is_open(), game.garage_stage.get_car_node() != null])
	game._close_garage()

	print("ALL_TESTS_PASSED")
	get_tree().quit(0)


func _fail(msg: String) -> void:
	push_error("TEST FAILURE: " + msg)
	get_tree().quit(1)


func set_error() -> void:
	pass
