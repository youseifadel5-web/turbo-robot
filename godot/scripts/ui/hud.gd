extends CanvasLayer
## RuntimeRaceHUD port — speedo + nitro ring, lap/position/time, drift combo,
## minimap, countdown flash, wrong-way, touch controls, CAM cycle, leaderboard.

var car: RigidBody3D = null
var input_bridge: Node = null

var _speedo: Control
var _speed_label: Label
var _gear_label: Label
var _drift_label: Label
var _lap_label: Label
var _pos_label: Label
var _time_label: Label
var _best_label: Label
var _flash_label: Label
var _wrong_way: Label
var _nitro_fill: Control
var _touch_state := {"left": false, "right": false, "gas": false, "brake": false, "drift": false, "nitro": false}
var _flash_timer := 0.0
var _wrong_way_on := false

var leaderboard_panel: VBoxContainer
var _lb_rows: Array = []
var minimap_rect: TextureRect

signal camera_cycle_requested


func bind_car(target_car: RigidBody3D) -> void:
	car = target_car


func set_lap(current: int, total: int) -> void:
	_lap_label.text = "%s %d/%d" % [L10n.tr_neon("hud.lap"), current, total]


func set_position(pos: int, total: int) -> void:
	_pos_label.text = "P%d/%d" % [pos, total]


func set_progress(_p: float) -> void:
	pass  # radial track progress kept minimal; lap + position carry the info


func flash_checkpoint(text: String) -> void:
	_flash_label.text = text
	_flash_label.modulate.a = 1.0
	_flash_timer = 1.0


func show_wrong_way(on: bool) -> void:
	_wrong_way_on = on
	_wrong_way.visible = on


func _ready() -> void:
	layer = 10
	var root := Control.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(root)
	_build(root)


func _build(root: Control) -> void:
	# ---- top-left: lap / position / time ----
	var tl := UiTheme.make_panel()
	tl.position = Vector2(24, 20)
	tl.custom_minimum_size = Vector2(300, 130)
	root.add_child(tl)
	var v := UiTheme.vertical(2)
	tl.add_child(v)
	_lap_label = UiTheme.make_label("LAP 1/3", 26, UiTheme.CYAN)
	_pos_label = UiTheme.make_label("P1/4", 26, UiTheme.MAGENTA)
	var h := UiTheme.horizontal(16)
	_time_label = UiTheme.make_label("00.00", 24)
	_best_label = UiTheme.make_label("BEST --.--", 18, UiTheme.DIM)
	h.add_child(_time_label)
	h.add_child(_best_label)
	v.add_child(_lap_label)
	v.add_child(_pos_label)
	v.add_child(h)

	# ---- top-right: leaderboard mini panel ----
	var tr := UiTheme.make_panel()
	tr.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	tr.position = Vector2(-280, 20)
	tr.custom_minimum_size = Vector2(256, 150)
	root.add_child(tr)
	leaderboard_panel = UiTheme.vertical(2)
	tr.add_child(leaderboard_panel)

	# ---- center flash ----
	_flash_label = UiTheme.make_label("", UiTheme.FONT_SIZE_HUGE, UiTheme.CYAN)
	_flash_label.set_anchors_preset(Control.PRESET_CENTER)
	_flash_label.position = Vector2(-200, -160)
	_flash_label.custom_minimum_size = Vector2(400, 130)
	_flash_label.modulate.a = 0.0
	root.add_child(_flash_label)

	# ---- wrong way ----
	_wrong_way = UiTheme.make_label(L10n.tr_neon("hud.wrongway"), 44, Color(1.0, 0.25, 0.2))
	_wrong_way.set_anchors_preset(Control.PRESET_CENTER)
	_wrong_way.position = Vector2(-300, 120)
	_wrong_way.custom_minimum_size = Vector2(600, 60)
	_wrong_way.visible = false
	root.add_child(_wrong_way)

	# ---- bottom-left: speedo + nitro ----
	_speedo = SpeedoGauge.new()
	_speedo.position = Vector2(40, -300)
	_speedo.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	_speedo.custom_minimum_size = Vector2(220, 220)
	root.add_child(_speedo)
	_speed_label = UiTheme.make_label("0", 44, UiTheme.TEXT)
	_speed_label.position = Vector2(70, -235)
	_speed_label.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	_speed_label.custom_minimum_size = Vector2(160, 50)
	root.add_child(_speed_label)
	var kmh := UiTheme.make_label(L10n.tr_neon("hud.speed"), 18, UiTheme.DIM)
	kmh.position = Vector2(70, -185)
	kmh.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	kmh.custom_minimum_size = Vector2(160, 24)
	root.add_child(kmh)
	_gear_label = UiTheme.make_label("G1", 22, UiTheme.CYAN)
	_gear_label.position = Vector2(70, -290)
	_gear_label.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	_gear_label.custom_minimum_size = Vector2(160, 26)
	root.add_child(_gear_label)

	# ---- drift score (bottom-center-left) ----
	_drift_label = UiTheme.make_label("DRIFT 0", 24, UiTheme.MAGENTA)
	_drift_label.position = Vector2(300, -60)
	_drift_label.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	_drift_label.custom_minimum_size = Vector2(360, 30)
	root.add_child(_drift_label)

	_nitro_fill = UiTheme.make_label("", 20, UiTheme.CYAN)
	_nitro_fill.position = Vector2(300, -95)
	_nitro_fill.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	_nitro_fill.custom_minimum_size = Vector2(360, 26)
	root.add_child(_nitro_fill)

	# ---- minimap slot (texture assigned by MinimapController) ----
	var map_panel := UiTheme.make_panel()
	map_panel.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	map_panel.position = Vector2(-280, 190)
	map_panel.custom_minimum_size = Vector2(240, 240)
	root.add_child(map_panel)
	minimap_rect = TextureRect.new()
	minimap_rect.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	minimap_rect.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	map_panel.add_child(minimap_rect)

	_build_touch_controls(root)


