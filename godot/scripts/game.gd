extends Node3D
## Game — the SceneAssembler equivalent: builds the whole game at runtime.
## Menu → Garage → Race lifecycle, career progression, ghost mode, results.

var track_builder: TrackBuilder
var line := RacingLine.new()
var race_manager: Node = null
var position_tracker := PositionTracker.new()
var hud: CanvasLayer = null
var menu: CanvasLayer = null
var garage_screen: CanvasLayer = null
var career_screen: CanvasLayer = null
var settings_screen: CanvasLayer = null
var results_screen: CanvasLayer = null
var chase_cam: Camera3D = null
var env_node: WorldEnvironment = null
var sun: DirectionalLight3D = null
var weather := Weather.new()

var player_car: RigidBody3D = null
var ai_cars: Array = []
var ai_nodes: Array = []
var drift_score: Node = null
var damage_sys: Node = null
var wrong_way: Node = null
var ghost_node: Node = null
var minimap: Node = null
var traffic: Node = null
var input_bridge: Node = null
var garage_stage: Node3D = null
var current_config: Dictionary = {}
var in_garage := false


func _ready() -> void:
	randomize()
	Engine.max_fps = Save.target_fps
	Bus.set_master(Save.master_volume)
	Bus.set_music(Save.music_volume)
	Bus.set_sfx(Save.sfx_volume)
	Bus.set_engine(Save.engine_volume)

	sun = $Sun
	env_node = $WorldEnv
	chase_cam = $Camera3D

	_build_ui()

	var config: Dictionary = Session.next_track
	if config.is_empty():
		config = Session.make_track_config(1, 3, "Neon City Freeway", "neon_city_01")
	current_config = config
	_build_world(config)

	Sfx.play("Ambience/amb_city_loop", 0.35)


func _build_ui() -> void:
	hud = preload("res://scripts/ui/hud.gd").new()
	add_child(hud)
	menu = preload("res://scripts/ui/main_menu.gd").new()
	add_child(menu)
	menu.refresh_header()
	garage_screen = preload("res://scripts/ui/garage.gd").new()
	add_child(garage_screen)
	career_screen = preload("res://scripts/ui/career_screen.gd").new()
	add_child(career_screen)
	settings_screen = preload("res://scripts/ui/settings_screen.gd").new()
	add_child(settings_screen)
	results_screen = preload("res://scripts/ui/results_screen.gd").new()
	add_child(results_screen)

	menu.play_requested.connect(func(): _start_race(current_config))
	menu.garage_requested.connect(_open_garage)
	menu.events_requested.connect(func(): career_screen.open())
	menu.settings_requested.connect(func(): settings_screen.open())
	menu.ghost_requested.connect(_start_ghost_race)

	garage_screen.race_requested.connect(func():
		_close_garage()
		_start_race(current_config))
	garage_screen.exit_requested.connect(_close_garage)

	career_screen.event_selected.connect(func(config: Dictionary, _type: String):
		current_config = config
		menu.hide_menu()
		_rebuild_world(config)
		_start_race(config))
	career_screen.exit_requested.connect(func(): career_screen.close())

	settings_screen.exit_requested.connect(func(): settings_screen.close())

	results_screen.retry_requested.connect(func():
		results_screen.hide_results()
		_rebuild_world(current_config)
		_start_race(current_config))
	results_screen.menu_requested.connect(_back_to_menu)
	results_screen.next_requested.connect(func():
		results_screen.hide_results()
		var next_cfg := _next_career_config()
		if not next_cfg.is_empty():
			current_config = next_cfg
			_rebuild_world(next_cfg)
			_start_race(next_cfg)
		else:
			_back_to_menu())


