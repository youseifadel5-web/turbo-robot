extends Node
## AudioBus — port of NeonRush.Audio.AudioBus (runtime mixing + crash ducking).
## Volumes are applied by every player through these getters.

var master := 1.0
var music := 0.7
var sfx := 1.0
var engine := 1.0
var environment := 0.8
var ui := 0.85

var _music_duck := 1.0
const CRASH_DUCK := 0.55
const DUCK_RECOVER := 2.0


func _ready() -> void:
	master = Save.master_volume
	music = Save.music_volume
	sfx = Save.sfx_volume
	engine = Save.engine_volume if Save.engine_volume > 0.0 else 1.0


func _process(delta: float) -> void:
	_music_duck = move_toward(_music_duck, 1.0, DUCK_RECOVER * delta)


func set_master(v: float) -> void: master = clampf(v, 0.0, 1.0)
func set_music(v: float) -> void: music = clampf(v, 0.0, 1.0)
func set_sfx(v: float) -> void: sfx = clampf(v, 0.0, 1.0)
func set_engine(v: float) -> void: engine = clampf(v, 0.0, 1.0)


func music_out() -> float: return music * master * _music_duck
func sfx_out() -> float: return sfx * master
func engine_out() -> float: return engine * master
func ui_out() -> float: return ui * master
func env_out() -> float: return environment * master


func pulse_crash_duck() -> void:
	_music_duck = minf(_music_duck, CRASH_DUCK)
