extends CanvasLayer
## Garage port — 360° studio: carousel, stats bars, buy/select, live paint/rims/neon.
## The 3D studio itself (rotating car) is driven by game.gd's GarageStudio.

signal race_requested
signal exit_requested

var _root: Control
var _panel: PanelContainer
var _car_name: Label
var _car_class: Label
var _stats_box: VBoxContainer
var _action_btn: Button
var _custom_box: VBoxContainer
var _coins_label: Label
var _stage: Node3D = null      # set by game.gd for the 360° studio

var _index := 0
var _custom_page := 0          # 0 main, 1 paints, 2 rims, 3 neon, 4 upgrades
var selected_id := ""


func _ready() -> void:
	layer = 20
	_root = Control.new()
	_root.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.visible = false
	add_child(_root)
	_build()


func open(stage: Node3D) -> void:
	_stage = stage
	_root.visible = true
	_index = CarCatalog.CARS.find_custom(func(c): return c["carId"] == Save.selected_car_id)
	if _index < 0:
		_index = 0
	refresh()


func close() -> void:
	_root.visible = false


func is_open() -> bool:
	return _root.visible


func _build() -> void:
	# Left panel: car info + actions
	_panel = UiTheme.make_panel()
	_panel.set_anchors_preset(Control.PRESET_LEFT_WIDE)
	_panel.position = Vector2(24, 120)
	_panel.custom_minimum_size = Vector2(430, 0)
	_panel.size.y = -160
	_root.add_child(_panel)

	var v := UiTheme.vertical(8)
	_panel.add_child(v)

	var header := UiTheme.horizontal(12)
	_car_name = UiTheme.make_label("Falcon S", 34, UiTheme.CYAN)
	_car_class = UiTheme.make_label("CLASS B", 22, UiTheme.MAGENTA)
	header.add_child(_car_name)
	header.add_child(_car_class)
	v.add_child(header)

	_coins_label = UiTheme.make_label("", 22, Color(1.0, 0.84, 0.3))
	v.add_child(_coins_label)

	_stats_box = UiTheme.vertical(4)
	v.add_child(_stats_box)

	_custom_box = UiTheme.vertical(4)
	v.add_child(_custom_box)

	_action_btn = UiTheme.make_button("RACE", UiTheme.GOOD, 30)
	_action_btn.custom_minimum_size = Vector2(0, 64)
	_action_btn.pressed.connect(_on_action)
	v.add_child(_action_btn)

	# Carousel arrows
	var left := UiTheme.make_button("◀", UiTheme.CYAN, 40)
	left.position = Vector2(520, -180)
	left.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	left.custom_minimum_size = Vector2(90, 90)
	left.pressed.connect(func(): _cycle(-1))
	_root.add_child(left)

	var right := UiTheme.make_button("▶", UiTheme.CYAN, 40)
	right.position = Vector2(-130, -180)
	right.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	right.custom_minimum_size = Vector2(90, 90)
	right.pressed.connect(func(): _cycle(1))
	_root.add_child(right)

	# Top bar: customize pages + back
	var top := UiTheme.horizontal(10)
	top.position = Vector2(-640, 24)
	top.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	_root.add_child(top)
	for entry in [["MAIN", 0], ["PAINT", 1], ["RIMS", 2], ["NEON", 3], ["UPGRADE", 4]]:
		var b := UiTheme.make_button(L10n.tr_neon("garage.%s" % entry[0].to_lower()) if entry[0] != "MAIN" and entry[0] != "UPGRADE" else entry[0], UiTheme.DIM, 18)
		b.pressed.connect(func():
			_custom_page = entry[1]
			Sfx.play_ui("SFX/sfx_ui_click", 0.5)
			refresh())
		top.add_child(b)

	var back := UiTheme.make_button(L10n.tr_neon("garage.back"), UiTheme.MAGENTA, 22)
	back.position = Vector2(-160, -100)
	back.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	back.custom_minimum_size = Vector2(140, 56)
	back.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_back", 0.6)
		exit_requested.emit())
	_root.add_child(back)


func _cycle(dir: int) -> void:
	Sfx.play_ui("SFX/sfx_ui_click", 0.6)
	_index = (_index + dir + CarCatalog.CARS.size()) % CarCatalog.CARS.size()
	refresh()


func current_car() -> Dictionary:
	return CarCatalog.CARS[_index]


func refresh() -> void:
	var def := current_car()
	var car_id: String = def["carId"]
	_car_name.text = def["displayName"]
	_car_class.text = "CLASS %s" % CarCatalog.car_class_letter(int(def["carClass"]))
	_coins_label.text = "◉ %s %d" % [L10n.tr_neon("menu.coins"), Save.coins]

	for c in _stats_box.get_children():
		c.queue_free()
	for c in _custom_box.get_children():
		c.queue_free()

	var owned := Save.is_owned(car_id)
	_show_stats(def, owned)

	match _custom_page:
		0:
			_action_btn.text = L10n.tr_neon("garage.race")
			if owned:
				var select := UiTheme.make_button(
					L10n.tr_neon("garage.selected") if Save.selected_car_id == car_id else L10n.tr_neon("garage.select"),
					UiTheme.CYAN, 22)
				select.pressed.connect(func():
					Save.selected_car_id = car_id
					Save.save()
					refresh())
				_custom_box.add_child(select)
			else:
				var buy := UiTheme.make_button("%s — ◉ %d" % [L10n.tr_neon("garage.buy"), int(def["priceCoins"])], Color(1.0, 0.84, 0.3), 22)
				buy.pressed.connect(func():
					if Save.spend_coins(int(def["priceCoins"])):
						Save.get_owned(car_id)
						Save.save()
						Sfx.play("SFX/sfx_coin", 0.9)
					refresh())
				_custom_box.add_child(buy)
		1: _show_option_list(CarCatalog.PAINTS, "paintId", "paint")
		2: _show_option_list(CarCatalog.RIMS, "rimId", "rims")
		3: _show_option_list(CarCatalog.NEON_COLORS, "neonId", "neon")
		4: _show_upgrades(car_id)

	if _stage and _stage.has_method("show_car"):
		var owned_rec := Save.get_owned(car_id)
		_stage.show_car(car_id, owned_rec.get("paintId", "default"),
			owned_rec.get("rimId", "stock"), owned_rec.get("neonId", "cyan"))


