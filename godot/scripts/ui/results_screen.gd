extends CanvasLayer
## ResultsScreen port — end-of-race stats, stars, rewards; retry / menu / next.

signal retry_requested
signal menu_requested
signal next_requested

var _root: Control
var _body: VBoxContainer


func _ready() -> void:
	layer = 30
	_root = Control.new()
	_root.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.visible = false
	add_child(_root)
	var dim := ColorRect.new()
	dim.color = Color(0.005, 0.008, 0.02, 0.88)
	dim.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.add_child(dim)

	var panel := UiTheme.make_panel()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.position = Vector2(-380, -260)
	panel.custom_minimum_size = Vector2(760, 520)
	_root.add_child(panel)

	var v := UiTheme.vertical(12)
	panel.add_child(v)
	v.add_child(UiTheme.make_label(L10n.tr_neon("results.title"), 44, UiTheme.CYAN))
	_body = UiTheme.vertical(6)
	v.add_child(_body)

	var buttons := UiTheme.horizontal(12)
	var retry := UiTheme.make_button(L10n.tr_neon("results.retry"), UiTheme.CYAN, 26)
	retry.custom_minimum_size = Vector2(200, 64)
	retry.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_click", 0.8)
		retry_requested.emit())
	buttons.add_child(retry)
	var menu := UiTheme.make_button(L10n.tr_neon("results.menu"), UiTheme.MAGENTA, 26)
	menu.custom_minimum_size = Vector2(200, 64)
	menu.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_back", 0.8)
		menu_requested.emit())
	buttons.add_child(menu)
	var next := UiTheme.make_button(L10n.tr_neon("results.next"), UiTheme.GOOD, 26)
	next.custom_minimum_size = Vector2(240, 64)
	next.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_click", 0.8)
		next_requested.emit())
	buttons.add_child(next)
	v.add_child(buttons)


func show_results(results: Dictionary, stars: int, coins: int, xp: int) -> void:
	for c in _body.get_children():
		c.queue_free()
	var rows := [
		[L10n.tr_neon("results.position"), "P%d / %d" % [results.get("position", 1), results.get("totalRacers", 1)]],
		[L10n.tr_neon("results.time"), "%.2f" % results.get("totalTime", 0.0)],
		[L10n.tr_neon("results.bestlap"), "%.2f" % results.get("bestLap", 0.0)],
		[L10n.tr_neon("results.drift"), "%d" % int(results.get("driftScore", 0.0))],
		[L10n.tr_neon("results.topspeed"), "%d KM/H" % int(results.get("topSpeedKmh", 0.0))],
	]
	for row in rows:
		var h := UiTheme.horizontal(24)
		var l := UiTheme.make_label(str(row[0]), 22, UiTheme.DIM)
		l.custom_minimum_size = Vector2(260, 30)
		l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
		h.add_child(l)
		var r := UiTheme.make_label(str(row[1]), 24, UiTheme.TEXT)
		h.add_child(r)
		_body.add_child(h)

	_body.add_child(UiTheme.make_label("★".repeat(stars) + "☆".repeat(maxi(0, 3 - stars)), 42, Color(1.0, 0.84, 0.3)))
	_body.add_child(UiTheme.make_label("%s  ◉+%d   XP+%d" % [L10n.tr_neon("results.reward"), coins, xp], 24, UiTheme.GOOD))
	_root.visible = true


func hide_results() -> void:
	_root.visible = false
