extends CanvasLayer
## MainMenu port — PLAY / GARAGE / EVENTS / SETTINGS / RACE GHOST + coins/level header.

signal play_requested
signal garage_requested
signal events_requested
signal settings_requested
signal ghost_requested

var _root: Control
var _coins_label: Label
var _level_label: Label
var _title: Label
var _sub: Label


static func _any_ghost() -> bool:
	var dir := DirAccess.open("user://ghosts")
	if dir == null:
		return false
	for f in dir.get_files():
		if f.ends_with(".json"):
			return true
	return false


func _ready() -> void:
	layer = 20
	_root = Control.new()
	_root.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(_root)
	_build()


func _build() -> void:
	var dim := ColorRect.new()
	dim.color = Color(0.005, 0.008, 0.02, 0.82)
	dim.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.add_child(dim)

	_title = UiTheme.make_label("NEON RUSH", 92, UiTheme.CYAN)
	_title.set_anchors_preset(Control.PRESET_CENTER_TOP)
	_title.position = Vector2(-400, 40)
	_title.custom_minimum_size = Vector2(800, 110)
	_root.add_child(_title)

	_sub = UiTheme.make_label("RACING", 34, UiTheme.MAGENTA)
	_sub.set_anchors_preset(Control.PRESET_CENTER_TOP)
	_sub.position = Vector2(-400, 140)
	_sub.custom_minimum_size = Vector2(800, 50)
	_root.add_child(_sub)

	var v := UiTheme.vertical(14)
	v.set_anchors_preset(Control.PRESET_CENTER)
	v.position = Vector2(-170, -60)
	_root.add_child(v)

	var play := UiTheme.make_button(L10n.tr_neon("menu.play"), UiTheme.CYAN, 34)
	play.custom_minimum_size = Vector2(340, 74)
	play.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_click")
		play_requested.emit())
	v.add_child(play)

	if _any_ghost():
		var ghost := UiTheme.make_button(L10n.tr_neon("menu.ghost"), Color(0.3, 0.95, 1.0, 0.8), 26)
		ghost.custom_minimum_size = Vector2(340, 56)
		ghost.pressed.connect(func():
			Sfx.play_ui("SFX/sfx_ui_click")
			ghost_requested.emit())
		v.add_child(ghost)

	var garage := UiTheme.make_button(L10n.tr_neon("menu.garage"), UiTheme.MAGENTA, 26)
	garage.custom_minimum_size = Vector2(340, 56)
	garage.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_click")
		garage_requested.emit())
	v.add_child(garage)

	var events := UiTheme.make_button(L10n.tr_neon("menu.events"), UiTheme.GOOD, 26)
	events.custom_minimum_size = Vector2(340, 56)
	events.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_click")
		events_requested.emit())
	v.add_child(events)

	var settings := UiTheme.make_button(L10n.tr_neon("menu.settings"), UiTheme.DIM, 26)
	settings.custom_minimum_size = Vector2(340, 56)
	settings.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_click")
		settings_requested.emit())
	v.add_child(settings)

	# Header: coins + level
	var header := UiTheme.make_panel()
	header.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	header.position = Vector2(-460, 20)
	header.custom_minimum_size = Vector2(430, 56)
	_root.add_child(header)
	var h := UiTheme.horizontal(20)
	header.add_child(h)
	_coins_label = UiTheme.make_label("", 24, Color(1.0, 0.84, 0.3))
	_level_label = UiTheme.make_label("", 22, UiTheme.CYAN)
	h.add_child(_coins_label)
	h.add_child(_level_label)

	var ver := UiTheme.make_label("GODOT 4 EDITION — v1.0", 16, UiTheme.DIM)
	ver.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	ver.position = Vector2(24, -34)
	_root.add_child(ver)


func refresh_header() -> void:
	_coins_label.text = "◉ %s %d" % [L10n.tr_neon("menu.coins"), Save.coins]
	_level_label.text = "%s %d  (%d/%d)" % [L10n.tr_neon("menu.level"), Save.level, Save.xp, Save.xp_to_next(Save.level)]


func hide_menu() -> void:
	_root.visible = false


func show_menu() -> void:
	refresh_header()
	_root.visible = true