func _build_world(config: Dictionary) -> void:
	# ---- environment ----
	DayNight.apply(env_node, sun, int(config.get("timeOfDay", 0)), "res://assets/skybox/sky_night_city_1k.png")

	# ---- track ----
	track_builder = TrackBuilder.new()
	track_builder.root = self
	track_builder.segment_length = 50.0
	track_builder.build_from_config(config)
	line.build(track_builder)

	# ---- city along the route ----
	var city := NeonCity.new()
	var city_root := Node3D.new()
	city_root.name = "NeonCity"
	add_child(city_root)
	city.build(city_root, line, city_root)

	# ---- player car ----
	var car_id: String = Save.selected_car_id
	var owned := Save.get_owned(car_id)
	var paint_hex := CarCatalog.paint_hex(owned.get("paintId", "default"))
	var rim_hex := CarCatalog.rim_hex(owned.get("rimId", "stock"))
	var neon_hex := CarCatalog.neon_hex(owned.get("neonId", "cyan"))
	var def := CarCatalog.get_car(car_id)
	var stats := CarCatalog.apply_upgrades(def, int(owned.get("engineLevel", 0)), int(owned.get("turboLevel", 0)),
		int(owned.get("brakesLevel", 0)), int(owned.get("nitroLevel", 0)))
	var car_root := CarBuilder.build_car(car_id, paint_hex, rim_hex, neon_hex, true)
	add_child(car_root)
	player_car = car_root.get_node("Body")
	player_car.apply_stats(stats)
	player_car.is_player = true
	var start := track_builder.start_point
	if start and line.is_valid() and line.points.size() > 1:
		var fwd := (line.points[1] - line.points[0]).normalized()
		player_car.global_position = start.global_position + fwd * 10.0 + Vector3(0, 0.8, 0)
		player_car.look_at(player_car.global_position + fwd, Vector3.UP)

	# ---- camera + HUD + input ----
	chase_cam.target = player_car
	hud.bind_car(player_car)
	input_bridge = preload("res://scripts/cars/car_input.gd").new()
	input_bridge.setup(player_car, hud)
	add_child(input_bridge)
	hud.camera_cycle_requested.connect(func():
		chase_cam.cycle_mode()
		hud.flash_checkpoint(chase_cam.mode_name()))

	# engine audio + vfx
	var eng := player_car.get_node_or_null("EngineAudio")
	if eng:
		eng.setup(player_car)
	var vfx := preload("res://scripts/cars/nitro_vfx.gd").new()
	vfx.setup(player_car, chase_cam)
	add_child(vfx)

	# ---- racing systems ----
	drift_score = preload("res://scripts/cars/drift_score.gd").new()
	add_child(drift_score)
	damage_sys = preload("res://scripts/racing/damage.gd").new()
	add_child(damage_sys)
	damage_sys.setup(player_car)
	wrong_way = preload("res://scripts/racing/wrong_way.gd").new()
	add_child(wrong_way)
	wrong_way.setup(line, player_car, hud)
	ghost_node = preload("res://scripts/racing/ghost.gd").new()
	add_child(ghost_node)
	minimap = preload("res://scripts/racing/minimap.gd").new()
	add_child(minimap)

	# ---- AI rivals ----
	ai_cars.clear()
	var waypoints := line.waypoints()
	var rival_ids := ["vortex_gt", "titan_x", "falcon_s"]
	if line.is_valid():
		for i in rival_ids.size():
			var rid: String = rival_ids[i]
			var rdef := CarCatalog.get_car(rid)
			var rroot := CarBuilder.build_car(rid, rdef["defaultPaintHex"], rdef["defaultPaintHex"], rdef["defaultNeonHex"], false)
			add_child(rroot)
			var rcar: RigidBody3D = rroot.get_node("Body")
			rcar.apply_stats(rdef)
			rcar.global_position = player_car.global_position + Vector3((i - 1) * 4.0, 0.0, -6.0 - i * 4.0)
			rcar.rotation.y = player_car.rotation.y
			var ai := preload("res://scripts/ai/racing_ai.gd").new()
			ai.setup(rcar, waypoints, Session.difficulty, (i - 1) * 1.5)
			add_child(ai)
			ai_cars.append(rcar)
			ai_nodes.append(rroot)

	# ---- positions + drift + minimap ----
	position_tracker.setup(line, player_car, ai_cars)
	drift_score.setup(player_car, ai_cars + _traffic_bodies())
	hud.setup_leaderboard(_leaderboard_names())
	minimap.setup(player_car, hud.minimap_rect)

	# ---- traffic ----
	if bool(config.get("hasTraffic", true)):
		traffic = preload("res://scripts/ai/traffic_manager.gd").new()
		add_child(traffic)
		traffic.setup(player_car, self, int(config.get("trafficDensity", 1)))

	# ---- race manager ----
	race_manager = preload("res://scripts/racing/race_manager.gd").new()
	add_child(race_manager)
	race_manager.track_builder = track_builder
	race_manager.track_config = config
	race_manager.player_car = player_car
	race_manager.ai_cars = ai_cars
	race_manager.hud = hud
	race_manager.drift_score = drift_score
	race_manager.position_tracker = position_tracker
	race_manager.ghost_recorder = ghost_node
	race_manager.minimap = minimap
	race_manager.race_finished.connect(_on_race_finished)

	# ---- weather ----
	weather.setup(env_node, player_car, [player_car])

	# ---- checkpoint/finish gates ----
	_wire_gates()

	# ---- garage studio (platform above the world) ----
	_build_garage_stage()


