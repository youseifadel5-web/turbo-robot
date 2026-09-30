extends CanvasLayer
## CareerScreen port — chapters of events with stars, rewards, event selection.

signal event_selected(config: Dictionary, race_type: String)
signal exit_requested

var _root: Control
var _list: VBoxContainer
var _chapter_label: Label
var _chapter := 1


func _ready() -> void:
	layer = 20
	_root = Control.new()
	_root.set_anchors_preset(Control.PRESET_FULL_RECT)
	_root.visible = false
	add_child(_root)
	_build()


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

	var v := UiTheme.vertical(10)
	panel.add_child(v)

	var header := UiTheme.horizontal(12)
	_chapter_label = UiTheme.make_label("", 32, UiTheme.CYAN)
	header.add_child(_chapter_label)
	var stars := UiTheme.make_label("", 24, Color(1.0, 0.84, 0.3))
	stars.name = "StarsLabel"
	header.add_child(stars)
	v.add_child(header)

	_list = UiTheme.vertical(6)
	_list.custom_minimum_size = Vector2(780, 420)
	v.add_child(_list)

	var nav := UiTheme.horizontal(10)
	var prev := UiTheme.make_button("◀", UiTheme.CYAN, 24)
	prev.custom_minimum_size = Vector2(60, 52)
	prev.pressed.connect(func(): _set_chapter(_chapter - 1))
	nav.add_child(prev)
	var next := UiTheme.make_button("▶", UiTheme.CYAN, 24)
	next.custom_minimum_size = Vector2(60, 52)
	next.pressed.connect(func(): _set_chapter(_chapter + 1))
	nav.add_child(next)
	var back := UiTheme.make_button(L10n.tr_neon("garage.back"), UiTheme.MAGENTA, 22)
	back.custom_minimum_size = Vector2(180, 52)
	back.pressed.connect(func():
		Sfx.play_ui("SFX/sfx_ui_back", 0.6)
		exit_requested.emit())
	nav.add_child(back)
	v.add_child(nav)


func open() -> void:
	_root.visible = true
	_chapter = Save.career_chapter
	refresh()


func close() -> void:
	_root.visible = false


func is_open() -> bool:
	return _root.visible


func _set_chapter(c: int) -> void:
	Sfx.play_ui("SFX/sfx_ui_click", 0.6)
	_chapter = clampi(c, 1, 3)
	refresh()


func refresh() -> void:
	var events := CareerCatalog.get_events(_chapter)
	_chapter_label.text = "%s %d / 3" % [L10n.tr_neon("events.chapter"), _chapter]
	for c in _list.get_children():
		c.queue_free()

	for i in events.size():
		var ev: Dictionary = events[i]
		var is_current: bool = _chapter == Save.career_chapter and (i + 1) == Save.career_event
		var unlocked: bool = _chapter < Save.career_chapter or (_chapter == Save.career_chapter and (i + 1) <= Save.career_event)

		var row := UiTheme.horizontal(12)
		row.custom_minimum_size = Vector2(760, 74)

		var name_l := UiTheme.make_label(ev["title"], 24, UiTheme.TEXT if unlocked else UiTheme.DIM)
		name_l.custom_minimum_size = Vector2(240, 30)
		name_l.horizontal_alignment = HORIZONTAL_ALIGNMENT_LEFT
		row.add_child(name_l)

		var type_l := UiTheme.make_label(CareerCatalog.TYPE_NAMES[ev["type"]], 18, UiTheme.MAGENTA if unlocked else UiTheme.DIM)
		type_l.custom_minimum_size = Vector2(150, 24)
		row.add_child(type_l)

		var reward_l := UiTheme.make_label("◉%d  XP%d" % [ev["rewardCoins"], ev["rewardXp"]], 18, Color(1.0, 0.84, 0.3) if unlocked else UiTheme.DIM)
		reward_l.custom_minimum_size = Vector2(150, 24)
		row.add_child(reward_l)

		if is_current:
			var go := UiTheme.make_button("GO", UiTheme.GOOD, 22)
			go.custom_minimum_size = Vector2(90, 56)
			go.pressed.connect(func():
				Sfx.play_ui("SFX/sfx_ui_click", 0.8)
				var cfg := Session.make_track_config(int(ev["length"]), int(ev["laps"]),
					ev["title"], ev["id"], 0, true, 1)
				Session.next_track = cfg
				Session.race_type = "Career"
				Session.career_event = ev
				event_selected.emit(cfg, "Career"))
			row.add_child(go)
		elif unlocked:
			row.add_child(UiTheme.make_label("✓", 22, UiTheme.GOOD))
		else:
			row.add_child(UiTheme.make_label("🔒", 20, UiTheme.DIM))

		_list.add_child(row)
