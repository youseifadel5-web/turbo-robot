extends Node
## Sfx — pooled one-shot player + clip registry (ports SfxPlayer + AudioClipRegistry).
## Clips live under res://assets/audio/{Engine,SFX,Ambience}.

var _cache := {}
var _pool: Array[AudioStreamPlayer] = []
const POOL_SIZE := 10


func _ready() -> void:
	for i in POOL_SIZE:
		var p := AudioStreamPlayer.new()
		p.bus = &"Master"
		add_child(p)
		_pool.append(p)


static func clip_path(key: String) -> String:
	# key like "SFX/sfx_ui_click" or "Engine/engine_A_idle"
	return "res://assets/audio/%s.wav" % key.replace("Engine/", "engine/").replace("SFX/", "sfx/").replace("Ambience/", "ambience/")


func get_clip(key: String) -> AudioStream:
	if _cache.has(key):
		return _cache[key]
	var path := clip_path(key)
	var clip: AudioStream = null
	if ResourceLoader.exists(path):
		clip = load(path)
	if clip == null and key.contains("/"):
		# trimmed-name fallback (Unity import quirk)
		var alt := "res://assets/audio/%s.wav" % (key.get_file().replace("Engine_", "engine_").to_lower())
		if ResourceLoader.exists(alt):
			clip = load(alt)
	_cache[key] = clip
	return clip


func play(key: String, volume := 1.0, pitch := 1.0) -> void:
	var clip := get_clip(key)
	if clip == null:
		return
	var p := _next_free()
	p.stream = clip
	p.pitch_scale = pitch
	p.volume_db = linear_to_db(clampf(volume * Bus.sfx_out(), 0.0001, 1.0))
	p.play()


func play_ui(key: String, volume := 1.0) -> void:
	var clip := get_clip(key)
	if clip == null:
		return
	var p := _next_free()
	p.stream = clip
	p.pitch_scale = 1.0
	p.volume_db = linear_to_db(clampf(volume * Bus.ui_out(), 0.0001, 1.0))
	p.play()


func _next_free() -> AudioStreamPlayer:
	for p in _pool:
		if not p.playing:
			return p
	return _pool[0]