func _traffic_bodies() -> Array:
	return []


func _leaderboard_names() -> Array:
	return ["YOU", "AI VORTEX", "AI TITAN", "AI FALCON"]


func _wire_gates() -> void:
	var idx := 0
	for cp in track_builder.checkpoints:
		var area := _find_area(cp)
		if area:
			var i := idx
			area.body_entered.connect(func(body: Node3D):
				if body == player_car:
					race_manager.on_player_checkpoint(i))
		idx += 1
	if track_builder.finish_point:
		var area := _find_area(track_builder.finish_point)
		if area:
			area.body_entered.connect(func(body: Node3D):
				if body == player_car:
					trace_manager.on_player_finish_line())


func _find_area(node: Node3D) -> Area3D:
	for c in node.get_children():
		if c is Area3D:
			return c
	return null


func _build_garage_stage() -> void:
	garage_stage = Node3D.new()
	garage_stage.name = "GarageStage"
	garage_stage.script = preload("res://scripts/environment/garage_stage.gd")
	garage_stage.position = Vector3(0, 500, 0)
	add_child(garage_stage)


func _process(delta: float) -> void:
	if in_garage:
		garage_screen.spin_stage(delta)
	if race_manager and race_manager.state == 2:  # RACING
		hud.set_race_time(race_manager.race_time)
		hud.update_leaderboard(_live_positions())
	if drift_score:
		hud.set_drift(drift_score.total_score, drift_score.current_combo)


func _live_positions() -> Array:
	var rows := []
	var names := _leaderboard_names()
	var all := [player_car] + ai_cars
	for car in all:
		var i := all.find(car)
		var pos: int = position_tracker.get_position_of(car) if position_tracker else 1
		rows.append([pos, str(names[i]) if i < names.size() else "AI"])
	rows.sort_custom(func(a, b): return a[0] < b[0])
	var out := []
	for r in rows:
		out.append("P%d  %s" % [r[0], r[1]])
	return out


func _start_race(config: Dictionary) -> void:
	menu.hide_menu()
	garage_screen.close()
	career_screen.close()
	settings_screen.close()
	results_screen.hide_results()
	in_garage = false
	chase_cam.target = player_car
	chase_cam.global_position = player_car.global_position + Vector3(0, 3.2, -7.5)
	damage_sys.reset_damage()
	race_manager.start_race()


func _start_ghost_race() -> void:
	var ghost_data := preload("res://scripts/racing/ghost.gd").load_ghost(String(current_config.get("trackId", "neon_city_01")))
	if ghost_data.is_empty():
		menu.refresh_header()
		return
	_start_race(current_config)
	if not ghost_data.is_empty():
		var ghost_scene := CarBuilder.build_car("falcon_s", "0A0C10", "9AA0AA", "00F0FF", false)
		add_child(ghost_scene)
		ghost_node.start_playback(ghost_data, ghost_scene)