func _build_touch_controls(root: Control) -> void:
	# CAM button top-center-right of speedo
	var cam := UiTheme.make_button("CAM", UiTheme.CYAN, 20)
	cam.position = Vector2(280, -130)
	cam.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	cam.pressed.connect(func():
		camera_cycle_requested.emit()
		Sfx.play_ui("SFX/sfx_ui_click", 0.6))
	root.add_child(cam)

	# Steering: left/right buttons bottom-left
	for side in ["left", "right"]:
		var b := UiTheme.make_button("◀" if side == "left" else "▶", UiTheme.CYAN, 44)
		b.custom_minimum_size = Vector2(120, 120)
		b.position = Vector2(40 if side == "left" else 180, -140)
		b.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
		b.toggle_mode = true
		b.toggled.connect(func(pressed: bool) -> void: _touch_state[side] = pressed)
		root.add_child(b)

	# Pedals: brake / gas bottom-right
	var brake := UiTheme.make_button("BRAKE", Color(1.0, 0.3, 0.25), 24)
	brake.custom_minimum_size = Vector2(140, 120)
	brake.position = Vector2(-300, -140)
	brake.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	brake.toggle_mode = true
	brake.toggled.connect(func(p: bool) -> void: _touch_state["brake"] = p)
	root.add_child(brake)

	var gas := UiTheme.make_button("GAS", UiTheme.GOOD, 28)
	gas.custom_minimum_size = Vector2(160, 140)
	gas.position = Vector2(-140, -160)
	gas.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	gas.toggle_mode = true
	gas.toggled.connect(func(p: bool) -> void: _touch_state["gas"] = p)
	root.add_child(gas)

	# Drift + Nitro above pedals
	var drift := UiTheme.make_button("DRIFT", UiTheme.MAGENTA, 22)
	drift.custom_minimum_size = Vector2(140, 80)
	drift.position = Vector2(-300, -240)
	drift.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	drift.toggle_mode = true
	drift.toggled.connect(func(p: bool) -> void: _touch_state["drift"] = p)
	root.add_child(drift)

	var nitro := UiTheme.make_button("NITRO", Color(0.3, 0.9, 1.0), 22)
	nitro.custom_minimum_size = Vector2(160, 80)
	nitro.position = Vector2(-140, -250)
	nitro.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	nitro.toggle_mode = true
	nitro.toggled.connect(func(p: bool) -> void: _touch_state["nitro"] = p)
	root.add_child(nitro)


