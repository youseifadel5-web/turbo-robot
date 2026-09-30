extends Node
## GameSession — cross-scene race request (port of NeonRush.Core.GameSession).

var next_track: Dictionary = {}   # TrackConfig as dictionary
var selected_car_id := "falcon_s"
var race_type := "Quick"          # Quick | Career | Ghost
var difficulty := 1               # 0 Easy, 1 Normal, 2 Hard, 3 Expert
var ghost_race := false
var career_event: Dictionary = {}


static func make_track_config(preset: int, laps: int, display_name: String,
		track_id: String, time_of_day := 0, has_traffic := true, traffic_density := 1) -> Dictionary:
	return {
		"trackId": track_id,
		"displayName": display_name,
		"lengthPreset": preset,        # 0 short, 1 medium, 2 long, 3 extreme
		"laps": maxi(1, laps),
		"checkpointCount": 8,
		"hasTraffic": has_traffic,
		"trafficDensity": clampi(traffic_density, 0, 2),
		"allowNitro": true,
		"scoringDrift": true,
		"timeOfDay": time_of_day,      # 0 night, 1 sunset, 2 dawn, 3 day
	}


static func target_km(preset: int) -> float:
	match preset:
		0: return 3.0
		1: return 6.5
		2: return 14.0
		3: return 22.0
	return 6.0


func consume() -> void:
	next_track = {}
	race_type = "Quick"
	ghost_race = false
	career_event = {}