func _rebuild_world(config: Dictionary) -> void:
	# Remove old world content and rebuild (retry / new event / garage changes).
	for child in get_children():
		if child is CanvasLayer or child is Camera3D or child is DirectionalLight3D or child is WorldEnvironment:
			continue
		if child.name == "Sun" or child.name == "WorldEnv" or child.name == "Camera3D":
			continue
		child.queue_free()
	# Keep UI nodes; rebuild gameplay next frame.
	await get_tree().process_frame
	_build_world(config)


func _open_garage() -> void:
	in_garage = true
	menu.hide_menu()
	hud.visible = false
	garage_stage.show_car(Save.selected_car_id,
		Save.get_owned(Save.selected_car_id).get("paintId", "default"),
		Save.get_owned(Save.selected_car_id).get("rimId", "stock"),
		Save.get_owned(Save.selected_car_id).get("neonId", "cyan"))
	chase_cam.target = garage_stage.get_car_node()
	garage_screen.open(garage_stage)


func _close_garage() -> void:
	in_garage = false
	garage_screen.close()
	hud.visible = true
	chase_cam.target = player_car
	menu.show_menu()
	# Rebuild player car with new customization
	var car_id: String = Save.selected_car_id
	var owned := Save.get_owned(car_id)
	var def := CarCatalog.get_car(car_id)
	var stats := CarCatalog.apply_upgrades(def, int(owned.get("engineLevel", 0)), int(owned.get("turboLevel", 0)),
		int(owned.get("brakesLevel", 0)), int(owned.get("nitroLevel", 0)))
	player_car.apply_stats(stats)
	if player_car.has_node("CarMesh") and player_car.get_node("CarMesh").material_override is StandardMaterial3D:
		player_car.get_node("CarMesh").material_override.albedo_color = CarCatalog.hex_color(CarCatalog.paint_hex(owned.get("paintId", "default")))


func _on_race_finished(results: Dictionary) -> void:
	var stars := 1
	if results.get("position", 1) <= 3:
		stars = 2
	if results.get("position", 1) == 1:
		stars = 3
	if Session.race_type == "Career" and not Session.career_event.is_empty():
		var ev: Dictionary = Session.career_event
		if int(ev.get("type", 0)) == 1 and results.get("driftScore", 0.0) > 4000.0:
			stars = maxi(stars, 3)
		var coins := int(ev.get("rewardCoins", 800)) + stars * 150
		var xp := int(ev.get("rewardXp", 40)) + stars * 10
		Save.add_coins(coins)
		Save.add_xp(xp)
		Save.career_event += 1
		var events := CareerCatalog.get_events(Save.career_chapter)
		if Save.career_event > events.size():
			if Save.career_chapter < 3:
				Save.career_chapter += 1
				Save.career_event = 1
			else:
				Save.career_event = events.size()
		Save.career_stars += stars
		Save.save()
		Session.consume()
		results_screen.show_results(results, stars, coins, xp)
	else:
		var coins := 300 + stars * 100
		var xp := 15 + stars * 5
		Save.add_coins(coins)
		Save.add_xp(xp)
		Save.save()
		results_screen.show_results(results, stars, coins, xp)


func _next_career_config() -> Dictionary:
	if Save.career_chapter > 3:
		return {}
	var events := CareerCatalog.get_events(Save.career_chapter)
	var i: int = clampi(Save.career_event - 1, 0, events.size() - 1)
	var ev: Dictionary = events[i]
	Session.next_track = Session.make_track_config(int(ev["length"]), int(ev["laps"]), ev["title"], ev["id"])
	Session.race_type = "Career"
	Session.career_event = ev
	return Session.next_track


func _back_to_menu() -> void:
	results_screen.hide_results()
	menu.show_menu()
	chase_cam.target = player_car
	hud.visible = true
