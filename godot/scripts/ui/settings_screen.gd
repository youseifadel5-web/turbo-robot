extends CanvasLayer
## SettingsScreen port — graphics, FPS, volumes, vibration, controls,
## sensitivity, language (EN/AR), delete save.

signal exit_requested

var _root: Control

const GRAPHICS_NAMES := ["LOW", "MEDIUM", "HIGH", "ULTRA"]
const FPS_OPTIONS := [30, 60, 90, 120]
const CONTROL_NAMES := ["BUTTONS", "WHEEL", "TILT"]
const DAMAGE_NAMES := ["OFF", "VISUAL", "FULL"]


func _ready() -> void:
	layer = 20
	_root = Control.new()
	_root.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.visible = false
	add_child(_root)
	_build()


func open() -> void:
	_root.visible = true
	_refresh()


func close() -> void:
	_root.visible = false


func is_open() -> bool:
	return _root.visible


func _build() -> void:
	var dim := ColorRect.new()
	dim.color = Color(0.005, 0.008, 0.02, 0.9)
	dim.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.add_child(dim)

	var panel := UiTheme.make_panel()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.position = Vector2(-420, -300)
	panel.custom_minimum_size = Vector2(840, 600)
	_root.add_child(panel)

	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(800, 540)
	panel.add_child(scroll)

	var v := UiTheme.vertical(10)
	scroll.add_child(v)

	v.add_child(_cycle_row(L10n.tr_neon("settings.graphics"), GRAPHICS_NAMES,
		func(): return GRAPHICS_NAMES[clampi(Save.graphics_level, 0, 3)],
		func(dir: int):
			Save.graphics_level = clampi(Save.graphics_level + dir, 0, 3)
			Save.save()))

	v.add_child(_cycle_row(L10n.tr_neon("settings.fps"), FPS_OPTIONS.map(func(x): return str(x)),
		func(): return str(Save.target_fps),
		func(dir: int):
			var idx := FPS_OPTIONS.find(Save.target_fps)
			idx = clampi(idx + dir, 0, FPS_OPTIONS.size() - 1)
			Save.target_fps = FPS_OPTIONS[idx]
			Engine.max_fps = Save.target_fps
			Save.save()))

	v.add_child(_cycle_row(L10n.tr_neon("settings.language"), ["EN", "العربية"],
		func(): return ["EN", "AR"][Save.language],
		func(_dir: int):
			L10n.cycle()
			# Rebuild the whole screen so labels refresh
			_root.queue_free()
			_ready()))

	v.add_child(_toggle_row(L10n.tr_neon("settings.vibration"), func(): return Save.vibration,
		func(on: bool):
			Save.vibration = on
			Save.save()))

	v.add_child(_slider_row(L10n.tr_neon("settings.master"), func(): return Save.master_volume,
		func(v: float):
			Save.master_volume = v
			Bus.set_master(v)
			Save.save()))
	v.add_child(_slider_row(L10n.tr_neon("settings.music"), func(): return Save.music_volume,
		func(v: float):
			Save.music_volume = v
			Bus.set_music(v)
			Save.save()))
	v.add_child(_slider_row(L10n.tr_neon("settings.sfx"), func(): return Save.sfx_volume,
		func(v: float):
			Save.sfx_volume = v
			Bus.set_sfx(v)
			Save.save()))
	v.add_child(_slider_row(L10n.tr_neon("settings.engine"), func(): return Save.engine_volume,
		func(v: float):
			Save.engine_volume = v
			Bus.set_engine(v)
			Save.save()))

	v.add_child(_slider_row(L10n.tr_neon("settings.sensitivity"), func(): return Save.steering_sensitivity,
		func(v: float):
			Save.steering_sensitivity = clampf(v, 0.3, 2.0)
			Save.save()))

	v.add_child(_cycle_row(L10n.tr_neon("settings.controls"), CONTROL_NAMES,
		func(): return CONTROL_NAMES[clampi(Save.control_scheme, 0, 2)],
		func(dir: int):
			Save.control_scheme = clampi(Save.control_scheme + dir, 0, 2)
			Save.save()))

	var close := UiTheme.make_button(L10n.tr_neon("settings.close"), UiTheme.MAGENTA, 24)
	close.custom_minimum_size = Vector2(300, 60)
	close.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_back", 0.6)
		exit_requested.emit())
	v.add_child(close)


func _refresh() -> void:
	Engine.max_fps = Save.target_fps


func _cycle_row(title: String, options: Array, getter: Callable, setter: Callable) -> HBoxContainer:
	var h := UiTheme.horizontal(12)
	var l := UiTheme.make_label(title, 20, UiTheme.TEXT)
	l.custom_minimum_size = Vector2(340, 30)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	h.add_child(l)
	var prev := UiTheme.make_button("◀", UiTheme.DIM, 20)
	prev.custom_minimum_size = Vector2(50, 44)
	prev.pressed.connect(func():
		setter.call(-1)
		Sfx.play_ui("SFX/sfx_ui_click", 0.5))
	h.add_child(prev)
	var value := UiTheme.make_label(str(getter.call()), 20, UiTheme.CYAN)
	value.custom_minimum_size = Vector2(130, 30)
	h.add_child(value)
	var next := UiTheme.make_button("▶", UiTheme.DIM, 20)
	next.custom_minimum_size = Vector2(50, 44)
	next.pressed.connect(func():
		setter.call(1)
		Sfx.play_ui("SFX/sfx_ui_click", 0.5))
	h.add_child(next)
	return h


func _toggle_row(title: String, getter: Callable, setter: Callable) -> HBoxContainer:
	var h := UiTheme.horizontal(12)
	var l := UiTheme.make_label(title, 20, UiTheme.TEXT)
	l.custom_minimum_size = Vector2(340, 30)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	h.add_child(l)
	var cb := CheckButton.new()
	cb.button_pressed = getter.call()
	cb.toggled.connect(func(on: bool):
		setter.call(on)
		Sfx.play_ui("SFX/sfx_ui_click", 0.5))
	h.add_child(cb)
	return h


func _slider_row(title: String, getter: Callable, setter: Callable) -> HBoxContainer:
	var h := UiTheme.horizontal(12)
	var l := UiTheme.make_label(title, 20, UiTheme.TEXT)
	l.custom_minimum_size = Vector2(340, 30)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
	h.add_child(l)
	var s := HSlider.new()
	s.min_value = 0.0
	s.max_value = 1.0
	s.step = 0.05
	s.custom_minimum_size = Vector2(240, 30)
	s.value = getter.call()
	s.value_changed.connect(func(v: float):
		setter.call(v))
	h.add_child(s)
	return h
