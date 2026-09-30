class_name UiTheme
## Neon Rush UI theme — port of NeonRush.UI.UiTheme.
## Cyan #00F0FF + Magenta #FF2BD6 on deep space blue.

const CYAN := Color(0.0, 0.94, 1.0)
const MAGENTA := Color(1.0, 0.17, 0.84)
const BG := Color(0.01, 0.015, 0.03, 1.0)
const PANEL := Color(0.045, 0.055, 0.095, 0.94)
const PANEL_LINE := Color(0.0, 0.94, 1.0, 0.55)
const TEXT := Color(0.9, 0.95, 1.0)
const DIM := Color(0.55, 0.62, 0.72)
const GOOD := Color(0.49, 1.0, 0.3)
const FONT_SIZE := 22
const FONT_SIZE_BIG := 40
const FONT_SIZE_HUGE := 110


static func panel_rect() -> StyleBoxFlat:
	var sb := StyleBoxFlat.new()
	sb.bg_color = PANEL
	sb.border_color = PANEL_LINE
	sb.set_border_width_all(2)
	sb.set_corner_radius_all(10)
	sb.content_margin_left = 18
	sb.content_margin_right = 18
	sb.content_margin_top = 12
	sb.content_margin_bottom = 12
	return sb


static func button_style(base := CYAN) -> StyleBoxFlat:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color(base.r, base.g, base.b, 0.12)
	sb.border_color = base
	sb.set_border_width_all(2)
	sb.set_corner_radius_all(8)
	sb.content_margin_left = 26
	sb.content_margin_right = 26
	sb.content_margin_top = 12
	sb.content_margin_bottom = 12
	return sb


static func make_button(text: String, color := CYAN, size := FONT_SIZE) -> Button:
	var b := Button.new()
	b.text = text
	b.add_theme_font_size_override("font_size", size)
	b.add_theme_color_override("font_color", TEXT)
	b.add_theme_color_override("font_hover_color", color)
	b.add_theme_color_override("font_pressed_color", color)
	b.add_theme_color_override("font_focus_color", color)
	b.add_theme_stylebox_override("normal", button_style(color))
	b.add_theme_stylebox_override("hover", button_style(color))
	b.add_theme_stylebox_override("pressed", button_style(Color(color.r, color.g, color.b, 0.6)))
	b.add_theme_stylebox_override("focus", button_style(color))
	b.mouse_filter = Control.MOUSE_FILTER_STOP
	return b


static func make_label(text: String, size := FONT_SIZE, color := TEXT) -> Label:
	var l := Label.new()
	l.text = text
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", color)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	return l


static func make_panel() -> PanelContainer:
	var p := PanelContainer.new()
	p.add_theme_stylebox_override("panel", panel_rect())
	return p


static func horizontal(n := 8) -> HBoxContainer:
	var h := HBoxContainer.new()
	h.add_theme_constant_override("separation", n)
	return h


static func vertical(n := 8) -> VBoxContainer:
	var v := VBoxContainer.new()
	v.add_theme_constant_override("separation", n)
	return v
