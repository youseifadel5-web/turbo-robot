extends Node
## SaveSystem — port of NeonRush.Save.SaveSystem (JSON instead of PlayerPrefs).
## Same fields as the Unity version so the game balance is identical.

const KEY := "NeonRush_Save_v2"
const PATH := "user://save.json"

var level := 1
var xp := 0
var coins := 2500
var premium_currency := 0
var career_chapter := 1
var career_event := 1
var career_stars := 0
var selected_car_id := "falcon_s"
var owned_cars: Array = []   # [{carId, engineLevel, turboLevel, brakesLevel, nitroLevel, paintId, rimId, neonId}]
var best_time := 0.0
var best_drift := 0.0
var graphics_level := 2
var target_fps := 60
var master_volume := 1.0
var music_volume := 0.7
var sfx_volume := 1.0
var engine_volume := 1.0
var vibration := true
var steering_sensitivity := 1.0
var control_scheme := 0        # 0 buttons, 1 wheel, 2 tilt
var language := 0              # 0 EN, 1 AR
var music_shuffle := false
var music_repeat := false
var premium_unlocked := false
var track_bests: Array = []     # [{track_id, best_time}]


func _ready() -> void:
	load_save()


func to_dict() -> Dictionary:
	return {
		"level": level, "xp": xp, "coins": coins, "premiumCurrency": premium_currency,
		"careerChapter": career_chapter, "careerEvent": career_event, "careerStars": career_stars,
		"selectedCarId": selected_car_id, "ownedCars": owned_cars,
		"bestTime": best_time, "bestDrift": best_drift,
		"graphicsLevel": graphics_level, "targetFps": target_fps,
		"masterVolume": master_volume, "musicVolume": music_volume,
		"sfxVolume": sfx_volume, "engineVolume": engine_volume,
		"vibration": vibration, "steeringSensitivity": steering_sensitivity,
		"controlScheme": control_scheme, "language": language,
		"musicShuffle": music_shuffle, "musicRepeat": music_repeat,
		"premiumUnlocked": premium_unlocked, "trackBests": track_bests,
	}


func from_dict(d: Dictionary) -> void:
	level = int(d.get("level", 1))
	xp = int(d.get("xp", 0))
	coins = int(d.get("coins", 2500))
	premium_currency = int(d.get("premiumCurrency", 0))
	career_chapter = int(d.get("careerChapter", 1))
	career_event = int(d.get("careerEvent", 1))
	career_stars = int(d.get("careerStars", 0))
	selected_car_id = str(d.get("selectedCarId", "falcon_s"))
	owned_cars = d.get("ownedCars", [])
	best_time = float(d.get("bestTime", 0.0))
	best_drift = float(d.get("bestDrift", 0.0))
	graphics_level = int(d.get("graphicsLevel", 2))
	target_fps = int(d.get("targetFps", 60))
	master_volume = float(d.get("masterVolume", 1.0))
	music_volume = float(d.get("musicVolume", 0.7))
	sfx_volume = float(d.get("sfxVolume", 1.0))
	engine_volume = float(d.get("engineVolume", 1.0))
	vibration = bool(d.get("vibration", true))
	steering_sensitivity = float(d.get("steeringSensitivity", 1.0))
	control_scheme = int(d.get("controlScheme", 0))
	language = int(d.get("language", 0))
	music_shuffle = bool(d.get("musicShuffle", false))
	music_repeat = bool(d.get("musicRepeat", false))
	premium_unlocked = bool(d.get("premiumUnlocked", false))
	track_bests = d.get("trackBests", [])


func save() -> void:
	var f := FileAccess.open(PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(to_dict()))
		f.close()


func load_save() -> void:
	if not FileAccess.file_exists(PATH):
		_ensure_starter_car()
		return
	var f := FileAccess.open(PATH, FileAccess.READ)
	if f == null:
		_ensure_starter_car()
		return
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	if parsed is Dictionary:
		from_dict(parsed)
	else:
		_ensure_starter_car()
	_ensure_starter_car()


func _ensure_starter_car() -> void:
	var has := false
	for c in owned_cars:
		if c is Dictionary and c.get("carId", "") == "falcon_s":
			has = true
			break
	if not has:
		owned_cars.append({
			"carId": "falcon_s", "engineLevel": 0, "turboLevel": 0,
			"brakesLevel": 0, "nitroLevel": 0,
			"paintId": "default", "rimId": "stock", "neonId": "cyan",
		})
		selected_car_id = "falcon_s"


func add_coins(amount: int) -> void:
	coins = max(0, coins + amount)
	save()


func spend_coins(amount: int) -> bool:
	if coins < amount:
		return false
	coins -= amount
	save()
	return true


func xp_to_next(lv: int) -> int:
	return 200 + lv * 75


func add_xp(amount: int) -> void:
	xp += max(0, amount)
	while xp >= xp_to_next(level):
		xp -= xp_to_next(level)
		level += 1
	save()


func record_race(time: float, drift_score: float) -> void:
	if best_time <= 0.0 or time < best_time:
		best_time = time
	if drift_score > best_drift:
		best_drift = drift_score
	save()


func get_owned(car_id: String) -> Dictionary:
	for c in owned_cars:
		if c is Dictionary and c.get("carId", "") == car_id:
			return c
	var o := {
		"carId": car_id, "engineLevel": 0, "turboLevel": 0,
		"brakesLevel": 0, "nitroLevel": 0,
		"paintId": "default", "rimId": "stock", "neonId": "cyan",
	}
	owned_cars.append(o)
	return o


func is_owned(car_id: String) -> bool:
	for c in owned_cars:
		if c is Dictionary and c.get("carId", "") == car_id:
			return true
	return false


func delete_save() -> void:
	DirAccess.remove_absolute(ProjectSettings.globalize_path(PATH))
	level = 1; xp = 0; coins = 2500; premium_currency = 0
	career_chapter = 1; career_event = 1; career_stars = 0
	selected_car_id = "falcon_s"; owned_cars = []
	best_time = 0.0; best_drift = 0.0
	graphics_level = 2; target_fps = 60
	master_volume = 1.0; music_volume = 0.7; sfx_volume = 1.0; engine_volume = 1.0
	vibration = true; steering_sensitivity = 1.0; control_scheme = 0; language = 0
	music_shuffle = false; music_repeat = false; premium_unlocked = false
	track_bests = []
	_ensure_starter_car()


# ---------------- Local leaderboard (was GameServices.LocalLeaderboard) ----------------

func submit_time(track_id: String, seconds: float) -> void:
	if track_id.is_empty() or seconds <= 0.0:
		return
	for b in track_bests:
		if b is Dictionary and b.get("trackId", "") == track_id:
			if seconds < float(b.get("bestTime", 9e9)):
				b["bestTime"] = seconds
			save()
			return
	track_bests.append({"trackId": track_id, "bestTime": seconds})
	save()


func get_best_time(track_id: String) -> float:
	for b in track_bests:
		if b is Dictionary and b.get("trackId", "") == track_id:
			return float(b.get("bestTime", 0.0))
	return 0.0