func _process(delta: float) -> void:
	if _flash_timer > 0.0:
		_flash_timer -= delta
		_flash_label.modulate.a = clampf(_flash_timer, 0.0, 1.0)
	if _wrong_way_on:
		_wrong_way.modulate.a = 0.6 + 0.4 * sin(Time.get_ticks_msec() * 0.012)

	if car == null:
		return
	var kmh: float = car.get("speed_kmh")
	_speed_label.text = "%d" % int(round(kmh))
	_gear_label.text = "G%d" % car.get("current_gear")
	_speedo.queue_redraw()
	if _nitro_fill:
		var n: float = car.get("nitro_normalized")
		var blocks := int(round(n * 14))
		_nitro_fill.text = "NITRO " + "▮".repeat(blocks) + "▯".repeat(14 - blocks)


func set_race_time(t: float) -> void:
	_time_label.text = "%.2f" % t
	var best: float = Save.get_best_time("")
	if best > 0.0:
		_best_label.text = "BEST %.2f" % best


func set_drift(score: float, combo: int) -> void:
	if combo > 0:
		_drift_label.text = "DRIFT %d  x%d" % [int(score), combo]
	else:
		_drift_label.text = "DRIFT %d" % int(score)


func setup_leaderboard(names: Array) -> void:
	for c in leaderboard_panel.get_children():
		c.queue_free()
	_lb_rows.clear()
	for n in names:
		var l := UiTheme.make_label(str(n), 20, UiTheme.TEXT)
		leaderboard_panel.add_child(l)
		_lb_rows.append(l)


func update_leaderboard(rows: Array) -> void:
	for i in mini(rows.size(), _lb_rows.size()):
		if _lb_rows[i] is Label:
			_lb_rows[i].text = str(rows[i])


## Touch state consumed by CarInput each frame.
func touch_state() -> Dictionary:
	return _touch_state


class SpeedoGauge:
	extends Control
	var speed_norm := 0.0
	var nitro_norm := 0.0
	const START_ANGLE := 135.0   # degrees
	const SWEEP := 270.0

	func _draw() -> void:
		var center := size * 0.5
		var radius := minf(size.x, size.y) * 0.42
		var width := 14.0
		draw_arc(center, radius, deg_to_rad(START_ANGLE), deg_to_rad(START_ANGLE + SWEEP), 48, Color(0.08, 0.1, 0.16, 0.9), width + 4)
		var parent_hud := get_parent().get_parent()
		if parent_hud and parent_hud.car:
			speed_norm = clampf(parent_hud.car.get("speed_kmh") / 300.0, 0.0, 1.0)
			nitro_norm = parent_hud.car.get("nitro_normalized")
		# speed arc — cyan to magenta
		var speed_angle := deg_to_rad(START_ANGLE + SWEEP * speed_norm)
		draw_arc(center, radius, deg_to_rad(START_ANGLE), speed_angle, 48, UiTheme.CYAN if speed_norm < 0.8 else UiTheme.MAGENTA, width)
		# nitro inner arc
		var nitro_angle := deg_to_rad(START_ANGLE + SWEEP * nitro_norm)
		draw_arc(center, radius - 22, deg_to_rad(START_ANGLE), nitro_angle, 32, Color(0.3, 0.95, 1.0, 0.9), 8)