func _show_stats(def: Dictionary, owned: bool) -> void:
	var rec := Save.get_owned(def["carId"]) if owned else {}
	var stats := def
	if owned:
		stats = CarCatalog.apply_upgrades(def, int(rec.get("engineLevel", 0)), int(rec.get("turboLevel", 0)),
			int(rec.get("brakesLevel", 0)), int(rec.get("nitroLevel", 0)))
	var rows := [
		["TOP SPEED", stats["topSpeedRating"]],
		["ACCELERATION", stats["accelerationRating"]],
		["HANDLING", stats["handlingRating"]],
		["BRAKING", stats["brakingRating"]],
		["DRIFT", stats["driftRating"]],
		["NITRO", stats["nitroRating"]],
	]
	for row in rows:
		var h := UiTheme.horizontal(6)
		var name_l := UiTheme.make_label(row[0], 16, UiTheme.DIM)
		name_l.custom_minimum_size = Vector2(150, 18)
		name_l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
		h.add_child(name_l)
		var bar := ProgressBar.new()
		bar.custom_minimum_size = Vector2(170, 14)
		bar.max_value = 100.0
		bar.value = row[1]
		var fill := StyleBoxFlat.new()
		fill.bg_color = UiTheme.CYAN
		var bg := StyleBoxFlat.new()
		bg.bg_color = Color(0.1, 0.12, 0.18)
		bar.add_theme_stylebox_override("fill", fill)
		bar.add_theme_stylebox_override("background", bg)
		h.add_child(bar)
		_stats_box.add_child(h)


func _show_option_list(options: Array, field: String, _label: String) -> void:
	var car_id: String = current_car()["carId"]
	var rec := Save.get_owned(car_id)
	var grid := GridContainer.new()
	grid.columns = 3
	grid.add_theme_constant_override("h_separation", 8)
	grid.add_theme_constant_override("v_separation", 8)
	_custom_box.add_child(grid)
	for opt in options:
		var hex: String = opt[2]
		var is_selected: bool = rec.get(field, "") == opt[0]
		var b := UiTheme.make_button(opt[1], UiTheme.TEXT, 18)
		if is_selected:
			b.text = "● " + opt[1]
		b.icon = ImageTexture.create_from_image(_solid_image(CarCatalog.hex_color(hex)))
		b.expand_icon = true
		b.pressed.connect(func():
			rec[field] = opt[0]
			Save.save()
			Sfx.play_ui("SFX/sfx_ui_click", 0.7)
			refresh())
		grid.add_child(b)


func _solid_image(c: Color) -> Image:
	var img := Image.create(24, 24, false, Image.FORMAT_RGB8)
	img.fill(c)
	return img


func _show_upgrades(car_id: String) -> void:
	var rec := Save.get_owned(car_id)
	if not Save.is_owned(car_id):
		_custom_box.add_child(UiTheme.make_label(L10n.tr_neon("garage.buy") + " — " + L10n.tr_neon("garage.owned"), 18, UiTheme.DIM))
		return
	var upgrades := [
		["ENGINE", "engineLevel"],
		["TURBO", "turboLevel"],
		["BRAKES", "brakesLevel"],
		["NITRO", "nitroLevel"],
	]
	for u in upgrades:
		var lvl := int(rec.get(u[1], 0))
		var h := UiTheme.horizontal(8)
		var name_l := UiTheme.make_label("%s  L%d/10" % [u[0], lvl], 18, UiTheme.TEXT)
		name_l.custom_minimum_size = Vector2(170, 30)
		name_l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
		h.add_child(name_l)
		var up := UiTheme.make_button("◉ %d" % CarCatalog.upgrade_cost(lvl), UiTheme.GOOD, 18)
		up.custom_minimum_size = Vector2(120, 36)
		up.disabled = lvl >= 10
		up.pressed.connect(func():
			var cost := CarCatalog.upgrade_cost(lvl)
			if Save.spend_coins(cost):
				rec[u[1]] = lvl + 1
				Save.save()
				Sfx.play("SFX/sfx_coin", 0.9)
				refresh())
		h.add_child(up)
		_custom_box.add_child(h)


func _on_action() -> void:
	Sfx.play_ui("SFX/sfx_ui_click", 0.8)
	race_requested.emit()


## Rotate the studio car slowly (called by game.gd each frame while open).
func spin_stage(delta: float) -> void:
	if _stage:
		_stage.rotate_y(delta * 0.6)
