extends Node3D
## EngineAudio — port of EngineAudioController: 5 crossfaded layers
## (idle/low/mid/high + nitro) driven by speed-derived RPM.

var car: RigidBody3D = null

var _layers: Array[AudioStreamPlayer] = []
var _nitro_src: AudioStreamPlayer
var _smooth_rpm := 0.0
const LAYER_KEYS := ["idle", "low", "mid", "high"]


func setup(target_car: RigidBody3D) -> void:
	car = target_car
	var cls := "B"
	if car and "car_class" in car:
		cls = CarCatalog.car_class_letter(car.car_class)
	for i in LAYER_KEYS.size():
		var key := "Engine/engine_%s_%s" % [cls, LAYER_KEYS[i]]
		var clip: AudioStream = Sfx.get_clip(key)
		if clip == null:
			clip = Sfx.get_clip("Engine/engine_B_%s" % LAYER_KEYS[i])
		var p := AudioStreamPlayer.new()
		p.stream = clip
		p.pitch_scale = 1.0
		add_child(p)
		p.volume_db = -60.0
		p.play()
		_layers.append(p)
	var nitro_clip: AudioStream = Sfx.get_clip("Engine/engine_nitro")
	_nitro_src = AudioStreamPlayer.new()
	_nitro_src.stream = nitro_clip
	_nitro_src.volume_db = -60.0
	add_child(_nitro_src)
	_nitro_src.play()


func _process(delta: float) -> void:
	if car == null or _layers.is_empty():
		return
	var speed_kmh: float = car.get("speed_kmh")
	var nitro_active: bool = car.get("is_nitro_active")

	var speed_norm := clampf(speed_kmh / 280.0, 0.0, 1.0)
	var rpm := clampf(speed_norm * 0.85 + (0.2 if nitro_active else 0.0), 0.0, 1.0)
	_smooth_rpm = lerpf(_smooth_rpm, rpm, 1.0 - exp(-8.0 * delta))

	var eng_vol := Bus.engine_out()
	_set_layer(0, _curve(_smooth_rpm, 0.0, 0.25), 0.85 + _smooth_rpm * 0.2, eng_vol, delta)
	_set_layer(1, _curve(_smooth_rpm, 0.1, 0.45), 0.9 + _smooth_rpm * 0.3, eng_vol, delta)
	_set_layer(2, _curve(_smooth_rpm, 0.35, 0.7), 1.0 + _smooth_rpm * 0.35, eng_vol, delta)
	_set_layer(3, _curve(_smooth_rpm, 0.55, 1.0), 1.05 + _smooth_rpm * 0.45, eng_vol, delta)

	var n := 1.0 if nitro_active else 0.0
	if _nitro_src.stream:
		var cur_n := db_to_linear(_nitro_src.volume_db)
		_nitro_src.volume_db = linear_to_db(clampf(lerpf(cur_n, n * 0.7 * eng_vol, delta * 10.0), 0.0001, 1.0))
		_nitro_src.pitch_scale = 1.1 + _smooth_rpm * 0.4


func _set_layer(i: int, weight: float, pitch: float, bus_vol: float, delta: float) -> void:
	if i >= _layers.size() or _layers[i].stream == null:
		return
	var target := clampf(weight * bus_vol * 0.55, 0.0001, 1.0)
	var cur := db_to_linear(_layers[i].volume_db)
	_layers[i].volume_db = linear_to_db(clampf(lerpf(cur, target, delta * 8.0), 0.0001, 1.0))
	_layers[i].pitch_scale = pitch


static func _curve(t: float, a: float, b: float) -> float:
	if t <= a:
		return 0.0
	if t >= b:
		return clampf(1.0 - (t - b) * 1.5, 0.0, 1.0)
	return smoothstep(0.0, 1.0, (t - a) / maxf(0.01, b - a))
